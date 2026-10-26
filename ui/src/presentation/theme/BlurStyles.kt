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

@file:Suppress("ConstPropertyName", "FunctionName")

package com.github.yumeyucca.yumebox.presentation.theme

import androidx.compose.foundation.background
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.textureBlur

/**
 * Captured page backdrop feeding every glass surface (bottom bar, Moe sidebar). Hosts attach
 * [top.yukonga.miuix.kmp.blur.layerBackdrop] with this instance and provide it; `null` disables
 * glass and every consumer falls back to a solid surface.
 *
 * Glass consumers must live OUTSIDE the subtree the backdrop captures (the main pager): a blur
 * node sampling a backdrop recorded over its own ancestors makes HyperOS hwui corrupt the
 * RenderNode tree during prepareTree (SIGSEGV, RenderThread). The floating delay-test FAB on the
 * node page therefore uses solid chrome instead of sampling [LocalGlassBackdrop].
 */
val LocalGlassBackdrop = compositionLocalOf<LayerBackdrop?> { null }

/**
 * Per-route backdrop captured by `GlassScaffold` over the scaffold body; the top bar samples it
 * for progressive blur. The top bar composes in the Scaffold's topBar slot, OUTSIDE the captured
 * body subtree, so the HyperOS same-subtree corruption constraint is respected. `null` (screens
 * not hosted in GlassScaffold, e.g. the Moe home chrome) keeps the scrim-only top bar.
 */
val LocalTopBarBackdrop = compositionLocalOf<LayerBackdrop?> { null }

/**
 * Blur & glass plumbing on top of `miuix-blur`. Device without RuntimeShader (Android < 33)
 * render every glass surface as its solid fallback — never raw see-through.
 */
object YumeBlur {
    /** Default glass blur radius (dp-as-float, miuix-blur convention, clamped to 150). */
    const val GlassBlurRadius = 26f

    /** NexioSchedule-aligned height of a sheet's top gradient fade band. */
    val SheetTopFadeHeight = 76.dp

    /**
     * Progressive top scrim mapped onto the top bar itself. Depth matched to the user's reference
     * screenshot (title crisp, scrolled content under the bar washed to near-illegible): near-solid
     * at the status-bar edge, held strong across the bar body, with the fade concentrated in the
     * last ~10% so it still reads progressive. The fade runs **strictly inside the bar bounds** —
     * transparent exactly at the bar's bottom edge, nothing drawn below it. User-approved depth
     * (2026-10): near-opaque hold, deeper than the original tuning.
     */
    fun Modifier.hyperOsTopScrim(color: Color): Modifier = drawBehind {
        if (size.height <= 0f) return@drawBehind
        drawRect(
            brush =
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to color.copy(alpha = 0.99f),
                        0.78f to color.copy(alpha = 0.99f),
                        0.89f to color.copy(alpha = 0.96f),
                        0.95f to color.copy(alpha = 0.55f),
                        1.0f to color.copy(alpha = 0f),
                    ),
                    startY = 0f,
                    endY = size.height,
                ),
        )
    }


    /**
     * Frosted-glass surface blurring the page behind [backdrop]; solid [fallbackColor] when the
     * backdrop is absent or the device has no RuntimeShader support. [tint] is blended over the
     * blur (NexioSchedule-style translucent surface tint).
     */
    fun Modifier.glassSurface(
        backdrop: LayerBackdrop?,
        shape: Shape,
        fallbackColor: Color,
        blurRadius: Float = GlassBlurRadius,
        tint: Color? = null,
    ): Modifier =
        if (backdrop != null && isRuntimeShaderSupported()) {
            textureBlur(
                backdrop = backdrop,
                shape = shape,
                blurRadius = blurRadius,
                colors =
                    if (tint != null) {
                        BlurColors(blendColors = listOf(BlendColorEntry(color = tint)))
                    } else {
                        BlurColors()
                    },
            )
        } else {
            background(fallbackColor, shape)
        }

    /**
     * NexioSchedule sheet-top fade: full sheet color at the very top fading to transparent over
     * [SheetTopFadeHeight]. Overlay it above content that scrolls under the sheet header
     * (zIndex) — the HyperOS "content sinks into the surface" look.
     */
    fun sheetTopFadeBrush(sheetColor: Color): Brush =
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.0f to sheetColor,
                0.2f to sheetColor.copy(alpha = 0.95f),
                0.5f to sheetColor.copy(alpha = 0.8f),
                0.8f to sheetColor.copy(alpha = 0.45f),
                1.0f to sheetColor.copy(alpha = 0f),
            ),
        )
}
