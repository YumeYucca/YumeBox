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
import androidx.compose.foundation.lazy.LazyListScope
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
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
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

private const val SegmentedTabTextAnimationDurationMillis = 220
private const val SegmentedTabIndicatorAnimationDurationMillis = 280

private val SegmentedTabTextAnimationSpec =
    tween<Color>(
        durationMillis = SegmentedTabTextAnimationDurationMillis,
        easing = FastOutSlowInEasing,
    )
private val SegmentedTabIndicatorAnimationSpec =
    tween<Dp>(
        durationMillis = SegmentedTabIndicatorAnimationDurationMillis,
        easing = FastOutSlowInEasing,
    )
private val ScrollableSegmentedTabIndicatorAnimationSpec =
    tween<Float>(
        durationMillis = SegmentedTabIndicatorAnimationDurationMillis,
        easing = FastOutSlowInEasing,
    )

/**
 * zashboard-style segmented tab: 40dp tall, 13sp text, palette borrowed from the bottom bar —
 * primary label on a translucent primary pill when selected, onSurface when not, sliding on a
 * surfaceVariant track.
 */
@Composable
fun SegmentedTabRow(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    // Defaults to [UiDp.dp40]; pass a matching height to align with a sibling (e.g. the 42dp glass top bar).
    tabHeight: Dp = UiDp.dp40,
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
        val colors = segmentedTabColors()
        GlassSegmentedTabRow(
            tabs = tabs,
            selectedIndex = selectedTabIndex,
            onSelect = onTabSelected,
            backdrop = backdrop,
            modifier = modifier,
            height = tabHeight,
            shadow = null,
            trackColor = colors.trackColor,
            indicatorColor = colors.selectedPillColor,
            selectedContentColor = colors.selectedTextColor,
            contentColor = colors.unselectedTextColor,
        )
        return
    }

    val colors = segmentedTabColors()
    val shape = RoundedCornerShape(100.dp)
    val indicatorInset = 4.dp
    val indicatorGap = 4.dp
    val textStyle =
        MiuixTheme.textStyles.main.copy(
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxWidth()
                .height(tabHeight)
                .clip(shape)
                .background(colors.trackColor)
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
                animationSpec = SegmentedTabIndicatorAnimationSpec,
                label = "segmented_tab_indicator_offset",
            )

        Box(
            modifier =
                Modifier
                    .offset(x = indicatorOffset)
                    .width(tabWidth)
                    .fillMaxHeight()
                    .clip(shape)
                    .background(colors.selectedPillColor)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(indicatorGap),
        ) {
            tabs.forEachIndexed { index, tab ->
                SegmentedTabItem(
                    text = tab,
                    selected = clampedSelectedTabIndex == index,
                    colors = colors,
                    selectedTextStyle = textStyle,
                    unselectedTextStyle = textStyle,
                    shape = shape,
                    onClick = { onTabSelected(index) },
                    modifier = Modifier.weight(1f),
                )
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
    val colors = segmentedTabColors()
    val shape = GlassSegmentedTabRowDefaults.shape(tabHeight)
    val indicatorShape =
        GlassSegmentedTabRowDefaults.shape(
            tabHeight - GlassSegmentedTabRowDefaults.TrackPadding * 2
        )
    val selectedTextStyle = MiuixTheme.textStyles.subtitle
    val unselectedTextStyle = MiuixTheme.textStyles.body2
    val density = LocalDensity.current
    val resolvedScrollState = scrollState ?: rememberScrollState()
    val tabBounds = remember { mutableStateMapOf<Int, Rect>() }
    val clampedSelectedTabIndex = selectedTabIndex.coerceIn(0, tabs.lastIndex)
    val selectedBounds = tabBounds[clampedSelectedTabIndex]
    val indicatorLeft by
        animateFloatAsState(
            targetValue = selectedBounds?.left ?: 0f,
            animationSpec = ScrollableSegmentedTabIndicatorAnimationSpec,
            label = "scrollable_segmented_tab_indicator_left",
        )
    val indicatorWidth by
        animateFloatAsState(
            targetValue = selectedBounds?.width ?: 0f,
            animationSpec = ScrollableSegmentedTabIndicatorAnimationSpec,
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
                        Modifier.background(colors.trackColor)
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
                                .background(colors.selectedPillColor),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxHeight(),
                    horizontalArrangement =
                        Arrangement.spacedBy(GlassSegmentedTabRowDefaults.TabGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    tabs.forEachIndexed { index, tab ->
                        SegmentedTabItem(
                            text = tab,
                            selected = clampedSelectedTabIndex == index,
                            colors = colors,
                            selectedTextStyle = selectedTextStyle,
                            unselectedTextStyle = unselectedTextStyle,
                            shape = indicatorShape,
                            onClick = { onTabSelected(index) },
                            horizontalPadding =
                                GlassSegmentedTabRowDefaults.TabPaddingHorizontal,
                            onBoundsChanged = { bounds -> tabBounds[index] = bounds },
                        )
                    }
                }
            }
        }
    }
}

private data class SegmentedTabColors(
    val trackColor: Color,
    val selectedPillColor: Color,
    val selectedTextColor: Color,
    val unselectedTextColor: Color,
)

@Composable
private fun segmentedTabColors(): SegmentedTabColors {
    val colorScheme = MiuixTheme.colorScheme
    val opacity = AppTheme.opacity
    return SegmentedTabColors(
        trackColor = colorScheme.surfaceVariant,
        selectedPillColor = colorScheme.primary.copy(alpha = opacity.subtle),
        selectedTextColor = colorScheme.primary,
        // Unselected labels use the prominent tier, not the secondary one.
        unselectedTextColor = colorScheme.onSurface.copy(alpha = opacity.prominentText),
    )
}

@Composable
private fun SegmentedTabItem(
    text: String,
    selected: Boolean,
    colors: SegmentedTabColors,
    selectedTextStyle: TextStyle,
    unselectedTextStyle: TextStyle,
    shape: Shape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 0.dp,
    onBoundsChanged: ((Rect) -> Unit)? = null,
) {
    val textColor by
        animateColorAsState(
            targetValue = if (selected) colors.selectedTextColor else colors.unselectedTextColor,
            animationSpec = SegmentedTabTextAnimationSpec,
            label = "segmented_tab_text_$text",
        )
    val boundsModifier =
        onBoundsChanged?.let { callback ->
            Modifier.onGloballyPositioned { coordinates ->
                callback(coordinates.boundsInParent())
            }
        } ?: Modifier

    Box(
        modifier =
            modifier
                .fillMaxHeight()
                .then(boundsModifier)
                .clip(shape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                )
                .padding(horizontal = horizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = if (selected) selectedTextStyle else unselectedTextStyle,
            color = textColor,
            maxLines = 1,
        )
    }
}

/**
 * Pins a segmented row to the top of a lazy list. The surface-coloured band is load-bearing: the tab
 * row's gutter is transparent, so rows scrolling underneath would otherwise show through it.
 */
fun LazyListScope.stickySegmentedTabs(
    key: Any,
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    tabHeight: Dp = UiDp.dp40,
    // Horizontal inset of the row itself; pass [UiDp.dp0] for a full-bleed row.
    tabRowPadding: Dp = UiDp.dp12,
) {
    stickyHeader(key = key) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MiuixTheme.colorScheme.surface)
                    .padding(vertical = UiDp.dp4),
        ) {
            SegmentedTabRow(
                tabs = tabs,
                selectedTabIndex = selectedTabIndex,
                onTabSelected = onTabSelected,
                modifier = Modifier.padding(horizontal = tabRowPadding),
                tabHeight = tabHeight,
            )
        }
    }
}
