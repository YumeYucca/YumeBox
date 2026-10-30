/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * Copyright (c) YumeYucca 2025 - Present
 */

@file:Suppress("FunctionName")

package com.github.yumeyucca.yumebox.screen.moe

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.yumeyucca.yumebox.domain.model.TrafficData
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.screen.home.HomeProxyControlState
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.getRoundedCorner

internal data class MoeHomeLayoutState(
    val wallpaperUri: String,
    val wallpaperZoom: Float,
    val wallpaperBiasX: Float,
    val wallpaperBiasY: Float,
    val statusBarTop: Dp,
    // Pager progress is read where it is drawn, not here: it changes on every scroll frame, and a
    // plain Float in this state would recompose the whole home layout as the pager moves.
    val pageProgress: State<Float>,
    val sidebarProgress: State<Float>,
    val sidebarToggleProgress: Float,
    val duration: MoeDurationPair,
    val batteryPercent: Int?,
    val sidebarIcons: List<MoeSidebarIconItem>,
    val contentSurface: Color,
    val isRunning: Boolean,
    val traffic: TrafficData,
    val selectedServerName: String?,
    val selectedServerPing: Int?,
    val now: Long,
    val quote: String,
    val quoteAuthor: String,
    val controlState: HomeProxyControlState,
    val canLaunch: Boolean,
    val isRemoteController: Boolean,
    val hasUpdate: Boolean,
)

internal class MoeHomeActions(
    val toggleSidebar: () -> Unit,
    val pickWallpaper: () -> Unit,
    val openSettings: () -> Unit,
    val toggleProxy: () -> Unit,
    val openUpdate: () -> Unit,
)

/** Which home layout to compose; the window layout mode decides once, at the call site. */
internal enum class MoeHomeLayoutMode {
    Phone,
    Tablet,
}

/** Where the hero card sits and how big it is; both layouts share the one derived value. */
internal data class MoeHeroGeometry(val top: Dp, val height: Dp, val radius: Dp)

private fun moeHeroGeometry(statusBarTop: Dp, maxHeight: Dp, deviceCorner: Dp): MoeHeroGeometry =
    MoeHeroGeometry(
        top = statusBarTop,
        height = (maxHeight - statusBarTop).coerceAtLeast(UiDp.dp0) * MoeUi.Hero.heightFraction,
        radius = MoeUi.Shape.heroCornerRadius(deviceCorner),
    )

@Composable
context(actions: MoeHomeActions)
internal fun MoeHomeLayout(state: MoeHomeLayoutState, layout: MoeHomeLayoutMode) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val greetingColor =
            rememberMoeWallpaperAccentColor(
                wallpaperUri = state.wallpaperUri,
                surface = state.contentSurface,
                fallback = MiuixTheme.colorScheme.primary,
            )
        // Read the device corner once and floor it once: the panel, its decoration and the hero
        // card all inherit the same radius, so a device that reports a flat corner cannot leave
        // any of them square.
        val deviceCorner = MoeUi.Shape.panelCorner(getRoundedCorner())
        val hero =
            moeHeroGeometry(
                statusBarTop = state.statusBarTop,
                maxHeight = maxHeight,
                deviceCorner = deviceCorner,
            )
        when (layout) {
            MoeHomeLayoutMode.Phone ->
                MoePhoneHomeLayout(state, greetingColor, maxWidth, maxHeight, hero, deviceCorner)
            MoeHomeLayoutMode.Tablet ->
                MoeTabletHomeLayout(state, greetingColor, maxWidth, maxHeight, hero)
        }
    }
}

@Composable
context(actions: MoeHomeActions)
private fun BoxScope.MoePhoneHomeLayout(
    state: MoeHomeLayoutState,
    greetingColor: Color,
    maxWidth: Dp,
    maxHeight: Dp,
    hero: MoeHeroGeometry,
    screenCorner: Dp,
) {
    val density = LocalDensity.current
    // Dedicated layer backdrop so the sidebar glass blurs only the wallpaper layer, not the
    // content panel.
    val sidebarBackdrop = rememberLayerBackdrop { drawContent() }
    val sidebarWidth = maxWidth * MoeUi.Sidebar.fraction
    val contentStart = (sidebarWidth - MoeUi.Sidebar.contentOverlap).coerceAtLeast(UiDp.dp0)
    val sidebarDecorationWidth = maxOf(sidebarWidth, contentStart + screenCorner)
    val sidebar = state.sidebarProgress.value.coerceIn(0f, 1f) * state.sidebarToggleProgress
    val sidebarWidthVisible = lerpDp(MoeUi.Sidebar.collapsedVisibleWidth, contentStart, sidebar)
    // Glass blur needs a RuntimeShader (API 33+); miuix falls back to a solid surface without
    // one. Skip the effect entirely while the rail is fully collapsed.
    val blurEnabled = sidebar > 0.03f
    MoeWallpaperBackground(
        wallpaperUri = state.wallpaperUri,
        wallpaperZoom = state.wallpaperZoom,
        wallpaperBiasX = state.wallpaperBiasX,
        wallpaperBiasY = state.wallpaperBiasY,
        qualityMode = MoeWallpaperQualityMode.BackgroundBlur,
        modifier = Modifier
            .matchParentSize()
            .layerBackdrop(sidebarBackdrop),
    )
    MoeSidebarDecoration(
        backdrop = sidebarBackdrop,
        blurEnabled = blurEnabled,
        blurProgress = sidebar,
        modifier =
            Modifier
                .align(Alignment.CenterStart)
                .width(sidebarDecorationWidth)
                .fillMaxHeight()
                .graphicsLayer {
                    translationX = with(density) { lerpDp((-56).dp, UiDp.dp0, sidebar).toPx() }
                    alpha = lerpFloat(0.78f, 1f, sidebar) * state.pageProgress.value.coerceIn(0f, 1f)
                },
    ) {
        MoeSidebarContent(
            topValue = state.duration.top,
            bottomValue = state.duration.bottom,
            batteryPercent = state.batteryPercent,
            icons = state.sidebarIcons,
            visibleWidth = sidebarWidthVisible,
        )
    }
    MoeHomePanel(
        state = state,
        greetingColor = greetingColor,
        contentStart = contentStart,
        hero = hero,
        sidebar = sidebar,
        screenCorner = screenCorner,
    )
}

@Composable
context(actions: MoeHomeActions)
private fun MoeHomePanel(
    state: MoeHomeLayoutState,
    greetingColor: Color,
    contentStart: Dp,
    hero: MoeHeroGeometry,
    sidebar: Float,
    screenCorner: Dp,
) {
    val corner = lerpDp(UiDp.dp0, screenCorner, sidebar)
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(start = lerpDp(UiDp.dp0, contentStart, sidebar))
                .background(
                    color = state.contentSurface,
                    shape =
                        RoundedCornerShape(
                            topStart = corner,
                            bottomStart = corner,
                        ),
                ),
    ) {
        MoeHeroCard(
            state = state,
            geometry = hero,
            modifier = Modifier.align(Alignment.TopStart),
            onDoubleTap = actions.toggleSidebar,
        )
        MoeHomeCopySection(
            state = state,
            greetingColor = greetingColor,
            modifier = Modifier.align(Alignment.TopStart).fillMaxWidth(),
            topPadding = hero.top + hero.height + MoeUi.Hero.belowHeroTopGap,
        )
    }
}

/**
 * Hero card shell shared by both layouts: [MoeHeroOverlay] owns wallpaper, scrim and runtime info;
 * this only owns the card frame, its gestures and the page-turn transform.
 */
@Composable
context(actions: MoeHomeActions)
private fun MoeHeroCard(
    state: MoeHomeLayoutState,
    geometry: MoeHeroGeometry,
    modifier: Modifier = Modifier,
    onDoubleTap: (() -> Unit)? = null,
    headerContent: @Composable BoxScope.() -> Unit = {},
) {
    val pickWallpaper by rememberUpdatedState(actions.pickWallpaper)
    val doubleTap = rememberUpdatedState(onDoubleTap)
    Box(
        modifier
            .fillMaxWidth()
            .padding(
                start = MoeUi.Hero.containerHorizontalInset,
                end = MoeUi.Hero.containerHorizontalInset,
                top = geometry.top,
            )
            .height(geometry.height)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { doubleTap.value?.invoke() },
                    onLongPress = { pickWallpaper() },
                )
            }
            .graphicsLayer {
                transformOrigin = TransformOrigin(0.5f, 0f)
                // At rest the eased term is exactly 0, so the hero sits at scale 1 without a
                // special case. Derived here rather than in the caller so a page turn only
                // re-runs this layer block instead of recomposing the card.
                val pageTurn =
                    FastOutSlowInEasing.transform(1f - state.pageProgress.value.coerceIn(0f, 1f))
                val scale = lerpFloat(1f, MoeUi.Hero.pageTurnScale, pageTurn)
                scaleX = scale
                scaleY = scale
            }
            .squircleClip(geometry.radius),
    ) {
        MoeHeroOverlay(state, headerContent)
    }
}

@Composable
private fun BoxScope.MoeHeroOverlay(
    state: MoeHomeLayoutState,
    headerContent: @Composable BoxScope.() -> Unit = {},
) {
    MoeWallpaperBackground(
        wallpaperUri = state.wallpaperUri,
        wallpaperZoom = state.wallpaperZoom,
        wallpaperBiasX = state.wallpaperBiasX,
        wallpaperBiasY = state.wallpaperBiasY,
        qualityMode = MoeWallpaperQualityMode.Foreground,
        modifier = Modifier.matchParentSize(),
    )
    Box(
        Modifier
            .matchParentSize()
            .background(
                Brush.verticalGradient(
                    // Fully opaque from 0.92 so the rounded corner band never lets the
                    // wallpaper bleed through as a colored line (issue #151 item 6); the
                    // longer fade avoids the hard mid-way cut (item 5).
                    0f to Color.Transparent,
                    0.52f to Color.Transparent,
                    0.82f to state.contentSurface.copy(alpha = 0.88f),
                    0.92f to state.contentSurface,
                    1f to state.contentSurface,
                )
            )
    )
    headerContent()
    AnimatedVisibility(
        visible = state.isRunning,
        modifier =
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(
                    start = MoeUi.Hero.contentHorizontalInset,
                    end = MoeUi.Hero.contentHorizontalInset,
                    bottom = MoeUi.Hero.trafficBottomInset,
                ),
        enter = fadeIn() + slideInVertically { it / 3 },
        exit = fadeOut() + slideOutVertically { it / 3 },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MoeUi.Hero.runtimeInfoTopGap)) {
            MoeTrafficStrip(state.traffic.download, state.traffic.upload)
            MoeHomeInfoPanel(
                serverName = state.selectedServerName.takeIf { state.isRunning },
                serverPing = state.selectedServerPing.takeIf { state.isRunning },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
context(actions: MoeHomeActions)
private fun MoeHomeCopySection(
    state: MoeHomeLayoutState,
    greetingColor: Color,
    modifier: Modifier,
    topPadding: Dp,
    bottomPadding: Dp = UiDp.dp0,
) {
    val horizontalPadding =
        MoeUi.Hero.containerHorizontalInset + MoeUi.Hero.contentHorizontalInset
    Column(
        modifier =
            modifier.padding(
                start = horizontalPadding,
                end = horizontalPadding,
                top = topPadding,
                bottom = bottomPadding,
            ),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(MoeUi.Hero.belowHeroContentGap),
    ) {
        MoeHomeCopyBlock(
            nowMillis = state.now,
            quoteText = state.quote,
            quoteAuthor = state.quoteAuthor,
            greetingColor = greetingColor,
            color = MiuixTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth(),
        ) {
            MoeLaunchControls(
                controlState = state.controlState,
                enabled = state.canLaunch && state.controlState.canInteract,
                isRemoteController = state.isRemoteController,
                surfaceColor = state.contentSurface,
                modifier = Modifier.fillMaxWidth(),
                onSettingsClick = actions.openSettings,
                onLaunchClick = actions.toggleProxy,
                hasUpdate = state.hasUpdate,
                onUpdateClick = actions.openUpdate,
            )
        }
    }
}

/** A pane too short for the hero and the copy block to share the height scrolls as one column. */
private enum class MoeTabletFlow {
    Scrolling,
    Filling,
}

// Keep the Moe panel readable on ultra-wide screens without a right-side config pane.
private val TabletPanelMaxWidth = UiDp.dp560

// Below this height the pane can no longer pin the hero above a weighted copy block.
private val TabletShortWindow = UiDp.dp560

private fun moeTabletPanelModifier(
    flow: MoeTabletFlow,
    horizontalGutter: Dp,
    scrollState: ScrollState,
): Modifier =
    when (flow) {
        MoeTabletFlow.Scrolling ->
            Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalGutter)
                .verticalScroll(scrollState)
        MoeTabletFlow.Filling ->
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = horizontalGutter)
    }

private fun ColumnScope.moeTabletCopyModifier(flow: MoeTabletFlow): Modifier =
    when (flow) {
        MoeTabletFlow.Scrolling -> Modifier.fillMaxWidth()
        MoeTabletFlow.Filling -> Modifier.fillMaxWidth().weight(1f, fill = true)
    }

@Composable
context(actions: MoeHomeActions)
private fun MoeTabletHomeLayout(
    state: MoeHomeLayoutState,
    greetingColor: Color,
    maxWidth: Dp,
    maxHeight: Dp,
    hero: MoeHeroGeometry,
) {
    val contentMaxWidth = minOf(maxWidth, TabletPanelMaxWidth)
    // Phone-landscape panes are barely wider than the content itself; a floor keeps the launch
    // controls clear of the divider and screen edges instead of pressing against them.
    val horizontalGutter = ((maxWidth - contentMaxWidth) / 2f).coerceIn(UiDp.dp16, maxWidth / 2f)
    val flow =
        if (maxHeight < TabletShortWindow) MoeTabletFlow.Scrolling else MoeTabletFlow.Filling
    val scrollState = rememberScrollState()

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(state.contentSurface),
    ) {
        Column(moeTabletPanelModifier(flow, horizontalGutter, scrollState)) {
            MoeHeroCard(state = state, geometry = hero) {
                Row(
                    modifier =
                        Modifier
                            .align(Alignment.TopStart)
                            .fillMaxWidth()
                            .padding(
                                start = MoeUi.Hero.contentHorizontalInset,
                                end = MoeUi.Hero.contentHorizontalInset,
                                top = UiDp.dp14,
                            ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${state.duration.top}:${state.duration.bottom}",
                        color = Color.White.copy(alpha = 0.94f),
                        style = MiuixTheme.textStyles.title3,
                    )
                    state.batteryPercent?.let { percent ->
                        Text(
                            text = "$percent%",
                            color = Color.White.copy(alpha = 0.86f),
                            style = MiuixTheme.textStyles.body2,
                        )
                    }
                }
            }

            MoeHomeCopySection(
                state = state,
                greetingColor = greetingColor,
                modifier = moeTabletCopyModifier(flow),
                topPadding = MoeUi.Hero.belowHeroTopGap,
                bottomPadding = UiDp.dp12,
            )
        }
    }
}
