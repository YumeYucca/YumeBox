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

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import com.github.yumeyucca.yumebox.feature.editor.component.EditorExitGuard
import com.github.yumeyucca.yumebox.feature.editor.component.EditorSurface
import com.github.yumeyucca.yumebox.feature.editor.editor.rememberConfiguredCodeEditorState
import com.github.yumeyucca.yumebox.core.model.LanguageScope
import com.github.yumeyucca.yumebox.feature.editor.viewmodel.ConfigEditorViewModel
import com.github.yumeyucca.yumebox.feature.editor.viewmodel.ConfigType
import com.github.yumeyucca.yumebox.presentation.component.GlassScaffold
import com.github.yumeyucca.yumebox.presentation.component.Navigator
import com.github.yumeyucca.yumebox.presentation.component.TopBar
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior

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
        )
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

    GlassScaffold(
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
        EditorSurface(
            state = editorState,
            onTextChange = { content -> viewModel.updateDraft(content) },
            paddingValues = paddingValues,
        )

        EditorExitGuard(
            hasUnsavedChanges = session.isDirty || editorState.isModified,
            onNavigateUp = navigator::navigateUp,
            onDiscard = viewModel::discardDraft,
        )
    }
}
