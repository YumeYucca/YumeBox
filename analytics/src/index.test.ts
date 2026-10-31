import { afterEach, describe, expect, it, vi } from "vitest";
import { createApp } from "./index";
import type { Bindings } from "./env";
import { baseVersion } from "./lib/analytics";

const validEvent = {
  schemaVersion: 1,
  deviceId: "7d3c5c34-f9c2-4b7e-b47c-1a37f0f99db5",
  androidVersion: "15",
  manufacturer: "Xiaomi",
  appVersion: "0.6.0",
  versionCode: 602,
  event: "app_start",
};

function mocks() {
  const prepared = {
    bind: vi.fn().mockReturnThis(),
    run: vi.fn().mockResolvedValue({ success: true }),
    first: vi.fn().mockResolvedValue(null),
    all: vi.fn().mockResolvedValue({ results: [] }),
  };
  const env = {
    DB: { prepare: vi.fn().mockReturnValue(prepared) },
    ANALYTICS: { writeDataPoint: vi.fn() },
    ANALYTICS_SQL: { query: vi.fn().mockResolvedValue({ data: [] }) },
    DEVICE_HASH_SECRET: "test-device-secret-that-is-long-enough",
    STATS_API_TOKEN: "test-stats-token",
  } as unknown as Bindings;
  const pending: Promise<unknown>[] = [];
  const context = {
    waitUntil: vi.fn((promise: Promise<unknown>) => pending.push(promise)),
    passThroughOnException: vi.fn(),
  } as unknown as ExecutionContext;
  return { env, prepared, context, pending };
}

function request(path: string, init?: RequestInit): Request {
  const value = new Request(`https://analytics.example.test${path}`, init);
  Object.defineProperty(value, "cf", { value: { country: "CN", colo: "SJC", asn: 4134 } });
  return value;
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("Worker API", () => {
  it("reports health without requiring bindings", async () => {
    const response = await createApp().request("/healthz");
    expect(response.status).toBe(200);
    await expect(response.json()).resolves.toEqual({ status: "ok", service: "yumebox-analytics" });
  });

  it("rejects invalid events before writing data", async () => {
    const { env, context } = mocks();
    const response = await createApp().fetch(
      request("/api/v1/events", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ deviceId: "not-a-uuid" }),
      }),
      env,
      context,
    );
    expect(response.status).toBe(400);
    expect(env.ANALYTICS.writeDataPoint).not.toHaveBeenCalled();
    expect(env.DB.prepare).not.toHaveBeenCalled();
  });

  it("stores only the device hash and updates the device registry", async () => {
    const { env, prepared, context, pending } = mocks();
    const response = await createApp().fetch(
      request("/api/v1/events", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(validEvent),
      }),
      env,
      context,
    );
    expect(response.status).toBe(202);
    await Promise.all(pending);

    const point = vi.mocked(env.ANALYTICS.writeDataPoint).mock.calls[0]?.[0];
    if (!point) throw new Error("Expected an Analytics Engine data point");
    const deviceHash = point.blobs?.[1] ?? "";
    expect(deviceHash).toMatch(/^[a-f0-9]{64}$/);
    expect(deviceHash).not.toBe(validEvent.deviceId);
    expect(point.blobs).toEqual(["1", deviceHash, "0.6.0", "602", "15", "Xiaomi"]);
    expect(point.indexes).toEqual([deviceHash]);
    expect(prepared.bind).toHaveBeenCalledWith(
      deviceHash,
      expect.any(Number),
      "0.6.0",
      602,
      "15",
      "Xiaomi",
    );
    expect(JSON.stringify(prepared.bind.mock.calls)).not.toContain(validEvent.deviceId);
  });

  it("fails closed when the device hash secret is absent", async () => {
    const { env, context } = mocks();
    env.DEVICE_HASH_SECRET = "";
    const response = await createApp().fetch(
      request("/api/v1/events", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(validEvent),
      }),
      env,
      context,
    );
    expect(response.status).toBe(503);
    expect(env.ANALYTICS.writeDataPoint).not.toHaveBeenCalled();
  });

  it("protects aggregate statistics with a bearer token", async () => {
    const { env, context } = mocks();
    const response = await createApp().fetch(request("/api/v1/stats/overview"), env, context);
    expect(response.status).toBe(401);
    expect(env.DB.prepare).not.toHaveBeenCalled();
  });

  it("returns D1-backed overview metrics", async () => {
    const { env, prepared, context } = mocks();
    prepared.first.mockResolvedValue({
      totalDevices: 12,
      totalLaunches: 80,
      activeToday: 5,
      activeThisWeek: 10,
      newToday: 2,
    });
    const response = await createApp().fetch(
      request("/api/v1/stats/overview", { headers: { Authorization: "Bearer test-stats-token" } }),
      env,
      context,
    );
    expect(response.status).toBe(200);
    await expect(response.json()).resolves.toMatchObject({
      totalDevices: 12,
      totalLaunches: 80,
      activeToday: 5,
      activeThisWeek: 10,
      newToday: 2,
    });
  });

  it("uses only whitelisted Analytics Engine dimensions", async () => {
    const { env, context } = mocks();
    vi.mocked(env.ANALYTICS_SQL.query).mockResolvedValue({
      data: [{ value: "0.6.0", devices: 8, launches: 20 }],
    } as never);
    const response = await createApp().fetch(
      request("/api/v1/stats/distribution/appVersion?days=30", {
        headers: { Authorization: "Bearer test-stats-token" },
      }),
      env,
      context,
    );
    expect(response.status).toBe(200);
    await expect(response.json()).resolves.toEqual({
      dimension: "appVersion",
      days: 30,
      values: [{ value: "0.6.0", devices: 8, launches: 20 }],
    });
    const sql = vi.mocked(env.ANALYTICS_SQL.query).mock.calls[0]?.[0].query ?? "";
    expect(sql).toContain("blob3 AS value");
    expect(sql).toContain("INTERVAL '30' DAY");
  });

  it("serves public aggregate metrics without a token", async () => {
    const { env, context } = mocks();
    const response = await createApp().fetch(
      request("/api/v1/public/metrics?days=30"),
      env,
      context,
    );
    expect(response.status).toBe(200);
    expect(response.headers.get("Cache-Control")).toBe("public, max-age=60");
    await expect(response.json()).resolves.toMatchObject({
      days: 30,
      overview: { totalDevices: 0, totalLaunches: 0 },
      trend: [],
      distributions: {
        versions: [],
        androidVersion: [],
        manufacturer: [],
      },
    });
    const queries = vi
      .mocked(env.ANALYTICS_SQL.query)
      .mock.calls.map((call) => call[0].query)
      .join("\n");
    expect(queries).toContain("lower(blob6) != 'unknown'");
  });

  describe("baseVersion", () => {
    it("folds dev build suffixes into the release version", () => {
      expect(baseVersion("0.6.5")).toBe("0.6.5");
      expect(baseVersion("0.6.5.moe.b965f0ea")).toBe("0.6.5");
      expect(baseVersion("0.7.0-dev")).toBe("0.7.0");
      expect(baseVersion(" nightly ")).toBe("nightly");
    });
  });
});
