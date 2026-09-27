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

package com.github.yumeyucca.yumebox.presentation.component



import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.takeOrElse
import androidx.compose.ui.zIndex
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.theme.Sizes
import com.github.yumeyucca.yumebox.presentation.theme.Spacing
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
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

@Composable
fun CollapsedSearchBar(
    label: String,
    topPadding: Dp,
    startPadding: Dp,
    endPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val spacing = AppTheme.spacing
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
            modifier
                .fillMaxWidth()
                .padding(start = startPadding, end = endPadding)
                .padding(top = topPadding, bottom = AppTheme.sizes.searchBarBottomPadding),
        onSearch = {},
        enabled = false,
        expanded = false,
        onExpandedChange = {},
    )
}

@Composable
fun SearchStatus.TopAppBarAnim(
    modifier: Modifier = Modifier,
    visible: Boolean = shouldCollapse(),
    content: @Composable () -> Unit,
) {
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
                .graphicsLayer {
                    this.alpha = alpha
                }
    ) {
        content()
    }
}

/**
 * Expandable search overlay. Callers must keep this composed always (do not `if (!isCollapsed)`) so
 * expand/collapse animations stay continuous.
 *
 * When settled `SearchStatus.Status.COLLAPSED`: zero-size, no zIndex, no pointer — does not cover
 * the page. While expanding / expanded / collapsing: full-screen dim + bar.
 *
 * Must be placed as a **full-window sibling** of the page Scaffold (see AccessControl /
 * Connection), not inside Scaffold content — `topPadding` animates relative to the window top
 * (status bars), matching the collapsed bar's `SearchStatus.offsetY`.
 */
@Composable
fun SearchStatus.SearchPager(
    onSearchStatusChange: (SearchStatus) -> Unit,
    defaultResult: @Composable () -> Unit = {},
    emptyResult: @Composable () -> Unit = {},
    padding: SearchBarPadding = SearchBarPadding(),
    expandedTopPadding: Dp? = null,
    collapseAtExpandedPosition: Boolean = false,
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

    val searchStatus = this
    // Settled COLLAPSED only. COLLAPSING keeps the expanded chrome so the bar can slide home.
    val fullyCollapsed = searchStatus.isCollapsed()
    val active = !fullyCollapsed
    val isExpanded = searchStatus.isExpanded()
    val keepExpandedPosition =
        searchStatus.shouldExpand() ||
            (collapseAtExpandedPosition && searchStatus.current == SearchStatus.Status.COLLAPSING)
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
    val topPadding by
    animateDpAsState(
        targetValue =
            if (keepExpandedPosition) {
                expandedTopPadding ?: (systemBarsPadding + componentSizes.listItemVerticalMinimal)
            } else {
                max(searchStatus.offsetY, spacing.space0)
            },
        animationSpec = tween(300, easing = LinearOutSlowInEasing),
        label = "SearchTopPadding",
        finishedListener = { onSearchStatusChange(searchStatus.onAnimationComplete()) },
    )
    val surfaceAlpha by
        animateFloatAsState(
        targetValue = if (searchStatus.shouldExpand()) 1f else 0f,
        animationSpec = tween(300, easing = LinearOutSlowInEasing),
            label = "SearchSurfaceAlpha",
            finishedListener = {
                if (collapseAtExpandedPosition && searchStatus.current == SearchStatus.Status.COLLAPSING) {
                    onSearchStatusChange(searchStatus.onAnimationComplete())
                }
            },
    )

    BackHandler(enabled = active) {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        onSearchStatusChange(
            searchStatus.copy(searchText = "", current = SearchStatus.Status.COLLAPSING)
        )
    }

    // Never early-return: remounting drops the animate* state and the bar "flies".
    // Collapsed: size 0, no elevation. Active: full-screen overlay above the page.
    Column(
        modifier =
            if (active) {
                Modifier
                    .fillMaxSize()
                    .zIndex(5f)
                    .background(colorScheme.surface.copy(alpha = surfaceAlpha))
                    .pointerInput(searchStatus.current) {}
            } else {
                Modifier.size(UiDp.dp0)
            }
    ) {
        if (active) {
            SearchPagerTopRow(
                searchStatus = searchStatus,
                onSearchStatusChange = onSearchStatusChange,
                topPadding = topPadding,
                barPadding = resolvedPadding,
            )
            SearchPagerResultsLayer(
                searchStatus = searchStatus,
                isExpanded = isExpanded,
                defaultResult = defaultResult,
                emptyResult = emptyResult,
                result = result,
            )
        }
    }
}

@Composable
private fun SearchPagerTopRow(
    searchStatus: SearchStatus,
    onSearchStatusChange: (SearchStatus) -> Unit,
    topPadding: Dp,
    barPadding: SearchBarPadding,
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
            searchStatus = searchStatus,
            onSearchStatusChange = onSearchStatusChange,
            padding = barPadding,
            modifier = Modifier.weight(1f),
        )
        SearchPagerCancelButton(
            searchStatus = searchStatus,
            onSearchStatusChange = onSearchStatusChange,
            searchBarTopPadding = barPadding.top,
        )
    }
}

@Composable
private fun SearchPagerCancelButton(
    searchStatus: SearchStatus,
    onSearchStatusChange: (SearchStatus) -> Unit,
    searchBarTopPadding: Dp,
) {
    val spacing = AppTheme.spacing
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val isExpanded = searchStatus.isExpanded() || searchStatus.isExpanding()
    AnimatedVisibility(
        visible = isExpanded,
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
                        enabled = searchStatus.isExpanded(),
                    ) {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                        onSearchStatusChange(
                            searchStatus.copy(
                                searchText = "",
                                current = SearchStatus.Status.COLLAPSING,
                            )
                        )
                    },
        )
    }
}

@Composable
private fun SearchPagerResultsLayer(
    searchStatus: SearchStatus,
    isExpanded: Boolean,
    defaultResult: @Composable () -> Unit,
    emptyResult: @Composable () -> Unit,
    result: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = isExpanded,
        modifier = Modifier
            .fillMaxSize()
            .zIndex(1f),
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        when (searchStatus.resultStatus) {
            SearchStatus.ResultStatus.DEFAULT -> defaultResult()
            SearchStatus.ResultStatus.EMPTY -> emptyResult()
            SearchStatus.ResultStatus.SHOW -> result()
        }
    }
}

@Composable
private fun SearchBar(
    searchStatus: SearchStatus,
    onSearchStatusChange: (SearchStatus) -> Unit,
    padding: SearchBarPadding,
    modifier: Modifier = Modifier,
) {
    val spacing = AppTheme.spacing
    val componentSizes = AppTheme.sizes
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    InputField(
        query = searchStatus.searchText,
        onQueryChange = { onSearchStatusChange(searchStatus.copy(searchText = it)) },
        label = searchStatus.label,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(start = padding.start, end = padding.end)
                .padding(top = padding.top, bottom = componentSizes.searchBarBottomPadding)
                .heightIn(min = componentSizes.searchFieldMinHeight),
        leadingIcon = { SearchBarLeadingIcon(componentSizes = componentSizes, spacing = spacing) },
        trailingIcon = {
            SearchBarClearButton(
                searchText = searchStatus.searchText,
                onClear = {
                    onSearchStatusChange(searchStatus.copy(searchText = ""))
                },
                componentSizes = componentSizes,
                spacing = spacing,
            )
        },
        onSearch = {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        },
        expanded = searchStatus.shouldExpand(),
        onExpandedChange = { expanded ->
            if (!expanded && searchStatus.shouldExpand()) {
                onSearchStatusChange(
                    searchStatus.copy(
                        searchText = "",
                        current = SearchStatus.Status.COLLAPSING,
                    )
                )
            }
        },
    )
}

@Composable
private fun SearchBarLeadingIcon(componentSizes: Sizes, spacing: Spacing) {
    Icon(
        imageVector = MiuixIcons.Basic.Search,
        contentDescription = YumeTxt.Component.Editor.Action.Search,
        modifier =
            Modifier
                .size(componentSizes.searchIconTouchTarget)
                .padding(start = spacing.space16, end = spacing.space8),
        tint = colorScheme.onSurfaceVariantSummary,
    )
}

@Composable
private fun SearchBarClearButton(
    searchText: String,
    onClear: () -> Unit,
    componentSizes: Sizes,
    spacing: Spacing,
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
                    .size(componentSizes.searchIconTouchTarget)
                    .padding(start = spacing.space8, end = spacing.space16)
                    .clickable(interactionSource = null, indication = null) { onClear() },
        )
    }
}
