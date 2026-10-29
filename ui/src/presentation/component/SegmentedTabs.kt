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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.glass.GlassDefaults
import top.yukonga.miuix.kmp.glass.GlassSegmentedTabRowDefaults
import top.yukonga.miuix.kmp.glass.GlassSegmentedTabRow
import top.yukonga.miuix.kmp.glass.glass
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

/**
 * zashboard 风格的分段 tab:40dp 高、13sp 文字,选区取色与底部导航栏一致——
 * 选中项 primary 文字叠 primary 微透胶囊,未选中 onSurface 次级文字,整体在 surfaceVariant 轨道上滑动。
 */
@Composable
fun SegmentedTabRow(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    // 40dp 为默认;与并排元素(如顶栏 42dp 玻璃容器)对齐时可传入一致高度。
    tabHeight: Dp = 40.dp,
    backdrop: LayerBackdrop? = null,
    scrollable: Boolean = false,
    scrollState: ScrollState? = null,
) {
    if (tabs.isEmpty()) return

    if (scrollable) {
        ScrollableSegmentedTabRow(
            tabs = tabs,
            selectedTabIndex = selectedTabIndex,
            onTabSelected = onTabSelected,
            modifier = modifier,
            tabHeight = tabHeight,
            backdrop = backdrop,
            scrollState = scrollState,
        )
        return
    }

    if (backdrop != null && isRuntimeShaderSupported()) {
        val colors = MiuixTheme.colorScheme
        GlassSegmentedTabRow(
            tabs = tabs,
            selectedIndex = selectedTabIndex,
            onSelect = onTabSelected,
            backdrop = backdrop,
            modifier = modifier,
            height = tabHeight,
            shadow = null,
            trackColor = colors.surfaceVariant,
            indicatorColor = colors.primary.copy(alpha = AppTheme.opacity.subtle),
            selectedContentColor = colors.primary,
            contentColor = colors.onSurface.copy(alpha = AppTheme.opacity.prominentText),
        )
        return
    }

    val opacity = AppTheme.opacity
    val colorScheme = MiuixTheme.colorScheme
    val shape = RoundedCornerShape(100.dp)
    val trackColor = colorScheme.surfaceVariant
    val selectedPillColor = colorScheme.primary.copy(alpha = opacity.subtle)
    val selectedTextColor = colorScheme.primary
    // 未选中文字取一级档(prominentText),不用 secondaryText 次级档。
    val unselectedTextColor = colorScheme.onSurface.copy(alpha = opacity.prominentText)
    val indicatorInset = 4.dp
    val indicatorGap = 4.dp
    val indicatorAnimationSpec = remember {
        tween<Dp>(durationMillis = 280, easing = FastOutSlowInEasing)
    }
    val textAnimationSpec = remember {
        tween<Color>(durationMillis = 220, easing = FastOutSlowInEasing)
    }

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxWidth()
                .height(tabHeight)
                .clip(shape)
                .background(trackColor)
                .padding(indicatorInset)
    ) {
        val clampedSelectedTabIndex = selectedTabIndex.coerceIn(0, tabs.lastIndex)
        val tabCount = tabs.size
        val tabWidth =
            remember(maxWidth, tabCount) {
                ((maxWidth - indicatorGap * (tabCount - 1)) / tabCount).coerceAtLeast(0.dp)
            }
        val targetOffset =
            remember(tabWidth, clampedSelectedTabIndex) {
                (tabWidth + indicatorGap) * clampedSelectedTabIndex
            }
        val indicatorOffset by
            animateDpAsState(
                targetValue = targetOffset,
                animationSpec = indicatorAnimationSpec,
                label = "segmented_tab_indicator_offset",
            )

        Box(modifier = Modifier.matchParentSize()) {
            Box(
                modifier =
                    Modifier
                        .offset(x = indicatorOffset)
                        .width(tabWidth)
                        .fillMaxHeight()
                        .clip(shape)
                        .background(selectedPillColor)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(indicatorGap),
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = clampedSelectedTabIndex == index
                val textColor by
                    animateColorAsState(
                        targetValue = if (isSelected) selectedTextColor else unselectedTextColor,
                        animationSpec = textAnimationSpec,
                        label = "segmented_tab_text_color_$index",
                    )

                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(shape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onTabSelected(index) },
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = tab,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun ScrollableSegmentedTabRow(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier,
    tabHeight: Dp,
    backdrop: LayerBackdrop?,
    scrollState: ScrollState?,
) {
    val opacity = AppTheme.opacity
    val colorScheme = MiuixTheme.colorScheme
    val shape = GlassSegmentedTabRowDefaults.shape(tabHeight)
    val indicatorShape =
        GlassSegmentedTabRowDefaults.shape(
            tabHeight - GlassSegmentedTabRowDefaults.TrackPadding * 2
        )
    val trackColor = colorScheme.surfaceVariant
    val selectedPillColor = colorScheme.primary.copy(alpha = opacity.subtle)
    val selectedTextColor = colorScheme.primary
    val unselectedTextColor = colorScheme.onSurface.copy(alpha = opacity.prominentText)
    val textAnimationSpec = remember {
        tween<Color>(durationMillis = 220, easing = FastOutSlowInEasing)
    }
    val indicatorAnimationSpec = remember {
        tween<Float>(durationMillis = 280, easing = FastOutSlowInEasing)
    }
    val density = LocalDensity.current
    val resolvedScrollState = scrollState ?: rememberScrollState()
    val tabBounds = remember { mutableStateMapOf<Int, Rect>() }
    val clampedSelectedTabIndex = selectedTabIndex.coerceIn(0, tabs.lastIndex)
    val selectedBounds = tabBounds[clampedSelectedTabIndex]
    val indicatorLeft by
        animateFloatAsState(
            targetValue = selectedBounds?.left ?: 0f,
            animationSpec = indicatorAnimationSpec,
            label = "scrollable_segmented_tab_indicator_left",
        )
    val indicatorWidth by
        animateFloatAsState(
            targetValue = selectedBounds?.width ?: 0f,
            animationSpec = indicatorAnimationSpec,
            label = "scrollable_segmented_tab_indicator_width",
        )

    LaunchedEffect(tabs) {
        tabBounds.clear()
    }

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxWidth()
                .height(tabHeight)
                .clip(shape)
                .then(
                    if (backdrop != null && isRuntimeShaderSupported()) {
                        Modifier.glass(
                            backdrop = backdrop,
                            shape = shape,
                            style = GlassDefaults.Style,
                            material = GlassSegmentedTabRowDefaults.material(),
                            stroke = GlassSegmentedTabRowDefaults.stroke(),
                            shading = false,
                        )
                    } else {
                        Modifier.background(trackColor)
                    }
                )
                .padding(GlassSegmentedTabRowDefaults.TrackPadding),
    ) {
        val viewportWidthPx = with(density) { maxWidth.toPx() }
        LaunchedEffect(
            clampedSelectedTabIndex,
            selectedBounds,
            resolvedScrollState.maxValue,
            viewportWidthPx,
        ) {
            if (selectedBounds != null) {
                val target =
                    (selectedBounds.center.x - viewportWidthPx / 2f)
                        .roundToInt()
                        .coerceIn(0, resolvedScrollState.maxValue)
                resolvedScrollState.animateScrollTo(target)
            }
        }

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .horizontalScroll(resolvedScrollState),
        ) {
            Box(modifier = Modifier.fillMaxHeight()) {
                if (indicatorWidth > 0f) {
                    Box(
                        modifier =
                            Modifier
                                .offset { IntOffset(indicatorLeft.roundToInt(), 0) }
                                .width(with(density) { indicatorWidth.toDp() })
                                .fillMaxHeight()
                                .clip(indicatorShape)
                                .background(selectedPillColor),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxHeight(),
                    horizontalArrangement =
                        Arrangement.spacedBy(GlassSegmentedTabRowDefaults.TabGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    tabs.forEachIndexed { index, tab ->
                        val isSelected = clampedSelectedTabIndex == index
                        val textColor by
                            animateColorAsState(
                                targetValue =
                                    if (isSelected) selectedTextColor else unselectedTextColor,
                                animationSpec = textAnimationSpec,
                                label = "scrollable_segmented_tab_text_$index",
                            )
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxHeight()
                                    .onGloballyPositioned { coordinates ->
                                        tabBounds[index] = coordinates.boundsInParent()
                                    }
                                    .clip(indicatorShape)
                                    .clickable(
                                        interactionSource =
                                            remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { onTabSelected(index) },
                                    )
                                    .padding(
                                        horizontal =
                                            GlassSegmentedTabRowDefaults.TabPaddingHorizontal,
                                    ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = tab,
                                style =
                                    if (isSelected) {
                                        MiuixTheme.textStyles.subtitle
                                    } else {
                                        MiuixTheme.textStyles.body2
                                    },
                                color = textColor,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}
