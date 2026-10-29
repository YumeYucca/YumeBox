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

package com.github.yumeyucca.yumebox.feature.editor.theme


import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.luminance
import com.github.yumeyucca.yumebox.feature.editor.language.TextMateInitializer
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.widget.CodeEditor
import top.yukonga.miuix.kmp.theme.MiuixTheme

object EditorThemeManager {
    @Composable
    fun rememberEditorTheme(): EditorThemeState {
        val isDark = MiuixTheme.colorScheme.surface.luminance() < 0.5f

        return remember(isDark) {
            EditorThemeState(
                isDark = isDark,
                themeName = if (isDark) "cursor-dark" else "cursor-light-colorblind",
            )
        }
    }

    @Suppress("TooGenericExceptionCaught")
    fun applyTheme(editor: CodeEditor) {
        try {
            val themeRegistry = ThemeRegistry.getInstance()
            editor.colorScheme = TextMateColorScheme.create(themeRegistry)
        } catch (
            _: Exception
        ) { // fault barrier: TextMate/tm4e throws unspecified exceptions, fall back
            editor.colorScheme = EditorColorSynchronizer.createColorScheme(false)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    fun updateTheme(editor: CodeEditor, isDark: Boolean) {
        try {
            TextMateInitializer.setTheme(isDark)

            editor.colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
            EditorColorSynchronizer.updateColors(editor, isDark)
        } catch (
            _: Exception
        ) { // fault barrier: TextMate/tm4e throws unspecified exceptions, fall back
            editor.colorScheme = EditorColorSynchronizer.createColorScheme(isDark)
            EditorColorSynchronizer.updateColors(editor, isDark)
        }
    }
}

data class EditorThemeState(
    val isDark: Boolean,
    val themeName: String,
)
