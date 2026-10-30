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

package com.github.yumeyucca.yumebox.screen.settings.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.github.yumeyucca.yumebox.common.util.toast
import com.github.yumeyucca.yumebox.presentation.component.AppCard
import com.github.yumeyucca.yumebox.presentation.component.AppConfirmDialog
import com.github.yumeyucca.yumebox.presentation.component.GlassDropdownOption
import com.github.yumeyucca.yumebox.presentation.component.GlassDropdownPreference
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import com.github.yumeyucca.yumebox.presentation.component.PreferenceArrowItem
import com.github.yumeyucca.yumebox.presentation.component.SectionSummary
import com.github.yumeyucca.yumebox.presentation.component.Title
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import org.koin.androidx.compose.koinViewModel

@Composable
fun BackupRestoreSection() {
    val viewModel = koinViewModel<BackupRestoreViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var restoreUri by remember { mutableStateOf<Uri?>(null) }
    var restoreStrategy by rememberSaveable {
        mutableStateOf(BackupRestoreStrategy.Overwrite)
    }

    val exportLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/zip")
        ) { uri ->
            uri?.let(viewModel::exportBackup)
        }

    val restoreLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri
            ->
            restoreUri = uri
        }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is BackupRestoreEvent.Message -> context.toast(event.text)
            }
        }
    }

    BackupActionsCard(
        isBusy = uiState.isBusy,
        onExport = {
            runCatching { exportLauncher.launch(viewModel.defaultBackupFileName()) }
                .onFailure {
                    context.toast(YumeTxt.Feature.BackupRestore.Error.OpenOutputFailed)
                }
        },
        onRestore = {
            runCatching {
                restoreLauncher.launch(
                    arrayOf("application/zip", "application/octet-stream", "*/*")
                )
            }
                .onFailure {
                    context.toast(YumeTxt.Feature.BackupRestore.Error.OpenInputFailed)
                }
        },
    )

    RestoreStrategyCard(
        isBusy = uiState.isBusy,
        strategy = restoreStrategy,
        onStrategySelected = { restoreStrategy = it },
    )

    RestoreConfirmDialog(
        pick = restoreUri,
        onConfirm = { uri ->
            restoreUri = null
            viewModel.restoreBackup(uri, strategy = restoreStrategy)
        },
        onDismissRequest = { restoreUri = null },
    )
}

/**
 * Asks before a picked archive replaces local state. [pick] doubles as the dialog's visibility, so
 * every exit path clears it: cancel, confirm and the back gesture all leave nothing to restore.
 */
@Composable
private fun RestoreConfirmDialog(
    pick: Uri?,
    onConfirm: (Uri) -> Unit,
    onDismissRequest: () -> Unit,
) {
    AppConfirmDialog(
        show = pick != null,
        title = YumeTxt.Feature.BackupRestore.RestoreDialog.Title,
        message = YumeTxt.Feature.BackupRestore.RestoreDialog.Message,
        confirmText = YumeTxt.Feature.BackupRestore.RestoreTitle,
        cancelText = YumeTxt.Feature.BackupRestore.Cancel,
        onConfirm = { pick?.let(onConfirm) },
        onDismissRequest = onDismissRequest,
    )
}

/** Export and restore rows. Both only launch a system picker; the work starts with the result. */
@Composable
private fun BackupActionsCard(
    isBusy: Boolean,
    onExport: () -> Unit,
    onRestore: () -> Unit,
) {
    Title(YumeTxt.Feature.BackupRestore.ActionSection)
    AppCard {
        PreferenceArrowItem(
            title = YumeTxt.Feature.BackupRestore.ExportTitle,
            enabled = !isBusy,
            onClick = onExport,
        )
        PreferenceArrowItem(
            title = YumeTxt.Feature.BackupRestore.RestoreTitle,
            enabled = !isBusy,
            onClick = onRestore,
        )
    }

    SectionSummary(YumeTxt.Settings.More.BackupRestoreSummary)
}

/** How an incoming archive is merged into what the device already has. */
@Composable
private fun RestoreStrategyCard(
    isBusy: Boolean,
    strategy: BackupRestoreStrategy,
    onStrategySelected: (BackupRestoreStrategy) -> Unit,
) {
    Title(YumeTxt.Feature.BackupRestore.RestoreStrategySection)
    AppCard {
        GlassDropdownPreference(
            options =
                BackupRestoreStrategy.entries.map { option ->
                    GlassDropdownOption(
                        option,
                        when (option) {
                            BackupRestoreStrategy.Overwrite ->
                                YumeTxt.Feature.BackupRestore.RestoreOverwrite
                            BackupRestoreStrategy.Merge ->
                                YumeTxt.Feature.BackupRestore.RestoreMerge
                        },
                    )
                },
            selected = strategy,
            title = YumeTxt.Feature.BackupRestore.RestoreStrategyTitle,
            maxHeight = UiDp.dp200,
            enabled = !isBusy,
            onSelected = onStrategySelected,
        )
    }
    SectionSummary(
        when (strategy) {
            BackupRestoreStrategy.Overwrite ->
                YumeTxt.Feature.BackupRestore.RestoreOverwriteSummary
            BackupRestoreStrategy.Merge -> YumeTxt.Feature.BackupRestore.RestoreMergeSummary
        }
    )
}
