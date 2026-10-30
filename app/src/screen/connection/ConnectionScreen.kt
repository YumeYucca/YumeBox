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

@file:Suppress("DuplicatedCode", "FunctionName")

package com.github.yumeyucca.yumebox.screen.connection


import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.github.yumeyucca.yumebox.core.model.ConnectionInfo
import com.github.yumeyucca.yumebox.feature.meta.presentation.component.ConnectionCard
import com.github.yumeyucca.yumebox.feature.meta.presentation.viewmodel.ConnectionTab
import com.github.yumeyucca.yumebox.feature.meta.presentation.viewmodel.ConnectionViewModel
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.navigation.Route
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import org.koin.androidx.compose.koinViewModel
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.github.yumeyucca.yumebox.presentation.component.rememberCollapsedTopPadding
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.ScrollBehavior

/** Tab label, listed in [ConnectionTab] order; the segmented row indexes tabs by ordinal. */
private val ConnectionTab.label: String
    get() =
        when (this) {
            ConnectionTab.ACTIVE -> YumeTxt.Connection.Tab.Active
            ConnectionTab.CLOSED -> YumeTxt.Connection.Tab.Closed
        }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConnectionScreen(navigator: Navigator) {
    val viewModel = koinViewModel<ConnectionViewModel>()
    val screen by viewModel.screenState.collectAsState()
    val search = rememberSearchPagerState()
    val scrollBehavior = MiuixScrollBehavior()
    val mainListState = rememberLazyListState()
    val mainLikePadding = rememberStandalonePageMainPadding()

    LaunchedEffect(Unit) { viewModel.startPolling() }

    fun openConnectionDetail(connection: ConnectionInfo) {
        // Collapse search first so the expand gesture never races the detail push.
        search.collapse()
        ConnectionDetailHolder.setup(info = connection)
        navigator.push(
            Route.ConnectionDetail(
                connectionId = connection.id,
                canInterrupt = screen.state.selectedTab == ConnectionTab.ACTIVE,
            )
        )
    }

    /*
     * Layering (AccessControl search pattern — no sheet sibling anymore):
     *
     *   Box (full window)
     *   ├─ Scaffold
     *   │    topBar: collapsed search chrome
     *   │    content: connection list
     *   └─ SearchPager                    ← full-window sibling (window-top offsetY animation)
     *
     * SearchPager is always composed; when settled collapsed it is size-0 and not a hit target.
     * Detail is a real navigation page so it never competes with search expand.
     */
    Box(modifier = Modifier.fillMaxSize()) {
        ConnectionListScaffold(
            screen = screen,
            search = search,
            scrollBehavior = scrollBehavior,
            listState = mainListState,
            onTabSelected = { viewModel.setTab(it) },
            onOpenConnection = { openConnectionDetail(it) },
        )

        ConnectionSearchOverlay(
            screen = screen,
            search = search,
            scrollBehavior = scrollBehavior,
            bottomPadding = mainLikePadding.calculateBottomPadding(),
            onQueryChange = viewModel::setSearchQuery,
            onOpenConnection = { openConnectionDetail(it) },
        )
    }
}

/** Collapsed-search chrome plus the connection list itself. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConnectionListScaffold(
    screen: ConnectionViewModel.ConnectionScreenState,
    search: SearchPagerState,
    scrollBehavior: ScrollBehavior,
    listState: LazyListState,
    onTabSelected: (ConnectionTab) -> Unit,
    onOpenConnection: (ConnectionInfo) -> Unit,
) {
    val spacing = AppTheme.spacing
    val listStartPadding = spacing.screenHorizontal
    val listEndPadding = spacing.screenHorizontal
    val tabs = ConnectionTab.entries.map { it.label }
    val emptyText =
        when {
            screen.state.isLoading -> YumeTxt.Connection.Loading
            screen.state.searchQuery.isNotEmpty() -> YumeTxt.Connection.NoResults
            else -> YumeTxt.Connection.Empty
        }
    val dynamicTopPadding by rememberCollapsedTopPadding(scrollBehavior)

    GlassScaffold(
        topBar = {
            search.TopAppBarAnim {
                TopBar(
                    title = YumeTxt.Connection.Title,
                    scrollBehavior = scrollBehavior,
                    bottomContent = {
                        search.CollapsedBar(
                            label = YumeTxt.Connection.SearchHint,
                            topPadding = dynamicTopPadding,
                            startPadding = listStartPadding,
                            endPadding = listEndPadding,
                        )
                    },
                )
            }
        }
    ) { innerPadding ->
        // Keep list during COLLAPSING so it is already under the fading overlay.
        if (search.keepsPageMounted) {
            val combinedInnerPadding = standalonePageInnerPadding(innerPadding)
            ScreenLazyColumn(
                scrollBehavior = scrollBehavior,
                innerPadding = combinedInnerPadding,
                lazyListState = listState,
                contentPadding =
                    PaddingValues(
                        start = listStartPadding,
                        end = listEndPadding,
                        top = combinedInnerPadding.calculateTopPadding(),
                        bottom = combinedInnerPadding.calculateBottomPadding() + spacing.space12,
                    ),
            ) {
                stickySegmentedTabs(
                    key = "__connection_tabs__",
                    tabs = tabs,
                    selectedTabIndex = screen.state.selectedTab.ordinal,
                    onTabSelected = { index -> onTabSelected(ConnectionTab.entries[index]) },
                    tabRowPadding = UiDp.dp0,
                )

                if (screen.filteredConnections.isEmpty()) {
                    item {
                        ConnectionEmptyText(
                            text = emptyText,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    connectionItems(screen.filteredConnections, onOpenConnection)
                }
            }
        }
    }
}

/** Full-window search page; only meaningful while [search] is expanded. */
@Composable
private fun ConnectionSearchOverlay(
    screen: ConnectionViewModel.ConnectionScreenState,
    search: SearchPagerState,
    scrollBehavior: ScrollBehavior,
    bottomPadding: Dp,
    onQueryChange: (String) -> Unit,
    onOpenConnection: (ConnectionInfo) -> Unit,
) {
    val spacing = AppTheme.spacing
    val listStartPadding = spacing.screenHorizontal
    val listEndPadding = spacing.screenHorizontal
    val dynamicTopPadding by rememberCollapsedTopPadding(scrollBehavior)

    search.SearchPager(
        label = YumeTxt.Connection.SearchHint,
        query = screen.state.searchQuery,
        onQueryChange = onQueryChange,
        hasResults = screen.filteredConnections.isNotEmpty(),
        padding =
            SearchBarPadding(
                top = dynamicTopPadding,
                start = listStartPadding,
                end = listEndPadding,
            ),
        emptyResult = {
            ConnectionEmptyText(
                text = YumeTxt.Connection.NoResults,
                modifier = Modifier.fillMaxSize().padding(bottom = bottomPadding),
            )
        },
    ) {
        search.SearchResults(
            query = screen.state.searchQuery,
            start = listStartPadding,
            end = listEndPadding,
            bottom = bottomPadding,
        ) {
            connectionItems(screen.filteredConnections, onOpenConnection)
        }
    }
}

/** One definition of a connection row's list identity and spacing. */
private fun LazyListScope.connectionItems(
    connections: List<ConnectionInfo>,
    onConnectionClick: (ConnectionInfo) -> Unit,
) {
    items(
        items = connections,
        key = { it.id },
        contentType = { "connection" },
    ) { connection ->
        ConnectionCard(
            connectionInfo = connection,
            onClick = { onConnectionClick(connection) },
            modifier = Modifier.padding(vertical = AppTheme.spacing.space6),
        )
    }
}

/** The slice's one quiet, centered empty-state line. */
@Composable
internal fun ConnectionEmptyText(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(AppTheme.spacing.space32),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}
