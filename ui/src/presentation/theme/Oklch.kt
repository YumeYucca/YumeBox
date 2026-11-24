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

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin

/**
 * A colour in OKLCH, the polar form of Björn Ottosson's OKLab space.
 *
 * [l] is the perceptual lightness in `0f..1f`, [c] is the chroma (`0f` is grey) and [h] is the hue angle in degrees,
 * `0f..360f` counter-clockwise from the positive `a` axis. Kept as a plain value type: this file intentionally imports
 * neither Compose nor Android so the conversion and gamut maths can be scripted and checked in isolation.
 */
data class Oklch(
    val l: Float,
    val c: Float,
    val h: Float,
)

/**
 * Hue returned for seeds that are indistinguishable from grey.
 *
 * Below [ACHROMATIC_CHROMA] the OKLab `a`/`b` pair is float noise, so `atan2` would hand back a random angle. Such
 * seeds still receive the minimum chroma the surface rules mandate, so the angle is clamped onto the cool-neutral
 * direction the Mist preset uses as the "no theme feature" fallback instead of letting rounding decide the tint.
 */
private const val ACHROMATIC_HUE = 240f

/** Chroma below which a seed carries no usable hue signal. */
private const val ACHROMATIC_CHROMA = 1e-4f

/** Slack for the in-gamut test; the OKLab matrices are rounded in their last decimals. */
private const val GAMUT_EPSILON = 1e-5f

/** Bisection depth of [gamutMapOklch]; 24 halvings resolve any realistic chroma to float precision. */
private const val GAMUT_ITERATIONS = 24

/**
 * Converts an sRGB triple, each component in `0f..1f`, to OKLCH.
 *
 * sRGB is linearised before the OKLab matrices are applied — skipping that step is the usual way to end up with a
 * "OKLab" that is really just gamma-encoded LMS.
 */
fun oklchFromSrgb(r: Float, g: Float, b: Float): Oklch {
    val red = linearize(r)
    val green = linearize(g)
    val blue = linearize(b)

    val l = 0.4122214708 * red + 0.5363325363 * green + 0.0514459929 * blue
    val m = 0.2119034982 * red + 0.6806995451 * green + 0.1073969566 * blue
    val s = 0.0883024619 * red + 0.2817188376 * green + 0.6299787005 * blue

    val lRoot = cubeRoot(l)
    val mRoot = cubeRoot(m)
    val sRoot = cubeRoot(s)

    val lightness = 0.2104542553 * lRoot + 0.7936177850 * mRoot - 0.0040720468 * sRoot
    val a = 1.9779984951 * lRoot - 2.4285922050 * mRoot + 0.4505937099 * sRoot
    val bAxis = 0.0259040371 * lRoot + 0.7827717662 * mRoot - 0.8086757660 * sRoot

    val chroma = hypot(a, bAxis).toFloat()
    val hue =
        if (chroma < ACHROMATIC_CHROMA) {
            ACHROMATIC_HUE
        } else {
            normalizeHue(atan2(bAxis, a) * 180.0 / PI)
        }

    return Oklch(
        l = lightness.coerceIn(0.0, 1.0).toFloat(),
        c = chroma,
        h = hue,
    )
}

/**
 * Converts OKLCH to an sRGB triple, each component in `0f..1f`.
 *
 * The result always sits inside the sRGB gamut: [gamutMapOklch] has already reduced the chroma, so the trailing
 * `coerceIn` only trims the last-digit overshoot of the round trip and never moves the hue (a per-channel clamp on the
 * unmapped values would).
 */
fun oklchToSrgb(l: Float, c: Float, h: Float): FloatArray {
    val mapped = gamutMapOklch(l, c, h)
    val rgb = rawSrgbFromOklch(mapped.l, mapped.c, mapped.h)
    return floatArrayOf(rgb[0].coerceIn(0f, 1f), rgb[1].coerceIn(0f, 1f), rgb[2].coerceIn(0f, 1f))
}

/**
 * Maps an OKLCH colour into the sRGB gamut by lowering [Oklch.c] only, keeping [Oklch.l] and [Oklch.h] as stable as the
 * gamut allows. In-gamut colours are returned untouched.
 */
fun gamutMapOklch(l: Float, c: Float, h: Float): Oklch {
    val lightness = if (l.isNaN()) 0f else l.coerceIn(0f, 1f)
    val chroma = if (c.isNaN()) 0f else c.coerceAtLeast(0f)
    if (isInSrgbGamut(rawSrgbFromOklch(lightness, chroma, h))) return Oklch(lightness, chroma, h)

    var low = 0f
    var high = chroma
    var best = 0f
    repeat(GAMUT_ITERATIONS) {
        val mid = (low + high) * 0.5f
        if (isInSrgbGamut(rawSrgbFromOklch(lightness, mid, h))) {
            best = mid
            low = mid
        } else {
            high = mid
        }
    }
    return Oklch(lightness, best, h)
}

/** WCAG 2.x relative luminance of an sRGB triple whose components are in `0f..1f`. */
fun wcagLuminance(r: Float, g: Float, b: Float): Float =
    (0.2126 * linearize(r) + 0.7152 * linearize(g) + 0.0722 * linearize(b)).toFloat()

/** WCAG 2.x contrast ratio between two [wcagLuminance] values; ranges from `1f` (none) to `21f` (black on white). */
fun contrastRatio(luminanceA: Float, luminanceB: Float): Float {
    val lighter = maxOf(luminanceA, luminanceB)
    val darker = minOf(luminanceA, luminanceB)
    return (lighter + 0.05f) / (darker + 0.05f)
}

private fun normalizeHue(degrees: Double): Float {
    val wrapped = degrees % 360.0
    return (if (wrapped < 0.0) wrapped + 360.0 else wrapped).toFloat()
}

/** sRGB transfer function: gamma-encoded component to linear light. */
private fun linearize(component: Float): Double {
    val v = (if (component.isNaN()) 0f else component).coerceIn(0f, 1f).toDouble()
    return if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
}

/** Inverse sRGB transfer function: linear light to gamma-encoded component. */
private fun delinearize(component: Double): Float {
    val encoded = if (component <= 0.0031308) component * 12.92 else 1.055 * component.pow(1.0 / 2.4) - 0.055
    return encoded.toFloat()
}

/** Cube root that also works for negative linear-light values, which `pow(1.0 / 3.0)` refuses. */
private fun cubeRoot(value: Double): Double = if (value < 0.0) -(-value).pow(1.0 / 3.0) else value.pow(1.0 / 3.0)

/** Untouched conversion to gamma-encoded sRGB; components may fall outside `0f..1f`. */
private fun rawSrgbFromOklch(l: Float, c: Float, h: Float): FloatArray {
    val radians = h.toDouble() * PI / 180.0
    val a = c.toDouble() * cos(radians)
    val bAxis = c.toDouble() * sin(radians)
    val lightness = l.toDouble()

    val lRoot = lightness + 0.3963377774 * a + 0.2158037573 * bAxis
    val mRoot = lightness - 0.1055613458 * a - 0.0638541728 * bAxis
    val sRoot = lightness - 0.0894841775 * a - 1.2914855480 * bAxis

    val lms = lRoot * lRoot * lRoot
    val mms = mRoot * mRoot * mRoot
    val sms = sRoot * sRoot * sRoot

    return floatArrayOf(
        delinearize(4.0767416621 * lms - 3.3077115913 * mms + 0.2309699292 * sms),
        delinearize(-1.2684380046 * lms + 2.6097574011 * mms - 0.3413193965 * sms),
        delinearize(-0.0041960863 * lms - 0.7034186147 * mms + 1.7076147010 * sms),
    )
}

private fun isInSrgbGamut(rgb: FloatArray): Boolean =
    rgb.all { it >= -GAMUT_EPSILON && it <= 1f + GAMUT_EPSILON }