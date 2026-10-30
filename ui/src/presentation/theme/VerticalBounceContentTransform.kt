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

package com.github.yumeyucca.yumebox.presentation.theme

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset

/** Vertical layered bounce used by tablet split-shell pane switches. */
fun verticalBounceContentTransform(forward: Boolean): ContentTransform {
    val enterSpring =
        spring<IntOffset>(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessLow,
        )
    val exitSpring =
        spring<IntOffset>(
            dampingRatio = 0.92f,
            stiffness = 280f,
        )
    val fadeInSpec = tween<Float>(durationMillis = 340, easing = AnimationSpecs.StandardEasing)
    val fadeOutSpec = tween<Float>(durationMillis = 260, easing = AnimationSpecs.StandardEasing)
    // Forward enters from below and leaves upwards; backward mirrors both by the same factor.
    val direction = if (forward) 1 else -1
    return ContentTransform(
        targetContentEnter =
            slideInVertically(animationSpec = enterSpring) { fullHeight ->
                direction * fullHeight / 10
            } + fadeIn(animationSpec = fadeInSpec),
        initialContentExit =
            slideOutVertically(animationSpec = exitSpring) { fullHeight ->
                -direction * fullHeight / 14
            } + fadeOut(animationSpec = fadeOutSpec),
        sizeTransform = SizeTransform(clip = false),
    )
}
