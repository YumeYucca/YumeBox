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

@file:Suppress("FunctionName")

package com.github.yumeyucca.yumebox.presentation.theme


import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import com.github.yumeyucca.yumebox.data.model.ThemeMode
import com.github.yumeyucca.yumebox.core.locale.ProvideYumeStrings
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal val LocalPlatformSystemUiEffect =
    compositionLocalOf<@Composable (isDark: Boolean) -> Unit> { {} }

/**
 * Whether this mode resolves to a dark colour scheme.
 *
 * [ThemeMode.Auto] follows the system; the explicit modes pin the answer so a user who picked
 * "Light" keeps a light UI even when the system is dark. Every surface that needs to know
 * (theme, moe home, system UI) asks this one question instead of re-deriving the mapping.
 */
@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.Auto -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}

/** Display label of the mode, resolved against the current locale on every read. */
val ThemeMode.label: String
    get() = when (this) {
        ThemeMode.Auto -> YumeTxt.AppSettings.Interface.ThemeModeSystem
        ThemeMode.Light -> YumeTxt.AppSettings.Interface.ThemeModeLight
        ThemeMode.Dark -> YumeTxt.AppSettings.Interface.ThemeModeDark
    }

@Composable
fun YumeTheme(
    themeMode: ThemeMode? = null,
    themeSeedColorArgb: Long = DEFAULT_THEME_SEED_ARGB,
    invertOnPrimaryColors: Boolean = false,
    content: @Composable () -> Unit,
) = ProvideYumeStrings {
    val isDark = (themeMode ?: ThemeMode.Auto).isDark()
    val systemUiEffect = LocalPlatformSystemUiEffect.current
    systemUiEffect(isDark)
    val colorScheme =
        remember(isDark, themeSeedColorArgb, invertOnPrimaryColors) {
            colorSchemeFromSeed(
                seed = colorFromArgb(themeSeedColorArgb),
                isDark = isDark,
                invertOnPrimaryColors = invertOnPrimaryColors,
            )
        }

    MiuixTheme(colors = colorScheme) { content() }
}
