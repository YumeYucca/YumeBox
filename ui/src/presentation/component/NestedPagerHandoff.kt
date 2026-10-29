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

package com.github.yumeyucca.yumebox.presentation.component

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.Velocity
import kotlin.math.absoluteValue

/**
 * Disables a Pager's built-in nested-scroll arbitration so [rememberNestedPagerHandoffConnection]
 * can own boundary hand-off without competing with the parent's default connection.
 */
object NoOpNestedScrollConnection : NestedScrollConnection

/**
 * Hands horizontal drag/fling from [childState] to the direct [parentState] only after the child
 * reaches its first or last page. LTR and `reverseLayout = false` are assumed.
 */
@Composable
fun rememberNestedPagerHandoffConnection(
    parentState: PagerState,
    childState: PagerState,
): NestedScrollConnection {
    val minimumFlingVelocity = LocalViewConfiguration.current.minimumFlingVelocity
    return remember(parentState, childState, minimumFlingVelocity) {
        NestedPagerHandoffConnection(
            parentState = parentState,
            childState = childState,
            minimumFlingVelocity = minimumFlingVelocity,
        )
    }
}

private class NestedPagerHandoffConnection(
    private val parentState: PagerState,
    private val childState: PagerState,
    private val minimumFlingVelocity: Float,
) : NestedScrollConnection {
    private var parentDragActive = false

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput || available.x == 0f) {
            return Offset.Zero
        }
        if (parentDragActive) {
            return scrollParent(available.x)
        }
        // YumeBox only nests one Proxy pager inside the main app pager. Do not keep the upstream
        // three-level guard: waiting for isScrollInProgress drops the first boundary frame and
        // makes the eventual hand-off feel like a jump.
        return if (!childState.canScrollInGestureDirection(available.x)) {
            scrollParent(available.x)
        } else {
            Offset.Zero
        }
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (source != NestedScrollSource.UserInput || available.x == 0f) {
            return Offset.Zero
        }
        return if (parentDragActive || parentState.canScrollInGestureDirection(available.x)) {
            scrollParent(available.x)
        } else {
            Offset.Zero
        }
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (available.x == 0f || !shouldParentHandlePreFling(available.x)) {
            return Velocity.Zero
        }
        settleParent(available.x)
        return available.copy(y = 0f)
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        if (available.x == 0f || !(shouldParentSettle() || shouldParentHandleFling(available.x))) {
            return Velocity.Zero
        }
        settleParent(available.x)
        return available.copy(y = 0f)
    }

    private fun scrollParent(deltaX: Float): Offset {
        val parentIsBetweenPages = parentState.isBetweenPages()
        if (!parentIsBetweenPages && !parentState.canScrollInGestureDirection(deltaX)) {
            parentDragActive = false
            return Offset.Zero
        }
        val consumed = -parentState.dispatchRawDelta(-deltaX)
        if (consumed.absoluteValue > 0f) {
            parentDragActive = true
        }
        return Offset(x = consumed, y = 0f)
    }

    private fun shouldParentHandleFling(velocityX: Float): Boolean {
        val childAtBoundary = !childState.canScrollInGestureDirection(velocityX)
        val parentCanMove =
            parentDragActive ||
                parentState.canScrollInGestureDirection(velocityX) ||
                parentState.isBetweenPages()
        return parentCanMove &&
            (parentDragActive || childAtBoundary) &&
            velocityX.absoluteValue >= minimumFlingVelocity
    }

    private fun shouldParentHandlePreFling(velocityX: Float): Boolean =
        (parentDragActive || childState.isScrollInProgress) &&
            shouldParentHandleFling(velocityX)

    private fun shouldParentSettle(): Boolean =
        parentDragActive || parentState.isBetweenPages()

    private suspend fun settleParent(velocityX: Float) {
        parentDragActive = false
        parentState.animateScrollToPage(
            parentState.settleTargetPage(velocityX, minimumFlingVelocity)
        )
    }
}

private fun PagerState.canScrollInGestureDirection(deltaX: Float): Boolean =
    when {
        deltaX < 0f -> canScrollForward
        deltaX > 0f -> canScrollBackward
        else -> false
    }

private fun PagerState.settleTargetPage(velocityX: Float, minimumFlingVelocity: Float): Int {
    val targetPage =
        when {
            velocityX < -minimumFlingVelocity && canScrollForward -> currentPage + 1
            velocityX > minimumFlingVelocity && canScrollBackward -> currentPage - 1
            else -> currentPage
        }
    return targetPage.coerceIn(0, pageCount - 1)
}

private fun PagerState.isBetweenPages(): Boolean =
    currentPageOffsetFraction.absoluteValue >= 0.001f
