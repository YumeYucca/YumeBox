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


import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.github.yumeyucca.yumebox.core.model.TunDnsMode
import com.github.yumeyucca.yumebox.data.model.TunStack
import com.github.yumeyucca.yumebox.presentation.component.*
import org.koin.androidx.compose.koinViewModel
import com.github.yumeyucca.yumebox.core.locale.YumeTxt

/**
 * Root Tun "service config" sub-page. Root provisions the virtual network interface and routing.
 * Shares [NetworkSettingsViewModel] with the picker screen.
 */
@Composable
fun TunServiceOptionsScreen() {
    val viewModel = koinViewModel<NetworkSettingsViewModel>()
    val screen by viewModel.tunOptionsScreenState.collectAsState()
    val ifName = screen.ifName
    val mtu = screen.mtu
    val stack = screen.stack
    val autoRoute = screen.autoRoute
    val strictRoute = screen.strictRoute
    val autoRedirect = screen.autoRedirect
    val dnsMode = screen.dnsMode
    val enableIPv6 = screen.enableIPv6

    ServiceOptionsPage(YumeTxt.NetworkSettings.TunOptions.Title) {
    item {
        Title(YumeTxt.NetworkSettings.RunMode.TunTitle)
        AppCard {
            TunInterfaceFields(ifName = ifName, mtu = mtu, viewModel = viewModel)
            TunProtocolFields(stack = stack, dnsMode = dnsMode, viewModel = viewModel)
            TunRoutingSwitches(
                autoRoute = autoRoute,
                strictRoute = strictRoute,
                autoRedirect = autoRedirect,
                enableIPv6 = enableIPv6,
                viewModel = viewModel,
            )
        }
    }
    }
}

/**
 * Interface name and MTU rows of the Tun card.
 */
@Composable
private fun TunInterfaceFields(ifName: String, mtu: Int, viewModel: NetworkSettingsViewModel) {
    TextInputArrowItem(
        title = YumeTxt.NetworkSettings.TunOptions.IfNameTitle,
        value = ifName,
        keyboardType = KeyboardType.Text,
        onConfirm = viewModel::onTunIfNameChange,
    )
    TextInputArrowItem(
        title = YumeTxt.NetworkSettings.TunOptions.MtuTitle,
        value = mtu.toString(),
        keyboardType = KeyboardType.Number,
        onConfirm = { it.toIntOrNull()?.let(viewModel::onTunMtuChange) },
    )
}

/**
 * Stack and DNS mode pickers of the Tun card.
 */
@Composable
private fun TunProtocolFields(stack: TunStack, dnsMode: TunDnsMode, viewModel: NetworkSettingsViewModel) {
    PreferenceEnumItem(
        title = YumeTxt.NetworkSettings.TunOptions.StackTitle,
        currentValue = stack,
        values = TunStack.entries,
        onValueChange = viewModel::onTunStackChange,
        label = ::tunStackLabel,
    )
    PreferenceEnumItem(
        title = YumeTxt.NetworkSettings.TunOptions.DnsModeTitle,
        currentValue = dnsMode,
        values = listOf(TunDnsMode.RedirHost, TunDnsMode.FakeIp),
        onValueChange = viewModel::onTunDnsModeChange,
        label = { dns ->
            when (dns) {
                TunDnsMode.RedirHost -> YumeTxt.NetworkSettings.TunOptions.DnsRedirHost
                TunDnsMode.FakeIp -> YumeTxt.NetworkSettings.TunOptions.DnsFakeIp
            }
        },
    )
}

/**
 * Routing and IPv6 switches of the Tun card.
 */
@Composable
private fun TunRoutingSwitches(
    autoRoute: Boolean,
    strictRoute: Boolean,
    autoRedirect: Boolean,
    enableIPv6: Boolean,
    viewModel: NetworkSettingsViewModel,
) {
    PreferenceSwitchItem(
        title = YumeTxt.NetworkSettings.TunOptions.AutoRouteTitle,
        checked = autoRoute,
        onCheckedChange = viewModel::onTunAutoRouteChange,
    )
    PreferenceSwitchItem(
        title = YumeTxt.NetworkSettings.TunOptions.StrictRouteTitle,
        checked = strictRoute,
        onCheckedChange = viewModel::onTunStrictRouteChange,
    )
    PreferenceSwitchItem(
        title = YumeTxt.NetworkSettings.TunOptions.AutoRedirectTitle,
        checked = autoRedirect,
        onCheckedChange = viewModel::onTunAutoRedirectChange,
    )
    PreferenceSwitchItem(
        title = YumeTxt.NetworkSettings.TunOptions.Ipv6Title,
        checked = enableIPv6,
        onCheckedChange = viewModel::onEnableIPv6Change,
    )
}

/**
 * Tappable row that opens an [AppTextFieldDialog] to edit a single text/number value (row + dialog
 * packaged together).
 */
@Composable
internal fun TextInputArrowItem(
    title: String,
    value: String,
    keyboardType: KeyboardType,
    onConfirm: (String) -> Unit,
) {
    val showDialog = remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    var field by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }

    PreferenceArrowItem(
        title = title,
        summary = value,
        onClick = {
            field = TextFieldValue(value, TextRange(value.length))
            showDialog.value = true
        },
        holdDownState = showDialog.value,
    )

    val confirm = {
        onConfirm(field.text)
        focusManager.clearFocus()
        showDialog.value = false
    }
    AppTextFieldDialog(
        show = showDialog.value,
        title = title,
        textFieldValue = field,
        onTextFieldValueChange = { field = it },
        onDismissRequest = {
            showDialog.value = false
            focusManager.clearFocus()
        },
        onConfirm = confirm,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        onImeAction = confirm,
    )
}

/**
 * Stack names are shared by every screen that offers the picker; [VpnServiceOptionsScreen] narrows
 * the offered values, not the labels.
 */
internal fun tunStackLabel(stack: TunStack): String =
    when (stack) {
        TunStack.System -> YumeTxt.NetworkSettings.TunOptions.StackSystem
        TunStack.GVisor -> YumeTxt.NetworkSettings.TunOptions.StackGVisor
        TunStack.Mixed -> YumeTxt.NetworkSettings.TunOptions.StackMixed
        TunStack.Mips -> YumeTxt.NetworkSettings.TunOptions.StackMips
    }
