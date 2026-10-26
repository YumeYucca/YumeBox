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
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.yumeyucca.yumebox.common.util.toast
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import com.github.yumeyucca.yumebox.data.model.AccessControlSortMode
import com.github.yumeyucca.yumebox.presentation.theme.LocalGlassBackdrop
import com.github.yumeyucca.yumebox.presentation.theme.LocalTopBarBackdrop
import top.yukonga.miuix.kmp.glass.GlassPopupAnchor
import top.yukonga.miuix.kmp.glass.GlassPopupDefaults
import top.yukonga.miuix.kmp.glass.GlassPopupItem
import top.yukonga.miuix.kmp.glass.GlassPopupSizing
import top.yukonga.miuix.kmp.glass.GlassTransformPopup
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils.Companion.PopupLayout
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
    Root,
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
    anchorContent: @Composable () -> Unit,
) {
    val settings = YumeTxt.AccessControl.Settings

    var page by remember { mutableStateOf(AccessControlMenuPage.Root) }
    LaunchedEffect(showPopup) {
        if (!showPopup) page = AccessControlMenuPage.Root
    }

    AccessControlGlassPopup(
        show = showPopup,
        onDismiss = { onShowPopupChange(false) },
        anchor = anchor,
        anchorContent = anchorContent,
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
            }

            AccessControlMenuPage.Batch -> {
                GlassPopupItem(text = settings.SelectAll, onClick = {
                    actions.onSelectAll()
                    onShowPopupChange(false)
                })
                GlassPopupItem(text = settings.DeselectAll, onClick = {
                    actions.onDeselectAll()
                    onShowPopupChange(false)
                })
                GlassPopupItem(text = settings.Invert, onClick = {
                    actions.onInvertSelection()
                    onShowPopupChange(false)
                })
            }

            AccessControlMenuPage.Region -> {
                GlassPopupItem(text = settings.ChinaApps, onClick = {
                    actions.onSelectChinaApps()
                    onShowPopupChange(false)
                })
                GlassPopupItem(text = settings.OverseasApps, onClick = {
                    actions.onSelectNonChinaApps()
                    onShowPopupChange(false)
                })
            }
        }
    }
}

@Composable
internal fun AccessControlSortMenu(
    show: Boolean,
    onDismiss: () -> Unit,
    sortMode: AccessControlSortMode,
    onSortSelected: (AccessControlSortMode) -> Unit,
    anchor: GlassPopupAnchor,
    anchorContent: @Composable () -> Unit,
) {
    AccessControlGlassPopup(
        show = show,
        onDismiss = onDismiss,
        anchor = anchor,
        anchorContent = anchorContent,
    ) {
        AccessControlSortMode.entries.forEach { mode ->
            GlassPopupItem(
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

@Composable
internal fun AccessControlImportExportMenu(
    show: Boolean,
    onDismiss: () -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
    anchor: GlassPopupAnchor,
    anchorContent: @Composable () -> Unit,
) {
    AccessControlGlassPopup(
        show = show,
        onDismiss = onDismiss,
        anchor = anchor,
        anchorContent = anchorContent,
    ) {
        GlassPopupItem(text = YumeTxt.AccessControl.Settings.Export, onClick = {
            onExport()
            onDismiss()
        })
        GlassPopupItem(text = YumeTxt.AccessControl.Settings.Import, onClick = {
            onImport()
            onDismiss()
        })
    }
}

@Composable
private fun AccessControlGlassPopup(
    show: Boolean,
    onDismiss: () -> Unit,
    anchor: GlassPopupAnchor,
    anchorContent: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    // PopupLayout relocates this content; capture the page backdrop before leaving its provider.
    val backdrop = LocalGlassBackdrop.current ?: LocalTopBarBackdrop.current
    val popupVisible = remember { mutableStateOf(false) }
    val popupVisuals = GlassPopupDefaults.visuals()
    LaunchedEffect(show) {
        if (show) popupVisible.value = true
    }
    PopupLayout(
        visible = popupVisible,
        enterTransition = EnterTransition.None,
        exitTransition = ExitTransition.None,
        enableWindowDim = false,
        enableBackHandler = false,
    ) {
        Box(Modifier.fillMaxSize()) {
            // Compose the component closed first so the host does not skip its entering transition.
            var expanded by remember { mutableStateOf(false) }
            LaunchedEffect(show) { expanded = show }
            GlassTransformPopup(
                show = expanded,
                onDismissRequest = onDismiss,
                anchor = anchor,
                backdrop = backdrop,
                anchorContent = anchorContent,
                anchorAlpha = 0f,
                sizing = GlassPopupSizing(minWidth = 188.dp, maxWidth = 188.dp),
                visuals = popupVisuals,
                alignToPageEnd = true,
                onDismissFinished = { if (!show) popupVisible.value = false },
            ) {
                content()
            }
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
