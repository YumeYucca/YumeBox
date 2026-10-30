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

package com.github.yumeyucca.yumebox.presentation.navigation

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import com.github.yumeyucca.yumebox.presentation.component.BottomBarDestination
import com.github.yumeyucca.yumebox.presentation.component.BottomBarTabs
import com.github.yumeyucca.yumebox.presentation.component.MainDestinationState
import com.github.yumeyucca.yumebox.presentation.component.MainPagerState
import com.github.yumeyucca.yumebox.presentation.component.rememberMainDestinationState
import com.github.yumeyucca.yumebox.presentation.component.rememberMainPagerState
import com.github.yumeyucca.yumebox.screen.moe.calculateHomeVisibility

/**
 * The bottom pager of one screen: which pages the bar offers, which one it rests on, where a swipe is
 * heading, and the two answers the shell derives from that.
 *
 * [MainDestinationState] and [MainPagerState] cannot be read independently — a page count change, a
 * page change and the end of a swipe each have to be reconciled against the other, and the effects in
 * [rememberMainPageModel] are the whole reconciliation. Holding them together keeps "which
 * destination is selected" answered in exactly one place.
 */
@Stable
internal class MainPageModel(
    val destinations: MainDestinationState,
    val pager: MainPagerState,
    /** Home-page progress in pages, read where the home page draws rather than here. */
    val homeVisibility: State<Float>,
) {
    val tabs: BottomBarTabs
        get() = destinations.tabs

    val selected: BottomBarDestination
        get() = tabs.destinationAt(pager.selectedPage)

    val settled: BottomBarDestination
        get() = destinations.settled

    /** The destination [settled] held before the last change, for one-shot tab-change reactions. */
    val previousSettled: BottomBarDestination
        get() = destinations.previousSettled

    /**
     * The floating bar shows on every page except the home, which has its own chrome; it also stays
     * hidden while a swipe towards home has selected but not settled.
     */
    val bottomBarVisible: Boolean
        get() = selected != BottomBarDestination.Home && settled != BottomBarDestination.Home

    /** Animates to the page that shows [destination]; the bar and the deep links both go through this. */
    fun show(destination: BottomBarDestination) {
        pager.animateToPage(destinations.onTabTapped(destination))
    }
}

/**
 * Remembers the page model and reconciles its three sources of change.
 *
 * Every `LaunchedEffect` here exists because a different thing changed — the visible page set, the
 * selected page, or the end of a scroll — and each would be wrong to fold into another: settling on a
 * page change alone would report a destination the pager has not reached, and reconciling visibility
 * while scrolling would move the user's page out from under the gesture.
 */
@Composable
internal fun rememberMainPageModel(
    initialDestination: BottomBarDestination,
    everReady: Boolean,
): MainPageModel {
    val destinations =
        rememberMainDestinationState(
            initialDestination = initialDestination,
            // Providers is opened above a freshly-created Main(Proxy) route on compact layouts, so a
            // session that starts on the proxy page must not wait for the first node load.
            proxyInitiallyCommitted = initialDestination == BottomBarDestination.Proxy,
        )
    val pagerState =
        rememberPagerState(
            initialPage = destinations.startPage,
            pageCount = { destinations.visible.size },
        )
    val pager = rememberMainPagerState(pagerState)

    LaunchedEffect(everReady, pager.pagerState.isScrollInProgress) {
        // Insert the proxy page only after the first node load and after a swipe has settled.
        destinations.revealProxy(
            everReady = everReady,
            scrolling = pager.pagerState.isScrollInProgress,
        )
    }

    LaunchedEffect(destinations.visible) {
        val targetPage = destinations.onVisibleChanged(pager.pagerState.currentPage)
        if (pager.pagerState.currentPage != targetPage) {
            pager.pagerState.requestScrollToPage(targetPage)
        }
        // The bar's contents changed, so the page it rests on has to be re-derived even when the page
        // number itself did not move.
        pager.syncPage()
    }

    LaunchedEffect(pager.pagerState.currentPage) { pager.syncPage() }

    LaunchedEffect(pager.pagerState.currentPage, pager.pagerState.isScrollInProgress) {
        if (!pager.pagerState.isScrollInProgress) {
            destinations.settle(pager.pagerState.currentPage)
        }
    }

    val homeVisibility =
        remember(pager) {
            derivedStateOf {
                calculateHomeVisibility(
                    currentPage = pager.pagerState.currentPage,
                    currentPageOffsetFraction = pager.pagerState.currentPageOffsetFraction,
                )
            }
        }

    return remember(destinations, pager, homeVisibility) {
        MainPageModel(destinations, pager, homeVisibility)
    }
}
