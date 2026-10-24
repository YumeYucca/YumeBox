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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.presentation.theme.YumeBlur.glassSurface
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** NexioSchedule top-bar chrome sizing: 42dp containers carrying 22dp icons. */
object GlassTopActionDefaults {
    val ContainerSize = UiDp.dp42
    val IconSize = UiDp.dp22
    /** Hit-slop padding around each icon inside a pill. */
    val PillIconHitInset = UiDp.dp8
    val PillIconGap = UiDp.dp14
    val PillHorizontalPadding = UiDp.dp10
}

/** One tappable action inside a frosted top-bar container. */
data class GlassTopAction(
    val icon: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit,
)

/**
 * NexioSchedule-style frosted top-bar actions: two or more icons share one capsule pill, single
 * icons stand in their own circles (the reference topbar's pill+circle chrome). The container
 * frosts [backdrop] under a translucent surface tint with an additive edge-light rim — the same
 * material recipe as the floating bottom bar — and falls back to a solid capsule without a live
 * backdrop or RuntimeShader. Press feedback is a glassy scale-up, no ripple.
 */
@Composable
fun GlassTopActionPill(
    actions: List<GlassTopAction>,
    backdrop: LayerBackdrop?,
    modifier: Modifier = Modifier,
) {
    if (actions.size < 2) {
        Row(modifier) {
            actions.forEach { action -> GlassTopIconButton(action, backdrop) }
        }
        return
    }
    val glass = rememberGlassTopActionStyle()
    val pillShape = remember { Capsule() }
    Box(
        modifier =
            modifier
                .height(GlassTopActionDefaults.ContainerSize)
                .graphicsLayer {
                    shape = pillShape
                    clip = false
                    shadowElevation = glass.shadowElevationPx
                    ambientShadowColor = glass.shadowColor
                    spotShadowColor = glass.shadowColor
                }
                .clip(pillShape)
                .glassSurface(
                    backdrop = backdrop,
                    shape = pillShape,
                    fallbackColor = glass.fallbackColor,
                    tint = glass.tint,
                )
                .edgeLight(
                    shape = pillShape,
                    color = glass.edgeLightColor,
                    width = glass.edgeLightWidth,
                    blurRadius = EdgeLightDefaults.BlurRadius,
                    blendMode = glass.edgeLightBlendMode,
                ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier =
                Modifier.padding(horizontal = GlassTopActionDefaults.PillHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GlassTopActionDefaults.PillIconGap),
        ) {
            actions.forEach { action -> PillActionIcon(action) }
        }
    }
}

/** Single frosted circle button, the one-icon variant of [GlassTopActionPill]'s chrome. */
@Composable
fun GlassTopIconButton(
    action: GlassTopAction,
    backdrop: LayerBackdrop?,
    modifier: Modifier = Modifier,
) {
    val glass = rememberGlassTopActionStyle()
    val circleShape = remember { Capsule() }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    // Reference LiquidTopBarButton press feedback: scale up by ~4dp over the container height.
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 1f + 4f / GlassTopActionDefaults.ContainerSize.value else 1f,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        label = "glass_top_action_press_scale",
    )
    Box(
        modifier =
            modifier
                .size(GlassTopActionDefaults.ContainerSize)
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                    shape = circleShape
                    clip = false
                    shadowElevation = glass.shadowElevationPx
                    ambientShadowColor = glass.shadowColor
                    spotShadowColor = glass.shadowColor
                }
                .clip(circleShape)
                .glassSurface(
                    backdrop = backdrop,
                    shape = circleShape,
                    fallbackColor = glass.fallbackColor,
                    tint = glass.tint,
                )
                .edgeLight(
                    shape = circleShape,
                    color = glass.edgeLightColor,
                    width = glass.edgeLightWidth,
                    blurRadius = EdgeLightDefaults.BlurRadius,
                    blendMode = glass.edgeLightBlendMode,
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = action.onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = action.icon,
            contentDescription = action.contentDescription,
            modifier = Modifier.size(GlassTopActionDefaults.IconSize),
            tint = glass.iconTint,
        )
    }
}

@Composable
private fun PillActionIcon(action: GlassTopAction) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.86f else 1f,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        label = "glass_top_pill_icon_press_scale",
    )
    val glass = rememberGlassTopActionStyle()
    Box(
        modifier =
            Modifier
                .height(GlassTopActionDefaults.ContainerSize)
                .width(GlassTopActionDefaults.IconSize + GlassTopActionDefaults.PillIconHitInset)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = action.onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = action.icon,
            contentDescription = action.contentDescription,
            modifier =
                Modifier.size(GlassTopActionDefaults.IconSize).graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                },
            tint = glass.iconTint,
        )
    }
}

/** Shared material recipe for the pill and circle containers, resolved per theme. */
private class GlassTopActionStyle(
    val fallbackColor: Color,
    val tint: Color,
    val shadowColor: Color,
    val shadowElevationPx: Float,
    val edgeLightColor: Color,
    val edgeLightWidth: Dp,
    val edgeLightBlendMode: BlendMode,
    val iconTint: Color,
)

@Composable
private fun rememberGlassTopActionStyle(): GlassTopActionStyle {
    val colorScheme = MiuixTheme.colorScheme
    val isLightTheme = colorScheme.background.luminance() >= 0.5f
    val opacity = AppTheme.opacity
    val density = LocalDensity.current
    val surfaceContainer = colorScheme.surfaceContainer
    return remember(isLightTheme, opacity, surfaceContainer, density) {
        GlassTopActionStyle(
            fallbackColor = surfaceContainer,
            // Over the top bar's own progressive blur the page behind is already washed out, so
            // the tint sits slightly denser than the bottom bar's to keep the chrome readable.
            tint = surfaceContainer.copy(alpha = if (isLightTheme) 0.62f else 0.72f),
            shadowColor =
                Color.Black.copy(
                    alpha = if (isLightTheme) opacity.softOverlay else opacity.surfaceSoft,
                ),
            shadowElevationPx = with(density) { UiDp.dp7.toPx() },
            edgeLightColor = EdgeLightDefaults.tint(surfaceContainer, isLightTheme),
            edgeLightWidth = EdgeLightDefaults.width(isLightTheme),
            edgeLightBlendMode = EdgeLightDefaults.blendMode(isLightTheme),
            iconTint = colorScheme.onSurface,
        )
    }
}
