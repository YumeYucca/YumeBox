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
 * Copyright (c) YumeYucca 2025 - Present
 *
 */

package com.github.yumeyucca.yumebox.presentation.screen.node

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.github.yumeyucca.yumebox.presentation.viewmodel.ProxyViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private val NoProxyDelay = MutableStateFlow<Int?>(null)

/** Resolves the live delay flow for a node without rebuilding the group list on every result. */
internal val LocalProxyDelayFlow = staticCompositionLocalOf<(String) -> StateFlow<Int?>> {
    { NoProxyDelay }
}

@Composable
internal fun ProvideProxyDelayFlows(
    viewModel: ProxyViewModel,
    content: @Composable () -> Unit,
) {
    val provider = remember(viewModel) { viewModel::proxyDelayFlow }
    CompositionLocalProvider(
        LocalProxyDelayFlow provides provider,
        content = content,
    )
}