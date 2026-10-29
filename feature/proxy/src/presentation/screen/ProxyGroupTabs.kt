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
 */

package com.github.yumeyucca.yumebox.presentation.screen

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.yumeyucca.yumebox.domain.model.ProxyGroupInfo
import com.github.yumeyucca.yumebox.presentation.component.LocalMainPagerState
import com.github.yumeyucca.yumebox.presentation.component.NoOpNestedScrollConnection
import com.github.yumeyucca.yumebox.presentation.component.SegmentedTabRow
import com.github.yumeyucca.yumebox.presentation.component.rememberNestedPagerHandoffConnection
import com.github.yumeyucca.yumebox.presentation.theme.LocalTopBarBackdrop
import com.github.yumeyucca.yumebox.presentation.theme.UiDp

internal val ProxyTabBandInset = 66.dp

@Stable
internal class ProxyTabScrollState internal constructor(
    private val maxCollapsePx: Float,
) {
    private var distanceFromTopPx by mutableFloatStateOf(0f)

    val collapsedPx: Float
        get() = distanceFromTopPx.coerceAtMost(maxCollapsePx)

    val nestedScrollConnection =
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (consumed.y != 0f) {
                    distanceFromTopPx =
                        (distanceFromTopPx - consumed.y).coerceAtLeast(0f)
                }
                return Offset.Zero
            }
        }

    fun syncContent(isAtTop: Boolean) {
        distanceFromTopPx = if (isAtTop) 0f else Float.MAX_VALUE
    }

    fun resetToTop() {
        distanceFromTopPx = 0f
    }
}

@Composable
internal fun rememberProxyTabScrollState(
    maxCollapse: Dp = ProxyTabBandInset,
): ProxyTabScrollState {
    val maxCollapsePx = with(LocalDensity.current) { maxCollapse.toPx() }
    return remember(maxCollapsePx) { ProxyTabScrollState(maxCollapsePx) }
}

@Composable
internal fun rememberProxyGroupPagerState(
    groups: List<ProxyGroupInfo>,
    selectedGroupName: String?,
    onGroupSelected: (ProxyGroupInfo) -> Unit,
): PagerState {
    val initialPage =
        groups
            .indexOfFirst { group -> group.name == selectedGroupName }
            .coerceAtLeast(0)
    val pagerState =
        rememberPagerState(
            initialPage = initialPage,
            pageCount = { groups.size },
        )
    val latestOnGroupSelected by rememberUpdatedState(onGroupSelected)

    LaunchedEffect(pagerState.currentPage, groups) {
        groups.getOrNull(pagerState.currentPage)?.let(latestOnGroupSelected)
    }
    LaunchedEffect(selectedGroupName, groups, pagerState.isScrollInProgress) {
        val selectedIndex = groups.indexOfFirst { group -> group.name == selectedGroupName }
        if (
            selectedIndex >= 0 &&
                selectedIndex != pagerState.currentPage &&
                !pagerState.isScrollInProgress
        ) {
            pagerState.scrollToPage(selectedIndex)
        }
    }

    return pagerState
}

@Composable
internal fun ProxyGroupTabPager(
    groups: List<ProxyGroupInfo>,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    pageContent: @Composable (ProxyGroupInfo) -> Unit,
) {
    if (groups.isEmpty()) return
    val parentPagerState = LocalMainPagerState.current.pagerState
    val handoffConnection =
        rememberNestedPagerHandoffConnection(
            parentState = parentPagerState,
            childState = pagerState,
        )

    HorizontalPager(
        state = pagerState,
        modifier =
            modifier
                .fillMaxSize()
                .nestedScroll(handoffConnection),
        beyondViewportPageCount = 0,
        userScrollEnabled = true,
        pageNestedScrollConnection = NoOpNestedScrollConnection,
        overscrollEffect = null,
    ) { page ->
        val group = groups.getOrNull(page)
        if (group != null) {
            pageContent(group)
        }
    }
}

@Composable
internal fun ProxyTabStrip(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    scrollState: ScrollState? = null,
) {
    if (tabs.isEmpty()) return
    SegmentedTabRow(
        tabs = tabs,
        selectedTabIndex = selectedTabIndex.coerceIn(0, tabs.lastIndex),
        onTabSelected = onTabSelected,
        modifier = modifier.fillMaxWidth(),
        tabHeight = UiDp.dp42,
        backdrop = LocalTopBarBackdrop.current,
        scrollable = scrollable,
        scrollState = scrollState,
    )
}

@Composable
internal fun ProxyCollapsingTabs(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    topPadding: Dp,
    collapseState: ProxyTabScrollState,
    modifier: Modifier = Modifier,
    scrollable: Boolean = false,
    tabScrollState: ScrollState? = null,
) {
    if (tabs.isEmpty()) return
    val density = LocalDensity.current
    val restTopPx = with(density) { topPadding.toPx() }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationY = restTopPx - collapseState.collapsedPx
                }
                .padding(start = UiDp.dp12, end = UiDp.dp12, bottom = UiDp.dp4)
    ) {
        ProxyTabStrip(
            tabs = tabs,
            selectedTabIndex = selectedTabIndex,
            onTabSelected = onTabSelected,
            scrollable = scrollable,
            scrollState = tabScrollState,
        )
    }
}
