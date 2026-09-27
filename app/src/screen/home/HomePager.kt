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

@file:Suppress("DuplicatedCode", "FunctionName", "RedundantIf")

package com.github.yumeyucca.yumebox.screen.home

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.github.yumeyucca.yumebox.common.util.toast
import com.github.yumeyucca.yumebox.data.network.IpMonitoringState
import com.github.yumeyucca.yumebox.domain.model.TrafficData
import com.github.yumeyucca.yumebox.presentation.component.LocalNavigator
import com.github.yumeyucca.yumebox.presentation.component.ScreenLazyColumn
import com.github.yumeyucca.yumebox.presentation.component.TopBar
import com.github.yumeyucca.yumebox.presentation.component.combinePaddingValues
import com.github.yumeyucca.yumebox.presentation.navigation.Route
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.runtime.api.Profile
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold

@Composable
fun HomePager(
    mainInnerPadding: PaddingValues,
    isActive: Boolean,
    onOpenPanel: (() -> Unit)? = null,
) {
    val homeViewModel = koinViewModel<HomeViewModel>()
    val navigator = LocalNavigator.current
    val screen by homeViewModel.screenState.collectAsState()
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) { homeViewModel.refreshProxyMode() }
    LaunchedEffect(isActive) { homeViewModel.setHomeScreenActive(isActive) }
    DisposableEffect(homeViewModel) { onDispose { homeViewModel.setHomeScreenActive(false) } }
    DisposableEffect(lifecycleOwner, homeViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                homeViewModel.reconcileRuntimeState()
                homeViewModel.refreshProxyMode()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(screen.uiError) {
        screen.uiError?.let {
            context.toast(it, Toast.LENGTH_LONG)
            homeViewModel.consumeError()
        }
    }
    LaunchedEffect(screen.uiMessage) {
        screen.uiMessage?.let {
            context.toast(it, Toast.LENGTH_SHORT)
            homeViewModel.consumeMessage()
        }
    }

    val scrollBehavior = MiuixScrollBehavior()
    val isRunning = screen.controlState == HomeProxyControlState.Running
    val isProxyEnabled =
        if (screen.isRemoteController) {
            false
        } else {
            screen.controlState.canInteract &&
                    (isRunning || (screen.profilesLoaded && screen.profiles.isNotEmpty()))
        }

    Scaffold(topBar = { TopBar(title = YumeTxt.Home.Title, scrollBehavior = scrollBehavior) }) { innerPadding ->
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = combinePaddingValues(innerPadding, mainInnerPadding),
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = UiDp.dp24),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(UiDp.dp24),
                ) {
                    TrafficDisplay(
                        trafficNow =
                            if (isRunning) {
                                TrafficData.from(screen.trafficNow)
                            } else {
                                TrafficData.zero
                            },
                        profileName =
                            if (screen.isRemoteController) {
                                screen.controllerBackendName
                            } else {
                                screen.currentProfile?.name?.takeIf { isRunning }
                            },
                        tunnelMode = null,
                        controlState = screen.controlState,
                        proxyMode = screen.proxyMode,
                        isRemoteController = screen.isRemoteController,
                        isEnabled = isProxyEnabled,
                        onOpenPanel = onOpenPanel,
                        onClick = {
                            if (screen.isRemoteController) {
                                return@TrafficDisplay
                            }
                            if (!isRunning &&
                                (!screen.hasEnabledProfile || screen.recommendedProfile == null)
                            ) {
                                context.toast(YumeTxt.ProfilesVM.Error.ProfileNotExist)
                                return@TrafficDisplay
                            }
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.VirtualKey)
                            handleProxyToggle(
                                isRunning = isRunning,
                                recommendedProfile = screen.recommendedProfile,
                                onStart = { profile ->
                                    homeViewModel.startProxy(
                                        profileId = profile.uuid.toString(),
                                        mode = null,
                                    )
                                },
                                onStop = { coroutineScope.launch { homeViewModel.stopProxy() } },
                            )
                        },
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(UiDp.dp16)) {
                        NodeInfoDisplay(
                            serverName = screen.selectedServerName.takeIf { isRunning },
                            serverPing = screen.selectedServerPing.takeIf { isRunning },
                        )
                        IpInfoDisplay(
                            state =
                                if (isRunning) {
                                    screen.ipMonitoringState
                                } else {
                                    IpMonitoringState.Loading
                                }
                        )
                    }

                    SpeedChart(
                        speedHistory = screen.speedHistory,
                        isRunning = isRunning,
                        onClick = { navigator.push(Route.TrafficStatistics) },
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(UiDp.dp32)) }
        }
    }
}

private fun handleProxyToggle(
    isRunning: Boolean,
    recommendedProfile: Profile?,
    onStart: (Profile) -> Unit,
    onStop: () -> Unit,
) {
    if (!isRunning) {
        recommendedProfile?.let { profile -> onStart(profile) }
    } else {
        onStop()
    }
}
