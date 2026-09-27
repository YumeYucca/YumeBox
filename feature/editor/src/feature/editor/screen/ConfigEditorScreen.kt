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
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.github.yumeyucca.yumebox.feature.editor.editor.CodeEditor
import com.github.yumeyucca.yumebox.feature.editor.editor.rememberConfiguredCodeEditorState
import com.github.yumeyucca.yumebox.feature.editor.language.LanguageScope
import com.github.yumeyucca.yumebox.feature.editor.viewmodel.ConfigEditorViewModel
import com.github.yumeyucca.yumebox.feature.editor.viewmodel.ConfigType
import com.github.yumeyucca.yumebox.presentation.component.AppDialog
import com.github.yumeyucca.yumebox.presentation.component.DialogButtonRow
import com.github.yumeyucca.yumebox.presentation.component.Navigator
import com.github.yumeyucca.yumebox.presentation.component.TopBar
import org.koin.androidx.compose.koinViewModel
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold

@Composable
fun ConfigEditorScreen(
    navigator: Navigator,
    configId: String,
    configType: ConfigType = ConfigType.Override,
    initialContent: String = "",
    language: LanguageScope = LanguageScope.Yaml,
) {
    val viewModel: ConfigEditorViewModel = koinViewModel()
    val session by viewModel.session.collectAsState()
    val editorState =
        rememberConfiguredCodeEditorState(
            initialContent = initialContent,
            language = language,
            readOnly = false,
        )
    val showDiscardDialog = remember { mutableStateOf(false) }
    val scrollBehavior = MiuixScrollBehavior()

    LaunchedEffect(configId) {
        viewModel.loadConfig(
            configId = configId,
            configType = configType,
            initialContent = initialContent,
        )
    }

    LaunchedEffect(session.configId, session.savedContent) {
        if (session.configId == configId) {
            editorState.loadContent(session.draftContent)
        }
    }

    BackHandler {
        if (session.isDirty || editorState.isModified) {
            showDiscardDialog.value = true
        } else {
            navigator.navigateUp()
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title =
                    when (configType) {
                        ConfigType.Override -> YumeTxt.Editor.Title.Override
                        ConfigType.Profile -> YumeTxt.Editor.Title.Profile
                    },
                scrollBehavior = scrollBehavior,
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
                onTextChange = { content -> viewModel.updateDraft(content) },
            )
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
                    viewModel.discardDraft()
                    navigator.navigateUp()
                },
                cancelText = YumeTxt.Component.Button.Cancel,
                confirmText = YumeTxt.Editor.Discard.Confirm,
            )
        }
    }
}
