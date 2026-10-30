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

package com.github.yumeyucca.yumebox.feature.override.presentation.screen

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.github.yumeyucca.yumebox.common.util.toast
import com.github.yumeyucca.yumebox.data.model.OverrideConfig
import com.github.yumeyucca.yumebox.data.model.OverrideContentType
import com.github.yumeyucca.yumebox.presentation.component.AppActionBottomSheet
import com.github.yumeyucca.yumebox.common.util.ByteFormatter
import com.github.yumeyucca.yumebox.presentation.component.AppBottomSheetConfirmAction
import com.github.yumeyucca.yumebox.presentation.component.AppCard
import com.github.yumeyucca.yumebox.presentation.component.AppTextFieldDialog
import com.github.yumeyucca.yumebox.presentation.component.AppTextFieldRow
import com.github.yumeyucca.yumebox.presentation.component.GlassDropdownOption
import com.github.yumeyucca.yumebox.presentation.component.GlassDropdownPreference
import com.github.yumeyucca.yumebox.presentation.component.PreferenceArrowItem
import com.github.yumeyucca.yumebox.presentation.component.SegmentedTabRow
import com.github.yumeyucca.yumebox.presentation.util.readDisplaySize
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

internal enum class OverrideConfigInputMode {
    CreateNew,
    LocalFile,
    NetworkUrl,
}

@Composable
internal fun CreateConfigDialog(
    show: Boolean,
    initialMode: OverrideConfigInputMode,
    onConfirmCreate: (String, OverrideContentType) -> Unit,
    onConfirmImport: suspend (String, String) -> Result<OverrideConfig>,
    onConfirmNetworkImport: suspend (String) -> Result<OverrideConfig>,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    var inputMode by remember(show, initialMode) { mutableStateOf(initialMode) }
    val nameTextFieldValueState = remember(show) { mutableStateOf(TextFieldValue()) }
    var contentType by remember(show) { mutableStateOf(OverrideContentType.Yaml) }
    var selectedImportUri by remember(show) { mutableStateOf<Uri?>(null) }
    var selectedImportFileName by remember(show) { mutableStateOf("") }
    var selectedImportFileSize by remember(show) { mutableStateOf(0L) }
    var networkImportUrl by remember(show) { mutableStateOf("") }
    var isImporting by remember(show) { mutableStateOf(false) }
    // Exposed as state (not a plain val) because it is read inside the sheet's endAction lambda:
    // the overlay-hosted title-bar row can be re-rendered from a stale composable after the app
    // returns from background, and only a state read there keeps it subscribed to updates.
    val canConfirm by rememberUpdatedState(
        when (inputMode) {
            OverrideConfigInputMode.CreateNew -> nameTextFieldValueState.value.text.isNotBlank()
            OverrideConfigInputMode.LocalFile ->
                selectedImportUri != null && selectedImportFileName.isNotBlank()

            OverrideConfigInputMode.NetworkUrl -> networkImportUrl.isNotBlank() && !isImporting
        }
    )
    val importConfigLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
            selectedImportUri = uri
            selectedImportFileName =
                uri?.let { selectedUri ->
                    context.contentResolver
                        .query(
                            selectedUri,
                            arrayOf(OpenableColumns.DISPLAY_NAME),
                            null,
                            null,
                            null,
                        )
                        ?.use { cursor ->
                            val columnIndex =
                                cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (cursor.moveToFirst() && columnIndex >= 0) {
                                cursor.getString(columnIndex)
                            } else {
                                ""
                            }
                        }
                        .orEmpty()
                        .ifBlank {
                            selectedUri.lastPathSegment
                                ?.substringAfterLast('/')
                                ?.substringAfterLast('\\')
                                .orEmpty()
                        }
                }
                    .orEmpty()
            selectedImportFileSize = uri?.let { readDisplaySize(context, it) } ?: 0L
        }

    AppActionBottomSheet(
        show = show,
        // Title follows the mode: "新建配置" / "本地文件" / "链接导入" say what the sheet is about,
        // where a single generic "添加配置" made all three look identical.
        title = inputMode.label,
        endAction = {
            AppBottomSheetConfirmAction(
                enabled = canConfirm && !isImporting,
                contentDescription = YumeTxt.Override.Action.Create,
                onClick = {
                    if (!canConfirm) return@AppBottomSheetConfirmAction
                    keyboardController?.hide()
                    when (inputMode) {
                        OverrideConfigInputMode.CreateNew -> {
                            onConfirmCreate(nameTextFieldValueState.value.text, contentType)
                        }

                        OverrideConfigInputMode.LocalFile -> {
                            val importUri = selectedImportUri
                                ?: return@AppBottomSheetConfirmAction
                            runCatching {
                                context.contentResolver
                                    .openInputStream(importUri)
                                    ?.bufferedReader()
                                    ?.use { reader -> reader.readText() }
                                    ?: error(YumeTxt.Override.Import.ReadError)
                            }
                                .onSuccess { content ->
                                    scope.launch {
                                        isImporting = true
                                        onConfirmImport(content, selectedImportFileName)
                                            .onSuccess { onDismiss() }
                                            .onFailure { error ->
                                                // Cancelled by leaving the dialog, not a failed read.
                                                if (error is CancellationException) throw error
                                                context.toast(
                                                    error.message
                                                        ?: YumeTxt.Override.Import.ReadError
                                                )
                                            }
                                        isImporting = false
                                    }
                                }
                                .onFailure { error ->
                                    context.toast(
                                        YumeTxt.Override.Import.FileError.format(
                                            error.message
                                        )
                                    )
                                }
                        }

                        OverrideConfigInputMode.NetworkUrl -> {
                            val url = networkImportUrl.trim()
                            scope.launch {
                                isImporting = true
                                onConfirmNetworkImport(url)
                                    .onSuccess { onDismiss() }
                                    .onFailure { error ->
                                        // Cancelled by leaving the dialog, not a failed download.
                                        if (error is CancellationException) throw error
                                        context.toast(
                                            YumeTxt.Override.Import.NetworkError.format(
                                                error.message
                                                    ?: YumeTxt.Util.Error.UnknownError
                                            )
                                        )
                                    }
                                isImporting = false
                            }
                        }
                    }
                },
            )
        },
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .animateContentSize(animationSpec = tween(200, easing = FastOutSlowInEasing))
                    .padding(bottom = UiDp.dp16),
            verticalArrangement = Arrangement.spacedBy(UiDp.dp16),
        ) {
            OverrideInputModeSelector(
                selectedMode = inputMode,
                onSelectedModeChange = { inputMode = it },
            )
            when (inputMode) {
                OverrideConfigInputMode.CreateNew -> {
                    AppCard(applyHorizontalPadding = false) {
                        Column {
                            OverrideTypeSelector(
                                selectedType = contentType,
                                onSelectedTypeChange = { contentType = it },
                            )
                            AppTextFieldRow(
                                label = YumeTxt.Override.Dialog.Create.Name,
                                value = nameTextFieldValueState.value,
                                onValueChange = { updatedTextFieldValue ->
                                    nameTextFieldValueState.value = updatedTextFieldValue
                                },
                                hint = YumeTxt.ProfilesPage.Input.Optional,
                            )
                        }
                    }
                }

                OverrideConfigInputMode.LocalFile -> {
                    AppCard(applyHorizontalPadding = false) {
                        ImportOverrideFileContent(
                            modifier = Modifier.fillMaxWidth(),
                            fileSize = selectedImportFileSize,
                            onPickFile = { importConfigLauncher.launch("*/*") },
                        )
                    }
                }

                OverrideConfigInputMode.NetworkUrl -> {
                    ImportOverrideNetworkContent(
                        modifier = Modifier.fillMaxWidth(),
                        url = networkImportUrl,
                        enabled = !isImporting,
                        onUrlChange = { networkImportUrl = it },
                    )
                }
            }
        }
    }
}

@Composable
private fun OverrideInputModeSelector(
    selectedMode: OverrideConfigInputMode,
    onSelectedModeChange: (OverrideConfigInputMode) -> Unit,
) {
    val inputModeOptions = OverrideConfigInputMode.entries
    val selectedModeIndex = inputModeOptions.indexOf(selectedMode).coerceAtLeast(0)

    SegmentedTabRow(
        tabs = inputModeOptions.map(OverrideConfigInputMode::label),
        selectedTabIndex = selectedModeIndex,
        onTabSelected = { index ->
            inputModeOptions.getOrNull(index)?.let(onSelectedModeChange)
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun OverrideTypeSelector(
    selectedType: OverrideContentType,
    onSelectedTypeChange: (OverrideContentType) -> Unit,
) {
    GlassDropdownPreference(
        title = YumeTxt.Override.Dialog.Create.Type,
        options = OverrideContentType.entries.map { GlassDropdownOption(it, it.displayName) },
        selected = selectedType,
        onSelected = onSelectedTypeChange,
    )
}

@Composable
private fun ImportOverrideFileContent(
    modifier: Modifier = Modifier,
    fileSize: Long,
    onPickFile: () -> Unit,
) {
    PreferenceArrowItem(
        title = YumeTxt.ProfilesPage.Input.SelectFile,
        modifier = modifier,
        // The right-hand text is the "already picked" signal — the file size once a file is
        // chosen, the faint 可选 tag before that.
        endActions = {
            Text(
                text =
                    if (fileSize > 0L) {
                        ByteFormatter.format(fileSize)
                    } else {
                        YumeTxt.ProfilesPage.Input.Optional
                    },
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
            )
        },
        onClick = onPickFile,
    )
}

@Composable
private fun ImportOverrideNetworkContent(
    modifier: Modifier = Modifier,
    url: String,
    enabled: Boolean,
    onUrlChange: (String) -> Unit,
) {
    AppCard(modifier = modifier, applyHorizontalPadding = false) {
        AppTextFieldRow(
            label = YumeTxt.Override.Dialog.Create.Url,
            value = url,
            onValueChange = onUrlChange,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        )
    }
}

@Composable
internal fun RenameConfigDialog(
    show: Boolean,
    config: OverrideConfig?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember(show, config?.id) { mutableStateOf(config?.name.orEmpty()) }

    fun confirmRename() {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) onConfirm(trimmed)
    }

    AppTextFieldDialog(
        show = show,
        title = YumeTxt.Override.Dialog.Rename,
        value = name,
        onValueChange = { name = it },
        onDismissRequest = onDismiss,
        onConfirm = { confirmRename() },
        singleLine = true,
        onImeAction = { confirmRename() },
    )
}

private val OverrideConfigInputMode.label: String
    get() =
        when (this) {
            OverrideConfigInputMode.CreateNew -> YumeTxt.Override.Action.New
            OverrideConfigInputMode.LocalFile -> YumeTxt.ProfilesPage.Type.LocalFile
            OverrideConfigInputMode.NetworkUrl -> YumeTxt.Override.Action.NetworkImport
        }
