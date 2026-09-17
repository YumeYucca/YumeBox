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

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.Velocity
import com.github.yumeyucca.yumebox.core.model.Proxy
import com.github.yumeyucca.yumebox.data.model.ProxySortMode
import com.github.yumeyucca.yumebox.domain.model.ProxyGroupInfo
import com.github.yumeyucca.yumebox.domain.model.isSelectable
import com.github.yumeyucca.yumebox.presentation.component.PaneWidths
import com.github.yumeyucca.yumebox.presentation.component.ScreenLazyColumn
import com.github.yumeyucca.yumebox.presentation.screen.node.NodeCard
import com.github.yumeyucca.yumebox.presentation.screen.node.nodeGridItems
import com.github.yumeyucca.yumebox.presentation.theme.LocalSpacing
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.presentation.util.KeepLazyListTopAnchorOnReorder
import com.github.yumeyucca.yumebox.presentation.util.ProxyDelayPullToRefresh
import com.github.yumeyucca.yumebox.presentation.util.rememberCurrentGroupPullToRefreshTexts
import tf.gal.yumebox.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Search
import top.yukonga.miuix.kmp.icon.basic.SearchCleanup

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
    testingGroupNames: Set<String>,
    testingProxyNames: Set<String>,
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
    showSearch: Boolean = false,
    onSearchQueryChange: (String) -> Unit = {},
) {
    if (group == null) return
    val spacing = LocalSpacing.current
    val isGroupTesting = testingGroupNames.contains(group.name)
    val isInteractionLocked = isGroupTesting || testingProxyNames.isNotEmpty()
    val visibleProxies = remember(group.proxies, searchQuery) { group.filterNodes(searchQuery) }
    val listItemKeys = remember(group.proxies) { group.proxies.map { it.name } }

    KeepLazyListTopAnchorOnReorder(
        listState = listState,
        itemKeys = listItemKeys,
        enabled = sortMode == ProxySortMode.BY_LATENCY && !useAdaptiveGrid,
        scrollToTopOnEnabled = true,
    )

    val contentPadding =
        PaddingValues(
            start = UiDp.dp12,
            end = UiDp.dp12,
            top = UiDp.dp20,
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
        Column(modifier = Modifier.fillMaxSize()) {
            NodeSearchField(
                query = searchQuery,
                visible = showSearch,
                onQueryChange = onSearchQueryChange,
                modifier = Modifier.padding(top = outerInnerPadding.calculateTopPadding()),
            )
            ProxyDelayPullToRefresh(
                isRefreshing = isGroupTesting,
                onRefresh = onTestDelay,
                refreshTexts = rememberCurrentGroupPullToRefreshTexts(),
                modifier = Modifier.fillMaxWidth().weight(1f),
                scrollBehavior = scrollBehavior,
            ) {
                Box(Modifier.fillMaxSize().nestedScroll(fabScrollObserver)) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = PaneWidths.NodeGridAdaptiveMin),
                        state = resolvedGridState,
                        userScrollEnabled = !isInteractionLocked,
                        contentPadding = contentPadding,
                        horizontalArrangement = Arrangement.spacedBy(UiDp.dp12),
                        verticalArrangement = Arrangement.spacedBy(UiDp.dp6),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(items = visibleProxies, key = { it.name }) { proxy ->
                            NodeCard(
                                proxy = proxy,
                                isSelected = proxy.name == group.now,
                                onClick =
                                    if (isInteractionLocked) {
                                        null
                                    } else {
                                        { proxyName ->
                                            if (group.isSelectable) {
                                                onSelectProxy(group.name, proxyName)
                                            } else {
                                                onTestDelay()
                                            }
                                        }
                                    },
                                onTestClick = onTestProxyDelay.takeUnless { isInteractionLocked },
                                isDelayTesting = testingProxyNames.contains(proxy.name),
                                showCountryFlag = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NodeSearchField(
            query = searchQuery,
            visible = showSearch,
            onQueryChange = onSearchQueryChange,
            modifier = Modifier.padding(top = outerInnerPadding.calculateTopPadding()),
        )
        ProxyDelayPullToRefresh(
            isRefreshing = isGroupTesting,
            onRefresh = onTestDelay,
            refreshTexts = rememberCurrentGroupPullToRefreshTexts(),
            modifier = Modifier.fillMaxWidth().weight(1f),
            scrollBehavior = scrollBehavior,
        ) {
            ScreenLazyColumn(
                lazyListState = listState,
                scrollBehavior = scrollBehavior,
                innerPadding = outerInnerPadding,
                enableGlobalScroll = true,
                userScrollEnabled = !isInteractionLocked,
                onScrollDirectionChanged = onScrollDirectionChanged,
                contentPadding = contentPadding,
            ) {
                nodeGridItems(
                    proxies = visibleProxies,
                    selectedProxyName = group.now,
                    onProxyClick =
                        if (isInteractionLocked) {
                            null
                        } else {
                            { proxyName ->
                                if (group.isSelectable) {
                                    onSelectProxy(group.name, proxyName)
                                } else {
                                    onTestDelay()
                                }
                            }
                        },
                    onProxyTest = onTestProxyDelay.takeUnless { isInteractionLocked },
                    testingProxyNames = testingProxyNames,
                    outerHorizontalPadding = UiDp.dp0,
                    itemVerticalPadding = UiDp.dp6,
                )
            }
        }
    }
}

@Composable
private fun NodeSearchField(
    query: String,
    visible: Boolean,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    Column(modifier = modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = visible,
            enter =
                expandVertically(
                    animationSpec = tween(durationMillis = 220),
                    expandFrom = Alignment.Top,
                ) + fadeIn(animationSpec = tween(durationMillis = 160)),
            exit =
                shrinkVertically(
                    animationSpec = tween(durationMillis = 180),
                    shrinkTowards = Alignment.Top,
                ) + fadeOut(animationSpec = tween(durationMillis = 120)),
        ) {
            InputField(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = {},
                expanded = false,
                onExpandedChange = {},
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
                        .fillMaxWidth()
                        .padding(horizontal = UiDp.dp12)
                        .padding(top = UiDp.dp4, bottom = UiDp.dp8),
                )
        }
    }
    LaunchedEffect(visible) {
        if (visible) {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }
}
