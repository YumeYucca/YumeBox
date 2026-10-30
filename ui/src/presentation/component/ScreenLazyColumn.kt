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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun ScreenLazyColumn(
    scrollBehavior: ScrollBehavior,
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues? = null,
    bottomPadding: Dp = UiDp.dp0,
    topPadding: Dp = UiDp.dp0,
    lazyListState: LazyListState = rememberLazyListState(),
    content: LazyListScope.() -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    // One wiring, two halves: the behaviour drives the bar's visibility from this list's scroll
    // position, so both the tracked state and the connection come from the same value.
    val bottomBarScrollBehavior = LocalBottomBarScrollBehavior.current
    if (bottomBarScrollBehavior != null) {
        bottomBarScrollBehavior.ShowBottomBarWhenScrolledToTop(lazyListState)
    }
    val resolvedContentPadding =
        // `layoutDirection` is a key: the block converts the padding's start/end to left/right, so an
        // RTL switch has to recompute it.
        remember(contentPadding, innerPadding, topPadding, bottomPadding, layoutDirection) {
            contentPadding
                ?: PaddingValues(
                    top = innerPadding.calculateTopPadding() + topPadding,
                    bottom = innerPadding.calculateBottomPadding() + bottomPadding,
                    start = innerPadding.calculateStartPadding(layoutDirection),
                    end = innerPadding.calculateEndPadding(layoutDirection),
                )
        }
    LazyColumn(
        state = lazyListState,
        modifier =
            modifier
                .fillMaxSize()
                .overScrollVertical()
                .scrollEndHaptic()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .let { mod ->
                    if (bottomBarScrollBehavior != null) {
                        mod.nestedScroll(bottomBarScrollBehavior.nestedScrollConnection)
                    } else {
                        mod
                    }
                },
        contentPadding = resolvedContentPadding,
        overscrollEffect = null,
        content = content,
    )
}

/**
 * Stacks two padding sets: the page's own start/end from [localPadding] and the extra bottom inset
 * [mainPadding] reserves for the floating bar.
 */
@Composable
fun combinePaddingValues(localPadding: PaddingValues, mainPadding: PaddingValues): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current
    return PaddingValues(
        top = localPadding.calculateTopPadding(),
        bottom =
            localPadding.calculateBottomPadding()
                .plus(mainPadding.calculateBottomPadding())
                .plus(AppTheme.spacing.space12),
        start = localPadding.calculateStartPadding(layoutDirection),
        end = localPadding.calculateEndPadding(layoutDirection),
    )
}

/**
 * [ScreenLazyColumn] for a standalone page: [scaffoldPadding] is the Scaffold's own padding and the
 * floating bottom bar's reserve is added here, so pages do not restate that policy themselves.
 */
@Composable
fun StandalonePageList(
    scrollBehavior: ScrollBehavior,
    scaffoldPadding: PaddingValues,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues? = null,
    bottomPadding: Dp = UiDp.dp0,
    topPadding: Dp = UiDp.dp0,
    lazyListState: LazyListState = rememberLazyListState(),
    content: LazyListScope.() -> Unit,
) {
    ScreenLazyColumn(
        scrollBehavior = scrollBehavior,
        innerPadding = standalonePageInnerPadding(scaffoldPadding),
        modifier = modifier,
        contentPadding = contentPadding,
        bottomPadding = bottomPadding,
        topPadding = topPadding,
        lazyListState = lazyListState,
        content = content,
    )
}

/**
 * Inner padding for a standalone page: the scaffold's padding plus the height the floating bottom
 * bar reserves, plus the spacing step that keeps the last row clear of it. Prefer this over holding
 * [rememberStandalonePageMainPadding] unless the page also needs the raw reserved height elsewhere.
 */
@Composable
fun standalonePageInnerPadding(scaffoldPadding: PaddingValues): PaddingValues =
    combinePaddingValues(
        localPadding = scaffoldPadding,
        mainPadding = rememberStandalonePageMainPadding(),
    )

@Composable
fun rememberStandalonePageMainPadding(): PaddingValues {
    val reservedBottomBarHeight = rememberBottomBarReservedHeight()
    return remember(reservedBottomBarHeight) {
        PaddingValues(bottom = reservedBottomBarHeight)
    }
}
