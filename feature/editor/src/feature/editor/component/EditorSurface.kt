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

package com.github.yumeyucca.yumebox.feature.editor.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.yumeyucca.yumebox.feature.editor.editor.CodeEditor
import com.github.yumeyucca.yumebox.feature.editor.editor.CodeEditorState

/**
 * The editing surface every editor screen shows: the code field filling the scaffold body below the
 * top bar, insets consumed so the caret never hides under the IME.
 *
 * Extracted because both screens that fill a scaffold body with the bare field drew this exact
 * inset chain by hand, and a missing [consumeWindowInsets] or [imePadding] is invisible until
 * someone types at the bottom of a file.
 */
internal fun Modifier.editorBodyInsets(paddingValues: PaddingValues): Modifier =
    fillMaxSize()
        .padding(paddingValues)
        .consumeWindowInsets(paddingValues)
        .imePadding()

@Composable
internal fun EditorSurface(
    state: CodeEditorState,
    onTextChange: (String) -> Unit,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.editorBodyInsets(paddingValues)
    ) {
        CodeEditor(
            state = state,
            modifier = Modifier.fillMaxSize(),
            onTextChange = onTextChange,
        )
    }
}
