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

import androidx.compose.runtime.Immutable

/**
 * The tab set the main pager renders, plus the set it is leaving.
 *
 * [visible] and [previous] differ only while the tab set changes: the proxy destination joins the
 * bar after the first node load settles, so for a few frames the pager still shows the older set.
 * Keeping the leaving set next to the current one is what stops a page from swapping its content
 * under the user's finger mid-transition.
 */
@Immutable
data class BottomBarTabs(
    val visible: List<BottomBarDestination>,
    val previous: List<BottomBarDestination>,
    val settled: BottomBarDestination,
) {
    /** True while the pager is rendering the set it is leaving. */
    val isTransitioning: Boolean
        get() = previous != visible

    /**
     * Destination the page at [page] is showing: the leaving set wins for a page that has not been
     * swapped yet, and [settled] is the last resort so a shrinking set never renders nothing.
     */
    fun destinationAt(page: Int): BottomBarDestination =
        (if (isTransitioning) previous.getOrNull(page) else visible.getOrNull(page))
            ?: previous.getOrNull(page)
            ?: settled
}
