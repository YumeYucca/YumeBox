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
 *
 */
package com.github.yumeyucca.yumebox.presentation.screen.node

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Search
import top.yukonga.miuix.kmp.icon.basic.SearchCleanup
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Node page search field. The delay-test / sort / locate actions used to live beside it; they now
 * sit in the top bar's glass pill+circle chrome (see [NodeListPage] callers and ProxyTopBar).
 */
@Composable
internal fun NodeSearchToolbar(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val colorScheme = MiuixTheme.colorScheme
    // 胶囊与页面共用同一层"悬浮白"材质,叠在 Scaffold 的 surface 灰底上。
    val toolbarSurface = colorScheme.background
    val imeVisible = WindowInsets.ime.getBottom(density) > 0

    fun collapseSearch() {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    LaunchedEffect(imeVisible) {
        if (!imeVisible) {
            focusManager.clearFocus(force = true)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = UiDp.dp4, bottom = UiDp.dp8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MiuixTheme(colors = colorScheme.copy(surfaceContainerHigh = toolbarSurface)) {
            InputField(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = { collapseSearch() },
                expanded = false,
                onExpandedChange = { requesting ->
                    if (!requesting) {
                        collapseSearch()
                    }
                },
                label = YumeTxt.Component.Editor.Action.Search,
                leadingIcon = {
                    Icon(
                        imageVector = MiuixIcons.Basic.Search,
                        contentDescription = YumeTxt.Component.Editor.Action.Search,
                        modifier =
                            Modifier
                                .size(UiDp.dp44)
                                .padding(start = UiDp.dp16, end = UiDp.dp8),
                    )
                },
                trailingIcon = {
                    AnimatedVisibility(visible = query.isNotEmpty()) {
                        Icon(
                            imageVector = MiuixIcons.Basic.SearchCleanup,
                            contentDescription = YumeTxt.Component.Button.Clear,
                            modifier =
                                Modifier
                                    .size(UiDp.dp44)
                                    .padding(start = UiDp.dp8, end = UiDp.dp16)
                                    .clickable { onQueryChange("") },
                        )
                    }
                },
                modifier =
                    Modifier
                        .weight(1f)
                        .shadow(
                            elevation = UiDp.dp6,
                            shape = CircleShape,
                            clip = false,
                            ambientColor = Color.Black.copy(alpha = 0.06f),
                            spotColor = Color.Black.copy(alpha = 0.12f),
                            ),
            )
        }
    }
}
