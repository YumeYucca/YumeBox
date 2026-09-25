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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

private const val LeadMillis = 120L
private const val StepMillis = 56L
private const val DurationMillis = 220
private const val StaggerLimit = 8
private const val InitialScale = 0.97f
private val Rise = 8.dp

@Composable
fun rememberRowReveal(itemCount: Int, replayKey: Any? = null): Int {
    var revealed by remember(replayKey) { mutableIntStateOf(0) }
    val latestCount = rememberUpdatedState(itemCount)
    LaunchedEffect(replayKey) {
        val count = snapshotFlow { latestCount.value }.first { it > 0 }
        revealed = 0
        delay(LeadMillis)
        val steps = minOf(count, StaggerLimit)
        for (step in 1..steps) {
            revealed = step
            if (step < steps) delay(StepMillis)
        }
        revealed = Int.MAX_VALUE
    }
    return revealed
}

@Composable
fun rememberRowShown(index: Int, revealCount: Int): Float {
    val shown by animateFloatAsState(
        targetValue = if (index < revealCount) 1f else 0f,
        animationSpec =
            tween(
                durationMillis = DurationMillis,
                easing = AnimationSpecs.EmphasizedDecelerate,
            ),
        label = "rowReveal",
    )
    return shown
}

@Composable
fun Modifier.rowReveal(shown: Float): Modifier {
    val rise = with(LocalDensity.current) { Rise.toPx() }
    return this.graphicsLayer {
        alpha = shown
        val scale = InitialScale + (1f - InitialScale) * shown
        scaleX = scale
        scaleY = scale
        translationY = (1f - shown) * rise
    }
}
