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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.github.yumeyucca.yumebox.common.util.toast
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import com.github.yumeyucca.yumebox.data.model.AccessControlSortMode
import com.github.yumeyucca.yumebox.presentation.component.GlassMenuItem
import com.github.yumeyucca.yumebox.presentation.component.GlassPopupHost
import com.github.yumeyucca.yumebox.presentation.component.GlassPopupSubmenuItem
import com.github.yumeyucca.yumebox.presentation.component.GlassSubmenuPageHeader
import com.github.yumeyucca.yumebox.presentation.component.GlassTopActionAnchorIcon
import com.github.yumeyucca.yumebox.presentation.component.rememberGlassSubmenuState
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.Bolt
import com.github.yumeyucca.yumebox.presentation.icon.yume.ListChevronsUpDown
import top.yukonga.miuix.kmp.glass.GlassPopupAnchor
import java.util.Locale

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
    val onShowSystemAppsChange: (Boolean) -> Unit,
    val onSelectedFirstChange: (Boolean) -> Unit,
    val onSelectAll: () -> Unit,
    val onDeselectAll: () -> Unit,
    val onInvertSelection: () -> Unit,
    val onSelectChinaApps: () -> Unit,
    val onSelectNonChinaApps: () -> Unit,
)

private enum class AccessControlMenuPage {
    Batch,
    Region,
}

@Composable
internal fun AccessControlOperationsMenu(
    showPopup: Boolean,
    onShowPopupChange: (Boolean) -> Unit,
    anchor: GlassPopupAnchor,
    uiState: AccessControlViewModel.UiState,
    actions: AccessControlMenuActions,
    anchorContent: @Composable () -> Unit = {
        GlassTopActionAnchorIcon(Yume.Bolt)
    },
) {
    val settings = YumeTxt.AccessControl.Settings
    val submenu = rememberGlassSubmenuState(showPopup)
    var submenuPage by remember { mutableStateOf(AccessControlMenuPage.Batch) }

    fun applyAndClose(action: () -> Unit) {
        action()
        submenu.close()
        onShowPopupChange(false)
    }

    GlassPopupHost(
        show = showPopup,
        onDismissRequest = { onShowPopupChange(false) },
        anchor = anchor,
        anchorContent = anchorContent,
        submenu = submenu,
        submenuContent = {
            GlassSubmenuPageHeader(
                state = submenu,
                text =
                    when (submenuPage) {
                        AccessControlMenuPage.Batch -> settings.BatchOperation
                        AccessControlMenuPage.Region -> settings.RegionQuickSelect
                    },
                anchor = anchor,
            )
            when (submenuPage) {
                AccessControlMenuPage.Batch -> {
                    GlassMenuItem(
                        text = settings.SelectAll,
                        onClick = { applyAndClose(actions.onSelectAll) },
                    )
                    GlassMenuItem(
                        text = settings.DeselectAll,
                        onClick = { applyAndClose(actions.onDeselectAll) },
                    )
                    GlassMenuItem(
                        text = settings.Invert,
                        onClick = { applyAndClose(actions.onInvertSelection) },
                    )
                }

                AccessControlMenuPage.Region -> {
                    GlassMenuItem(
                        text = settings.ChinaApps,
                        onClick = { applyAndClose(actions.onSelectChinaApps) },
                    )
                    GlassMenuItem(
                        text = settings.OverseasApps,
                        onClick = { applyAndClose(actions.onSelectNonChinaApps) },
                    )
                }
            }
        },
    ) {
        GlassMenuItem(
            text = settings.ShowSystemApps,
            selected = uiState.showSystemApps,
            onClick = { actions.onShowSystemAppsChange(!uiState.showSystemApps) },
        )
        GlassMenuItem(
            text = settings.SelectedFirst,
            selected = uiState.selectedFirst,
            onClick = { actions.onSelectedFirstChange(!uiState.selectedFirst) },
        )
        GlassPopupSubmenuItem(
            text = settings.BatchOperation,
            anchor = anchor,
            open = submenu.open && submenuPage == AccessControlMenuPage.Batch,
            onOpen = { bounds ->
                submenuPage = AccessControlMenuPage.Batch
                submenu.openAt(bounds)
            },
        )
        GlassPopupSubmenuItem(
            text = settings.RegionQuickSelect,
            anchor = anchor,
            open = submenu.open && submenuPage == AccessControlMenuPage.Region,
            onOpen = { bounds ->
                submenuPage = AccessControlMenuPage.Region
                submenu.openAt(bounds)
            },
        )
    }
}

@Composable
internal fun AccessControlSortMenu(
    show: Boolean,
    onDismiss: () -> Unit,
    sortMode: AccessControlSortMode,
    onSortSelected: (AccessControlSortMode) -> Unit,
    anchor: GlassPopupAnchor,
    anchorContent: @Composable () -> Unit = {
        GlassTopActionAnchorIcon(Yume.ListChevronsUpDown)
    },
) {
    GlassPopupHost(
        show = show,
        onDismissRequest = onDismiss,
        anchor = anchor,
        anchorContent = anchorContent,
    ) {
        AccessControlSortMode.entries.forEach { mode ->
            GlassMenuItem(
                text = mode.displayName,
                selected = sortMode == mode,
                onClick = {
                    onSortSelected(mode)
                    onDismiss()
                },
            )
        }
    }
}

internal fun importAccessControlPackages(
    context: Context,
    viewModel: AccessControlViewModel,
) {
    val clipboardManager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val text =
        clipboardManager.primaryClip
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.text
            ?.toString()
            .orEmpty()
    val state = viewModel.uiState.value
    if (text.isBlank() || state.isLoading || state.apps.isEmpty()) {
        context.toast(YumeTxt.AccessControl.Settings.ImportFailed)
        return
    }

    val count = viewModel.importPackages(text)
    context.toast(
        if (count > 0) {
            String.format(Locale.getDefault(), YumeTxt.AccessControl.Settings.ImportSuccess, count)
        } else {
            YumeTxt.AccessControl.Settings.ImportFailed
        }
    )
}

internal fun exportAccessControlPackages(
    context: Context,
    viewModel: AccessControlViewModel,
) {
    val clipboardManager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val count = viewModel.uiState.value.selectedPackages.size
    clipboardManager.setPrimaryClip(
        ClipData.newPlainText("packages", viewModel.exportPackages()),
    )
    context.toast(
        String.format(Locale.getDefault(), YumeTxt.AccessControl.Settings.ExportSuccess, count)
    )
}
