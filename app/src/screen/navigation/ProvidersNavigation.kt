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


import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import com.github.yumeyucca.yumebox.presentation.component.Navigator
import com.github.yumeyucca.yumebox.presentation.screen.ProvidersContent
import top.yukonga.miuix.kmp.basic.ScrollBehavior

@Composable
fun ProvidersScreen(
    navigator: Navigator,
    embedded: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(),
    scrollBehaviorOverride: ScrollBehavior? = null,
) {
    ProvidersContent(
        navigator = navigator,
        embedded = embedded,
        contentPadding = contentPadding,
        scrollBehaviorOverride = scrollBehaviorOverride,
    )
}
