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

/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License.
 */

@file:Suppress("FunctionName")

package com.github.yumeyucca.yumebox.screen.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.github.yumeyucca.yumebox.presentation.component.*
import org.koin.androidx.compose.koinViewModel
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold

/** Settings for the active kernel's native eBPF listener. */
@Composable
fun EbpfServiceOptionsScreen() {
    val scrollBehavior = MiuixScrollBehavior()
    val viewModel = koinViewModel<NetworkSettingsViewModel>()
    val options by viewModel.ebpfServiceOptionsUiState.collectAsState()

    Scaffold(
        topBar = {
            TopBar(
                title = YumeTxt.NetworkSettings.RunMode.EbpfTitle,
                scrollBehavior = scrollBehavior,
            )
        }
    ) { innerPadding ->
        val mainLikePadding = rememberStandalonePageMainPadding()
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = combinePaddingValues(innerPadding, mainLikePadding),
        ) {
            item {
                Title(YumeTxt.NetworkSettings.RunMode.EbpfTitle)
                AppCard {
                    PreferenceSwitchItem(
                        title = YumeTxt.NetworkSettings.EbpfOptions.BypassCnTitle,
                        checked = options.bypassCn,
                        onCheckedChange = viewModel::onEbpfBypassCnChange,
                    )
                }
            }
        }
    }
}
