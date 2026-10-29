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

@file:Suppress("UnusedSymbol", "FunctionName")

package com.github.yumeyucca.yumebox.feature.editor.theme

import androidx.compose.ui.graphics.toArgb
import com.github.yumeyucca.yumebox.feature.editor.editor.IndentGuideCodeEditor
import com.github.yumeyucca.yumebox.presentation.theme.AppColors
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

object EditorColorSynchronizer {
    fun createColorScheme(isDark: Boolean, appColors: AppColors = AppColors()): EditorColorScheme {
        val editorColors = appColors.editor
        return object : EditorColorScheme(isDark) {
            override fun applyDefault() {
                super.applyDefault()
                applyCursorPalette(this, isDark)

                setColor(
                    TEXT_ACTION_WINDOW_BACKGROUND,
                    if (isDark) {
                        editorColors.darkTextActionBackground.toArgb()
                    } else {
                        editorColors.lightTextActionBackground.toArgb()
                    },
                )
                setColor(
                    TEXT_ACTION_WINDOW_ICON_COLOR,
                    if (isDark) {
                        editorColors.darkTextActionIcon.toArgb()
                    } else {
                        editorColors.lightTextActionIcon.toArgb()
                    },
                )
            }
        }
    }

    fun updateColors(editor: CodeEditor, isDark: Boolean, appColors: AppColors = AppColors()) {
        val editorColors = appColors.editor
        val scheme = editor.colorScheme

        applyCursorPalette(scheme, isDark)
        if (editor is IndentGuideCodeEditor) {
            val palette = if (isDark) DarkCursorPalette else LightCursorPalette
            editor.indentGuideColor = palette.blockLine
            editor.activeIndentGuideColor = palette.blockLineCurrent
        }
        scheme.setColor(
            EditorColorScheme.TEXT_ACTION_WINDOW_BACKGROUND,
            if (isDark) {
                editorColors.darkTextActionBackground.toArgb()
            } else {
                editorColors.lightTextActionBackground.toArgb()
            },
        )
        scheme.setColor(
            EditorColorScheme.TEXT_ACTION_WINDOW_ICON_COLOR,
            if (isDark) {
                editorColors.darkTextActionIcon.toArgb()
            } else {
                editorColors.lightTextActionIcon.toArgb()
            },
        )
    }

    private fun applyCursorPalette(scheme: EditorColorScheme, isDark: Boolean) {
        val palette = if (isDark) DarkCursorPalette else LightCursorPalette
        scheme.setColor(EditorColorScheme.WHOLE_BACKGROUND, palette.background)
        scheme.setColor(EditorColorScheme.TEXT_NORMAL, palette.foreground)
        scheme.setColor(EditorColorScheme.LINE_NUMBER, palette.lineNumber)
        scheme.setColor(EditorColorScheme.LINE_NUMBER_CURRENT, palette.lineNumberCurrent)
        scheme.setColor(EditorColorScheme.LINE_NUMBER_BACKGROUND, palette.background)
        scheme.setColor(EditorColorScheme.CURRENT_LINE, palette.currentLine)
        scheme.setColor(EditorColorScheme.SELECTION_INSERT, palette.cursor)
        scheme.setColor(EditorColorScheme.SELECTION_HANDLE, palette.cursor)
        scheme.setColor(EditorColorScheme.SELECTED_TEXT_BACKGROUND, palette.selection)
        scheme.setColor(EditorColorScheme.BLOCK_LINE, palette.blockLine)
        scheme.setColor(EditorColorScheme.BLOCK_LINE_CURRENT, palette.blockLineCurrent)
        scheme.setColor(EditorColorScheme.TEXT_INLAY_HINT_FOREGROUND, palette.inlayHint)
        scheme.setColor(EditorColorScheme.TEXT_INLAY_HINT_BACKGROUND, palette.inlayHintBackground)
        scheme.setColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_FOREGROUND, palette.foreground)
        scheme.setColor(
            EditorColorScheme.HIGHLIGHTED_DELIMITERS_BACKGROUND,
            palette.bracketMatchBackground,
        )
    }

    private data class CursorPalette(
        val background: Int,
        val foreground: Int,
        val lineNumber: Int,
        val lineNumberCurrent: Int,
        val currentLine: Int,
        val cursor: Int,
        val selection: Int,
        val blockLine: Int,
        val blockLineCurrent: Int,
        val inlayHint: Int,
        val inlayHintBackground: Int,
        val bracketMatchBackground: Int,
    )

    // Values from the installed Cursor Light Colorblind / Cursor Dark themes.
    private val LightCursorPalette =
        CursorPalette(
            background = 0xFFFCFCFC.toInt(),
            foreground = 0xFF141414.toInt(),
            lineNumber = 0x5C141414,
            lineNumberCurrent = 0xBD141414.toInt(),
            currentLine = 0xFFEAEAEA.toInt(),
            cursor = 0xFF141414.toInt(),
            selection = 0x14141414,
            blockLine = 0x14141414,
            blockLineCurrent = 0x33141414,
            inlayHint = 0xBD141414.toInt(),
            inlayHintBackground = 0x00FCFCFC,
            bracketMatchBackground = 0x24141414,
        )

    private val DarkCursorPalette =
        CursorPalette(
            background = 0xFF181818.toInt(),
            foreground = 0xFFF0F0F0.toInt(),
            lineNumber = 0x5CF0F0F0,
            lineNumberCurrent = 0xFFF0F0F0.toInt(),
            currentLine = 0xFF262626.toInt(),
            cursor = 0xFFF0F0F0.toInt(),
            selection = 0x99404040.toInt(),
            blockLine = 0x13F0F0F0,
            blockLineCurrent = 0x30F0F0F0,
            inlayHint = 0x5CF0F0F0,
            inlayHintBackground = 0x00000000,
            bracketMatchBackground = 0x1EF0F0F0,
        )
}
