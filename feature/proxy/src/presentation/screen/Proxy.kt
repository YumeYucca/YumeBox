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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.github.yumeyucca.yumebox.data.model.ProxySortMode
import com.github.yumeyucca.yumebox.domain.model.ProxyGroupInfo
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.Eye
import com.github.yumeyucca.yumebox.presentation.icon.yume.ListChevronsUpDown
import com.github.yumeyucca.yumebox.presentation.icon.yume.Speed
import com.github.yumeyucca.yumebox.presentation.screen.node.NodeSortPopup
import com.github.yumeyucca.yumebox.presentation.screen.node.nodeGroupItems
import com.github.yumeyucca.yumebox.presentation.theme.*
import com.github.yumeyucca.yumebox.presentation.util.ProxyDelayPullToRefresh
import com.github.yumeyucca.yumebox.presentation.util.rememberAllGroupsPullToRefreshTexts
import com.github.yumeyucca.yumebox.presentation.viewmodel.ProxyViewModel
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import tf.gal.yumebox.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back

private data class ProxyScreenVmState(
    val proxyGroups: List<ProxyGroupInfo>,
    val testingGroupNames: Set<String>,
    val testingProxyNames: Set<String>,
    val sortMode: ProxySortMode,
    val uiSelectedGroupName: String?,
    val showNodeSearch: Boolean,
)

@Composable
private fun rememberProxyScreenVmState(proxyViewModel: ProxyViewModel): ProxyScreenVmState {
    val proxyGroups by proxyViewModel.sortedProxyGroups.collectAsState()
    val testingGroupNames by proxyViewModel.testingGroupNames.collectAsState()
    val testingProxyNames by proxyViewModel.testingProxyNames.collectAsState()
    val sortMode by proxyViewModel.sortMode.collectAsState()
    val uiSelectedGroupName by proxyViewModel.uiSelectedGroupName.collectAsState()
    val showNodeSearch by proxyViewModel.showNodeSearch.collectAsState()
    return remember(
        proxyGroups,
        testingGroupNames,
        testingProxyNames,
        sortMode,
        uiSelectedGroupName,
        showNodeSearch,
    ) {
        ProxyScreenVmState(
            proxyGroups = proxyGroups,
            testingGroupNames = testingGroupNames,
            testingProxyNames = testingProxyNames,
            sortMode = sortMode,
            uiSelectedGroupName = uiSelectedGroupName,
            showNodeSearch = showNodeSearch,
        )
    }
}

@Composable
fun ProxyPager(
    mainInnerPadding: PaddingValues,
    onNavigateToProviders: (() -> Unit)?,
    isActive: Boolean,
    @Suppress("UNUSED_PARAMETER") windowLayoutMode: WindowLayoutMode = WindowLayoutMode.Compact,
) {
    val proxyViewModel = koinViewModel<ProxyViewModel>()
    val screen = rememberProxyScreenVmState(proxyViewModel)
    val proxyGroups = screen.proxyGroups
    val testingGroupNames = screen.testingGroupNames
    val testingProxyNames = screen.testingProxyNames
    val sortMode = screen.sortMode
    val uiSelectedGroupName = screen.uiSelectedGroupName
    val showNodeSearch = screen.showNodeSearch
    val groupScrollBehavior = MiuixScrollBehavior(snapAnimationSpec = null)
    val topBarHazeState = LocalTopBarHazeState.current

    var showSortPopup by rememberSaveable { mutableStateOf(false) }
    val inSplitShell = LocalDetailNavigator.current.isSplitShell
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
    var nodeSearchQuery by rememberSaveable(selectedGroupName) { mutableStateOf("") }

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
        remember(coroutineScope, nodeListState, selectedGroupName, proxyViewModel) {
            {
                val groupName = selectedGroupName ?: return@remember
                coroutineScope.launch {
                    if (nodeListState.isScrolledFromTop()) {
                        nodeListState.animateScrollToItem(0)
                    }
                    proxyViewModel.testDelay(groupName)
                }
            }
        }
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

    Scaffold(
            topBar = {
                ProxyTopBar(
                    title = YumeTxt.Proxy.Title,
                    scrollBehavior = groupScrollBehavior,
                    showBack = false,
                    onBack = {},
                    onNavigateToProviders = onNavigateToProviders,
                    onLocateCurrentProxy = locateCurrentProxy,
                    onTestDelay = null,
                    isDelayTesting = false,
                    searchEnabled = showNodeSearch,
                    onSearchToggle =
                        selectedGroupName?.let {
                            {
                                val nextVisible = !showNodeSearch
                                proxyViewModel.setShowNodeSearch(nextVisible)
                                if (!nextVisible) nodeSearchQuery = ""
                            }
                        },
                    showSortPopup = showSortPopup,
                    onShowSortPopupChange = { showSortPopup = it },
                    sortMode = sortMode,
                    onSortSelected = proxyViewModel::setSortMode,
                )
            }
        ) { scaffoldPadding ->
            Box(
                modifier =
                    Modifier.fillMaxSize().let { mod ->
                        if (topBarHazeState != null) mod.hazeSource(state = topBarHazeState) else mod
                    }
            ) {
                if (inSplitShell) {
                    if (proxyGroups.isNotEmpty()) {
                    ProxyContent(
                        proxyGroups = proxyGroups,
                        scrollBehavior = groupScrollBehavior,
                        innerPadding = scaffoldPadding,
                        mainInnerPadding = mainInnerPadding,
                        testingGroupNames = testingGroupNames,
                        onGroupClick = groupSelection.selectGroup,
                        onGroupTest = { group -> proxyViewModel.testDelay(group.name) },
                        onPullRefresh = { proxyViewModel.testDelay() },
                        listState = groupListState,
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
                                proxyGroups = proxyGroups,
                                scrollBehavior = groupScrollBehavior,
                                innerPadding = scaffoldPadding,
                                mainInnerPadding = mainInnerPadding,
                                testingGroupNames = testingGroupNames,
                                onGroupClick = groupSelection.selectGroup,
                                onGroupTest = { group -> proxyViewModel.testDelay(group.name) },
                                onPullRefresh = { proxyViewModel.testDelay() },
                                listState = groupListState,
                            )
                        }
                    } else {
                        NodeListPage(
                            group = selectedNodeGroup,
                            sortMode = sortMode,
                            testingGroupNames = testingGroupNames,
                            testingProxyNames = testingProxyNames,
                            mainInnerPadding = mainInnerPadding,
                            outerInnerPadding = scaffoldPadding,
                            scrollBehavior = groupScrollBehavior,
                            listState = nodeListState,
                            onSelectProxy = { groupName, proxyName ->
                                proxyViewModel.selectProxy(groupName, proxyName)
                            },
                            onTestDelay = requestSelectedGroupDelayTest,
                            onTestProxyDelay = { proxyName ->
                                selectedGroupName?.let { groupName ->
                                    proxyViewModel.testProxyDelay(groupName, proxyName)
                                }
                            },
                            onScrollDirectionChanged = {},
                            useAdaptiveGrid = false,
                            searchQuery = nodeSearchQuery,
                            showSearch = showNodeSearch,
                            onSearchQueryChange = { nodeSearchQuery = it },
                        )
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
    onPullRefresh: () -> Unit,
    testingGroupNames: Set<String>,
    listState: LazyListState,
) {
    val spacing = LocalSpacing.current
    ProxyDelayPullToRefresh(
        isRefreshing = testingGroupNames.isNotEmpty(),
        onRefresh = onPullRefresh,
        refreshTexts = rememberAllGroupsPullToRefreshTexts(),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = innerPadding.calculateTopPadding()),
        scrollBehavior = scrollBehavior,
    ) {
        ScreenLazyColumn(
            lazyListState = listState,
            scrollBehavior = scrollBehavior,
            innerPadding = innerPadding,
            enableGlobalScroll = true,
            enableOverScroll = false,
            enableTopBarNestedScroll = false,
            contentPadding =
                PaddingValues(
                    start = UiDp.dp12,
                    end = UiDp.dp12,
                    top = innerPadding.calculateTopPadding() + UiDp.dp14,
                    bottom = mainInnerPadding.calculateBottomPadding() + spacing.space12,
                ),
        ) {
            nodeGroupItems(
                groups = proxyGroups,
                onGroupClick = onGroupClick,
                onGroupTest = onGroupTest,
                testingGroupNames = testingGroupNames,
                itemVerticalPadding = UiDp.dp6,
            )
        }
    }
}

@Composable
internal fun ProxyShellNodeDetailContent(
    mainInnerPadding: PaddingValues,
    onNavigateToProviders: (() -> Unit)? = null,
) {
    val proxyViewModel = koinViewModel<ProxyViewModel>()
    val screen = rememberProxyScreenVmState(proxyViewModel)
    val proxyGroups = screen.proxyGroups
    val testingGroupNames = screen.testingGroupNames
    val testingProxyNames = screen.testingProxyNames
    val sortMode = screen.sortMode
    val uiSelectedGroupName = screen.uiSelectedGroupName
    val showNodeSearch = screen.showNodeSearch
    val scrollBehavior = MiuixScrollBehavior(snapAnimationSpec = null)
    val coroutineScope = rememberCoroutineScope()
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
            remember(coroutineScope, nodeGridState, groupName, proxyViewModel) {
                {
                    val targetGroupName = groupName ?: return@remember
                    coroutineScope.launch {
                        if (nodeGridState.isScrolledFromTop()) {
                            nodeGridState.animateScrollToItem(0)
                        }
                        proxyViewModel.testDelay(targetGroupName)
                    }
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
        Scaffold(
                topBar = {
                    ProxyTopBar(
                        title = pageGroup?.name ?: YumeTxt.Proxy.Title,
                        scrollBehavior = scrollBehavior,
                        showBack = false,
                        onBack = {},
                        onNavigateToProviders = onNavigateToProviders,
                        onLocateCurrentProxy = locateCurrentProxy,
                        onTestDelay = requestSelectedGroupDelayTest,
                        isDelayTesting = groupName != null && groupName in testingGroupNames,
                        searchEnabled = showNodeSearch,
                        onSearchToggle = {
                            val nextVisible = !showNodeSearch
                            proxyViewModel.setShowNodeSearch(nextVisible)
                            if (!nextVisible) nodeSearchQuery = ""
                        },
                        showSortPopup = showSortPopup,
                        onShowSortPopupChange = { showSortPopup = it },
                        sortMode = sortMode,
                        onSortSelected = proxyViewModel::setSortMode,
                    )
                }
            ) { scaffoldPadding ->
                if (pageGroup != null) {
                    NodeListPage(
                        group = pageGroup,
                        sortMode = sortMode,
                        testingGroupNames = testingGroupNames,
                        testingProxyNames = testingProxyNames,
                        mainInnerPadding = mainInnerPadding,
                        outerInnerPadding = scaffoldPadding,
                        scrollBehavior = scrollBehavior,
                        listState = remember { LazyListState() },
                        gridState = nodeGridState,
                        onSelectProxy = { selectedGroup, proxyName ->
                            proxyViewModel.selectProxy(selectedGroup, proxyName)
                        },
                        onTestDelay = requestSelectedGroupDelayTest,
                        onTestProxyDelay = { proxyName ->
                            selectedGroupName?.let { groupName ->
                                proxyViewModel.testProxyDelay(groupName, proxyName)
                            }
                        },
                        onScrollDirectionChanged = {},
                        useAdaptiveGrid = true,
                        searchQuery = nodeSearchQuery,
                        showSearch = showNodeSearch,
                        onSearchQueryChange = { nodeSearchQuery = it },
                    )
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
    onLocateCurrentProxy: (() -> Unit)?,
    onTestDelay: (() -> Unit)?,
    isDelayTesting: Boolean,
    searchEnabled: Boolean,
    onSearchToggle: (() -> Unit)?,
    showSortPopup: Boolean,
    onShowSortPopupChange: (Boolean) -> Unit,
    sortMode: ProxySortMode,
    onSortSelected: (ProxySortMode) -> Unit,
) {
    val spacing = AppTheme.spacing

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
            } else if (onTestDelay != null) {
                // The detail pane has no back arrow; use the leading slot for one-tap delay
                // testing instead of forcing users back to the list's small test button
                // (issue #151 item 10).
                IconButton(onClick = onTestDelay) {
                    if (isDelayTesting) {
                        InfiniteProgressIndicator(
                            modifier = Modifier.size(UiDp.dp22),
                        )
                    } else {
                        Icon(
                            Yume.Speed,
                            contentDescription = YumeTxt.Proxy.Action.Test,
                        )
                    }
                }
            }
        },
        actions = {
            if (onLocateCurrentProxy != null) {
                IconButton(
                    modifier = Modifier.padding(end = spacing.space12),
                    onClick = onLocateCurrentProxy,
                ) {
                    Icon(Yume.Eye, contentDescription = YumeTxt.Proxy.Action.LocateCurrent)
                }
            }
            Box {
                IconButton(onClick = { onShowSortPopupChange(true) }) {
                    Icon(
                        Yume.ListChevronsUpDown,
                        contentDescription = YumeTxt.Proxy.Action.Sort,
                    )
                }
                NodeSortPopup(
                    show = showSortPopup,
                    onDismiss = { onShowSortPopupChange(false) },
                    sortMode = sortMode,
                    alignment = PopupPositionProvider.Align.BottomEnd,
                    onNavigateToProviders = onNavigateToProviders,
                    searchEnabled = searchEnabled,
                    onSearchToggle = onSearchToggle,
                    onSortSelected = onSortSelected,
                )
            }
        },
    )
}
