import type { AnalyticsEvent } from "../events";

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
