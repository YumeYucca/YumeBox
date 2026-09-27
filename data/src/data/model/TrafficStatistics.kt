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

package com.github.yumeyucca.yumebox.data.model


import kotlinx.serialization.Serializable

@Serializable
data class DailyTrafficSummary(
    val dateMillis: Long,
    val totalUpload: Long,
    val totalDownload: Long,
) {
    val total: Long
        get() = totalUpload + totalDownload

    companion object {
        val empty = DailyTrafficSummary(0L, 0L, 0L)
    }
}

@Serializable
data class HourlyTrafficSummary(
    val hourStartMillis: Long,
    val totalUpload: Long,
    val totalDownload: Long,
) {
    val total: Long
        get() = totalUpload + totalDownload
}

@Serializable
data class AppTrafficUsage(
    val appKey: String,
    val packageName: String? = null,
    val appName: String,
    val totalUpload: Long = 0L,
    val totalDownload: Long = 0L,
    val lastActiveAt: Long = 0L,
) {
    val totalBytes: Long
        get() = totalUpload + totalDownload
}

data class AppTrafficDeltaRecord(
    val appKey: String,
    val packageName: String? = null,
    val appName: String,
    val uploadDelta: Long,
    val downloadDelta: Long,
    val routeKey: String? = null,
    val routeLabel: String? = null,
)

object TrafficStatisticsBuckets {
    const val UNATTRIBUTED_APP_KEY = "system:unattributed"
    const val UNATTRIBUTED_APP_NAME = "未归属流量"
    const val UNATTRIBUTED_ROUTE_KEY = "route:unattributed"
    const val UNATTRIBUTED_ROUTE_NAME = "未归属线路"

    fun buildUnattributedRecord(uploadDelta: Long, downloadDelta: Long) =
        AppTrafficDeltaRecord(
            appKey = UNATTRIBUTED_APP_KEY,
            packageName = null,
            appName = UNATTRIBUTED_APP_NAME,
            uploadDelta = uploadDelta,
            downloadDelta = downloadDelta,
            routeKey = UNATTRIBUTED_ROUTE_KEY,
            routeLabel = UNATTRIBUTED_ROUTE_NAME,
        )
}

@Serializable
data class AppRouteTrafficUsage(
    val appKey: String,
    val routeKey: String,
    val routeLabel: String,
    val totalUpload: Long = 0L,
    val totalDownload: Long = 0L,
    val lastActiveAt: Long = 0L,
) {
    val totalBytes: Long
        get() = totalUpload + totalDownload
}

@Serializable
data class ConnectionTrafficBaseline(
    val id: String,
    val upload: Long,
    val download: Long,
    val appKey: String,
    val packageName: String? = null,
    val appName: String,
)

enum class StatisticsTimeRange(val days: Int) {
    TODAY(1),
    WEEK(7);

    val label: String
        get() =
            when (this) {
                TODAY -> com.github.yumeyucca.yumebox.core.locale.YumeTxt.TrafficStatistics.TimeRange.Today
                WEEK -> com.github.yumeyucca.yumebox.core.locale.YumeTxt.TrafficStatistics.TimeRange.Week
            }
}
