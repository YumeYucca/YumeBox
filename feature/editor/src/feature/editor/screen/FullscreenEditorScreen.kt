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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.github.yumeyucca.yumebox.common.util.toast
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import com.github.yumeyucca.yumebox.feature.editor.component.EditorExitGuard
import com.github.yumeyucca.yumebox.feature.editor.component.EditorSurface
import com.github.yumeyucca.yumebox.feature.editor.editor.rememberConfiguredCodeEditorState
import com.github.yumeyucca.yumebox.core.model.LanguageScope
import com.github.yumeyucca.yumebox.presentation.component.GlassScaffold
import com.github.yumeyucca.yumebox.presentation.component.Navigator
import com.github.yumeyucca.yumebox.presentation.component.TopBar
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.Atom
import com.github.yumeyucca.yumebox.presentation.icon.yume.Save
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior

@Composable
fun FullscreenEditorScreen(
    navigator: Navigator,
    title: String = YumeTxt.Editor.Title.Config,
    initialContent: String = "",
    language: LanguageScope = LanguageScope.Yaml,
    onSave: (String) -> Unit = {},
) {
    val context = LocalContext.current

    val editorState =
        rememberConfiguredCodeEditorState(
            initialContent = initialContent,
            language = language,
        )
    val scrollBehavior = MiuixScrollBehavior()

    EditorExitGuard(
        hasUnsavedChanges = editorState.isModified,
        onNavigateUp = navigator::navigateUp,
    )

    GlassScaffold(
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
        EditorSurface(
            state = editorState,
            onTextChange = { editorState.syncContentFromEditor() },
            paddingValues = paddingValues,
        )
    }

}
