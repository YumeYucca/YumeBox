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

@file:Suppress("UnusedSymbol")

package com.github.yumeyucca.yumebox.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

private data class ThemeColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryVariant: Color,
    val onPrimaryVariant: Color,
    val disabledPrimary: Color,
    val disabledOnPrimary: Color,
    val disabledPrimaryButton: Color,
    val disabledOnPrimaryButton: Color,
    val disabledPrimarySlider: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val tertiaryContainerVariant: Color,
    val onBackgroundVariant: Color,
)

private data class ThemePalette(
    val light: ThemeColors,
    val dark: ThemeColors,
)

const val DEFAULT_THEME_SEED_ARGB: Long = 0xFF016888L

data class LogLevelColors(
    val debug: Color = Color(0xFF9E9E9E),
    val warning: Color = Color(0xFFFF9800),
    val error: Color = Color(0xFFF44336),
    val neutral: Color = Color(0xFF9E9E9E),
)

data class LatencyColors(
    val fast: Color = Color(0xFF007906),
    val moderate: Color = Color(0xFFFFB300),
    val slow: Color = Color(0xFFE53935),
    val timeout: Color = Color(0xFF9E9E9E),
)

data class ProtocolColors(
    val tcp: Color = Color(0xFF2196F3),
    val udp: Color = Color(0xFF4CAF50),
    val http: Color = Color(0xFF9E9E9E),
    val https: Color = Color(0xFF00BCD4),
    val unknown: Color = Color(0xFF9E9E9E),
)

data class ConnectionColors(
    val chainArrow: Color = Color(0xFF6B7280),
    val chainInactiveText: Color = Color(0xFF6B7280),
    val chainActive: Color = Color(0xFF00BFA5),
)

data class StatusColors(
    val destructive: Color = Color(0xFFFF3B30),
    val destructiveContainer: Color = Color(0x1AFF3B30),
)

data class StateColors(
    val subtleDivider: Color = Color(0xFFC7C7CC),
    val neutralPlaceholderBackground: Color = Color(0xFFE0E0E0),
    /** Neutral container behind selectable profile / node chrome. */
    val inactiveSurface: ToneColors = ToneColors(light = Color(0xFFF7F7F7), dark = Color(0xFF131415)),
)

/** A palette entry that resolves to a different colour per theme brightness. */
data class ToneColors(
    val light: Color,
    val dark: Color,
)

data class MoeColors(
    val pingExcellent: Color = Color(0xFF0E7A34),
    val pingWarning: Color = Color(0xFFB87900),
)

/**
 * Colours the text-selection action menu paints over the code surface. The editor's own syntax/background palette lives
 * in `feature/editor` (CursorPalette); do not mirror it here.
 */
data class EditorColors(
    val darkTextActionBackground: Color = Color(0xFF2D2D2D),
    val darkTextActionIcon: Color = Color(0xFFD4D4D4),
    val lightTextActionBackground: Color = Color(0xFFF0F0F0),
    val lightTextActionIcon: Color = Color(0xFF333333),
)

data class AppColors(
    val logLevel: LogLevelColors = LogLevelColors(),
    val latency: LatencyColors = LatencyColors(),
    val protocol: ProtocolColors = ProtocolColors(),
    val connection: ConnectionColors = ConnectionColors(),
    val status: StatusColors = StatusColors(),
    val state: StateColors = StateColors(),
    val title: ToneColors = ToneColors(light = Color(0xFF636363), dark = Color(0xFF9E9E9E)),
    val moe: MoeColors = MoeColors(),
    val editor: EditorColors = EditorColors(),
)

/** 当前主题是否处于暗色模式（以背景亮度为准）。 */
@Composable fun isDarkTheme(): Boolean = isDarkSurface(MiuixTheme.colorScheme.background)

/**
 * 绘制在 [color] 上的内容是否需要浅色前景。
 *
 * 用于任意表面色（例如用户选定的强调色、壁纸推导色、首页内容底色）。若该颜色本身就是主题背景， 请改用 [isDarkTheme]。
 */
fun isDarkSurface(color: Color): Boolean = color.luminance() < 0.5f

/** 按当前主题明暗取用该调色板条目。 */
@Composable fun ToneColors.resolve(): Color = if (isDarkTheme()) dark else light

/** 可选卡片（配置 / 节点）共用的中性底色。 */
@Composable fun inactiveSurfaceColor(): Color = AppTheme.colors.state.inactiveSurface.resolve()

private fun ThemeColors.toLightScheme() =
    lightColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryVariant = primaryVariant,
        onPrimaryVariant = onPrimaryVariant,
        disabledPrimary = disabledPrimary,
        disabledOnPrimary = disabledOnPrimary,
        disabledPrimaryButton = disabledPrimaryButton,
        disabledOnPrimaryButton = disabledOnPrimaryButton,
        disabledPrimarySlider = disabledPrimarySlider,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        tertiaryContainerVariant = tertiaryContainerVariant,
        onBackgroundVariant = onBackgroundVariant,
    )

private fun ThemeColors.toDarkScheme() =
    darkColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryVariant = primaryVariant,
        onPrimaryVariant = onPrimaryVariant,
        disabledPrimary = disabledPrimary,
        disabledOnPrimary = disabledOnPrimary,
        disabledPrimaryButton = disabledPrimaryButton,
        disabledOnPrimaryButton = disabledOnPrimaryButton,
        disabledPrimarySlider = disabledPrimarySlider,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        tertiaryContainerVariant = tertiaryContainerVariant,
        onBackgroundVariant = onBackgroundVariant,
    )

private fun ThemeColors.toColorScheme(isDark: Boolean) = if (isDark) toDarkScheme() else toLightScheme()

private val basePalette =
    ThemePalette(
        light =
            ThemeColors(
                primary = Color(0xFF000000),
                onPrimary = Color.White,
                primaryVariant = Color(0xFF222222),
                onPrimaryVariant = Color(0xFFAAAAAA),
                disabledPrimary = Color(0xFFBDBDBD),
                disabledOnPrimary = Color(0xFFE0E0E0),
                disabledPrimaryButton = Color(0xFFBDBDBD),
                disabledOnPrimaryButton = Color(0xFFEEEEEE),
                disabledPrimarySlider = Color(0xFFDCDCDC),
                primaryContainer = Color(0xFFF0F0F0),
                onPrimaryContainer = Color(0xFF000000),
                tertiaryContainer = Color(0xFFF8F8F8),
                onTertiaryContainer = Color(0xFF000000),
                tertiaryContainerVariant = Color(0xFFF8F8F8),
                onBackgroundVariant = Color(0xFF000000),
            ),
        dark =
            ThemeColors(
                primary = Color.White,
                onPrimary = Color(0xFF000000),
                primaryVariant = Color(0xFFE0E0E0),
                onPrimaryVariant = Color(0xFF555555),
                disabledPrimary = Color(0xFF333333),
                disabledOnPrimary = Color(0xFF757575),
                disabledPrimaryButton = Color(0xFF333333),
                disabledOnPrimaryButton = Color(0xFF757575),
                disabledPrimarySlider = Color(0xFF444444),
                primaryContainer = Color(0xFF252525),
                onPrimaryContainer = Color.White,
                tertiaryContainer = Color(0xFF1C1C1C),
                onTertiaryContainer = Color.White,
                tertiaryContainerVariant = Color(0xFF303030),
                onBackgroundVariant = Color(0xFFE0E0E0),
            ),
    )

/**
 * Builds the miuix colour scheme for [seed].
 *
 * With [monetAdaptive] off this is exactly the historical seed theme: only primary and its family move, every neutral
 * surface keeps the miuix default. With it on, the surfaces, outlines and neutral text roles are replaced by the
 * tokens [generateMonetTokens] derives from the same seed. Primary keeps the seed theme's derivation, but the generator
 * picks its foreground: it contrast-corrects the fill against the on-colour it will actually carry, including the
 * black/white swap of [invertOnPrimaryColors].
 */
fun colorSchemeFromSeed(
    seed: Color,
    isDark: Boolean,
    invertOnPrimaryColors: Boolean = false,
    monetAdaptive: Boolean = false,
): Colors =
    if (monetAdaptive) {
        // The generator has to know about the inversion before it searches, otherwise it balances the fill against the
        // foreground the scheme later throws away.
        val monetTokens = generateMonetTokens(seed, isDark, invertOnPrimaryColors)
        schemeFromSeed(seed, isDark, invertOnPrimaryColors, monetTokens).withMonetTokens(monetTokens)
    } else {
        schemeFromSeed(seed, isDark, invertOnPrimaryColors, monetTokens = null)
    }

/**
 * Pre-[monetAdaptive] entry point, kept so already-compiled callers still link.
 *
 * [DeprecationLevel.HIDDEN] keeps it out of source resolution — a three-argument call site resolves to the default
 * argument of the current function, so there is no ambiguity — while its descriptor stays in the bytecode.
 */
@Deprecated("Binary compatibility", level = DeprecationLevel.HIDDEN)
fun colorSchemeFromSeed(seed: Color, isDark: Boolean, invertOnPrimaryColors: Boolean): Colors =
    colorSchemeFromSeed(seed, isDark, invertOnPrimaryColors, monetAdaptive = false)

private fun schemeFromSeed(
    seed: Color,
    isDark: Boolean,
    invertOnPrimaryColors: Boolean,
    monetTokens: MonetSurfaceTokens?,
): Colors =
    deriveThemeColors(
            base = if (isDark) basePalette.dark else basePalette.light,
            seed = seed,
            dark = isDark,
            invertOnPrimaryColors = invertOnPrimaryColors,
            monetTokens = monetTokens,
        )
        .toColorScheme(isDark)

/**
 * Overrides the surface, outline and neutral text roles of this scheme with [tokens].
 *
 * Only roles that describe a surface, a border or neutral text are replaced: primary and its whole derived family are
 * already built from the generated primary in [deriveThemeColors], and the semantic error roles plus the slider and
 * window-dimming roles stay untouched so status colours keep their fixed meaning.
 */
private fun Colors.withMonetTokens(tokens: MonetSurfaceTokens): Colors =
    copy(
        background = tokens.canvas,
        onBackground = tokens.onSurface,
        // The accent-coloured label role is neutral text in the design document (§7), not a seeded accent.
        onBackgroundVariant = tokens.onSurfaceVariant,
        surface = tokens.surface,
        onSurface = tokens.onSurface,
        surfaceVariant = tokens.surface,
        onSurfaceSecondary = tokens.onSurface.copy(alpha = 0.8f),
        onSurfaceVariantSummary = tokens.onSurfaceVariant,
        onSurfaceVariantActions = tokens.onSurfaceVariant,
        onSurfaceContainerVariant = tokens.mutedText,
        disabledOnSurface = tokens.disabledText,
        surfaceContainer = tokens.surfaceContainer,
        onSurfaceContainer = tokens.onSurface,
        surfaceContainerHigh = tokens.surfaceContainerHigh,
        onSurfaceContainerHigh = tokens.onSurface,
        surfaceContainerHighest = tokens.surfaceContainerHighest,
        onSurfaceContainerHighest = tokens.onSurface,
        outline = tokens.outline,
        dividerLine = tokens.outlineSoft,
        secondary = tokens.outlineSoft,
        onSecondary = tokens.onSurface,
        secondaryVariant = tokens.surfaceContainerHigh,
        onSecondaryVariant = tokens.onSurface,
        secondaryContainer = tokens.surfaceContainer,
        onSecondaryContainer = tokens.onSurfaceVariant,
        secondaryContainerVariant = tokens.surfaceContainerHighest,
        onSecondaryContainerVariant = tokens.onSurfaceVariant,
    )

fun colorFromArgb(argb: Long): Color = Color(argb.toInt())

fun colorToArgbLong(color: Color): Long = color.toArgb().toLong()

private fun deriveThemeColors(
    base: ThemeColors,
    seed: Color,
    dark: Boolean,
    invertOnPrimaryColors: Boolean,
    monetTokens: MonetSurfaceTokens? = null,
): ThemeColors {
    // Adaptive mode re-bases the whole primary family on the contrast-corrected fill, so the derived variants keep
    // matching the primary the user actually sees instead of the raw seed.
    val mixSeed = monetTokens?.primary ?: seed
    val primary = monetTokens?.primary ?: if (dark) seed else seed.mix(Color.Black, 0.05f)
    val primaryVariant = if (dark) mixSeed.mix(Color.White, 0.36f) else mixSeed.mix(Color.White, 0.25f)

    val defaultOnPrimary =
        monetTokens?.onPrimary ?: primary.autoOnColor(threshold = if (dark) 0.72f else 0.52f)
    // The variants mixed off the fill are light enough that a luminance threshold can hand back a white label with
    // less than 4.5:1 on them, so the adaptive path picks the label by measured contrast instead.
    val defaultOnPrimaryVariant =
        if (monetTokens != null) {
            primaryVariant.contrastOnColor()
        } else {
            primaryVariant.autoOnColor(threshold = if (dark) 0.68f else 0.52f)
        }

    val disabledPrimary =
        if (dark) {
            base.disabledPrimary.mix(mixSeed, 0.18f)
        } else {
            base.disabledPrimary.mix(mixSeed, 0.14f)
        }
    val disabledOnPrimary =
        if (dark) {
            base.disabledOnPrimary.mix(mixSeed, 0.08f)
        } else {
            base.disabledOnPrimary.mix(mixSeed, 0.06f)
        }
    val disabledPrimaryButton =
        if (dark) {
            base.disabledPrimaryButton.mix(mixSeed, 0.16f)
        } else {
            base.disabledPrimaryButton.mix(mixSeed, 0.12f)
        }
    val disabledOnPrimaryButton =
        if (dark) {
            base.disabledOnPrimaryButton.mix(mixSeed, 0.06f)
        } else {
            base.disabledOnPrimaryButton.mix(mixSeed, 0.05f)
        }
    val disabledPrimarySlider =
        if (dark) {
            base.disabledPrimarySlider.mix(mixSeed, 0.14f)
        } else {
            base.disabledPrimarySlider.mix(mixSeed, 0.10f)
        }

    val primaryContainer =
        monetTokens?.primaryContainer
            ?: if (dark) {
                base.primaryContainer.mix(mixSeed, 0.18f)
            } else {
                base.primaryContainer.mix(mixSeed, 0.14f)
            }

    val tertiaryContainer =
        if (dark) {
            base.tertiaryContainer.mix(mixSeed, 0.13f)
        } else {
            base.tertiaryContainer.mix(mixSeed, 0.16f)
        }

    val tertiaryContainerVariant =
        if (dark) {
            base.tertiaryContainerVariant.mix(mixSeed, 0.15f)
        } else {
            base.tertiaryContainerVariant.mix(mixSeed, 0.13f)
        }

    // Same crossover as the primary variant: the light container sits near the white end, where a luminance threshold
    // can still hand back a white label, so the adaptive path picks it by measured contrast instead.
    val defaultOnPrimaryContainer =
        if (monetTokens != null) {
            primaryContainer.contrastOnColor()
        } else {
            primaryContainer.autoOnColor()
        }
    val onTertiaryContainer = tertiaryContainer.autoOnColor()

    val onPrimary =
        if (monetTokens != null) {
            // Already the effective foreground: the generator inverted it before correcting the fill against it.
            defaultOnPrimary
        } else if (invertOnPrimaryColors) {
            defaultOnPrimary.invertBlackWhite()
        } else {
            defaultOnPrimary
        }
    val onPrimaryVariant =
        if (monetTokens != null) {
            // Picked by contrast against the fill, so inverting it would only walk away from the readable end.
            defaultOnPrimaryVariant
        } else if (invertOnPrimaryColors) {
            defaultOnPrimaryVariant.invertBlackWhite()
        } else {
            defaultOnPrimaryVariant
        }
    val onPrimaryContainer =
        if (monetTokens != null) {
            // Picked by contrast against the fill, so inverting it would only walk away from the readable end.
            defaultOnPrimaryContainer
        } else if (invertOnPrimaryColors) {
            defaultOnPrimaryContainer.invertBlackWhite()
        } else {
            defaultOnPrimaryContainer
        }
    val disabledOnPrimaryFinal =
        if (invertOnPrimaryColors) {
            disabledOnPrimary.invertLuminance()
        } else {
            disabledOnPrimary
        }
    val disabledOnPrimaryButtonFinal =
        if (invertOnPrimaryColors) {
            disabledOnPrimaryButton.invertLuminance()
        } else {
            disabledOnPrimaryButton
        }

    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryVariant = primaryVariant,
        onPrimaryVariant = onPrimaryVariant,
        disabledPrimary = disabledPrimary,
        disabledOnPrimary = disabledOnPrimaryFinal,
        disabledPrimaryButton = disabledPrimaryButton,
        disabledOnPrimaryButton = disabledOnPrimaryButtonFinal,
        disabledPrimarySlider = disabledPrimarySlider,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        tertiaryContainerVariant = tertiaryContainerVariant,
        onBackgroundVariant = if (dark) seed.mix(Color.White, 0.25f) else seed.mix(Color.Black, 0.18f),
    )
}

private fun Color.mix(other: Color, ratio: Float): Color {
    val t = ratio.coerceIn(0f, 1f)
    return Color(
        red = red + (other.red - red) * t,
        green = green + (other.green - green) * t,
        blue = blue + (other.blue - blue) * t,
        alpha = alpha + (other.alpha - alpha) * t,
    )
}

internal fun Color.autoOnColor(threshold: Float = 0.52f): Color =
    if (luminance() > threshold) Color.Black else Color.White

/**
 * Black or white, whichever reaches the higher WCAG contrast ratio against this fill.
 *
 * Contrast derived from measured luminance, unlike [autoOnColor]'s threshold on Compose's perceptual luminance: the two
 * agree on most colours but not near the crossover, which is exactly where a variant fill sits.
 */
private fun Color.contrastOnColor(): Color {
    val fillLuminance = wcagLuminance(red, green, blue)
    return if (contrastRatio(fillLuminance, 1f) >= contrastRatio(fillLuminance, 0f)) Color.White else Color.Black
}

private fun Color.invertBlackWhite(): Color = if (this == Color.Black) Color.White else Color.Black

private fun Color.invertLuminance(): Color = Color(1f - red, 1f - green, 1f - blue, alpha)
