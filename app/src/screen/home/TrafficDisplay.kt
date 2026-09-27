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

@file:Suppress("FunctionName", "UnusedVariable")

package com.github.yumeyucca.yumebox.screen.home

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.github.yumeyucca.yumebox.common.util.formatBytesForDisplay
import com.github.yumeyucca.yumebox.core.model.RunMode
import com.github.yumeyucca.yumebox.core.model.TunnelState
import com.github.yumeyucca.yumebox.domain.model.TrafficData
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.*
import com.github.yumeyucca.yumebox.presentation.theme.AnimationSpecs
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun TrafficDisplay(
    trafficNow: TrafficData,
    profileName: String?,
    tunnelMode: TunnelState.Mode?,
    controlState: HomeProxyControlState,
    proxyMode: RunMode,
    isRemoteController: Boolean,
    isEnabled: Boolean,
    onOpenPanel: (() -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = AppTheme.spacing
    val componentSizes = AppTheme.sizes

    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(
                    enabled = isEnabled,
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                )
                .padding(top = componentSizes.homeTrafficTopPadding, bottom = spacing.space16),
        verticalArrangement = Arrangement.spacedBy(spacing.space24),
    ) {
        DownloadSection(
            downloadSpeed = trafficNow.download,
            profileName = profileName,
            tunnelMode = tunnelMode,
        )

        UploadSection(
            uploadSpeed = trafficNow.upload,
            controlState = controlState,
            proxyMode = proxyMode,
            isRemoteController = isRemoteController,
            onOpenPanel = onOpenPanel,
        )
    }
}

@Composable
private fun DownloadSection(
    downloadSpeed: Long,
    profileName: String?,
    tunnelMode: TunnelState.Mode?,
) {
    val spacing = AppTheme.spacing
    val componentSizes = AppTheme.sizes

    Column(horizontalAlignment = Alignment.Start) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = componentSizes.statusCapsuleHeight),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "DOWNLOAD",
                style = MiuixTheme.textStyles.footnote1.copy(fontSize = 14.sp),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )

            ProfileModeBadge(profileName = profileName, tunnelMode = tunnelMode)
        }

        SpeedValue(speed = downloadSpeed)
    }
}

@Composable
private fun ProfileModeBadge(profileName: String?, tunnelMode: TunnelState.Mode?) {
    if (profileName == null && tunnelMode == null) return

    val spacing = AppTheme.spacing
    val componentSizes = AppTheme.sizes
    val opacity = AppTheme.opacity

    Surface(
        color = MiuixTheme.colorScheme.primary.copy(alpha = opacity.subtle),
        shape = RoundedCornerShape(50),
        modifier = Modifier.height(componentSizes.statusCapsuleHeight),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = spacing.space12),
            horizontalArrangement = Arrangement.spacedBy(spacing.space8),
        ) {
            Text(
                text = profileName ?: YumeTxt.Home.Traffic.NoProfile,
                style =
                    MiuixTheme.textStyles.footnote1.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                color = MiuixTheme.colorScheme.primary,
            )

            Box(
                modifier =
                    Modifier
                        .size(spacing.space4)
                        .background(MiuixTheme.colorScheme.primary, CircleShape)
            )

            Text(
                text = tunnelMode.toDisplayName(),
                style =
                    MiuixTheme.textStyles.footnote1.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                color = MiuixTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun SpeedValue(speed: Long) {
    val spacing = AppTheme.spacing
    val opacity = AppTheme.opacity

    val (value, unit) = formatBytesForDisplay(speed)
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = value,
            style =
                MiuixTheme.textStyles.headline1.copy(
                    fontSize = trafficValueFontSize,
                    lineHeight = trafficValueFontSize,
                    letterSpacing = trafficValueLetterSpacing,
                ),
            color = MiuixTheme.colorScheme.primary,
        )
        Text(
            text = unit,
            style = MiuixTheme.textStyles.title2.copy(fontSize = trafficUnitFontSize),
            color = MiuixTheme.colorScheme.primary.copy(alpha = opacity.medium),
            modifier = Modifier.padding(bottom = spacing.space14, start = spacing.space8),
        )
    }
}

private val trafficValueFontSize = 96.sp
private val trafficValueLetterSpacing = (-3).sp
private val trafficUnitFontSize = 24.sp

@Composable
private fun UploadSection(
    uploadSpeed: Long,
    controlState: HomeProxyControlState,
    proxyMode: RunMode,
    isRemoteController: Boolean,
    onOpenPanel: (() -> Unit)?,
) {
    val spacing = AppTheme.spacing

    val (value, unit) = formatBytesForDisplay(uploadSpeed)
    val isRunning = controlState == HomeProxyControlState.Running
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(spacing.space12),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.space12),
        ) {
            Text(
                text = "UPLOAD",
                style = MiuixTheme.textStyles.footnote1.copy(fontSize = 14.sp),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Text(
                text = "$value $unit",
                style = MiuixTheme.textStyles.title2.copy(fontSize = 20.sp),
                color = MiuixTheme.colorScheme.primary,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.space8),
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier.animateContentSize(
                    tween(
                        AnimationSpecs.DURATION_FAST,
                        easing = AnimationSpecs.EmphasizedDecelerate,
                    )
                ),
        ) {
            ProxyStatusCapsule(controlState = controlState, isRemoteController = isRemoteController)
            AnimatedVisibility(
                visible = isRunning,
                enter =
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec =
                            tween(
                                AnimationSpecs.DURATION_FAST,
                                easing = AnimationSpecs.EmphasizedDecelerate,
                            ),
                    ) +
                            fadeIn(
                                tween(AnimationSpecs.DURATION_FAST, easing = AnimationSpecs.EnterEasing)
                            ),
                exit =
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec =
                            tween(
                                AnimationSpecs.DURATION_INSTANT,
                                easing = AnimationSpecs.EmphasizedAccelerate,
                            ),
                    ) +
                            fadeOut(
                                tween(
                                    AnimationSpecs.DURATION_INSTANT,
                                    easing = AnimationSpecs.ExitEasing,
                                )
                            ),
            ) {
                ProxyTypeCapsule(proxyMode = proxyMode)
            }
            if (isRunning && onOpenPanel != null) {
                NetworkPanelStatusButton(onClick = onOpenPanel)
            }
        }
    }
}

@Composable
private fun NetworkPanelStatusButton(onClick: () -> Unit) {
    val componentSizes = AppTheme.sizes
    val primary = MiuixTheme.colorScheme.primary
    Box(
        modifier =
            Modifier
                .size(componentSizes.statusCapsuleHeight)
                .clip(CircleShape)
                .background(primary.copy(alpha = AppTheme.opacity.subtle))
                .clickable(
                    role = Role.Button,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Yume.Zashboard,
            contentDescription = YumeTxt.Proxy.Action.Panel,
            tint = primary,
            modifier = Modifier.size(AppTheme.spacing.space16),
        )
    }
}

@Composable
private fun ProxyTypeCapsule(proxyMode: RunMode) {
    val spacing = AppTheme.spacing
    val componentSizes = AppTheme.sizes
    val opacity = AppTheme.opacity

    val primary = MiuixTheme.colorScheme.primary
    Surface(
        color = primary.copy(alpha = opacity.subtle),
        shape = RoundedCornerShape(50),
        modifier = Modifier.height(componentSizes.statusCapsuleHeight),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = spacing.space12),
            horizontalArrangement = Arrangement.spacedBy(spacing.space6),
        ) {
            Icon(
                imageVector =
                    when (proxyMode) {
                        RunMode.VpnService -> Yume.PlaneTakeoff
                        RunMode.Tun -> Yume.Tun
                        RunMode.Ebpf -> Yume.Tun
                    },
                contentDescription = null,
                tint = primary,
                modifier = Modifier.size(spacing.space12),
            )
            Text(
                text =
                    when (proxyMode) {
                        RunMode.VpnService -> YumeTxt.Home.ProxyMode.Vpn
                        RunMode.Tun -> YumeTxt.Home.ProxyMode.Tun
                        RunMode.Ebpf -> YumeTxt.Home.ProxyMode.Ebpf
                    },
                style =
                    MiuixTheme.textStyles.footnote1.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                color = primary,
            )
        }
    }
}

@Composable
private fun ProxyStatusCapsule(controlState: HomeProxyControlState, isRemoteController: Boolean) {
    val spacing = AppTheme.spacing
    val componentSizes = AppTheme.sizes
    val opacity = AppTheme.opacity

    val primary = MiuixTheme.colorScheme.primary
    Surface(
        color = primary.copy(alpha = opacity.subtle),
        shape = RoundedCornerShape(50),
        modifier =
            Modifier
                .height(componentSizes.statusCapsuleHeight)
                .animateContentSize(
                    tween(
                        AnimationSpecs.DURATION_FAST,
                        easing = AnimationSpecs.EmphasizedDecelerate,
                    )
                ),
    ) {
        AnimatedContent(
            targetState = controlState,
            transitionSpec = {
                (slideInHorizontally(
                    initialOffsetX = { it / 2 },
                    animationSpec =
                        tween(
                            AnimationSpecs.DURATION_FAST,
                            easing = AnimationSpecs.EmphasizedDecelerate,
                        ),
                ) +
                        fadeIn(
                            tween(AnimationSpecs.DURATION_FAST, easing = AnimationSpecs.EnterEasing)
                        ))
                    .togetherWith(
                        slideOutHorizontally(
                            targetOffsetX = { -it / 2 },
                            animationSpec =
                                tween(
                                    AnimationSpecs.DURATION_INSTANT,
                                    easing = AnimationSpecs.EmphasizedAccelerate,
                                ),
                        ) +
                                fadeOut(
                                    tween(
                                        AnimationSpecs.DURATION_INSTANT,
                                        easing = AnimationSpecs.ExitEasing,
                                    )
                                )
                    )
            },
            label = "CapsuleStateTransition",
        ) { state ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = spacing.space12),
                horizontalArrangement = Arrangement.spacedBy(spacing.space6),
            ) {
                Icon(
                    imageVector =
                        when (state) {
                            HomeProxyControlState.Idle -> Yume.Rocket
                            HomeProxyControlState.Connecting,
                            HomeProxyControlState.Disconnecting -> Yume.Waiting

                            HomeProxyControlState.Lost,
                            HomeProxyControlState.Running -> Yume.Activity
                        },
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(spacing.space12),
                )
                Text(
                    text =
                        when (state) {
                            HomeProxyControlState.Idle -> YumeTxt.Home.Status.TapToStart
                            HomeProxyControlState.Connecting -> YumeTxt.Home.Status.Connecting
                            HomeProxyControlState.Running ->
                                if (isRemoteController) {
                                    YumeTxt.Home.Status.Running
                                } else {
                                    YumeTxt.Home.Status.Running
                                }

                            HomeProxyControlState.Lost -> YumeTxt.Home.Status.Lost
                            HomeProxyControlState.Disconnecting -> YumeTxt.Home.Status.Disconnecting
                        },
                    style =
                        MiuixTheme.textStyles.footnote1.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    color = primary,
                )
            }
        }
    }
}

private fun TunnelState.Mode?.toDisplayName(): String =
    when (this) {
        TunnelState.Mode.Direct -> "Direct"
        TunnelState.Mode.Global -> "Global"
        TunnelState.Mode.Rule -> "Rule"
        else -> "Rule"
    }
