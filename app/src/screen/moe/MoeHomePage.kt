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

@file:Suppress("FunctionName", "KotlinDeprecation")

package com.github.yumeyucca.yumebox.screen.moe

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.view.WindowCompat
import com.github.yumeyucca.yumebox.common.util.toast
import com.github.yumeyucca.yumebox.core.util.PollingTimerSpecs
import com.github.yumeyucca.yumebox.core.util.PollingTimers
import com.github.yumeyucca.yumebox.domain.model.TrafficData
import com.github.yumeyucca.yumebox.presentation.component.WindowLayoutMode
import com.github.yumeyucca.yumebox.presentation.component.BottomBarDestination
import com.github.yumeyucca.yumebox.presentation.component.LocalDetailNavigator
import com.github.yumeyucca.yumebox.presentation.component.LocalHandlePageChange
import com.github.yumeyucca.yumebox.presentation.component.LocalNavigator
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.Zashboard
import com.github.yumeyucca.yumebox.presentation.icon.yume.Bolt
import com.github.yumeyucca.yumebox.presentation.icon.yume.PackageCheck
import com.github.yumeyucca.yumebox.presentation.icon.yume.Palette
import com.github.yumeyucca.yumebox.presentation.navigation.Route
import com.github.yumeyucca.yumebox.presentation.theme.AnimationSpecs
import com.github.yumeyucca.yumebox.presentation.theme.isDark
import com.github.yumeyucca.yumebox.presentation.util.findActivity
import com.github.yumeyucca.yumebox.screen.home.HomeProxyControlState
import com.github.yumeyucca.yumebox.screen.home.HomeViewModel
import com.github.yumeyucca.yumebox.screen.settings.AppSettingsViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.theme.MiuixTheme

// The moe home backs its translucent cards with paper white in light mode; the themed surface
// reads as grey next to the wallpaper.
private val MoeHomeSurfaceLight = Color.White

@Composable
fun MoeHomePage(
    wallpaperUri: String,
    wallpaperZoom: Float = 1f,
    wallpaperBiasX: Float = 0f,
    wallpaperBiasY: Float = 0f,
    isActive: Boolean,
    /** Pager progress, handed down as state and read where it is drawn. */
    pageProgress: State<Float>,
    onOpenPanel: (() -> Unit)? = null,
    windowLayoutMode: WindowLayoutMode = WindowLayoutMode.Compact,
) {
    val homeViewModel = koinViewModel<HomeViewModel>()
    val appSettingsViewModel = koinViewModel<AppSettingsViewModel>()
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val screen by homeViewModel.screenState.collectAsState()
    val moe by appSettingsViewModel.moeHomeSectionState.collectAsState()
    val moeHomeQuoteAuthor by appSettingsViewModel.moeHomeQuoteAuthor.state.collectAsState()
    val sidebarExpanded = moe.sidebarExpanded
    val useSystemWallpaper = moe.useSystemWallpaper
    val statusBarSpacing by appSettingsViewModel.moeStatusBarSpacing.state.collectAsState()

    val statusBarInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val statusBarTop =
        if (statusBarSpacing) statusBarInset else MoeUi.Hero.containerHorizontalInset
    // The window picks the layout once here; neither layout composable inspects the window mode.
    val homeLayout =
        if (windowLayoutMode.usesSplitShell) MoeHomeLayoutMode.Tablet
        else MoeHomeLayoutMode.Phone

    val batteryPercent = rememberMoeBatteryPercent(context)
    var showHomeSettingsSheet by remember { mutableStateOf(false) }
    var wallpaperRefreshKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(isActive) {
        homeViewModel.setHomeScreenActive(isActive)
        if (isActive) {
            homeViewModel.reconcileRuntimeState()
        }
    }

    DisposableEffect(homeViewModel) { onDispose { homeViewModel.setHomeScreenActive(false) } }

    DisposableEffect(lifecycleOwner, homeViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                homeViewModel.reconcileRuntimeState()
                wallpaperRefreshKey++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(screen.uiError) {
        screen.uiError?.let { error ->
            context.toast(error, Toast.LENGTH_LONG)
            homeViewModel.consumeError()
        }
    }
    LaunchedEffect(screen.uiMessage) {
        screen.uiMessage?.let { message ->
            context.toast(message, Toast.LENGTH_SHORT)
            homeViewModel.consumeMessage()
        }
    }

    // Tick once a second whether running or idle: running drives the elapsed timer, idle drives the
    // wall-clock shown in the rail so it always reflects the real time instead of a frozen 00:00.
    val now by
    produceState(initialValue = System.currentTimeMillis()) {
        PollingTimers.ticks(PollingTimerSpecs.MoeElapsedClock).collect {
            value = System.currentTimeMillis()
        }
    }
    val startedAt = screen.runtimeStartedAt
    val isRunning = screen.controlState == HomeProxyControlState.Running
    val elapsedMillis =
        if (isRunning && startedAt != null && !screen.isRemoteController) {
            (now - startedAt).coerceAtLeast(0L)
        } else {
            0L
        }
    val durationPair =
        remember(isRunning, screen.isRemoteController, elapsedMillis, now) {
            if (isRunning && !screen.isRemoteController) {
                formatMoeDuration(elapsedMillis)
            } else {
                formatMoeClock(now)
            }
        }
    val trafficData =
        remember(screen.trafficNow, isRunning) {
            if (isRunning) TrafficData.from(screen.trafficNow) else TrafficData.zero
        }
    val isDarkHomeSurface = moe.themeMode.isDark()
    val contentSurface = if (isDarkHomeSurface) MiuixTheme.colorScheme.surface else MoeHomeSurfaceLight
    HomeStatusBarAppearance(
        isActive = isActive,
        statusBarSpacing = statusBarSpacing,
        isDarkHomeSurface = isDarkHomeSurface,
    )
    val handlePageChange = LocalHandlePageChange.current
    val rootNavigator = LocalNavigator.current
    val detailNavigator = LocalDetailNavigator.current
    // Settings is no longer a pager tab: the sidebar entry pushes it as a standalone page
    // (replacing the detail pane inside the tablet shell).
    val openSettingsPage: () -> Unit =
        remember(rootNavigator, detailNavigator) {
            val targetNavigator = detailNavigator ?: rootNavigator
            { targetNavigator.push(Route.Settings) }
        }
    val quoteText = moe.moeHomeQuote.ifBlank { YumeTxt.AppSettings.Interface.HomeQuoteDefault }
    val animatedSidebarToggleProgress by
    animateFloatAsState(
        targetValue = if (sidebarExpanded) 1f else 0f,
        animationSpec =
            tween(
                durationMillis = if (sidebarExpanded) 420 else 320,
                easing =
                    if (sidebarExpanded) {
                        AnimationSpecs.EmphasizedDecelerate
                    } else {
                        AnimationSpecs.EmphasizedAccelerate
                    },
            ),
        label = "moe_sidebar_toggle",
    )

    // Order matters: stopping must never wait on the profile list, and a remote controller only
    // mirrors another device's state instead of toggling it.
    val handleProxyAction: () -> Unit = {
        val profile = screen.recommendedProfile
        when {
            screen.isRemoteController -> Unit
            screen.controlState == HomeProxyControlState.Running -> {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.VirtualKey)
                scope.launch { homeViewModel.stopProxy() }
            }
            !screen.controlState.canInteract -> Unit
            profile == null || !screen.hasEnabledProfile ->
                context.toast(YumeTxt.ProfilesVM.Error.ProfileNotExist, Toast.LENGTH_SHORT)
            else -> {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.VirtualKey)
                homeViewModel.startProxy(profileId = profile.uuid.toString(), mode = null)
            }
        }
    }

    val wallpaperPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri ?: return@rememberLauncherForActivityResult
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            rootNavigator.push(
                Route.MoeWallpaperCrop(
                    wallpaperUri = uri.toString(),
                    initialZoom = wallpaperZoom,
                    initialBiasX = wallpaperBiasX,
                    initialBiasY = wallpaperBiasY,
                )
            )
        }
    val launchWallpaperPicker: () -> Unit =
        remember(wallpaperPickerLauncher) {
            {
                wallpaperPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        }
    val homeSidebarIcons =
        remember(onOpenPanel, handlePageChange, openSettingsPage, launchWallpaperPicker) {
            listOf(
                MoeSidebarIconItem(Yume.Zashboard) { onOpenPanel?.invoke() },
                MoeSidebarIconItem(Yume.PackageCheck) {
                    handlePageChange(BottomBarDestination.Config)
                },
                MoeSidebarIconItem(Yume.Bolt) { openSettingsPage() },
                MoeSidebarIconItem(Yume.Palette) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    launchWallpaperPicker()
                },
            )
        }

    val layoutState =
        MoeHomeLayoutState(
            wallpaperUri = wallpaperUri,
            wallpaperZoom = wallpaperZoom,
            wallpaperBiasX = wallpaperBiasX,
            wallpaperBiasY = wallpaperBiasY,
            statusBarTop = statusBarTop,
            pageProgress = pageProgress,
            sidebarProgress = pageProgress,
            sidebarToggleProgress = animatedSidebarToggleProgress,
            duration = durationPair,
            batteryPercent = batteryPercent,
            sidebarIcons = homeSidebarIcons,
            contentSurface = contentSurface,
            isRunning = isRunning,
            traffic = trafficData,
            selectedServerName = screen.selectedServerName,
            selectedServerPing = screen.selectedServerPing,
            now = now,
            quote = quoteText,
            quoteAuthor = moeHomeQuoteAuthor,
            controlState = screen.controlState,
            canLaunch =
                screen.profilesLoaded && screen.profiles.isNotEmpty() && !screen.isRemoteController,
            isRemoteController = screen.isRemoteController,
        )
    val actions =
        MoeHomeActions(
            toggleSidebar = {
                if (homeLayout == MoeHomeLayoutMode.Phone) {
                    appSettingsViewModel.onMoeSidebarExpandedChange(!sidebarExpanded)
                }
            },
            pickWallpaper = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                launchWallpaperPicker()
            },
            openSettings = { showHomeSettingsSheet = true },
            toggleProxy = handleProxyAction,
        )
    CompositionLocalProvider(LocalWallpaperRefreshKey provides wallpaperRefreshKey) {
        with(actions) { MoeHomeLayout(layoutState, homeLayout) }
    }

    MoeHomeSettingsSheet(
        show = showHomeSettingsSheet,
        copy =
            MoeHomeCopy(
                quote = moe.moeHomeQuote,
                author = moeHomeQuoteAuthor,
                expandSidebar = sidebarExpanded,
            ),
        useSystemWallpaper = useSystemWallpaper,
        statusBarSpacing = statusBarSpacing,
        onUseSystemWallpaperChange = appSettingsViewModel::onUseSystemWallpaperChange,
        onStatusBarSpacingChange = appSettingsViewModel::onMoeStatusBarSpacingChange,
        onSaveCopy = { edited ->
            appSettingsViewModel.onMoeHomeQuoteChange(edited.quote)
            appSettingsViewModel.onMoeHomeQuoteAuthorChange(edited.author)
            appSettingsViewModel.onMoeSidebarExpandedChange(edited.expandSidebar)
        },
        onChangeWallpaper = {
            showHomeSettingsSheet = false
            launchWallpaperPicker()
        },
        onDismiss = { showHomeSettingsSheet = false },
    )
}

/**
 * Light status-bar icons over the hero, and only while the home surface is actually showing.
 * Leaving the page (or turning the dynamic status-bar inset off) hands the icons back to the
 * app-wide system-night-mode appearance, hence the restore in [DisposableEffect].
 */
@Composable
private fun HomeStatusBarAppearance(
    isActive: Boolean,
    statusBarSpacing: Boolean,
    isDarkHomeSurface: Boolean,
) {
    val view = LocalView.current
    val activity = view.context.findActivity()
    val controller =
        remember(view, activity) {
            activity?.let { WindowCompat.getInsetsController(it.window, view) }
        }
    val heroOwnsStatusBar = isActive && !statusBarSpacing
    SideEffect {
        controller?.let { bar ->
            bar.isAppearanceLightStatusBars = !heroOwnsStatusBar && !isDarkHomeSurface
        }
    }
    // Remembered so dispose restores the latest appearance instead of the first composed one.
    val currentIsDarkHomeSurface by rememberUpdatedState(isDarkHomeSurface)
    DisposableEffect(controller) {
        onDispose {
            controller?.let { bar ->
                bar.isAppearanceLightStatusBars = !currentIsDarkHomeSurface
            }
        }
    }
}

@Composable
private fun rememberMoeBatteryPercent(context: Context): Int? {
    val percent by
    produceState<Int?>(initialValue = null, context) {
        val batteryIntent =
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        value =
            if (level >= 0 && scale > 0) {
                ((level / scale.toFloat()) * 100).toInt().coerceIn(0, 100)
            } else {
                null
            }
    }
    return percent
}
