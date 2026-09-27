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

package com.github.yumeyucca.yumebox.screen.feature

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.github.yumeyucca.yumebox.data.model.RemoteBackend
import com.github.yumeyucca.yumebox.data.model.RemoteProtocol
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.screen.settings.RemoteControllerViewModel
import org.koin.androidx.compose.koinViewModel
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun RemoteControllerSection(
    viewModel: RemoteControllerViewModel = koinViewModel(),
    onRequestLocalNetworkPermission: (RemoteBackend, () -> Unit) -> Unit = { _, action -> action() },
) {
    val section by viewModel.sectionState.collectAsState()
    val controllerEnabled = section.controllerEnabled
    val backends = section.backends
    val activeBackendId = section.activeBackendId

    var sheetState by remember { mutableStateOf<BackendSheetState?>(null) }
    var sheetVisible by remember { mutableStateOf(false) }

    val activeBackend = backends.firstOrNull { it.id == activeBackendId } ?: backends.firstOrNull()
    val selectedBackendIndex =
        backends.indexOfFirst { it.id == activeBackend?.id }.takeIf { it >= 0 } ?: 0
    val backendItems = backends.map { it.displayName() }

    Title(YumeTxt.Feature.RemoteController.Section)
    AppCard {
        PreferenceSwitchItem(
            title = YumeTxt.Feature.RemoteController.ModeTitle,
            checked = controllerEnabled,
            onCheckedChange = { enabled ->
                val backend = activeBackend
                if (enabled && backend != null) {
                    onRequestLocalNetworkPermission(backend) { viewModel.setEnabled(true) }
                } else {
                    viewModel.setEnabled(false)
                }
            },
            enabled = backends.isNotEmpty(),
        )

        if (backends.isNotEmpty()) {
            WindowDropdownPreference(
                title = YumeTxt.Feature.RemoteController.ControlBackend,
                summary = null,
                items = backendItems,
                selectedIndex = selectedBackendIndex,
                onSelectedIndexChange = { index ->
                    backends.getOrNull(index)?.let { backend ->
                        if (controllerEnabled) {
                            onRequestLocalNetworkPermission(backend) { viewModel.setActive(backend.id) }
                        } else {
                            viewModel.setActive(backend.id)
                        }
                    }
                },
            )
            WindowDropdownPreference(
                title = YumeTxt.Feature.RemoteController.Protocol,
                summary = null,
                items = RemoteProtocol.entries.map { it.scheme.uppercase() },
                selectedIndex = RemoteProtocol.entries.indexOf(activeBackend?.protocol),
                onSelectedIndexChange = { index ->
                    val protocol = RemoteProtocol.entries[index]
                    activeBackend?.let { backend ->
                        val update = { viewModel.setProtocol(backend.id, protocol) }
                        if (controllerEnabled) {
                            onRequestLocalNetworkPermission(backend, update)
                        } else {
                            update()
                        }
                    }
                },
            )
        }

        PreferenceArrowItem(
            title = YumeTxt.Feature.RemoteController.AddBackend,
            onClick = {
                sheetState = BackendSheetState.Add(BackendFormState.empty())
                sheetVisible = true
            },
        )

        activeBackend?.let { backend ->
            PreferenceArrowItem(
                title = YumeTxt.Feature.RemoteController.EditBackend,
                onClick = {
                    sheetState = BackendSheetState.Edit(BackendFormState.from(backend))
                    sheetVisible = true
                },
            )
        }
    }

    sheetState?.let { state ->
        BackendEditSheet(
            show = sheetVisible,
            state = state.form,
            title =
                if (state is BackendSheetState.Add) {
                    YumeTxt.Feature.RemoteController.AddBackend
                } else {
                    YumeTxt.Feature.RemoteController.EditBackend
                },
            onDismiss = { sheetVisible = false },
            onDismissFinished = { sheetState = null },
            onConfirm = { name, host, port, secret ->
                when (state) {
                    is BackendSheetState.Add -> {
                        viewModel.addBackend(name, host, port, secret)
                    }

                    is BackendSheetState.Edit -> {
                        state.form.id?.let { id ->
                            viewModel.updateBackend(
                                RemoteBackend(
                                    id = id,
                                    name = name.trim(),
                                    host = host.trim(),
                                    port = port,
                                    secret = secret.trim(),
                                ),
                            )
                        }
                    }
                }
                sheetVisible = false
            },
            onDelete =
                (state as? BackendSheetState.Edit)?.let { editState ->
                    {
                        editState.form.id?.let(viewModel::deleteBackend)
                        sheetVisible = false
                    }
                },
        )
    }
}

@Composable
private fun BackendEditSheet(
    show: Boolean,
    state: BackendFormState,
    title: String,
    onDismiss: () -> Unit,
    onDismissFinished: () -> Unit,
    onConfirm: (name: String, host: String, port: Int, secret: String) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val spacing = AppTheme.spacing
    var name by remember(state) { mutableStateOf(state.name) }
    var host by remember(state) { mutableStateOf(state.host) }
    var port by remember(state) { mutableStateOf(state.port) }
    var secret by remember(state) { mutableStateOf(state.secret) }

    val normalizedHost =
        host
            .trim()
            .removePrefix("http://")
            .removePrefix("https://")
            .trimEnd('/')
    val parsedPort = port.trim().toIntOrNull()
    val portValid = parsedPort != null && parsedPort in 1..65535
    val canConfirm = normalizedHost.isNotEmpty() && portValid

    AppDialog(
        show = show,
        title = title,
        onDismissRequest = onDismiss,
        onDismissFinished = onDismissFinished,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.space16),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.space12),
            ) {
                OemTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = YumeTxt.Feature.RemoteController.Name,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.space12),
                    verticalAlignment = Alignment.Top,
                ) {
                    OemTextField(
                        value = host,
                        onValueChange = { raw ->
                            val input = parseHostPortInput(raw, port)
                            host = input.host
                            port = input.port
                        },
                        label = YumeTxt.Feature.RemoteController.Host,
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OemTextField(
                        value = port,
                        onValueChange = { port = it.filter(Char::isDigit).take(5) },
                        label = YumeTxt.Feature.RemoteController.Port,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.45f),
                    )
                }

                OemTextField(
                    value = secret,
                    onValueChange = { secret = it },
                    label = YumeTxt.Feature.RemoteController.Secret,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (!portValid && port.isNotBlank()) {
                    Text(
                        text = YumeTxt.Feature.RemoteController.PortRangeError,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.error,
                        modifier = Modifier.padding(start = spacing.space12),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.space16),
            ) {
                if (onDelete != null) {
                    TextButton(
                        text = YumeTxt.Feature.RemoteController.Delete,
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        colors =
                            ButtonDefaults.textButtonColors(
                                textColor = MiuixTheme.colorScheme.error,
                            ),
                    )
                } else {
                    TextButton(
                        text = YumeTxt.Component.Button.Cancel,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    )
                }
                TextButton(
                    text = YumeTxt.Component.Button.Confirm,
                    onClick = {
                        if (canConfirm) {
                            onConfirm(name, normalizedHost, parsedPort, secret)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = canConfirm,
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    }
}

private sealed interface BackendSheetState {
    val form: BackendFormState

    data class Add(
        override val form: BackendFormState,
    ) : BackendSheetState

    data class Edit(
        override val form: BackendFormState,
    ) : BackendSheetState
}

private data class BackendFormState(
    val id: String?,
    val name: String,
    val host: String,
    val port: String,
    val secret: String,
) {
    companion object {
        fun empty() =
            BackendFormState(
                id = null,
                name = "",
                host = "",
                port = "9090",
                secret = "",
            )

        fun from(backend: RemoteBackend) =
            BackendFormState(
                id = backend.id,
                name = backend.name,
                host = backend.host,
                port = backend.port.toString(),
                secret = backend.secret,
            )
    }
}

private fun RemoteBackend.displayName(): String = name.ifBlank { "$host:$port" }

private data class HostPortInput(
    val host: String,
    val port: String,
)

private fun parseHostPortInput(
    raw: String,
    currentPort: String,
): HostPortInput {
    val endpoint =
        raw
            .trim()
            .removePrefix("http://")
            .removePrefix("https://")
            .substringBefore('/')
            .substringBefore('?')
            .trimEnd('/')
    val lastColonIndex = endpoint.lastIndexOf(':')
    if (lastColonIndex > 0 && endpoint.indexOf(':') == lastColonIndex) {
        val possiblePort = endpoint.substring(lastColonIndex + 1)
        if (possiblePort.isNotBlank() && possiblePort.all(Char::isDigit)) {
            return HostPortInput(
                host = endpoint.substring(0, lastColonIndex),
                port = possiblePort.take(5),
            )
        }
    }
    return HostPortInput(host = endpoint, port = currentPort)
}
