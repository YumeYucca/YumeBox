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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * The bottom pager's page identity: which destinations the bar currently offers, which one the
 * pager rests on, and which one a swipe is heading to.
 *
 * The proxy page joins the bar only once the runtime has nodes, so the bar's contents, the pager's
 * page count and the settled destination all change at slightly different moments. Keeping them in
 * one holder means [settle] and [onVisibleChanged] are the only places that can reconcile those
 * changes, so the three fallback rules that used to live in separate effects cannot drift apart.
 */
@Stable
class MainDestinationState(
    private val initialDestination: BottomBarDestination,
    proxyInitiallyCommitted: Boolean,
) {
    var proxyCommitted by mutableStateOf(proxyInitiallyCommitted)
        private set
    var pending by mutableStateOf<BottomBarDestination?>(null)
        private set
    var settled by mutableStateOf(initialDestination)
        private set
    var previous by mutableStateOf(visible)
        private set
    /**
     * The destination [settled] held before the last change. It is owned here, next to [settled],
     * because the screen that reacts to a tab change would otherwise have to keep its own copy of the
     * previous value and could remember it wrongly on recreation.
     */
    var previousSettled by mutableStateOf(initialDestination)
        private set

    /** Destinations the bottom bar offers right now, in pager order. */
    val visible: List<BottomBarDestination>
        get() =
            BottomBarDestination.entries.filter { destination ->
                destination != BottomBarDestination.Proxy || proxyCommitted
            }

    val tabs: BottomBarTabs
        get() = BottomBarTabs(visible = visible, previous = previous, settled = settled)

    /** The page the pager starts on; the persisted destination is an entry of [visible]. */
    val startPage: Int
        get() = visible.indexOf(initialDestination).coerceAtLeast(0)

    /**
     * Reveals the proxy page once the runtime has loaded nodes, but never while the user is mid-swipe:
     * the page must not be inserted under a moving pager.
     *
     * The page is never hidden again — taking it back out would renumber every page under the user,
     * and a session that started on the proxy page already seeded it as committed.
     */
    fun revealProxy(everReady: Boolean, scrolling: Boolean) {
        if (!scrolling && everReady) proxyCommitted = true
    }

    /**
     * Records the user's tap on [destination] and answers the page to animate to.
     *
     * A destination that is not in the bar yet is remembered in [pending] and the pager moves to the
     * nearest page that exists, so the tap still lands somewhere instead of being dropped. The
     * [pending] write is why this is an event handler and not a query: ask it twice and the second
     * answer is the same, but the request has been recorded.
     */
    fun onTabTapped(destination: BottomBarDestination): Int {
        pending = destination.takeIf { it !in visible }
        val target = destination.takeIf { it in visible } ?: fallbackDestination
        return visible.indexOf(target).coerceAtLeast(0)
    }

    /**
     * Reconciles [settled]/[previous] after the bar's contents changed. Returns the page the pager
     * should show, which is what a page that just left the bar has to fall back to.
     */
    fun onVisibleChanged(currentPage: Int): Int {
        val requested = pending
        val current =
            requested?.takeIf { it in visible }
                ?: previous.getOrNull(currentPage)
                ?: settled
        val target = current.takeIf { it in visible } ?: fallbackDestination
        if (settled != target) {
            previousSettled = settled
            settled = target
        }
        previous = visible
        // A request that cannot be honoured right now is dropped instead of parked: honouring it after
        // some unrelated tab-set change would later teleport the pager long after the user tapped.
        pending = null
        return visible.indexOf(target).coerceAtLeast(0)
    }

    /** Records the page the pager came to rest on. */
    fun settle(page: Int) {
        val target = visible.getOrNull(page) ?: previous.getOrNull(page) ?: settled
        if (settled != target) {
            previousSettled = settled
            settled = target
        }
    }

    /**
     * Where a page that is no longer offered lands. Profiles is used rather than the home page
     * because it is the only destination that is always in the bar and is not the home chrome; the
     * home page owns its own navigation and must not be entered by a silent correction.
     */
    private val fallbackDestination: BottomBarDestination
        get() = BottomBarDestination.Config
}

/**
 * Remembers the holder for the life of the screen.
 *
 * [initialDestination] and [proxyInitiallyCommitted] are seeds, not inputs: the holder reports the
 * page it settles on back to the shell, the shell hands that value straight back down, and keying
 * this remember on it would rebuild the holder (and forget [MainDestinationState.pending]) on every
 * settle.
 */
@Composable
fun rememberMainDestinationState(
    initialDestination: BottomBarDestination,
    proxyInitiallyCommitted: Boolean,
): MainDestinationState =
    remember {
        MainDestinationState(initialDestination, proxyInitiallyCommitted)
    }
