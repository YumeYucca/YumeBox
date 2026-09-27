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

package com.github.yumeyucca.yumebox.feature.meta.presentation.component


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.yumeyucca.yumebox.common.util.formatBytes
import com.github.yumeyucca.yumebox.core.model.ConnectionInfo
import com.github.yumeyucca.yumebox.presentation.component.AppCard
import com.github.yumeyucca.yumebox.presentation.component.Title
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val CONNECTION_LEADING_ICON_BITMAP_SIZE = 96

/**
 * Full-page connection detail body. Mirrors the settings-style label/value rows (screenshot
 * reference: large title + card groups) instead of the old bottom sheet.
 */
@Composable
fun ConnectionDetailContent(
    connectionInfo: ConnectionInfo,
    canInterrupt: Boolean,
    isInterrupting: Boolean,
    onInterrupt: () -> Unit,
) {
    val spacing = AppTheme.spacing
    val state = remember(connectionInfo) { connectionInfo.toDetailState() }

    Column(modifier = Modifier.fillMaxWidth()) {
        Title(YumeTxt.Connection.Detail.Section.Info)
        AppCard {
            ConnectionHeaderRow(state = state)
            DetailValueRow(
                title = YumeTxt.Connection.Detail.Label.Host,
                value = state.displayHost.ifEmpty { "—" },
            )
            DetailValueRow(
                title = YumeTxt.Connection.Detail.Label.Protocol,
                value = state.network.uppercase(),
            )
            if (state.process.isNotEmpty()) {
                DetailValueRow(
                    title = YumeTxt.Connection.Detail.Label.Process,
                    value = state.process,
                )
            }
            DetailValueRow(
                title = YumeTxt.Connection.Detail.Label.SourceAddress,
                value = state.sourceAddress.ifEmpty { "—" },
            )
            if (state.destinationAddress.isNotEmpty()) {
                DetailValueRow(
                    title = YumeTxt.Connection.Detail.Label.DestinationAddress,
                    value = state.destinationAddress,
                )
            }
            DetailValueRow(
                title = YumeTxt.Connection.Detail.Label.Duration,
                value = state.duration,
            )
            DetailValueRow(
                title = YumeTxt.Connection.Detail.Label.Upload,
                value = formatBytes(connectionInfo.upload),
                valueColor = AppTheme.colors.protocol.tcp,
            )
            DetailValueRow(
                title = YumeTxt.Connection.Detail.Label.Download,
                value = formatBytes(connectionInfo.download),
                valueColor = AppTheme.colors.protocol.udp,
            )
        }

        if (connectionInfo.rule.isNotEmpty()) {
            Title(YumeTxt.Connection.Detail.Section.Rule)
            AppCard {
                DetailValueRow(
                    title = YumeTxt.Connection.Detail.Label.Type,
                    value = connectionInfo.rule,
                )
                if (connectionInfo.rulePayload.isNotEmpty()) {
                    DetailValueRow(
                        title = YumeTxt.Connection.Detail.Label.Content,
                        value = connectionInfo.rulePayload,
                    )
                }
            }
        }

        if (connectionInfo.chains.isNotEmpty()) {
            Title(YumeTxt.Connection.Detail.Section.Chain)
            AppCard {
                ProxyChainRow(
                    chains = connectionInfo.chains,
                    modifier =
                        Modifier.padding(
                            horizontal = spacing.space16,
                            vertical = spacing.space12,
                        ),
                )
            }
        }

        if (canInterrupt) {
            Spacer(modifier = Modifier.height(spacing.space16))
            InterruptConnectionButton(
                isInterrupting = isInterrupting,
                onInterrupt = onInterrupt,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.screenHorizontal),
            )
            Spacer(modifier = Modifier.height(spacing.space12))
        }
    }
}

@Composable
private fun ConnectionHeaderRow(state: ConnectionDetailState) {
    val spacing = AppTheme.spacing
    val sizes = AppTheme.sizes
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.space16, vertical = spacing.space12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.space12),
    ) {
        ConnectionLeadingIcon(
            metadata = state.metadata,
            network = state.network,
            size = sizes.connectionLeadingIconSize,
            bitmapSize = CONNECTION_LEADING_ICON_BITMAP_SIZE,
        )
        Text(
            text = state.displayHost.ifEmpty { YumeTxt.Connection.Detail.Title },
            style = MiuixTheme.textStyles.headline1,
            color = MiuixTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DetailValueRow(
    title: String,
    value: String,
    valueColor: Color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
) {
    BasicComponent(
        title = title,
        endActions = {
            Text(
                text = value,
                style = MiuixTheme.textStyles.body2,
                color = valueColor,
                textAlign = TextAlign.End,
                modifier = Modifier.widthIn(max = 220.dp),
            )
        },
    )
}

@Composable
private fun ProxyChainRow(chains: List<String>, modifier: Modifier = Modifier) {
    val spacing = AppTheme.spacing
    val appColors = AppTheme.colors
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.space2),
        verticalArrangement = Arrangement.spacedBy(spacing.space2),
    ) {
        chains.forEachIndexed { index, chain ->
            val isLast = index == chains.lastIndex
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.space2),
            ) {
                ChainNode(name = chain, isActive = isLast)
                if (!isLast) {
                    Text(
                        text = "→",
                        style = MiuixTheme.textStyles.footnote1,
                        color = appColors.connection.chainArrow,
                        modifier = Modifier.padding(horizontal = spacing.space2),
                    )
                }
            }
        }
    }
}

@Composable
private fun InterruptConnectionButton(
    isInterrupting: Boolean,
    onInterrupt: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onInterrupt,
        enabled = !isInterrupting,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(),
    ) {
        Text(
            text =
                if (isInterrupting) {
                    YumeTxt.Connection.Detail.Action.Interrupting
                } else {
                    YumeTxt.Connection.Detail.Action.Interrupt
                },
            color = MiuixTheme.colorScheme.error,
        )
    }
}

@Composable
private fun ChainNode(name: String, isActive: Boolean) {
    val spacing = AppTheme.spacing
    val radii = AppTheme.radii
    val opacity = AppTheme.opacity
    val sizes = AppTheme.sizes
    val appColors = AppTheme.colors
    val backgroundColor =
        if (isActive) {
            appColors.connection.chainActive.copy(alpha = opacity.subtleStrong)
        } else {
            MiuixTheme.colorScheme.surfaceVariant
        }
    val textColor =
        if (isActive) {
            appColors.connection.chainActive
        } else {
            appColors.connection.chainInactiveText
        }

    Row(
        modifier =
            Modifier
                .clip(RoundedCornerShape(radii.radius8))
                .background(backgroundColor)
                .padding(
                    horizontal = sizes.nodeChainNodeHorizontalPadding,
                    vertical = sizes.nodeChainNodeVerticalPadding,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.space4),
    ) {
        if (isActive) {
            Box(
                modifier =
                    Modifier
                        .size(sizes.nodeChainIndicatorSize)
                        .clip(CircleShape)
                        .background(appColors.connection.chainActive)
            )
        }
        Text(
            text = name,
            style = MiuixTheme.textStyles.footnote1.copy(fontSize = 11.sp),
            color = textColor,
            maxLines = 1,
        )
    }
}

internal data class ConnectionDetailState(
    val metadata: JsonObject,
    val displayHost: String,
    val network: String,
    val process: String,
    val sourceAddress: String,
    val destinationAddress: String,
    val duration: String,
)

internal fun ConnectionInfo.toDetailState(): ConnectionDetailState {
    val host = metadata.stringOrEmpty("host")
    val network = metadata.stringOrEmpty("network").ifEmpty { "TCP" }
    val process = metadata.stringOrEmpty("process")
    val destinationPort = metadata.stringOrEmpty("destinationPort")
    val sourceIP = metadata.stringOrEmpty("sourceIP")
    val sourcePort = metadata.stringOrEmpty("sourcePort")
    val destinationIP = metadata.stringOrEmpty("destinationIP")

    val displayHost =
        when {
            host.isNotEmpty() && destinationPort.isNotEmpty() -> "$host:$destinationPort"
            host.isNotEmpty() -> host
            sourceIP.isNotEmpty() -> "$sourceIP:$sourcePort"
            else -> ""
        }

    return ConnectionDetailState(
        metadata = metadata,
        displayHost = displayHost,
        network = network,
        process = process,
        sourceAddress = "$sourceIP:$sourcePort",
        destinationAddress =
            destinationIP.takeIf(String::isNotEmpty)?.let { "$it:$destinationPort" }.orEmpty(),
        duration = start.takeIf(String::isNotEmpty)?.let(::calculateDuration) ?: "00:00:00",
    )
}

private fun calculateDuration(start: String): String {
    if (start.isEmpty()) return "00:00:00"

    return try {
        val startTime = java.time.OffsetDateTime.parse(start).toInstant()
        val now = java.time.Instant.now()
        val duration = java.time.Duration.between(startTime, now)

        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60
        val seconds = duration.seconds % 60

        "%02d:%02d:%02d".format(hours, minutes, seconds)
    } catch (_: java.time.DateTimeException) {
        "00:00:00"
    }
}

private fun JsonObject.stringOrEmpty(key: String): String =
    get(key)?.jsonPrimitive?.content.orEmpty()
