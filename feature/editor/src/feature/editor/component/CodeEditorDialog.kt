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

@file:Suppress("UnusedSymbol", "FunctionName", "KotlinConstantConditions")

package com.github.yumeyucca.yumebox.feature.editor.component

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.yumeyucca.yumebox.feature.editor.editor.CodeEditor
import com.github.yumeyucca.yumebox.feature.editor.editor.rememberConfiguredCodeEditorState
import com.github.yumeyucca.yumebox.feature.editor.language.LanguageScope
import com.github.yumeyucca.yumebox.presentation.component.AppDialog
import com.github.yumeyucca.yumebox.presentation.component.DialogButtonRow
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun CodeEditorDialog(
    show: Boolean,
    title: String,
    subtitle: String? = null,
    value: String?,
    language: LanguageScope = LanguageScope.Json,
    placeholder: String = "",
    onValueChange: (String?) -> Unit,
    onDismiss: () -> Unit = {},
) {
    val editorState =
        rememberConfiguredCodeEditorState(
            initialContent = value ?: "",
            language = language,
            readOnly = false,
        )

    if (show) {
        AppDialog(show = show, title = title, onDismissRequest = onDismiss) {
            Column(modifier = Modifier.padding(UiDp.dp20)) {
                subtitle?.let { text ->
                    Text(
                        text = text,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.outline,
                    )
                    Spacer(modifier = Modifier.height(UiDp.dp12))
                }

                CodeEditor(
                    state = editorState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = UiDp.dp280, max = UiDp.dp400),
                    onTextChange = {},
                )

                Spacer(modifier = Modifier.height(UiDp.dp24))

                DialogButtonRow(
                    onCancel = onDismiss,
                    onConfirm = {
                        onValueChange(editorState.content.takeIf { it.isNotBlank() })
                        onDismiss()
                    },
                    cancelText = YumeTxt.Component.Button.Cancel,
                    confirmText = YumeTxt.Component.Button.Confirm,
                )
            }
        }
    }
}

@Composable
fun YamlEditorDialog(
    show: Boolean,
    title: String,
    subtitle: String? = null,
    value: String?,
    onValueChange: (String?) -> Unit,
    onDismiss: () -> Unit = {},
) {
    CodeEditorDialog(
        show = show,
        title = title,
        subtitle = subtitle,
        value = value,
        language = LanguageScope.Yaml,
        onValueChange = onValueChange,
        onDismiss = onDismiss,
    )
}

@Composable
fun JsonEditorDialog(
    show: Boolean,
    title: String,
    subtitle: String? = YumeTxt.Editor.JsonSubtitle,
    value: String?,
    onValueChange: (String?) -> Unit,
    onDismiss: () -> Unit = {},
) {
    CodeEditorDialog(
        show = show,
        title = title,
        subtitle = subtitle,
        value = value,
        language = LanguageScope.Json,
        onValueChange = onValueChange,
        onDismiss = onDismiss,
    )
}
