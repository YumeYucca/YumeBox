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

@file:Suppress("FunctionName", "UsePropertyAccessSyntax")

package com.github.yumeyucca.yumebox.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import com.github.yumeyucca.yumebox.data.model.AccessControlSortMode
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.Bolt
import com.github.yumeyucca.yumebox.presentation.icon.yume.ListChevronsUpDown
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.theme.LocalTopBarBackdrop
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.glass.rememberGlassPopupAnchor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.github.yumeyucca.yumebox.presentation.component.rememberCollapsedTopPadding

@Composable
fun AccessControlAppListScreen() {
    val scrollBehavior = MiuixScrollBehavior()
    val spacing = AppTheme.spacing
    val mainLikePadding = rememberStandalonePageMainPadding()
    val viewModel = koinViewModel<AccessControlViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val filteredApps by viewModel.filteredApps.collectAsState()

    LaunchedEffect(viewModel) { viewModel.refreshSelection() }

    val mainListState = rememberLazyListState()
    val backdrop = LocalTopBarBackdrop.current
    val search = rememberSearchPagerState()
    val dynamicTopPadding by rememberCollapsedTopPadding(scrollBehavior)
    val listStartPadding = spacing.screenHorizontal
    val listEndPadding = spacing.screenHorizontal

    InstalledAppsPermissionRequest(
        needsPermission = uiState.needsMiuiPermission,
        onGranted = viewModel::onPermissionResult,
    )

    AccessControlAppListScrollToTopEffect(
        listState = mainListState,
        sortMode = uiState.sortMode,
        selectedFirst = uiState.selectedFirst,
        initialSelectedPackages = uiState.initialSelectedPackages,
        isLoading = uiState.isLoading,
    )

    Box(modifier = Modifier.fillMaxSize()) {
        AccessControlAppListScaffold(
            search = search, scrollBehavior = scrollBehavior, uiState = uiState,
            viewModel = viewModel, backdrop = backdrop, topPadding = dynamicTopPadding,
            apps = filteredApps, listState = mainListState,
        )

        AccessControlAppListSearch(
            search = search,
            query = uiState.searchQuery,
            onQueryChange = viewModel::onSearchQueryChange,
            hasResults = filteredApps.isNotEmpty(),
            topPadding = dynamicTopPadding,
            startPadding = listStartPadding,
            endPadding = listEndPadding,
            bottomPadding = mainLikePadding.calculateBottomPadding(),
            apps = filteredApps,
            uiState = uiState,
            viewModel = viewModel,
        )
    }
}

/** The page scaffold: the animated top bar and the loading-or-list body beneath it. */
@Composable
private fun AccessControlAppListScaffold(
    search: SearchPagerState,
    scrollBehavior: ScrollBehavior,
    uiState: AccessControlViewModel.UiState,
    viewModel: AccessControlViewModel,
    backdrop: LayerBackdrop?,
    topPadding: Dp,
    apps: List<AccessControlViewModel.AppInfo>,
    listState: LazyListState,
) {
    val startPadding = AppTheme.spacing.screenHorizontal
    val endPadding = AppTheme.spacing.screenHorizontal

    Scaffold(
        topBar = {
            AccessControlAppListTopBar(
                search = search,
                scrollBehavior = scrollBehavior,
                uiState = uiState,
                viewModel = viewModel,
                backdrop = backdrop,
                topPadding = topPadding,
                startPadding = startPadding,
                endPadding = endPadding,
            )
        },
    ) { innerPadding ->
        val combinedInnerPadding = standalonePageInnerPadding(innerPadding)

        if (uiState.isLoading) {
            AccessControlAppListLoading(
                topPadding = combinedInnerPadding.calculateTopPadding(),
                bottomPadding = combinedInnerPadding.calculateBottomPadding(),
                startPadding = startPadding,
                endPadding = endPadding,
            )
        } else if (search.keepsPageMounted) {
            // Render the list during COLLAPSING too (not just at the COLLAPSED end state) so it
            // is already settled underneath the fading search overlay — otherwise it pops in on
            // the final frame and looks like a refresh flash.
            AccessControlAppListContent(
                scrollBehavior = scrollBehavior,
                innerPadding = combinedInnerPadding,
                listState = listState,
                startPadding = startPadding,
                endPadding = endPadding,
                apps = apps,
                uiState = uiState,
                viewModel = viewModel,
            )
        }
    }
}

/** The animated top bar: title, the two glass top actions and the collapsed search bar. */
@Composable
private fun AccessControlAppListTopBar(
    search: SearchPagerState,
    scrollBehavior: ScrollBehavior,
    uiState: AccessControlViewModel.UiState,
    viewModel: AccessControlViewModel,
    backdrop: LayerBackdrop?,
    topPadding: Dp,
    startPadding: Dp,
    endPadding: Dp,
) {
    search.TopAppBarAnim {
        TopBar(
            title = YumeTxt.AccessControl.AppList.ScreenTitle,
            scrollBehavior = scrollBehavior,
            actions = {
                AccessControlAppListTopActions(
                    uiState = uiState,
                    viewModel = viewModel,
                    backdrop = backdrop,
                )
            },
            bottomContent = {
                search.CollapsedBar(
                    label = YumeTxt.AccessControl.Search.Placeholder,
                    topPadding = topPadding,
                    startPadding = startPadding,
                    endPadding = endPadding,
                )
            },
        )
    }
}

/**
 * Sort and batch-operation buttons, sharing one glass pill.
 *
 * The two popup flags and their anchors belong to the menus they open, so they are remembered here
 * instead of by the screen.
 */
@Composable
private fun AccessControlAppListTopActions(
    uiState: AccessControlViewModel.UiState,
    viewModel: AccessControlViewModel,
    backdrop: LayerBackdrop?,
) {
    var showActionsMenu by remember { mutableStateOf(false) }
    val actionsMenuAnchor = rememberGlassPopupAnchor()
    var showSortMenu by remember { mutableStateOf(false) }
    val sortMenuAnchor = rememberGlassPopupAnchor()

    // 与节点页一致：剩余两个操作收进同一枚玻璃胶囊。
    GlassTopActionPill(
        actions =
            listOf(
                GlassTopAction(
                    icon = Yume.ListChevronsUpDown,
                    contentDescription = YumeTxt.AccessControl.Settings.SortMode,
                    onClick = { showSortMenu = !showSortMenu },
                    anchor = sortMenuAnchor,
                    popup = {
                        AccessControlSortMenu(
                            show = showSortMenu,
                            onDismiss = { showSortMenu = false },
                            sortMode = uiState.sortMode,
                            onSortSelected = viewModel::onSortModeChange,
                            anchor = sortMenuAnchor,
                        )
                    },
                ),
                GlassTopAction(
                    icon = Yume.Bolt,
                    contentDescription = YumeTxt.AccessControl.Settings.Title,
                    onClick = { showActionsMenu = !showActionsMenu },
                    anchor = actionsMenuAnchor,
                    popup = {
                        AccessControlOperationsMenu(
                            showPopup = showActionsMenu,
                            onShowPopupChange = { showActionsMenu = it },
                            anchor = actionsMenuAnchor,
                            uiState = uiState,
                            actions =
                                remember(viewModel) {
                                    AccessControlMenuActions(
                                        onShowSystemAppsChange =
                                            viewModel::onShowSystemAppsChange,
                                        onSelectedFirstChange = viewModel::onSelectedFirstChange,
                                        onSelectAll = viewModel::selectAll,
                                        onDeselectAll = viewModel::deselectAll,
                                        onInvertSelection = viewModel::invertSelection,
                                        onSelectChinaApps = viewModel::selectChinaAppsInCurrentList,
                                        onSelectNonChinaApps =
                                            viewModel::selectNonChinaAppsInCurrentList,
                                    )
                                },
                        )
                    },
                ),
            ),
        backdrop = backdrop,
    )
}

/** Re-anchors the main list at its top whenever the row order changes. */
@Composable
private fun AccessControlAppListScrollToTopEffect(
    listState: LazyListState,
    sortMode: AccessControlSortMode,
    selectedFirst: Boolean,
    initialSelectedPackages: Set<String>,
    isLoading: Boolean,
) {
    // Jump the main list back to the top whenever the order changes (sort mode, selected-first,
    // or re-entry refresh of initial selection). LazyColumn keeps the previous viewport anchor
    // when keyed items reorder, so without this the selected-first rows can be at the top while
    LaunchedEffect(sortMode, selectedFirst, initialSelectedPackages, isLoading) {
        if (isLoading) return@LaunchedEffect
        listState.scrollToItem(0)
        // LazyColumn may re-apply its key-based scroll anchor on the next layout pass.
        withFrameNanos {}
        listState
            .takeUnless { it.firstVisibleItemIndex == 0 && it.firstVisibleItemScrollOffset == 0 }
            ?.scrollToItem(0)
    }
}

/** Centered progress indicator filling the page area while the app list is being read. */
@Composable
private fun AccessControlAppListLoading(
    topPadding: Dp,
    bottomPadding: Dp,
    startPadding: Dp,
    endPadding: Dp,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(
                    top = topPadding,
                    start = startPadding,
                    end = endPadding,
                    bottom = bottomPadding,
                ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        InfiniteProgressIndicator(color = MiuixTheme.colorScheme.onSurface)
    }
}

/** The paged row list, scrolled by the page's own scroll behavior. */
@Composable
private fun AccessControlAppListContent(
    scrollBehavior: ScrollBehavior,
    innerPadding: PaddingValues,
    listState: LazyListState,
    startPadding: Dp,
    endPadding: Dp,
    apps: List<AccessControlViewModel.AppInfo>,
    uiState: AccessControlViewModel.UiState,
    viewModel: AccessControlViewModel,
) {
    val spacing = AppTheme.spacing

    ScreenLazyColumn(
        scrollBehavior = scrollBehavior,
        innerPadding = innerPadding,
        lazyListState = listState,
        contentPadding =
            PaddingValues(
                top = innerPadding.calculateTopPadding() + spacing.space6,
                bottom = innerPadding.calculateBottomPadding(),
                start = startPadding,
                end = endPadding,
            ),
    ) {
        accessControlAppItems(apps, uiState, viewModel)
    }
}

/** The search overlay and the result list it reveals, both driven by the caller's query. */
@Composable
private fun AccessControlAppListSearch(
    search: SearchPagerState,
    query: String,
    onQueryChange: (String) -> Unit,
    hasResults: Boolean,
    topPadding: Dp,
    startPadding: Dp,
    endPadding: Dp,
    bottomPadding: Dp,
    apps: List<AccessControlViewModel.AppInfo>,
    uiState: AccessControlViewModel.UiState,
    viewModel: AccessControlViewModel,
) {
    search.SearchPager(
        label = YumeTxt.AccessControl.Search.Placeholder,
        query = query,
        onQueryChange = onQueryChange,
        hasResults = hasResults,
        padding =
            SearchBarPadding(
                top = topPadding,
                start = startPadding,
                end = endPadding,
            ),
        emptyResult = {
            AccessControlSearchEmptyState(
                text = YumeTxt.AccessControl.Search.Empty,
                modifier = Modifier.padding(bottom = bottomPadding),
            )
        },
    ) {
        search.SearchResults(
            query = query,
            start = startPadding,
            end = endPadding,
            bottom = bottomPadding,
        ) {
            accessControlAppItems(apps, uiState, viewModel)
        }
    }
}
