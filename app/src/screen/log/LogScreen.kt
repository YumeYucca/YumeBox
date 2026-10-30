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

@file:Suppress("DuplicatedCode", "FunctionName", "UsePropertyAccessSyntax")

package com.github.yumeyucca.yumebox.screen.log

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.github.yumeyucca.yumebox.common.util.toast
import com.github.yumeyucca.yumebox.core.model.LogMessage
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.glass.GlassPopupAnchor
import top.yukonga.miuix.kmp.glass.glassPopupAnchor
import top.yukonga.miuix.kmp.glass.glassPopupAnchorContent
import top.yukonga.miuix.kmp.glass.rememberGlassPopupAnchor
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Filter
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import com.github.yumeyucca.yumebox.presentation.component.rememberCollapsedTopPadding
import top.yukonga.miuix.kmp.basic.ScrollBehavior

private val LogLevelFilter.label: String
    get() =
        when (this) {
            LogLevelFilter.All -> YumeTxt.Log.Level.All
            LogLevelFilter.Debug -> YumeTxt.Log.Level.Debug
            LogLevelFilter.Info -> YumeTxt.Log.Level.Info
            LogLevelFilter.Warning -> YumeTxt.Log.Level.Warning
            LogLevelFilter.Error -> YumeTxt.Log.Level.Error
        }

@Composable
private fun LogLevelFilterMenu(
    show: Boolean,
    onDismiss: () -> Unit,
    levelFilter: LogLevelFilter,
    onSelect: (LogLevelFilter) -> Unit,
    anchor: GlassPopupAnchor,
) {
    GlassPopupHost(
        show = show,
        onDismissRequest = onDismiss,
        anchor = anchor,
        anchorContent = { GlassTopActionAnchorIcon(MiuixIcons.Filter) },
    ) {
        LogLevelFilter.entries.forEach { filter ->
            GlassMenuItem(
                text = filter.label,
                selected = filter == levelFilter,
                onClick = {
                    if (filter != levelFilter) {
                        onSelect(filter)
                    }
                    onDismiss()
                },
            )
        }
    }
}

@Composable
fun LogScreen(navigator: Navigator) {
    val viewModel = koinViewModel<LogViewModel>()
    val scrollBehavior = MiuixScrollBehavior()
    val spacing = AppTheme.spacing

    val screen by viewModel.screenState.collectAsState()
    val mainLikePadding = rememberStandalonePageMainPadding()

    val listState = rememberLazyListState()
    val searchListState = rememberLazyListState()
    val followLatest = rememberFollowsNewest(listState)
    val followLatestInSearch = rememberFollowsNewest(searchListState)
    var showLevelPopup by remember { mutableStateOf(false) }
    val levelFilterAnchor = rememberGlassPopupAnchor()
    val search = rememberSearchPagerState()
    val dynamicTopPadding by rememberCollapsedTopPadding(scrollBehavior)
    val listStartPadding = spacing.screenHorizontal
    val listEndPadding = spacing.screenHorizontal

    LogFollowNewestEffect(
        entries = screen.filteredEntries, search = search,
        listState = listState, searchListState = searchListState,
        followLatest = followLatest, followLatestInSearch = followLatestInSearch,
    )

    LaunchedEffect(Unit) { viewModel.connect() }

    Box(modifier = Modifier.fillMaxSize()) {
        GlassScaffold(
            topBar = {
                LogTopBar(
                    search = search, scrollBehavior = scrollBehavior,
                    dynamicTopPadding = dynamicTopPadding, startPadding = listStartPadding,
                    endPadding = listEndPadding, levelFilter = screen.levelFilter,
                    onLevelFilterSelected = viewModel::setLevelFilter,
                    levelFilterAnchor = levelFilterAnchor, showLevelPopup = showLevelPopup,
                    onShowLevelPopupChange = { showLevelPopup = it },
                )
            }
        ) { innerPadding ->
            LogPageContent(
                search = search, scrollBehavior = scrollBehavior,
                innerPadding = innerPadding, listState = listState,
                screen = screen,
            )
        }

        // Always composed — same as AccessControl. Do not gate with !isCollapsed().
        LogSearchOverlay(
            search = search, screen = screen,
            onQueryChange = viewModel::setSearchQuery, searchListState = searchListState,
            topPadding = dynamicTopPadding,
            bottomPadding = mainLikePadding.calculateBottomPadding(),
        )
    }
}

/** The page body behind the top bar: the mounted list and the hint that explains an empty one. */
@Composable
private fun LogPageContent(
    search: SearchPagerState,
    scrollBehavior: ScrollBehavior,
    innerPadding: PaddingValues,
    listState: LazyListState,
    screen: LogViewModel.LogScreenState,
) {
    val context = LocalContext.current
    if (search.keepsPageMounted) {
        Box(modifier = Modifier.fillMaxSize()) {
            StandalonePageList(
                scrollBehavior = scrollBehavior,
                scaffoldPadding = innerPadding,
                lazyListState = listState,
            ) {
                logEntryItems(screen.filteredEntries) { copyLogEntry(context, it) }
            }

            if (screen.filteredEntries.isEmpty()) {
                LogEmptyHint(
                    connectionState = screen.connectionState,
                    levelFilter = screen.levelFilter,
                )
            }
        }
    }
}

/** The search overlay: the bar that fades in and the entries the query matches. */
@Composable
private fun LogSearchOverlay(
    search: SearchPagerState,
    screen: LogViewModel.LogScreenState,
    onQueryChange: (String) -> Unit,
    searchListState: LazyListState,
    topPadding: Dp,
    bottomPadding: Dp,
) {
    val spacing = AppTheme.spacing
    val context = LocalContext.current
    search.SearchPager(
        label = YumeTxt.Log.SearchHint,
        query = screen.searchQuery,
        onQueryChange = onQueryChange,
        hasResults = screen.filteredEntries.isNotEmpty(),
        padding =
            SearchBarPadding(
                top = topPadding,
                start = spacing.screenHorizontal,
                end = spacing.screenHorizontal,
            ),
        emptyResult = {
            CenteredText(
                firstLine = YumeTxt.Log.Empty.NoResults,
                secondLine = screen.searchQuery,
            )
        },
    ) {
        LazyColumn(
            state = searchListState,
            modifier = Modifier.fillMaxSize(),
            // Horizontal inset is owned by AppCard.horizontalPadding(); only vertical here.
            contentPadding =
                PaddingValues(
                    top = spacing.space6,
                    bottom = bottomPadding,
                ),
        ) {
            logEntryItems(screen.filteredEntries) { copyLogEntry(context, it) }
        }
    }
}

/**
 * The log page's top bar: the level filter action (with its glass popup) and the search field that
 * rides along with the collapsing bar.
 *
 * The filter icon is bound to [levelFilterAnchor] here and not at the call site because a
 * `GlassPopupAnchor` only works when the button and the popup agree on it — splitting the two
 * halves across a parameter list is how they drift apart.
 */
@Composable
private fun LogTopBar(
    search: SearchPagerState,
    scrollBehavior: ScrollBehavior,
    dynamicTopPadding: Dp,
    startPadding: Dp,
    endPadding: Dp,
    levelFilter: LogLevelFilter,
    onLevelFilterSelected: (LogLevelFilter) -> Unit,
    levelFilterAnchor: GlassPopupAnchor,
    showLevelPopup: Boolean,
    onShowLevelPopupChange: (Boolean) -> Unit,
) {
    search.TopAppBarAnim {
        TopBar(
            title = YumeTxt.Log.Title,
            scrollBehavior = scrollBehavior,
            actions = {
                Box(
                    modifier =
                        Modifier.glassPopupAnchor(
                            anchor = levelFilterAnchor,
                            cornerRadius = GlassTopActionDefaults.IconSize / 2,
                            floating = true,
                        ),
                ) {
                    IconButton(onClick = { onShowLevelPopupChange(true) }) {
                        Icon(
                            imageVector = MiuixIcons.Filter,
                            contentDescription = YumeTxt.Log.Level.Filter,
                            tint = MiuixTheme.colorScheme.onSurface,
                            modifier = Modifier.glassPopupAnchorContent(levelFilterAnchor),
                        )
                    }
                }
                LogLevelFilterMenu(
                    show = showLevelPopup,
                    onDismiss = { onShowLevelPopupChange(false) },
                    levelFilter = levelFilter,
                    onSelect = onLevelFilterSelected,
                    anchor = levelFilterAnchor,
                )
            },
            bottomContent = {
                search.CollapsedBar(
                    label = YumeTxt.Log.SearchHint,
                    topPadding = dynamicTopPadding,
                    startPadding = startPadding,
                    endPadding = endPadding,
                )
            },
        )
    }
}

/**
 * What the empty list means, in the reader's terms: a connection problem outranks an empty filter,
 * which outranks "nothing has been logged yet".
 */
@Composable
private fun LogEmptyHint(connectionState: LogConnectionState, levelFilter: LogLevelFilter) {
    val (firstLine, secondLine) =
        when {
            connectionState == LogConnectionState.Connecting ->
                YumeTxt.Log.Empty.Connecting to YumeTxt.Log.Empty.ConnectingHint

            connectionState == LogConnectionState.Retrying ->
                YumeTxt.Log.Empty.Retrying to YumeTxt.Log.Empty.RetryingHint

            levelFilter != LogLevelFilter.All -> YumeTxt.Log.Empty.NoMatch to levelFilter.label

            else -> YumeTxt.Log.Empty.NoLogs to YumeTxt.Log.Empty.LiveHint
        }
    CenteredText(firstLine = firstLine, secondLine = secondLine)
}

@Composable
private fun LogEntryRow(
    entry: LiveLogEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = AppTheme.spacing
    val semanticColors = AppTheme.colors
    val metaStyle =
        MiuixTheme.textStyles.body2.copy(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    val messageStyle =
        MiuixTheme.textStyles.body2.copy(fontSize = 12.sp, fontFamily = FontFamily.Monospace)

    val levelColor =
        when (entry.level) {
            LogMessage.Level.Debug -> semanticColors.logLevel.debug
            LogMessage.Level.Info -> MiuixTheme.colorScheme.primary
            LogMessage.Level.Warning -> semanticColors.logLevel.warning
            LogMessage.Level.Error -> semanticColors.logLevel.error
            LogMessage.Level.Silent -> semanticColors.logLevel.neutral
            LogMessage.Level.Unknown -> semanticColors.logLevel.neutral
        }

    AppCard(
        modifier = modifier.padding(vertical = spacing.space4),
        onClick = onClick,
        pressFeedbackType = PressFeedbackType.Sink,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.space12, vertical = spacing.space10)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.space8),
            ) {
                Text(
                    text = entry.time,
                    style = metaStyle,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Text(
                    text = entry.level.name.take(1),
                    style = metaStyle,
                    color = levelColor,
                )
            }
            Spacer(modifier = Modifier.size(spacing.space6))
            Text(
                text = entry.message,
                style = messageStyle,
                color = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** The one definition of a log row's list identity and entrance animation. */
private fun LazyListScope.logEntryItems(
    entries: List<LiveLogEntry>,
    onEntryClick: (LiveLogEntry) -> Unit,
) {
    itemsIndexed(
        items = entries,
        key = { _, item -> item.id },
        contentType = { _, _ -> "log" },
    ) { _, entry ->
        LogEntryRow(
            entry = entry,
            onClick = { onEntryClick(entry) },
            modifier =
                Modifier.animateItem(
                    // New head fades in; existing rows keep placement so they slide down as
                    // newer logs push from the top.
                    fadeInSpec = tween(160, easing = FastOutSlowInEasing),
                    placementSpec = tween(260, easing = FastOutSlowInEasing),
                    fadeOutSpec = tween(100),
                ),
        )
    }
}

private fun copyLogEntry(context: Context, entry: LiveLogEntry) {
    val text = "[${entry.time}] [${entry.level.name}] ${entry.message}"
    context
        .getSystemService(ClipboardManager::class.java)
        ?.setPrimaryClip(ClipData.newPlainText("YumeBox log", text))
    context.toast(YumeTxt.Log.Action.Copied)
}

/**
 * Pins the visible list back to its newest row while the reader is following it.
 *
 * The keys are the newest entry's id and the overlay phase, so a batch that lands while the list
 * sits scrolled and idle never re-pins it.
 */
@Composable
private fun LogFollowNewestEffect(
    entries: List<LiveLogEntry>,
    search: SearchPagerState,
    listState: LazyListState,
    searchListState: LazyListState,
    followLatest: State<Boolean>,
    followLatestInSearch: State<Boolean>,
) {
    LaunchedEffect(entries.firstOrNull()?.id, search.showsOverlay) {
        if (entries.isEmpty()) return@LaunchedEffect
        // Newest-first: LazyColumn anchors to the previous head key after prepend, so the
        // new rows land above the viewport. Pin back to 0 while following so placement
        // animation can push older rows downward (not a fade-only flash).
        if (search.keepsPageMounted && followLatest.value) {
            listState.requestScrollToItem(0)
        } else if (search.wantsOverlay && followLatestInSearch.value) {
            searchListState.requestScrollToItem(0)
        }
    }
}

/**
 * Whether [listState] is still parked on its newest row.
 *
 * The value only flips while the user scrolls or the list snaps back to the top, so a batch that
 * lands while the list sits scrolled and idle never re-pins it.
 */
@Composable
private fun rememberFollowsNewest(listState: LazyListState): State<Boolean> {
    val follows = remember { mutableStateOf(true) }
    LaunchedEffect(listState) {
        snapshotFlow {
            Triple(
                listState.isScrollInProgress,
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset,
            )
        }
            .collectLatest { (scrolling, index, offset) ->
                val atTop = index == 0 && offset == 0
                if (scrolling || atTop) follows.value = atTop
            }
    }
    return follows
}
