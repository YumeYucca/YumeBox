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

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.Stable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Hop count from which [MainPagerState.animateToPage] jumps instead of animating through every
 * intermediate page; animating a long run makes the bar's capsule lag far behind the finger.
 */
private const val PagerAnimateScrollPreJumpThreshold = 3

@Stable
class MainPagerState(
    val pagerState: PagerState,
    private val coroutineScope: CoroutineScope,
) {
    var selectedPage by mutableIntStateOf(pagerState.currentPage)
        private set

    var isNavigating by mutableStateOf(false)
        private set

    private var navJob: Job? = null

    fun animateToPage(targetIndex: Int) {
        val boundedTarget = targetIndex.coerceIn(0, (pagerState.pageCount - 1).coerceAtLeast(0))
        // The single "already there" rule for the bar and for programmatic navigation. A page counts
        // as reached when it is selected and at rest: mid-drag the bar must still bring it home, and
        // while a navigation runs the newest target wins.
        if (
            boundedTarget == selectedPage &&
            (isNavigating || pagerState.currentPageOffsetFraction == 0f)
        ) {
            return
        }

        navJob?.cancel()
        selectedPage = boundedTarget
        isNavigating = true
        navJob = coroutineScope.launch {
            val myJob = coroutineContext.job
            try {
                val hops = abs(boundedTarget - pagerState.currentPage)
                val animationSpec = MainBottomBarDefaults.pagerNavigationAnimationSpec(hops)
                if (
                    hops >= PagerAnimateScrollPreJumpThreshold &&
                        pagerState.layoutInfo.pageSize > 0
                ) {
                    pagerState.animateScrollToPageUninterrupted(
                        page = boundedTarget,
                        animationSpec = animationSpec,
                    )
                } else {
                    pagerState.animateScrollToPage(
                        page = boundedTarget,
                        animationSpec = animationSpec,
                    )
                }
            } finally {
                if (navJob == myJob) {
                    isNavigating = false
                    if (pagerState.currentPage != boundedTarget) {
                        selectedPage = pagerState.currentPage
                    }
                }
            }
        }
    }

    fun syncPage() {
        if (!isNavigating && selectedPage != pagerState.currentPage) {
            selectedPage = pagerState.currentPage
        }
    }
}

private suspend fun PagerState.animateScrollToPageUninterrupted(
    page: Int,
    animationSpec: AnimationSpec<Float>,
) {
    val target = page.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
    val pageSizeWithSpacing = layoutInfo.pageSize + layoutInfo.pageSpacing
    if (pageSizeWithSpacing <= 0) {
        animateScrollToPage(page = target, animationSpec = animationSpec)
        return
    }
    scroll {
        updateTargetPage(target)
        val distance = (target - currentPage - currentPageOffsetFraction) * pageSizeWithSpacing
        var previous = 0f
        animate(0f, distance, animationSpec = animationSpec) { value, _ ->
            previous += scrollBy(value - previous)
        }
    }
}

@Composable
fun rememberMainPagerState(
    pagerState: PagerState,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
): MainPagerState =
    remember(pagerState, coroutineScope) { MainPagerState(pagerState, coroutineScope) }

val LocalMainPagerState =
    compositionLocalOf<MainPagerState> { error("LocalMainPagerState is not provided") }
val LocalHandlePageChange =
    compositionLocalOf<(BottomBarDestination) -> Unit> {
        error("LocalHandlePageChange is not provided")
    }
