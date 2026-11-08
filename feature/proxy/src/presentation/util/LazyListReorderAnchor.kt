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

package com.github.yumeyucca.yumebox.presentation.util

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.*
import kotlin.math.abs

@Stable
private data class LazyListTopAnchor(
    val key: String,
    val offset: Int,
)

@Stable
private data class LazyGridTopAnchor(
    val key: String,
    val offset: Int,
)

@Composable
fun KeepLazyListTopAnchorOnReorder(
    listState: LazyListState,
    itemKeys: List<String>,
    enabled: Boolean,
    scrollToTopOnEnabled: Boolean = false,
) {
    var pendingAnchor by remember(listState) { mutableStateOf<LazyListTopAnchor?>(null) }
    var previousEnabled by remember(listState) { mutableStateOf(enabled) }

    LaunchedEffect(enabled, scrollToTopOnEnabled, itemKeys) {
        when {
            scrollToTopOnEnabled && enabled && !previousEnabled && itemKeys.isNotEmpty() -> {
                listState.scrollToItem(0)
            }

            !scrollToTopOnEnabled && enabled && pendingAnchor != null -> {
                val anchor = pendingAnchor ?: return@LaunchedEffect
                val targetIndex = itemKeys.indexOf(anchor.key)
                if (targetIndex >= 0) {
                    val topNow = listState.captureTopAnchor()
                    if (topNow == null || topNow.key != anchor.key || abs(topNow.offset - anchor.offset) > 1) {
                        listState.scrollToItem(targetIndex, anchor.offset)
                    }
                }
                pendingAnchor = null
            }
        }
        previousEnabled = enabled
    }

    DisposableEffect(listState, itemKeys, enabled) {
        onDispose {
            if (enabled && itemKeys.isNotEmpty() && !scrollToTopOnEnabled) {
                pendingAnchor = listState.captureTopAnchor()
            }
        }
    }
}

private fun LazyListState.captureTopAnchor(): LazyListTopAnchor? {
    val layoutInfo = layoutInfo
    val topItem = layoutInfo.visibleItemsInfo.firstOrNull() ?: return null
    val key = topItem.key as? String ?: return null
    val offset = (topItem.offset - layoutInfo.viewportStartOffset).coerceAtLeast(0)
    return LazyListTopAnchor(key = key, offset = offset)
}

/**
 * 网格版的重排锚定:测速重排网格时,落在延迟顺序里的还是原来那一张卡,而不是原地换一批卡。 排序完成后 [scrollToTopOnEnabled] 会平滑回到顶部,把结果从第一行开始呈现。
 * 键表要包含网格里的固定前置项,`indexOf` 才能换算成真实的网格下标。
 */
@Composable
fun KeepLazyGridTopAnchorOnReorder(
    gridState: LazyGridState,
    itemKeys: List<String>,
    enabled: Boolean,
    scrollToTopOnEnabled: Boolean = false,
) {
    var pendingAnchor by remember(gridState) { mutableStateOf<LazyGridTopAnchor?>(null) }
    var previousEnabled by remember(gridState) { mutableStateOf(enabled) }

    LaunchedEffect(enabled, scrollToTopOnEnabled, itemKeys) {
        when {
            scrollToTopOnEnabled && enabled && !previousEnabled && itemKeys.isNotEmpty() -> {
                gridState.animateScrollToItem(0)
            }

            !scrollToTopOnEnabled && enabled && pendingAnchor != null -> {
                val anchor = pendingAnchor ?: return@LaunchedEffect
                val targetIndex = itemKeys.indexOf(anchor.key)
                if (targetIndex >= 0) {
                    val topNow = gridState.captureTopAnchor()
                    if (topNow == null || topNow.key != anchor.key || abs(topNow.offset - anchor.offset) > 1) {
                        gridState.scrollToItem(targetIndex, anchor.offset)
                    }
                }
                pendingAnchor = null
            }
        }
        previousEnabled = enabled
    }

    DisposableEffect(gridState, itemKeys, enabled) {
        onDispose {
            if (enabled && itemKeys.isNotEmpty() && !scrollToTopOnEnabled) {
                pendingAnchor = gridState.captureTopAnchor()
            }
        }
    }
}

private fun LazyGridState.captureTopAnchor(): LazyGridTopAnchor? {
    val topItem = layoutInfo.visibleItemsInfo.minByOrNull { it.offset.y } ?: return null
    val key = topItem.key as? String ?: return null
    return LazyGridTopAnchor(key = key, offset = firstVisibleItemScrollOffset.coerceAtLeast(0))
}
