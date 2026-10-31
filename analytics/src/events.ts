import { z } from "zod";

const trimmedString = (maxLength: number) => z.string().trim().min(1).max(maxLength);

export const analyticsEventSchema = z
  .object({
    schemaVersion: z.literal(1),
    deviceId: z.string().uuid(),
    androidVersion: trimmedString(32),
    manufacturer: trimmedString(64).optional(),
    model: trimmedString(96).optional(),
    appVersion: trimmedString(64),
    versionCode: z.number().int().min(1).max(2_100_000_000),
    event: z.literal("app_start"),
  })
  .strict();

export type AnalyticsEvent = z.infer<typeof analyticsEventSchema>;
