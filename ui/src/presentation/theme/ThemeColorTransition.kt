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

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import top.yukonga.miuix.kmp.theme.Colors

/**
 * Returns the colour scheme to render now, cross-fading towards [target].
 *
 * The first composition hands [target] straight back, so a cold start never animates from a default palette. Every
 * later change of [target] — a new seed, the light/dark switch, the adaptive-canvas toggle — replays a single
 * [AnimationSpecs.Theme.ColorTransitionDuration] fade whose easing is [AnimationSpecs.EaseInOutCubic], driving every
 * role at once so no widget is left on a stale colour. An interrupted fade restarts from the colours currently on
 * screen instead of jumping. When the system animator duration scale is zero the fade is skipped entirely.
 */
@Composable
internal fun rememberAnimatedThemeColors(target: Colors): Colors {
    val context = LocalContext.current
    val progress = remember { Animatable(1f) }
    val visible = remember { mutableStateOf(target) }

    LaunchedEffect(target) {
        val from = visible.value
        if (from === target) return@LaunchedEffect
        if (!systemAnimationsEnabled(context)) {
            progress.snapTo(1f)
            visible.value = target
            return@LaunchedEffect
        }
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec =
                tween(
                    durationMillis = AnimationSpecs.Theme.ColorTransitionDuration,
                    easing = AnimationSpecs.EaseInOutCubic,
                ),
        ) {
            visible.value = lerpColors(from, target, value)
        }
        visible.value = target
    }

    return visible.value
}

/** Interpolates every colour role of [Colors] between [start] and [stop]; [fraction] is clamped to `0f..1f`. */
internal fun lerpColors(start: Colors, stop: Colors, fraction: Float): Colors {
    val t = fraction.coerceIn(0f, 1f)
    return Colors(
        primary = lerp(start.primary, stop.primary, t),
        onPrimary = lerp(start.onPrimary, stop.onPrimary, t),
        primaryVariant = lerp(start.primaryVariant, stop.primaryVariant, t),
        onPrimaryVariant = lerp(start.onPrimaryVariant, stop.onPrimaryVariant, t),
        error = lerp(start.error, stop.error, t),
        onError = lerp(start.onError, stop.onError, t),
        errorContainer = lerp(start.errorContainer, stop.errorContainer, t),
        onErrorContainer = lerp(start.onErrorContainer, stop.onErrorContainer, t),
        disabledPrimary = lerp(start.disabledPrimary, stop.disabledPrimary, t),
        disabledOnPrimary = lerp(start.disabledOnPrimary, stop.disabledOnPrimary, t),
        disabledPrimaryButton = lerp(start.disabledPrimaryButton, stop.disabledPrimaryButton, t),
        disabledOnPrimaryButton = lerp(start.disabledOnPrimaryButton, stop.disabledOnPrimaryButton, t),
        disabledPrimarySlider = lerp(start.disabledPrimarySlider, stop.disabledPrimarySlider, t),
        primaryContainer = lerp(start.primaryContainer, stop.primaryContainer, t),
        onPrimaryContainer = lerp(start.onPrimaryContainer, stop.onPrimaryContainer, t),
        secondary = lerp(start.secondary, stop.secondary, t),
        onSecondary = lerp(start.onSecondary, stop.onSecondary, t),
        secondaryVariant = lerp(start.secondaryVariant, stop.secondaryVariant, t),
        onSecondaryVariant = lerp(start.onSecondaryVariant, stop.onSecondaryVariant, t),
        disabledSecondary = lerp(start.disabledSecondary, stop.disabledSecondary, t),
        disabledOnSecondary = lerp(start.disabledOnSecondary, stop.disabledOnSecondary, t),
        disabledSecondaryVariant = lerp(start.disabledSecondaryVariant, stop.disabledSecondaryVariant, t),
        disabledOnSecondaryVariant = lerp(start.disabledOnSecondaryVariant, stop.disabledOnSecondaryVariant, t),
        secondaryContainer = lerp(start.secondaryContainer, stop.secondaryContainer, t),
        onSecondaryContainer = lerp(start.onSecondaryContainer, stop.onSecondaryContainer, t),
        secondaryContainerVariant = lerp(start.secondaryContainerVariant, stop.secondaryContainerVariant, t),
        onSecondaryContainerVariant = lerp(start.onSecondaryContainerVariant, stop.onSecondaryContainerVariant, t),
        tertiaryContainer = lerp(start.tertiaryContainer, stop.tertiaryContainer, t),
        onTertiaryContainer = lerp(start.onTertiaryContainer, stop.onTertiaryContainer, t),
        tertiaryContainerVariant = lerp(start.tertiaryContainerVariant, stop.tertiaryContainerVariant, t),
        background = lerp(start.background, stop.background, t),
        onBackground = lerp(start.onBackground, stop.onBackground, t),
        onBackgroundVariant = lerp(start.onBackgroundVariant, stop.onBackgroundVariant, t),
        surface = lerp(start.surface, stop.surface, t),
        onSurface = lerp(start.onSurface, stop.onSurface, t),
        surfaceVariant = lerp(start.surfaceVariant, stop.surfaceVariant, t),
        onSurfaceSecondary = lerp(start.onSurfaceSecondary, stop.onSurfaceSecondary, t),
        onSurfaceVariantSummary = lerp(start.onSurfaceVariantSummary, stop.onSurfaceVariantSummary, t),
        onSurfaceVariantActions = lerp(start.onSurfaceVariantActions, stop.onSurfaceVariantActions, t),
        disabledOnSurface = lerp(start.disabledOnSurface, stop.disabledOnSurface, t),
        surfaceContainer = lerp(start.surfaceContainer, stop.surfaceContainer, t),
        onSurfaceContainer = lerp(start.onSurfaceContainer, stop.onSurfaceContainer, t),
        onSurfaceContainerVariant = lerp(start.onSurfaceContainerVariant, stop.onSurfaceContainerVariant, t),
        surfaceContainerHigh = lerp(start.surfaceContainerHigh, stop.surfaceContainerHigh, t),
        onSurfaceContainerHigh = lerp(start.onSurfaceContainerHigh, stop.onSurfaceContainerHigh, t),
        surfaceContainerHighest = lerp(start.surfaceContainerHighest, stop.surfaceContainerHighest, t),
        onSurfaceContainerHighest = lerp(start.onSurfaceContainerHighest, stop.onSurfaceContainerHighest, t),
        outline = lerp(start.outline, stop.outline, t),
        dividerLine = lerp(start.dividerLine, stop.dividerLine, t),
        windowDimming = lerp(start.windowDimming, stop.windowDimming, t),
        sliderKeyPoint = lerp(start.sliderKeyPoint, stop.sliderKeyPoint, t),
        sliderKeyPointForeground = lerp(start.sliderKeyPointForeground, stop.sliderKeyPointForeground, t),
        sliderBackground = lerp(start.sliderBackground, stop.sliderBackground, t),
    )
}

/**
 * Whether system animations run at all.
 *
 * `ANIMATOR_DURATION_SCALE` is the switch behind "Remove animations" in accessibility settings; it is read when a
 * transition starts rather than observed, because only an actual theme change cares about the answer.
 */
private fun systemAnimationsEnabled(context: Context): Boolean =
    runCatching {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }.getOrDefault(true)