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

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.core.graphics.drawable.toBitmap
import com.github.yumeyucca.yumebox.common.util.formatBytes
import com.github.yumeyucca.yumebox.common.util.toast
import com.github.yumeyucca.yumebox.data.controller.AppIdentityResolver
import com.github.yumeyucca.yumebox.data.model.AppTrafficUsage
import com.github.yumeyucca.yumebox.data.model.StatisticsTimeRange
import com.github.yumeyucca.yumebox.data.model.TrafficStatisticsBuckets
import com.github.yumeyucca.yumebox.feature.meta.presentation.component.TabRowWithContour
import com.github.yumeyucca.yumebox.feature.meta.presentation.viewmodel.TrafficStatisticsViewModel
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.component.AppCard
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.text.Collator

/**
 * Screen-time-style traffic statistics with day/week charts and one app-usage list. Large TopBar
 * title is left as-is.
 */
@Composable
fun TrafficStatisticsScreen() {
    val viewModel = koinViewModel<TrafficStatisticsViewModel>()
    val scrollBehavior = MiuixScrollBehavior()
    val context = LocalContext.current
    val spacing = AppTheme.spacing
    val componentSizes = AppTheme.sizes

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
    val selectedTabIndex = timeRanges.indexOf(uiState.selectedTimeRange).coerceAtLeast(0)
    val activeSummary = uiState.summary
    val isToday = uiState.selectedTimeRange == StatisticsTimeRange.TODAY
    val appsSectionTitle =
        if (isToday) {
            YumeTxt.TrafficStatistics.Section.TodayApps
        } else {
            YumeTxt.TrafficStatistics.Section.WeekApps
        }
    val summaryCaption =
        if (isToday) {
            YumeTxt.TrafficStatistics.Summary.TodayTraffic
        } else {
            YumeTxt.TrafficStatistics.Summary.WeekTraffic
        }

    Scaffold(
        topBar = {
            TopBar(
                title = YumeTxt.TrafficStatistics.Title,
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.clearAllStatistics()
                            context.toast(YumeTxt.TrafficStatistics.Action.ClearSuccess)
                        }
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Delete,
                            contentDescription = YumeTxt.TrafficStatistics.Action.Clear,
                            tint = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        }
    ) { innerPadding ->
        val mainLikePadding = rememberStandalonePageMainPadding()
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = combinePaddingValues(innerPadding, mainLikePadding),
        ) {
            item {
                TabRowWithContour(
                    tabs = timeRanges.map { it.label },
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = { index ->
                        timeRanges.getOrNull(index)?.let(viewModel::setTimeRange)
                    },
                    modifier = Modifier.padding(horizontal = spacing.screenHorizontal),
                )
            }

            item {
                Spacer(modifier = Modifier.height(spacing.space12))
                // Summary + chart card
                AppCard(insideMargin = PaddingValues(spacing.space16)) {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.space16)) {
                        if (isToday) {
                            Column(
                                modifier = Modifier.height(UiDp.dp56),
                                verticalArrangement = Arrangement.spacedBy(spacing.space6),
                            ) {
                                Text(
                                    text = formatBytes(activeSummary.total),
                                    style = MiuixTheme.textStyles.title3,
                                    color = MiuixTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text =
                                        "$summaryCaption · ${YumeTxt.TrafficStatistics.Metric.Download} ${
                                            formatBytes(
                                                activeSummary.totalDownload
                                            )
                                        } · ${YumeTxt.TrafficStatistics.Metric.Upload} ${formatBytes(activeSummary.totalUpload)}",
                                    style = MiuixTheme.textStyles.footnote1,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                )
                            }
                        }

                        TrafficBarChart(
                            items = uiState.chartBars,
                            animationKey = uiState.selectedTimeRange,
                            averageValue = uiState.chartAverage.takeIf { !isToday && it > 0L },
                            averageLabel =
                                YumeTxt.TrafficStatistics.Summary.Average.takeIf {
                                    !isToday && uiState.chartAverage > 0L
                                },
                            showSelectionTip = !isToday,
                            tipTitleFor = { bar ->
                                YumeTxt.TrafficStatistics.Summary.DayTrafficTip.format(bar.label)
                            },
                            chartHeight =
                                if (isToday) {
                                    componentSizes.trafficChartHeight
                                } else {
                                    componentSizes.trafficBarChartHeight
                                },
                            barWidth =
                                if (isToday) {
                                    UiDp.dp8
                                } else {
                                    componentSizes.trafficBarWidth
                                },
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(spacing.space12))
                AppCard(insideMargin = PaddingValues(spacing.space0)) {
                    OverlayDropdownPreference(
                        items =
                            listOf(
                                YumeTxt.TrafficStatistics.Metric.SortByName,
                                YumeTxt.TrafficStatistics.Metric.SortByUsage,
                            ),
                        selectedIndex = appSortMode.ordinal,
                        title = appsSectionTitle,
                        maxHeight = UiDp.dp200,
                        onSelectedIndexChange = { index ->
                            AppTrafficSortMode.entries.getOrNull(index)?.let { appSortMode = it }
                        },
                    )

                    if (displayedApps.isEmpty()) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = spacing.space16,
                                        vertical = spacing.space24,
                                    ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = YumeTxt.TrafficStatistics.Section.EmptyApps,
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    } else {
                        val maxBytes =
                            displayedApps
                                .filterNot {
                                    it.appKey == TrafficStatisticsBuckets.UNATTRIBUTED_APP_KEY ||
                                            it.appKey == AppIdentityResolver.UNKNOWN_APP_KEY
                                }
                                .maxOfOrNull(AppTrafficUsage::totalBytes)
                                ?.coerceAtLeast(1L) ?: 1L
                        displayedApps.forEachIndexed { index, usage ->
                            AppTrafficProgressRow(
                                context = context,
                                usage = usage,
                                progress =
                                    (usage.totalBytes.toFloat() / maxBytes.toFloat()).coerceIn(
                                        0f,
                                        1f,
                                    ),
                            )
                            if (index < displayedApps.lastIndex) {
                                HorizontalDivider(
                                    modifier =
                                        Modifier.padding(
                                            start = UiDp.dp72,
                                            end = spacing.space16,
                                        ),
                                    thickness = componentSizes.thinDividerThickness,
                                    color =
                                        MiuixTheme.colorScheme.outline.copy(
                                            alpha = AppTheme.opacity.outline
                                        ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class AppTrafficSortMode {
    NAME,
    USAGE,
}

@Composable
private fun AppTrafficProgressRow(
    context: Context,
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
            context = context,
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
                    text = formatBytes(usage.totalBytes),
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
                            .fillMaxWidth(progress.coerceAtLeast(0.02f))
                            .height(UiDp.dp6)
                            .clip(RoundedCornerShape(radii.full))
                            .background(MiuixTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Composable
private fun AppIconBadge(context: Context, packageName: String?, appName: String) {
    val componentSizes = AppTheme.sizes
    val opacity = AppTheme.opacity
    val radii = AppTheme.radii

    val iconBitmap by
    produceState<ImageBitmap?>(initialValue = null, key1 = packageName) {
        value =
            withContext(Dispatchers.IO) {
                packageName
                    ?.takeIf { it.isNotBlank() }
                    ?.let { target ->
                        runCatching {
                            context.packageManager
                                .getApplicationIcon(target)
                                .toBitmap(width = 84, height = 84)
                                .asImageBitmap()
                        }
                            .getOrNull()
                    }
            }
    }

    val bitmap = iconBitmap
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
