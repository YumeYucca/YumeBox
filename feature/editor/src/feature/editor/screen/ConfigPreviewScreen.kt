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
 * Copyright (c) YumeYucca 2025 - Present
 */

@file:Suppress("FunctionName")

package com.github.yumeyucca.yumebox.feature.editor.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.github.yumeyucca.yumebox.common.util.toast
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import com.github.yumeyucca.yumebox.feature.editor.component.EditorExitGuard
import com.github.yumeyucca.yumebox.feature.editor.component.editorBodyInsets
import com.github.yumeyucca.yumebox.feature.editor.editor.CodeEditor
import com.github.yumeyucca.yumebox.feature.editor.editor.DefaultEditorFontSizeSp
import com.github.yumeyucca.yumebox.feature.editor.editor.rememberConfiguredCodeEditorState
import com.github.yumeyucca.yumebox.feature.editor.format.CodeFormatter
import com.github.yumeyucca.yumebox.core.model.LanguageScope
import com.github.yumeyucca.yumebox.presentation.component.GlassScaffold
import com.github.yumeyucca.yumebox.presentation.component.GlassTopAction
import com.github.yumeyucca.yumebox.presentation.component.GlassTopActionDefaults
import com.github.yumeyucca.yumebox.presentation.component.GlassTopActionPill
import com.github.yumeyucca.yumebox.presentation.component.Navigator
import com.github.yumeyucca.yumebox.presentation.component.SmallTopBar
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.Check
import com.github.yumeyucca.yumebox.presentation.icon.yume.ListChevronsUpDown
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.theme.LocalTopBarBackdrop
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.glass.rememberGlassPopupAnchor
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = LocalTopBarBackdrop.current
    val actionsAnchor = rememberGlassPopupAnchor()

    var isSaving by remember { mutableStateOf(false) }
    var showActions by remember { mutableStateOf(false) }
    var fontSizeSp by rememberSaveable { mutableFloatStateOf(DefaultEditorFontSizeSp) }

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
        )

    val saveContent: () -> Unit = {
        val save = onSave
        if (!isSaving && save != null) {
            isSaving = true
            coroutineScope.launch {
                try {
                    runCatching { save(editorState.content) }
                        .onSuccess {
                            editorState.resetModified()
                            navigator.navigateUp()
                        }
                        .onFailure { error ->
                            // Leaving the screen cancels this scope; that is not a save failure.
                            if (error is CancellationException) throw error
                            context.toast(error.message ?: YumeTxt.Editor.Message.SaveFailed)
                        }
                } finally {
                    isSaving = false
                }
            }
        }
    }

    GlassScaffold(
        topBar = {
            SmallTopBar(
                title = title,
                scrollBehavior = scrollBehavior,
                actionIconPadding = UiDp.dp12,
                actions = {
                    GlassTopActionPill(
                        actions =
                            listOf(
                                GlassTopAction(
                                    icon = Yume.Check,
                                    contentDescription = YumeTxt.Editor.Action.Save,
                                    onClick = saveContent,
                                    tint =
                                        if (onSave != null && !isSaving) {
                                            Color.Unspecified
                                        } else {
                                            MiuixTheme.colorScheme.onSurface.copy(
                                                alpha = AppTheme.opacity.muted,
                                            )
                                        },
                                ),
                                GlassTopAction(
                                    icon = Yume.ListChevronsUpDown,
                                    contentDescription = YumeTxt.Proxy.Action.More,
                                    onClick = { showActions = true },
                                    anchor = actionsAnchor,
                                    popup = {
                                        EditorActionsMenu(
                                            show = showActions,
                                            onDismiss = { showActions = false },
                                            anchor = actionsAnchor,
                                            anchorContent = {
                                                Icon(
                                                    imageVector = Yume.ListChevronsUpDown,
                                                    contentDescription = null,
                                                    modifier =
                                                        Modifier.size(
                                                            GlassTopActionDefaults.IconSize
                                                        ),
                                                    tint = MiuixTheme.colorScheme.onSurface,
                                                )
                                            },
                                            canUndo = editorState.canUndo(),
                                            canRedo = editorState.canRedo(),
                                            fontSizeSp = fontSizeSp,
                                            onUndo = { editorState.undo() },
                                            onRedo = { editorState.redo() },
                                            onFormat = {
                                                if (editorState.format()) {
                                                    context.toast(
                                                        YumeTxt.Editor.Message.FormatSuccess
                                                    )
                                                } else {
                                                    context.toast(
                                                        YumeTxt.Editor.Message.FormatSkipped
                                                    )
                                                }
                                            },
                                            onFontSizeSelected = { fontSizeSp = it },
                                        )
                                    },
                                ),
                            ),
                        backdrop = backdrop,
                    )
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier.editorBodyInsets(paddingValues),
        ) {
            CodeEditor(
                state = editorState,
                modifier = Modifier.weight(1f),
                fontSizeSp = fontSizeSp,
                onScroll = { scroll ->
                    scrollBehavior.state.contentOffset = -scroll.toFloat()
                },
            )
            EditorSymbolBar(
                onSymbol = { action ->
                    editorState.editor?.let { editor ->
                        when (action) {
                            EditorSymbolAction.Indent -> editor.indentOrCommitTab()
                            is EditorSymbolAction.Insert -> editor.insertText(action.text, 1)
                        }
                    }
                },
            )
        }

        EditorExitGuard(
            hasUnsavedChanges = editorState.isModified,
            onNavigateUp = navigator::navigateUp,
        )
    }
}
