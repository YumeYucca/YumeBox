/*
 * YumeBox - 一个简洁的 Clash 客户端
 * Copyright (C) 2024-2026 YumeYuka
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.yumeyucca.yumebox.screen.profiles

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import com.github.yumeyucca.yumebox.presentation.component.GlassScaffold
import com.github.yumeyucca.yumebox.presentation.component.StandalonePageList
import com.github.yumeyucca.yumebox.presentation.component.TopBar
import top.yukonga.miuix.kmp.basic.ScrollBehavior

/**
 * 订阅设置里「玻璃顶栏 + 单页列表」子页面的统一外壳。
 *
 * [ProfileEditScreen] 和 [ProfileOverrideScreen] 原本各自抄了一遍 Scaffold/TopBar/StandalonePageList
 * 的三层嵌套,差别只有标题和顶栏动作;再往后的子页面也走这里。
 */
@Composable
internal fun ProfileSettingsPage(
    title: String,
    scrollBehavior: ScrollBehavior,
    listState: LazyListState,
    actions: @Composable RowScope.() -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    GlassScaffold(
        topBar = {
            TopBar(
                title = title,
                scrollBehavior = scrollBehavior,
                actions = actions,
            )
        },
    ) { paddingValues ->
        StandalonePageList(
            scrollBehavior = scrollBehavior,
            scaffoldPadding = paddingValues,
            lazyListState = listState,
            content = content,
        )
    }
}
