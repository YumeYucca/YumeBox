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

package com.github.yumeyucca.yumebox.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.github.yumeyucca.yumebox.presentation.theme.ProvideAndroidPlatformTheme
import com.github.yumeyucca.yumebox.presentation.theme.YumeTheme
import com.github.yumeyucca.yumebox.screen.settings.AppSettingsViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Themed root every Activity composes into: reads the theme triple (mode / seed / inverted primary
 * colours) from [AppSettingsViewModel] once, installs the Android system-bar effect and wraps
 * [content] in [YumeTheme]. Activities must not re-wire those three states themselves.
 */
@Composable
fun YumeAppShell(content: @Composable () -> Unit) {
    val appSettingsViewModel = koinViewModel<AppSettingsViewModel>()
    val themeMode = appSettingsViewModel.themeMode.state.collectAsState().value
    val themeSeedColorArgb = appSettingsViewModel.themeSeedColorArgb.state.collectAsState().value
    val invertOnPrimaryColors =
        appSettingsViewModel.invertOnPrimaryColors.state.collectAsState().value
    ProvideAndroidPlatformTheme {
        YumeTheme(
            themeMode = themeMode,
            themeSeedColorArgb = themeSeedColorArgb,
            invertOnPrimaryColors = invertOnPrimaryColors,
            content = content,
        )
    }
}
