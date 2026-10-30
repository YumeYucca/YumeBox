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

package com.github.yumeyucca.yumebox.screen.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import com.github.yumeyucca.yumebox.data.model.AccessControlImportStrategy
import com.github.yumeyucca.yumebox.data.model.AccessControlMode
import com.github.yumeyucca.yumebox.presentation.component.AppCard
import com.github.yumeyucca.yumebox.presentation.component.GlassDropdownOption
import com.github.yumeyucca.yumebox.presentation.component.GlassDropdownPreference
import com.github.yumeyucca.yumebox.presentation.component.GlassScaffold
import com.github.yumeyucca.yumebox.presentation.component.Navigator
import com.github.yumeyucca.yumebox.presentation.component.PreferenceArrowItem
import com.github.yumeyucca.yumebox.presentation.component.PreferenceEnumItem
import com.github.yumeyucca.yumebox.presentation.component.PreferenceSwitchItem
import com.github.yumeyucca.yumebox.presentation.component.StandalonePageList
import com.github.yumeyucca.yumebox.presentation.component.SectionSummary
import com.github.yumeyucca.yumebox.presentation.component.Title
import com.github.yumeyucca.yumebox.presentation.component.TopBar
import com.github.yumeyucca.yumebox.presentation.navigation.Route
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior

/** Access-control home: enabled switch, mode selector, app list and clipboard import/export. */
@Composable
fun AccessControlScreen(navigator: Navigator) {
    val scrollBehavior = MiuixScrollBehavior()
    val viewModel = koinViewModel<NetworkSettingsViewModel>()
    val accessControlViewModel = koinViewModel<AccessControlViewModel>()
    val screen by viewModel.networkScreenState.collectAsState()
    val accessControlState by accessControlViewModel.uiState.collectAsState()
    val accessControlMode = screen.accessControlMode
    val context = LocalContext.current

    LaunchedEffect(accessControlViewModel) { accessControlViewModel.refreshSelection() }

    InstalledAppsPermissionRequest(
        needsPermission = accessControlState.needsMiuiPermission,
        onGranted = accessControlViewModel::onPermissionResult,
    )

    GlassScaffold(
        topBar = {
            TopBar(
                title = YumeTxt.AccessControl.Title,
                scrollBehavior = scrollBehavior,
            )
        }
    ) { innerPadding ->

        StandalonePageList(
            scrollBehavior = scrollBehavior,
            scaffoldPadding = innerPadding,
        ) {
            item {
                Title(YumeTxt.AccessControl.Settings.Title)
                AppCard {
                    PreferenceSwitchItem(
                        title = YumeTxt.AccessControl.Settings.Enabled,
                        checked = accessControlMode != AccessControlMode.ALLOW_ALL,
                        onCheckedChange = { enabled ->
                            viewModel.onAccessControlModeChange(
                                if (enabled) AccessControlMode.ALLOW_SPECIFIC
                                else AccessControlMode.ALLOW_ALL
                            )
                        },
                    )
                    PreferenceEnumItem(
                        title = YumeTxt.AccessControl.Settings.ModeTitle,
                        currentValue = accessControlMode,
                        values = AccessControlMode.entries,
                        onValueChange = viewModel::onAccessControlModeChange,
                        label = { mode ->
                            when (mode) {
                                AccessControlMode.ALLOW_ALL ->
                                    YumeTxt.NetworkSettings.ProxyOptions.AllowAll
                                AccessControlMode.ALLOW_SPECIFIC ->
                                    YumeTxt.NetworkSettings.ProxyOptions.AllowSelected
                                AccessControlMode.DENY_SPECIFIC ->
                                    YumeTxt.NetworkSettings.ProxyOptions.RejectSelected
                            }
                        },
                    )
                }
            }
            item {
                Title(YumeTxt.AccessControl.AppList.ScreenTitle)
                AppCard {
                    PreferenceArrowItem(
                        title = YumeTxt.AccessControl.AppList.ScreenTitle,
                        onClick = { navigator.push(Route.AccessControlAppList) },
                    )
                }
            }
            item {
                Title(YumeTxt.AccessControl.Settings.ImportExport)
                AppCard {
                    PreferenceArrowItem(
                        title = YumeTxt.AccessControl.Settings.Export,
                        onClick = { exportAccessControlPackages(context, accessControlViewModel) },
                    )
                    PreferenceArrowItem(
                        title = YumeTxt.AccessControl.Settings.Import,
                        onClick = { importAccessControlPackages(context, accessControlViewModel) },
                    )
                }
                SectionSummary(YumeTxt.AccessControl.Settings.ImportExportSummary)
            }
            item {
                Title(YumeTxt.AccessControl.Settings.ImportStrategy)
                AppCard {
                    GlassDropdownPreference(
                        options =
                            AccessControlImportStrategy.entries.map { strategy ->
                                GlassDropdownOption(strategy, importStrategyLabel(strategy))
                            },
                        selected = accessControlState.importStrategy,
                        title = YumeTxt.Feature.BackupRestore.RestoreStrategyTitle,
                        maxHeight = UiDp.dp200,
                        onSelected = accessControlViewModel::onImportStrategyChange,
                    )
                }
            }
        }
    }
}

private fun importStrategyLabel(strategy: AccessControlImportStrategy): String =
    when (strategy) {
        AccessControlImportStrategy.MERGE -> YumeTxt.AccessControl.Settings.ImportMerge
        AccessControlImportStrategy.OVERWRITE -> YumeTxt.AccessControl.Settings.ImportOverwrite
    }
