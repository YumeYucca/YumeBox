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

@file:Suppress("UnusedSymbol", "FunctionName")

package com.github.yumeyucca.yumebox.feature.editor.screen


import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.github.yumeyucca.yumebox.common.util.toast
import com.github.yumeyucca.yumebox.feature.editor.editor.CodeEditor
import com.github.yumeyucca.yumebox.feature.editor.editor.rememberConfiguredCodeEditorState
import com.github.yumeyucca.yumebox.feature.editor.language.LanguageScope
import com.github.yumeyucca.yumebox.presentation.component.AppDialog
import com.github.yumeyucca.yumebox.presentation.component.DialogButtonRow
import com.github.yumeyucca.yumebox.presentation.component.Navigator
import com.github.yumeyucca.yumebox.presentation.component.TopBar
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.Atom
import com.github.yumeyucca.yumebox.presentation.icon.yume.Save
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold

@Composable
fun FullscreenEditorScreen(
    navigator: Navigator,
    title: String = YumeTxt.Editor.Title.Config,
    initialContent: String = "",
    language: LanguageScope = LanguageScope.Yaml,
    onSave: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val showDiscardDialog = remember { mutableStateOf(false) }

    val editorState =
        rememberConfiguredCodeEditorState(
            initialContent = initialContent,
            language = language,
            readOnly = false,
        )
    val scrollBehavior = MiuixScrollBehavior()

    fun handleBack() {
        if (editorState.isModified) {
            showDiscardDialog.value = true
        } else {
            navigator.navigateUp()
        }
    }

    BackHandler { handleBack() }

    Scaffold(
        topBar = {
            TopBar(
                title = title,
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(
                        modifier = Modifier.padding(end = UiDp.dp12),
                        onClick = {
                            if (editorState.format()) {
                                context.toast(YumeTxt.Editor.Message.FormatSuccess)
                            } else {
                                context.toast(YumeTxt.Editor.Message.FormatSkipped)
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Yume.Atom,
                            contentDescription = YumeTxt.Editor.Action.Format,
                        )
                    }

                    IconButton(
                        onClick = {
                            onSave(editorState.content)
                            editorState.resetModified()
                            navigator.navigateUp()
                        }
                    ) {
                        Icon(
                            imageVector = Yume.Save,
                            contentDescription = YumeTxt.Editor.Action.Save,
                        )
                    }
                },
            )
        }
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .consumeWindowInsets(paddingValues)
                    .imePadding()
        ) {
            CodeEditor(
                state = editorState,
                modifier = Modifier.fillMaxSize(),
                onTextChange = { newContent -> editorState.syncContentFromEditor() },
            )
        }
    }

    AppDialog(
        show = showDiscardDialog.value,
        title = YumeTxt.Editor.Discard.Title,
        summary = YumeTxt.Editor.Discard.Summary,
        onDismissRequest = { showDiscardDialog.value = false },
    ) {
        DialogButtonRow(
            onCancel = { showDiscardDialog.value = false },
            onConfirm = {
                showDiscardDialog.value = false
                navigator.navigateUp()
            },
            cancelText = YumeTxt.Component.Button.Cancel,
            confirmText = YumeTxt.Editor.Discard.Confirm,
        )
    }
}
