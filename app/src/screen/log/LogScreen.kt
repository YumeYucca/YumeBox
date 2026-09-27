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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.github.yumeyucca.yumebox.common.util.toast
import com.github.yumeyucca.yumebox.core.model.LogMessage
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.component.AppCard
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.Share
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Filter
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

private val LevelFilters =
    listOf(
        LogLevelFilter.All,
        LogLevelFilter.Debug,
        LogLevelFilter.Info,
        LogLevelFilter.Warning,
        LogLevelFilter.Error,
    )

private fun LogLevelFilter.displayName(): String =
    when (this) {
        LogLevelFilter.All -> YumeTxt.Log.Level.All
        LogLevelFilter.Debug -> YumeTxt.Log.Level.Debug
        LogLevelFilter.Info -> YumeTxt.Log.Level.Info
        LogLevelFilter.Warning -> YumeTxt.Log.Level.Warning
        LogLevelFilter.Error -> YumeTxt.Log.Level.Error
    }

@Composable
fun LogScreen(navigator: Navigator) {
    val density = LocalDensity.current
    val viewModel = koinViewModel<LogViewModel>()
    val scrollBehavior = MiuixScrollBehavior()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val spacing = AppTheme.spacing

    val screen by viewModel.screenState.collectAsState()
    val filteredEntries = screen.filteredEntries
    val levelFilter = screen.levelFilter
    val connectionState = screen.connectionState
    val mainLikePadding = rememberStandalonePageMainPadding()

    val listState = rememberLazyListState()
    val searchListState = rememberLazyListState()
    var followLatest by remember { mutableStateOf(true) }
    var followLatestInSearch by remember { mutableStateOf(true) }
    var showLevelPopup by remember { mutableStateOf(false) }
    val selectedLevelIndex =
        remember(levelFilter) { LevelFilters.indexOf(levelFilter).coerceAtLeast(0) }
    var searchStatus by remember { mutableStateOf(SearchStatus(label = YumeTxt.Log.SearchHint)) }
    val dynamicTopPadding by remember {
        derivedStateOf { spacing.space12 * (1f - scrollBehavior.state.collapsedFraction) }
    }
    val listStartPadding = spacing.screenHorizontal
    val listEndPadding = spacing.screenHorizontal
    val currentSearchStatus =
        remember(searchStatus, filteredEntries) {
            searchStatus.copy(
                resultStatus =
                    when {
                        searchStatus.searchText.isBlank() -> SearchStatus.ResultStatus.DEFAULT
                        filteredEntries.isEmpty() -> SearchStatus.ResultStatus.EMPTY
                        else -> SearchStatus.ResultStatus.SHOW
                    }
            )
        }

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
                if (scrolling || atTop) followLatest = atTop
            }
    }
    LaunchedEffect(searchListState) {
        snapshotFlow {
            Triple(
                searchListState.isScrollInProgress,
                searchListState.firstVisibleItemIndex,
                searchListState.firstVisibleItemScrollOffset,
            )
        }
            .collectLatest { (scrolling, index, offset) ->
                val atTop = index == 0 && offset == 0
                if (scrolling || atTop) followLatestInSearch = atTop
            }
    }
    LaunchedEffect(filteredEntries.firstOrNull()?.id, currentSearchStatus.current) {
        if (filteredEntries.isEmpty()) return@LaunchedEffect
        // Newest-first: LazyColumn anchors to the previous head key after prepend, so the
        // new rows land above the viewport. Pin back to 0 while following so placement
        // animation can push older rows downward (not a fade-only flash).
        if (currentSearchStatus.shouldCollapse() && followLatest) {
            listState.requestScrollToItem(0)
        } else if (currentSearchStatus.shouldExpand() && followLatestInSearch) {
            searchListState.requestScrollToItem(0)
        }
    }

    val saveFileLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("text/plain")
        ) { uri ->
            uri ?: return@rememberLauncherForActivityResult
            scope.launch(Dispatchers.IO) {
                val success = viewModel.export(uri)
                if (!success) {
                    launch(Dispatchers.Main) { context.toast(YumeTxt.Util.Error.UnknownError) }
                }
            }
        }

    LaunchedEffect(Unit) { viewModel.connect() }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                currentSearchStatus.TopAppBarAnim {
                    TopBar(
                        title = YumeTxt.Log.Title,
                        scrollBehavior = scrollBehavior,
                        actions = {
                            Box {
                                IconButton(
                                    modifier = Modifier.padding(end = spacing.space12),
                                    onClick = { showLevelPopup = true },
                                ) {
                                    Icon(
                                        imageVector = MiuixIcons.Filter,
                                        contentDescription = YumeTxt.Log.Level.Filter,
                                        tint = MiuixTheme.colorScheme.onSurface,
                                    )
                                }
                                OverlayListPopup(
                                    show = showLevelPopup,
                                    alignment = PopupPositionProvider.Align.BottomEnd,
                                    onDismissRequest = { showLevelPopup = false },
                                ) {
                                    ListPopupColumn {
                                        LevelFilters.forEachIndexed { index, filter ->
                                            DropdownImpl(
                                                text = filter.displayName(),
                                                optionSize = LevelFilters.size,
                                                isSelected = selectedLevelIndex == index,
                                                onSelectedIndexChange = {
                                                    if (filter != levelFilter) {
                                                        viewModel.setLevelFilter(filter)
                                                    }
                                                    showLevelPopup = false
                                                },
                                                index = index,
                                            )
                                        }
                                    }
                                }
                            }
                            if (filteredEntries.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        saveFileLauncher.launch(
                                            "log_${System.currentTimeMillis()}.txt"
                                        )
                                    }
                                ) {
                                    Icon(
                                        imageVector = Yume.Share,
                                        contentDescription = YumeTxt.Log.Action.Export,
                                    )
                                }
                            }
                        },
                        bottomContent = {
                            Box(
                                modifier =
                                    Modifier
                                        .alpha(
                                            if (currentSearchStatus.isCollapsed()) 1f else 0f
                                        )
                                        .onGloballyPositioned { coordinates ->
                                            with(density) {
                                                val offset = coordinates.positionInWindow().y.toDp()
                                                if (currentSearchStatus.offsetY != offset) {
                                                    searchStatus =
                                                        currentSearchStatus.copy(offsetY = offset)
                                                }
                                            }
                                        }
                                        .then(
                                            if (currentSearchStatus.isCollapsed()) {
                                                Modifier.pointerInput(currentSearchStatus.current) {
                                                    detectTapGestures {
                                                        searchStatus =
                                                            currentSearchStatus.copy(
                                                                current =
                                                                    SearchStatus.Status.EXPANDING
                                                            )
                                                    }
                                                }
                                            } else {
                                                Modifier
                                            }
                                        )
                            ) {
                                CollapsedSearchBar(
                                    label = currentSearchStatus.label,
                                    topPadding = dynamicTopPadding,
                                    startPadding = listStartPadding,
                                    endPadding = listEndPadding,
                                )
                            }
                        },
                    )
                }
            }
        ) { innerPadding ->
            if (currentSearchStatus.shouldCollapse()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    ScreenLazyColumn(
                        scrollBehavior = scrollBehavior,
                        innerPadding = combinePaddingValues(innerPadding, mainLikePadding),
                        lazyListState = listState,
                    ) {
                        itemsIndexed(
                            items = filteredEntries,
                            key = { _, item -> item.id },
                            contentType = { _, _ -> "log" },
                        ) { _, entry ->
                            LogEntryRow(
                                entry = entry,
                                onClick = { copyLogEntry(context, entry) },
                                modifier =
                                    Modifier.animateItem(
                                        // New head fades in; existing rows keep placement
                                        // so they slide down as newer logs push from top.
                                        fadeInSpec = tween(160, easing = FastOutSlowInEasing),
                                        placementSpec = tween(260, easing = FastOutSlowInEasing),
                                        fadeOutSpec = tween(100),
                                    ),
                            )
                        }
                    }

                    if (filteredEntries.isEmpty()) {
                        val (firstLine, secondLine) =
                            when {
                                connectionState == LogConnectionState.Connecting ->
                                    YumeTxt.Log.Empty.Connecting to YumeTxt.Log.Empty.ConnectingHint

                                connectionState == LogConnectionState.Retrying ->
                                    YumeTxt.Log.Empty.Retrying to YumeTxt.Log.Empty.RetryingHint

                                levelFilter != LogLevelFilter.All ->
                                    YumeTxt.Log.Empty.NoMatch to levelFilter.displayName()

                                else -> YumeTxt.Log.Empty.NoLogs to YumeTxt.Log.Empty.LiveHint
                            }
                        CenteredText(firstLine = firstLine, secondLine = secondLine)
                    }
                }
            }
        }

        // Always composed — same as AccessControl. Do not gate with !isCollapsed().
        currentSearchStatus.SearchPager(
            onSearchStatusChange = {
                searchStatus = it
                viewModel.setSearchQuery(it.searchText)
            },
            padding =
                SearchBarPadding(
                    top = dynamicTopPadding,
                    start = listStartPadding,
                    end = listEndPadding,
                ),
            emptyResult = {
                CenteredText(
                    firstLine = YumeTxt.Log.Empty.NoResults,
                    secondLine = currentSearchStatus.searchText,
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
                        bottom = mainLikePadding.calculateBottomPadding(),
                    ),
            ) {
                itemsIndexed(
                    items = filteredEntries,
                    key = { _, item -> item.id },
                    contentType = { _, _ -> "log" },
                ) { _, entry ->
                    LogEntryRow(
                        entry = entry,
                        onClick = { copyLogEntry(context, entry) },
                        modifier =
                            Modifier.animateItem(
                                fadeInSpec = tween(160, easing = FastOutSlowInEasing),
                                placementSpec = tween(260, easing = FastOutSlowInEasing),
                                fadeOutSpec = tween(100),
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun LogEntryRow(
    entry: LiveLogEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = AppTheme.spacing
    val semanticColors = AppTheme.colors

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
                    style =
                        MiuixTheme.textStyles.body2.copy(
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                        ),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Text(
                    text = entry.level.name.uppercase().take(1),
                    style =
                        MiuixTheme.textStyles.body2.copy(
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                        ),
                    color = levelColor,
                )
            }
            Spacer(modifier = Modifier.size(spacing.space6))
            Text(
                text = entry.message,
                style =
                    MiuixTheme.textStyles.body2.copy(
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                    ),
                color = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun copyLogEntry(context: Context, entry: LiveLogEntry) {
    val text = "[${entry.time}] [${entry.level.name}] ${entry.message}"
    context
        .getSystemService(ClipboardManager::class.java)
        ?.setPrimaryClip(ClipData.newPlainText("YumeBox log", text))
    context.toast(YumeTxt.Log.Action.Copied)
}
