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

package com.github.yumeyucca.yumebox.screen.navigation


import androidx.compose.runtime.Composable
import com.github.yumeyucca.yumebox.data.model.OverrideContentType
import com.github.yumeyucca.yumebox.feature.editor.language.LanguageScope
import com.github.yumeyucca.yumebox.feature.editor.screen.ConfigPreviewScreen
import com.github.yumeyucca.yumebox.presentation.component.Navigator
import com.github.yumeyucca.yumebox.presentation.navigation.Route
import com.github.yumeyucca.yumebox.presentation.screen.OverrideListScreen
import com.github.yumeyucca.yumebox.presentation.util.OverrideEditorStore
import com.github.yumeyucca.yumebox.presentation.viewmodel.OverrideConfigViewModel
import org.koin.androidx.compose.koinViewModel
import com.github.yumeyucca.yumebox.core.locale.YumeTxt

@Composable
fun OverrideScreen(navigator: Navigator) {
    // Must share the same ViewModelStoreOwner instance as OverrideListScreen (koinViewModel),
    // not koinInject — inject creates/returns a separate instance and first paint stays empty.
    val overrideConfigViewModel: OverrideConfigViewModel = koinViewModel()

    OverrideListScreen(
        viewModel = overrideConfigViewModel,
        onOpenCodeEditor = { config ->
            OverrideEditorStore.setupConfigPreview(
                title = config.name,
                content = overrideConfigViewModel.getConfigContent(config.id) ?: config.content,
                language = config.contentType.toLanguageScope(),
                callback =
                    if (overrideConfigViewModel.isBuiltInConfig(config.id)) {
                        null
                    } else {
                        { content ->
                            if (!overrideConfigViewModel.saveConfigContent(config.id, content)) {
                                error(YumeTxt.Override.Save.Failed)
                            }
                        }
                    }
            )
            navigator.push(Route.OverrideConfigPreview)
        },
    )
}

@Composable
fun OverrideConfigPreviewRoute(navigator: Navigator) {
    ConfigPreviewScreen(
        navigator = navigator,
        title = OverrideEditorStore.configPreviewTitle,
        initialContent = OverrideEditorStore.configPreviewContent,
        language = OverrideEditorStore.configPreviewLanguage,
        onSave = OverrideEditorStore.configPreviewCallback,
    )
}

private fun OverrideContentType.toLanguageScope(): LanguageScope =
    when (this) {
        OverrideContentType.Yaml -> LanguageScope.Yaml
        OverrideContentType.JavaScript -> LanguageScope.JavaScript
    }
