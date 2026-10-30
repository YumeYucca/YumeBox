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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
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
import com.github.yumeyucca.yumebox.presentation.theme.inactiveSurfaceColor
import com.github.yumeyucca.yumebox.presentation.util.extractNodeTags
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.pressable

/** Measured delay rendered as a chip label plus the colour its bucket resolves to. */
@Immutable
internal data class NodeLatency(val text: String, val color: Color)

/** "Ping" reads the same in every shipped language, so it stays out of the generated locale files. */
private const val UntestedLatencyText = "Ping"

@Composable
internal fun nodeLatencyLabel(delay: Int?): NodeLatency =
    when {
        delay == null || delay == 0 ->
            NodeLatency(UntestedLatencyText, MiuixTheme.colorScheme.onSurfaceVariantSummary)
        delay < 0 -> NodeLatency(YumeTxt.Proxy.Node.Timeout, AppTheme.colors.latency.timeout)
        delay in 1..300 -> NodeLatency(YumeTxt.Home.NodeInfo.DelayValue.format(delay), AppTheme.colors.latency.fast)
        delay in 301..1000 ->
            NodeLatency(YumeTxt.Home.NodeInfo.DelayValue.format(delay), AppTheme.colors.latency.moderate)

        else -> NodeLatency(YumeTxt.Home.NodeInfo.DelayValue.format(delay), AppTheme.colors.latency.slow)
    }

/** Delay measured by the running core for [proxyName], falling back to the snapshot's own value. */
@Composable
internal fun rememberLiveNodeDelay(proxyName: String, fallbackDelay: Int): Int {
    val delayProvider = LocalProxyDelayFlow.current
    val delayFlow = remember(proxyName, delayProvider) { delayProvider(proxyName) }
    val liveDelay by delayFlow.collectAsState()
    return liveDelay ?: fallbackDelay
}

/**
 * 每种协议只在这张表里描述一次:[badge] 是卡片角标,[name] 是给人看的协议名,为空就显示原始类型串。
 * 以前角标和显示名是两张各三十多行的 when,改一种协议要记得同时改两处。
 */
private data class NodeTypeLabel(val badge: String, val name: String? = null)

private val nodeTypeLabels =
    mapOf(
        Proxy.Type.Direct to NodeTypeLabel("DI"),
        Proxy.Type.Reject to NodeTypeLabel("RJ"),
        Proxy.Type.RejectDrop to NodeTypeLabel("RD"),
        Proxy.Type.Compatible to NodeTypeLabel("CP"),
        Proxy.Type.Pass to NodeTypeLabel("PS"),
        Proxy.Type.PassRule to NodeTypeLabel("PR"),
        Proxy.Type.Relay to NodeTypeLabel("RL"),
        Proxy.Type.Selector to NodeTypeLabel("SE"),
        Proxy.Type.Fallback to NodeTypeLabel("FB"),
        Proxy.Type.URLTest to NodeTypeLabel("UT"),
        Proxy.Type.LoadBalance to NodeTypeLabel("LB"),
        Proxy.Type.Smart to NodeTypeLabel("SM"),
        Proxy.Type.Unknown to NodeTypeLabel("UN"),
        Proxy.Type.Shadowsocks to NodeTypeLabel("SS", name = "SS"),
        Proxy.Type.ShadowsocksR to NodeTypeLabel("SR", name = "SSR"),
        Proxy.Type.Snell to NodeTypeLabel("SN"),
        Proxy.Type.Socks5 to NodeTypeLabel("S5", name = "SOCKS5"),
        Proxy.Type.Http to NodeTypeLabel("HT", name = "HTTP"),
        Proxy.Type.Vmess to NodeTypeLabel("VM", name = "VMess"),
        Proxy.Type.Vless to NodeTypeLabel("VL", name = "VLESS"),
        Proxy.Type.Trojan to NodeTypeLabel("TR"),
        Proxy.Type.Hysteria to NodeTypeLabel("HY"),
        Proxy.Type.Hysteria2 to NodeTypeLabel("H2"),
        Proxy.Type.Tuic to NodeTypeLabel("TU", name = "TUIC"),
        Proxy.Type.WireGuard to NodeTypeLabel("WG"),
        Proxy.Type.Dns to NodeTypeLabel("DN", name = "DNS"),
        Proxy.Type.Ssh to NodeTypeLabel("SH", name = "SSH"),
        Proxy.Type.Mieru to NodeTypeLabel("MI"),
        Proxy.Type.AnyTLS to NodeTypeLabel("AT"),
        Proxy.Type.Sudoku to NodeTypeLabel("SU"),
        Proxy.Type.Masque to NodeTypeLabel("MQ"),
        Proxy.Type.TrustTunnel to NodeTypeLabel("TT"),
        Proxy.Type.OpenVPN to NodeTypeLabel("OP"),
        Proxy.Type.Tailscale to NodeTypeLabel("Tl"),
        Proxy.Type.GostRelay to NodeTypeLabel("GR"),
    )

internal fun displayName(type: String): String = nodeTypeLabels[type]?.name ?: type

internal fun iconLabel(type: String): String = nodeTypeLabels[type]?.badge ?: type.take(2).uppercase()

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
    paddingHorizontal: Dp = UiDp.dp16,
    cornerRadius: Dp = UiDp.dp24,
    content: @Composable BoxScope.(isSelected: Boolean, selectionProgress: Float) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hapticFeedback = LocalHapticFeedback.current
    val shape = RoundedCornerShape(cornerRadius)
    val backgroundColor = MiuixTheme.colorScheme.background
    val selectionProgress by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
        label = "node_card_selection",
    )
    val selectionBorderColor by animateColorAsState(
        targetValue =
            if (isSelected) {
                MiuixTheme.colorScheme.primary
            } else {
                MiuixTheme.colorScheme.primary.copy(alpha = 0f)
            },
        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
        label = "node_card_selection_border",
    )

    // Selection ring mirrors ProfileCard: the ring is a later sibling at the card's own bounds,
    // OUTSIDE the card body's clip — inside it, the RoundedCornerShape clip shaves the squircle
    // stroke at the corners. squircleBorder paints behind its node, hence the overlay on top.
    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier =
                Modifier
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
                                        hapticFeedback.performHapticFeedback(
                                            HapticFeedbackType.VirtualKey
                                        )
                                    }
                                    onClick()
                                },
                            )
                        } else {
                            it
                        }
                    }
                    .padding(horizontal = paddingHorizontal, vertical = paddingVertical),
        ) {
            content(isSelected, selectionProgress)
        }
        Box(
            modifier =
                Modifier.matchParentSize().squircleBorder(
                    width = 1.5.dp,
                    color = selectionBorderColor,
                    cornerRadius = cornerRadius,
                ),
        )
    }
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
    compact: Boolean = false,
) {
    val spacing = AppTheme.spacing
    val sizes = AppTheme.sizes
    val onCardClick = onClick?.let { click -> { click(proxy.name) } }

    NodeSelectableCard(
        isSelected = isSelected,
        onClick = onCardClick,
        modifier = modifier,
        paddingVertical = sizes.nodeCardPaddingVertical,
        paddingHorizontal = if (compact) UiDp.dp12 else sizes.nodeCardPaddingHorizontal,
        cornerRadius = if (compact) UiDp.dp20 else UiDp.dp24,
    ) { selected, _ ->
        val presentation =
            remember(proxy.name, proxy.title) {
                resolveProxyDisplayPresentation(name = proxy.name, title = proxy.title)
            }
        if (compact) {
            NodeCompactCardContent(
                proxy = proxy,
                presentation = presentation,
                isDelayTesting = isDelayTesting,
                onTestClick = onTestClick,
                showCountryFlag = showCountryFlag,
            )
        } else {
            val tags = remember(proxy.name) { extractNodeTags(proxy.name) }
            val typeLabel = remember(proxy.type) { displayName(proxy.type) }
            val iconLabel = remember(proxy.type) { iconLabel(proxy.type) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(sizes.nodeCardContentGap),
            ) {
                NodeLargeIcon(
                    countryCode = presentation.countryCode,
                    showCountryFlag = showCountryFlag,
                    typeName = iconLabel,
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(sizes.nodeCardTitleGap),
                ) {
                    NodeDisplayName(presentation.displayName)

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
                        LiveNodeLatencyChip(
                            proxy = proxy,
                            isDelayTesting = isDelayTesting,
                            onTestClick = onTestClick,
                        )
                    }
                }

                NodeSelectionTag(
                    isSelected = selected,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
            }
        }
    }
}

@Composable
private fun NodeCompactCardContent(
    proxy: Proxy,
    presentation: ProxyDisplayPresentation,
    isDelayTesting: Boolean,
    onTestClick: ((String) -> Unit)?,
    showCountryFlag: Boolean,
) {
    val spacing = AppTheme.spacing
    val iconLabel = remember(proxy.type) { iconLabel(proxy.type) }

    Column(verticalArrangement = Arrangement.spacedBy(spacing.space6)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(UiDp.dp6),
        ) {
            if (showCountryFlag) {
                CountryFlagCircle(
                    countryCode = presentation.countryCode,
                    size = UiDp.dp20,
                )
            } else {
                Text(
                    text = iconLabel,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            NodeDisplayName(
                presentation.displayName,
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = proxy.type,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Box(modifier = Modifier.padding(start = UiDp.dp8)) {
                LiveNodeLatencyChip(
                    proxy = proxy,
                    isDelayTesting = isDelayTesting,
                    onTestClick = onTestClick,
                )
            }
        }
    }
}

/** A node's name: one line, one style, wherever a card shows it. */
@Composable
private fun NodeDisplayName(name: String, modifier: Modifier = Modifier) {
    Text(
        text = name,
        style = MiuixTheme.textStyles.body2,
        color = MiuixTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
private fun LiveNodeLatencyChip(
    proxy: Proxy,
    isDelayTesting: Boolean,
    onTestClick: ((String) -> Unit)?,
) {
    val latency = rememberNodeDelayLabel(proxy)
    NodeLatencyChip(
        label = latency.text,
        color = latency.color,
        isTesting = isDelayTesting,
        onClick = onTestClick?.let { test -> { test(proxy.name) } },
    )
}

@Composable
private fun rememberNodeDelayLabel(proxy: Proxy): NodeLatency =
    nodeLatencyLabel(rememberLiveNodeDelay(proxy.name, proxy.delay))

@Composable
internal fun NodeLargeIcon(
    modifier: Modifier = Modifier,
    countryCode: String?,
    showCountryFlag: Boolean,
    typeName: String,
) {
    val sizes = AppTheme.sizes
    val backgroundColor = inactiveSurfaceColor()
    Box(
        modifier =
            modifier
                .size(sizes.nodeLargeIconSize)
                .clip(RoundedCornerShape(sizes.nodeLargeIconCornerRadius))
                .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        if (showCountryFlag) {
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

/** The 10sp label every node chip shares. */
@Composable
private fun NodeChipLabel(text: String, color: Color) {
    Text(
        text = text,
        style = MiuixTheme.textStyles.footnote1.copy(fontSize = 10.sp),
        color = color,
    )
}

@Composable
private fun NodeTagChip(label: String) {
    val opacity = AppTheme.opacity
    val primary = MiuixTheme.colorScheme.primary
    NodeChip(backgroundColor = primary.copy(alpha = opacity.subtle)) {
        NodeChipLabel(text = label, color = primary)
    }
}

@Composable
private fun NodeMultiplierChip(multiplier: Float) {
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

    NodeChip(backgroundColor = chipBg) {
        Icon(
            imageVector = Yume.BadgeDollarSign,
            contentDescription = null,
            tint = chipColor,
            modifier = Modifier.size(sizes.nodeTagIconSize),
        )
        NodeChipLabel(text = label, color = chipColor)
    }
}

@Composable
private fun NodeLatencyChip(
    label: String,
    color: Color,
    isTesting: Boolean,
    onClick: (() -> Unit)?,
) {
    val sizes = AppTheme.sizes
    val backgroundColor = inactiveSurfaceColor()

    NodeChip(
        backgroundColor = backgroundColor,
        onClick = onClick,
        enabled = !isTesting,
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
        NodeChipLabel(text = label, color = color)
    }
}

@Composable
private fun NodeChip(
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val spacing = AppTheme.spacing
    val radii = AppTheme.radii
    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(radii.full))
                .background(backgroundColor)
                .let { chipModifier ->
                    if (onClick == null) {
                        chipModifier
                    } else {
                        chipModifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = SinkFeedback(),
                            enabled = enabled,
                            onClick = onClick,
                        )
                    }
                }
                .padding(horizontal = spacing.space4, vertical = spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.space2),
        content = content,
    )
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
