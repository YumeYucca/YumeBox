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

package com.github.yumeyucca.yumebox.presentation.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import com.github.yumeyucca.yumebox.data.model.ProxySortMode
import com.github.yumeyucca.yumebox.domain.model.ProxyGroupInfo
import com.github.yumeyucca.yumebox.presentation.component.GlassScaffold
import com.github.yumeyucca.yumebox.presentation.component.LocalDetailNavigator
import com.github.yumeyucca.yumebox.presentation.screen.node.NodeActionsMenu
import com.github.yumeyucca.yumebox.presentation.screen.node.NodeLayoutActions
import com.github.yumeyucca.yumebox.presentation.screen.node.NodeMenuActions
import com.github.yumeyucca.yumebox.presentation.screen.node.NodeSearchActions
import com.github.yumeyucca.yumebox.presentation.screen.node.ProvideProxyDelayFlows
import com.github.yumeyucca.yumebox.presentation.theme.AnimationSpecs
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.presentation.util.rememberSavedGridState
import com.github.yumeyucca.yumebox.presentation.util.rememberSavedListState
import com.github.yumeyucca.yumebox.presentation.viewmodel.ProxyViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal enum class ProxyPageMode {
    Compact,
    SplitMaster,
    SplitDetail,
}

@Composable
fun ProxyPager(
    mainInnerPadding: PaddingValues,
    isActive: Boolean,
    onOpenRouting: (() -> Unit)? = null,
) {
    val pane =
        if (LocalDetailNavigator.current != null) {
            ProxyPageMode.SplitMaster
        } else {
            ProxyPageMode.Compact
        }
    ProxyPageShell(
        pane = pane,
        mainInnerPadding = mainInnerPadding,
        isActive = isActive,
        onOpenRouting = onOpenRouting,
    )
}

/** Proxy page as rendered in the split shell's right pane. */
@Composable
fun ProxyShellNodeDetail(
    mainInnerPadding: PaddingValues,
    onOpenRouting: (() -> Unit)? = null,
) {
    ProxyPageShell(
        pane = ProxyPageMode.SplitDetail,
        mainInnerPadding = mainInnerPadding,
        isActive = true,
        onOpenRouting = onOpenRouting,
    )
}

@Composable
private fun ProxyPageShell(
    pane: ProxyPageMode,
    mainInnerPadding: PaddingValues,
    isActive: Boolean,
    onOpenRouting: (() -> Unit)? = null,
) {
    val proxyViewModel = koinViewModel<ProxyViewModel>()
    val proxyGroups by proxyViewModel.sortedProxyGroups.collectAsState()
    val delayTests by proxyViewModel.delayTests.collectAsState()
    val sortMode by proxyViewModel.sortMode.collectAsState()
    val uiSelectedGroupName by proxyViewModel.uiSelectedGroupName.collectAsState()
    val nodeSearchVisible by proxyViewModel.nodeSearchVisible.collectAsState()
    val scrollBehavior = MiuixScrollBehavior(snapAnimationSpec = null)
    val coroutineScope = rememberCoroutineScope()

    // Which selection the page uses is decided by pane: the phone split keeps its own,
    // while the split shell shares it with the left side through the ViewModel.
    // The local holder always stays in composition, so switching back to the phone
    // split restores the previous selection.
    val localSelection = rememberSelectedGroupController()
    val selectionController =
        if (pane == ProxyPageMode.Compact) {
            localSelection
        } else {
            rememberSelectedGroupController(uiSelectedGroupName, proxyViewModel::selectUiGroup)
        }
    val state =
        rememberProxyPageState(
            pane = pane,
            isActive = isActive,
            proxyViewModel = proxyViewModel,
            proxyGroups = proxyGroups,
            nodeSearchVisible = nodeSearchVisible,
            selectionController = selectionController,
        )
    // The group-tab branch uses the pager state as non-null, so bind it to a local
    // val before that branch.
    val tabbedPagerState = state.tabbedPagerState

    val tabs =
        when {
            state.groupTabsMode -> state.visibleProxyGroups.map { it.name }
            pane == ProxyPageMode.SplitDetail -> emptyList()
            else -> state.proxyListTabs
        }
    val selectedTabIndex = state.tabbedPagerState?.currentPage ?: state.proxyListTabIndex
    val searchAvailable =
        when (pane) {
            ProxyPageMode.SplitDetail -> state.selectedNodeGroup != null
            ProxyPageMode.Compact,
            ProxyPageMode.SplitMaster -> state.selectedGroupName != null
        }
    // What can appear in the "More" menu is decided once, here; the top bar only
    // lays it out and owns the anchor.
    val nodeMenuActions =
        buildNodeMenuActions(
            pane = pane,
            sortMode = sortMode,
            onSortSelected = proxyViewModel::setSortMode,
            state = state,
            searchAvailable = searchAvailable,
            nodeSearchVisible = nodeSearchVisible,
            onOpenRouting = onOpenRouting,
        )
    // Top bar speed-test button: always tests all nodes (the group page and the node
    // page are no longer distinguished).
    val testAllNodes: () -> Unit = { proxyViewModel.testDelay() }

    ProvideProxyDelayFlows(proxyViewModel) {
        GlassScaffold(
            topBar = {
                ProxyTopBarSection(
                    scrollBehavior = scrollBehavior,
                    onTestAllNodes = testAllNodes,
                    nodeMenuActions = nodeMenuActions,
                )
            },
            contentOverlay = { scaffoldPadding ->
                if (tabs.isNotEmpty()) {
                    ProxyTabsOverlay(
                        tabs = tabs,
                        selectedTabIndex = selectedTabIndex,
                        groupTabsMode = state.groupTabsMode,
                        tabCollapseState = state.tabCollapseState,
                        topPadding = scaffoldPadding.calculateTopPadding() + UiDp.dp20,
                        onTabSelected = { index ->
                            if (state.groupTabsMode) {
                                tabbedPagerState?.let { pagerState ->
                                    coroutineScope.launch { pagerState.animateScrollToPage(index) }
                                }
                            } else {
                                state.selectProxyListTab(index)
                            }
                        },
                    )
                }
            },
        ) { scaffoldPadding ->
            // The strategy page (split left side) and the phone split's group page render
            // the same list.
            val groupListContent: @Composable () -> Unit = {
                ProxyContent(
                    proxyGroups = state.displayedProxyGroups,
                    scrollBehavior = scrollBehavior,
                    innerPadding = scaffoldPadding,
                    mainInnerPadding = mainInnerPadding,
                    testingGroupNames = delayTests.groupNames,
                    onGroupClick = selectionController::selectGroup,
                    onGroupTest = { group -> proxyViewModel.testDelay(group.name) },
                    listState = state.groupListState,
                    topOverlayInset = state.categoryTabInset,
                )
            }
            // Values shared by all three node-page hosts; each call site copies only the
            // entries it changes.
            val nodePage =
                ProxyNodePageState(
                    sortMode = sortMode,
                    testingGroupNames = delayTests.groupNames,
                    testingProxyNames = delayTests.proxyNames,
                    mainInnerPadding = mainInnerPadding,
                    outerInnerPadding = scaffoldPadding,
                    scrollBehavior = scrollBehavior,
                    listState = state.nodeListState,
                    gridState = state.nodeGridState,
                    twoColumnNodes = state.twoColumnNodes,
                    searchQuery = state.nodeSearchQuery,
                    searchVisible = nodeSearchVisible,
                )
            val onSearchQueryChange: (String) -> Unit = { state.nodeSearchQuery = it }
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .nestedScroll(state.tabCollapseState.nestedScrollConnection)
            ) {
                if (state.groupTabsMode && tabbedPagerState != null) {
                    ProxyGroupTabsPane(
                        state = state,
                        pagerState = tabbedPagerState,
                        nodePage = nodePage,
                        proxyViewModel = proxyViewModel,
                        onSearchQueryChange = onSearchQueryChange,
                    )
                } else {
                    when (pane) {
                        ProxyPageMode.SplitMaster -> {
                            if (proxyGroups.isNotEmpty()) {
                                groupListContent()
                            }
                        }

                        ProxyPageMode.Compact -> {
                            ProxyCompactNodesPane(
                                state = state,
                                proxyGroups = proxyGroups,
                                groupListContent = groupListContent,
                                nodePage = nodePage,
                                proxyViewModel = proxyViewModel,
                                onSearchQueryChange = onSearchQueryChange,
                            )
                        }

                        ProxyPageMode.SplitDetail -> {
                            ProxySplitDetailNodesPane(
                                state = state,
                                proxyGroups = proxyGroups,
                                nodePage = nodePage,
                                proxyViewModel = proxyViewModel,
                                onSearchQueryChange = onSearchQueryChange,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProxyTopBarSection(
    scrollBehavior: ScrollBehavior,
    onTestAllNodes: () -> Unit,
    nodeMenuActions: NodeMenuActions,
) {
    ProxyTopBar(
        title = YumeTxt.Proxy.Title,
        scrollBehavior = scrollBehavior,
        onTestAllNodes = onTestAllNodes,
        menu = { popup ->
            NodeActionsMenu(
                show = popup.expanded,
                onDismiss = popup::dismiss,
                actions = nodeMenuActions,
                anchor = popup.anchor,
            )
        },
    )
}

/**
 * Collapsing group-tab overlay. The call site decides whether the slot emits at all, so an
 * empty [tabs] list never gets here.
 */
@Composable
private fun ProxyTabsOverlay(
    tabs: List<String>,
    selectedTabIndex: Int,
    groupTabsMode: Boolean,
    tabCollapseState: ProxyTabScrollState,
    topPadding: Dp,
    onTabSelected: (Int) -> Unit,
) {
    ProxyCollapsingTabs(
        tabs = tabs,
        selectedTabIndex = selectedTabIndex,
        onTabSelected = onTabSelected,
        topPadding = topPadding,
        collapseState = tabCollapseState,
        scrollable = groupTabsMode,
    )
}

/**
 * Group-tab host: one swipeable [ProxyNodePage] per visible group, each keeping its own saved
 * list/grid state, registered back into [ProxyPageState] for the following recompositions.
 */
@Composable
private fun ProxyGroupTabsPane(
    state: ProxyPageState,
    pagerState: PagerState,
    nodePage: ProxyNodePageState,
    proxyViewModel: ProxyViewModel,
    onSearchQueryChange: (String) -> Unit,
) {
    ProxyGroupTabPager(
        groups = state.visibleProxyGroups,
        pagerState = pagerState,
    ) { pageGroup ->
        val pageListState = rememberSavedListState(pageGroup.name)
        val pageGridState = rememberSavedGridState(pageGroup.name)
        LaunchedEffect(pageGroup.name, pageListState, pageGridState) {
            state.groupTabListStates[pageGroup.name] = pageListState
            state.groupTabGridStates[pageGroup.name] = pageGridState
        }
        ProxyNodePage(
            page =
                nodePage.copy(
                    group = pageGroup,
                    listState = pageListState,
                    gridState = pageGridState,
                    topOverlayInset = ProxyTabBandInset,
                ),
            proxyViewModel = proxyViewModel,
            onSearchQueryChange = onSearchQueryChange,
            onTestDelay = { proxyViewModel.testDelay(pageGroup.name) },
        )
    }
}

/**
 * Phone split host: slides between the group list and the selected group's node page, and stops
 * on its own saved scroll state while the node page is shown.
 */
@Composable
private fun ProxyCompactNodesPane(
    state: ProxyPageState,
    proxyGroups: List<ProxyGroupInfo>,
    groupListContent: @Composable () -> Unit,
    nodePage: ProxyNodePageState,
    proxyViewModel: ProxyViewModel,
    onSearchQueryChange: (String) -> Unit,
) {
    AnimatedContent(
        targetState = state.selectedGroupName,
        transitionSpec = {
            proxyGroupContentTransform(forward = targetState != null)
        },
        label = "proxy_content_slide",
    ) { targetGroupName ->
        if (targetGroupName == null) {
            if (proxyGroups.isNotEmpty()) {
                groupListContent()
            }
        } else {
            // The transition needs an opaque background so it can cover the group list
            // underneath.
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MiuixTheme.colorScheme.surface),
            ) {
                ProxyNodePage(
                    page =
                        nodePage.copy(
                            group = state.selectedNodeGroup,
                            topOverlayInset = state.categoryTabInset,
                        ),
                    proxyViewModel = proxyViewModel,
                    onSearchQueryChange = onSearchQueryChange,
                    onTestDelay = state::testSelectedGroupDelay,
                )
            }
        }
    }
}

/**
 * Split shell right-pane host: switches node pages by group, animating in the direction the
 * group order moved.
 */
@Composable
private fun ProxySplitDetailNodesPane(
    state: ProxyPageState,
    proxyGroups: List<ProxyGroupInfo>,
    nodePage: ProxyNodePageState,
    proxyViewModel: ProxyViewModel,
    onSearchQueryChange: (String) -> Unit,
) {
    val activeGroupName =
        state.selectedGroupName ?: proxyGroups.firstOrNull()?.name
    AnimatedContent(
        targetState = activeGroupName,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            val fromIndex =
                initialState?.let { name ->
                    proxyGroups.indexOfFirst { it.name == name }
                } ?: -1
            val toIndex =
                targetState?.let { name ->
                    proxyGroups.indexOfFirst { it.name == name }
                } ?: -1
            proxyGroupContentTransform(forward = toIndex >= fromIndex)
        },
        label = "proxy_shell_group_switch",
    ) { groupName ->
        val pageGroup =
            groupName
                ?.let { name ->
                    proxyGroups.firstOrNull { it.name == name }
                }
                ?: state.selectedNodeGroup
        // The selected layout owns the real scroll surface on the shell right pane.
        // Keyed on the group actually rendered: [groupName] is null while the
        // shell has not selected one, and keying on that would let every group
        // share one saved scroll position.
        val pageListState = rememberSavedListState(pageGroup?.name)
        val pageGridState = rememberSavedGridState(pageGroup?.name)
        ProxyNodePage(
            page =
                nodePage.copy(
                    group = pageGroup,
                    listState = pageListState,
                    gridState = pageGridState,
                ),
            proxyViewModel = proxyViewModel,
            onSearchQueryChange = onSearchQueryChange,
            onTestDelay = {
                pageGroup?.let { group ->
                    proxyViewModel.testDelay(group.name)
                }
            },
        )
    }
}

/**
 * Contents of the "More" menu: sorting is always there, layout only appears on the side
 * that has a node list, and search only on a group page that can search.
 * It is a function so the shell is left with wiring only — reading this one expression
 * tells you what the menu holds.
 */
private fun buildNodeMenuActions(
    pane: ProxyPageMode,
    sortMode: ProxySortMode,
    onSortSelected: (ProxySortMode) -> Unit,
    state: ProxyPageState,
    searchAvailable: Boolean,
    nodeSearchVisible: Boolean,
    onOpenRouting: (() -> Unit)?,
): NodeMenuActions =
    NodeMenuActions(
        sortMode = sortMode,
        onSortSelected = onSortSelected,
        layout =
            if (pane == ProxyPageMode.SplitMaster) {
                // The split left side only has a group list, so there is no node list to lay out.
                null
            } else {
                NodeLayoutActions(
                    twoColumnNodes = state.twoColumnNodes,
                    onTwoColumnNodesChange = { state.twoColumnNodes = it },
                    groupTabsMode = state.groupTabsMode,
                    onGroupTabsModeChange = { state.groupTabsMode = it },
                )
            },
        routing = onOpenRouting,
        search =
            if (searchAvailable) {
                NodeSearchActions(visible = nodeSearchVisible, onToggle = state::toggleNodeSearch)
            } else {
                null
            },
    )

/** Horizontal group ↔ node transition used by the phone and split-shell proxy pages. */
private fun proxyGroupContentTransform(forward: Boolean): ContentTransform {
    val enterMillis = if (forward) 380 else 340
    val exitMillis = if (forward) 340 else 380
    return (slideInHorizontally(
        animationSpec = tween(durationMillis = enterMillis, easing = AnimationSpecs.Legacy),
        initialOffsetX = { width -> if (forward) width else -width / 3 },
    ) + fadeIn(animationSpec = tween(durationMillis = 180, easing = AnimationSpecs.Legacy))) togetherWith
        (slideOutHorizontally(
            animationSpec = tween(durationMillis = exitMillis, easing = AnimationSpecs.Legacy),
            targetOffsetX = { width -> if (forward) -width / 3 else width },
        ) + fadeOut(animationSpec = tween(durationMillis = 160, easing = AnimationSpecs.Legacy)))
}
