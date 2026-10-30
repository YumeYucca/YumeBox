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

package com.github.yumeyucca.yumebox

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalLayoutDirection
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.navigation.Route
import com.github.yumeyucca.yumebox.screen.rules.RulesProvidersTab
import com.github.yumeyucca.yumebox.presentation.navigation.RouteStackHost
import com.github.yumeyucca.yumebox.presentation.navigation.rememberRouteStack
import com.github.yumeyucca.yumebox.presentation.theme.verticalBounceContentTransform
import com.github.yumeyucca.yumebox.presentation.screen.ProxyShellNodeDetail
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.presentation.theme.LocalGlassBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.basic.Scaffold
import com.github.yumeyucca.yumebox.presentation.navigation.PaneRestingRoute

/**
 * Hosts the main pager (and optional tablet dual-pane shell).
 *
 * Split shell: each pane owns a Scaffold root so sheets/dialogs stay pane-local and can cover the
 * floating bottom bar on the left without spanning the full window.
 */
@Composable
internal fun MainContentHost(
    windowLayoutMode: WindowLayoutMode,
    mainPagerState: MainPagerState,
    tabs: BottomBarTabs,
    bottomBarVisible: Boolean,
    home: MoeHomeFrame,
    detailNavigator: Navigator,
    splitLeftFraction: Float,
    onSplitLeftFractionChange: (Float) -> Unit,
    onOpenPanel: () -> Unit,
) {
    val visibleBottomBarReservedHeight = rememberBottomBarReservedHeight()
    // The bar yields to scrolling, and the space it takes has to yield with it: reserving a full
    // bar's worth of padding while it is auto-hidden left a dead band under every list.
    val bottomBarScrollBehavior = LocalBottomBarScrollBehavior.current
    val barShown = bottomBarVisible && (bottomBarScrollBehavior?.isBottomBarVisible ?: true)
    val bottomBarReservedHeight by
    animateDpAsState(
        targetValue = if (barShown) visibleBottomBarReservedHeight else UiDp.dp0,
        // Matches the bar's own hide duration (BottomBar's exit translate): a longer tween would keep
        // a strip of padding after the bar has already left the screen.
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "main_bottom_bar_reserved_height",
    )
    val pagerFlingBehavior = rememberMainPagerFlingBehavior(mainPagerState.pagerState)
    // The split shell's left pane is only 280–420dp wide; rendering the phone Moe layout there
    // clips the rail clock and squeezes the panel against the divider (issue #151 items 1/3/8),
    // so the pager inside the pane always uses the tablet (card) home layout.
    val leftLayoutMode = WindowLayoutMode.RailSingle
    // One detail stack for the whole screen: it must outlive the pane switch, otherwise every
    // split↔compact toggle builds a new ChildStack subscribing to the same navigator.
    val detailStack = rememberRouteStack(detailNavigator)

    if (windowLayoutMode.usesSplitShell) {
        DualPaneLayout(
            left = {
                Scaffold { leftPadding ->
                    MainPagerHost(
                        layoutMode = leftLayoutMode,
                        mainInnerPadding =
                            paneInnerPadding(
                                scaffoldPadding = leftPadding,
                                reserveBottomBar = true,
                                bottomBarReservedHeight = bottomBarReservedHeight,
                                horizontalInset = PaneHorizontalInset.Start,
                            ),
                        mainPagerState = mainPagerState,
                        pagerFlingBehavior = pagerFlingBehavior,
                        tabs = tabs,
                        bottomBarVisible = bottomBarVisible,
                        home = home,
                        onOpenPanel = onOpenPanel,
                    )
                }
            },
            right = {
                Scaffold { rightPadding ->
                    val rightInnerPadding =
                        paneInnerPadding(
                            scaffoldPadding = rightPadding,
                            // The bar belongs to the shell's own bottom, not to this pane.
                            reserveBottomBar = false,
                            bottomBarReservedHeight = bottomBarReservedHeight,
                            horizontalInset = PaneHorizontalInset.End,
                        )
                    // The pane shows the node detail only while the stack still holds nothing but the
                    // placeholder root that the proxy tab resets to: anything the user opened (a deep
                    // link, the routing page) therefore always wins over the node pane.
                    // Remembered so the node pane keeps skipping: a fresh lambda would make it
                    // recompose every time this pane does.
                    val openRoutingInPane: () -> Unit =
                        remember(detailNavigator) {
                            {
                                detailNavigator.openRoute(
                                    Route.RulesAndProviders(RulesProvidersTab.Rules),
                                )
                            }
                        }
                    val showProxyNodes =
                        tabs.settled == BottomBarDestination.Proxy &&
                            detailNavigator.backStack.lastOrNull() == PaneRestingRoute
                    AnimatedContent(
                        targetState = showProxyNodes,
                        modifier = Modifier.fillMaxSize(),
                        transitionSpec = { verticalBounceContentTransform(forward = targetState) },
                        label = "split_shell_right_pane",
                    ) { nodesVisible ->
                        if (nodesVisible) {
                            ProxyShellNodeDetail(
                                mainInnerPadding = rightInnerPadding,
                                onOpenRouting = openRoutingInPane,
                            )
                        } else {
                            RouteStackHost(stack = detailStack)
                        }
                    }
                }
            },
            initialLeftFraction = splitLeftFraction,
            minLeftWidth = UiDp.dp280,
            maxLeftWidth = UiDp.dp420,
            minRightWidth = UiDp.dp320,
            onLeftFractionChange = onSplitLeftFractionChange,
        )
    } else {
        Scaffold { innerPadding ->
            MainPagerHost(
                layoutMode = windowLayoutMode,
                mainInnerPadding =
                    paneInnerPadding(
                        scaffoldPadding = innerPadding,
                        reserveBottomBar = true,
                        bottomBarReservedHeight = bottomBarReservedHeight,
                        horizontalInset = PaneHorizontalInset.Both,
                    ),
                mainPagerState = mainPagerState,
                pagerFlingBehavior = pagerFlingBehavior,
                tabs = tabs,
                bottomBarVisible = bottomBarVisible,
                home = home,
                onOpenPanel = onOpenPanel,
            )
        }
    }
}

/** Which screen edges a pane actually touches; the divider-facing edge has no system bar to clear. */
private enum class PaneHorizontalInset { Both, Start, End }

/**
 * Inner padding of a pager pane: the Scaffold's own insets plus the floating bottom bar's reserved
 * height when the pane draws the bar, and the system bars horizontally on whichever side the pane
 * sits.
 */
@Composable
private fun paneInnerPadding(
    scaffoldPadding: PaddingValues,
    reserveBottomBar: Boolean,
    bottomBarReservedHeight: Dp,
    horizontalInset: PaneHorizontalInset,
): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current
    val bottomExtra = bottomBarReservedHeight.takeIf { reserveBottomBar } ?: UiDp.dp0
    val systemBars = WindowInsets.systemBars.asPaddingValues()
    val startInset = systemBars.calculateStartPadding(layoutDirection)
    val endInset = systemBars.calculateEndPadding(layoutDirection)
    return PaddingValues(
        top = scaffoldPadding.calculateTopPadding(),
        bottom = scaffoldPadding.calculateBottomPadding() + bottomExtra,
        start = startInset.takeIf { horizontalInset != PaneHorizontalInset.End } ?: UiDp.dp0,
        end = endInset.takeIf { horizontalInset != PaneHorizontalInset.Start } ?: UiDp.dp0,
    )
}

@Composable
private fun MainPagerHost(
    layoutMode: WindowLayoutMode,
    mainInnerPadding: PaddingValues,
    mainPagerState: MainPagerState,
    pagerFlingBehavior: TargetedFlingBehavior,
    tabs: BottomBarTabs,
    bottomBarVisible: Boolean,
    home: MoeHomeFrame,
    onOpenPanel: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        HorizontalPager(
            modifier =
                Modifier
                    .fillMaxSize()
                    .let { modifier ->
                        val glassBackdrop = LocalGlassBackdrop.current
                        if (glassBackdrop != null) modifier.layerBackdrop(glassBackdrop) else modifier
                    },
            state = mainPagerState.pagerState,
            beyondViewportPageCount = (tabs.visible.size - 1).coerceAtLeast(0),
            flingBehavior = pagerFlingBehavior,
            userScrollEnabled = true,
            overscrollEffect = null,
            pageNestedScrollConnection = NoOpNestedScrollConnection,
        ) { page ->
            val destination = tabs.destinationAt(page)
            MainRootPageContent(
                state = MainRootPageState(
                    destination = destination,
                    mainInnerPadding = mainInnerPadding,
                    home = home,
                    isActive = destination == tabs.settled,
                    windowLayoutMode = layoutMode,
                    onOpenPanel = onOpenPanel,
                ),
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
        ) {
            BottomBarContent(
                isVisible = bottomBarVisible,
                destinations = tabs.visible,
            )
        }
    }
}
