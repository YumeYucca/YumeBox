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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import com.github.yumeyucca.yumebox.data.model.ProxySortMode
import com.github.yumeyucca.yumebox.core.model.isProxyGroup
import com.github.yumeyucca.yumebox.domain.model.ProxyGroupInfo
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.House
import com.github.yumeyucca.yumebox.presentation.icon.yume.ListChevronsUpDown
import com.github.yumeyucca.yumebox.presentation.icon.yume.Settings2
import com.github.yumeyucca.yumebox.presentation.icon.yume.Speed
import com.github.yumeyucca.yumebox.presentation.screen.node.NodeActionsMenu
import com.github.yumeyucca.yumebox.presentation.screen.node.ProvideProxyDelayFlows
import com.github.yumeyucca.yumebox.presentation.screen.node.nodeGroupItems
import com.github.yumeyucca.yumebox.presentation.theme.*
import com.github.yumeyucca.yumebox.presentation.viewmodel.ProxyViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.glass.GlassPopupDefaults
import top.yukonga.miuix.kmp.glass.glassPopupAnchor
import top.yukonga.miuix.kmp.glass.glassPopupAnchorContent
import top.yukonga.miuix.kmp.glass.rememberGlassPopupAnchor
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
    isActive: Boolean,
    onOpenRouting: (() -> Unit)? = null,
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
    var twoColumnNodes by rememberSaveable(inSplitShell) { mutableStateOf(inSplitShell) }
    // Group-tab mode replaces the category/group drill-down with one swipeable group strip.
    var groupTabsMode by rememberSaveable(inSplitShell) { mutableStateOf(false) }
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
    val visibleProxyGroups = if (groupTabsMode) proxyGroups else displayedProxyGroups
    val tabbedPagerState =
        if (groupTabsMode && visibleProxyGroups.isNotEmpty()) {
            key(proxyListTabIndex) {
                rememberProxyGroupPagerState(
                    groups = visibleProxyGroups,
                    selectedGroupName = selectedGroupName,
                    onGroupSelected = groupSelection.selectGroup,
                )
            }
        } else {
            null
        }
    // 节点列表页里点 tab(含重按当前 tab)= 切换过滤并回到分组列表。
    val selectProxyListTab: (Int) -> Unit = { index ->
        proxyListTabIndex = index
        if (!groupTabsMode && selectedGroupName != null) {
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
    val nodeGridState =
        rememberSaveable(selectedGroupName, saver = LazyGridState.Saver) { LazyGridState() }
    val groupTabListStates = remember { mutableStateMapOf<String, LazyListState>() }
    val groupTabGridStates = remember { mutableStateMapOf<String, LazyGridState>() }
    val activeGroupTabName = tabbedPagerState?.currentPage?.let(visibleProxyGroups::getOrNull)?.name
    val activeGroupTabListState = activeGroupTabName?.let(groupTabListStates::get)
    val activeGroupTabGridState = activeGroupTabName?.let(groupTabGridStates::get)
    val tabCollapseState = rememberProxyTabScrollState()
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

    BackHandler(
        enabled =
            isActive &&
                !inSplitShell &&
                selectedGroupName != null &&
                !groupTabsMode
    ) {
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
                    onOpenRouting = onOpenRouting,
                    onOpenSettings = onOpenSettings,
                    onTestAllNodes = testAllNodes,
                    twoColumnNodes = twoColumnNodes,
                    onNodeColumnsSelected =
                        if (!inSplitShell) {
                            { twoColumnNodes = it }
                        } else {
                            null
                        },
                    groupTabsMode = groupTabsMode,
                    onGroupTabsModeSelected =
                        if (!inSplitShell) {
                            { groupTabsMode = it }
                        } else {
                            null
                        },
                    sortMode = sortMode,
                    onSortSelected = proxyViewModel::setSortMode,
                    searchVisible = nodeSearchVisible,
                    onToggleSearch = if (selectedGroupName != null) toggleNodeSearch else null,
                )
            },
            contentOverlay = { scaffoldPadding ->
                val tabs = if (groupTabsMode) visibleProxyGroups.map { it.name } else proxyListTabs
                val selectedTabIndex = tabbedPagerState?.currentPage ?: proxyListTabIndex
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
                    remember(activeListState, activeGridState, twoColumnNodes) {
                        derivedStateOf {
                            val stateAtTop =
                                if (twoColumnNodes) {
                                    activeGridState?.let {
                                        it.firstVisibleItemIndex == 0 &&
                                            it.firstVisibleItemScrollOffset == 0
                                    } ?: true
                                } else {
                                    activeListState?.let {
                                        it.firstVisibleItemIndex == 0 &&
                                            it.firstVisibleItemScrollOffset == 0
                                    } ?: true
                                }
                            stateAtTop
                        }
                    }
                LaunchedEffect(activeListState, activeGridState) {
                    tabCollapseState.syncContent(activeIsAtTop)
                }
                LaunchedEffect(activeIsAtTop) {
                    if (activeIsAtTop) tabCollapseState.resetToTop()
                }
                if (tabs.isNotEmpty()) {
                    ProxyCollapsingTabs(
                        tabs = tabs,
                        selectedTabIndex = selectedTabIndex,
                        onTabSelected = { index ->
                            if (groupTabsMode) {
                                tabbedPagerState?.let { state ->
                                    coroutineScope.launch { state.animateScrollToPage(index) }
                                }
                            } else {
                                selectProxyListTab(index)
                            }
                        },
                        topPadding = scaffoldPadding.calculateTopPadding() + UiDp.dp20,
                        collapseState = tabCollapseState,
                        scrollable = groupTabsMode,
                    )
                }
            },
        ) { scaffoldPadding ->
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .nestedScroll(tabCollapseState.nestedScrollConnection)
            ) {
                if (groupTabsMode && tabbedPagerState != null) {
                    ProxyGroupTabPager(
                        groups = visibleProxyGroups,
                        pagerState = tabbedPagerState,
                    ) { pageGroup ->
                        val pageListState =
                            rememberSaveable(pageGroup.name, saver = LazyListState.Saver) {
                                LazyListState()
                            }
                        val pageGridState =
                            rememberSaveable(pageGroup.name, saver = LazyGridState.Saver) {
                                LazyGridState()
                            }
                        LaunchedEffect(pageGroup.name, pageListState, pageGridState) {
                            groupTabListStates[pageGroup.name] = pageListState
                            groupTabGridStates[pageGroup.name] = pageGridState
                        }
                        NodeListPage(
                            group = pageGroup,
                            sortMode = sortMode,
                            testingGroupNames = proxyViewModel.testingGroupNames,
                            testingProxyNames = testingProxyNames,
                            delayTestProgress = proxyViewModel.delayTestProgress,
                            mainInnerPadding = mainInnerPadding,
                            outerInnerPadding = scaffoldPadding,
                            scrollBehavior = groupScrollBehavior,
                            listState = pageListState,
                            onSelectProxy = { groupName, proxyName ->
                                proxyViewModel.selectProxy(groupName, proxyName)
                            },
                            onTestDelay = {
                                proxyViewModel.testDelay(pageGroup.name)
                            },
                            onTestProxyDelay = proxyViewModel::testProxyDelay,
                            onScrollDirectionChanged = {},
                            twoColumnGrid = twoColumnNodes,
                            gridState = pageGridState,
                            searchQuery = nodeSearchQuery,
                            onSearchQueryChange = { nodeSearchQuery = it },
                            searchVisible = nodeSearchVisible,
                            topOverlayInset = ProxyTabBandInset,
                        )
                    }
                } else if (inSplitShell) {
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
                        topOverlayInset = if (proxyListTabs.isEmpty()) UiDp.dp0 else ProxyTabBandInset,
                    )
                    }
                } else {
                    AnimatedContent(
                    targetState = selectedGroupName,
                    transitionSpec = {
                        if (targetState != null) {
                            // 钻入分组 = 镜头向前推(shared-axis Z,纯 scale+alpha):
                            // 分组列表放大掠过镜头淡出,节点页从远处(0.90)迎面长到原位淡入。
                            (fadeIn(
                                animationSpec =
                                    tween(
                                        durationMillis = 220,
                                        easing = AnimationSpecs.Legacy,
                                    )
                            ) +
                                scaleIn(
                                    animationSpec =
                                        tween(
                                            durationMillis = 360,
                                            easing = AnimationSpecs.EmphasizedDecelerate,
                                        ),
                                    initialScale = 0.90f,
                                )) togetherWith
                                (fadeOut(
                                    animationSpec =
                                        tween(
                                            durationMillis = 160,
                                            easing = AnimationSpecs.Legacy,
                                        )
                                ) +
                                    scaleOut(
                                        animationSpec =
                                            tween(
                                                durationMillis = 360,
                                                easing = AnimationSpecs.EmphasizedAccelerate,
                                            ),
                                        targetScale = 1.10f,
                                    ))
                        } else {
                            // 返回 = 镜头拉回:分组列表从 1.10 缩回复位,节点页退回远处(0.90)淡出。
                            (fadeIn(
                                animationSpec =
                                    tween(
                                        durationMillis = 220,
                                        easing = AnimationSpecs.Legacy,
                                    )
                            ) +
                                scaleIn(
                                    animationSpec =
                                        tween(
                                            durationMillis = 360,
                                            easing = AnimationSpecs.EmphasizedDecelerate,
                                        ),
                                    initialScale = 1.10f,
                                )) togetherWith
                                (fadeOut(
                                    animationSpec =
                                        tween(
                                            durationMillis = 160,
                                            easing = AnimationSpecs.Legacy,
                                        )
                                ) +
                                    scaleOut(
                                        animationSpec =
                                            tween(
                                                durationMillis = 360,
                                                easing = AnimationSpecs.EmphasizedAccelerate,
                                            ),
                                        targetScale = 0.90f,
                                    ))
                        }
                    },
                    label = "proxy_content_drill",
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
                                topOverlayInset =
                                    if (proxyListTabs.isEmpty()) UiDp.dp0 else ProxyTabBandInset,
                            )
                        }
                    } else {
                        // 钻入转场需要不透明底色,才能盖住底下的分组列表。
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .background(MiuixTheme.colorScheme.surface),
                        ) {
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
                                twoColumnGrid = twoColumnNodes,
                                gridState = nodeGridState,
                                searchQuery = nodeSearchQuery,
                                onSearchQueryChange = { nodeSearchQuery = it },
                                searchVisible = nodeSearchVisible,
                                topOverlayInset =
                                    if (proxyListTabs.isEmpty()) UiDp.dp0 else ProxyTabBandInset,
                            )
                        }
                    }
                }
            }
        }
    }
    }
}

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
    topOverlayInset: Dp = UiDp.dp0,
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
            testingGroupNames = testingNames,
            itemVerticalPadding = UiDp.dp5,
            revealCount = revealCount,
        )
    }
}

@Composable
internal fun ProxyShellNodeDetailContent(
    mainInnerPadding: PaddingValues,
    onOpenSettings: (() -> Unit)? = null,
    onOpenRouting: (() -> Unit)? = null,
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
    var nodeSearchQuery by rememberSaveable(selectedGroupName) { mutableStateOf("") }
    var twoColumnNodes by rememberSaveable { mutableStateOf(true) }
    var groupTabsMode by rememberSaveable { mutableStateOf(false) }
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
    val groupTabPagerState =
        if (groupTabsMode && proxyGroups.isNotEmpty()) {
            rememberProxyGroupPagerState(
                groups = proxyGroups,
                selectedGroupName = selectedGroupName,
                onGroupSelected = groupSelection.selectGroup,
            )
        } else {
            null
        }
    val groupTabListStates = remember { mutableStateMapOf<String, LazyListState>() }
    val groupTabGridStates = remember { mutableStateMapOf<String, LazyGridState>() }
    val activeGroupTabName = groupTabPagerState?.currentPage?.let(proxyGroups::getOrNull)?.name
    val activeGroupTabListState = activeGroupTabName?.let(groupTabListStates::get)
    val activeGroupTabGridState = activeGroupTabName?.let(groupTabGridStates::get)
    val tabCollapseState = rememberProxyTabScrollState()
    val activeIsAtTop by
        remember(activeGroupTabListState, activeGroupTabGridState, twoColumnNodes) {
            derivedStateOf {
                if (twoColumnNodes) {
                    activeGroupTabGridState?.let {
                        it.firstVisibleItemIndex == 0 && it.firstVisibleItemScrollOffset == 0
                    } ?: true
                } else {
                    activeGroupTabListState?.let {
                        it.firstVisibleItemIndex == 0 && it.firstVisibleItemScrollOffset == 0
                    } ?: true
                }
            }
        }
    LaunchedEffect(activeGroupTabListState, activeGroupTabGridState) {
        tabCollapseState.syncContent(activeIsAtTop)
    }
    LaunchedEffect(activeIsAtTop) {
        if (activeIsAtTop) tabCollapseState.resetToTop()
    }

    AnimatedContent(
        targetState = if (groupTabsMode) null else activeGroupName,
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
        // The selected layout owns the real scroll surface on the shell right pane.
        val nodeListState =
            rememberSaveable(groupName, saver = LazyListState.Saver) { LazyListState() }
        val nodeGridState =
            rememberSaveable(groupName, saver = LazyGridState.Saver) { LazyGridState() }
        val requestSelectedGroupDelayTest =
            remember(groupName, proxyViewModel) {
                {
                    val targetGroupName = groupName ?: return@remember
                    proxyViewModel.testDelay(targetGroupName)
                }
            }
        ProvideProxyDelayFlows(proxyViewModel) {
            GlassScaffold(
                topBar = {
                    ProxyTopBar(
                        title = YumeTxt.Proxy.Title,
                        scrollBehavior = scrollBehavior,
                        showBack = false,
                        onBack = {},
                        onOpenRouting = onOpenRouting,
                        onOpenSettings = onOpenSettings,
                        onTestAllNodes = testAllNodes,
                        twoColumnNodes = twoColumnNodes,
                        onNodeColumnsSelected = { twoColumnNodes = it },
                        groupTabsMode = groupTabsMode,
                        onGroupTabsModeSelected = { groupTabsMode = it },
                        sortMode = sortMode,
                        onSortSelected = proxyViewModel::setSortMode,
                        searchVisible = nodeSearchVisible,
                        onToggleSearch = if (pageGroup != null) toggleNodeSearch else null,
                    )
                },
                contentOverlay = { scaffoldPadding ->
                    if (groupTabsMode && groupTabPagerState != null) {
                        val tabs = remember(proxyGroups) { proxyGroups.map { it.name } }
                        ProxyCollapsingTabs(
                            tabs = tabs,
                            selectedTabIndex = groupTabPagerState.currentPage,
                            onTabSelected = { index ->
                                coroutineScope.launch { groupTabPagerState.animateScrollToPage(index) }
                            },
                            topPadding = scaffoldPadding.calculateTopPadding() + UiDp.dp20,
                            collapseState = tabCollapseState,
                            scrollable = true,
                        )
                    }
                },
            ) { scaffoldPadding ->
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .nestedScroll(tabCollapseState.nestedScrollConnection)
                ) {
                if (groupTabsMode && groupTabPagerState != null) {
                    ProxyGroupTabPager(
                        groups = proxyGroups,
                        pagerState = groupTabPagerState,
                    ) { pageGroup ->
                        val pageListState =
                            rememberSaveable(pageGroup.name, saver = LazyListState.Saver) {
                                LazyListState()
                            }
                        val pageGridState =
                            rememberSaveable(pageGroup.name, saver = LazyGridState.Saver) {
                                LazyGridState()
                            }
                        LaunchedEffect(pageGroup.name, pageListState, pageGridState) {
                            groupTabListStates[pageGroup.name] = pageListState
                            groupTabGridStates[pageGroup.name] = pageGridState
                        }
                        NodeListPage(
                            group = pageGroup,
                            sortMode = sortMode,
                            testingGroupNames = proxyViewModel.testingGroupNames,
                            testingProxyNames = testingProxyNames,
                            delayTestProgress = proxyViewModel.delayTestProgress,
                            mainInnerPadding = mainInnerPadding,
                            outerInnerPadding = scaffoldPadding,
                            scrollBehavior = scrollBehavior,
                            listState = pageListState,
                            gridState = pageGridState,
                            onSelectProxy = { selectedGroup, proxyName ->
                                proxyViewModel.selectProxy(selectedGroup, proxyName)
                            },
                            onTestDelay = {
                                proxyViewModel.testDelay(pageGroup.name)
                            },
                            onTestProxyDelay = proxyViewModel::testProxyDelay,
                            onScrollDirectionChanged = {},
                            twoColumnGrid = twoColumnNodes,
                            searchQuery = nodeSearchQuery,
                            onSearchQueryChange = { nodeSearchQuery = it },
                            searchVisible = nodeSearchVisible,
                            topOverlayInset = ProxyTabBandInset,
                        )
                    }
                } else if (pageGroup != null) {
                    NodeListPage(
                        group = pageGroup,
                        sortMode = sortMode,
                        testingGroupNames = proxyViewModel.testingGroupNames,
                        testingProxyNames = testingProxyNames,
                        delayTestProgress = proxyViewModel.delayTestProgress,
                        mainInnerPadding = mainInnerPadding,
                        outerInnerPadding = scaffoldPadding,
                        scrollBehavior = scrollBehavior,
                        listState = nodeListState,
                        gridState = nodeGridState,
                        onSelectProxy = { selectedGroup, proxyName ->
                            proxyViewModel.selectProxy(selectedGroup, proxyName)
                        },
                        onTestDelay = requestSelectedGroupDelayTest,
                        onTestProxyDelay = proxyViewModel::testProxyDelay,
                        onScrollDirectionChanged = {},
                        twoColumnGrid = twoColumnNodes,
                        searchQuery = nodeSearchQuery,
                        onSearchQueryChange = { nodeSearchQuery = it },
                        searchVisible = nodeSearchVisible,
                    )
                }
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
    onOpenRouting: (() -> Unit)?,
    onOpenSettings: (() -> Unit)? = null,
    // 测试全部节点;顶栏 chrome 在分组页/节点页不再区分,统一为这一个胶囊。
    onTestAllNodes: () -> Unit,
    twoColumnNodes: Boolean = false,
    onNodeColumnsSelected: ((Boolean) -> Unit)? = null,
    groupTabsMode: Boolean = false,
    onGroupTabsModeSelected: ((Boolean) -> Unit)? = null,
    // 节点页搜索栏开关;非空 → "更多"弹窗里出现"搜索节点"勾选项。
    searchVisible: Boolean = false,
    onToggleSearch: (() -> Unit)? = null,
    sortMode: ProxySortMode,
    onSortSelected: (ProxySortMode) -> Unit,
) {
    val backdrop = LocalTopBarBackdrop.current
    var showActionsMenu by remember { mutableStateOf(false) }
    val actionsMenuAnchor = rememberGlassPopupAnchor()
    TopBar(
        title = title,
        scrollBehavior = scrollBehavior,
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
                    keepSurface = true,
                )
            }
        },
        actions = {
            // miuix 的 actions Row 自身不带间距;玻璃容器之间按参考实现留 8dp。
            Row(
                horizontalArrangement = Arrangement.spacedBy(UiDp.dp8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 测速与更多保持在同一个玻璃胶囊里，弹窗只由右侧更多按钮触发。
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
                                onClick = { showActionsMenu = !showActionsMenu },
                                modifier = Modifier.glassPopupAnchor(
                                    anchor = actionsMenuAnchor,
                                    cornerRadius = GlassTopActionDefaults.IconSize / 2,
                                    floating = true,
                                ),
                                iconModifier = Modifier.glassPopupAnchorContent(actionsMenuAnchor),
                            ),
                        ),
                    popupAnchor = actionsMenuAnchor,
                    backdrop = backdrop,
                    actionOverlay = { index ->
                        if (index == 1) {
                            NodeActionsMenu(
                                show = showActionsMenu,
                                onDismiss = { showActionsMenu = false },
                                sortMode = sortMode,
                                onOpenRouting = onOpenRouting,
                                twoColumnNodes = twoColumnNodes,
                                onNodeColumnsSelected = onNodeColumnsSelected,
                                groupTabsMode = groupTabsMode,
                                onGroupTabsModeSelected = onGroupTabsModeSelected,
                                searchVisible = searchVisible,
                                onToggleSearch = onToggleSearch,
                                onSortSelected = onSortSelected,
                                anchor = actionsMenuAnchor,
                                anchorContent = {
                                    Icon(
                                        imageVector = Yume.ListChevronsUpDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(GlassTopActionDefaults.IconSize),
                                        tint = MiuixTheme.colorScheme.onSurface,
                                    )
                                },
                            )
                        }
                    },
                )
                onOpenSettings?.let { openSettings ->
                    GlassTopIconButton(
                        action =
                            GlassTopAction(
                                icon = Yume.Settings2,
                                contentDescription = YumeTxt.Settings.Title,
                                onClick = openSettings,
                            ),
                        backdrop = backdrop,
                        keepSurface = true,
                    )
                }
            }
        },
    )
}





