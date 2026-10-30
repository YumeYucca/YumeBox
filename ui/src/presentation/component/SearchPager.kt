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

package com.github.yumeyucca.yumebox.presentation.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.takeOrElse
import androidx.compose.ui.zIndex
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Search
import top.yukonga.miuix.kmp.icon.basic.SearchCleanup
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

/**
 * Search-bar padding trio threaded from the screen; unspecified fields fall back to theme values.
 */
data class SearchBarPadding(
    val top: Dp = Dp.Unspecified,
    val start: Dp = Dp.Unspecified,
    val end: Dp = Dp.Unspecified,
)

/**
 * Presentation state of one screen's search overlay.
 *
 * The bar is *controlled*: the query stays where the screen already keeps it (a `ViewModel` or a
 * `rememberSaveable`), and this class only owns what no caller can know from the query itself — the
 * expand/collapse phase and where the collapsed bar sits inside the window, which is what the
 * overlay animates from.
 *
 * The previous model was a `SearchStatus` data class copied by the screen on every keystroke and
 * every animation tick; because the phase lived in the screen and the query lived in the
 * `ViewModel`, a `rememberSyncedSearchStatus` adapter had to write each value back to the other,
 * and each screen re-derived the same result status by hand.
 */
@Stable
class SearchPagerState internal constructor() {
    internal var phase by mutableStateOf(SearchPagerPhase.Collapsed)
        private set

    internal var collapsedBarTop by mutableStateOf(UiDp.dp0)
        private set

    /**
     * Whether the page content and the collapsed bar belong in composition. True while collapsing so
     * the list is already in place underneath the fading overlay.
     */
    val keepsPageMounted: Boolean
        get() = phase == SearchPagerPhase.Collapsed || phase == SearchPagerPhase.Collapsing

    val isCollapsed: Boolean
        get() = phase == SearchPagerPhase.Collapsed

    /** Whether the overlay is on screen at all — expanding, expanded or collapsing. */
    val showsOverlay: Boolean
        get() = phase != SearchPagerPhase.Collapsed

    /** Whether the overlay is expanded or on its way there, i.e. the results are actionable. */
    val wantsOverlay: Boolean
        get() = phase == SearchPagerPhase.Expanded || phase == SearchPagerPhase.Expanding

    /** Opens the overlay from the collapsed bar. */
    fun expand() {
        if (keepsPageMounted) phase = SearchPagerPhase.Expanding
    }

    /** Starts closing the overlay, from a gesture, a Back press or a navigation request. */
    fun collapse() {
        if (wantsOverlay) phase = SearchPagerPhase.Collapsing
    }

    internal fun reportCollapsedBarTop(top: Dp) {
        if (collapsedBarTop != top) collapsedBarTop = top
    }

    /** Settles the running transition; returns true when the overlay has finished collapsing. */
    internal fun settle(): Boolean =
        when (phase) {
            SearchPagerPhase.Expanding -> {
                phase = SearchPagerPhase.Expanded
                false
            }

            SearchPagerPhase.Collapsing -> {
                phase = SearchPagerPhase.Collapsed
                true
            }

            else -> false
        }
}

internal enum class SearchPagerPhase {
    Expanded,
    Expanding,
    Collapsed,
    Collapsing,
}

internal enum class SearchResultStatus {
    DEFAULT,
    EMPTY,
    SHOW,
}

@Composable
fun rememberSearchPagerState(): SearchPagerState = remember { SearchPagerState() }

/**
 * Collapsed search bar for a Scaffold top bar.
 *
 * Owns the three things every screen used to repeat around this bar: fading it out while the
 * overlay is open, reporting its window position so the overlay can animate from it, and turning a
 * tap into [SearchPagerState.expand].
 */
@Composable
fun SearchPagerState.CollapsedBar(
    label: String,
    topPadding: Dp,
    startPadding: Dp,
    endPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val spacing = AppTheme.spacing
    val density = LocalDensity.current
    Box(
        modifier =
            modifier
                .alpha(if (keepsPageMounted) 1f else 0f)
                .onGloballyPositioned { coordinates ->
                    reportCollapsedBarTop(
                        with(density) { coordinates.positionInWindow().y.toDp() }
                    )
                }
                .then(
                    if (isCollapsed) {
                        Modifier.pointerInput(phase) { detectTapGestures { expand() } }
                    } else {
                        Modifier
                    }
                )
    ) {
        InputField(
            query = "",
            onQueryChange = {},
            label = label,
            leadingIcon = {
                Icon(
                    imageVector = MiuixIcons.Basic.Search,
                    contentDescription = label,
                    modifier =
                        Modifier
                            .size(AppTheme.sizes.searchIconTouchTarget)
                            .padding(start = spacing.space16, end = spacing.space8),
                    tint = colorScheme.onSurfaceVariantSummary,
                )
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = startPadding, end = endPadding)
                    .padding(top = topPadding, bottom = AppTheme.sizes.searchBarBottomPadding),
            onSearch = {},
            enabled = false,
            expanded = false,
            onExpandedChange = {},
        )
    }
}

@Composable
fun SearchPagerState.TopAppBarAnim(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val visible = keepsPageMounted
    val alpha by
    animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(if (visible) 550 else 0, easing = FastOutSlowInEasing),
        label = "SearchTopBarAlpha",
    )
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(colorScheme.surface)
                .alpha(alpha)
    ) {
        content()
    }
}

/**
 * The list an overlay shows, with the two rules every result list needs:
 *
 * - a new query starts the list over — a filtered list whose first match is scrolled out of sight
 *   reads as empty, and the overlay just opened on top of it;
 * - the last row stays clear of the keyboard, on top of whatever the page already reserves.
 *
 * [app/src/screen/log/LogScreen.kt] keeps its own list on purpose: live entries arrive at the head
 * and that screen re-anchors on the newest one instead of jumping to the top.
 */
@Composable
fun SearchPagerState.SearchResults(
    query: String,
    start: Dp,
    end: Dp,
    bottom: Dp,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    content: LazyListScope.() -> Unit,
) {
    val spacing = AppTheme.spacing
    val imeBottomPadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()

    LaunchedEffect(query) {
        if (query.isNotBlank()) state.scrollToItem(0)
    }

    LazyColumn(
        state = state,
        modifier = modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                start = start,
                end = end,
                top = spacing.space6,
                bottom = maxOf(bottom, imeBottomPadding),
            ),
        content = content,
    )
}

/**
 * Expandable search overlay. Callers must keep this composed always (do not `if (!isCollapsed)`) so
 * expand/collapse animations stay continuous.
 *
 * When settled collapsed it is zero-size, without zIndex or pointer input, so it never covers the
 * page. While expanding / expanded / collapsing it is a full-screen dim plus the bar.
 *
 * Must be placed as a **full-window sibling** of the page `Scaffold`, not inside its content —
 * `topPadding` animates relative to the window top (status bars), matching the collapsed bar's
 * reported position.
 *
 * `query`/`onQueryChange` are the caller's own search text: the overlay never holds a second copy,
 * so a locale switch, a restored state or a `ViewModel` update all reach the field unchanged.
 * `hasResults` is the caller's filtered result — the empty state is picked here instead of by each
 * screen deriving a status from the same two values.
 */
@Composable
fun SearchPagerState.SearchPager(
    label: String,
    query: String,
    onQueryChange: (String) -> Unit,
    hasResults: Boolean,
    defaultResult: @Composable () -> Unit = {},
    emptyResult: @Composable () -> Unit = {},
    padding: SearchBarPadding = SearchBarPadding(),
    result: @Composable () -> Unit,
) {
    val spacing = AppTheme.spacing
    val componentSizes = AppTheme.sizes
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val resolvedPadding =
        SearchBarPadding(
            top = padding.top.takeOrElse { componentSizes.searchBarTopPadding },
            start = padding.start.takeOrElse { spacing.space0 },
            end = padding.end.takeOrElse { spacing.space0 },
        )
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()

    fun dismissSearch() {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        onQueryChange("")
        collapse()
    }

    val topPadding by
    animateDpAsState(
        targetValue =
            if (wantsOverlay) {
                systemBarsPadding + componentSizes.listItemVerticalMinimal
            } else {
                max(collapsedBarTop, spacing.space0)
            },
        animationSpec = tween(300, easing = LinearOutSlowInEasing),
        label = "SearchTopPadding",
        finishedListener = { if (settle()) onQueryChange("") },
    )
    val overlayAlpha by
    animateFloatAsState(
        targetValue = if (wantsOverlay) 1f else 0f,
        animationSpec = tween(300, easing = LinearOutSlowInEasing),
        label = "SearchOverlayAlpha",
    )

    BackHandler(enabled = showsOverlay) { dismissSearch() }

    // Never early-return: remounting drops the animate* state and the bar "flies".
    // Collapsed: size 0, no elevation. Active: full-screen overlay above the page.
    Column(
        modifier =
            if (showsOverlay) {
                Modifier
                    .fillMaxSize()
                    .zIndex(5f)
                    .background(colorScheme.surface.copy(alpha = overlayAlpha))
                    .pointerInput(phase) {}
            } else {
                Modifier.size(UiDp.dp0)
            }
    ) {
        if (showsOverlay) {
            SearchPagerTopRow(
                state = this@SearchPager,
                query = query,
                onQueryChange = onQueryChange,
                onDismiss = ::dismissSearch,
                topPadding = topPadding,
                barPadding = resolvedPadding,
                label = label,
            )
            SearchPagerResultsLayer(
                visible = phase == SearchPagerPhase.Expanded,
                resultStatus =
                    when {
                        query.isBlank() -> SearchResultStatus.DEFAULT
                        !hasResults -> SearchResultStatus.EMPTY
                        else -> SearchResultStatus.SHOW
                    },
                defaultResult = defaultResult,
                emptyResult = emptyResult,
                result = result,
            )
        }
    }
}

@Composable
private fun SearchPagerTopRow(
    state: SearchPagerState,
    query: String,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    topPadding: Dp,
    barPadding: SearchBarPadding,
    label: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = topPadding)
                .background(colorScheme.surface),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SearchBar(
            query = query,
            onQueryChange = onQueryChange,
            onDismiss = onDismiss,
            label = label,
            wantsOverlay = state.wantsOverlay,
            padding = barPadding,
            modifier = Modifier.weight(1f),
        )
        SearchPagerCancelButton(
            visible = state.wantsOverlay,
            clickable = state.phase == SearchPagerPhase.Expanded,
            onDismiss = onDismiss,
            searchBarTopPadding = barPadding.top,
        )
    }
}

@Composable
private fun SearchPagerCancelButton(
    visible: Boolean,
    clickable: Boolean,
    onDismiss: () -> Unit,
    searchBarTopPadding: Dp,
) {
    val spacing = AppTheme.spacing
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandHorizontally(),
        exit = fadeOut() + shrinkHorizontally(),
    ) {
        Text(
            text = YumeTxt.Component.Button.Cancel,
            fontWeight = FontWeight.Bold,
            color = colorScheme.primary,
            modifier =
                Modifier
                    .padding(
                        start = spacing.space4,
                        end = spacing.space16,
                        top = searchBarTopPadding,
                        bottom = spacing.space6,
                    )
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        enabled = clickable,
                        onClick = onDismiss,
                    ),
        )
    }
}

@Composable
private fun SearchPagerResultsLayer(
    visible: Boolean,
    resultStatus: SearchResultStatus,
    defaultResult: @Composable () -> Unit,
    emptyResult: @Composable () -> Unit,
    result: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        modifier =
            Modifier
                .fillMaxSize()
                .zIndex(1f),
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        when (resultStatus) {
            SearchResultStatus.DEFAULT -> defaultResult()
            SearchResultStatus.EMPTY -> emptyResult()
            SearchResultStatus.SHOW -> result()
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    label: String,
    wantsOverlay: Boolean,
    padding: SearchBarPadding,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    InputField(
        query = query,
        onQueryChange = onQueryChange,
        label = label,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(start = padding.start, end = padding.end)
                .padding(top = padding.top, bottom = AppTheme.sizes.searchBarBottomPadding)
                .heightIn(min = AppTheme.sizes.searchFieldMinHeight),
        leadingIcon = { SearchBarLeadingIcon() },
        trailingIcon = {
            SearchBarClearButton(searchText = query, onClear = { onQueryChange("") })
        },
        onSearch = {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        },
        expanded = wantsOverlay,
        onExpandedChange = { expanded -> if (!expanded && wantsOverlay) onDismiss() },
    )
}

@Composable
private fun SearchBarLeadingIcon() {
    Icon(
        imageVector = MiuixIcons.Basic.Search,
        contentDescription = YumeTxt.Component.Editor.Action.Search,
        modifier =
            Modifier
                .size(AppTheme.sizes.searchIconTouchTarget)
                .padding(start = AppTheme.spacing.space16, end = AppTheme.spacing.space8),
        tint = colorScheme.onSurfaceVariantSummary,
    )
}

@Composable
private fun SearchBarClearButton(
    searchText: String,
    onClear: () -> Unit,
) {
    AnimatedVisibility(
        visible = searchText.isNotEmpty(),
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
    ) {
        Icon(
            imageVector = MiuixIcons.Basic.SearchCleanup,
            contentDescription = YumeTxt.Component.Button.Clear,
            tint = colorScheme.onSurface,
            modifier =
                Modifier
                    .size(AppTheme.sizes.searchIconTouchTarget)
                    .padding(start = AppTheme.spacing.space8, end = AppTheme.spacing.space16)
                    .clickable(interactionSource = null, indication = null, onClick = onClear),
        )
    }
}
