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

package com.github.yumeyucca.yumebox.screen.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.github.yumeyucca.yumebox.presentation.component.*
import org.koin.androidx.compose.koinViewModel
import com.github.yumeyucca.yumebox.core.locale.YumeTxt

/** Settings for the active kernel's native eBPF listener. */
@Composable
fun EbpfServiceOptionsScreen() {
    val viewModel = koinViewModel<NetworkSettingsViewModel>()
    val bypassCn by viewModel.ebpfBypassCn.state.collectAsState()

    ServiceOptionsPage(YumeTxt.NetworkSettings.RunMode.EbpfTitle) {
    item {
        AppCard {
            PreferenceSwitchItem(
                title = YumeTxt.NetworkSettings.EbpfOptions.BypassCnTitle,
                checked = bypassCn,
                onCheckedChange = viewModel::onEbpfBypassCnChange,
            )
        }
    }
    }
}
