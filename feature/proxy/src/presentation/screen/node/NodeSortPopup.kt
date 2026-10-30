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

package com.github.yumeyucca.yumebox.presentation.screen.node

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import com.github.yumeyucca.yumebox.data.model.ProxySortMode
import com.github.yumeyucca.yumebox.presentation.component.GlassMenuDivider
import com.github.yumeyucca.yumebox.presentation.component.GlassMenuItem
import com.github.yumeyucca.yumebox.presentation.component.GlassPopupHost
import com.github.yumeyucca.yumebox.presentation.component.GlassPopupSubmenuItem
import com.github.yumeyucca.yumebox.presentation.component.GlassSubmenuPageHeader
import com.github.yumeyucca.yumebox.presentation.component.GlassTopActionAnchorIcon
import com.github.yumeyucca.yumebox.presentation.component.rememberGlassSubmenuState
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.ListChevronsUpDown
import io.github.yumeyucca.lucide.Lucide
import io.github.yumeyucca.lucide.lucide.ArrowUpDown
import io.github.yumeyucca.lucide.lucide.Columns2
import io.github.yumeyucca.lucide.lucide.Gift
import io.github.yumeyucca.lucide.lucide.Search
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.glass.GlassPopupAnchor

internal val NodeSortModes =
    listOf(ProxySortMode.DEFAULT, ProxySortMode.BY_NAME, ProxySortMode.BY_LATENCY)

/**
 * Everything the "more" menu can offer, as one value.
 *
 * A capability that is absent never renders, and each capability carries its own state together
 * with the callback that changes it, so a caller cannot half-provide one action (the old shape had
 * two nullable callbacks driving a single row, which could disagree) and the menu never repeats
 * `?.invoke`. Row order and labels stay owned by [NodeActionsMenu], not by the caller.
 */
@Immutable
internal data class NodeMenuActions(
    val sortMode: ProxySortMode,
    val onSortSelected: (ProxySortMode) -> Unit,
    /** Layout page; empty for the split shell's left pane, which has no node list. */
    val layout: NodeLayoutActions? = null,
    /** Entry that opens the providers/routing page. */
    val routing: (() -> Unit)? = null,
    /** Search toggle row; empty on pages that cannot search nodes. */
    val search: NodeSearchActions? = null,
)

@Immutable
internal data class NodeLayoutActions(
    val twoColumnNodes: Boolean,
    val onTwoColumnNodesChange: (Boolean) -> Unit,
    val groupTabsMode: Boolean,
    val onGroupTabsModeChange: (Boolean) -> Unit,
)

@Immutable
internal data class NodeSearchActions(
    val visible: Boolean,
    val onToggle: () -> Unit,
)

/**
 * 二级页只有两个,页面自己带图标和标题取值,菜单里不再写两份并行的 when。
 * 标题用 lambda 求值:语言是运行时可切换的,YumeTxt 的 val 都是 getter,
 * 在枚举初始化时取值会把当时的语言固化下来。
 */
private enum class NodeSubmenu(
    val icon: ImageVector,
    val label: () -> String,
) {
    Layout(icon = Lucide.Columns2, label = { YumeTxt.Proxy.Action.Layout }),
    Sort(icon = Lucide.ArrowUpDown, label = { YumeTxt.Proxy.Action.Sort }),
}

@Composable
internal fun NodeActionsMenu(
    show: Boolean,
    onDismiss: () -> Unit,
    actions: NodeMenuActions,
    anchor: GlassPopupAnchor,
    anchorContent: @Composable () -> Unit = { GlassTopActionAnchorIcon(Yume.ListChevronsUpDown) },
) {
    val submenu = rememberGlassSubmenuState(show)
    var submenuPage by remember { mutableStateOf(NodeSubmenu.Layout) }
    val layout = actions.layout
    val routing = actions.routing
    val search = actions.search
    // 布局页只在有布局动作时存在:入口列表由枚举本身决定,不再逐个 if 手写一遍。
    val submenuPages = if (layout != null) NodeSubmenu.entries else listOf(NodeSubmenu.Sort)

    GlassPopupHost(
        show = show,
        onDismissRequest = onDismiss,
        anchor = anchor,
        anchorContent = anchorContent,
        submenu = submenu,
        submenuContent = {
            GlassSubmenuPageHeader(
                state = submenu,
                text = submenuPage.label(),
                icon = submenuPage.icon,
                anchor = anchor,
            )
            when (submenuPage) {
                NodeSubmenu.Layout -> {
                    layout?.let { available ->
                        NodeLayoutItems(available, onDismiss = submenu::close)
                    }
                }
                NodeSubmenu.Sort -> {
                    NodeSortItems(
                        sortMode = actions.sortMode,
                        onSortSelected = actions.onSortSelected,
                        onDismiss = submenu::close,
                    )
                }
            }
        },
    ) {
        if (routing != null) {
            GlassMenuItem(
                text = YumeTxt.Providers.TabTitle,
                icon = Lucide.Gift,
                onClick = {
                    routing()
                    onDismiss()
                },
            )
        }
        if (search != null) {
            GlassMenuItem(
                text = YumeTxt.Connection.Search,
                icon = Lucide.Search,
                selected = search.visible,
                onClick = {
                    search.onToggle()
                    onDismiss()
                },
            )
        }
        submenuPages.forEach { page ->
            GlassPopupSubmenuItem(
                text = page.label(),
                icon = page.icon,
                anchor = anchor,
                open = submenu.open && submenuPage == page,
                onOpen = { bounds ->
                    submenuPage = page
                    submenu.openAt(bounds)
                },
            )
        }
    }
}

/** The layout page of the "more" menu: column count, then the group-tab switch. */
@Composable
private fun NodeLayoutItems(
    actions: NodeLayoutActions,
    onDismiss: () -> Unit,
) {
    GlassMenuItem(
        text = YumeTxt.Proxy.DisplayMode.Single,
        selected = !actions.twoColumnNodes,
        onClick = {
            actions.onTwoColumnNodesChange(false)
            onDismiss()
        },
    )
    GlassMenuItem(
        text = YumeTxt.Proxy.DisplayMode.Double,
        selected = actions.twoColumnNodes,
        onClick = {
            actions.onTwoColumnNodesChange(true)
            onDismiss()
        },
    )
    GlassMenuDivider()
    GlassMenuItem(
        text = YumeTxt.Proxy.Action.TabsMode,
        selected = actions.groupTabsMode,
        onClick = {
            actions.onGroupTabsModeChange(!actions.groupTabsMode)
            onDismiss()
        },
    )
}

/** The sort modes as menu rows; shared by the actions submenu and the standalone sort popup. */
@Composable
private fun NodeSortItems(
    sortMode: ProxySortMode,
    onSortSelected: (ProxySortMode) -> Unit,
    onDismiss: () -> Unit,
) {
    NodeSortModes.forEach { mode ->
        GlassMenuItem(
            text = mode.displayName,
            selected = mode == sortMode,
            onClick = {
                if (mode != sortMode) onSortSelected(mode)
                onDismiss()
            },
        )
    }
}

@Composable
internal fun NodeSortPopupOverlay(
    show: Boolean,
    onDismiss: () -> Unit,
    anchor: GlassPopupAnchor,
    backdrop: Backdrop?,
    sortMode: ProxySortMode,
    onSortSelected: (ProxySortMode) -> Unit,
) {
    GlassPopupHost(
        show = show,
        onDismissRequest = onDismiss,
        anchor = anchor,
        anchorContent = { GlassTopActionAnchorIcon(Yume.ListChevronsUpDown) },
        backdrop = backdrop,
        alignToPageEnd = false,
    ) {
        NodeSortItems(
            sortMode = sortMode,
            onSortSelected = onSortSelected,
            onDismiss = onDismiss,
        )
    }
}
