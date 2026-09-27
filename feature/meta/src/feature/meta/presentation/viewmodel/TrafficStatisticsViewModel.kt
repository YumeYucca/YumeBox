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

@file:Suppress("FunctionName")

package com.github.yumeyucca.yumebox.feature.meta.presentation.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumeyucca.yumebox.data.controller.AppIdentityResolver
import com.github.yumeyucca.yumebox.data.model.*
import com.github.yumeyucca.yumebox.data.store.TrafficStatisticsStore
import com.github.yumeyucca.yumebox.presentation.component.BarChartItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import java.util.*

class TrafficStatisticsViewModel(private val trafficStatisticsStore: TrafficStatisticsStore) :
    ViewModel() {
    private val selectedTimeRange = MutableStateFlow(StatisticsTimeRange.TODAY)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TrafficStatisticsUiState> =
        selectedTimeRange
            .flatMapLatest { range ->
                combine(
                    trafficStatisticsStore.getAppUsagesFlow(range),
                    trafficStatisticsStore.getDailyTotalsFlow(range),
                    trafficStatisticsStore.getTodayHourlyTotalsFlow(),
                ) { topApps, dailyTotals, hourlyTotals ->
                    buildUiState(range, topApps, dailyTotals, hourlyTotals)
                }
            }
            .distinctUntilChanged()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                TrafficStatisticsUiState(),
            )

    fun setTimeRange(range: StatisticsTimeRange) {
        selectedTimeRange.value = range
    }

    fun clearAllStatistics() {
        viewModelScope.launch { trafficStatisticsStore.clearAll() }
    }

    private fun buildUiState(
        range: StatisticsTimeRange,
        topApps: List<AppTrafficUsage>,
        dailyTotals: List<DailyTrafficSummary>,
        hourlyTotals: List<HourlyTrafficSummary>,
    ): TrafficStatisticsUiState {
        val totalUpload = topApps.sumOf(AppTrafficUsage::totalUpload)
        val totalDownload = topApps.sumOf(AppTrafficUsage::totalDownload)
        val summary =
            DailyTrafficSummary(
                dateMillis = range.days.toLong(),
                totalUpload = totalUpload,
                totalDownload = totalDownload,
            )
        val bars =
            when (range) {
                StatisticsTimeRange.TODAY -> buildTodayBars(hourlyTotals)
                StatisticsTimeRange.WEEK -> buildWeekBars(dailyTotals)
            }
        val average =
            if (range == StatisticsTimeRange.WEEK && bars.isNotEmpty()) {
                bars.sumOf(BarChartItem::value) / bars.size
            } else {
                0L
            }

        return TrafficStatisticsUiState(
            selectedTimeRange = range,
            summary = summary,
            topApps = mergeSystemTraffic(topApps),
            chartBars = bars,
            chartAverage = average,
        )
    }

    private fun mergeSystemTraffic(apps: List<AppTrafficUsage>): List<AppTrafficUsage> {
        val systemApps = apps.filter {
            it.appKey == AppIdentityResolver.UNKNOWN_APP_KEY ||
                    it.appKey == TrafficStatisticsBuckets.UNATTRIBUTED_APP_KEY
        }
        if (systemApps.isEmpty()) return apps

        val regularApps = apps.filterNot {
            it.appKey == AppIdentityResolver.UNKNOWN_APP_KEY ||
                    it.appKey == TrafficStatisticsBuckets.UNATTRIBUTED_APP_KEY
        }
        val systemTraffic =
            AppTrafficUsage(
                appKey = TrafficStatisticsBuckets.UNATTRIBUTED_APP_KEY,
                packageName = ANDROID_SYSTEM_PACKAGE,
                appName = YumeTxt.TrafficStatistics.Section.SystemTraffic,
                totalUpload = systemApps.sumOf(AppTrafficUsage::totalUpload),
                totalDownload = systemApps.sumOf(AppTrafficUsage::totalDownload),
                lastActiveAt = systemApps.maxOfOrNull(AppTrafficUsage::lastActiveAt) ?: 0L,
            )
        return regularApps + systemTraffic
    }

    private fun buildTodayBars(hourlyTotals: List<HourlyTrafficSummary>): List<BarChartItem> {
        val totalsByHour = hourlyTotals.associateBy { it.hourStartMillis }
        val start = startOfDay(System.currentTimeMillis())
        val calendar = Calendar.getInstance().apply { timeInMillis = start }
        return (0 until HOURS_PER_DAY).map { hour ->
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            BarChartItem(
                label = if (hour in LABELED_HOURS) "%02d:00".format(hour) else "",
                value = totalsByHour[calendar.timeInMillis]?.total ?: 0L,
            )
        }
    }

    /** Always emit 7 day slots ending today, filling missing days with 0. */
    private fun buildWeekBars(dailyTotals: List<DailyTrafficSummary>): List<BarChartItem> {
        val byDay = dailyTotals.associateBy { it.dateMillis }
        val today =
            Calendar.getInstance().apply { timeInMillis = startOfDay(System.currentTimeMillis()) }
        return (6 downTo 0).map { daysAgo ->
            val day = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
            val dayStart = day.timeInMillis
            val total = byDay[dayStart]?.total ?: 0L
            BarChartItem(
                label = weekdayLabel(dayStart, isToday = daysAgo == 0),
                value = total,
                isHighlighted = daysAgo == 0,
            )
        }
    }

    private fun weekdayLabel(dayStartMillis: Long, isToday: Boolean): String {
        if (isToday) return YumeTxt.TrafficStatistics.Weekday.Today
        val calendar = Calendar.getInstance().apply { timeInMillis = dayStartMillis }
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> YumeTxt.TrafficStatistics.Weekday.Mon
            Calendar.TUESDAY -> YumeTxt.TrafficStatistics.Weekday.Tue
            Calendar.WEDNESDAY -> YumeTxt.TrafficStatistics.Weekday.Wed
            Calendar.THURSDAY -> YumeTxt.TrafficStatistics.Weekday.Thu
            Calendar.FRIDAY -> YumeTxt.TrafficStatistics.Weekday.Fri
            Calendar.SATURDAY -> YumeTxt.TrafficStatistics.Weekday.Sat
            else -> YumeTxt.TrafficStatistics.Weekday.Sun
        }
    }

    private fun startOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    companion object {
        private const val ANDROID_SYSTEM_PACKAGE = "android"
        private const val HOURS_PER_DAY = 24
        private val LABELED_HOURS = setOf(0, 6, 12, 18, 23)
    }
}

data class TrafficStatisticsUiState(
    val selectedTimeRange: StatisticsTimeRange = StatisticsTimeRange.TODAY,
    val summary: DailyTrafficSummary = DailyTrafficSummary.empty,
    val topApps: List<AppTrafficUsage> = emptyList(),
    val chartBars: List<BarChartItem> = emptyList(),
    val chartAverage: Long = 0L,
)
