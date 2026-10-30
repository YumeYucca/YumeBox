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

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.*
import timber.log.Timber
import androidx.compose.ui.platform.LocalContext
import com.github.yumeyucca.yumebox.common.util.buildRemotePanelUrl
import com.github.yumeyucca.yumebox.common.util.requiresLocalNetworkPermission
import com.github.yumeyucca.yumebox.data.model.SubStorePanel
import com.github.yumeyucca.yumebox.data.store.FeatureStore
import com.github.yumeyucca.yumebox.data.store.RemoteControllerStore
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.navigation.Route
import com.github.yumeyucca.yumebox.presentation.navigation.RouteStack
import com.github.yumeyucca.yumebox.presentation.navigation.rememberMainPageModel
import com.github.yumeyucca.yumebox.presentation.screen.ProxyPager
import com.github.yumeyucca.yumebox.presentation.theme.LocalGlassBackdrop
import com.github.yumeyucca.yumebox.screen.home.HomeViewModel
import com.github.yumeyucca.yumebox.screen.moe.LocalUseSystemWallpaper
import com.github.yumeyucca.yumebox.screen.moe.MoeHomePage
import com.github.yumeyucca.yumebox.screen.profiles.ProfilesPager
import com.github.yumeyucca.yumebox.screen.rules.RulesProvidersTab
import com.github.yumeyucca.yumebox.screen.settings.AppSettingsViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.github.yumeyucca.yumebox.presentation.navigation.PaneRestingRoute

@Composable
internal fun MainScreen(
    navigator: Navigator,
    initialDestination: BottomBarDestination,
    onDestinationSettled: (BottomBarDestination) -> Unit,
    rootStack: RouteStack,
    detailStack: RouteStack,
) {
    val detailNavigator = detailStack.navigator
    val context = LocalContext.current
    val windowLayoutMode = rememberWindowLayoutMode()
    val appSettingsViewModel = koinViewModel<AppSettingsViewModel>()
    val homeViewModel = koinViewModel<HomeViewModel>()
    val featureStore = koinInject<FeatureStore>()
    val remoteControllerStore = koinInject<RemoteControllerStore>()
    val localNetworkGate = rememberLocalNetworkGate()
    val nodeSession by homeViewModel.nodeSession.collectAsState()
    val pageModel =
        rememberMainPageModel(
            initialDestination = initialDestination,
            everReady = nodeSession.everReady,
        )
    val mainPagerState = pageModel.pager

    val bottomBarAutoHideEnabled by appSettingsViewModel.bottomBarAutoHide.state.collectAsState()
    // Glass backdrop feeding the floating bottom bar's frosted capsule — force-enabled:
    // glass is the design now (solid fallback only on devices without RuntimeShader), the
    // legacy blur toggle no longer gates it. The capture draws an opaque surface first so
    // translucent pages don't bleed through (miuix-blur pattern); keyed on the surface color
    // so a theme switch re-records with the right base.
    val glassBackdrop = rememberSurfaceBackdrop(MiuixTheme.colorScheme.surface)
    val splitLeftFraction by appSettingsViewModel.splitLeftRatio.state.collectAsState()
    val moeWallpaperUri by appSettingsViewModel.moeWallpaperUri.state.collectAsState()
    val moeWallpaperZoom by appSettingsViewModel.moeWallpaperZoom.state.collectAsState()
    val moeWallpaperBiasX by appSettingsViewModel.moeWallpaperBiasX.state.collectAsState()
    val moeWallpaperBiasY by appSettingsViewModel.moeWallpaperBiasY.state.collectAsState()
    val useSystemWallpaper by appSettingsViewModel.useSystemWallpaper.state.collectAsState()
    val selectedPanelType by featureStore.selectedPanelType.state.collectAsState()
    val panelProtocol by featureStore.panelProtocol.state.collectAsState()
    val panelUrl =
        remember(selectedPanelType, panelProtocol) {
            SubStorePanel.fromIndex(selectedPanelType)?.url(panelProtocol).orEmpty()
        }
    // Remembered so the lambda keeps its identity: `MainRootPageState` is `@Immutable` and compares
    // by value, so a fresh lambda on every recomposition would make it always differ.
    val openNetworkPanel: () -> Unit =
        remember(panelUrl, context, remoteControllerStore, localNetworkGate) {
            {
                panelUrl.takeIf { it.isNotBlank() }?.let { panel ->
                    val backend = remoteControllerStore.activeBackend()
                    val targetUrl = backend?.let { buildRemotePanelUrl(panel, it) } ?: panel
                    localNetworkGate.run(
                        requiresPermission = backend?.requiresLocalNetworkPermission() == true,
                        action = { WebViewActivity.start(context, targetUrl) },
                    )
                }
            }
        }
    val bottomBarScrollBehavior =
        rememberBottomBarScrollBehavior(autoHideEnabled = bottomBarAutoHideEnabled)
    // A bound reference is a new object per recomposition, and the host compares its arguments.
    val onSplitLeftFractionChange: (Float) -> Unit =
        remember(appSettingsViewModel) { appSettingsViewModel::onSplitLeftRatioChange }
    val settledDestination = pageModel.settled
    val bottomBarVisible = pageModel.bottomBarVisible

    val vpnPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            homeViewModel.onVpnPermissionResult(result.resultCode == Activity.RESULT_OK)
        }

    val vpnPrepareIntent by homeViewModel.vpnPrepareIntent.collectAsState()
    LaunchedEffect(vpnPrepareIntent) {
        val intent = vpnPrepareIntent ?: return@LaunchedEffect
        // A stripped build can ship without the consent activity, and an exception out of the
        // launcher would escape this effect and take the composition down with it.
        val shown =
            runCatching { vpnPermissionLauncher.launch(intent) }
                .onFailure { Timber.e(it, "Could not show the VPN consent prompt") }
                .isSuccess
        homeViewModel.consumeVpnPrepareIntent()
        if (!shown) homeViewModel.onVpnPermissionResult(false)
    }

    val handlePageChange: (BottomBarDestination) -> Unit = remember(pageModel) { pageModel::show }

    LaunchedEffect(initialDestination, pageModel) {
        if (pageModel.settled != initialDestination) {
            pageModel.show(initialDestination)
        }
    }

    val usesSplitShell = windowLayoutMode.usesSplitShell

    // The right pane shows the current tab's side content: whatever the user opened there belongs to
    // the tab they opened it from, so leaving that tab takes the pane back to its resting route
    // instead of pinning a page across every other tab. The proxy tab keeps its pane, because the
    // pane is that tab's own content. Only a real tab change counts — the stack outlives this screen,
    // so a pane restored after a rotation or after returning from a pushed route survives the first
    // run of the effect.
    LaunchedEffect(settledDestination) {
        // Write-back happens on every settled destination, including the first one; the pane reset
        // below only on a real tab change, which is what [MainPageModel.previousSettled] answers.
        onDestinationSettled(settledDestination)
        if (pageModel.previousSettled == settledDestination) return@LaunchedEffect
        // Read here, not in the composition body: a pane push must not recompose the shell, and the
        // question only has to be answered when the tab actually changes.
        if (
            settledDestination != BottomBarDestination.Proxy &&
            detailNavigator.currentRoute != PaneRestingRoute
        ) {
            detailNavigator.replaceAll(listOf(PaneRestingRoute))
        }
    }

    MainScreenBackHandler(
        mainPagerState = mainPagerState,
        rootStack = rootStack,
        detailStack = detailStack,
        splitShell = usesSplitShell,
        onPopDetail = { detailNavigator.pop() },
    )

    // `LocalNavigator` is provided once by the route stack that hosts this screen; only the
    // right-pane navigator is page-specific, so it is the one thing installed here.
    CompositionLocalProvider(
        LocalDetailNavigator provides if (usesSplitShell) detailNavigator else null,
        LocalMainPagerState provides mainPagerState,
        LocalHandlePageChange provides handlePageChange,
        LocalBottomBarScrollBehavior provides bottomBarScrollBehavior,
        LocalGlassBackdrop provides glassBackdrop,
        LocalUseSystemWallpaper provides useSystemWallpaper,
    ) {
        MainContentHost(
            windowLayoutMode = windowLayoutMode,
            mainPagerState = mainPagerState,
            tabs = pageModel.tabs,
            bottomBarVisible = bottomBarVisible,
            home =
                MoeHomeFrame(
                    wallpaperUri = moeWallpaperUri,
                    wallpaperZoom = moeWallpaperZoom,
                    wallpaperBiasX = moeWallpaperBiasX,
                    wallpaperBiasY = moeWallpaperBiasY,
                    pageProgress = pageModel.homeVisibility,
                ),
            detailStack = detailStack,
            splitLeftFraction = splitLeftFraction,
            onSplitLeftFractionChange = onSplitLeftFractionChange,
            onOpenPanel = openNetworkPanel,
        )
    }
}

@Composable
private fun MainScreenBackHandler(
    mainPagerState: MainPagerState,
    rootStack: RouteStack,
    detailStack: RouteStack,
    splitShell: Boolean,
    onPopDetail: () -> Unit,
) {
    val root by rootStack.value.subscribeAsState()
    val detail by detailStack.value.subscribeAsState()
    val canPopRoute = root.backStack.isNotEmpty()
    val canPopDetail = splitShell && detail.backStack.isNotEmpty()
    val canReturnHome = mainPagerState.selectedPage != 0 && !canPopRoute && !canPopDetail
    // A route the split shell pushed into the detail pane closes before back is allowed to leave the
    // page, so the two handlers are never enabled at the same time.
    BackHandler(enabled = canReturnHome) { mainPagerState.animateToPage(0) }
    BackHandler(enabled = canPopDetail) { onPopDetail() }
}

/**
 * One frame of the Moe home's animated chrome: the wallpaper transform plus the page progress
 * that fades it in. These values always travel together, so the pager hosts forward them as a
 * single value instead of one parameter each.
 */
@Immutable
internal data class MoeHomeFrame(
    val wallpaperUri: String,
    val wallpaperZoom: Float,
    val wallpaperBiasX: Float,
    val wallpaperBiasY: Float,
    val pageProgress: State<Float>,
)

@Immutable
internal data class MainRootPageState(
    val destination: BottomBarDestination,
    val mainInnerPadding: PaddingValues,
    val home: MoeHomeFrame,
    val isActive: Boolean,
    val windowLayoutMode: WindowLayoutMode,
    val onOpenPanel: () -> Unit,
)

@Composable
internal fun MainRootPageContent(state: MainRootPageState) {
    val navigator = LocalNavigator.current
    val detailNavigator = LocalDetailNavigator.current
    val openRoutingFromProxy: () -> Unit = {
        (detailNavigator ?: navigator).push(Route.RulesAndProviders(RulesProvidersTab.Rules))
    }
    when (state.destination) {
        BottomBarDestination.Home -> {
            MoeHomePage(
                wallpaperUri = state.home.wallpaperUri,
                wallpaperZoom = state.home.wallpaperZoom,
                wallpaperBiasX = state.home.wallpaperBiasX,
                wallpaperBiasY = state.home.wallpaperBiasY,
                isActive = state.isActive,
                pageProgress = state.home.pageProgress,
                onOpenPanel = state.onOpenPanel,
                windowLayoutMode = state.windowLayoutMode,
            )
        }

        BottomBarDestination.Proxy -> {
            ProxyPager(
                mainInnerPadding = state.mainInnerPadding,
                onOpenRouting = openRoutingFromProxy,
                isActive = state.isActive,
            )
        }

        BottomBarDestination.Config -> {
            ProfilesPager(state.mainInnerPadding)
        }
    }
}
