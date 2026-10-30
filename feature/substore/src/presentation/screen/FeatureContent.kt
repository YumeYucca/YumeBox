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
import com.github.yumeyucca.yumebox.data.model.SubStorePanel
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.viewmodel.FeatureViewModel
import com.github.yumeyucca.yumebox.substore.model.AutoCloseMode
import org.koin.androidx.compose.koinViewModel
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior

@Composable
fun FeatureContent(
    onOpenInAppUrl: (String) -> Unit,
    onCreatePanelShortcut: (url: String, label: String) -> Unit = { _, _ -> },
    topSection: @Composable () -> Unit = {},
    bottomSection: @Composable () -> Unit = {},
) {
    val scrollBehavior = MiuixScrollBehavior()
    val viewModel = koinViewModel<FeatureViewModel>()
    val screen by viewModel.screenState.collectAsState()

    val host = "127.0.0.1"
    val frontendUrl = "http://$host:${screen.frontendPort}"
    val backendUrl = "http://$host:${screen.backendPort}"
    val subStoreUrl = "$frontendUrl/subs?api=$backendUrl"

    val panelProtocol by viewModel.panelProtocol.state.collectAsState()

    val panelType = screen.panelType
    val currentPanelName = panelType?.displayName ?: YumeTxt.Feature.Panel.Unknown
    val panelUrl = panelType?.url(panelProtocol).orEmpty()

    LaunchedEffect(Unit) { viewModel.initializeSubStoreStatus() }

    GlassScaffold(topBar = { TopBar(title = YumeTxt.Feature.Title, scrollBehavior = scrollBehavior) }) { innerPadding ->

        StandalonePageList(
            scrollBehavior = scrollBehavior,
            scaffoldPadding = innerPadding,
        ) {
            item { topSection() }

            item {
                Title(YumeTxt.Feature.Panel.Section)
                AppCard {
                    PreferenceEnumItem(
                        title = YumeTxt.Feature.Panel.SelectPanel,
                        summary = null,
                        currentValue = panelType ?: SubStorePanel.entries.first(),
                        values = SubStorePanel.entries,
                        label = { it.displayName },
                        onValueChange = viewModel::setSelectedPanelType,
                    )
                }
            }

            item {
                Title(YumeTxt.Feature.Panel.OpenSection)
                AppCard {
                    PreferenceEnumItem(
                        title = YumeTxt.Feature.Panel.OpenMode,
                        summary = null,
                        currentValue = panelProtocol,
                        values = RemoteProtocol.entries,
                        label = { it.scheme.uppercase() },
                        onValueChange = viewModel::setPanelProtocol,
                    )
                    PreferenceArrowItem(
                        title = YumeTxt.Feature.Panel.CreateShortcut,
                        enabled = panelUrl.isNotBlank(),
                        onClick = { onCreatePanelShortcut(panelUrl, currentPanelName) },
                    )
                }
                SectionSummary(YumeTxt.Feature.Panel.CreateShortcutSummary)
            }

            item {
                Title(YumeTxt.Feature.ServiceStatus.Section)
                AppCard {
                    PreferenceSwitchItem(
                        title = YumeTxt.Feature.ServiceStatus.SwitchStartSubStore,
                        checked = screen.isServiceRunning,
                        onCheckedChange = {
                            if (it) viewModel.startService() else viewModel.stopService()
                        },
                    )
                    PreferenceArrowItem(
                        title = YumeTxt.Feature.ServiceStatus.OpenSubStorePanel,
                        enabled = !DeviceUtil.is32BitDevice() && screen.isServiceRunning,
                        onClick = { onOpenInAppUrl(subStoreUrl) },
                    )
                }
            }

            item {
                Title(YumeTxt.Feature.ServiceStatus.RuntimeSection)
                AppCard {
                    PreferenceSwitchItem(
                        title = YumeTxt.Feature.ServiceStatus.AllowLan,
                        checked = screen.allowLanAccess,
                        onCheckedChange = { viewModel.setAllowLanAccess(it) },
                    )
                    PreferenceEnumItem(
                        title = YumeTxt.Feature.ServiceStatus.AutoCloseModeTitle,
                        summary = null,
                        currentValue = screen.autoCloseMode,
                        values = AutoCloseMode.entries,
                        label = { it.getDisplayName() },
                        onValueChange = { viewModel.setAutoCloseMode(it) },
                    )
                }
            }

            item {
                Title(YumeTxt.Feature.SubStore.Section)
                AppCard {
                    PreferenceArrowItem(
                        title =
                            if (screen.isJavetLoaded) {
                                YumeTxt.Feature.SubStore.JavetLibraryReady
                            } else {
                                YumeTxt.Feature.SubStore.JavetLibraryDownload
                            },
                        onClick = { viewModel.installResources() },
                        enabled = screen.downloading.isEmpty(),
                    )
                }
                SectionSummary(YumeTxt.Feature.SubStore.DownloadResourcesSummary)
            }

            item { bottomSection() }
        }
    }
}
