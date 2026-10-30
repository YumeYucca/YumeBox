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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import com.github.yumeyucca.yumebox.core.model.LanguageScope
import com.github.yumeyucca.yumebox.data.model.OverrideContentType
import com.github.yumeyucca.yumebox.feature.editor.screen.ConfigPreviewScreen
import com.github.yumeyucca.yumebox.presentation.component.Navigator
import com.github.yumeyucca.yumebox.presentation.navigation.ConfigPreviewRequest
import com.github.yumeyucca.yumebox.presentation.navigation.LocalConfigPreviewSlot
import com.github.yumeyucca.yumebox.presentation.navigation.Route
import com.github.yumeyucca.yumebox.feature.override.presentation.screen.OverrideListScreen
import com.github.yumeyucca.yumebox.feature.override.presentation.viewmodel.OverrideConfigViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun OverrideScreen(navigator: Navigator) {
    // Must share the same ViewModelStoreOwner instance as OverrideListScreen (koinViewModel),
    // not koinInject — inject creates/returns a separate instance and first paint stays empty.
    val overrideConfigViewModel: OverrideConfigViewModel = koinViewModel()
    val configPreviewSlot = LocalConfigPreviewSlot.current
    val scope = rememberCoroutineScope()

    OverrideListScreen(
        viewModel = overrideConfigViewModel,
        onOpenCodeEditor = { config ->
            // The stored content is read from disk, so the request is built on a coroutine: doing
            // it inline would block the frame that opens the editor.
            scope.launch {
                val previewId =
                    configPreviewSlot.put(
                        ConfigPreviewRequest(
                            title = config.name,
                            content = overrideConfigViewModel.getConfigContent(config.id) ?: config.content,
                            language = config.contentType.toLanguageScope(),
                            onSave =
                                if (overrideConfigViewModel.isBuiltInConfig(config.id)) {
                                    null
                                } else {
                                    { content ->
                                        if (!overrideConfigViewModel.saveConfigContent(config.id, content)) {
                                            error(YumeTxt.Override.Save.Failed)
                                        }
                                    }
                                },
                        )
                    )
                navigator.push(Route.OverrideConfigPreview(previewId))
            }
        },
    )
}

@Composable
fun OverrideConfigPreviewRoute(navigator: Navigator, previewId: Long) {
    // The request is a handoff, not state: whatever opened the preview put it there, so dropping it
    // on the way out cannot strand another screen.
    val configPreviewSlot = LocalConfigPreviewSlot.current
    // Pinned to this route's own handoff: the shell keeps the outgoing route composed for the whole
    // stack animation, so a newer preview can already be open while this one is still on screen.
    val ownRequest = remember(previewId) { configPreviewSlot.get(previewId) }
    DisposableEffect(previewId) {
        onDispose { configPreviewSlot.release(previewId) }
    }
    // Nothing to preview means the route outlived its request (process death); show the plain
    // read-only editor rather than a blank document.
    ConfigPreviewScreen(
        navigator = navigator,
        title = ownRequest?.title ?: YumeTxt.Editor.Title.Preview,
        initialContent = ownRequest?.content.orEmpty(),
        language = ownRequest?.language ?: LanguageScope.Yaml,
        onSave = ownRequest?.onSave,
    )
}

private fun OverrideContentType.toLanguageScope(): LanguageScope =
    when (this) {
        OverrideContentType.Yaml -> LanguageScope.Yaml
        OverrideContentType.JavaScript -> LanguageScope.JavaScript
    }
