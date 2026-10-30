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

package com.github.yumeyucca.yumebox.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import com.github.yumeyucca.yumebox.presentation.component.BottomBarDestination
import com.github.yumeyucca.yumebox.presentation.component.GlassTopAction
import com.github.yumeyucca.yumebox.presentation.component.GlassTopActionPill
import com.github.yumeyucca.yumebox.presentation.component.GlassTopIconButton
import com.github.yumeyucca.yumebox.presentation.component.LocalHandlePageChange
import com.github.yumeyucca.yumebox.presentation.component.TopBar
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.House
import com.github.yumeyucca.yumebox.presentation.icon.yume.ListChevronsUpDown
import com.github.yumeyucca.yumebox.presentation.icon.yume.Speed
import com.github.yumeyucca.yumebox.presentation.theme.LocalTopBarBackdrop
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.glass.GlassPopupAnchor
import top.yukonga.miuix.kmp.glass.rememberGlassPopupAnchor

/**
 * 顶栏"更多"弹窗的锚点与展开态。
 *
 * 锚点必须由顶栏持有(它贴在胶囊里的排序钮上),菜单内容则由调用方按 [ProxyTopBar] 的
 * menu 槽组合;这样顶栏不再逐个转发菜单数据,只做 chrome。
 */
@Stable
internal class ProxyMenuState internal constructor(
    internal val anchor: GlassPopupAnchor,
) {
    var expanded by mutableStateOf(false)
        private set

    fun toggle() {
        expanded = !expanded
    }

    fun dismiss() {
        expanded = false
    }
}

@Composable
internal fun ProxyTopBar(
    title: String,
    scrollBehavior: ScrollBehavior,
    // 测试全部节点;顶栏 chrome 在分组页/节点页不再区分,统一为这一个胶囊。
    onTestAllNodes: () -> Unit,
    // 弹窗锚点与展开态留在顶栏,菜单里有什么由调用方决定。
    menu: @Composable (ProxyMenuState) -> Unit,
) {
    val backdrop = LocalTopBarBackdrop.current
    val menuAnchor = rememberGlassPopupAnchor()
    val menuState = remember(menuAnchor) { ProxyMenuState(menuAnchor) }
    TopBar(
        title = title,
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            // 左侧玻璃圆钮:首页图标,点击切回首页 tab(与底栏 Home 同一入口)。
            val handlePageChange = LocalHandlePageChange.current
            GlassTopIconButton(
                action =
                    GlassTopAction(
                        icon = Yume.House,
                        contentDescription = YumeTxt.Component.BottomBar.Home,
                        onClick = { handlePageChange(BottomBarDestination.Home) },
                    ),
                backdrop = backdrop,
            )
        },
        // 测速与更多保持在同一个玻璃胶囊里，弹窗只由右侧更多按钮触发。
        actions = {
            GlassTopActionPill(
                actions =
                    listOf(
                        GlassTopAction(
                            icon = Yume.Speed,
                            contentDescription = YumeTxt.Proxy.Action.Test,
                            onClick = onTestAllNodes,
                        ),
                        GlassTopAction(
                            icon = Yume.ListChevronsUpDown,
                            contentDescription = YumeTxt.Proxy.Action.Sort,
                            onClick = menuState::toggle,
                            anchor = menuAnchor,
                            popup = { menu(menuState) },
                        ),
                    ),
                backdrop = backdrop,
            )
        },
    )
}
