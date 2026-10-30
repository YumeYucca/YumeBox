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
import com.github.yumeyucca.yumebox.core.model.LanguageScope
import com.github.yumeyucca.yumebox.feature.meta.presentation.screen.CustomRoutingScreen
import com.github.yumeyucca.yumebox.presentation.component.Navigator
import com.github.yumeyucca.yumebox.presentation.navigation.Route
import com.github.yumeyucca.yumebox.presentation.navigation.LocalConfigPreviewSlot
import com.github.yumeyucca.yumebox.presentation.navigation.ConfigPreviewRequest

@Composable
fun CustomRoutingRoute(navigator: Navigator) {
    val configPreviewSlot = LocalConfigPreviewSlot.current
    CustomRoutingScreen(
        onNavigateBack = { navigator.pop() },
        onOpenYamlEditor = { title, content, onSave ->
            val previewId =
                configPreviewSlot.put(
                    ConfigPreviewRequest(
                        title = title,
                        content = content,
                        language = LanguageScope.Yaml,
                        onSave = onSave,
                    )
                )
            navigator.push(Route.OverrideConfigPreview(previewId))
        },
    )
}
