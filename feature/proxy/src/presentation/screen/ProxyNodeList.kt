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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.github.yumeyucca.yumebox.core.model.Proxy
import com.github.yumeyucca.yumebox.data.model.ProxySortMode
import com.github.yumeyucca.yumebox.domain.model.ProxyGroupInfo
import com.github.yumeyucca.yumebox.presentation.component.ScreenLazyColumn
import com.github.yumeyucca.yumebox.presentation.screen.node.GroupDelayRefreshIndicator
import com.github.yumeyucca.yumebox.presentation.screen.node.NodeCard
import com.github.yumeyucca.yumebox.presentation.screen.node.NodeSearchToolbar
import com.github.yumeyucca.yumebox.presentation.screen.node.nodeClickHandler
import com.github.yumeyucca.yumebox.presentation.screen.node.nodeGridItems
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.viewmodel.ProxyViewModel
import com.github.yumeyucca.yumebox.presentation.theme.AnimationSpecs
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.presentation.util.KeepLazyListTopAnchorOnReorder

private fun ProxyGroupInfo.filterNodes(query: String): List<Proxy> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return proxies
    return proxies.filter { proxy ->
        proxy.name.contains(normalizedQuery, ignoreCase = true) ||
            proxy.title.contains(normalizedQuery, ignoreCase = true) ||
            proxy.subtitle.contains(normalizedQuery, ignoreCase = true)
    }
}

/**
 * 一页节点列表。宿主只决定取值([ProxyNodePageState]),这里只负责把这一页画出来,
 * 所以不需要把 13 个取值再展开成 17 个参数。
 */
@Composable
internal fun ProxyNodePage(
    page: ProxyNodePageState,
    proxyViewModel: ProxyViewModel,
    onSearchQueryChange: (String) -> Unit,
    onTestDelay: () -> Unit,
) {
    val group = page.group ?: return
    val sortMode = page.sortMode
    val testingGroupNames = page.testingGroupNames
    val testingProxyNames = page.testingProxyNames
    val twoColumnGrid = page.twoColumnNodes
    val gridState = page.gridState
    val searchQuery = page.searchQuery
    val searchVisible = page.searchVisible
    val outerInnerPadding = page.outerInnerPadding
    val mainInnerPadding = page.mainInnerPadding
    val scrollBehavior = page.scrollBehavior
    val listState = page.listState
    val topOverlayInset = page.topOverlayInset
    val delayTestProgress = proxyViewModel.delayTestProgress
    val onTestProxyDelay = proxyViewModel::testProxyDelay
    val onSelectProxy: (groupName: String, proxyName: String) -> Unit = proxyViewModel::selectProxy
    val spacing = AppTheme.spacing
    val visibleProxies = remember(group.proxies, searchQuery) { group.filterNodes(searchQuery) }
    val listItemKeys = remember(group.proxies) { group.proxies.map { it.name } }
    val resolvedGridState = if (twoColumnGrid) gridState ?: rememberLazyGridState() else null
    // 可选中分组点击即选中;不可选中的分组(自动选择/负载均衡)没有可选目标,点击改测速。
    val nodeClick =
        remember(group, onSelectProxy, onTestDelay) {
            nodeClickHandler(
                group = group,
                onSelectProxy = { proxyName -> onSelectProxy(group.name, proxyName) },
                onTestDelay = onTestDelay,
            )
        }

    GroupDelayTestListAnchor(
        listState = listState,
        itemKeys = listItemKeys,
        sortByLatency = sortMode == ProxySortMode.BY_LATENCY && !twoColumnGrid,
        groupName = group.name,
        testingGroupNames = testingGroupNames,
    )

    val contentPadding =
        PaddingValues(
            start = UiDp.dp12,
            end = UiDp.dp12,
            top =
                outerInnerPadding.calculateTopPadding() +
                    (if (topOverlayInset > UiDp.dp0) topOverlayInset else UiDp.dp20) +
                    (if (twoColumnGrid) UiDp.dp5 else UiDp.dp0),
            bottom = mainInnerPadding.calculateBottomPadding() + spacing.space12,
        )

    if (twoColumnGrid) {
        val activeGridState = resolvedGridState ?: return
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = activeGridState,
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(UiDp.dp8),
            verticalArrangement = Arrangement.spacedBy(UiDp.dp8),
            modifier =
                Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
        ) {
            if (searchVisible) {
                item(key = "__node_search__", span = { GridItemSpan(maxLineSpan) }) {
                    NodeSearchSlot(
                        visible = true,
                        query = searchQuery,
                        onQueryChange = onSearchQueryChange,
                    )
                }
            }
            if (group.name in testingGroupNames) {
                item(key = "__refresh_indicator__", span = { GridItemSpan(maxLineSpan) }) {
                    GroupDelayRefreshIndicator(
                        groupName = group.name,
                        testingGroupNames = testingGroupNames,
                        progress = delayTestProgress,
                    )
                }
            }
            itemsIndexed(items = visibleProxies, key = { _, proxy -> proxy.name }) { _, proxy ->
                NodeCard(
                    proxy = proxy,
                    isSelected = proxy.name == group.now,
                    onClick = nodeClick,
                    onTestClick = onTestProxyDelay,
                    isDelayTesting = testingProxyNames.contains(proxy.name),
                    showCountryFlag = true,
                    compact = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        return
    }

    ScreenLazyColumn(
        lazyListState = listState,
        scrollBehavior = scrollBehavior,
        innerPadding = outerInnerPadding,
        contentPadding = contentPadding,
    ) {
        // 搜索框常驻槽位:展开/收起走 AnimatedVisibility 高度过渡,而不是硬切。
        item(key = "__node_search__") {
            NodeSearchSlot(
                visible = searchVisible,
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
            )
        }
        item(key = "__refresh_indicator__") {
            GroupDelayRefreshIndicator(
                groupName = group.name,
                testingGroupNames = testingGroupNames,
                progress = delayTestProgress,
            )
        }
        nodeGridItems(
            proxies = visibleProxies,
            selectedProxyName = group.now,
            onProxyClick = nodeClick,
            onProxyTest = onTestProxyDelay,
            testingProxyNames = testingProxyNames,
            outerHorizontalPadding = UiDp.dp0,
            itemVerticalPadding = UiDp.dp5,
        )
    }
}

@Composable
private fun NodeSearchSlot(
    visible: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter =
            expandVertically(
                tween(durationMillis = 280, easing = AnimationSpecs.Legacy)
            ) + fadeIn(tween(durationMillis = 160, easing = AnimationSpecs.Legacy)),
        exit =
            shrinkVertically(
                tween(durationMillis = 240, easing = AnimationSpecs.Legacy)
            ) + fadeOut(tween(durationMillis = 120, easing = AnimationSpecs.Legacy)),
    ) {
        NodeSearchToolbar(
            query = query,
            onQueryChange = onQueryChange,
        )
    }
}

@Composable
private fun GroupDelayTestListAnchor(
    listState: LazyListState,
    itemKeys: List<String>,
    sortByLatency: Boolean,
    groupName: String,
    testingGroupNames: Set<String>,
) {
    KeepLazyListTopAnchorOnReorder(
        listState = listState,
        itemKeys = itemKeys,
        enabled = sortByLatency && groupName !in testingGroupNames,
        scrollToTopOnEnabled = true,
    )
}
