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
import androidx.compose.foundation.background
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.presentation.theme.YumeBlur.glassSurface
import com.github.yumeyucca.yumebox.presentation.theme.isDarkTheme
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.glass.GlassAnchorSurface
import top.yukonga.miuix.kmp.glass.GlassDefaults
import top.yukonga.miuix.kmp.glass.GlassPopupAnchor
import top.yukonga.miuix.kmp.glass.GlassPopupAnchorElement
import top.yukonga.miuix.kmp.glass.GlassShape
import top.yukonga.miuix.kmp.glass.glassPanel
import top.yukonga.miuix.kmp.glass.glassPopupAnchor
import top.yukonga.miuix.kmp.glass.glassPopupAnchorContent
import top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults
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
    val modifier: Modifier = Modifier,
    val iconModifier: Modifier = Modifier,
    /** Icon tint override; [Color.Unspecified] keeps the chrome's own on-surface tint. */
    val tint: Color = Color.Unspecified,
    /**
     * Menu hosted by this button. Declaring it here keeps the menu next to the action that opens it,
     * so the pill never has to guess which overlay slot belongs to which icon.
     */
    val popup: (@Composable () -> Unit)? = null,
    /**
     * Anchor [popup] is placed against. The button binds itself to it, so call sites no longer have
     * to hand-wire `glassPopupAnchor` on [modifier] and `glassPopupAnchorContent` on [iconModifier].
     */
    val anchor: GlassPopupAnchor? = null,
)

/**
 * The glyph an anchored top-bar action keeps showing while its popup is collapsed into the button.
 * Shared so the anchor an icon hands to [GlassPopupHost] cannot drift in size or tint from the
 * glyph every menu redraws in its own header.
 */
@Composable
fun GlassTopActionAnchorIcon(imageVector: ImageVector) {
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        modifier = Modifier.size(GlassTopActionDefaults.IconSize),
        tint = MiuixTheme.colorScheme.onSurface,
    )
}

/** The popup anchor this action exposes through its own modifier, if any. */
private val GlassTopAction.popupAnchor: GlassPopupAnchor?
    get() =
        anchor ?: modifier.foldIn<GlassPopupAnchor?>(null) { found, element ->
            if (element is GlassPopupAnchorElement) element.anchor else found
        }

/** Binds this button's bounds to its own [GlassTopAction.anchor], if it declares one. */
private val GlassTopAction.anchorModifier: Modifier
    get() =
        anchor?.let {
            Modifier.glassPopupAnchor(
                anchor = it,
                cornerRadius = GlassTopActionDefaults.IconSize / 2,
                floating = true,
            )
        } ?: Modifier

/** Draws the collapsed glyph that [GlassPopupHost] morphs out of, when this button owns a menu. */
private val GlassTopAction.anchorIconModifier: Modifier
    get() = anchor?.let { Modifier.glassPopupAnchorContent(it) } ?: Modifier

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
            actions.forEach { action ->
                PillActionSlot(action) { GlassTopIconButton(action, backdrop) }
            }
        }
        return
    }
    val glass = rememberGlassTopActionStyle()
    // The action list is rebuilt on every recomposition (its modifiers and lambdas are new
    // instances), so the anchor list is not a usable effect key. Key the surface wiring on the
    // surface itself and read the current anchors through a snapshot-aware holder instead.
    val resolvedPopupAnchors = actions.mapNotNull { it.popupAnchor }.distinct()
    val currentPopupAnchors = rememberUpdatedState(resolvedPopupAnchors)
    val containerMaterial = GlassTopAppBarDefaults.buttonMaterial()
    val containerStroke = GlassTopAppBarDefaults.buttonStroke()
    val popupSurface = remember(backdrop, containerMaterial, containerStroke, glass.fallbackColor) {
        GlassAnchorSurface(
            backdrop = backdrop,
            style = GlassDefaults.Style,
            material = containerMaterial,
            underlayMaterial = null,
            stroke = containerStroke,
            fill = glass.fallbackColor,
        )
    }
    val collapsedSurfaceProgress = remember { mutableFloatStateOf(0f) }
    SideEffect {
        resolvedPopupAnchors.forEach { anchor ->
            anchor.surface = popupSurface
            anchor.surfaceProgress = collapsedSurfaceProgress
            anchor.surfaceOpacity = 1f
            anchor.surfaceFloating = false
        }
    }
    DisposableEffect(popupSurface) {
        onDispose {
            currentPopupAnchors.value.forEach { anchor ->
                if (anchor.surface === popupSurface) {
                    anchor.surface = null
                    anchor.surfaceProgress = null
                }
            }
        }
    }
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
                .glassPanel(
                    backdrop = backdrop,
                    shape = GlassShape(GlassTopActionDefaults.ContainerSize / 2),
                    style = GlassDefaults.Style,
                    material = containerMaterial,
                    stroke = containerStroke,
                    shadow = null,
                    shading = false,
                    fallback = Modifier.clip(pillShape).background(glass.fallbackColor),
                ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier =
                Modifier.padding(horizontal = GlassTopActionDefaults.PillHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GlassTopActionDefaults.PillIconGap),
        ) {
            actions.forEach { action ->
                PillActionSlot(action) { PillActionIcon(action, glass) }
            }
        }
    }
}

/**
 * One button of the pill together with the menu it hosts. The menu is painted in the same slot as its
 * own button, so an action carries its menu instead of the pill juggling parallel index lists.
 */
@Composable
private fun PillActionSlot(action: GlassTopAction, icon: @Composable () -> Unit) {
    Box {
        icon()
        action.popup?.let { popup -> Box(Modifier.matchParentSize()) { popup() } }
    }
}

/**
 * Single frosted circle button, the one-icon variant of [GlassTopActionPill]'s chrome.
 *
 * Always draws the app's own panel recipe rather than the library `GlassIconButton`: its rim only
 * reads once content scrolls underneath, which never happens behind a top bar at rest.
 */
@Composable
fun GlassTopIconButton(
    action: GlassTopAction,
    backdrop: LayerBackdrop?,
    modifier: Modifier = Modifier,
) {
    val glass = rememberGlassTopActionStyle()
    val circleShape = remember { Capsule() }
    val glassShape = remember { GlassShape(GlassTopActionDefaults.ContainerSize / 2) }
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
        // [GlassTopAction.modifier] belongs to the action, so it has to survive the one-icon path
        // exactly as it does on the pill path; dropping it silently broke anchor/inset overrides.
        modifier =
            action.modifier
                .then(modifier)
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
                .then(
                    if (backdrop != null && isRuntimeShaderSupported()) {
                        // Same material as the action pill, and never subject to the bar's
                        // scroll-driven floating state, so the circle cannot vanish at rest.
                        Modifier.glassPanel(
                            backdrop = backdrop,
                            shape = glassShape,
                            style = GlassDefaults.Style,
                            material = GlassTopAppBarDefaults.buttonMaterial(),
                            stroke = GlassTopAppBarDefaults.buttonStroke(),
                            shadow = null,
                            shading = false,
                            fallback = Modifier.clip(circleShape).background(glass.fallbackColor),
                        )
                    } else {
                        Modifier
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
                    },
                )
                .then(action.anchorModifier)
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
                Modifier
                    .size(GlassTopActionDefaults.IconSize)
                    .then(action.iconModifier)
                    .then(action.anchorIconModifier),
            tint = action.tint.takeOr(glass.iconTint),
        )
    }
}

@Composable
private fun PillActionIcon(action: GlassTopAction, glass: GlassTopActionStyle) {
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
    Box(
        modifier =
            action.modifier
                .then(action.anchorModifier)
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
                Modifier
                    .size(GlassTopActionDefaults.IconSize)
                    .then(action.iconModifier)
                    .then(action.anchorIconModifier)
                    .graphicsLayer {
                        scaleX = pressScale
                        scaleY = pressScale
                    },
            tint = action.tint.takeOr(glass.iconTint),
        )
    }
}

/** [fallback] unless this colour carries an explicit tint. */
private fun Color.takeOr(fallback: Color): Color =
    if (this == Color.Unspecified) fallback else this

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
    val isLightTheme = !isDarkTheme()
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
            shadowElevationPx = 0f,
            edgeLightColor = EdgeLightDefaults.tint(surfaceContainer, isLightTheme),
            edgeLightWidth = EdgeLightDefaults.width(isLightTheme),
            edgeLightBlendMode = EdgeLightDefaults.blendMode(isLightTheme),
            iconTint = colorScheme.onSurface,
        )
    }
}
