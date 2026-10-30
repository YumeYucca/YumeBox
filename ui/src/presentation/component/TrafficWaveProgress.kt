/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
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

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.PI
import kotlin.math.sin

@Composable
internal fun TrafficWaveProgress(
    ratio: Float,
    warn: Boolean,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MiuixTheme.colorScheme
    val opacity = AppTheme.opacity

    val waveColor = if (warn) AppTheme.colors.moe.pingWarning else colorScheme.primary
    val trackColor = colorScheme.onSurface.copy(alpha = opacity.subtleStrong)
    val animatedRatio by animateFloatAsState(
        targetValue = ratio.coerceIn(0f, 1f),
        animationSpec = WavyProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "profileTrafficRatio",
    )
    // Phase crawls forward one wavelength per cycle so the wave is visibly alive; both ends
    // taper back to the centerline so the start and the head stay clean at any phase.
    val phase by rememberInfiniteTransition(label = "waveCrawl").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "waveCrawlPhase",
    )

    Canvas(
        modifier =
            modifier
                .height(UiDp.dp12)
                .semantics {
                    progressBarRangeInfo =
                        ProgressBarRangeInfo(current = animatedRatio, range = 0f..1f)
                },
    ) {
        val strokePx = UiDp.dp4.toPx()
        val gapPx = UiDp.dp8.toPx()
        val wavelengthPx = UiDp.dp20.toPx()
        val centerY = size.height / 2f
        val amplitude = (size.height - strokePx) / 2f * 0.9f
        val headX = ((size.width - gapPx) * animatedRatio).coerceAtLeast(0f)

        if (headX > strokePx) {
            val taperLength = (wavelengthPx * 0.6f).coerceAtMost(headX / 2f)
            // Pixel-step sampling: a dp-sized step collapses a 20dp wavelength into a handful of
            // segments and the polyline reads as jagged crests. 2px keeps the sine smooth.
            val stepPx = 2f
            val steps = maxOf(8, (headX / stepPx).toInt())
            val halfPi = (PI / 2).toFloat()
            val twoPi = (PI * 2).toFloat()
            val path = Path()
            for (i in 0..steps) {
                val x = headX * i / steps
                val startTaper = sin((x / taperLength).coerceIn(0f, 1f) * halfPi)
                val headTaper =
                    sin(((headX - x) / taperLength).coerceIn(0f, 1f) * halfPi)
                val y =
                    centerY -
                            amplitude *
                                    startTaper *
                                    headTaper *
                                    sin(twoPi * (x / wavelengthPx - phase))
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = waveColor,
                style =
                    Stroke(
                        width = strokePx,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
            )
        }

        val trackStart = headX + gapPx
        if (trackStart < size.width - strokePx / 2f) {
            drawLine(
                color = trackColor,
                start = Offset(trackStart, centerY),
                end = Offset(size.width, centerY),
                strokeWidth = strokePx,
                cap = StrokeCap.Round,
            )
        }
    }
}
