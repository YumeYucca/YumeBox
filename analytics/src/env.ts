export interface Bindings {
  ASSETS: Fetcher;
  DB: D1Database;
  ANALYTICS: AnalyticsEngineDataset;
  ANALYTICS_SQL: AnalyticsSQLBinding;
  DEVICE_HASH_SECRET: string;
  STATS_API_TOKEN: string;
}
