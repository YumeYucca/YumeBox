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

package com.github.yumeyucca.yumebox.screen.moe

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun HomeToNodeSwipeMockup(modifier: Modifier = Modifier) {
    val colorScheme = MiuixTheme.colorScheme
    val palette = mockupPalette()

    val transition = rememberInfiniteTransition(label = "home_to_node_swipe")
    val swipeProgress by
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 0f,
            animationSpec =
                infiniteRepeatable(
                    animation =
                        keyframes {
                            durationMillis = 3600
                            0f at 0
                            0f at 620
                            1f at 1320 using FastOutSlowInEasing
                            1f at 2380
                            0f at 3100 using FastOutSlowInEasing
                            0f at 3600
                        },
                    repeatMode = RepeatMode.Restart,
                ),
            label = "home_to_node_swipe_progress",
        )
    val touchScale by
        transition.animateFloat(
            initialValue = 1f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation =
                        keyframes {
                            durationMillis = 3600
                            1f at 0
                            1f at 520
                            0.86f at 680 using FastOutSlowInEasing
                            1f at 900
                            1f at 3600
                        },
                    repeatMode = RepeatMode.Restart,
                ),
            label = "home_to_node_touch_scale",
        )

    MockupPhoneFrame(palette = palette, modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(15.dp))) {
            Box(Modifier.matchParentSize().background(colorScheme.surface))

            NodePagePreview(
                palette = palette,
                modifier =
                    Modifier.fillMaxSize().graphicsLayer {
                        translationX = (1f - swipeProgress) * size.width
                        alpha = 0.7f + 0.3f * swipeProgress
                    },
            )

            HomePagePreview(
                palette = palette,
                modifier =
                    Modifier.fillMaxSize()
                        .graphicsLayer {
                            translationX = -swipeProgress * size.width
                            val scale = 1f - 0.025f * swipeProgress
                            scaleX = scale
                            scaleY = scale
                        }
                        .padding(7.dp),
            )

            Box(
                modifier =
                    Modifier.matchParentSize().graphicsLayer {
                        translationX = (0.22f - 0.32f * swipeProgress) * size.width
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier.graphicsLayer {
                                scaleX = touchScale
                                scaleY = touchScale
                            }
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(colorScheme.primary)
                            .border(2.dp, colorScheme.surface, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun HomePagePreview(palette: MockupPalette, modifier: Modifier = Modifier) {
    val colorScheme = MiuixTheme.colorScheme
    val opacity = AppTheme.opacity

    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(15.dp))
                .background(colorScheme.surface)
                .border(1.dp, palette.frameBorder, RoundedCornerShape(15.dp))
                .padding(7.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxHeight().weight(0.22f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(10.dp))
            Box(Modifier.width(12.dp).height(52.dp).clip(RoundedCornerShape(6.dp)).background(palette.maskStrong))
            Spacer(Modifier.weight(1f))
            repeat(3) { index ->
                Box(
                    Modifier.size(13.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (index == 0) colorScheme.primary else palette.mask)
                )
                if (index < 2) Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(10.dp))
        }

        Column(
            modifier = Modifier.fillMaxHeight().weight(0.78f),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Box(Modifier.fillMaxWidth().weight(2f).clip(RoundedCornerShape(13.dp)).background(palette.maskStrong)) {
                Box(
                    Modifier.align(Alignment.BottomStart)
                        .fillMaxWidth(0.64f)
                        .padding(8.dp)
                        .height(9.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(colorScheme.surface.copy(alpha = opacity.secondaryText))
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf(0.92f, 0.76f, 0.48f).forEach { lineWidth ->
                    Box(
                        Modifier.fillMaxWidth(lineWidth)
                            .height(8.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(palette.mask)
                    )
                }
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        Modifier.size(18.dp)
                            .clip(CircleShape)
                            .background(colorScheme.surfaceVariant.copy(alpha = opacity.surfaceVariant))
                            .border(1.dp, palette.frameBorder, CircleShape)
                    )
                    Box(
                        Modifier.weight(1f)
                            .height(18.dp)
                            .clip(RoundedCornerShape(50))
                            .background(colorScheme.surfaceVariant.copy(alpha = opacity.surfaceVariant))
                            .border(1.dp, palette.frameBorder, RoundedCornerShape(50))
                    )
                }
                Spacer(Modifier.height(3.dp))
            }
        }
    }
}

@Composable
private fun NodePagePreview(palette: MockupPalette, modifier: Modifier = Modifier) {
    val colorScheme = MiuixTheme.colorScheme
    val opacity = AppTheme.opacity

    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(15.dp))
                .background(colorScheme.surface)
                .border(1.dp, palette.frameBorder, RoundedCornerShape(15.dp))
                .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(38.dp).height(13.dp).clip(RoundedCornerShape(4.dp)).background(palette.maskStrong))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(2) { Box(Modifier.size(13.dp).clip(CircleShape).background(palette.mask)) }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(3) { index ->
                Box(
                    Modifier.weight(if (index == 0) 1.25f else 1f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (index == 0) colorScheme.primary else palette.mask)
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            repeat(4) { index ->
                NodePreviewCard(
                    selected = index == 0,
                    palette = palette,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            }
        }
    }
}

@Composable
private fun NodePreviewCard(
    selected: Boolean,
    palette: MockupPalette,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MiuixTheme.colorScheme
    val opacity = AppTheme.opacity

    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(11.dp))
                .background(palette.cardVariant)
                .border(
                    1.dp,
                    if (selected) colorScheme.primary.copy(alpha = opacity.disabled) else Color.Transparent,
                    RoundedCornerShape(11.dp),
                )
                .padding(7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            Modifier.size(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (selected) colorScheme.primary else palette.maskStrong)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                Modifier.fillMaxWidth(if (selected) 0.74f else 0.58f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(palette.maskStrong)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(2) {
                    Box(Modifier.width(18.dp).height(7.dp).clip(RoundedCornerShape(4.dp)).background(palette.mask))
                }
            }
        }
        Box(
            Modifier.width(26.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(if (selected) colorScheme.primary else palette.mask)
        )
    }
}
