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
import tf.gal.yumebox.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold

/**
 * Root Tun "service config" sub-page. Root provisions the virtual network interface and routing.
 * Shares [NetworkSettingsViewModel] with the picker screen.
 */
@Composable
fun TunServiceOptionsScreen() {
    val scrollBehavior = MiuixScrollBehavior()
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

    Scaffold(
        topBar = {
            TopBar(
                title = YumeTxt.NetworkSettings.TunOptions.Title,
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
                Title(YumeTxt.NetworkSettings.RunMode.TunTitle)
                AppCard {
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
                    PreferenceEnumItem(
                        title = YumeTxt.NetworkSettings.TunOptions.StackTitle,
                        currentValue = stack,
                        items =
                            listOf(
                                YumeTxt.NetworkSettings.TunOptions.StackSystem,
                                YumeTxt.NetworkSettings.TunOptions.StackGVisor,
                                YumeTxt.NetworkSettings.TunOptions.StackMixed,
                                YumeTxt.NetworkSettings.TunOptions.StackMips,
                            ),
                        values = TunStack.entries,
                        onValueChange = viewModel::onTunStackChange,
                    )
                    PreferenceEnumItem(
                        title = YumeTxt.NetworkSettings.TunOptions.DnsModeTitle,
                        currentValue = dnsMode,
                        items =
                            listOf(
                                YumeTxt.NetworkSettings.TunOptions.DnsRedirHost,
                                YumeTxt.NetworkSettings.TunOptions.DnsFakeIp,
                            ),
                        values = listOf(TunDnsMode.RedirHost, TunDnsMode.FakeIp),
                        onValueChange = viewModel::onTunDnsModeChange,
                    )
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
            }
        }
    }
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
