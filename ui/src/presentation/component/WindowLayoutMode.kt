/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 */

package com.github.yumeyucca.yumebox.presentation.component


import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Window size class for adaptive layout.
 *
 * Product rule (main tabs):
 * - [Compact]: phone single stack; secondary screens push on the root navigator.
 * - [RailSingle]/[TwoPane]: tablet dual-pane shell (left main pager + right detail host).
 *
 * Module-level master/detail inside a tab is intentionally not used anymore; tablet detail always
 * goes through [LocalDetailNavigator] in the shell right pane.
 *
 * Note this ranks windows for layout, not devices: a landscape phone earns [TwoPane] by its width
 * while staying a phone for [isTabletWidth].
 */
enum class WindowLayoutMode {
    Compact,
    RailSingle,
    TwoPane;

    /** Tablet dual-pane shell should be used (left pager + right detail). */
    val usesSplitShell: Boolean
        get() = this != Compact
}

/** Smallest-side width from which a window counts as tablet-sized (Android's `sw600dp` rule). */
const val TabletWidthBreakpointDp = 600

/** Shortest side from which a landscape phone also earns the two-pane shell. */
private const val TwoPaneBreakpointDp = 700

/**
 * Whether this window is tablet-sized, i.e. its shortest side clears [TabletWidthBreakpointDp].
 *
 * Deliberately not `rememberWindowLayoutMode() != Compact`: that answers "which shell should this
 * window use", which a landscape phone also earns by width alone. This answers "what kind of
 * device is this", which only the shortest side can tell (a tall phone would otherwise pass on
 * height alone).
 */
@Composable
fun isTabletWidth(): Boolean {
    val configuration = LocalConfiguration.current
    return minOf(configuration.screenWidthDp, configuration.screenHeightDp) >= TabletWidthBreakpointDp
}

@Composable
fun rememberWindowLayoutMode(): WindowLayoutMode {
    val configuration = LocalConfiguration.current
    // Device class comes from the SHORTEST side (smallestScreenWidthDp semantics): a phone's
    // tallest side also clears 700dp, so judging by the longest side would wrongly give phones
    // the split shell in portrait. Tablets keep the shell in every rotation (issue #151 item 1),
    // while phones only enter the shell when the current width itself is wide (landscape).
    val shortestSideDp = minOf(configuration.screenWidthDp, configuration.screenHeightDp)
    val widthDp = configuration.screenWidthDp
    return remember(shortestSideDp, widthDp) {
        when {
            shortestSideDp >= TwoPaneBreakpointDp -> WindowLayoutMode.TwoPane
            shortestSideDp >= TabletWidthBreakpointDp -> WindowLayoutMode.RailSingle
            widthDp >= TwoPaneBreakpointDp -> WindowLayoutMode.TwoPane
            else -> WindowLayoutMode.Compact
        }
    }
}
