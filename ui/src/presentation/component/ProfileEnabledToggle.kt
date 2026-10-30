/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import com.github.yumeyucca.yumebox.presentation.theme.inactiveSurfaceColor
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Enable switch in the top-right corner; mirrors Miuix Switch's track colour animation. */
@Composable
internal fun ProfileEnabledToggle(
    isActive: Boolean,
    enabled: Boolean,
    activeColor: Color,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ProfileEnabledToggleVisual(
        isActive = isActive,
        enabled = enabled,
        activeColor = activeColor,
        modifier =
            modifier.toggleable(
                value = isActive,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onValueChange = { onToggle() },
            ),
    )
}

@Composable
internal fun ProfileEnabledToggleVisual(
    isActive: Boolean,
    enabled: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MiuixTheme.colorScheme
    val strings = YumeTxt.Component.ProfileCard
    val inactiveContainer = inactiveSurfaceColor()
    val progress by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = ProfileToggleSpring,
        label = "profileEnabledProgress",
    )
    val fillProgress = progress.coerceIn(0f, 1f)

    Box(
        modifier =
            modifier
                .height(24.dp)
                .widthIn(min = 64.dp)
                .clip(CircleShape)
                .background(inactiveContainer)
                .drawBehind {
                    drawRoundRect(
                        color = activeColor,
                        size = Size(size.width * fillProgress, size.height),
                        cornerRadius = CornerRadius(size.height / 2f),
                    )
                }
                .alpha(if (enabled) 1f else 0.45f)
                .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = isActive,
            contentAlignment = Alignment.Center,
            transitionSpec = {
                ContentTransform(
                    targetContentEnter =
                        fadeIn(tween(durationMillis = 180)) +
                            slideInHorizontally(tween(durationMillis = 220)) { it / 3 },
                    initialContentExit =
                        fadeOut(tween(durationMillis = 120)) +
                            slideOutHorizontally(tween(durationMillis = 180)) { -it / 3 },
                    sizeTransform = SizeTransform(clip = false),
                )
            },
            label = "profileEnabledLabel",
        ) { active ->
            Text(
                text = if (active) strings.CurrentProfile else strings.NotEnabled,
                fontSize = 12.sp,
                fontWeight = FontWeight(600),
                color =
                    if (active) Color.White else colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
            )
        }
    }
}

private val ProfileToggleSpring = spring<Float>(dampingRatio = 0.99f, stiffness = 438.6f)
