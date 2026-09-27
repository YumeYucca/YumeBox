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

package com.github.yumeyucca.yumebox.presentation.screen

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.TextFieldValue
import com.github.yumeyucca.yumebox.common.util.toast
import com.github.yumeyucca.yumebox.data.model.OverrideConfig
import com.github.yumeyucca.yumebox.data.model.OverrideContentType
import com.github.yumeyucca.yumebox.presentation.component.AppActionBottomSheet
import com.github.yumeyucca.yumebox.presentation.component.AppBottomSheetCloseAction
import com.github.yumeyucca.yumebox.presentation.component.AppBottomSheetConfirmAction
import com.github.yumeyucca.yumebox.presentation.component.AppCard
import com.github.yumeyucca.yumebox.presentation.component.AppDialog
import com.github.yumeyucca.yumebox.presentation.component.OemTextField
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.presentation.viewmodel.OverrideConfigViewModel
import kotlinx.coroutines.launch
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

internal enum class OverrideConfigInputMode {
    CreateNew,
    LocalFile,
    NetworkUrl,
}

@Composable
internal fun CreateConfigDialog(
    show: MutableState<Boolean>,
    initialMode: OverrideConfigInputMode,
    onConfirmCreate: (String, OverrideContentType) -> Unit,
    onConfirmImport: suspend (String, String) -> Result<OverrideConfig>,
    onConfirmNetworkImport: suspend (String) -> Result<OverrideConfig>,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    var inputMode by remember(show.value, initialMode) { mutableStateOf(initialMode) }
    val nameTextFieldValueState = remember(show.value) { mutableStateOf(TextFieldValue()) }
    var contentType by remember(show.value) { mutableStateOf(OverrideContentType.Yaml) }
    var selectedImportUri by remember(show.value) { mutableStateOf<Uri?>(null) }
    var selectedImportFileName by remember(show.value) { mutableStateOf("") }
    var networkImportUrl by remember(show.value) { mutableStateOf("") }
    var isImporting by remember(show.value) { mutableStateOf(false) }
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
        }

    AppActionBottomSheet(
        show = show.value,
        title = YumeTxt.Override.Dialog.Create.Title,
        startAction = { AppBottomSheetCloseAction(onClick = onDismiss) },
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
                            val importUri = selectedImportUri ?: return@AppBottomSheetConfirmAction
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
                                            .onSuccess { show.value = false }
                                            .onFailure { error ->
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
                                        YumeTxt.Override.Import.FileError.format(error.message)
                                    )
                                }
                        }

                        OverrideConfigInputMode.NetworkUrl -> {
                            val url = networkImportUrl.trim()
                            scope.launch {
                                isImporting = true
                                onConfirmNetworkImport(url)
                                    .onSuccess { show.value = false }
                                    .onFailure { error ->
                                        context.toast(
                                            YumeTxt.Override.Import.NetworkError.format(
                                                error.message ?: YumeTxt.Util.Error.UnknownError
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
            if (inputMode == OverrideConfigInputMode.CreateNew) {
                OverrideTypeSelector(
                    selectedType = contentType,
                    onSelectedTypeChange = { contentType = it },
                )
            }
            when (inputMode) {
                OverrideConfigInputMode.CreateNew -> {
                    OemTextField(
                        value = nameTextFieldValueState.value,
                        onValueChange = { updatedTextFieldValue ->
                            nameTextFieldValueState.value = updatedTextFieldValue
                        },
                        label = YumeTxt.Override.Dialog.Create.Name,
                        useLabelAsPlaceholder = true,
                    )
                }

                OverrideConfigInputMode.LocalFile -> {
                    ImportOverrideFileContent(
                        modifier = Modifier.fillMaxWidth(),
                        fileName = selectedImportFileName,
                        onPickFile = { importConfigLauncher.launch("*/*") },
                    )
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
    val inputModeOptions = remember { OverrideConfigInputMode.entries.toList() }
    val selectedModeIndex = inputModeOptions.indexOf(selectedMode).coerceAtLeast(0)

    AppCard(applyHorizontalPadding = false) {
        OverlayDropdownPreference(
            title = YumeTxt.ProfilesPage.Type.Title,
            items = inputModeOptions.map(OverrideConfigInputMode::label),
            selectedIndex = selectedModeIndex,
            onSelectedIndexChange = { index ->
                inputModeOptions.getOrNull(index)?.let(onSelectedModeChange)
            },
        )
    }
}

@Composable
private fun OverrideTypeSelector(
    selectedType: OverrideContentType,
    onSelectedTypeChange: (OverrideContentType) -> Unit,
) {
    val contentTypeOptions = remember { OverrideContentType.entries.toList() }
    val selectedTypeIndex = contentTypeOptions.indexOf(selectedType).coerceAtLeast(0)

    AppCard(applyHorizontalPadding = false) {
        OverlayDropdownPreference(
            title = YumeTxt.Override.Dialog.Create.Type,
            items = contentTypeOptions.map { it.label },
            selectedIndex = selectedTypeIndex,
            onSelectedIndexChange = { index ->
                contentTypeOptions.getOrNull(index)?.let(onSelectedTypeChange)
            },
        )
    }
}

@Composable
private fun ImportOverrideFileContent(
    modifier: Modifier = Modifier,
    fileName: String,
    onPickFile: () -> Unit,
) {
    Box(modifier = modifier) {
        AppCard(applyHorizontalPadding = false) {
            BasicComponent(
                title = YumeTxt.ProfilesPage.Input.SelectFile,
                summary = fileName.ifBlank { YumeTxt.ProfilesPage.Validation.SelectFile },
                onClick = onPickFile,
            )
        }
    }
}

@Composable
private fun ImportOverrideNetworkContent(
    modifier: Modifier = Modifier,
    url: String,
    enabled: Boolean,
    onUrlChange: (String) -> Unit,
) {
    OemTextField(
        value = url,
        onValueChange = onUrlChange,
        label = YumeTxt.Override.Dialog.Create.Url,
        useLabelAsPlaceholder = true,
        singleLine = true,
        enabled = enabled,
        modifier = modifier,
    )
}

@Composable
internal fun DeleteConfirmDialog(
    show: MutableState<Boolean>,
    config: OverrideConfig?,
    viewModel: OverrideConfigViewModel,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    var isInUse by remember { mutableStateOf(false) }

    LaunchedEffect(show.value, config?.id) {
        isInUse = show.value && config != null && viewModel.isConfigInUse(config.id)
    }

    val summary =
        when {
            config == null -> ""
            isInUse -> YumeTxt.Override.Dialog.Delete.InUseMessage.format(config.name)
            else -> YumeTxt.Override.Dialog.Delete.Message.format(config.name)
        }

    AppDialog(
        show = show.value,
        title = YumeTxt.Override.Dialog.Delete.Title,
        summary = summary,
        onDismissRequest = onDismiss,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(UiDp.dp12)) {
            Button(modifier = Modifier.weight(1f), onClick = onDismiss) {
                Text(YumeTxt.Override.Dialog.Button.Cancel)
            }
            Button(
                modifier = Modifier.weight(1f),
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(text = YumeTxt.Override.Dialog.Button.Delete, color = colorScheme.onPrimary)
            }
        }
    }
}

internal val OverrideContentType.label: String
    get() =
        when (this) {
            OverrideContentType.Yaml -> "YAML"
            OverrideContentType.JavaScript -> "JavaScript"
        }

private val OverrideConfigInputMode.label: String
    get() =
        when (this) {
            OverrideConfigInputMode.CreateNew -> YumeTxt.Override.Action.New
            OverrideConfigInputMode.LocalFile -> YumeTxt.ProfilesPage.Type.LocalFile
            OverrideConfigInputMode.NetworkUrl -> YumeTxt.Override.Action.NetworkImport
        }
