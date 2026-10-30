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

package com.github.yumeyucca.yumebox.feature.editor.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.github.yumeyucca.yumebox.feature.editor.format.CodeFormatter
import com.github.yumeyucca.yumebox.core.model.LanguageScope
import io.github.rosemoe.sora.widget.CodeEditor

/** The one default text size for every editor surface: preview, fullscreen and the dialog. */
internal const val DefaultEditorFontSizeSp = 14f

class CodeEditorState(
    initialContent: String = "",
    val language: LanguageScope = LanguageScope.Yaml,
) {
    var editor: CodeEditor? by mutableStateOf(null)
        internal set

    private var canUndoState: Boolean by mutableStateOf(false)
    private var canRedoState: Boolean by mutableStateOf(false)

    var content: String by mutableStateOf(initialContent)
        private set

    var isModified: Boolean by mutableStateOf(false)
        private set

    fun loadContent(newContent: String) {
        if (content != newContent) {
            content = newContent
            editor?.setText(newContent)
        }
        isModified = false
        refreshHistoryState()
    }

    fun syncContentFromEditor() {
        editor?.text?.toString()?.let { editorContent ->
            if (content != editorContent) {
                content = editorContent
                isModified = true
            }
        }
        refreshHistoryState()
    }

    fun resetModified() {
        isModified = false
    }

    fun undo() {
        editor?.undo()
        syncContentFromEditor()
    }

    fun redo() {
        editor?.redo()
        syncContentFromEditor()
    }

    fun canUndo(): Boolean = canUndoState

    fun canRedo(): Boolean = canRedoState

    internal fun refreshHistoryState() {
        canUndoState = editor?.canUndo() == true
        canRedoState = editor?.canRedo() == true
    }

    fun format(): Boolean {
        val formatted = CodeFormatter.format(content, language)
        return if (formatted != null && formatted != content) {
            content = formatted
            editor?.setText(formatted)
            isModified = true
            refreshHistoryState()
            true
        } else {
            false
        }
    }
}
