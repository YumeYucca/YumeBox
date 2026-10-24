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

@file:Suppress("DuplicatedCode", "FunctionName", "UnnecessaryVariable")

package com.github.yumeyucca.yumebox.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.github.yumeyucca.yumebox.data.model.ProxySortMode
import com.github.yumeyucca.yumebox.core.model.isProxyGroup
import com.github.yumeyucca.yumebox.domain.model.ProxyGroupInfo
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.House
import com.github.yumeyucca.yumebox.presentation.icon.yume.ListChevronsUpDown
import com.github.yumeyucca.yumebox.presentation.icon.yume.Settings2
import com.github.yumeyucca.yumebox.presentation.icon.yume.Speed
import com.github.yumeyucca.yumebox.presentation.screen.node.NodeSortPopup
import com.github.yumeyucca.yumebox.presentation.screen.node.ProvideProxyDelayFlows
import com.github.yumeyucca.yumebox.presentation.screen.node.nodeGroupItems
import com.github.yumeyucca.yumebox.presentation.theme.*
import com.github.yumeyucca.yumebox.presentation.viewmodel.ProxyViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

private data class ProxyScreenVmState(
    val proxyGroups: List<ProxyGroupInfo>,
    val testingProxyNames: Set<String>,
    val sortMode: ProxySortMode,
    val uiSelectedGroupName: String?,
    val nodeSearchVisible: Boolean,
)

@Composable
private fun rememberProxyScreenVmState(proxyViewModel: ProxyViewModel): ProxyScreenVmState {
    val proxyGroups by proxyViewModel.sortedProxyGroups.collectAsState()
    val testingProxyNames by proxyViewModel.testingProxyNames.collectAsState()
    val sortMode by proxyViewModel.sortMode.collectAsState()
    val uiSelectedGroupName by proxyViewModel.uiSelectedGroupName.collectAsState()
    val nodeSearchVisible by proxyViewModel.nodeSearchVisible.collectAsState()
    return remember(
        proxyGroups,
        testingProxyNames,
        sortMode,
        uiSelectedGroupName,
        nodeSearchVisible,
    ) {
        ProxyScreenVmState(
            proxyGroups = proxyGroups,
            testingProxyNames = testingProxyNames,
            sortMode = sortMode,
            uiSelectedGroupName = uiSelectedGroupName,
            nodeSearchVisible = nodeSearchVisible,
        )
    }
}

@Composable
fun ProxyPager(
    mainInnerPadding: PaddingValues,
    onNavigateToProviders: (() -> Unit)?,
    isActive: Boolean,
    onOpenSettings: (() -> Unit)? = null,
    @Suppress("UNUSED_PARAMETER") windowLayoutMode: WindowLayoutMode = WindowLayoutMode.Compact,
) {
    val proxyViewModel = koinViewModel<ProxyViewModel>()
    val screen = rememberProxyScreenVmState(proxyViewModel)
    val proxyGroups = screen.proxyGroups
    val testingProxyNames = screen.testingProxyNames
    val sortMode = screen.sortMode
    val uiSelectedGroupName = screen.uiSelectedGroupName
    val nodeSearchVisible = screen.nodeSearchVisible
    val groupScrollBehavior = MiuixScrollBehavior(snapAnimationSpec = null)

    var showSortPopup by rememberSaveable { mutableStateOf(false) }
    val inSplitShell = LocalDetailNavigator.current.isSplitShell
    // zashboard 式拆分:策略组 = 成员里含其他分组;节点组 = 成员全是节点(典型如地域组)。
    // 两类同时存在时才出现 tab 切换,默认停留在策略;否则维持原有的单一分组列表。
    val (strategyProxyGroups, nodeOnlyProxyGroups) =
        remember(proxyGroups) {
            val strategy = proxyGroups.filter { group -> group.proxies.any { it.isProxyGroup } }
            val nodeOnly =
                proxyGroups.filter { group ->
                    group.proxies.isNotEmpty() && group.proxies.none { it.isProxyGroup }
                }
            strategy to nodeOnly
        }
    val proxyListTabs =
        remember(strategyProxyGroups, nodeOnlyProxyGroups) {
            if (strategyProxyGroups.isNotEmpty() && nodeOnlyProxyGroups.isNotEmpty()) {
                listOf(YumeTxt.Proxy.Tab.Policy, YumeTxt.Proxy.Tab.NodeGroups)
            } else {
                emptyList()
            }
        }
    var proxyListTabIndex by rememberSaveable { mutableStateOf(0) }
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
            controlledSelectedGroupName = if (inSplitShell) uiSelectedGroupName else null,
            onControlledSelectedGroupNameChange =
                if (inSplitShell) proxyViewModel::selectUiGroup else null,
    )
    val selectedGroupName = groupSelection.selectedGroupName
    val displayGroup = groupSelection.displayGroup
    val selectedNodeGroup = groupSelection.selectedGroup ?: displayGroup
    // 节点列表页里点 tab(含重按当前 tab)= 切换过滤并回到分组列表。
    val selectProxyListTab: (Int) -> Unit = { index ->
        proxyListTabIndex = index
        if (selectedGroupName != null) {
            groupSelection.clearSelection()
        }
    }
    var nodeSearchQuery by rememberSaveable(selectedGroupName) { mutableStateOf("") }
    // "更多"弹窗里的搜索开关;关闭时清空查询,避免隐藏的过滤条件继续生效。
    val toggleNodeSearch = {
        val next = !nodeSearchVisible
        proxyViewModel.setNodeSearchVisible(next)
        if (!next) {
            nodeSearchQuery = ""
        }
    }

    LaunchedEffect(inSplitShell, proxyGroups, selectedGroupName) {
        if (!inSplitShell || proxyGroups.isEmpty()) return@LaunchedEffect
        if (selectedGroupName == null || proxyGroups.none { it.name == selectedGroupName }) {
            groupSelection.selectGroup(proxyGroups.first())
        }
    }
    val coroutineScope = rememberCoroutineScope()
    val groupListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val nodeListState =
        rememberSaveable(selectedGroupName, saver = LazyListState.Saver) { LazyListState() }
    LaunchedEffect(isActive, inSplitShell) {
        if (!isActive && !inSplitShell) {
            nodeSearchQuery = ""
        }
    }

    val requestSelectedGroupDelayTest =
        remember(selectedGroupName, proxyViewModel) {
            {
                val groupName = selectedGroupName ?: return@remember
                proxyViewModel.testDelay(groupName)
            }
        }
    // 顶栏测速按钮:统一测全部节点(分组页/节点页不再区分)。
    val testAllNodes = remember(proxyViewModel) { { proxyViewModel.testDelay() } }
    val locateCurrentProxy =
        remember(coroutineScope, displayGroup, nodeListState, selectedGroupName) {
            if (selectedGroupName == null) {
                null
            } else {
                displayGroup
                    ?.takeIf { group -> group.name == selectedGroupName }
                    ?.let { group ->
                        fun() {
                            val proxyIndex =
                                group.proxies.indexOfFirst { proxy -> proxy.name == group.now }
                            if (proxyIndex < 0) return
                            coroutineScope.launch {
                                nodeListState.animateLocateToItem(proxyIndex + 1)
                            }
                        }
                    }
            }
        }

    BackHandler(enabled = isActive && !inSplitShell && selectedGroupName != null) {
        groupSelection.clearSelection()
    }

    LaunchedEffect(isActive) { proxyViewModel.ensureCoreLoaded(isActive, source = "proxy_page") }

    DisposableEffect(proxyViewModel) {
        onDispose { proxyViewModel.ensureCoreLoaded(false, source = "proxy_page") }
    }

    ProvideProxyDelayFlows(proxyViewModel) {
        GlassScaffold(
            topBar = {
                ProxyTopBar(
                    title = YumeTxt.Proxy.Title,
                    scrollBehavior = groupScrollBehavior,
                    showBack = false,
                    onBack = {},
                    onNavigateToProviders = onNavigateToProviders,
                    onOpenSettings = onOpenSettings,
                    onTestAllNodes = testAllNodes,
                    onLocateCurrentProxy = locateCurrentProxy,
                    showSortPopup = showSortPopup,
                    onShowSortPopupChange = { showSortPopup = it },
                    sortMode = sortMode,
                    onSortSelected = proxyViewModel::setSortMode,
                    searchVisible = nodeSearchVisible,
                    onToggleSearch = if (selectedGroupName != null) toggleNodeSearch else null,
                )
            }
        ) { scaffoldPadding ->
            Box(
                modifier =
                    Modifier.fillMaxSize()
            ) {
                if (inSplitShell) {
                    if (proxyGroups.isNotEmpty()) {
                    ProxyContent(
                        proxyGroups = displayedProxyGroups,
                        scrollBehavior = groupScrollBehavior,
                        innerPadding = scaffoldPadding,
                        mainInnerPadding = mainInnerPadding,
                        testingGroupNames = proxyViewModel.testingGroupNames,
                        onGroupClick = groupSelection.selectGroup,
                        onGroupTest = { group -> proxyViewModel.testDelay(group.name) },
                        listState = groupListState,
                        tabs = proxyListTabs,
                        selectedTabIndex = proxyListTabIndex,
                        onTabSelected = { proxyListTabIndex = it },
                    )
                    }
                } else {
                    AnimatedContent(
                    targetState = selectedGroupName,
                    transitionSpec = {
                        if (targetState != null) {
                            (slideInHorizontally(
                                animationSpec =
                                    tween(durationMillis = 380, easing = AnimationSpecs.Legacy),
                                initialOffsetX = { it },
                            ) +
                                fadeIn(
                                    animationSpec =
                                        tween(
                                            durationMillis = 180,
                                            easing = AnimationSpecs.Legacy,
                                        )
                                )) togetherWith
                                (slideOutHorizontally(
                                    animationSpec =
                                        tween(durationMillis = 340, easing = AnimationSpecs.Legacy),
                                    targetOffsetX = { -it / 3 },
                                ) +
                                    fadeOut(
                                        animationSpec =
                                            tween(
                                                durationMillis = 160,
                                                easing = AnimationSpecs.Legacy,
                                            )
                                    ))
                        } else {
                            (slideInHorizontally(
                                animationSpec =
                                    tween(durationMillis = 340, easing = AnimationSpecs.Legacy),
                                initialOffsetX = { -it / 3 },
                            ) +
                                fadeIn(
                                    animationSpec =
                                        tween(
                                            durationMillis = 180,
                                            easing = AnimationSpecs.Legacy,
                                        )
                                )) togetherWith
                                (slideOutHorizontally(
                                    animationSpec =
                                        tween(durationMillis = 380, easing = AnimationSpecs.Legacy),
                                    targetOffsetX = { it },
                                ) +
                                    fadeOut(
                                        animationSpec =
                                            tween(
                                                durationMillis = 160,
                                                easing = AnimationSpecs.Legacy,
                                            )
                                    ))
                        }
                    },
                    label = "proxy_content_slide",
                ) { targetGroupName ->
                    if (targetGroupName == null) {
                        if (proxyGroups.isNotEmpty()) {
                            ProxyContent(
                                proxyGroups = displayedProxyGroups,
                                scrollBehavior = groupScrollBehavior,
                                innerPadding = scaffoldPadding,
                                mainInnerPadding = mainInnerPadding,
                                testingGroupNames = proxyViewModel.testingGroupNames,
                                onGroupClick = groupSelection.selectGroup,
                                onGroupTest = { group -> proxyViewModel.testDelay(group.name) },
                                listState = groupListState,
                                tabs = proxyListTabs,
                                selectedTabIndex = proxyListTabIndex,
                                onTabSelected = { proxyListTabIndex = it },
                            )
                        }
                    } else {
                        NodeListPage(
                            group = selectedNodeGroup,
                            sortMode = sortMode,
                            testingGroupNames = proxyViewModel.testingGroupNames,
                            testingProxyNames = testingProxyNames,
                            delayTestProgress = proxyViewModel.delayTestProgress,
                            mainInnerPadding = mainInnerPadding,
                            outerInnerPadding = scaffoldPadding,
                            scrollBehavior = groupScrollBehavior,
                            listState = nodeListState,
                            onSelectProxy = { groupName, proxyName ->
                                proxyViewModel.selectProxy(groupName, proxyName)
                            },
                            onTestDelay = requestSelectedGroupDelayTest,
                            onTestProxyDelay = proxyViewModel::testProxyDelay,
                            onScrollDirectionChanged = {},
                            useAdaptiveGrid = false,
                            searchQuery = nodeSearchQuery,
                            onSearchQueryChange = { nodeSearchQuery = it },
                            searchVisible = nodeSearchVisible,
                            tabs = proxyListTabs,
                            selectedTabIndex = proxyListTabIndex,
                            onTabSelected = selectProxyListTab,
                        )
                    }
                }
            }
        }
    }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProxyContent(
    proxyGroups: List<ProxyGroupInfo>,
    scrollBehavior: ScrollBehavior,
    innerPadding: PaddingValues,
    mainInnerPadding: PaddingValues,
    onGroupClick: (ProxyGroupInfo) -> Unit,
    onGroupTest: (ProxyGroupInfo) -> Unit,
    testingGroupNames: StateFlow<Set<String>>,
    listState: LazyListState,
    tabs: List<String> = emptyList(),
    selectedTabIndex: Int = 0,
    onTabSelected: (Int) -> Unit = {},
) {
    val testingNames by testingGroupNames.collectAsState()
    val spacing = LocalSpacing.current
    val revealCount = rememberRowReveal(itemCount = proxyGroups.size, listState = listState)
    ScreenLazyColumn(
        lazyListState = listState,
        scrollBehavior = scrollBehavior,
        innerPadding = innerPadding,
        enableGlobalScroll = true,
        contentPadding =
            PaddingValues(
                start = UiDp.dp12,
                end = UiDp.dp12,
                top = innerPadding.calculateTopPadding() + UiDp.dp14,
                bottom = mainInnerPadding.calculateBottomPadding() + spacing.space12,
            ),
    ) {
        if (tabs.isNotEmpty()) {
            // 策略/节点组切换(zashboard 逻辑):吸附在内容区顶部,底色与页面一致避免内容透出。
            stickyHeader(key = "__proxy_list_tabs__") {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(MiuixTheme.colorScheme.surface)
                            .padding(vertical = UiDp.dp4),
                ) {
                    SegmentedTabRow(
                        tabs = tabs,
                        selectedTabIndex = selectedTabIndex,
                        onTabSelected = onTabSelected,
                    )
                }
            }
        }
        nodeGroupItems(
            groups = proxyGroups,
            onGroupClick = onGroupClick,
            onGroupTest = onGroupTest,
            testingGroupNames = testingNames,
            itemVerticalPadding = UiDp.dp6,
            revealCount = revealCount,
        )
    }
}

@Composable
internal fun ProxyShellNodeDetailContent(
    mainInnerPadding: PaddingValues,
    onNavigateToProviders: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
) {
    val proxyViewModel = koinViewModel<ProxyViewModel>()
    val screen = rememberProxyScreenVmState(proxyViewModel)
    val proxyGroups = screen.proxyGroups
    val testingProxyNames = screen.testingProxyNames
    val sortMode = screen.sortMode
    val uiSelectedGroupName = screen.uiSelectedGroupName
    val scrollBehavior = MiuixScrollBehavior(snapAnimationSpec = null)
    val coroutineScope = rememberCoroutineScope()
    // 顶栏测速按钮:统一测全部节点(分组页/节点页不再区分)。
    val testAllNodes = remember(proxyViewModel) { { proxyViewModel.testDelay() } }
    val groupSelection =
        rememberProxyGroupSelectionState(
            proxyGroups = proxyGroups,
            onRefreshGroup = proxyViewModel::refreshGroup,
            retainLastKnownGroup = true,
            controlledSelectedGroupName = uiSelectedGroupName,
            onControlledSelectedGroupNameChange = proxyViewModel::selectUiGroup,
        )
    val selectedGroupName = groupSelection.selectedGroupName
    val displayGroup = groupSelection.displayGroup
    val currentGroup = groupSelection.selectedGroup ?: displayGroup ?: proxyGroups.firstOrNull()
    var showSortPopup by rememberSaveable { mutableStateOf(false) }
    var nodeSearchQuery by rememberSaveable(selectedGroupName) { mutableStateOf("") }
    val nodeSearchVisible = screen.nodeSearchVisible
    // "更多"弹窗里的搜索开关;关闭时清空查询,避免隐藏的过滤条件继续生效。
    val toggleNodeSearch = {
        val next = !nodeSearchVisible
        proxyViewModel.setNodeSearchVisible(next)
        if (!next) {
            nodeSearchQuery = ""
        }
    }

    // The tablet detail pane can outlive the left pager during a destination transition. Keep a
    // dedicated sync owner so a cold local core is queried even when the left page is not resumed.
    LaunchedEffect(proxyViewModel) {
        proxyViewModel.ensureCoreLoaded(true, source = "proxy_detail")
    }

    DisposableEffect(proxyViewModel) {
        onDispose { proxyViewModel.ensureCoreLoaded(false, source = "proxy_detail") }
    }

    LaunchedEffect(proxyGroups, selectedGroupName) {
        if (proxyGroups.isEmpty()) return@LaunchedEffect
        if (selectedGroupName == null || proxyGroups.none { it.name == selectedGroupName }) {
            groupSelection.selectGroup(proxyGroups.first())
        }
    }

    val activeGroupName = selectedGroupName ?: currentGroup?.name

    AnimatedContent(
        targetState = activeGroupName,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            val fromIndex =
                initialState?.let { name -> proxyGroups.indexOfFirst { it.name == name } } ?: -1
            val toIndex =
                targetState?.let { name -> proxyGroups.indexOfFirst { it.name == name } } ?: -1
            verticalBounceContentTransform(forward = toIndex >= fromIndex)
        },
        label = "proxy_shell_group_switch",
    ) { groupName ->
        val pageGroup =
            groupName?.let { name -> proxyGroups.firstOrNull { it.name == name } } ?: currentGroup
        // Adaptive grid owns the real scroll surface on the shell right pane.
        val nodeGridState =
            rememberSaveable(groupName, saver = LazyGridState.Saver) { LazyGridState() }
        val requestSelectedGroupDelayTest =
            remember(groupName, proxyViewModel) {
                {
                    val targetGroupName = groupName ?: return@remember
                    proxyViewModel.testDelay(targetGroupName)
                }
            }
        val locateCurrentProxy =
            remember(coroutineScope, pageGroup, nodeGridState, groupName) {
                if (groupName == null || pageGroup == null) {
                    null
                } else {
                    pageGroup
                        .takeIf { group -> group.name == groupName }
                        ?.let { group ->
                            fun() {
                                val proxyIndex =
                                    group.proxies.indexOfFirst { proxy -> proxy.name == group.now }
                                if (proxyIndex < 0) return
                                coroutineScope.launch {
                                    nodeGridState.animateLocateToItem(proxyIndex + 1)
                                }
                            }
                        }
                }
            }
        ProvideProxyDelayFlows(proxyViewModel) {
            GlassScaffold(
                topBar = {
                    ProxyTopBar(
                        title = pageGroup?.name ?: YumeTxt.Proxy.Title,
                        scrollBehavior = scrollBehavior,
                        showBack = false,
                        onBack = {},
                        onNavigateToProviders = onNavigateToProviders,
                        onOpenSettings = onOpenSettings,
                        onTestAllNodes = testAllNodes,
                        onLocateCurrentProxy = locateCurrentProxy,
                        showSortPopup = showSortPopup,
                        onShowSortPopupChange = { showSortPopup = it },
                        sortMode = sortMode,
                        onSortSelected = proxyViewModel::setSortMode,
                        searchVisible = nodeSearchVisible,
                        onToggleSearch = if (pageGroup != null) toggleNodeSearch else null,
                    )
                }
            ) { scaffoldPadding ->
                if (pageGroup != null) {
                    NodeListPage(
                        group = pageGroup,
                        sortMode = sortMode,
                        testingGroupNames = proxyViewModel.testingGroupNames,
                        testingProxyNames = testingProxyNames,
                        delayTestProgress = proxyViewModel.delayTestProgress,
                        mainInnerPadding = mainInnerPadding,
                        outerInnerPadding = scaffoldPadding,
                        scrollBehavior = scrollBehavior,
                        listState = remember { LazyListState() },
                        gridState = nodeGridState,
                        onSelectProxy = { selectedGroup, proxyName ->
                            proxyViewModel.selectProxy(selectedGroup, proxyName)
                        },
                        onTestDelay = requestSelectedGroupDelayTest,
                        onTestProxyDelay = proxyViewModel::testProxyDelay,
                        onScrollDirectionChanged = {},
                        useAdaptiveGrid = true,
                        searchQuery = nodeSearchQuery,
                        onSearchQueryChange = { nodeSearchQuery = it },
                        searchVisible = nodeSearchVisible,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProxyTopBar(
    title: String,
    scrollBehavior: ScrollBehavior,
    showBack: Boolean,
    onBack: () -> Unit,
    onNavigateToProviders: (() -> Unit)?,
    onOpenSettings: (() -> Unit)? = null,
    // 测试全部节点;顶栏 chrome 在分组页/节点页不再区分,统一为这一个胶囊。
    onTestAllNodes: () -> Unit,
    // 仅在选中节点组(节点页)时非空 → "更多"弹窗里出现"定位当前节点"。
    onLocateCurrentProxy: (() -> Unit)? = null,
    // 节点页搜索栏开关;非空 → "更多"弹窗里出现"搜索节点"勾选项。
    searchVisible: Boolean = false,
    onToggleSearch: (() -> Unit)? = null,
    showSortPopup: Boolean,
    onShowSortPopupChange: (Boolean) -> Unit,
    sortMode: ProxySortMode,
    onSortSelected: (ProxySortMode) -> Unit,
) {
    val backdrop = LocalTopBarBackdrop.current
    TopBar(
        title = title,
        scrollBehavior = scrollBehavior,
        navigationIconPadding = UiDp.dp24,
        actionIconPadding = UiDp.dp24,
        navigationIcon = {
            if (showBack) {
                IconButton(onClick = onBack) {
                    Icon(
                        MiuixIcons.Back,
                        contentDescription = YumeTxt.Component.Navigation.Back,
                    )
                }
            } else {
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
            }
        },
        actions = {
            // miuix 的 actions Row 自身不带间距;玻璃容器之间按参考实现留 8dp。
            Row(
                horizontalArrangement = Arrangement.spacedBy(UiDp.dp8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 合并后的胶囊:测速全部 + 更多。外部资源/定位/排序都收进"更多"弹窗,
                // 弹窗锚定到包裹胶囊的 Box(BottomEnd ≈ 胶囊右缘,即 more 图标下方)。
                Box {
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
                                    onClick = { onShowSortPopupChange(!showSortPopup) },
                                ),
                            ),
                        backdrop = backdrop,
                    )
                    NodeSortPopup(
                        show = showSortPopup,
                        onDismiss = { onShowSortPopupChange(false) },
                        sortMode = sortMode,
                        alignment = PopupPositionProvider.Align.BottomEnd,
                        onOpenProviders = onNavigateToProviders,
                        onLocateCurrentProxy = onLocateCurrentProxy,
                        searchVisible = searchVisible,
                        onToggleSearch = onToggleSearch,
                        onSortSelected = onSortSelected,
                    )
                }
                onOpenSettings?.let { openSettings ->
                    GlassTopIconButton(
                        action =
                            GlassTopAction(
                                icon = Yume.Settings2,
                                contentDescription = YumeTxt.Settings.Title,
                                onClick = openSettings,
                            ),
                        backdrop = backdrop,
                    )
                }
            }
        },
    )
}
