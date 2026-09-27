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
 */

package com.github.yumeyucca.yumebox.screen.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.github.yumeyucca.yumebox.data.model.AccessControlSortMode
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.overlay.OverlayCascadingListPopup

// :data cannot depend on YumeTxt, so persisted sort modes are localized at the app boundary.
private val AccessControlSortMode.displayName: String
    get() =
        when (this) {
            AccessControlSortMode.PACKAGE_NAME -> YumeTxt.AccessControl.SortMode.PackageName
            AccessControlSortMode.LABEL -> YumeTxt.AccessControl.SortMode.Label
            AccessControlSortMode.INSTALL_TIME -> YumeTxt.AccessControl.SortMode.InstallTime
            AccessControlSortMode.UPDATE_TIME -> YumeTxt.AccessControl.SortMode.UpdateTime
        }

@Composable
internal fun AccessControlSortMenu(
    show: Boolean,
    sortMode: AccessControlSortMode,
    onDismiss: () -> Unit,
    onSortModeChange: (AccessControlSortMode) -> Unit,
) {
    val entries =
        listOf(
            DropdownEntry(
                items =
                    AccessControlSortMode.entries.map { mode ->
                        DropdownItem(
                            text = mode.displayName,
                            selected = mode == sortMode,
                            onClick = { onSortModeChange(mode) },
                        )
                    }
            )
        )

    OverlayCascadingListPopup(
        show = show,
        entries = entries,
        onDismissRequest = onDismiss,
    )
}

internal data class AccessControlMenuActions(
    val onShowSystemAppsChange: (Boolean) -> Unit,
    val onSelectedFirstChange: (Boolean) -> Unit,
    val onSelectAll: () -> Unit,
    val onDeselectAll: () -> Unit,
    val onInvertSelection: () -> Unit,
    val onSelectChinaApps: () -> Unit,
    val onSelectNonChinaApps: () -> Unit,
    val onImportPackages: (String) -> Int,
    val onExportPackages: () -> String,
)

@Composable
internal fun AccessControlOperationsMenu(
    show: Boolean,
    uiState: AccessControlViewModel.UiState,
    onDismiss: () -> Unit,
    actions: AccessControlMenuActions,
) {
    val context = LocalContext.current
    val clipboardManager =
        remember(context) {
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        }
    val settings = YumeTxt.AccessControl.Settings

    val entries =
        listOf(
            DropdownEntry(
                items =
                    listOf(
                        DropdownItem(
                            text = settings.ShowSystemApps,
                            selected = uiState.showSystemApps,
                            onClick = { actions.onShowSystemAppsChange(!uiState.showSystemApps) },
                        ),
                        DropdownItem(
                            text = settings.SelectedFirst,
                            selected = uiState.selectedFirst,
                            onClick = { actions.onSelectedFirstChange(!uiState.selectedFirst) },
                        ),
                    )
            ),
            DropdownEntry(
                items =
                    listOf(
                        DropdownItem(
                            text = settings.BatchOperation,
                            children =
                                listOf(
                                    DropdownItem(
                                        text = settings.SelectAll,
                                        onClick = actions.onSelectAll,
                                    ),
                                    DropdownItem(
                                        text = settings.DeselectAll,
                                        onClick = actions.onDeselectAll,
                                    ),
                                    DropdownItem(
                                        text = settings.Invert,
                                        onClick = actions.onInvertSelection,
                                    ),
                                ),
                        ),
                        DropdownItem(
                            text = settings.RegionQuickSelect,
                            children =
                                listOf(
                                    DropdownItem(
                                        text = settings.ChinaApps,
                                        onClick = actions.onSelectChinaApps,
                                    ),
                                    DropdownItem(
                                        text = settings.OverseasApps,
                                        onClick = actions.onSelectNonChinaApps,
                                    ),
                                ),
                        ),
                        DropdownItem(
                            text = settings.ImportExport,
                            children =
                                listOf(
                                    DropdownItem(
                                        text = settings.Import,
                                        onClick = {
                                            val text =
                                                clipboardManager.primaryClip
                                                    ?.takeIf { it.itemCount > 0 }
                                                    ?.getItemAt(0)
                                                    ?.text
                                                    ?.toString()
                                                    .orEmpty()
                                            if (text.isNotEmpty()) {
                                                actions.onImportPackages(text)
                                            }
                                        },
                                    ),
                                    DropdownItem(
                                        text = settings.Export,
                                        onClick = {
                                            clipboardManager.setPrimaryClip(
                                                ClipData.newPlainText(
                                                    "packages",
                                                    actions.onExportPackages(),
                                                ),
                                            )
                                        },
                                    ),
                                ),
                        ),
                    )
            ),
        )

    OverlayCascadingListPopup(
        show = show,
        entries = entries,
        onDismissRequest = onDismiss,
    )
}
