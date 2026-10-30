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

package com.github.yumeyucca.yumebox.presentation.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.yumeyucca.yumebox.common.util.ByteFormatter
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme

data class BarChartItem(
    val label: String,
    val value: Long,
    val isHighlighted: Boolean = false,
)

/**
 * Traffic chart shared by the 24-hour and seven-day views.
 *
 * [animationKey] identifies a chart session. Live traffic updates deliberately do not participate
 * in the key, so polling cannot restart the entrance animation.
 */
@Composable
fun TrafficBarChart(
    items: List<BarChartItem>,
    modifier: Modifier = Modifier,
    animationKey: Any? = null,
    averageValue: Long? = null,
    averageLabel: String? = null,
    showSelectionTip: Boolean = false,
    tipTitleFor: (BarChartItem) -> String = { it.label },
    chartHeight: Dp = AppTheme.sizes.trafficBarChartHeight,
    barWidth: Dp = AppTheme.sizes.trafficBarWidth,
) {
    val spacing = AppTheme.spacing
    val radii = AppTheme.radii
    val sizes = AppTheme.sizes
    val displayItems = items.ifEmpty { listOf(BarChartItem("", 0L)) }
    val slotCount = displayItems.size
    val safeMaxValue =
        maxOf(displayItems.maxOfOrNull(BarChartItem::value) ?: 0L, averageValue ?: 0L)
            .coerceAtLeast(1L)
    val hasTraffic = displayItems.any { it.value > 0L }

    val growth = remember(animationKey) { Animatable(0f) }
    LaunchedEffect(animationKey, hasTraffic) {
        if (hasTraffic) {
            growth.snapTo(0f)
            growth.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            )
        } else {
            growth.snapTo(1f)
        }
    }

    val defaultIndex =
        displayItems.indexOfFirst(BarChartItem::isHighlighted).takeIf { it >= 0 }
            ?: displayItems.lastIndex
    var internalSelectedIndex by
    remember(animationKey, slotCount) { mutableIntStateOf(defaultIndex) }
    val activeIndex =
        internalSelectedIndex.takeIf { it in displayItems.indices } ?: defaultIndex
    val averageFraction =
        averageValue
            ?.takeIf { it > 0L }
            ?.let { (it.toFloat() / safeMaxValue.toFloat()).coerceIn(0f, 1f) }

    val horizontalGridColor = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val verticalGridColor = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.24f)
    val guideColor = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.28f)
    val labelColor = MiuixTheme.colorScheme.onSurfaceVariantSummary
    // One ramp for every bar: the selected bar is the full-strength primary, the rest sit just
    // under it, so selection reads as emphasis rather than as a second series.
    val barColor = MiuixTheme.colorScheme.primary
    val averageLineColor = MiuixTheme.colorScheme.primary.copy(alpha = 0.48f)
    val tipBubbleHeight = 60.dp
    val tipHeight = if (showSelectionTip) 72.dp else 0.dp
    val scaleWidth = 60.dp

    Column(modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints(modifier = Modifier
            .fillMaxWidth()
            .height(chartHeight)) {
            val plotWidth = (maxWidth - scaleWidth).coerceAtLeast(1.dp)
            val plotHeight = (chartHeight - tipHeight).coerceAtLeast(1.dp)
            val slotWidth = plotWidth / slotCount
            val actualBarWidth = barWidth.coerceAtMost(slotWidth * 0.68f)

            if (showSelectionTip && activeIndex in displayItems.indices) {
                val selectedCenter = slotWidth * (activeIndex + 0.5f)
                Canvas(modifier = Modifier
                    .width(plotWidth)
                    .fillMaxHeight()) {
                    drawLine(
                        color = guideColor,
                        start = Offset(selectedCenter.toPx(), tipBubbleHeight.toPx()),
                        end = Offset(selectedCenter.toPx(), size.height),
                        strokeWidth = 0.75.dp.toPx(),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .width(plotWidth)
                    .height(plotHeight)
                    .align(Alignment.BottomStart)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val thinStroke = 0.5.dp.toPx()
                    val verticalDash =
                        PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()))

                    listOf(0f, size.height / 2f, size.height).forEach { y ->
                        drawLine(
                            color = horizontalGridColor,
                            start = Offset.Zero.copy(y = y),
                            end = Offset(size.width, y),
                            strokeWidth = thinStroke,
                        )
                    }

                    val verticalLines =
                        if (slotCount == 24) {
                            listOf(0, 6, 12, 18, 24)
                        } else {
                            (0..slotCount).toList()
                        }
                    verticalLines.forEach { position ->
                        val x = size.width * position / slotCount
                        drawLine(
                            color = verticalGridColor,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = thinStroke,
                            pathEffect = verticalDash,
                        )
                    }

                    averageFraction?.let { fraction ->
                        val y = size.height * (1f - fraction)
                        drawLine(
                            color = averageLineColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.25.dp.toPx(),
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    displayItems.forEachIndexed { index, item ->
                        val targetFraction =
                            (item.value.toFloat() / safeMaxValue.toFloat()).coerceIn(0f, 1f)
                        val heightFraction = targetFraction * growth.value
                        Box(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable(
                                        enabled = showSelectionTip,
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() },
                                    ) {
                                        internalSelectedIndex = index
                                    },
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            if (heightFraction > 0f) {
                                Spacer(
                                    modifier =
                                        Modifier
                                            .width(actualBarWidth)
                                            .fillMaxHeight(heightFraction)
                                            .squircleBackground(
                                                if (index == activeIndex && showSelectionTip) {
                                                    barColor
                                                } else {
                                                    barColor.copy(alpha = 0.92f)
                                                },
                                                topStart = radii.radius4,
                                                topEnd = radii.radius4,
                                                bottomEnd = 0.dp,
                                                bottomStart = 0.dp,
                                            )
                                )
                            }
                        }
                    }
                }
            }

            Box(
                modifier =
                    Modifier
                        .width(scaleWidth)
                        .height(plotHeight)
                        .align(Alignment.BottomEnd)
                        .padding(start = spacing.space8)
            ) {
                ScaleLabel(ByteFormatter.format(safeMaxValue), Modifier.align(Alignment.TopStart))
                ScaleLabel(ByteFormatter.format(safeMaxValue / 2), Modifier.align(Alignment.CenterStart))
                ScaleLabel("0", Modifier.align(Alignment.BottomStart))

                if (averageFraction != null && !averageLabel.isNullOrBlank()) {
                    val labelHeight = 16.dp
                    val top =
                        (plotHeight * (1f - averageFraction) - labelHeight / 2f).coerceIn(
                            0.dp,
                            plotHeight - labelHeight,
                        )
                    Text(
                        text = averageLabel,
                        modifier = Modifier.offset(y = top),
                        style = MiuixTheme.textStyles.footnote1.copy(fontSize = 10.sp),
                        color = MiuixTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                    )
                }
            }

            if (showSelectionTip && activeIndex in displayItems.indices) {
                val selectedItem = displayItems[activeIndex]
                val selectedCenter = slotWidth * (activeIndex + 0.5f)
                val bubbleWidth = 136.dp.coerceAtMost(maxWidth)
                val bubbleLeft =
                    (selectedCenter - bubbleWidth / 2f).coerceIn(
                        0.dp,
                        (maxWidth - bubbleWidth).coerceAtLeast(0.dp),
                    )
                Box(
                    modifier =
                        Modifier
                            .offset(x = bubbleLeft)
                            .width(bubbleWidth)
                            .height(tipBubbleHeight)
                            .squircleSurface(MiuixTheme.colorScheme.onSurface.copy(alpha = 0.06f), radii.radius14)
                            .padding(horizontal = spacing.space14, vertical = spacing.space10)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
                        Text(
                            text = tipTitleFor(selectedItem),
                            style = MiuixTheme.textStyles.footnote1.copy(fontSize = 11.sp),
                            color = labelColor,
                            maxLines = 1,
                        )
                        Text(
                            text = ByteFormatter.format(selectedItem.value),
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.space8))

        BoxWithConstraints(modifier = Modifier
            .fillMaxWidth()
            .height(sizes.trafficBarLabelHeight)) {
            val plotWidth = (maxWidth - scaleWidth).coerceAtLeast(1.dp)
            val slotWidth = plotWidth / slotCount
            val labelWidth = if (slotCount == 24) 44.dp else slotWidth
            displayItems.forEachIndexed { index, item ->
                if (item.label.isEmpty()) return@forEachIndexed
                val center = slotWidth * (index + 0.5f)
                val left =
                    (center - labelWidth / 2f).coerceIn(
                        0.dp,
                        (plotWidth - labelWidth).coerceAtLeast(0.dp),
                    )
                Text(
                    text = item.label,
                    modifier = Modifier
                        .offset(x = left)
                        .width(labelWidth),
                    style = MiuixTheme.textStyles.footnote1.copy(fontSize = 10.sp),
                    color =
                        if (showSelectionTip && index == activeIndex) {
                            MiuixTheme.colorScheme.primary
                        } else {
                            labelColor
                        },
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun ScaleLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.wrapContentWidth(),
        style = MiuixTheme.textStyles.footnote1.copy(fontSize = 10.sp),
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        maxLines = 1,
    )
}
