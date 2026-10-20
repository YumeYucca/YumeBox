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

@file:Suppress("UnusedSymbol", "ConstPropertyName", "FunctionName")

package com.github.yumeyucca.yumebox.screen.moe


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.yumeyucca.yumebox.presentation.theme.*
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.util.*
import kotlin.math.abs

private val moeSpacing = Spacing()
private val moeRadii = Radii()
private val moeSizes = Sizes()
private val moeOpacity = Opacity()

internal object MoeUi {
    object Shape {
        val heroRadius = moeSpacing.space28
        val launchButton = RoundedCornerShape(moeRadii.full)
        val launchButtonRadius = moeRadii.full
    }

    object Sidebar {
        const val fraction = 0.25f
        val contentOverlap = moeSpacing.space28
        val innerHorizontalPadding = moeSpacing.space12
        val topInset = moeSizes.heroStartButtonSize - moeSpacing.space2
        val bottomInset = moeSpacing.space32 + moeSpacing.space6
        val timeGap = moeSpacing.space12
        val timeTopGap = moeSpacing.space18
        val dividerWidth = moeSizes.settingsIconGlyphSize
        val dividerHeight = moeSpacing.space2
        val statusTopGap = moeSpacing.space18
        val statusGap = moeSpacing.space8
        val batteryWidth = moeSpacing.space32 + moeSpacing.space4
        val batteryHeight = moeSpacing.space16
        val batteryKnobWidth = moeSpacing.space3
        val iconSpacing = moeSpacing.space14
        val iconHitSize = UiDp.dp44
        val iconSize = UiDp.dp30
        val digitLetterSpacing = 0.sp
        const val timeAlpha = 0.96f
        const val dividerAlpha = 0.62f
        const val statusAlpha = 0.82f
        const val iconAlpha = 0.88f
        val collapsedVisibleWidth = moeSpacing.space8
    }

    object Hero {
        const val heightFraction = 0.63f
        val containerHorizontalInset = moeSpacing.space12
        val contentHorizontalInset = moeSpacing.space12
        val trafficRowGap = moeSpacing.space28
        val trafficBottomInset = moeSpacing.space12
        val runtimeInfoTopGap = moeSpacing.space16
        val delayWidth = moeSizes.nodeDelayColumnWidth
        val belowHeroTopGap = moeSpacing.space14
        val belowHeroContentGap = moeSpacing.space12
        val launchTopGap = moeSpacing.space24
        val infoPlaceholderAlpha = moeOpacity.surfaceSoft
        val infoRowMinHeight = moeSpacing.space24
        val infoPlaceholderNodeWidth = moeSizes.homeIdleTopPadding - moeSpacing.space8
    }

    object Button {
        val bottomInset = moeSpacing.space16
        val height = UiDp.dp46
        val circleSize = UiDp.dp46
        val controlGap = moeSpacing.space10
        val horizontalPadding = moeSpacing.space20
        val verticalPadding = moeSpacing.space8
        val iconSize = UiDp.dp20
        val shadowElevation = moeSpacing.space3
        val borderWidth = UiDp.dp0_5
        const val pressedScale = 0.94f
    }

    object Quote {
        val contentGap = moeSpacing.space12
        val eyebrowSize = 17.sp
        val textSize = 23.sp
        val lineHeight = 31.sp
        const val eyebrowAlpha = 0.56f
    }

    object Traffic {
        val itemGap = moeSpacing.space6
        val labelBottomPadding = moeSpacing.space3
    }

    object Info {
        val trailingPadding = moeSpacing.space16
        val blockGap = moeSpacing.space8
    }
}

@Composable
internal fun MoeSidebarRail(
    topValue: String,
    bottomValue: String,
    batteryPercent: Int?,
    icons: List<MoeSidebarIconItem>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(MoeUi.Sidebar.topInset))

        MoeSidebarValueStack(
            topValue = topValue,
            bottomValue = bottomValue,
            modifier = Modifier.padding(top = MoeUi.Sidebar.timeTopGap),
        )
        MoeSidebarStatusStack(
            batteryPercent = batteryPercent,
            modifier = Modifier.padding(top = MoeUi.Sidebar.statusTopGap),
        )

        Spacer(modifier = Modifier.weight(1f))

        MoeSidebarIconRail(icons = icons)

        Spacer(modifier = Modifier.height(MoeUi.Sidebar.bottomInset))
    }
}

@Composable
private fun MoeSidebarValueStack(
    topValue: String,
    bottomValue: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MoeUi.Sidebar.timeGap),
    ) {
        MoeSidebarTimeValue(value = topValue)
        Box(
            modifier =
                Modifier
                    .width(MoeUi.Sidebar.dividerWidth)
                    .height(MoeUi.Sidebar.dividerHeight)
                    .background(Color.White.copy(alpha = MoeUi.Sidebar.dividerAlpha))
        )
        MoeSidebarTimeValue(value = bottomValue)
    }
}

@Composable
private fun MoeSidebarIconRail(icons: List<MoeSidebarIconItem>) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MoeUi.Sidebar.iconSpacing),
    ) {
        icons.forEachIndexed { index, item ->
            if (index > 0) MoeSidebarDivider()
            MoeSidebarIconItemView(item = item)
        }
    }
}

@Composable
private fun MoeSidebarIconItemView(item: MoeSidebarIconItem) {
    Box(
        modifier =
            Modifier
                .size(MoeUi.Sidebar.iconHitSize)
                .clickable(
                    interactionSource =
                        remember {
                            androidx.compose.foundation.interaction.MutableInteractionSource()
                        },
                    indication = null,
                    onClick = item.onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = MoeUi.Sidebar.iconAlpha),
            modifier = Modifier.size(MoeUi.Sidebar.iconSize),
        )
    }
}

@Composable
private fun MoeSidebarTimeValue(value: String) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = value,
            color = Color.White.copy(alpha = MoeUi.Sidebar.timeAlpha),
            style = MiuixTheme.textStyles.title1,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            fontSize = 43.sp,
            letterSpacing = MoeUi.Sidebar.digitLetterSpacing,
            softWrap = false,
        )
    }
}

@Composable
private fun MoeSidebarStatusStack(batteryPercent: Int?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MoeUi.Sidebar.statusGap),
    ) {
        MoeBatteryCapsule(percent = batteryPercent)
        Text(
            text = batteryPercent?.let { "$it%" } ?: "--%",
            color = Color.White.copy(alpha = MoeUi.Sidebar.statusAlpha),
            style = MiuixTheme.textStyles.footnote1,
            fontSize = 12.sp,
            softWrap = false,
        )
    }
}

@Composable
private fun MoeBatteryCapsule(percent: Int?) {
    val clampedPercent = percent?.coerceIn(0, 100) ?: 66
    val fillFraction = (clampedPercent / 100f).coerceIn(0.12f, 1f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier =
                Modifier
                    .width(MoeUi.Sidebar.batteryWidth)
                    .height(MoeUi.Sidebar.batteryHeight)
                    .clip(RoundedCornerShape(moeRadii.full))
                    .background(Color.White.copy(alpha = 0.36f))
                    .padding(2.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(fillFraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(moeRadii.full))
                        .background(Color.White.copy(alpha = 0.86f))
            )
        }
        Box(
            modifier =
                Modifier
                    .width(MoeUi.Sidebar.batteryKnobWidth)
                    .height(MoeUi.Sidebar.batteryHeight * 0.46f)
                    .clip(RoundedCornerShape(moeRadii.full))
                    .background(Color.White.copy(alpha = 0.54f))
        )
    }
}

@Composable
private fun MoeSidebarDivider() {
    Box(
        modifier =
            Modifier
                .width(MoeUi.Sidebar.dividerWidth)
                .height(MoeUi.Sidebar.dividerHeight)
                .background(Color.White.copy(alpha = MoeUi.Sidebar.dividerAlpha))
    )
}

internal enum class MoeWallpaperQualityMode {
    Foreground,
    BackgroundBlur,
}

internal fun lerpFloat(start: Float, stop: Float, progress: Float): Float =
    start + (stop - start) * progress

internal fun lerpDp(start: Dp, stop: Dp, progress: Float): Dp = start + (stop - start) * progress

internal fun calculateHomeVisibility(currentPage: Int, currentPageOffsetFraction: Float): Float {
    val offset = abs(currentPage.toFloat() + currentPageOffsetFraction)
    return 1f - offset.coerceIn(0f, 1f)
}

internal data class MoeSidebarIconItem(
    val icon: ImageVector,
    val onClick: () -> Unit,
)

internal data class MoeDurationPair(
    val top: String = "00",
    val bottom: String = "00",
)

/**
 * Wall-clock variant used while idle: maps an epoch timestamp to the current local hour/minute so
 * the rail shows the real time (top = HH, bottom = mm) instead of a frozen 00 / 00.
 */
internal fun formatMoeClock(nowMillis: Long): MoeDurationPair {
    val calendar = Calendar.getInstance().apply { timeInMillis = nowMillis }
    return MoeDurationPair(
        top = calendar.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0'),
        bottom = calendar.get(Calendar.MINUTE).toString().padStart(2, '0'),
    )
}

internal fun formatMoeDuration(elapsedMillis: Long): MoeDurationPair {
    val totalSeconds = (elapsedMillis / 1000L).coerceAtLeast(0L)
    val totalMinutes = totalSeconds / 60L
    val totalHours = totalMinutes / 60L
    return if (totalMinutes < 60L) {
        MoeDurationPair(
            top = totalMinutes.toString().padStart(2, '0'),
            bottom = (totalSeconds % 60L).toString().padStart(2, '0'),
        )
    } else {
        MoeDurationPair(
            top = totalHours.toString().padStart(2, '0'),
            bottom = (totalMinutes % 60L).toString().padStart(2, '0'),
        )
    }
}
