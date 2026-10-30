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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.github.panpf.sketch.request.ImageRequest
import com.github.panpf.sketch.state.IntColorDrawableStateImage
import com.github.yumeyucca.yumebox.domain.model.ProxyGroupInfo
import com.github.yumeyucca.yumebox.domain.model.displayedSelectionDelay
import com.github.yumeyucca.yumebox.domain.model.resolveTerminalProxy
import com.github.yumeyucca.yumebox.presentation.component.CountryFlagCircle
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.chevron
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.presentation.theme.inactiveSurfaceColor
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.SinkFeedback
import com.github.panpf.sketch.AsyncImage as SketchAsyncImage

internal fun LazyListScope.nodeGroupItems(
    groups: List<ProxyGroupInfo>,
    onGroupClick: (ProxyGroupInfo) -> Unit,
    onGroupTest: (ProxyGroupInfo) -> Unit,
    testingGroupNames: Set<String> = emptySet(),
    itemVerticalPadding: Dp = UiDp.dp5,
) {
    itemsIndexed(
        items = groups,
        key = { _, group -> "${group.type}:${group.name}" },
        contentType = { _, _ -> "NodeGroupCard" },
    ) { _, group ->
        NodeGroupCard(
            group = group,
            allGroups = groups,
            isDelayTesting = testingGroupNames.contains(group.name),
            onClick = onGroupClick,
            onTestClick = onGroupTest,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = itemVerticalPadding),
        )
    }
}

@Composable
internal fun NodeGroupCard(
    group: ProxyGroupInfo,
    allGroups: List<ProxyGroupInfo>,
    isDelayTesting: Boolean,
    onClick: (ProxyGroupInfo) -> Unit,
    onTestClick: (ProxyGroupInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardShape = RoundedCornerShape(AppTheme.radii.radius24)
    val interactionSource = remember { MutableInteractionSource() }
    val currentProxy =
        remember(group.now, allGroups) {
            allGroups.resolveTerminalProxy(group.now)
                ?: group.proxies.firstOrNull { it.name == group.now }
        }
    val currentNode =
        remember(currentProxy?.name, currentProxy?.title, group.now) {
            resolveProxyDisplayPresentation(
                name = currentProxy?.name ?: group.now,
                title = currentProxy?.title,
            )
        }
    val currentNodeName =
        remember(currentNode.displayName, group.now) {
            currentNode.displayName
                .ifBlank { group.now.trim() }
                .ifBlank { YumeTxt.Proxy.Mode.Direct }
        }
    val iconUri =
        remember(group.icon) {
            group.icon?.trim()?.takeIf { it.isNotEmpty() }?.let(::normalizeNodeGroupIconUri)
        }
    val currentDelay =
        remember(group.now, allGroups) {
            allGroups.displayedSelectionDelay(group)
        }

    Column(
        modifier =
            modifier
                // Keep the press motion outside the card's visual layers so the shadow, shape,
                // background, and content move as one card. Use one clickable node rather than a
                // separate pressable + clickable pair, which can compete for the same gesture.
                .clickable(
                    interactionSource = interactionSource,
                    indication = SinkFeedback(),
                    onClick = { onClick(group) },
                )
                .shadow(
                    elevation = UiDp.dp4,
                    shape = cardShape,
                    ambientColor = Color.Black.copy(alpha = 0.05f),
                    spotColor = Color.Black.copy(alpha = 0.05f),
                )
                .clip(cardShape)
                .background(MiuixTheme.colorScheme.background)
                .padding(horizontal = UiDp.dp16, vertical = UiDp.dp10),
        verticalArrangement = Arrangement.spacedBy(UiDp.dp8),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(UiDp.dp16),
        ) {
            if (iconUri != null) {
                NodeGroupIcon(
                    iconUri = iconUri,
                    modifier = Modifier
                        .size(UiDp.dp44)
                        .clip(RoundedCornerShape(UiDp.dp14)),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(UiDp.dp10),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = group.name,
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    val primary = MiuixTheme.colorScheme.primary
                    Text(
                        text = group.type,
                        style = MiuixTheme.textStyles.footnote1.copy(fontSize = 10.sp),
                        color = primary,
                        modifier =
                            Modifier
                                .padding(start = UiDp.dp8)
                                .clip(RoundedCornerShape(UiDp.dp100))
                                .background(primary.copy(alpha = 0.1f))
                                .padding(horizontal = UiDp.dp8, vertical = UiDp.dp3),
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(UiDp.dp14))
                        .background(inactiveSurfaceColor())
                        .padding(horizontal = UiDp.dp10, vertical = UiDp.dp6),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(UiDp.dp8),
                            modifier = Modifier.weight(1f),
                        ) {
                            CountryFlagCircle(
                                countryCode = currentNode.countryCode,
                                size = UiDp.dp20,
                            )
                            Text(
                                text = currentNodeName,
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        Box(
                            modifier = Modifier.padding(start = UiDp.dp8),
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            GroupDelayLabel(
                                proxyName = currentProxy?.name ?: group.now,
                                fallbackDelay = currentDelay,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupDelayLabel(proxyName: String, fallbackDelay: Int) {
    // The group card keeps its navigation chevron until a real delay is available. The number is
    // the selected node's measurement, including a copy published on another group.
    val delay = rememberLiveNodeDelay(proxyName, fallbackDelay)
    val latency = if (delay != 0) nodeLatencyLabel(delay) else null
    if (latency != null) {
        Text(
            text = latency.text,
            style = MiuixTheme.textStyles.footnote1,
            color = latency.color,
        )
    } else {
        Icon(
            Yume.chevron,
            contentDescription = null,
            modifier = Modifier.size(UiDp.dp18),
            tint = AppTheme.colors.state.subtleDivider,
        )
    }
}

/** `scheme:` prefixes the app should keep as written instead of guessing a web host. */
private val SchemeUriRegex = Regex("^[a-zA-Z][a-zA-Z\\d+.-]*:.*$")

/** Bare `host.domain/path` inputs that are really https URLs missing their scheme. */
private val BareHostUriRegex = Regex("^[^/\\s]+\\.[^/\\s]+(?:/.*)?$")

private fun normalizeNodeGroupIconUri(raw: String): String {
    val normalized = raw.trim()
    if (normalized.startsWith("//")) return "https:$normalized"
    if (normalized.startsWith("www.", ignoreCase = true)) return "https://$normalized"
    if (normalized.matches(SchemeUriRegex)) return normalized
    if (normalized.matches(BareHostUriRegex)) return "https://$normalized"
    return normalized
}

@Composable
private fun NodeGroupIcon(iconUri: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val placeholderColorInt = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.10f).toArgb()
    val request =
        remember(context, iconUri, placeholderColorInt) {
            ImageRequest(context, iconUri) {
                placeholder(IntColorDrawableStateImage(placeholderColorInt))
                error(IntColorDrawableStateImage(placeholderColorInt))
                crossfade(true)
            }
        }
    SketchAsyncImage(
        request = request,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
}
