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

package com.github.yumeyucca.yumebox.presentation.screen.node


import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.github.yumeyucca.yumebox.core.model.Proxy
import com.github.yumeyucca.yumebox.presentation.component.CountryFlagCircle
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.BadgeDollarSign
import com.github.yumeyucca.yumebox.presentation.icon.yume.CircleGauge
import com.github.yumeyucca.yumebox.presentation.icon.yume.Tags
import com.github.yumeyucca.yumebox.presentation.icon.yume.clock
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.presentation.util.extractNodeTags
import tf.gal.yumebox.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.pressable

@Composable
internal fun nodeLatencyLabel(delay: Int?): Pair<String, Color>? =
    when {
        delay == null || delay == 0 ->
            "Ping" to MiuixTheme.colorScheme.onSurfaceVariantSummary
        delay < 0 -> YumeTxt.Proxy.Node.Timeout to AppTheme.colors.latency.timeout
        delay in 1..300 ->
            YumeTxt.Home.NodeInfo.DelayValue.format(delay) to AppTheme.colors.latency.fast

        delay in 301..1000 ->
            YumeTxt.Home.NodeInfo.DelayValue.format(delay) to AppTheme.colors.latency.moderate

        delay > 1000 ->
            YumeTxt.Home.NodeInfo.DelayValue.format(delay) to AppTheme.colors.latency.slow

        else ->
            YumeTxt.Home.NodeInfo.DelayValue.format(delay) to AppTheme.colors.latency.slow
    }

internal fun displayName(type: String): String =
    when (type) {
        Proxy.Type.Shadowsocks -> "SS"
        Proxy.Type.ShadowsocksR -> "SSR"
        Proxy.Type.Socks5 -> "SOCKS5"
        Proxy.Type.Http -> "HTTP"
        Proxy.Type.Vmess -> "VMess"
        Proxy.Type.Vless -> "VLESS"
        Proxy.Type.Tuic -> "TUIC"
        Proxy.Type.Dns -> "DNS"
        Proxy.Type.Ssh -> "SSH"
        else -> type
    }

internal fun iconLabel(type: String): String =
    when (type) {
        Proxy.Type.Direct -> "DI"
        Proxy.Type.Reject -> "RJ"
        Proxy.Type.RejectDrop -> "RD"
        Proxy.Type.Compatible -> "CP"
        Proxy.Type.Pass -> "PS"
        Proxy.Type.PassRule -> "PR"
        Proxy.Type.Relay -> "RL"
        Proxy.Type.Selector -> "SE"
        Proxy.Type.Fallback -> "FB"
        Proxy.Type.URLTest -> "UT"
        Proxy.Type.LoadBalance -> "LB"
        Proxy.Type.Smart -> "SM"
        Proxy.Type.Unknown -> "UN"
        Proxy.Type.Shadowsocks -> "SS"
        Proxy.Type.ShadowsocksR -> "SR"
        Proxy.Type.Snell -> "SN"
        Proxy.Type.Socks5 -> "S5"
        Proxy.Type.Http -> "HT"
        Proxy.Type.Vmess -> "VM"
        Proxy.Type.Vless -> "VL"
        Proxy.Type.Trojan -> "TR"
        Proxy.Type.Hysteria -> "HY"
        Proxy.Type.Hysteria2 -> "H2"
        Proxy.Type.Tuic -> "TU"
        Proxy.Type.WireGuard -> "WG"
        Proxy.Type.Dns -> "DN"
        Proxy.Type.Ssh -> "SH"
        Proxy.Type.Mieru -> "MI"
        Proxy.Type.AnyTLS -> "AT"
        Proxy.Type.Sudoku -> "SU"
        Proxy.Type.Masque -> "MQ"
        Proxy.Type.TrustTunnel -> "TT"
        Proxy.Type.OpenVPN -> "OP"
        Proxy.Type.Tailscale -> "Tl"
        Proxy.Type.GostRelay -> "GR"
        else -> type.take(2).uppercase()
    }

@Composable
internal fun RotatingCircleGauge(
    isRotating: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = MiuixTheme.colorScheme.primary,
    contentDescription: String? = YumeTxt.Proxy.Action.Test,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "circle_gauge_rotation")
    val rotation by
    infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec =
            infiniteRepeatable(animation = tween(durationMillis = 1000, easing = LinearEasing)),
        label = "circle_gauge_rotation_value",
    )

    Icon(
        imageVector = Yume.CircleGauge,
        contentDescription = contentDescription,
        tint = tint,
        modifier = if (isRotating) modifier.rotate(rotation) else modifier,
    )
}

@Composable
internal fun NodeSelectableCard(
    isSelected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    paddingVertical: Dp,
    content: @Composable BoxScope.(isSelected: Boolean) -> Unit,
) {
    val radii = AppTheme.radii
    val sizes = AppTheme.sizes
    val interactionSource = remember { MutableInteractionSource() }
    val hapticFeedback = LocalHapticFeedback.current
    val shape = RoundedCornerShape(radii.radius18)
    val backgroundColor = MiuixTheme.colorScheme.background

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .let {
                    if (onClick != null) {
                        it.pressable(
                            interactionSource = interactionSource,
                            indication = SinkFeedback(),
                        )
                    } else {
                        it
                    }
                }
                .clip(shape)
                .background(backgroundColor)
                .let {
                    if (onClick != null) {
                        it.clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = {
                                if (!isSelected) {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.VirtualKey)
                                }
                                onClick()
                            },
                        )
                    } else {
                        it
                    }
                }
                .padding(horizontal = sizes.nodeCardPaddingHorizontal, vertical = paddingVertical),
        content = { content(isSelected) },
    )
}

@Composable
internal fun NodeCard(
    proxy: Proxy,
    isSelected: Boolean,
    onClick: ((String) -> Unit)?,
    onTestClick: ((String) -> Unit)? = null,
    isDelayTesting: Boolean = false,
    modifier: Modifier = Modifier,
    showCountryFlag: Boolean = true,
) {
    val spacing = AppTheme.spacing
    val sizes = AppTheme.sizes
    val onCardClick = onClick?.let { click -> { click(proxy.name) } }

    NodeSelectableCard(
        isSelected = isSelected,
        onClick = onCardClick,
        modifier = modifier,
        paddingVertical = sizes.nodeCardPaddingVertical,
    ) { selected ->
        val presentation =
            remember(proxy.name, proxy.title) {
                resolveProxyDisplayPresentation(name = proxy.name, title = proxy.title)
            }
        val tags = remember(proxy.name) { extractNodeTags(proxy.name) }
        val delayLabel = nodeLatencyLabel(proxy.delay)
        val typeLabel = remember(proxy.type) { displayName(proxy.type) }
        val iconLabel = remember(proxy.type) { iconLabel(proxy.type) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(sizes.nodeCardContentGap),
        ) {
            NodeLargeIcon(
                countryCode = presentation.countryCode.takeIf { showCountryFlag },
                typeName = iconLabel,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(sizes.nodeCardTitleGap),
            ) {
                Text(
                    text = presentation.displayName,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(AppTheme.sizes.listItemVerticalMinimal),
                    verticalArrangement = Arrangement.spacedBy(spacing.space4),
                ) {
                    NodeTagChip(label = typeLabel)
                    tags.keywords.forEach { keyword -> NodeTagChip(label = keyword) }
                    tags.multiplier?.let { multiplier ->
                        if (multiplier > 0f) NodeMultiplierChip(multiplier = multiplier)
                    }
                    delayLabel?.let { (delayText, delayColor) ->
                        NodeLatencyChip(
                            label = delayText,
                            color = delayColor,
                            isTesting = isDelayTesting,
                            onClick = onTestClick?.let { test -> { test(proxy.name) } },
                        )
                    }
                }
            }

            NodeSelectionTag(
                isSelected = selected,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
    }
}

@Composable
internal fun NodeLargeIcon(modifier: Modifier = Modifier, countryCode: String?, typeName: String) {
    val opacity = AppTheme.opacity
    val sizes = AppTheme.sizes
    val neutral = MiuixTheme.colorScheme.onSurface
    Box(
        modifier =
            modifier
                .size(sizes.nodeLargeIconSize)
                .clip(RoundedCornerShape(sizes.nodeLargeIconCornerRadius))
                .background(neutral.copy(alpha = opacity.ambientLight + opacity.ambientShadow)),
        contentAlignment = Alignment.Center,
    ) {
        if (countryCode != null) {
            CountryFlagCircle(countryCode = countryCode, size = sizes.nodeLargeIconFlagSize)
        } else {
            Text(
                text = typeName.take(2),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

@Composable
private fun NodeTagChip(label: String) {
    val spacing = AppTheme.spacing
    val radii = AppTheme.radii
    val opacity = AppTheme.opacity
    val primary = MiuixTheme.colorScheme.primary
    Text(
        text = label,
        style = MiuixTheme.textStyles.footnote1.copy(fontSize = 10.sp),
        color = primary,
        modifier =
            Modifier
                .clip(RoundedCornerShape(radii.full))
                .background(primary.copy(alpha = opacity.subtle))
                .padding(horizontal = spacing.space4, vertical = spacing.space2),
    )
}

@Composable
private fun NodeMultiplierChip(multiplier: Float) {
    val spacing = AppTheme.spacing
    val radii = AppTheme.radii
    val opacity = AppTheme.opacity
    val appColors = AppTheme.colors
    val sizes = AppTheme.sizes
    val isHigh = multiplier >= 2.0f
    val primary = MiuixTheme.colorScheme.primary
    val chipBg =
        if (isHigh) appColors.status.destructiveContainer else primary.copy(alpha = opacity.subtle)
    val chipColor = if (isHigh) appColors.status.destructive else primary
    val label =
        if (multiplier == multiplier.toLong().toFloat()) {
            "x${multiplier.toLong()}"
        } else {
            "x$multiplier"
        }

    Row(
        modifier =
            Modifier
                .clip(RoundedCornerShape(radii.full))
                .background(chipBg)
                .padding(horizontal = spacing.space4, vertical = spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.space2),
    ) {
        Icon(
            imageVector = Yume.BadgeDollarSign,
            contentDescription = null,
            tint = chipColor,
            modifier = Modifier.size(sizes.nodeTagIconSize),
        )
        Text(
            text = label,
            style = MiuixTheme.textStyles.footnote1.copy(fontSize = 10.sp),
            color = chipColor,
        )
    }
}

@Composable
private fun NodeLatencyChip(
    label: String,
    color: Color,
    isTesting: Boolean,
    onClick: (() -> Unit)?,
) {
    val spacing = AppTheme.spacing
    val radii = AppTheme.radii
    val opacity = AppTheme.opacity
    val sizes = AppTheme.sizes

    Row(
        modifier =
            Modifier
                .clip(RoundedCornerShape(radii.full))
                .background(color.copy(alpha = opacity.subtle))
                .let { modifier ->
                    if (onClick == null) {
                        modifier
                    } else {
                        modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = SinkFeedback(),
                            enabled = !isTesting,
                            onClick = onClick,
                        )
                    }
                }
                .padding(horizontal = spacing.space4, vertical = spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.space2),
    ) {
        if (isTesting) {
            RotatingCircleGauge(
                isRotating = true,
                modifier = Modifier.size(sizes.nodeTagIconSize),
                tint = color,
                contentDescription = null,
            )
        } else {
            Icon(
                imageVector = Yume.clock,
                contentDescription = if (onClick == null) null else YumeTxt.Proxy.Action.Test,
                tint = color,
                modifier = Modifier.size(sizes.nodeTagIconSize),
            )
        }
        Text(
            text = label,
            style = MiuixTheme.textStyles.footnote1.copy(fontSize = 10.sp),
            color = color,
        )
    }
}

@Composable
private fun NodeSelectionTag(isSelected: Boolean, modifier: Modifier = Modifier) {
    val scale = remember { Animatable(if (isSelected) 0.72f else 1f) }

    LaunchedEffect(isSelected) {
        if (isSelected) {
            scale.snapTo(0.72f)
            scale.animateTo(
                targetValue = 1f,
                animationSpec =
                    keyframes {
                        durationMillis = 260
                        1.1f at 150 using FastOutSlowInEasing
                    },
            )
        }
    }
    if (!isSelected) return

    Icon(
        imageVector = Yume.Tags,
        contentDescription = null,
        tint = MiuixTheme.colorScheme.primary,
        modifier =
            modifier
                .size(UiDp.dp24)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                },
    )
}
