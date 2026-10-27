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

package com.github.yumeyucca.yumebox

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import com.github.yumeyucca.yumebox.common.util.AppLanguageManager
import com.github.yumeyucca.yumebox.presentation.theme.ProvideAndroidPlatformTheme
import com.github.yumeyucca.yumebox.presentation.theme.YumeTheme
import com.github.yumeyucca.yumebox.screen.settings.AppSettingsViewModel
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Scaffold

class ProxySheetActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguageManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFinishOnTouchOutside(false)
        @Suppress("DEPRECATION") overridePendingTransition(0, 0)

        setContent {
            val appSettingsViewModel = koinViewModel<AppSettingsViewModel>()
            val themeMode = appSettingsViewModel.themeMode.state.collectAsState().value
            val themeSeedColorArgb =
                appSettingsViewModel.themeSeedColorArgb.state.collectAsState().value
            val invertOnPrimaryColors =
                appSettingsViewModel.invertOnPrimaryColors.state.collectAsState().value
            ProvideAndroidPlatformTheme {
                YumeTheme(
                    themeMode = themeMode,
                    themeSeedColorArgb = themeSeedColorArgb,
                    invertOnPrimaryColors = invertOnPrimaryColors,
                ) {
                    // GlassBottomSheet hosts itself in a Scaffold's dialog host, and this activity
                    // had none — the sheet composited nothing. A fully transparent, inset-less
                    // Scaffold gives it (and the sheet's popups) their host without painting
                    // anything over the activity's transparent window.
                    Scaffold(
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    ) { _ ->
                        ProxySheetContent(
                            onDismiss = {
                                finish()
                                @Suppress("DEPRECATION") overridePendingTransition(0, 0)
                            }
                        )
                    }
                }
            }
        }
    }
}
