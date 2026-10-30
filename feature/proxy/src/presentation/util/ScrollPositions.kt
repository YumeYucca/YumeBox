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

package com.github.yumeyucca.yumebox.presentation.util

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable

/**
 * The proxy feature's one definition of "where a scroll surface is".
 *
 * The collapsing tab strip and the two sheet/page lists all need the same two answers — "is this
 * surface still at its first pixel" and "a saved state keyed by which page" — so they live here
 * instead of being re-derived per call site (one of which had grown the inverted `isScrolledFromTop`
 * copy of the same test).
 */
@Composable
internal fun rememberSavedListState(input: Any? = Unit): LazyListState =
    rememberSaveable(input, saver = LazyListState.Saver) { LazyListState() }

@Composable
internal fun rememberSavedGridState(input: Any? = Unit): LazyGridState =
    rememberSaveable(input, saver = LazyGridState.Saver) { LazyGridState() }

internal val LazyListState.isAtTop: Boolean
    get() = firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0

internal val LazyGridState.isAtTop: Boolean
    get() = firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0
