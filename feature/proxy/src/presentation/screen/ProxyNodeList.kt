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

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import com.github.yumeyucca.yumebox.core.model.Proxy
import com.github.yumeyucca.yumebox.data.model.ProxySortMode
import com.github.yumeyucca.yumebox.domain.model.ProxyGroupInfo
import com.github.yumeyucca.yumebox.domain.model.isSelectable
import com.github.yumeyucca.yumebox.presentation.component.PaneWidths
import com.github.yumeyucca.yumebox.presentation.component.ScreenLazyColumn
import com.github.yumeyucca.yumebox.presentation.screen.node.GroupDelayRefreshIndicator
import com.github.yumeyucca.yumebox.presentation.screen.node.NodeCard
import com.github.yumeyucca.yumebox.presentation.screen.node.NodeSearchToolbar
import com.github.yumeyucca.yumebox.presentation.screen.node.nodeGridItems
import com.github.yumeyucca.yumebox.presentation.theme.LocalSpacing
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.presentation.util.KeepLazyListTopAnchorOnReorder
import com.github.yumeyucca.yumebox.presentation.viewmodel.ProxyDelayTestProgress
import kotlinx.coroutines.flow.StateFlow
import top.yukonga.miuix.kmp.basic.ScrollBehavior

private fun ProxyGroupInfo.filterNodes(query: String): List<Proxy> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return proxies
    return proxies.filter { proxy ->
        proxy.name.contains(normalizedQuery, ignoreCase = true) ||
            proxy.title.contains(normalizedQuery, ignoreCase = true) ||
            proxy.subtitle.contains(normalizedQuery, ignoreCase = true)
    }
}

@Composable
internal fun NodeListPage(
    group: ProxyGroupInfo?,
    sortMode: ProxySortMode,
    testingGroupNames: StateFlow<Set<String>>,
    testingProxyNames: Set<String>,
    delayTestProgress: StateFlow<ProxyDelayTestProgress?>,
    mainInnerPadding: PaddingValues,
    outerInnerPadding: PaddingValues,
    scrollBehavior: ScrollBehavior,
    listState: LazyListState,
    onSelectProxy: (groupName: String, proxyName: String) -> Unit,
    onTestDelay: () -> Unit,
    onTestProxyDelay: (String) -> Unit,
    onScrollDirectionChanged: (Boolean) -> Unit,
    useAdaptiveGrid: Boolean = false,
    gridState: LazyGridState? = null,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    showSortPopup: Boolean = false,
    onShowMorePopupChange: (Boolean) -> Unit = {},
    onSortSelected: (ProxySortMode) -> Unit = {},
    onLocateCurrentProxy: (() -> Unit)? = null,
) {
    if (group == null) return
    val spacing = LocalSpacing.current
    val visibleProxies = remember(group.proxies, searchQuery) { group.filterNodes(searchQuery) }
    val listItemKeys = remember(group.proxies) { group.proxies.map { it.name } }

    GroupDelayTestListAnchor(
        listState = listState,
        itemKeys = listItemKeys,
        sortByLatency = sortMode == ProxySortMode.BY_LATENCY && !useAdaptiveGrid,
        groupName = group.name,
        testingGroupNames = testingGroupNames,
    )

    val contentPadding =
        PaddingValues(
            start = UiDp.dp12,
            end = UiDp.dp12,
            top = outerInnerPadding.calculateTopPadding() + UiDp.dp20,
            bottom = mainInnerPadding.calculateBottomPadding() + spacing.space12,
        )

    if (useAdaptiveGrid) {
        val resolvedGridState = gridState ?: rememberLazyGridState()
        val latestScrollDirectionCallback by rememberUpdatedState(onScrollDirectionChanged)
        var lastHiddenState by remember(resolvedGridState) { mutableStateOf(false) }
        val fabScrollObserver =
            remember(resolvedGridState) {
                object : NestedScrollConnection {
                    override fun onPreScroll(
                        available: Offset,
                        source: NestedScrollSource,
                    ): Offset {
                        val hiddenState =
                            when {
                                available.y < -1f -> true
                                available.y > 1f -> false
                                else -> return Offset.Zero
                            }
                        if (hiddenState != lastHiddenState) {
                            lastHiddenState = hiddenState
                            latestScrollDirectionCallback(hiddenState)
                        }
                        return Offset.Zero
                    }

                    override suspend fun onPostFling(
                        consumed: Velocity,
                        available: Velocity,
                    ): Velocity {
                        if (consumed.y > 1f || available.y > 1f) {
                            latestScrollDirectionCallback(false)
                            lastHiddenState = false
                        }
                        return Velocity.Zero
                    }
                }
            }
        LaunchedEffect(resolvedGridState) {
            latestScrollDirectionCallback(false)
            lastHiddenState = false
        }
        Box(
            Modifier
                .fillMaxSize()
                .nestedScroll(fabScrollObserver),
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = PaneWidths.NodeGridAdaptiveMin),
                state = resolvedGridState,
                contentPadding = contentPadding,
                horizontalArrangement = Arrangement.spacedBy(UiDp.dp12),
                verticalArrangement = Arrangement.spacedBy(UiDp.dp6),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(key = "__node_search__", span = { GridItemSpan(maxLineSpan) }) {
                    NodeSearchToolbar(
                        query = searchQuery,
                        onQueryChange = onSearchQueryChange,
                        sortMode = sortMode,
                        showMorePopup = showSortPopup,
                        onShowMorePopupChange = onShowMorePopupChange,
                        onSortSelected = onSortSelected,
                        onTestDelay = onTestDelay,
                        onLocateCurrentProxy = onLocateCurrentProxy,
                    )
                }
                item(key = "__refresh_indicator__", span = { GridItemSpan(maxLineSpan) }) {
                    GroupDelayRefreshIndicator(
                        groupName = group.name,
                        testingGroupNames = testingGroupNames,
                        progress = delayTestProgress,
                    )
                }
                items(items = visibleProxies, key = { it.name }) { proxy ->
                    NodeCard(
                        proxy = proxy,
                        isSelected = proxy.name == group.now,
                        onClick = { proxyName ->
                            if (group.isSelectable) {
                                onSelectProxy(group.name, proxyName)
                            } else {
                                onTestDelay()
                            }
                        },
                        onTestClick = onTestProxyDelay,
                        isDelayTesting = testingProxyNames.contains(proxy.name),
                        showCountryFlag = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        return
    }

    ScreenLazyColumn(
        lazyListState = listState,
        scrollBehavior = scrollBehavior,
        innerPadding = outerInnerPadding,
        enableGlobalScroll = true,
        onScrollDirectionChanged = onScrollDirectionChanged,
        contentPadding = contentPadding,
    ) {
        item(key = "__node_search__") {
            NodeSearchToolbar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                sortMode = sortMode,
                showMorePopup = showSortPopup,
                onShowMorePopupChange = onShowMorePopupChange,
                onSortSelected = onSortSelected,
                onTestDelay = onTestDelay,
                onLocateCurrentProxy = onLocateCurrentProxy,
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
            onProxyClick = { proxyName ->
                if (group.isSelectable) {
                    onSelectProxy(group.name, proxyName)
                } else {
                    onTestDelay()
                }
            },
            onProxyTest = onTestProxyDelay,
            testingProxyNames = testingProxyNames,
            outerHorizontalPadding = UiDp.dp0,
            itemVerticalPadding = UiDp.dp6,
        )
    }
}

@Composable
private fun GroupDelayTestListAnchor(
    listState: LazyListState,
    itemKeys: List<String>,
    sortByLatency: Boolean,
    groupName: String,
    testingGroupNames: StateFlow<Set<String>>,
) {
    val testing by testingGroupNames.collectAsState()
    KeepLazyListTopAnchorOnReorder(
        listState = listState,
        itemKeys = itemKeys,
        enabled = sortByLatency && groupName !in testing,
        scrollToTopOnEnabled = true,
    )
}
