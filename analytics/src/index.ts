import { zValidator } from "@hono/zod-validator";
import { Hono } from "hono";
import { bodyLimit } from "hono/body-limit";
import { z } from "zod";
import type { Bindings } from "./env";
import { analyticsEventSchema } from "./events";
import { isDistributionDimension, queryActivityTrend, queryDistribution } from "./lib/analytics";
import { hashDeviceId, tokensEqual } from "./lib/crypto";
import { queryDeviceDistribution, queryDeviceVersions, upsertDevice } from "./lib/devices";

const daysSchema = z.coerce.number().int().min(1).max(90);

interface OverviewRow {
  totalDevices: number;
  totalLaunches: number;
  activeToday: number;
  activeThisWeek: number;
  newToday: number;
}

interface NewDeviceRow {
  date: string;
  newDevices: number;
}

function startOfUtcDaySeconds(date: Date): number {
  return Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate()) / 1000;
}

function parseDays(value: string | undefined): number | null {
  const result = daysSchema.safeParse(value ?? "30");
  return result.success ? result.data : null;
}

async function loadOverview(env: Bindings) {
  const now = new Date();
  const todayStart = startOfUtcDaySeconds(now);
  const weekStart = Math.floor(now.getTime() / 1000) - 7 * 24 * 60 * 60;
  const row = await env.DB.prepare(
    `SELECT
      COUNT(*) AS totalDevices,
      COALESCE(SUM(launch_count), 0) AS totalLaunches,
      COALESCE(SUM(CASE WHEN last_seen_at >= ?1 THEN 1 ELSE 0 END), 0) AS activeToday,
      COALESCE(SUM(CASE WHEN last_seen_at >= ?2 THEN 1 ELSE 0 END), 0) AS activeThisWeek,
      COALESCE(SUM(CASE WHEN first_seen_at >= ?1 THEN 1 ELSE 0 END), 0) AS newToday
    FROM devices`,
  )
    .bind(todayStart, weekStart)
    .first<OverviewRow>();

  return {
    generatedAt: now.toISOString(),
    totalDevices: Number(row?.totalDevices ?? 0),
    totalLaunches: Number(row?.totalLaunches ?? 0),
    activeToday: Number(row?.activeToday ?? 0),
    activeThisWeek: Number(row?.activeThisWeek ?? 0),
    newToday: Number(row?.newToday ?? 0),
  };
}

async function loadTrend(env: Bindings, days: number) {
  const since = Math.floor(Date.now() / 1000) - days * 24 * 60 * 60;
  const [activity, registrations] = await Promise.all([
    queryActivityTrend(env, days),
    env.DB.prepare(
      `SELECT date(first_seen_at, 'unixepoch') AS date, COUNT(*) AS newDevices
      FROM devices
      WHERE first_seen_at >= ?1
      GROUP BY date
      ORDER BY date`,
    )
      .bind(since)
      .all<NewDeviceRow>(),
  ]);

  const trend = new Map<
    string,
    { date: string; launches: number; activeDevices: number; newDevices: number }
  >();
  for (const row of activity) {
    trend.set(row.date, {
      date: row.date,
      launches: Number(row.launches),
      activeDevices: Number(row.activeDevices),
      newDevices: 0,
    });
  }
  for (const row of registrations.results) {
    const current = trend.get(row.date) ?? {
      date: row.date,
      launches: 0,
      activeDevices: 0,
      newDevices: 0,
    };
    current.newDevices = Number(row.newDevices);
    trend.set(row.date, current);
  }

  return [...trend.values()].sort((left, right) => left.date.localeCompare(right.date));
}

function distributionValues(rows: Awaited<ReturnType<typeof queryDistribution>>) {
  return rows.map((row) => ({
    value: row.value,
    devices: Number(row.devices),
    launches: Number(row.launches),
  }));
}

export function createApp() {
  const app = new Hono<{ Bindings: Bindings }>();

  app.get("/healthz", (c) => c.json({ status: "ok", service: "yumebox-analytics" }));

  app.post(
    "/api/v1/events",
    bodyLimit({
      maxSize: 8 * 1024,
      onError: (c) => c.json({ error: "payload_too_large" }, 413),
    }),
    zValidator("json", analyticsEventSchema, (result, c) => {
      if (!result.success) return c.json({ error: "invalid_payload" }, 400);
    }),
    async (c) => {
      const secret = c.env.DEVICE_HASH_SECRET?.trim();
      if (!secret) return c.json({ error: "ingest_not_configured" }, 503);

      const event = c.req.valid("json");
      const deviceHash = await hashDeviceId(event.deviceId, secret);
      const nowSeconds = Math.floor(Date.now() / 1000);

      c.env.ANALYTICS.writeDataPoint({
        blobs: [
          "1",
          deviceHash,
          event.appVersion,
          String(event.versionCode),
          event.androidVersion,
          event.manufacturer ?? "Unknown",
        ],
        doubles: [1],
        indexes: [deviceHash],
      });

      c.executionCtx.waitUntil(
        upsertDevice(c.env.DB, event, deviceHash, nowSeconds).catch((error: unknown) => {
          console.error("Failed to update the device registry", error);
        }),
      );

      return c.json({ accepted: true }, 202);
    },
  );

  app.use("/api/v1/stats/*", async (c, next) => {
    c.header("Cache-Control", "no-store");
    const expected = c.env.STATS_API_TOKEN?.trim();
    if (!expected) return c.json({ error: "stats_not_configured" }, 503);
    const authorization = c.req.header("Authorization") ?? "";
    const received = authorization.startsWith("Bearer ") ? authorization.slice(7) : "";
    if (!received || !(await tokensEqual(received, expected))) {
      c.header("WWW-Authenticate", "Bearer");
      return c.json({ error: "unauthorized" }, 401);
    }
    await next();
  });

  app.use("/api/v1/stats/*", async (c, next) => {
    try {
      await next();
    } catch (error) {
      console.error("Stats query failed", error);
      return c.json({ error: "stats_query_failed" }, 502);
    }
  });

  app.get("/api/v1/stats/overview", async (c) => {
    return c.json(await loadOverview(c.env));
  });

  app.get("/api/v1/stats/trend", async (c) => {
    const days = parseDays(c.req.query("days"));
    if (!days) return c.json({ error: "days_must_be_between_1_and_90" }, 400);

    return c.json({
      days,
      trend: await loadTrend(c.env, days),
    });
  });

  app.get("/api/v1/stats/distribution/:dimension", async (c) => {
    const dimension = c.req.param("dimension");
    const days = parseDays(c.req.query("days"));
    if (!isDistributionDimension(dimension)) return c.json({ error: "unknown_dimension" }, 404);
    if (!days) return c.json({ error: "days_must_be_between_1_and_90" }, 400);

    const rows = await queryDistribution(c.env, dimension, days);
    return c.json({
      dimension,
      days,
      values: distributionValues(rows),
    });
  });

  app.get("/api/v1/public/metrics", async (c) => {
    const days = parseDays(c.req.query("days"));
    if (!days) return c.json({ error: "days_must_be_between_1_and_90" }, 400);

    try {
      const since = Math.floor(Date.now() / 1000) - days * 86_400;
      const [overview, trend, versions, androidVersion, manufacturer] = await Promise.all([
        loadOverview(c.env),
        loadTrend(c.env, days),
        queryDeviceVersions(c.env.DB, since),
        queryDeviceDistribution(c.env.DB, "androidVersion", since),
        queryDeviceDistribution(c.env.DB, "manufacturer", since),
      ]);

      c.header("Cache-Control", "public, max-age=60");
      return c.json({
        generatedAt: new Date().toISOString(),
        days,
        overview,
        trend,
        distributions: {
          versions: versions.map((row) => ({
            version: row.value,
            versionCode: "",
            devices: Number(row.devices),
            launches: Number(row.launches),
          })),
          androidVersion: distributionValues(androidVersion),
          manufacturer: distributionValues(manufacturer),
        },
      });
    } catch (error) {
      console.error("Public stats query failed", error);
      return c.json({ error: "stats_query_failed" }, 502);
    }
  });

  app.notFound((c) => c.json({ error: "not_found" }, 404));
  app.onError((error, c) => {
    console.error("Unhandled request failure", error);
    return c.json({ error: "internal_error" }, 500);
  });

  return app;
}

const app = createApp();

export default {
  async fetch(request, env, ctx): Promise<Response> {
    const path = new URL(request.url).pathname;
    if (path === "/healthz" || path.startsWith("/api/")) {
      return app.fetch(request, env, ctx);
    }
    return env.ASSETS.fetch(request);
  },
} satisfies ExportedHandler<Bindings>;
