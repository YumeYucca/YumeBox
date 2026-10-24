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

@file:Suppress("UnusedSymbol", "FunctionName")

package com.github.yumeyucca.yumebox.presentation.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Luminous hairline rim along the inside of [shape], in the NexioSchedule bottom-bar style,
 * replacing a dark hairline border. The stroke color is derived from the panel's own background
 * color (near-neutral colors fall back to white) and is drawn with an additive blend, so it reads
 * as a light rim instead of a solid line. [blurRadius] softens the rim with a wider, fainter
 * halo stroke underneath, approximating NexioSchedule's BlurMaskFilter without framework paints.
 */
fun Modifier.edgeLight(
    shape: Shape,
    color: Color,
    width: Dp,
    blurRadius: Dp = EdgeLightDefaults.BlurRadius,
    blendMode: BlendMode = BlendMode.Plus,
): Modifier = drawWithContent {
    drawContent()
    val outline = shape.createOutline(size, layoutDirection, this)
    val strokeWidthPx = width.toPx() * 2f
    if (strokeWidthPx <= 0f) return@drawWithContent
    // Strokes straddle the outline centerline; clipping to the outline keeps only the inner
    // half, so the visible rim width equals [width].
    val rimClipPath = outline.toClipPath()
    val canvas = drawContext.canvas
    clipPath(rimClipPath) {
        canvas.drawOutline(
            outline,
            paint =
                strokePaint(
                    color = color.copy(alpha = color.alpha * HaloAlphaFactor),
                    strokeWidth = strokeWidthPx + blurRadius.toPx() * 4f,
                    blendMode = blendMode,
                ),
        )
        canvas.drawOutline(
            outline,
            paint = strokePaint(color = color, strokeWidth = strokeWidthPx, blendMode = blendMode),
        )
    }
}

private fun strokePaint(color: Color, strokeWidth: Float, blendMode: BlendMode): Paint =
    Paint().apply {
        this.color = color
        style = PaintingStyle.Stroke
        this.strokeWidth = strokeWidth
        this.blendMode = blendMode
    }

/** Clip [Path] for an [Outline], so the stroke can be clipped to the shape it follows. */
private fun Outline.toClipPath(): Path =
    when (this) {
        is Outline.Rounded -> Path().apply { addRoundRect(roundRect) }
        is Outline.Generic -> path
        is Outline.Rectangle -> Path().apply { addRect(rect) }
    }

/** Bottom-bar edge light defaults, aligned with NexioSchedule's `rememberDefaultEdgeLight`. */
object EdgeLightDefaults {
    val WidthLight: Dp = 0.26.dp
    val WidthDark: Dp = 0.4.dp
    val BlurRadius: Dp = 0.8.dp
    const val AlphaLight = 0.24f
    const val AlphaDark = 0.2f

    fun width(isLightTheme: Boolean): Dp = if (isLightTheme) WidthLight else WidthDark

    fun blendMode(isLightTheme: Boolean): BlendMode =
        if (isLightTheme) BlendMode.Plus else BlendMode.Screen

    /** Rim color = the base color with boosted saturation and capped value; neutrals go white. */
    fun tint(baseColor: Color, isLightTheme: Boolean): Color =
        highlightTint(baseColor, if (isLightTheme) AlphaLight else AlphaDark)
}

/** Alpha multiplier of the soft halo stroke relative to the crisp rim. */
private const val HaloAlphaFactor = 0.35f

/** Boosts saturation by [SATURATION_BOOST] while scaling value down to [HIGHLIGHT_VALUE]. */
private fun highlightTint(baseColor: Color, alpha: Float): Color {
    if (baseColor.alpha <= 0f) return Color.White.copy(alpha = alpha)
    val v = maxOf(baseColor.red, maxOf(baseColor.green, baseColor.blue))
    val minC = minOf(baseColor.red, minOf(baseColor.green, baseColor.blue))
    val saturation = if (v > 0f) (v - minC) / v else 0f
    if (saturation < NEUTRAL_SATURATION_EPS) return Color.White.copy(alpha = alpha)
    fun boost(channel: Float) =
        ((v - (v - channel) * SATURATION_BOOST) / v * HIGHLIGHT_VALUE).coerceIn(0f, 1f)
    val vivid =
        Color(
            red = boost(baseColor.red),
            green = boost(baseColor.green),
            blue = boost(baseColor.blue),
        )
    return vivid.copy(alpha = alpha)
}

/** Saturation multiplier applied to the rim tint relative to the base color. */
private const val SATURATION_BOOST = 2.5f

/** Value cap for the rim tint: lower is denser/darker, higher is brighter. */
private const val HIGHLIGHT_VALUE = 0.4f

/** Base colors below this saturation are treated as neutral and get a plain white rim. */
private const val NEUTRAL_SATURATION_EPS = 0.02f
