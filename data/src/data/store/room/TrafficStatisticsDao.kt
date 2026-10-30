/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c)  YumeYucca 2025 - Present
 *
 */

@file:Suppress("UnusedSymbol")

package com.github.yumeyucca.yumebox.data.store.room


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.github.yumeyucca.yumebox.data.model.AppTrafficUsage
import com.github.yumeyucca.yumebox.data.model.DailyTrafficSummary
import com.github.yumeyucca.yumebox.data.model.HourlyTrafficSummary
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for traffic statistics.
 *
 * Declared as an `abstract class` (not an interface) so the accumulate-then-persist logic can live
 * in concrete `@Transaction` methods that call the generated query/insert primitives.
 *
 * Write model — pre-UPSERT accumulate (minSdk 26 ships SQLite < 3.24, so SQLite `UPSERT` `INSERT
 * ... ON CONFLICT ... DO UPDATE` is unavailable): each row is first `UPDATE`d in place (`total +=
 * :delta`); if the `UPDATE` affects 0 rows the row does not exist yet and is `INSERT`ed.
 */
@Dao
abstract class TrafficStatisticsDao {

    // region writes (accumulate)

    /**
     * Records a batch of app + route deltas and applies 90-day retention, all in one transaction.
     *
     * Per (date, app) / (date, app, route): `total_upload += upDelta`, `total_download +=
     * downDelta`, `last_active_at = lastActiveAt`; `app_name`/`route_label` are overwritten only
     * when the incoming value is non-blank, and `package_name` is `COALESCE`d (new value wins only
     * when non-null). Records with both deltas `<= 0` are skipped (the caller already filters, but
     * we keep the guard).
     *
     * @param retentionCutoffMillis rows with `date_millis < retentionCutoffMillis` are deleted
     *   after the writes. Pass `0L` (or any non-positive value) to skip retention — all real day
     *   buckets are positive, so `date_millis < 0` matches nothing.
     */
    @Transaction
    open suspend fun recordBatch(
        hourStartMillis: Long,
        hourlyUploadDelta: Long,
        hourlyDownloadDelta: Long,
        appDeltas: List<AppTrafficDelta>,
        routeDeltas: List<RouteTrafficDelta>,
        retentionCutoffMillis: Long,
    ) {
        if (hourlyUploadDelta > 0L || hourlyDownloadDelta > 0L) {
            val updated =
                accumulateHourly(
                    hourStartMillis = hourStartMillis,
                    uploadDelta = hourlyUploadDelta,
                    downloadDelta = hourlyDownloadDelta,
                )
            if (updated == 0) {
                insertHourly(
                    TrafficHourlyEntity(
                        hourStartMillis = hourStartMillis,
                        totalUpload = hourlyUploadDelta,
                        totalDownload = hourlyDownloadDelta,
                    )
                )
            }
        }

        appDeltas.forEach { delta ->
            if (delta.uploadDelta <= 0L && delta.downloadDelta <= 0L) return@forEach
            val updated =
                accumulateApp(
                    dateMillis = delta.dateMillis,
                    appKey = delta.appKey,
                    uploadDelta = delta.uploadDelta,
                    downloadDelta = delta.downloadDelta,
                    appName = delta.appName,
                    packageName = delta.packageName,
                    lastActiveAt = delta.lastActiveAt,
                )
            if (updated == 0) {
                insertApp(
                    AppTrafficDailyEntity(
                        dateMillis = delta.dateMillis,
                        appKey = delta.appKey,
                        packageName = delta.packageName,
                        appName = delta.appName,
                        totalUpload = delta.uploadDelta,
                        totalDownload = delta.downloadDelta,
                        lastActiveAt = delta.lastActiveAt,
                    )
                )
            }
        }

        routeDeltas.forEach { delta ->
            if (delta.uploadDelta <= 0L && delta.downloadDelta <= 0L) return@forEach
            val updated =
                accumulateRoute(
                    dateMillis = delta.dateMillis,
                    appKey = delta.appKey,
                    routeKey = delta.routeKey,
                    uploadDelta = delta.uploadDelta,
                    downloadDelta = delta.downloadDelta,
                    routeLabel = delta.routeLabel,
                    lastActiveAt = delta.lastActiveAt,
                )
            if (updated == 0) {
                insertRoute(
                    RouteTrafficDailyEntity(
                        dateMillis = delta.dateMillis,
                        appKey = delta.appKey,
                        routeKey = delta.routeKey,
                        routeLabel = delta.routeLabel,
                        totalUpload = delta.uploadDelta,
                        totalDownload = delta.downloadDelta,
                        lastActiveAt = delta.lastActiveAt,
                    )
                )
            }
        }

        deleteAppOlderThan(retentionCutoffMillis)
        deleteRouteOlderThan(retentionCutoffMillis)
        deleteHourlyOlderThan(retentionCutoffMillis)
    }

    @Query(
        """
        UPDATE traffic_hourly
        SET total_upload = total_upload + :uploadDelta,
            total_download = total_download + :downloadDelta
        WHERE hour_start_millis = :hourStartMillis
        """
    )
    protected abstract suspend fun accumulateHourly(
        hourStartMillis: Long,
        uploadDelta: Long,
        downloadDelta: Long,
    ): Int

    @Insert
    protected abstract suspend fun insertHourly(entity: TrafficHourlyEntity)

    @Query(
        """
        UPDATE app_traffic_daily
        SET total_upload = total_upload + :uploadDelta,
            total_download = total_download + :downloadDelta,
            last_active_at = :lastActiveAt,
            app_name = CASE WHEN trim(:appName) <> '' THEN :appName ELSE app_name END,
            package_name = COALESCE(:packageName, package_name)
        WHERE date_millis = :dateMillis AND app_key = :appKey
        """
    )
    protected abstract suspend fun accumulateApp(
        dateMillis: Long,
        appKey: String,
        uploadDelta: Long,
        downloadDelta: Long,
        appName: String,
        packageName: String?,
        lastActiveAt: Long,
    ): Int

    @Insert
    protected abstract suspend fun insertApp(entity: AppTrafficDailyEntity)

    @Query(
        """
        UPDATE route_traffic_daily
        SET total_upload = total_upload + :uploadDelta,
            total_download = total_download + :downloadDelta,
            last_active_at = :lastActiveAt,
            route_label = CASE WHEN trim(:routeLabel) <> '' THEN :routeLabel ELSE route_label END
        WHERE date_millis = :dateMillis AND app_key = :appKey AND route_key = :routeKey
        """
    )
    protected abstract suspend fun accumulateRoute(
        dateMillis: Long,
        appKey: String,
        routeKey: String,
        uploadDelta: Long,
        downloadDelta: Long,
        routeLabel: String,
        lastActiveAt: Long,
    ): Int

    @Insert
    protected abstract suspend fun insertRoute(entity: RouteTrafficDailyEntity)

    // endregion

    // region reads (aggregation)

    /**
     * Reactive per-app aggregation over `[cutoffMillis, +inf)`, summed and sorted.
     *
     * The unattributed bucket (`app_key = 'system:unattributed'`,
     * [com.github.yumeyucca.yumebox.data.model.TrafficStatisticsBuckets.UNATTRIBUTED_APP_KEY])
     * always sorts LAST regardless of its total; every other app is ordered by total bytes
     * descending. `app_name`/`package_name` are taken from the most-recent row via the SQLite
     * single-`max()` bare-column rule (`MAX(last_active_at)` picks the winning row's bare columns).
     */
    @Query(
        """
        SELECT app_key AS appKey,
               package_name AS packageName,
               app_name AS appName,
               SUM(total_upload) AS totalUpload,
               SUM(total_download) AS totalDownload,
               MAX(last_active_at) AS lastActiveAt
        FROM app_traffic_daily
        WHERE date_millis >= :cutoffMillis
        GROUP BY app_key
        ORDER BY (app_key = 'system:unattributed') ASC,
                 (SUM(total_upload) + SUM(total_download)) DESC
        """
    )
    abstract fun getAppUsagesFlow(cutoffMillis: Long): Flow<List<AppTrafficUsage>>

    /**
     * Daily totals over `[cutoffMillis, +inf)`, one row per day, oldest first. Used by the week bar
     * chart (screen-time style).
     */
    @Query(
        """
        SELECT date_millis AS dateMillis,
               SUM(total_upload) AS totalUpload,
               SUM(total_download) AS totalDownload
        FROM app_traffic_daily
        WHERE date_millis >= :cutoffMillis
        GROUP BY date_millis
        ORDER BY date_millis ASC
        """
    )
    abstract fun getDailyTotalsFlow(cutoffMillis: Long): Flow<List<DailyTrafficSummary>>

    @Query(
        """
        SELECT hour_start_millis AS hourStartMillis,
               total_upload AS totalUpload,
               total_download AS totalDownload
        FROM traffic_hourly
        WHERE hour_start_millis >= :cutoffMillis
        ORDER BY hour_start_millis ASC
        """
    )
    abstract fun getHourlyTotalsFlow(cutoffMillis: Long): Flow<List<HourlyTrafficSummary>>

    // endregion

    // region retention & clear

    @Query("DELETE FROM app_traffic_daily WHERE date_millis < :cutoffMillis")
    protected abstract suspend fun deleteAppOlderThan(cutoffMillis: Long): Int

    @Query("DELETE FROM route_traffic_daily WHERE date_millis < :cutoffMillis")
    protected abstract suspend fun deleteRouteOlderThan(cutoffMillis: Long): Int

    @Query("DELETE FROM traffic_hourly WHERE hour_start_millis < :cutoffMillis")
    protected abstract suspend fun deleteHourlyOlderThan(cutoffMillis: Long): Int

    @Transaction
    open suspend fun clearAll() {
        clearAllApp()
        clearAllRoute()
        clearAllHourly()
    }

    @Query("DELETE FROM app_traffic_daily")
    protected abstract suspend fun clearAllApp()

    @Query("DELETE FROM route_traffic_daily")
    protected abstract suspend fun clearAllRoute()

    @Query("DELETE FROM traffic_hourly")
    protected abstract suspend fun clearAllHourly()

    // endregion
}
