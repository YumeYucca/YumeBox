import type { Bindings } from "../env";

const DATASET = 'events.analyticsEngine."yumebox_launches"';

export type ActivityRow = {
  date: string;
  launches: number | string;
  activeDevices: number | string;
};

export type DistributionRow = {
  value: string;
  devices: number | string;
  launches: number | string;
};

const dimensionColumns = {
  appVersion: "blob3",
  versionCode: "blob4",
  androidVersion: "blob5",
  manufacturer: "if(blob6 = '', 'Unknown', blob6)",
} as const;

export type DistributionDimension = keyof typeof dimensionColumns;

export function isDistributionDimension(value: string): value is DistributionDimension {
  return value in dimensionColumns;
}

async function queryAnalytics<Row extends Record<string, unknown>>(
  env: Bindings,
  query: string,
): Promise<Row[]> {
  try {
    const result = await env.ANALYTICS_SQL.query<Row>({ query });
    return result.data;
  } catch (error) {
    // A dataset is created by its first write; an empty deployment has nothing to report.
    if (error instanceof Error && /unknown (table|dataset)|does not exist/i.test(error.message)) {
      return [];
    }
    throw error;
  }
}

export function queryActivityTrend(env: Bindings, days: number): Promise<ActivityRow[]> {
  return queryAnalytics<ActivityRow>(
    env,
    `SELECT
      formatDateTime(timestamp, '%Y-%m-%d') AS date,
      SUM(double1) AS launches,
      COUNT(DISTINCT blob2) AS activeDevices
    FROM ${DATASET}
    WHERE timestamp >= NOW() - INTERVAL '${days}' DAY
    GROUP BY date
    ORDER BY date`,
  );
}

export function queryDistribution(
  env: Bindings,
  dimension: DistributionDimension,
  days: number,
): Promise<DistributionRow[]> {
  const column = dimensionColumns[dimension];
  const filter =
    dimension === "manufacturer" ? " AND blob6 != '' AND lower(blob6) != 'unknown'" : "";
  return queryAnalytics<DistributionRow>(
    env,
    `SELECT
      ${column} AS value,
      COUNT(DISTINCT blob2) AS devices,
      SUM(double1) AS launches
    FROM ${DATASET}
    WHERE timestamp >= NOW() - INTERVAL '${days}' DAY${filter}
    GROUP BY value
    ORDER BY devices DESC
    LIMIT 50`,
  );
}

/** `0.6.5.moe.b965f0ea` and `0.6.5-dev` both belong to `0.6.5`. */
export function baseVersion(value: string): string {
  const trimmed = value.trim();
  const match = /^[0-9]+\.[0-9]+\.[0-9]+/.exec(trimmed);
  return match ? match[0] : trimmed;
}
