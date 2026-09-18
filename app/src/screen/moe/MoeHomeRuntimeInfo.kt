/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * Copyright (c) YumeYucca 2025 - Present
 */

@file:Suppress("FunctionName")

package com.github.yumeyucca.yumebox.screen.moe


import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.github.yumeyucca.yumebox.common.util.formatBytesForDisplay
import com.github.yumeyucca.yumebox.presentation.component.CountryFlagCircle
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.util.extractFlaggedName
import tf.gal.yumebox.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun MoeTrafficStrip(
    downloadSpeed: Long,
    uploadSpeed: Long,
    modifier: Modifier = Modifier,
) {
    Row(modifier, Arrangement.spacedBy(MoeUi.Hero.trafficRowGap), Alignment.CenterVertically) {
        Box(Modifier.weight(1f), Alignment.CenterStart) {
            MoeTrafficItem(YumeTxt.Home.Traffic.UpShort, uploadSpeed)
        }
        Box(Modifier.weight(1f), Alignment.CenterEnd) {
            MoeTrafficItem(YumeTxt.Home.Traffic.DownShort, downloadSpeed)
        }
    }
}

@Composable
private fun MoeTrafficItem(label: String, speed: Long) {
    val (value, unit) = formatBytesForDisplay(speed)
    val onSurface = MiuixTheme.colorScheme.onSurface
    Row(
        horizontalArrangement = Arrangement.spacedBy(MoeUi.Traffic.itemGap),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = label,
            color = onSurface.copy(alpha = 0.62f),
            style = MiuixTheme.textStyles.footnote1,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(bottom = MoeUi.Traffic.labelBottomPadding),
        )
        Text(
            text = value,
            color = onSurface,
            style = MiuixTheme.textStyles.title1,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
        )
        Text(
            text = unit,
            color = onSurface.copy(alpha = 0.55f),
            style = MiuixTheme.textStyles.footnote1,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = MoeUi.Traffic.labelBottomPadding),
        )
    }
}

@Composable
internal fun MoeHomeInfoPanel(
    serverName: String?,
    serverPing: Int?,
    modifier: Modifier = Modifier,
) {
    val node = remember(serverName) { serverName?.let(::extractFlaggedName) }
    val name = node?.displayName ?: serverName.orEmpty()
    val ping =
        serverPing
            ?.takeIf { it in 1..1000 }
            ?.let { value -> YumeTxt.Home.NodeInfo.DelayValue.format(value) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MoeUi.Hero.infoRowMinHeight),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (name.isNotBlank()) {
            MoeInfoBlock(
                value = name,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = MoeUi.Info.trailingPadding),
                leading = {
                    CountryFlagCircle(
                        countryCode = node?.countryCode,
                        size = AppTheme.spacing.space16,
                    )
                },
            )
        } else Spacer(Modifier
            .weight(1f)
            .padding(end = MoeUi.Info.trailingPadding))
        if (ping != null) {
            MoeInfoBlock(
                value = ping,
                modifier = Modifier.width(MoeUi.Hero.delayWidth),
                valueColor =
                    if (serverPing < 500) AppTheme.colors.moe.pingExcellent
                    else AppTheme.colors.moe.pingWarning,
                alignEnd = true,
            )
        }
    }
}

@Composable
private fun MoeInfoBlock(
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MiuixTheme.colorScheme.onBackground,
    valueFontFamily: FontFamily? = null,
    alignEnd: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.spacedBy(
                space = MoeUi.Info.blockGap,
                alignment = if (alignEnd) Alignment.End else Alignment.Start,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        Text(
            text = value,
            color = valueColor,
            style = MiuixTheme.textStyles.body1,
            fontWeight = FontWeight.Medium,
            fontFamily = valueFontFamily,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = if (alignEnd) Modifier else Modifier.weight(1f),
        )
    }
}
