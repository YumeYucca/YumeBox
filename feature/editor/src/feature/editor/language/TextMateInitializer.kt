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

package com.github.yumeyucca.yumebox.feature.editor.language


import android.content.Context
import com.github.yumeyucca.yumebox.core.model.LanguageScope
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.GrammarDefinition
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import io.github.rosemoe.sora.langs.textmate.registry.reader.LanguageDefinitionReader
import io.github.rosemoe.sora.widget.CodeEditor
import org.eclipse.tm4e.core.internal.oniguruma.impl.onig.NativeOnigConfig
import org.eclipse.tm4e.core.registry.IThemeSource
import timber.log.Timber

object TextMateInitializer {
    private const val TAG = "TextMateInitializer"

    private const val THEME_DARK = "cursor-dark"
    private const val THEME_LIGHT = "cursor-light-colorblind"

    private var initialized = false
    private var grammarDefinitions: Map<LanguageScope, GrammarDefinition> = emptyMap()

    @Suppress("TooGenericExceptionCaught")
    @Synchronized
    fun initialize(context: Context) {
        if (initialized) {
            Timber.tag(TAG).d("TextMate already initialized, skipping")
            return
        }

        try {
            Timber.tag(TAG).i("Initializing TextMate...")

            NativeOnigConfig.setSearchInBatch(false)

            registerFileProvider(context)

            loadThemes()

            loadGrammars()

            initialized = true
            Timber.tag(TAG).i("TextMate initialized successfully")
        } catch (error: Exception) { // fault barrier: TextMate/tm4e throws unspecified exceptions
            Timber.e(error, "Failed to initialize TextMate")
        }
    }

    fun isInitialized(): Boolean = initialized

    private fun registerFileProvider(context: Context) {
        Timber.tag(TAG).d("Registering file provider...")
        FileProviderRegistry.getInstance()
            .addFileProvider(AssetsFileResolver(context.applicationContext.assets))
    }

    private fun loadThemes() {
        Timber.tag(TAG).d("Loading themes...")
        val themeRegistry = ThemeRegistry.getInstance()

        loadTheme(themeRegistry, THEME_DARK, "textmate/cursor-dark.json", isDark = true)

        loadTheme(
            themeRegistry,
            THEME_LIGHT,
            "textmate/cursor-light-colorblind.json",
            isDark = false,
        )

        themeRegistry.setTheme(THEME_DARK)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun loadTheme(
        themeRegistry: ThemeRegistry,
        name: String,
        path: String,
        isDark: Boolean,
    ) {
        try {
            val inputStream = FileProviderRegistry.getInstance().tryGetInputStream(path)
            if (inputStream != null) {
                val themeSource = IThemeSource.fromInputStream(inputStream, path, null)
                val themeModel = ThemeModel(themeSource, name).apply { this.isDark = isDark }
                themeRegistry.loadTheme(themeModel)
                Timber.tag(TAG).d("Theme '$name' loaded successfully")
            } else {
                Timber.tag(TAG).w("Theme file not found: $path")
            }
        } catch (error: Exception) { // fault barrier: TextMate/tm4e throws unspecified exceptions
            Timber.e(error, "Failed to load theme: $name")
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun loadGrammars() {
        Timber.tag(TAG).d("Loading grammars...")
        try {
            val definitions = LanguageDefinitionReader.read("textmate/languages.json")
            // A language whose display name is missing from languages.json keeps plain-text
            // editing instead of failing the whole load: one gap must not cost every other
            // language its highlighting for the rest of the process.
            val missing = mutableListOf<String>()
            grammarDefinitions =
                LanguageScope.entries
                    .filterNot { it == LanguageScope.Text }
                    .mapNotNull { language ->
                        definitions
                            .firstOrNull { it.name.equals(language.displayName, ignoreCase = true) }
                            ?.let { definition -> language to definition }
                            ?: run {
                                missing += language.displayName
                                null
                            }
                    }
                    .toMap()
            if (missing.isNotEmpty()) {
                Timber.tag(TAG).w("No grammar definition for: ${missing.joinToString()}")
            }
            Timber.tag(TAG).d("Grammar definitions loaded successfully")
        } catch (error: Exception) { // fault barrier: TextMate/tm4e throws unspecified exceptions
            Timber.e(error, "Failed to load grammars")
        }
    }

    @Suppress("TooGenericExceptionCaught")
    fun setLanguage(editor: CodeEditor, language: LanguageScope) {
        if (language == LanguageScope.Text) {
            editor.setEditorLanguage(null)
            return
        }

        try {
            val definition =
                grammarDefinitions[language] ?: error("Grammar not found: ${language.scopeName}")
            val themeRegistry = ThemeRegistry.getInstance()
            val grammarRegistry =
                GrammarRegistry(GrammarRegistry.getInstance()).apply {
                    setTheme(themeRegistry.currentThemeModel)
                }
            val textMateLanguage =
                TextMateLanguage.create(
                    definition,
                    grammarRegistry,
                    themeRegistry,
                    true,
                )
            editor.setEditorLanguage(textMateLanguage)
            Timber.tag(TAG).d("Language set to: ${language.displayName}")
        } catch (error: Exception) { // fault barrier: TextMate/tm4e throws unspecified exceptions
            Timber.e(error, "Failed to set language: ${language.displayName}")
            editor.setEditorLanguage(null)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    fun setTheme(isDark: Boolean) {
        try {
            val themeName = if (isDark) THEME_DARK else THEME_LIGHT
            ThemeRegistry.getInstance().setTheme(themeName)
            Timber.tag(TAG).d("Theme switched to: $themeName")
        } catch (error: Exception) { // fault barrier: TextMate/tm4e throws unspecified exceptions
            Timber.e(error, "Failed to switch theme")
        }
    }
}
