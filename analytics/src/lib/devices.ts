import type { AnalyticsEvent } from "../events";
import { baseVersion } from "./analytics";

export type DeviceDistributionRow = {
  value: string;
  devices: number;
  launches: number;
};

const deviceColumns = {
  appVersion: "app_version",
  androidVersion: "android_version",
  manufacturer: "manufacturer",
} as const;

export type DeviceDimension = keyof typeof deviceColumns;

/**
 * One device contributes exactly one row, so per-dimension device counts can never
 * exceed the number of devices seen in the same window.
 */
export async function queryDeviceDistribution(
  db: D1Database,
  dimension: DeviceDimension,
  sinceSeconds: number,
): Promise<DeviceDistributionRow[]> {
  const column = deviceColumns[dimension];
  const filter =
    dimension === "manufacturer"
      ? " AND trim(manufacturer) != '' AND lower(manufacturer) != 'unknown'"
      : "";
  const { results } = await db
    .prepare(
      `SELECT ${column} AS value, COUNT(*) AS devices, SUM(launch_count) AS launches
       FROM devices
       WHERE last_seen_at >= ?1${filter}
       GROUP BY value
       ORDER BY devices DESC
       LIMIT 50`,
    )
    .bind(sinceSeconds)
    .all<DeviceDistributionRow>();
  return results ?? [];
}

export async function queryDeviceVersions(
  db: D1Database,
  sinceSeconds: number,
): Promise<DeviceDistributionRow[]> {
  const merged = new Map<string, DeviceDistributionRow>();
  for (const row of await queryDeviceDistribution(db, "appVersion", sinceSeconds)) {
    const version = baseVersion(String(row.value ?? ""));
    if (!version) continue;
    const devices = Number(row.devices) || 0;
    const launches = Number(row.launches) || 0;
    const existing = merged.get(version);
    if (existing) {
      existing.devices += devices;
      existing.launches += launches;
    } else {
      merged.set(version, { value: version, devices, launches });
    }
  }
  return [...merged.values()].sort((left, right) => right.devices - left.devices);
}

export async function upsertDevice(
  db: D1Database,
  event: AnalyticsEvent,
  deviceHash: string,
  nowSeconds: number,
): Promise<void> {
  await db
    .prepare(
      `INSERT INTO devices (
        device_hash, first_seen_at, last_seen_at, app_version, version_code,
        android_version, manufacturer, launch_count
      ) VALUES (?1, ?2, ?2, ?3, ?4, ?5, ?6, 1)
      ON CONFLICT(device_hash) DO UPDATE SET
        last_seen_at = excluded.last_seen_at,
        app_version = excluded.app_version,
        version_code = excluded.version_code,
        android_version = excluded.android_version,
        manufacturer = excluded.manufacturer,
        launch_count = devices.launch_count + 1`,
    )
    .bind(
      deviceHash,
      nowSeconds,
      event.appVersion,
      event.versionCode,
      event.androidVersion,
      event.manufacturer ?? "Unknown",
    )
    .run();
}
