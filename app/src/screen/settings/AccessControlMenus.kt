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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import com.github.yumeyucca.yumebox.data.model.AccessControlSortMode
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.Settings2
import com.github.yumeyucca.yumebox.presentation.theme.LocalTopBarBackdrop
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.glass.GlassPopup
import top.yukonga.miuix.kmp.glass.GlassPopupItem
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils.Companion.PopupLayout

// :data cannot depend on YumeTxt, so persisted sort modes are localized at the app boundary.
private val AccessControlSortMode.displayName: String
    get() =
        when (this) {
            AccessControlSortMode.PACKAGE_NAME -> YumeTxt.AccessControl.SortMode.PackageName
            AccessControlSortMode.LABEL -> YumeTxt.AccessControl.SortMode.Label
            AccessControlSortMode.INSTALL_TIME -> YumeTxt.AccessControl.SortMode.InstallTime
            AccessControlSortMode.UPDATE_TIME -> YumeTxt.AccessControl.SortMode.UpdateTime
        }

internal data class AccessControlMenuActions(
    val onSortModeChange: (AccessControlSortMode) -> Unit,
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

private enum class AccessControlMenuPage {
    Root,
    Batch,
    Region,
    ImportExport,
    Sort,
}

@Composable
internal fun AccessControlOperationsMenu(
    uiState: AccessControlViewModel.UiState,
    actions: AccessControlMenuActions,
) {
    val context = LocalContext.current
    val clipboardManager =
        remember(context) {
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        }
    val settings = YumeTxt.AccessControl.Settings

    var showPopup by remember { mutableStateOf(false) }
    var anchorBounds by remember { mutableStateOf(Rect.Zero) }
    var page by remember { mutableStateOf(AccessControlMenuPage.Root) }
    val popupVisible = remember { mutableStateOf(false) }
    LaunchedEffect(showPopup) {
        popupVisible.value = showPopup
        if (!showPopup) page = AccessControlMenuPage.Root
    }

    Box(modifier = Modifier.onGloballyPositioned { anchorBounds = it.boundsInRoot() }) {
        IconButton(onClick = { showPopup = true }) {
            Icon(
                imageVector = Yume.Settings2,
                contentDescription = settings.Title,
            )
        }
    }
    PopupLayout(
        visible = popupVisible,
        enableWindowDim = false,
        enableBackHandler = false,
    ) {
        Box(Modifier.fillMaxSize()) {
            GlassPopup(
                show = showPopup,
                onDismissRequest = { showPopup = false },
                anchorBounds = anchorBounds,
                backdrop = LocalTopBarBackdrop.current,
            ) {
                when (page) {
                    AccessControlMenuPage.Root -> {
                        GlassPopupItem(
                            text = settings.ShowSystemApps,
                            selected = uiState.showSystemApps,
                            onClick = { actions.onShowSystemAppsChange(!uiState.showSystemApps) },
                        )
                        GlassPopupItem(
                            text = settings.SelectedFirst,
                            selected = uiState.selectedFirst,
                            onClick = { actions.onSelectedFirstChange(!uiState.selectedFirst) },
                        )
                        GlassPopupItem(
                            text = settings.BatchOperation,
                            showArrow = true,
                            onClick = { page = AccessControlMenuPage.Batch },
                        )
                        GlassPopupItem(
                            text = settings.RegionQuickSelect,
                            showArrow = true,
                            onClick = { page = AccessControlMenuPage.Region },
                        )
                        GlassPopupItem(
                            text = settings.ImportExport,
                            showArrow = true,
                            onClick = { page = AccessControlMenuPage.ImportExport },
                        )
                        GlassPopupItem(
                            text = settings.SortMode,
                            showArrow = true,
                            onClick = { page = AccessControlMenuPage.Sort },
                        )
                    }

                    AccessControlMenuPage.Batch -> {
                        GlassPopupItem(text = settings.SelectAll, onClick = {
                            actions.onSelectAll()
                            showPopup = false
                        })
                        GlassPopupItem(text = settings.DeselectAll, onClick = {
                            actions.onDeselectAll()
                            showPopup = false
                        })
                        GlassPopupItem(text = settings.Invert, onClick = {
                            actions.onInvertSelection()
                            showPopup = false
                        })
                    }

                    AccessControlMenuPage.Region -> {
                        GlassPopupItem(text = settings.ChinaApps, onClick = {
                            actions.onSelectChinaApps()
                            showPopup = false
                        })
                        GlassPopupItem(text = settings.OverseasApps, onClick = {
                            actions.onSelectNonChinaApps()
                            showPopup = false
                        })
                    }

                    AccessControlMenuPage.ImportExport -> {
                        GlassPopupItem(text = settings.Import, onClick = {
                            val text = clipboardManager.primaryClip
                                ?.takeIf { it.itemCount > 0 }
                                ?.getItemAt(0)
                                ?.text
                                ?.toString()
                                .orEmpty()
                            if (text.isNotEmpty()) actions.onImportPackages(text)
                            showPopup = false
                        })
                        GlassPopupItem(text = settings.Export, onClick = {
                            clipboardManager.setPrimaryClip(
                                ClipData.newPlainText("packages", actions.onExportPackages()),
                            )
                            showPopup = false
                        })
                    }

                    AccessControlMenuPage.Sort -> {
                        AccessControlSortMode.entries.forEach { mode ->
                            GlassPopupItem(
                                text = mode.displayName,
                                selected = uiState.sortMode == mode,
                                onClick = {
                                    actions.onSortModeChange(mode)
                                    showPopup = false
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
