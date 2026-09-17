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

package com.github.yumeyucca.yumebox.presentation.screen

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import com.github.yumeyucca.yumebox.presentation.theme.AnimationSpecs
import kotlin.math.abs

private suspend fun animateLocateScroll(
    targetIndex: Int,
    isEmpty: Boolean,
    viewportSize: Int,
    beforeContentPadding: Int,
    firstVisibleIndex: Int,
    targetMainAxisOffset: () -> Int?,
    scrollTo: suspend (index: Int) -> Unit,
    scrollByDelta: suspend (delta: Float) -> Float,
    animateBy: suspend (delta: Float, anim: AnimationSpec<Float>) -> Float,
) {
    if (isEmpty) {
        scrollTo(targetIndex)
        return
    }

    val viewport = viewportSize.coerceAtLeast(1).toFloat()
    val pad = beforeContentPadding.toFloat()
    val goingDown = targetIndex > firstVisibleIndex
    val alreadyOnScreen = targetMainAxisOffset() != null

    if (!alreadyOnScreen) {
        scrollTo(targetIndex)
        val pull = viewport * 0.72f * if (goingDown) -1f else 1f
        scrollByDelta(pull)
    }

    val current = targetMainAxisOffset()?.toFloat()
    if (current == null) {
        scrollTo(targetIndex)
        return
    }

    val remaining = current - pad
    if (abs(remaining) < 0.5f) {
        val tick = 28f
        animateBy(tick, tween(durationMillis = 110, easing = AnimationSpecs.EmphasizedAccelerate))
        animateBy(
            -tick,
            spring(dampingRatio = 0.48f, stiffness = Spring.StiffnessMedium),
        )
        return
    }

    val direction = if (remaining > 0f) 1f else -1f
    val overshoot = (abs(remaining) * 0.14f).coerceIn(28f, 88f) * direction

    animateBy(
        remaining + overshoot,
        tween(durationMillis = 520, easing = AnimationSpecs.EmphasizedDecelerate),
    )
    animateBy(
        -overshoot,
        spring(dampingRatio = 0.52f, stiffness = 360f),
    )
}

internal suspend fun LazyListState.animateLocateToItem(targetIndex: Int) {
    animateLocateScroll(
        targetIndex = targetIndex,
        isEmpty = layoutInfo.visibleItemsInfo.isEmpty(),
        viewportSize = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset,
        beforeContentPadding = layoutInfo.beforeContentPadding,
        firstVisibleIndex = firstVisibleItemIndex,
        targetMainAxisOffset = {
            layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIndex }?.offset
        },
        scrollTo = { index -> scrollToItem(index) },
        scrollByDelta = { delta -> scrollBy(delta) },
        animateBy = { delta, anim -> animateScrollBy(delta, anim) },
    )
}

internal suspend fun LazyGridState.animateLocateToItem(targetIndex: Int) {
    animateLocateScroll(
        targetIndex = targetIndex,
        isEmpty = layoutInfo.visibleItemsInfo.isEmpty(),
        viewportSize = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset,
        beforeContentPadding = layoutInfo.beforeContentPadding,
        firstVisibleIndex = firstVisibleItemIndex,
        targetMainAxisOffset = {
            layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIndex }?.offset?.y
        },
        scrollTo = { index -> scrollToItem(index) },
        scrollByDelta = { delta -> scrollBy(delta) },
        animateBy = { delta, anim -> animateScrollBy(delta, anim) },
    )
}
