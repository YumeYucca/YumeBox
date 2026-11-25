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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isUnspecified

/**
 * The surface system generated from one seed colour, in the vocabulary of the Monet design document.
 *
 * [canvas] is the page background, [surface] carries content, [surfaceContainer] / [surfaceContainerHigh] /
 * [surfaceContainerHighest] are the rising container levels and the three outline roles sit underneath them. The four
 * text roles are the fixed neutrals: they describe the surface system and never follow the seed.
 */
data class MonetSurfaceTokens(
    val canvas: Color,
    val surface: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val outlineSoft: Color,
    val outline: Color,
    val outlineStrong: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val mutedText: Color,
    val disabledText: Color,
)

/** WCAG AA contrast the primary fill has to reach against its on-colour. */
private const val MIN_PRIMARY_CONTRAST = 4.5f

/** Chroma cap for the primary fill: a seed may be far more saturated than an interactive colour should be. */
private const val MAX_PRIMARY_CHROMA = 0.18f

/** Lightness the primary search walks towards when the on-colour is light; darkening raises contrast against white. */
private const val DARKENING_TARGET_LIGHTNESS = 0f

/** Lightness the primary search walks towards when the on-colour is dark; lightening raises contrast against it. */
private const val LIGHTENING_TARGET_LIGHTNESS = 1f

/** Coarse scan resolution of the primary lightness search. */
private const val CONTRAST_SEARCH_STEPS = 64

/** Bisection steps used to tighten the coarse scan onto the 4.5:1 boundary. */
private const val CONTRAST_REFINE_STEPS = 20

/** Fixed neutral text colours of the light scheme (design document §7.1). */
private val LightOnSurface = Color(0xFF202A35)
private val LightOnSurfaceVariant = Color(0xFF65717E)
private val LightMutedText = Color(0xFF89939D)
private val LightDisabledText = Color(0xFFA6AFB8)

/** Fixed neutral text colours of the dark scheme (design document §7.2). */
private val DarkOnSurface = Color(0xFFEEF2F6)
private val DarkOnSurfaceVariant = Color(0xFFB1BCC7)
private val DarkMutedText = Color(0xFF8995A1)
private val DarkDisabledText = Color(0xFF697582)

/** Foreground painted on the primary fill: white in light schemes, the near-black slate of §8.4 in dark schemes. */
private val LightOnPrimary = Color(0xFFFFFFFF)
private val DarkOnPrimary = Color(0xFF18212B)

/**
 * One surface rule: a fixed lightness plus a scaled, clamped slice of the seed chroma.
 *
 * @param l perceptual lightness of the generated colour.
 * @param scale fraction of the seed chroma that survives into the surface.
 * @param minC lower clamp of that fraction — even a grey seed leaves a faint tint.
 * @param maxC upper clamp, so a screaming seed cannot paint a saturated page.
 */
private class SurfaceRule(
    val l: Float,
    val scale: Float,
    val minC: Float,
    val maxC: Float,
) {
    fun resolve(seedChroma: Float, hue: Float): Color =
        oklchColor(l, (seedChroma * scale).coerceIn(minC, maxC), hue)
}

private object LightSurfaceRules {
    val Canvas = SurfaceRule(l = 0.975f, scale = 0.10f, minC = 0.006f, maxC = 0.014f)
    val SurfaceContainerHigh = SurfaceRule(l = 0.945f, scale = 0.18f, minC = 0.010f, maxC = 0.022f)
    val SurfaceContainerHighest = SurfaceRule(l = 0.925f, scale = 0.20f, minC = 0.012f, maxC = 0.026f)
    val OutlineSoft = SurfaceRule(l = 0.925f, scale = 0.12f, minC = 0.006f, maxC = 0.016f)
    val Outline = SurfaceRule(l = 0.890f, scale = 0.12f, minC = 0.008f, maxC = 0.018f)
    val OutlineStrong = SurfaceRule(l = 0.840f, scale = 0.14f, minC = 0.010f, maxC = 0.022f)
    val PrimaryContainer = SurfaceRule(l = 0.940f, scale = 0.20f, minC = 0.010f, maxC = 0.026f)
}

private object DarkSurfaceRules {
    val Canvas = SurfaceRule(l = 0.19f, scale = 0.08f, minC = 0.006f, maxC = 0.015f)
    val Surface = SurfaceRule(l = 0.23f, scale = 0.08f, minC = 0.008f, maxC = 0.018f)
    val SurfaceContainer = SurfaceRule(l = 0.26f, scale = 0.12f, minC = 0.010f, maxC = 0.020f)
    val SurfaceContainerHigh = SurfaceRule(l = 0.30f, scale = 0.14f, minC = 0.010f, maxC = 0.022f)
    val SurfaceContainerHighest = SurfaceRule(l = 0.33f, scale = 0.14f, minC = 0.010f, maxC = 0.022f)
    val OutlineSoft = SurfaceRule(l = 0.32f, scale = 0.08f, minC = 0.006f, maxC = 0.015f)
    val Outline = SurfaceRule(l = 0.40f, scale = 0.10f, minC = 0.008f, maxC = 0.018f)
    val OutlineStrong = SurfaceRule(l = 0.48f, scale = 0.10f, minC = 0.008f, maxC = 0.020f)
    val PrimaryContainer = SurfaceRule(l = 0.32f, scale = 0.16f, minC = 0.012f, maxC = 0.030f)
}

/**
 * Generates the Monet surface tokens for [seed] in either [isDark] scheme.
 *
 * Light and dark are generated independently from the same hue rather than being inverses of each other: the light
 * canvas is a near-white carrying a tenth of the seed chroma, the dark canvas a low-chroma deep tone. A seed without
 * usable colour information ([Color.Unspecified] or fully transparent) falls back to [DEFAULT_THEME_SEED_ARGB].
 *
 * [invertOnPrimaryColors] is resolved here rather than in the scheme, because the primary fill is contrast-corrected
 * against the foreground it will actually carry: inverting white to black (light) or the slate to white (dark) flips
 * the direction the lightness search has to walk, so the generator must know about it before it searches.
 */
fun generateMonetTokens(seed: Color, isDark: Boolean, invertOnPrimaryColors: Boolean = false): MonetSurfaceTokens {
    val safeSeed = if (seed.isUnspecified || seed.alpha <= 0f) colorFromArgb(DEFAULT_THEME_SEED_ARGB) else seed
    val seedOklch = oklchFromSrgb(safeSeed.red, safeSeed.green, safeSeed.blue)
    val baseOnPrimary = if (isDark) DarkOnPrimary else LightOnPrimary
    // Inversion is a plain black/white swap of the foreground, so the effective on-colour is the black or white end of
    // the scale — never a mix of the base slate.
    val onPrimary =
        if (invertOnPrimaryColors) {
            if (isDark) Color.White else Color.Black
        } else {
            baseOnPrimary
        }
    return if (isDark) darkTokens(seedOklch, onPrimary) else lightTokens(seedOklch, onPrimary)
}

private fun lightTokens(seed: Oklch, onPrimary: Color): MonetSurfaceTokens {
    val primaryContainer = LightSurfaceRules.PrimaryContainer.resolve(seed.c, seed.h)
    return MonetSurfaceTokens(
        canvas = LightSurfaceRules.Canvas.resolve(seed.c, seed.h),
        // A content surface stays white: only the canvas and the containers below it carry the theme colour.
        surface = Color.White,
        surfaceContainer = Color.White,
        surfaceContainerHigh = LightSurfaceRules.SurfaceContainerHigh.resolve(seed.c, seed.h),
        surfaceContainerHighest = LightSurfaceRules.SurfaceContainerHighest.resolve(seed.c, seed.h),
        outlineSoft = LightSurfaceRules.OutlineSoft.resolve(seed.c, seed.h),
        outline = LightSurfaceRules.Outline.resolve(seed.c, seed.h),
        outlineStrong = LightSurfaceRules.OutlineStrong.resolve(seed.c, seed.h),
        primary = primaryFor(seed, onPrimary),
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = primaryContainer.autoOnColor(),
        onSurface = LightOnSurface,
        onSurfaceVariant = LightOnSurfaceVariant,
        mutedText = LightMutedText,
        disabledText = LightDisabledText,
    )
}

private fun darkTokens(seed: Oklch, onPrimary: Color): MonetSurfaceTokens {
    val primaryContainer = DarkSurfaceRules.PrimaryContainer.resolve(seed.c, seed.h)
    return MonetSurfaceTokens(
        canvas = DarkSurfaceRules.Canvas.resolve(seed.c, seed.h),
        surface = DarkSurfaceRules.Surface.resolve(seed.c, seed.h),
        surfaceContainer = DarkSurfaceRules.SurfaceContainer.resolve(seed.c, seed.h),
        surfaceContainerHigh = DarkSurfaceRules.SurfaceContainerHigh.resolve(seed.c, seed.h),
        surfaceContainerHighest = DarkSurfaceRules.SurfaceContainerHighest.resolve(seed.c, seed.h),
        outlineSoft = DarkSurfaceRules.OutlineSoft.resolve(seed.c, seed.h),
        outline = DarkSurfaceRules.Outline.resolve(seed.c, seed.h),
        outlineStrong = DarkSurfaceRules.OutlineStrong.resolve(seed.c, seed.h),
        primary = primaryFor(seed, onPrimary),
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = primaryContainer.autoOnColor(),
        onSurface = DarkOnSurface,
        onSurfaceVariant = DarkOnSurfaceVariant,
        mutedText = DarkMutedText,
        disabledText = DarkDisabledText,
    )
}

/** Primary fill for [seed]: same hue, chroma capped at [MAX_PRIMARY_CHROMA], lightness corrected for contrast. */
private fun primaryFor(seed: Oklch, onPrimary: Color): Color {
    val chroma = minOf(seed.c, MAX_PRIMARY_CHROMA)
    val lightness = readablePrimaryLightness(seed.l, chroma, seed.h, onPrimary)
    return oklchColor(lightness, chroma, seed.h)
}

/**
 * Keeps the seed lightness when the pair already reaches [MIN_PRIMARY_CONTRAST], and otherwise walks the lightness away
 * from [onPrimary] — towards black under a light foreground, towards white under a dark one — until it does.
 *
 * The walk only moves in the direction that increases contrast against [onPrimary], which is what makes the coarse scan
 * plus the bisection sound, and it never touches the chroma or the hue: the resulting fill is still recognisably the
 * seed rather than a generic dark grey. The direction follows the on-colour rather than the scheme, because an inverted
 * foreground puts a dark on-colour in a light scheme and vice versa.
 */
private fun readablePrimaryLightness(seedLightness: Float, chroma: Float, hue: Float, onPrimary: Color): Float {
    val onPrimaryLuminance = wcagLuminance(onPrimary.red, onPrimary.green, onPrimary.blue)

    fun contrastAt(lightness: Float): Float {
        val rgb = oklchToSrgb(lightness, chroma, hue)
        return contrastRatio(wcagLuminance(rgb[0], rgb[1], rgb[2]), onPrimaryLuminance)
    }

    val target = if (onPrimaryLuminance >= 0.5f) DARKENING_TARGET_LIGHTNESS else LIGHTENING_TARGET_LIGHTNESS
    fun lightnessAt(step: Float): Float = seedLightness + step * (target - seedLightness)

    if (contrastAt(seedLightness) >= MIN_PRIMARY_CONTRAST) return seedLightness

    var hit = CONTRAST_SEARCH_STEPS
    for (step in 1..CONTRAST_SEARCH_STEPS) {
        if (contrastAt(lightnessAt(step.toFloat() / CONTRAST_SEARCH_STEPS)) >= MIN_PRIMARY_CONTRAST) {
            hit = step
            break
        }
    }

    var low = (hit - 1).toFloat() / CONTRAST_SEARCH_STEPS
    var high = hit.toFloat() / CONTRAST_SEARCH_STEPS
    repeat(CONTRAST_REFINE_STEPS) {
        val mid = (low + high) * 0.5f
        if (contrastAt(lightnessAt(mid)) >= MIN_PRIMARY_CONTRAST) high = mid else low = mid
    }
    return lightnessAt(high)
}

private fun oklchColor(lightness: Float, chroma: Float, hue: Float): Color {
    val rgb = oklchToSrgb(lightness, chroma, hue)
    return Color(red = rgb[0], green = rgb[1], blue = rgb[2], alpha = 1f)
}