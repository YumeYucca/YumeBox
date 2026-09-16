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
import com.github.yumeyucca.yumebox.data.model.TunStack
import com.github.yumeyucca.yumebox.presentation.component.*
import org.koin.androidx.compose.koinViewModel
import tf.gal.yumebox.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold

/**
 * The VpnService "service config" sub-page (reached from the network-settings Advanced section).
 * Holds every knob the userspace VpnService path exposes, including the gVisor/MIPS stack picker.
 * Shares [NetworkSettingsViewModel] with the picker screen.
 */
@Composable
fun VpnServiceOptionsScreen() {
    val scrollBehavior = MiuixScrollBehavior()
    val viewModel = koinViewModel<NetworkSettingsViewModel>()
    val tunOptions by viewModel.tunServiceOptionsUiState.collectAsState()
    val stack by viewModel.tunStack.state.collectAsState()

    Scaffold(
        topBar = {
            TopBar(
                title = YumeTxt.NetworkSettings.Section.VpnOptions,
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
                Title(YumeTxt.NetworkSettings.RunMode.VpnServiceTitle)
                AppCard {
                    PreferenceEnumItem(
                        title = YumeTxt.NetworkSettings.TunOptions.StackTitle,
                        currentValue = stack.forVpnService(),
                        items =
                            listOf(
                                YumeTxt.NetworkSettings.TunOptions.StackGVisor,
                                YumeTxt.NetworkSettings.TunOptions.StackMips,
                            ),
                        values = listOf(TunStack.GVisor, TunStack.Mips),
                        onValueChange = viewModel::onTunStackChange,
                    )
                    PreferenceSwitchItem(
                        title = YumeTxt.NetworkSettings.VpnOptions.BypassPrivateTitle,
                        checked = tunOptions.common.bypassPrivateNetwork,
                        onCheckedChange = viewModel::onBypassPrivateNetworkChange,
                    )
                    PreferenceSwitchItem(
                        title = YumeTxt.NetworkSettings.VpnOptions.DnsHijackTitle,
                        checked = tunOptions.common.dnsHijack,
                        onCheckedChange = viewModel::onDnsHijackChange,
                    )
                    PreferenceSwitchItem(
                        title = YumeTxt.NetworkSettings.VpnOptions.EnableIpv6Title,
                        checked = tunOptions.common.enableIPv6,
                        onCheckedChange = viewModel::onEnableIPv6Change,
                    )
                    PreferenceSwitchItem(
                        title = YumeTxt.NetworkSettings.VpnOptions.AllowBypassTitle,
                        checked = tunOptions.allowBypass,
                        onCheckedChange = viewModel::onAllowBypassChange,
                    )
                    PreferenceSwitchItem(
                        title = YumeTxt.NetworkSettings.VpnOptions.SystemProxyTitle,
                        checked = tunOptions.systemProxy,
                        onCheckedChange = viewModel::onSystemProxyChange,
                    )
                }
            }
            item { WifiAutomationSettingsSection() }
        }
    }
}
