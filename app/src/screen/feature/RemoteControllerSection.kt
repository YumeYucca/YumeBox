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

    // 表单为 null 表示关闭；非空时保留到退场动画结束，可见性由 sheetVisible 单独控制。
    var pendingForm by remember { mutableStateOf<BackendFormState?>(null) }
    var sheetVisible by remember { mutableStateOf(false) }

    val activeBackend = backends.firstOrNull { it.id == activeBackendId } ?: backends.firstOrNull()

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
    }
    SectionSummary(YumeTxt.Feature.RemoteController.ModeSummary)

    if (backends.isNotEmpty()) {
        Title(YumeTxt.Feature.RemoteController.BackendSection)
        AppCard {
            GlassDropdownPreference(
                title = YumeTxt.Feature.RemoteController.ControlBackend,
                options = backends.map { GlassDropdownOption(it, it.displayName) },
                selected = activeBackend,
                onSelected = { backend ->
                    if (controllerEnabled) {
                        onRequestLocalNetworkPermission(backend) { viewModel.setActive(backend.id) }
                    } else {
                        viewModel.setActive(backend.id)
                    }
                },
            )
            GlassDropdownPreference(
                title = YumeTxt.Feature.RemoteController.Protocol,
                options = RemoteProtocol.entries.map { GlassDropdownOption(it, it.scheme.uppercase()) },
                selected = activeBackend?.protocol,
                onSelected = { protocol ->
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
        SectionSummary(YumeTxt.Feature.RemoteController.BackendSummary)
    }

    Title(YumeTxt.Feature.RemoteController.ManageSection)
    AppCard {
        PreferenceArrowItem(
            title = YumeTxt.Feature.RemoteController.AddBackend,
            onClick = {
                pendingForm = BackendFormState.empty()
                sheetVisible = true
            },
        )

        activeBackend?.let { backend ->
            PreferenceArrowItem(
                title = YumeTxt.Feature.RemoteController.EditBackend,
                onClick = {
                    pendingForm = BackendFormState.from(backend)
                    sheetVisible = true
                },
            )
        }
    }

    pendingForm?.let { form ->
        BackendEditSheet(
            show = sheetVisible,
            form = form,
            title =
                if (form.id == null) {
                    YumeTxt.Feature.RemoteController.AddBackend
                } else {
                    YumeTxt.Feature.RemoteController.EditBackend
                },
            onDismiss = { sheetVisible = false },
            onDismissFinished = { pendingForm = null },
            onConfirm = { name, host, port, secret ->
                val id = form.id
                if (id == null) {
                    viewModel.addBackend(name, host, port, secret)
                } else {
                    viewModel.updateBackend(
                        RemoteBackend(
                            id = id,
                            name = name.trim(),
                            host = host.trim(),
                            port = port,
                            secret = secret.trim(),
                            protocol = form.protocol,
                        ),
                    )
                }
                sheetVisible = false
            },
            onDelete =
                form.id?.let { id ->
                    {
                        viewModel.deleteBackend(id)
                        sheetVisible = false
                    }
                },
        )
    }
}

@Composable
private fun BackendEditSheet(
    show: Boolean,
    form: BackendFormState,
    title: String,
    onDismiss: () -> Unit,
    onDismissFinished: () -> Unit,
    onConfirm: (name: String, host: String, port: Int, secret: String) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val spacing = AppTheme.spacing
    var name by remember(form) { mutableStateOf(form.name) }
    var host by remember(form) { mutableStateOf(form.host) }
    var port by remember(form) { mutableStateOf(form.port) }
    var secret by remember(form) { mutableStateOf(form.secret) }

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

private data class BackendFormState(
    val id: String?,
    val name: String,
    val host: String,
    val port: String,
    val secret: String,
    val protocol: RemoteProtocol,
) {
    companion object {
        fun empty() =
            BackendFormState(
                id = null,
                name = "",
                host = "",
                port = "9090",
                secret = "",
                protocol = RemoteProtocol.HTTP,
            )

        fun from(backend: RemoteBackend) =
            BackendFormState(
                id = backend.id,
                name = backend.name,
                host = backend.host,
                port = backend.port.toString(),
                secret = backend.secret,
                protocol = backend.protocol,
            )
    }
}

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
