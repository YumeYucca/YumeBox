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

@file:Suppress("DuplicatedCode")

package com.github.yumeyucca.yumebox.data.store

import com.github.yumeyucca.yumebox.data.model.*
import com.github.yumeyucca.yumebox.data.store.room.AppTrafficDelta
import com.github.yumeyucca.yumebox.data.store.room.RouteTrafficDelta
import com.github.yumeyucca.yumebox.data.store.room.TrafficStatisticsDao
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.flow.Flow
import java.util.*

/**
 * Facade over traffic statistics persistence.
 *
 * Daily per-app / per-route aggregation lives in Room (see [TrafficStatisticsDao]); each record is
 * a row-level accumulate rather than a whole-history JSON rewrite. The only state that stays in
 * MMKV is the collector's resume baseline (last total upload/download + profile id), kept
 * synchronous.
 */
class TrafficStatisticsStore(
    private val mmkv: MMKV,
    private val dao: TrafficStatisticsDao,
    private val now: () -> Long = { System.currentTimeMillis() },
) {
    init {
        // One-time, idempotent cleanup: the daily-summary aggregation now lives in Room, so drop
        // the
        // legacy MMKV blobs (current v2/v3 keys + legacy v1 keys). Baseline scalars are
        // intentionally
        // left in MMKV.
        mmkv.removeValueForKey(KEY_DAILY_APP_SUMMARIES)
        mmkv.removeValueForKey(KEY_DAILY_ROUTE_SUMMARIES)
        mmkv.removeValueForKey(KEY_STATS_SCHEMA_VERSION)
        mmkv.removeValueForKey(LEGACY_KEY_DAILY_SUMMARIES)
        mmkv.removeValueForKey(LEGACY_KEY_PROFILE_USAGES)
    }

    // region writes (Room row-level accumulate)

    suspend fun recordAppTrafficBatch(timestamp: Long, records: List<AppTrafficDeltaRecord>) {
        if (records.isEmpty()) return

        val dayKey = startOfDay(timestamp)
        val appDeltas = ArrayList<AppTrafficDelta>(records.size)
        val routeDeltas = ArrayList<RouteTrafficDelta>(records.size)

        records.forEach { record ->
            if (record.uploadDelta <= 0L && record.downloadDelta <= 0L) return@forEach

            appDeltas +=
                AppTrafficDelta(
                    dateMillis = dayKey,
                    appKey = record.appKey,
                    packageName = record.packageName,
                    appName = record.appName,
                    uploadDelta = record.uploadDelta,
                    downloadDelta = record.downloadDelta,
                    lastActiveAt = timestamp,
                )
            routeDeltas +=
                RouteTrafficDelta(
                    dateMillis = dayKey,
                    appKey = record.appKey,
                    routeKey =
                        record.routeKey?.takeIf(String::isNotBlank)
                            ?: TrafficStatisticsBuckets.UNATTRIBUTED_ROUTE_KEY,
                    routeLabel =
                        record.routeLabel?.takeIf(String::isNotBlank)
                            ?: TrafficStatisticsBuckets.UNATTRIBUTED_ROUTE_NAME,
                    uploadDelta = record.uploadDelta,
                    downloadDelta = record.downloadDelta,
                    lastActiveAt = timestamp,
                )
        }

        if (appDeltas.isEmpty()) return

        dao.recordBatch(
            hourStartMillis = startOfHour(timestamp),
            hourlyUploadDelta = appDeltas.sumOf(AppTrafficDelta::uploadDelta),
            hourlyDownloadDelta = appDeltas.sumOf(AppTrafficDelta::downloadDelta),
            appDeltas = appDeltas,
            routeDeltas = routeDeltas,
            retentionCutoffMillis = now() - (MAX_APP_DAYS_TO_KEEP * DAY_MS),
        )
    }

    // endregion

    // region reads (SQL aggregation)

    fun getAppUsagesFlow(range: StatisticsTimeRange): Flow<List<AppTrafficUsage>> =
        dao.getAppUsagesFlow(rangeCutoff(range))

    fun getDailyTotalsFlow(range: StatisticsTimeRange): Flow<List<DailyTrafficSummary>> =
        dao.getDailyTotalsFlow(rangeCutoff(range))

    fun getTodayHourlyTotalsFlow(): Flow<List<HourlyTrafficSummary>> =
        dao.getHourlyTotalsFlow(startOfDay(now()))

    // endregion

    // region clear

    suspend fun clearAll() {
        dao.clearAll()
        mmkv.removeValueForKey(KEY_LAST_TRAFFIC_UPLOAD)
        mmkv.removeValueForKey(KEY_LAST_TRAFFIC_DOWNLOAD)
        mmkv.removeValueForKey(KEY_LAST_PROFILE_ID)
    }

    // endregion

    // region resume baseline (synchronous MMKV scalars)

    fun getLastTrafficUpload(): Long =
        mmkv.decodeLong(KEY_LAST_TRAFFIC_UPLOAD, NO_PERSISTED_TRAFFIC)

    fun getLastTrafficDownload(): Long =
        mmkv.decodeLong(KEY_LAST_TRAFFIC_DOWNLOAD, NO_PERSISTED_TRAFFIC)

    fun getLastProfileId(): String? = mmkv.decodeString(KEY_LAST_PROFILE_ID)

    /** Persists the resume baseline scalars. Room/MMKV writes are already immediate. */
    fun setLastTraffic(
        upload: Long,
        download: Long,
        profileId: String?,
    ) {
        mmkv.encode(KEY_LAST_TRAFFIC_UPLOAD, upload)
        mmkv.encode(KEY_LAST_TRAFFIC_DOWNLOAD, download)
        if (profileId.isNullOrBlank()) {
            mmkv.removeValueForKey(KEY_LAST_PROFILE_ID)
        } else {
            mmkv.encode(KEY_LAST_PROFILE_ID, profileId)
        }
    }

    // endregion

    // region helpers

    private fun startOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun startOfHour(timestamp: Long): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun rangeCutoff(range: StatisticsTimeRange): Long =
        startOfDay(now()) - ((range.days - 1) * DAY_MS)

    // endregion

    companion object {
        private const val KEY_DAILY_APP_SUMMARIES = "daily_app_summaries_v2"
        private const val KEY_DAILY_ROUTE_SUMMARIES = "daily_route_summaries_v2"
        private const val KEY_LAST_TRAFFIC_UPLOAD = "last_traffic_upload_v2"
        private const val KEY_LAST_TRAFFIC_DOWNLOAD = "last_traffic_download_v2"
        private const val KEY_LAST_PROFILE_ID = "last_profile_id_v2"
        private const val KEY_STATS_SCHEMA_VERSION = "traffic_stats_schema_version"
        private const val LEGACY_KEY_DAILY_SUMMARIES = "daily_summaries"
        private const val LEGACY_KEY_PROFILE_USAGES = "profile_usages"
        private const val DAY_MS = 24 * 60 * 60 * 1000L
        private const val MAX_APP_DAYS_TO_KEEP = 90
        private const val NO_PERSISTED_TRAFFIC = -1L
    }
}
