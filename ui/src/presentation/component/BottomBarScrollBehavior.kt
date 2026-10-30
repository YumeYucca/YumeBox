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

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlin.math.abs

@Stable
class BottomBarScrollBehavior {
    var isBottomBarVisible by mutableStateOf(true)
        private set

    var isAutoHideEnabled by mutableStateOf(true)

    private val scrollThreshold = 12f
    private val flingThreshold = 120f

    /** Deltas below this are sub-pixel noise from the scroll pipeline, not user intent. */
    private val minScrollDelta = 0.5f

    private var lastToggleTime = 0L
    private val toggleDelay = 150L

    private var accumulatedScroll = 0f

    val nestedScrollConnection =
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!drivesAutoHide(source)) return Offset.Zero

                updateVisibilityFromDelta(available.y)
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (!drivesAutoHide(source)) return Offset.Zero

                val delta =
                    when {
                        abs(consumed.y) >= minScrollDelta -> consumed.y
                        abs(available.y) >= minScrollDelta -> available.y
                        else -> 0f
                    }
                if (delta == 0f) return Offset.Zero

                updateVisibilityFromDelta(delta)
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (!isAutoHideEnabled) return Velocity.Zero

                accumulatedScroll = 0f
                val velocityY =
                    when {
                        abs(consumed.y) >= flingThreshold -> consumed.y
                        abs(available.y) >= flingThreshold -> available.y
                        else -> 0f
                    }

                when {
                    velocityY < 0f -> hideBottomBar()
                    velocityY > 0f -> showBottomBar()
                }
                return Velocity.Zero
            }
        }

    private fun drivesAutoHide(source: NestedScrollSource): Boolean =
        isAutoHideEnabled && source == NestedScrollSource.UserInput

    private fun updateVisibilityFromDelta(deltaY: Float) {
        if (abs(deltaY) < minScrollDelta) return

        val reversedWhileShowing = accumulatedScroll > 0f && deltaY < 0f
        val reversedWhileHiding = accumulatedScroll < 0f && deltaY > 0f
        if (reversedWhileShowing || reversedWhileHiding) {
            accumulatedScroll = 0f
        }

        accumulatedScroll += deltaY

        if (abs(accumulatedScroll) >= scrollThreshold) {
            if (accumulatedScroll < 0f) hideBottomBar() else showBottomBar()
            accumulatedScroll = 0f
        }
    }

    fun showBottomBar() {
        transitionTo(visible = true)
    }

    fun hideBottomBar() {
        transitionTo(visible = false)
    }

    private fun transitionTo(visible: Boolean) {
        val currentTime = System.currentTimeMillis()
        if (isBottomBarVisible == visible) return
        if (currentTime - lastToggleTime < toggleDelay) return
        isBottomBarVisible = visible
        lastToggleTime = currentTime
    }
}

@Composable
fun rememberBottomBarScrollBehavior(autoHideEnabled: Boolean = true): BottomBarScrollBehavior {
    val behavior = remember { BottomBarScrollBehavior() }
    // The flag is mutable state on the behaviour, so toggling it writes through: re-creating the
    // object here would throw away the current visibility and the toggle debounce timestamp.
    SideEffect { behavior.isAutoHideEnabled = autoHideEnabled }
    return behavior
}

/**
 * Reveals the bottom bar whenever [listState] sits at the very top of the list.
 *
 * This is a side effect, not a builder: call it as a statement from the list host.
 */
@Composable
fun BottomBarScrollBehavior.ShowBottomBarWhenScrolledToTop(listState: LazyListState) {
    val isAtTop by
    remember(listState) {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        }
    }

    LaunchedEffect(isAtTop) {
        if (isAtTop) {
            showBottomBar()
        }
    }
}

val LocalBottomBarScrollBehavior = compositionLocalOf<BottomBarScrollBehavior?> { null }
