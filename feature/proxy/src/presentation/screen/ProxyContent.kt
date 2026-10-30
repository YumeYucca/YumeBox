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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import com.github.yumeyucca.yumebox.data.model.ProxySortMode
import com.github.yumeyucca.yumebox.domain.model.ProxyGroupInfo
import com.github.yumeyucca.yumebox.presentation.component.ScreenLazyColumn
import com.github.yumeyucca.yumebox.presentation.screen.node.nodeGroupItems
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import top.yukonga.miuix.kmp.basic.ScrollBehavior

@Composable
internal fun ProxyContent(
    proxyGroups: List<ProxyGroupInfo>,
    scrollBehavior: ScrollBehavior,
    innerPadding: PaddingValues,
    mainInnerPadding: PaddingValues,
    onGroupClick: (ProxyGroupInfo) -> Unit,
    onGroupTest: (ProxyGroupInfo) -> Unit,
    testingGroupNames: Set<String>,
    listState: LazyListState,
    topOverlayInset: Dp = UiDp.dp0,
) {
    val spacing = AppTheme.spacing
    ScreenLazyColumn(
        lazyListState = listState,
        scrollBehavior = scrollBehavior,
        innerPadding = innerPadding,
        contentPadding =
            PaddingValues(
                start = UiDp.dp12,
                end = UiDp.dp12,
                top =
                    innerPadding.calculateTopPadding() +
                        if (topOverlayInset > UiDp.dp0) topOverlayInset else UiDp.dp14,
                bottom = mainInnerPadding.calculateBottomPadding() + spacing.space12,
            ),
    ) {
        nodeGroupItems(
            groups = proxyGroups,
            onGroupClick = onGroupClick,
            onGroupTest = onGroupTest,
            testingGroupNames = testingGroupNames,
            itemVerticalPadding = UiDp.dp5,
        )
    }
}

/**
 * 节点列表页的取值。三种宿主(分组 tab 分页、手机分栏、分栏右页)共用同一份,
 * 每个调用点只 copy 自己不同的几项,不再各自摆 15 个参数。
 */
@Immutable
internal data class ProxyNodePageState(
    val sortMode: ProxySortMode,
    val testingGroupNames: Set<String>,
    val testingProxyNames: Set<String>,
    val mainInnerPadding: PaddingValues,
    val outerInnerPadding: PaddingValues,
    val scrollBehavior: ScrollBehavior,
    val listState: LazyListState,
    val gridState: LazyGridState?,
    val twoColumnNodes: Boolean,
    val searchQuery: String,
    val searchVisible: Boolean,
    val group: ProxyGroupInfo? = null,
    val topOverlayInset: Dp = UiDp.dp0,
)
