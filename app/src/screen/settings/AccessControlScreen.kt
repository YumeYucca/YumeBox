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

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import com.github.yumeyucca.yumebox.data.model.AccessControlImportStrategy
import com.github.yumeyucca.yumebox.data.model.AccessControlMode
import com.github.yumeyucca.yumebox.presentation.component.AppCard
import com.github.yumeyucca.yumebox.presentation.component.GlassDropdownPreference
import com.github.yumeyucca.yumebox.presentation.component.GlassScaffold
import com.github.yumeyucca.yumebox.presentation.component.Navigator
import com.github.yumeyucca.yumebox.presentation.component.PreferenceEnumItem
import com.github.yumeyucca.yumebox.presentation.component.ScreenLazyColumn
import com.github.yumeyucca.yumebox.presentation.component.SectionSummary
import com.github.yumeyucca.yumebox.presentation.component.Title
import com.github.yumeyucca.yumebox.presentation.component.TopBar
import com.github.yumeyucca.yumebox.presentation.component.combinePaddingValues
import com.github.yumeyucca.yumebox.presentation.component.rememberStandalonePageMainPadding
import com.github.yumeyucca.yumebox.presentation.navigation.Route
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference

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

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { isGranted ->
                if (isGranted) {
                    accessControlViewModel.onPermissionResult()
                }
            },
        )

    LaunchedEffect(accessControlViewModel) { accessControlViewModel.refreshSelection() }

    LaunchedEffect(accessControlState.needsMiuiPermission) {
        if (accessControlState.needsMiuiPermission) {
            permissionLauncher.launch("com.android.permission.GET_INSTALLED_APPS")
        }
    }

    GlassScaffold(
        topBar = {
            TopBar(
                title = YumeTxt.AccessControl.Title,
                scrollBehavior = scrollBehavior,
            )
        }
    ) { innerPadding ->
        val mainLikePadding = rememberStandalonePageMainPadding()
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = combinePaddingValues(innerPadding, mainLikePadding),
        ) {
            item {
                Title(YumeTxt.AccessControl.Settings.Title)
                AppCard {
                    SwitchPreference(
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
                        items =
                            listOf(
                                YumeTxt.NetworkSettings.ProxyOptions.AllowAll,
                                YumeTxt.NetworkSettings.ProxyOptions.AllowSelected,
                                YumeTxt.NetworkSettings.ProxyOptions.RejectSelected,
                            ),
                        values = AccessControlMode.entries,
                        onValueChange = viewModel::onAccessControlModeChange,
                    )
                }
            }
            item {
                Title(YumeTxt.AccessControl.AppList.ScreenTitle)
                AppCard {
                    ArrowPreference(
                        title = YumeTxt.AccessControl.AppList.ScreenTitle,
                        onClick = { navigator.push(Route.AccessControlAppList) },
                    )
                }
            }
            item {
                Title(YumeTxt.AccessControl.Settings.ImportExport)
                AppCard {
                    ArrowPreference(
                        title = YumeTxt.AccessControl.Settings.Export,
                        onClick = { exportAccessControlPackages(context, accessControlViewModel) },
                    )
                    ArrowPreference(
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
                        items =
                            listOf(
                                YumeTxt.AccessControl.Settings.ImportMerge,
                                YumeTxt.AccessControl.Settings.ImportOverwrite,
                            ),
                        selectedIndex = accessControlState.importStrategy.ordinal,
                        title = YumeTxt.Feature.BackupRestore.RestoreStrategyTitle,
                        maxHeight = UiDp.dp200,
                        onSelectedIndexChange = { index ->
                            AccessControlImportStrategy.entries.getOrNull(index)?.let(
                                accessControlViewModel::onImportStrategyChange
                            )
                        },
                    )
                }
            }
        }
    }
}
