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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
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
import com.github.yumeyucca.yumebox.feature.editor.editor.CodeEditor
import com.github.yumeyucca.yumebox.feature.editor.editor.rememberConfiguredCodeEditorState
import com.github.yumeyucca.yumebox.feature.editor.format.CodeFormatter
import com.github.yumeyucca.yumebox.feature.editor.language.LanguageScope
import com.github.yumeyucca.yumebox.presentation.component.AppDialog
import com.github.yumeyucca.yumebox.presentation.component.DialogButtonRow
import com.github.yumeyucca.yumebox.presentation.component.GlassScaffold
import com.github.yumeyucca.yumebox.presentation.component.GlassTopAction
import com.github.yumeyucca.yumebox.presentation.component.GlassTopActionDefaults
import com.github.yumeyucca.yumebox.presentation.component.GlassTopActionPill
import com.github.yumeyucca.yumebox.presentation.component.Navigator
import com.github.yumeyucca.yumebox.presentation.component.SmallTopBar
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.Check
import com.github.yumeyucca.yumebox.presentation.icon.yume.ListChevronsUpDown
import com.github.yumeyucca.yumebox.presentation.theme.LocalTopBarBackdrop
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.glass.glassPopupAnchor
import top.yukonga.miuix.kmp.glass.glassPopupAnchorContent
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
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showActions by remember { mutableStateOf(false) }
    var fontSizeSp by rememberSaveable { mutableFloatStateOf(DEFAULT_EDITOR_FONT_SIZE_SP) }

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

    fun hasUnsavedChanges(): Boolean =
        editorState.isModified ||
            editorState.editor?.text?.toString()?.let { it != formattedContent } == true

    BackHandler {
        if (hasUnsavedChanges()) {
            showDiscardDialog = true
        } else {
            navigator.navigateUp()
        }
    }

    val saveContent: () -> Unit = {
        if (!isSaving && onSave != null) {
            coroutineScope.launch {
                val save = onSave
                isSaving = true
                val content = editorState.editor?.text?.toString() ?: editorState.content
                runCatching { save(content) }
                    .onSuccess {
                        editorState.resetModified()
                        navigator.navigateUp()
                    }
                    .onFailure {
                        context.toast(it.message ?: YumeTxt.Editor.Message.SaveFailed)
                    }
                isSaving = false
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
                                            MiuixTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                                        },
                                ),
                                GlassTopAction(
                                    icon = Yume.ListChevronsUpDown,
                                    contentDescription = YumeTxt.Proxy.Action.More,
                                    onClick = { showActions = true },
                                    modifier =
                                        Modifier.glassPopupAnchor(
                                            anchor = actionsAnchor,
                                            cornerRadius = GlassTopActionDefaults.IconSize / 2,
                                            floating = true,
                                        ),
                                    iconModifier =
                                        Modifier.glassPopupAnchorContent(actionsAnchor),
                                ),
                            ),
                        popupAnchor = actionsAnchor,
                        backdrop = backdrop,
                        actionOverlay = { index ->
                            if (index == 1) {
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
                            }
                        },
                    )
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .consumeWindowInsets(paddingValues)
                    .imePadding(),
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
                onInsert = { text ->
                    editorState.editor?.let { editor ->
                        if (text == "\t") {
                            editor.indentOrCommitTab()
                        } else {
                            editor.insertText(text, 1)
                        }
                    }
                },
            )
        }

        AppDialog(
            show = showDiscardDialog,
            title = YumeTxt.Editor.Discard.Title,
            summary = YumeTxt.Editor.Discard.Summary,
            onDismissRequest = { showDiscardDialog = false },
        ) {
            DialogButtonRow(
                onCancel = { showDiscardDialog = false },
                onConfirm = {
                    showDiscardDialog = false
                    navigator.navigateUp()
                },
                cancelText = YumeTxt.Component.Button.Cancel,
                confirmText = YumeTxt.Editor.Discard.Confirm,
            )
        }
    }
}

private const val DEFAULT_EDITOR_FONT_SIZE_SP = 14f
