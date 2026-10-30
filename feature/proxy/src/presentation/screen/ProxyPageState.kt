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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.unit.Dp
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import com.github.yumeyucca.yumebox.core.model.isProxyGroup
import com.github.yumeyucca.yumebox.domain.model.ProxyGroupInfo
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.presentation.util.isAtTop
import com.github.yumeyucca.yumebox.presentation.util.rememberSavedGridState
import com.github.yumeyucca.yumebox.presentation.util.rememberSavedListState
import com.github.yumeyucca.yumebox.presentation.viewmodel.ProxyViewModel
import com.github.yumeyucca.yumebox.runtime.client.ProxyGroupSyncSource

/**
 * 代理页的全部非渲染状态。
 *
 * 分组分区、tab 选择、节点搜索开关、列表滚动位置、分组 tab 分页状态和选中项的派生值都收在
 * 这里,页面只读结果并渲染,不再自己摊开二十个 local。
 *
 * 实例每次重组都会重建:会随数据变化的值都在工厂里重新算(`remember` 的记忆化语义保持不变),
 * 需要跨重组存活的那几个 `rememberSaveable` 值由工厂创建后以 [MutableState] 传入,
 * 所以换实例不会丢状态。
 */
@Stable
internal class ProxyPageState(
    private val proxyListTabIndexState: MutableState<Int>,
    private val twoColumnNodesState: MutableState<Boolean>,
    private val groupTabsModeState: MutableState<Boolean>,
    private val nodeSearchQueryState: MutableState<String>,
    private val proxyViewModel: ProxyViewModel,
    private val selectionController: SelectedGroupController,
    private val nodeSearchVisible: Boolean,
    val proxyListTabs: List<String>,
    val displayedProxyGroups: List<ProxyGroupInfo>,
    val visibleProxyGroups: List<ProxyGroupInfo>,
    val selectedGroupName: String?,
    val selectedNodeGroup: ProxyGroupInfo?,
    val tabbedPagerState: PagerState?,
    val groupListState: LazyListState,
    val nodeListState: LazyListState,
    val nodeGridState: LazyGridState,
    val groupTabListStates: SnapshotStateMap<String, LazyListState>,
    val groupTabGridStates: SnapshotStateMap<String, LazyGridState>,
    val tabCollapseState: ProxyTabScrollState,
    val categoryTabInset: Dp,
) {
    /** 策略页当前停在哪个 tab;分组 tab 模式下仍保留切换前的选择。 */
    val proxyListTabIndex: Int
        get() = proxyListTabIndexState.value

    var twoColumnNodes: Boolean
        get() = twoColumnNodesState.value
        set(value) {
            twoColumnNodesState.value = value
        }

    var groupTabsMode: Boolean
        get() = groupTabsModeState.value
        set(value) {
            groupTabsModeState.value = value
        }

    var nodeSearchQuery: String
        get() = nodeSearchQueryState.value
        set(value) {
            nodeSearchQueryState.value = value
        }

    /** 节点列表页里点 tab(含重按当前 tab)= 切换过滤并回到分组列表。 */
    fun selectProxyListTab(index: Int) {
        proxyListTabIndexState.value = index
        if (!groupTabsMode && selectedGroupName != null) {
            selectionController.clear()
        }
    }

    /** "更多"弹窗里的搜索开关;关闭时清空查询,避免隐藏的过滤条件继续生效。 */
    fun toggleNodeSearch() {
        val next = !nodeSearchVisible
        proxyViewModel.setNodeSearchVisible(next)
        if (!next) {
            nodeSearchQuery = ""
        }
    }

    /** 顶栏测速只测当前分组;没有选中分组时无动作。 */
    fun testSelectedGroupDelay() {
        selectedGroupName?.let(proxyViewModel::testDelay)
    }
}

/**
 * [ProxyPageState] 的唯一构造入口。
 *
 * 它读页面自己的 `remember` / `rememberSaveable`,因此返回值只在本次组合内有效;
 * 渲染侧对它的读取都发生在 `ProxyPageShell` 里,状态一变页面(以及这里)就重新执行。
 */
@Composable
internal fun rememberProxyPageState(
    pane: ProxyPageMode,
    isActive: Boolean,
    proxyViewModel: ProxyViewModel,
    proxyGroups: List<ProxyGroupInfo>,
    nodeSearchVisible: Boolean,
    selectionController: SelectedGroupController,
): ProxyPageState {
    // zashboard 式拆分:策略组 = 成员里含其他分组;节点组 = 成员全是节点(典型如地域组)。
    // 两类同时存在时才出现 tab 切换,默认停留在策略;否则维持原有的单一分组列表。
    val (strategyProxyGroups, nodeOnlyProxyGroups) =
        remember(proxyGroups) {
            proxyGroups
                .filter { group -> group.proxies.isNotEmpty() }
                .partition { group -> group.proxies.any { it.isProxyGroup } }
        }
    val proxyListTabs =
        remember(strategyProxyGroups, nodeOnlyProxyGroups) {
            if (strategyProxyGroups.isNotEmpty() && nodeOnlyProxyGroups.isNotEmpty()) {
                listOf(YumeTxt.Proxy.Tab.Policy, YumeTxt.Proxy.Tab.NodeGroups)
            } else {
                emptyList()
            }
        }
    val proxyListTabIndexState = rememberSaveable { mutableStateOf(0) }
    val twoColumnNodesState =
        rememberSaveable(pane) {
            mutableStateOf(pane != ProxyPageMode.Compact)
        }
    // Group-tab mode replaces the category/group drill-down with one swipeable group strip.
    val groupTabsModeState = rememberSaveable(pane) { mutableStateOf(false) }
    val groupTabsMode = groupTabsModeState.value
    val proxyListTabIndex = proxyListTabIndexState.value
    val displayedProxyGroups =
        when {
            proxyListTabs.isEmpty() -> proxyGroups
            proxyListTabIndex == 0 -> strategyProxyGroups
            else -> nodeOnlyProxyGroups
        }
    val groupSelection =
        rememberProxyGroupSelectionState(
            proxyGroups = proxyGroups,
            onRefreshGroup = proxyViewModel::refreshGroup,
            retainLastKnownGroup = true,
            controller = selectionController,
        )
    val selectedGroupName = groupSelection.selectedGroupName
    val selectedNodeGroup =
        groupSelection.selectedGroup
            ?: groupSelection.displayGroup
            ?: if (pane == ProxyPageMode.SplitDetail) proxyGroups.firstOrNull() else null
    val visibleProxyGroups = if (groupTabsMode) proxyGroups else displayedProxyGroups
    val tabbedPagerState =
        if (groupTabsMode && visibleProxyGroups.isNotEmpty()) {
            key(proxyListTabIndex) {
                rememberProxyGroupPagerState(
                    groups = visibleProxyGroups,
                    selectedGroupName = selectedGroupName,
                    onGroupSelected = selectionController::selectGroup,
                )
            }
        } else {
            null
        }
    val nodeSearchQueryState = rememberSaveable(selectedGroupName) { mutableStateOf("") }

    LaunchedEffect(pane, proxyGroups, selectedGroupName) {
        if (pane == ProxyPageMode.Compact || proxyGroups.isEmpty()) return@LaunchedEffect
        if (selectedGroupName == null || proxyGroups.none { it.name == selectedGroupName }) {
            selectionController.selectGroup(proxyGroups.first())
        }
    }

    val groupListState = rememberSavedListState()
    val nodeListState = rememberSavedListState(selectedGroupName)
    val nodeGridState = rememberSavedGridState(selectedGroupName)
    val groupTabListStates = remember { mutableStateMapOf<String, LazyListState>() }
    val groupTabGridStates = remember { mutableStateMapOf<String, LazyGridState>() }
    val activeGroupTabName = tabbedPagerState?.currentPage?.let(visibleProxyGroups::getOrNull)?.name
    val activeGroupTabListState = activeGroupTabName?.let(groupTabListStates::get)
    val activeGroupTabGridState = activeGroupTabName?.let(groupTabGridStates::get)
    val tabCollapseState = rememberProxyTabScrollState()
    LaunchedEffect(isActive, pane) {
        if (pane == ProxyPageMode.Compact && !isActive) {
            nodeSearchQueryState.value = ""
        }
    }

    // One step back at a time: tabs mode collapses to the group list first, then the open group
    // closes. Leaving the page is the root stack's job, not this handler's.
    BackHandler(
        enabled = pane == ProxyPageMode.Compact && isActive && (groupTabsMode || selectedGroupName != null)
    ) {
        if (groupTabsMode) groupTabsModeState.value = false else selectionController.clear()
    }

    val coreSource =
        if (pane == ProxyPageMode.SplitDetail) ProxyGroupSyncSource.Detail else ProxyGroupSyncSource.Page
    LaunchedEffect(proxyViewModel, isActive, coreSource) {
        proxyViewModel.ensureCoreLoaded(isActive, source = coreSource)
    }
    DisposableEffect(proxyViewModel, coreSource) {
        onDispose { proxyViewModel.ensureCoreLoaded(false, source = coreSource) }
    }

    val categoryTabInset = if (proxyListTabs.isEmpty()) UiDp.dp0 else ProxyTabBandInset
    val activeListState =
        when {
            groupTabsMode -> activeGroupTabListState
            selectedGroupName != null -> nodeListState
            else -> groupListState
        }
    val activeGridState =
        when {
            groupTabsMode -> activeGroupTabGridState
            selectedGroupName != null -> nodeGridState
            else -> null
        }
    val activeIsAtTop by
        remember(activeListState, activeGridState, twoColumnNodesState.value) {
            derivedStateOf {
                if (twoColumnNodesState.value) {
                    activeGridState?.isAtTop ?: true
                } else {
                    activeListState?.isAtTop ?: true
                }
            }
        }
    LaunchedEffect(activeIsAtTop) {
        tabCollapseState.syncContent(activeIsAtTop)
    }

    return ProxyPageState(
        proxyListTabIndexState = proxyListTabIndexState,
        twoColumnNodesState = twoColumnNodesState,
        groupTabsModeState = groupTabsModeState,
        nodeSearchQueryState = nodeSearchQueryState,
        proxyViewModel = proxyViewModel,
        selectionController = selectionController,
        nodeSearchVisible = nodeSearchVisible,
        proxyListTabs = proxyListTabs,
        displayedProxyGroups = displayedProxyGroups,
        visibleProxyGroups = visibleProxyGroups,
        selectedGroupName = selectedGroupName,
        selectedNodeGroup = selectedNodeGroup,
        tabbedPagerState = tabbedPagerState,
        groupListState = groupListState,
        nodeListState = nodeListState,
        nodeGridState = nodeGridState,
        groupTabListStates = groupTabListStates,
        groupTabGridStates = groupTabGridStates,
        tabCollapseState = tabCollapseState,
        categoryTabInset = categoryTabInset,
    )
}
