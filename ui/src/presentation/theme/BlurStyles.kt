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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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
 * The backdrop an anchored glass surface should sample: the page's shared backdrop when the host
 * provides one, else the top bar's. Floating menus ask this instead of spelling the fallback
 * themselves, so "page backdrop first, bar backdrop second" stays one rule.
 */
@Composable
fun glassSurfaceBackdrop(): LayerBackdrop? =
    LocalGlassBackdrop.current ?: LocalTopBarBackdrop.current

/**
 * Blur & glass plumbing on top of `miuix-blur`. Device without RuntimeShader (Android < 33)
 * render every glass surface as its solid fallback — never raw see-through.
 */
object YumeBlur {
    /** Default glass blur radius (dp-as-float, miuix-blur convention, clamped to 150). */
    const val GlassBlurRadius = 26f

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
}
