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

@file:Suppress("FunctionName", "UnusedVariable")

package com.github.yumeyucca.yumebox.screen.traffic

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.github.yumeyucca.yumebox.common.util.ByteFormatter
import com.github.yumeyucca.yumebox.data.controller.AppIdentityResolver
import com.github.yumeyucca.yumebox.data.model.AppTrafficUsage
import com.github.yumeyucca.yumebox.data.model.StatisticsTimeRange
import com.github.yumeyucca.yumebox.data.model.TrafficStatisticsBuckets
import com.github.yumeyucca.yumebox.feature.meta.presentation.viewmodel.TrafficStatisticsUiState
import com.github.yumeyucca.yumebox.feature.meta.presentation.viewmodel.TrafficStatisticsViewModel
import com.github.yumeyucca.yumebox.feature.meta.presentation.viewmodel.TrafficTotals
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.component.AppCard
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import org.koin.androidx.compose.koinViewModel
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.text.Collator

private enum class AppTrafficSortMode {
    NAME,
    USAGE,
}

/** Localized label, resolved per read so a locale switch is visible without touching the enum. */
private val AppTrafficSortMode.label: String
    get() =
        when (this) {
            AppTrafficSortMode.NAME -> YumeTxt.TrafficStatistics.Metric.SortByName
            AppTrafficSortMode.USAGE -> YumeTxt.TrafficStatistics.Metric.SortByUsage
        }

/** A non-zero share keeps a sliver of fill, so a tiny usage never reads as "nothing here". */
private const val MinTrafficBarFraction = 0.02f

/**
 * Everything today and this week differ in — wording and chart geometry — so the differences live
 * in one place instead of scattered boolean ternaries.
 */
private data class TrafficRangePresentation(
    val appsSectionTitle: String,
    val summaryCaption: String,
    val chartHeight: Dp,
    val barWidth: Dp,
    val isToday: Boolean,
)

@Composable
private fun trafficRangePresentation(range: StatisticsTimeRange): TrafficRangePresentation {
    val componentSizes = AppTheme.sizes
    val today = range == StatisticsTimeRange.TODAY
    return TrafficRangePresentation(
        appsSectionTitle =
            if (today) {
                YumeTxt.TrafficStatistics.Section.TodayApps
            } else {
                YumeTxt.TrafficStatistics.Section.WeekApps
            },
        summaryCaption =
            if (today) {
                YumeTxt.TrafficStatistics.Summary.TodayTraffic
            } else {
                YumeTxt.TrafficStatistics.Summary.WeekTraffic
            },
        chartHeight =
            if (today) componentSizes.trafficChartHeight else componentSizes.trafficBarChartHeight,
        barWidth = if (today) UiDp.dp8 else componentSizes.trafficBarWidth,
        isToday = today,
    )
}

/**
 * Screen-time-style traffic statistics with day/week charts and one app-usage list. Large TopBar
 * title is left as-is.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrafficStatisticsScreen() {
    val viewModel = koinViewModel<TrafficStatisticsViewModel>()
    val scrollBehavior = MiuixScrollBehavior()
    val spacing = AppTheme.spacing

    val uiState by viewModel.uiState.collectAsState()
    var appSortMode by rememberSaveable { mutableStateOf(AppTrafficSortMode.USAGE) }
    val appNameCollator = remember { Collator.getInstance() }
    val displayedApps =
        remember(uiState.topApps, appSortMode, appNameCollator) {
            when (appSortMode) {
                AppTrafficSortMode.NAME ->
                    uiState.topApps.sortedWith { left, right ->
                        appNameCollator.compare(left.appName, right.appName)
                    }

                AppTrafficSortMode.USAGE ->
                    uiState.topApps.sortedByDescending(AppTrafficUsage::totalBytes)
            }
        }
    val timeRanges = StatisticsTimeRange.entries
    val selectedTabIndex = uiState.selectedTimeRange.ordinal
    val activeSummary = uiState.summary
    val presentation = trafficRangePresentation(uiState.selectedTimeRange)
    val chartAverage = uiState.chartAverage.takeIf { !presentation.isToday && it > 0L }

    GlassScaffold(
        topBar = {
            TopBar(
                title = YumeTxt.TrafficStatistics.Title,
                scrollBehavior = scrollBehavior,
            )
        }
    ) { innerPadding ->

        StandalonePageList(
            scrollBehavior = scrollBehavior,
            scaffoldPadding = innerPadding,
        ) {
            stickySegmentedTabs(
                key = "__time_range_tabs__",
                tabs = timeRanges.map { it.label },
                selectedTabIndex = selectedTabIndex,
                onTabSelected = { index -> timeRanges.getOrNull(index)?.let(viewModel::setTimeRange) },
                tabRowPadding = spacing.screenHorizontal,
            )

            item {
                Spacer(modifier = Modifier.height(spacing.space12))
                TrafficSummaryCard(
                    state = uiState,
                    presentation = presentation,
                    chartAverage = chartAverage,
                )
            }

            item {
                Spacer(modifier = Modifier.height(spacing.space12))
                TrafficAppsCard(
                    presentation = presentation,
                    appSortMode = appSortMode,
                    apps = displayedApps,
                    onSortModeChange = { appSortMode = it },
                )
            }
        }
    }
}

/** Totals plus the chart card; every value it draws is already decided by the caller. */
@Composable
private fun TrafficSummaryCard(
    state: TrafficStatisticsUiState,
    presentation: TrafficRangePresentation,
    chartAverage: Long?,
) {
    val spacing = AppTheme.spacing
    AppCard(insideMargin = PaddingValues(spacing.space16)) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.space16)) {
            if (presentation.isToday) {
                TrafficTodaySummary(
                    caption = presentation.summaryCaption,
                    totals = state.summary,
                )
            }

            TrafficBarChart(
                items = state.chartBars,
                animationKey = state.selectedTimeRange,
                averageValue = chartAverage,
                averageLabel = YumeTxt.TrafficStatistics.Summary.Average.takeIf { chartAverage != null },
                showSelectionTip = !presentation.isToday,
                tipTitleFor = { bar ->
                    YumeTxt.TrafficStatistics.Summary.DayTrafficTip.format(bar.label)
                },
                chartHeight = presentation.chartHeight,
                barWidth = presentation.barWidth,
            )
        }
    }
}

/** The per-app list with its sort dropdown and the empty line it falls back to. */
@Composable
private fun TrafficAppsCard(
    presentation: TrafficRangePresentation,
    appSortMode: AppTrafficSortMode,
    apps: List<AppTrafficUsage>,
    onSortModeChange: (AppTrafficSortMode) -> Unit,
) {
    val spacing = AppTheme.spacing
    AppCard(insideMargin = PaddingValues(spacing.space0)) {
        GlassDropdownPreference(
            options = AppTrafficSortMode.entries.map { GlassDropdownOption(it, it.label) },
            selected = appSortMode,
            title = presentation.appsSectionTitle,
            maxHeight = UiDp.dp200,
            onSelected = onSortModeChange,
        )

        if (apps.isEmpty()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.space16, vertical = spacing.space24),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = YumeTxt.TrafficStatistics.Section.EmptyApps,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        } else {
            TrafficAppsContent(apps)
        }
    }
}

/**
 * The today-header above the chart: one big total plus a download/upload/delta caption.
 *
 * Kept separate from [TrafficStatisticsScreen] because it renders nothing the screen decides — it
 * only formats [totals] against [caption].
 */
@Composable
private fun TrafficTodaySummary(caption: String, totals: TrafficTotals) {
    val spacing = AppTheme.spacing
    val download = ByteFormatter.format(totals.totalDownload)
    val upload = ByteFormatter.format(totals.totalUpload)
    Column(
        modifier = Modifier.height(UiDp.dp56),
        verticalArrangement = Arrangement.spacedBy(spacing.space6),
    ) {
        Text(
            text = ByteFormatter.format(totals.total),
            style = MiuixTheme.textStyles.title3,
            color = MiuixTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "$caption · ${YumeTxt.TrafficStatistics.Metric.Download} $download · ${YumeTxt.TrafficStatistics.Metric.Upload} $upload",
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}

/**
 * The per-app usage list below the sort dropdown.
 *
 * The bar length is relative to the busiest *attributed* app: unattributed and unknown buckets carry
 * the aggregate of everything the resolver could not name, which would otherwise pin every real app
 * to a sliver of the track.
 */
@Composable
private fun TrafficAppsContent(apps: List<AppTrafficUsage>) {
    val spacing = AppTheme.spacing
    val componentSizes = AppTheme.sizes
    val maxBytes =
        apps
            .filterNot {
                it.appKey == TrafficStatisticsBuckets.UNATTRIBUTED_APP_KEY ||
                    it.appKey == AppIdentityResolver.UNKNOWN_APP_KEY
            }
            .maxOfOrNull(AppTrafficUsage::totalBytes)
            ?.coerceAtLeast(1L) ?: 1L
    apps.forEachIndexed { index, usage ->
        AppTrafficProgressRow(
            usage = usage,
            progress = (usage.totalBytes.toFloat() / maxBytes.toFloat()).coerceIn(0f, 1f),
        )
        if (index < apps.lastIndex) {
            HorizontalDivider(
                modifier = Modifier.padding(start = UiDp.dp72, end = spacing.space16),
                thickness = componentSizes.thinDividerThickness,
                color = MiuixTheme.colorScheme.outline.copy(alpha = AppTheme.opacity.outline),
            )
        }
    }
}

@Composable
private fun AppTrafficProgressRow(
    usage: AppTrafficUsage,
    progress: Float,
) {
    val spacing = AppTheme.spacing
    val componentSizes = AppTheme.sizes
    val radii = AppTheme.radii

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.space16, vertical = spacing.space12),
        horizontalArrangement = Arrangement.spacedBy(spacing.space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIconBadge(
            packageName = usage.packageName,
            appName = usage.appName,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.space6),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = usage.appName,
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text(
                    text = ByteFormatter.format(usage.totalBytes),
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            // Track + fill (screen-time progress bar)
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(UiDp.dp6)
                        .clip(RoundedCornerShape(radii.full))
                        .background(MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(progress.coerceAtLeast(MinTrafficBarFraction))
                            .height(UiDp.dp6)
                            .clip(RoundedCornerShape(radii.full))
                            .background(MiuixTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Composable
private fun AppIconBadge(packageName: String?, appName: String) {
    val componentSizes = AppTheme.sizes
    val opacity = AppTheme.opacity
    val radii = AppTheme.radii

    // Decode at the size the badge actually draws, so the icon is sharp at every density.
    val iconPx = with(LocalDensity.current) { componentSizes.iconBadgeMedium.roundToPx() }
    val bitmap = rememberAppIconBitmap(packageName, iconPx)
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = appName,
            modifier =
                Modifier
                    .size(componentSizes.iconBadgeMedium)
                    .clip(RoundedCornerShape(radii.radius12)),
        )
        return
    }

    Box(
        modifier =
            Modifier
                .size(componentSizes.iconBadgeMedium)
                .clip(RoundedCornerShape(radii.radius12))
                .background(MiuixTheme.colorScheme.primary.copy(alpha = opacity.subtleStrong)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = appName.take(1).ifBlank { "?" },
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
    }
}
