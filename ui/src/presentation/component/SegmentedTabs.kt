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
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
) {
    if (tabs.isEmpty()) return

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
