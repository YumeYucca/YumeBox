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

package com.github.yumeyucca.yumebox.feature.editor.screen


import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.github.yumeyucca.yumebox.common.util.toast
import com.github.yumeyucca.yumebox.feature.editor.editor.CodeEditor
import com.github.yumeyucca.yumebox.feature.editor.editor.rememberConfiguredCodeEditorState
import com.github.yumeyucca.yumebox.feature.editor.format.CodeFormatter
import com.github.yumeyucca.yumebox.feature.editor.language.LanguageScope
import com.github.yumeyucca.yumebox.presentation.component.Navigator
import com.github.yumeyucca.yumebox.presentation.component.SmallTopBar
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.ArrowLeft
import com.github.yumeyucca.yumebox.presentation.icon.yume.ArrowRight
import com.github.yumeyucca.yumebox.presentation.icon.yume.ListCollapse
import com.github.yumeyucca.yumebox.presentation.icon.yume.Save
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import kotlinx.coroutines.launch
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold

@Composable
fun ConfigPreviewScreen(
    navigator: Navigator,
    title: String = YumeTxt.Editor.Title.Preview,
    initialContent: String = "",
    language: LanguageScope = LanguageScope.Yaml,
    onSave: (suspend (String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }

    val formattedContent =
        remember(initialContent, language) {
            if (language == LanguageScope.Json) {
                CodeFormatter.format(initialContent, language) ?: initialContent
            } else {
                initialContent
            }
        }

    val editorState =
        rememberConfiguredCodeEditorState(
            initialContent = formattedContent,
            language = language,
            readOnly = false,
        )
    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        topBar = {
            SmallTopBar(
                title = title,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    Row(horizontalArrangement = Arrangement.spacedBy(UiDp.dp12)) {
                        IconButton(
                            onClick = { editorState.undo() },
                            enabled = editorState.canUndo(),
                        ) {
                            Icon(Yume.ArrowLeft, null)
                        }
                        IconButton(
                            onClick = { editorState.redo() },
                            enabled = editorState.canRedo(),
                        ) {
                            Icon(Yume.ArrowRight, null)
                        }
                    }
                },
                actions = {
                    IconButton(
                        modifier = Modifier.padding(end = UiDp.dp12),
                        onClick = { editorState.format() },
                    ) {
                        Icon(
                            Yume.ListCollapse,
                            contentDescription = YumeTxt.Editor.Action.Format,
                        )
                    }
                    IconButton(
                        onClick = {
                            if (isSaving || onSave == null) return@IconButton
                            coroutineScope.launch {
                                isSaving = true
                                runCatching { onSave(editorState.content) }
                                    .onSuccess {
                                        editorState.resetModified()
                                        navigator.navigateUp()
                                    }
                                    .onFailure {
                                        context.toast(
                                            it.message ?: YumeTxt.Editor.Message.SaveFailed
                                        )
                                    }
                                isSaving = false
                            }
                        },
                        enabled = onSave != null && editorState.isModified && !isSaving,
                    ) {
                        Icon(Yume.Save, contentDescription = YumeTxt.Editor.Action.Save)
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
            CodeEditor(state = editorState, modifier = Modifier.fillMaxSize())
        }
    }
}
