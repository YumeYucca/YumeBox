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

package com.github.yumeyucca.yumebox.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.github.yumeyucca.yumebox.common.util.DeviceUtil
import com.github.yumeyucca.yumebox.data.model.RemoteProtocol
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.viewmodel.FeatureViewModel
import com.github.yumeyucca.yumebox.substore.model.AutoCloseMode
import org.koin.androidx.compose.koinViewModel
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference

@Composable
fun FeatureContent(
    onOpenInAppUrl: (String) -> Unit,
    onOpenPanel: (url: String) -> Unit,
    onCreatePanelShortcut: (url: String, label: String) -> Unit = { _, _ -> },
    topSection: @Composable () -> Unit = {},
    bottomSection: @Composable () -> Unit = {},
) {
    val scrollBehavior = MiuixScrollBehavior()
    val viewModel = koinViewModel<FeatureViewModel>()
    val screen by viewModel.screenState.collectAsState()
    val isServiceRunning = screen.isServiceRunning
    val allowLanAccess = screen.allowLanAccess
    val frontendPort = screen.frontendPort
    val backendPort = screen.backendPort
    val autoCloseMode = screen.autoCloseMode

    val host = "127.0.0.1"
    val frontendUrl = "http://$host:$frontendPort"
    val backendUrl = "http://$host:$backendPort"
    val subStoreUrl = "$frontendUrl/subs?api=$backendUrl"

    val isDownloadingSubStoreFrontend = screen.isDownloadingSubStoreFrontend
    val isDownloadingSubStoreBackend = screen.isDownloadingSubStoreBackend
    val isDownloadingJavet = screen.isDownloadingJavet
    val isJavetLoaded = screen.isJavetLoaded
    val selectedPanelType = screen.selectedPanelType
    val panelProtocol by viewModel.panelProtocol.state.collectAsState()

    val panelDisplayNames = listOf("Zashboard", "MetaCubeXD", "Yacd")

    LaunchedEffect(Unit) { viewModel.initializeSubStoreStatus() }

    Scaffold(topBar = { TopBar(title = YumeTxt.Feature.Title, scrollBehavior = scrollBehavior) }) { innerPadding ->
        val mainLikePadding = rememberStandalonePageMainPadding()
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = combinePaddingValues(innerPadding, mainLikePadding),
        ) {
            item { topSection() }

            item {
                val currentPanelName =
                    panelDisplayNames.getOrElse(selectedPanelType) { YumeTxt.Feature.Panel.Unknown }
                val panelUrl = panelUrlFor(selectedPanelType, panelProtocol)

                Title(YumeTxt.Feature.Panel.Section)
                AppCard {
                    val safeSelectedPanelType =
                        selectedPanelType.coerceIn(0, panelDisplayNames.lastIndex)
                    WindowDropdownPreference(
                        title = YumeTxt.Feature.Panel.SelectPanel,
                        summary = null,
                        items = panelDisplayNames,
                        selectedIndex = safeSelectedPanelType,
                        onSelectedIndexChange = { viewModel.setSelectedPanelType(it) },
                    )

                    EnumSelector(
                        title = YumeTxt.Feature.Panel.OpenMode,
                        summary = null,
                        currentValue = panelProtocol,
                        items = RemoteProtocol.entries.map { it.scheme.uppercase() },
                        values = RemoteProtocol.entries,
                        onValueChange = viewModel::setPanelProtocol,
                    )
                    ArrowPreference(
                        title = YumeTxt.Feature.Panel.OpenPanel,
                        summary = null,
                        enabled = panelUrl.isNotBlank(),
                        onClick = { onOpenPanel(panelUrl) },
                    )
                    ArrowPreference(
                        title = YumeTxt.Feature.Panel.CreateShortcut,
                        summary = null,
                        enabled = panelUrl.isNotBlank(),
                        onClick = { onCreatePanelShortcut(panelUrl, currentPanelName) },
                    )
                }
            }

            item {
                Title(YumeTxt.Feature.ServiceStatus.Section)
                AppCard {
                    val autoCloseItems = AutoCloseMode.entries.map { it.getDisplayName() }
                    val autoCloseValues = AutoCloseMode.entries

                    SwitchPreference(
                        title = YumeTxt.Feature.ServiceStatus.SwitchStartSubStore,
                        summary = null,
                        checked = isServiceRunning,
                        onCheckedChange = {
                            if (it) viewModel.startService() else viewModel.stopService()
                        },
                    )
                    SwitchPreference(
                        title = YumeTxt.Feature.ServiceStatus.AllowLan,
                        summary = null,
                        checked = allowLanAccess,
                        onCheckedChange = { viewModel.setAllowLanAccess(it) },
                    )
                    EnumSelector(
                        title = YumeTxt.Feature.ServiceStatus.AutoCloseModeTitle,
                        summary = null,
                        currentValue = autoCloseMode,
                        items = autoCloseItems,
                        values = autoCloseValues,
                        onValueChange = { viewModel.setAutoCloseMode(it) },
                    )
                    ArrowPreference(
                        title = YumeTxt.Feature.ServiceStatus.OpenSubStorePanel,
                        summary = null,
                        enabled = !DeviceUtil.is32BitDevice() && isServiceRunning,
                        onClick = {
                            if (!isServiceRunning) return@ArrowPreference
                            onOpenInAppUrl(subStoreUrl)
                        },
                    )
                }
            }

            item {
                Title(YumeTxt.Feature.SubStore.Section)
                AppCard {
                    ArrowPreference(
                        title =
                            if (isJavetLoaded) {
                                YumeTxt.Feature.SubStore.JavetLibraryReady
                            } else {
                                YumeTxt.Feature.SubStore.JavetLibraryDownload
                            },
                        summary =
                            if (isJavetLoaded) {
                                YumeTxt.Feature.SubStore.JavetAvailable
                            } else {
                                YumeTxt.Feature.SubStore.DownloadHint
                            },
                        onClick = { viewModel.downloadJavetLibrary() },
                        enabled = !isDownloadingJavet,
                    )
                    ArrowPreference(
                        title = YumeTxt.Feature.SubStore.DownloadResources,
                        summary = YumeTxt.Feature.SubStore.DownloadResourcesSummary,
                        onClick = { viewModel.downloadSubStoreAll() },
                        enabled = !isDownloadingSubStoreFrontend && !isDownloadingSubStoreBackend,
                    )
                }
            }

            item { bottomSection() }
        }
    }
}

private fun panelUrlFor(
    panelType: Int,
    protocol: RemoteProtocol,
): String {
    val host =
        when (panelType) {
            0 -> "board.zash.run.place"
            1 -> "metacubex.github.io/metacubexd"
            2 -> "yacd.haishan.me"
            else -> "board.zash.run.place"
        }
    return "${protocol.scheme}://$host"
}
