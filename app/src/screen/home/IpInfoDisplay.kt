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

@file:Suppress("UnusedSymbol", "FunctionName", "IntroduceWhenSubject")

package com.github.yumeyucca.yumebox.screen.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.github.yumeyucca.yumebox.data.network.IpMonitoringState
import com.github.yumeyucca.yumebox.presentation.component.CountryFlagCircle
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import tf.gal.yumebox.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val infoValueCornerRadius = RoundedCornerShape(UiDp.dp10)
private val infoValueMaxWidth = UiDp.dp220
internal val infoTextHeight = UiDp.dp24

@Composable
fun IpInfoDisplay(state: IpMonitoringState, modifier: Modifier = Modifier) {
    val externalIp = (state as? IpMonitoringState.Success)?.externalIp
    var isIpVisible by rememberSaveable(externalIp?.ip) { mutableStateOf(false) }

    when {
        externalIp != null -> {
            IpInfoRow(
                label = YumeTxt.Home.IpInfo.ExitIp,
                value = buildDisplayIpValue(ipAddress = externalIp.ip, isIpVisible = isIpVisible),
                valueColor = MiuixTheme.colorScheme.onSurface,
                countryCode = externalIp.countryCode,
                isRevealable = true,
                onToggleVisibility = { isIpVisible = !isIpVisible },
                modifier = modifier,
            )
        }

        else -> {
            IpInfoRow(
                label = YumeTxt.Home.IpInfo.ExitIp,
                value = "--",
                valueColor = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                countryCode = null,
                isRevealable = false,
                onToggleVisibility = {},
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun IpInfoRow(
    label: String,
    value: String,
    valueColor: Color,
    countryCode: String?,
    isRevealable: Boolean,
    onToggleVisibility: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .weight(1f)
                .padding(end = UiDp.dp16),
        ) {
            Text(
                text = label,
                style = MiuixTheme.textStyles.footnote1.copy(fontSize = 12.sp),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Spacer(modifier = Modifier.height(UiDp.dp4))
            Text(
                text = value,
                style = MiuixTheme.textStyles.body1.copy(lineHeight = 20.sp),
                fontFamily = FontFamily.Monospace,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    if (isRevealable) {
                        Modifier
                            .widthIn(max = infoValueMaxWidth)
                            .height(infoTextHeight)
                            .clip(infoValueCornerRadius)
                            .clickable(onClick = onToggleVisibility)
                    } else {
                        Modifier.height(infoTextHeight)
                    },
            )
        }

        CountryBadge(countryCode = countryCode)
    }
}

@Composable
private fun CountryBadge(countryCode: String?) {
    if (countryCode != null) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(UiDp.dp8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CountryFlagCircle(countryCode = countryCode, size = UiDp.dp20)
            Text(
                text = countryCode,
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = UiDp.dp40),
            )
        }
    }
}

private fun buildDisplayIpValue(ipAddress: String, isIpVisible: Boolean): String =
    if (ipAddress.contains(":")) {
        formatIpv6Address(ipAddress = ipAddress, isIpVisible = isIpVisible)
    } else {
        if (isIpVisible) ipAddress else maskIpv4Address(ipAddress)
    }

private fun maskIpv4Address(ipAddress: String): String {
    val segments = ipAddress.split(".")
    if (segments.size != 4) {
        return "****"
    }
    return buildString {
        append(segments[0])
        append(".")
        append(segments[1])
        append(".")
        append("*".repeat(segments[2].length.coerceAtLeast(1)))
        append(".")
        append(segments[3])
    }
}

private fun formatIpv6Address(ipAddress: String, isIpVisible: Boolean): String {
    val visibleSegments = ipAddress.split(":").filter { it.isNotBlank() }
    if (visibleSegments.isEmpty()) {
        return "****"
    }
    if (!isIpVisible) {
        return when {
            visibleSegments.size == 1 -> "${visibleSegments.first()}:****"
            visibleSegments.size == 2 ->
                "${visibleSegments[0]}:${"*".repeat(visibleSegments[1].length.coerceAtLeast(4))}"

            else -> "${visibleSegments[0]}:${visibleSegments[1]}:****"
        }
    }

    val visiblePrefix = visibleSegments.take(4)
    return if (visibleSegments.size > 4) {
        "${visiblePrefix.joinToString(":")}..."
    } else {
        visiblePrefix.joinToString(":")
    }
}

internal fun maskIpAddress(ipAddress: String): String =
    if (ipAddress.contains(":")) {
        formatIpv6Address(ipAddress = ipAddress, isIpVisible = false)
    } else {
        maskIpv4Address(ipAddress)
    }
