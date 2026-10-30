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

package com.github.yumeyucca.yumebox.presentation.component

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.yumeyucca.yumebox.common.util.ByteFormatter
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.theme.horizontalPadding
import com.github.yumeyucca.yumebox.presentation.util.*
import com.github.yumeyucca.yumebox.runtime.api.Profile
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.glass.*
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlinx.coroutines.delay

/**
 * 订阅卡片。
 *
 * 交互约定:点右上角开关 = 启停该配置;点卡片其余位置 = 弹出操作列表(启用 / 编辑 / 覆写 / 更新 / 分享 / 删除)。
 * 卡片本身只保留「名称 + 启用开关 + 流量 + 到期 + 波形进度」,更新时间等次要信息不再上卡。
 *
 * 动作由调用方决定:[onAction] 收到用户选中的菜单项,[canOverride] 决定菜单是否提供「覆写」行;
 * 卡片自己不判断业务,也不认识任何具体跳转。
 */
@Composable
fun ProfileCard(
    profile: Profile,
    onAction: (Profile, ProfileAction) -> Unit,
    onToggleEnabled: (Profile, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isDownloading: Boolean = false,
    canOverride: Boolean = false,
) {
    val spacing = AppTheme.spacing
    val colorScheme = MiuixTheme.colorScheme
    val warnColor = AppTheme.colors.moe.pingWarning

    // Keep the pill on the user's chosen state while the profile list refreshes. A stale refresh
    // must not flip "当前配置" back to "未启用" for a frame.
    var pendingActive by remember(profile.uuid) { mutableStateOf<Boolean?>(null) }
    val isActive = pendingActive ?: profile.active
    LaunchedEffect(profile.active, pendingActive) {
        val pending = pendingActive ?: return@LaunchedEffect
        // Let a burst of refresh emissions settle before handing the pill back to the list.
        delay(
            if (pending == profile.active) {
                ProfileToggleSettleMillis
            } else {
                ProfileToggleTimeoutMillis
            },
        )
        if (pendingActive == pending) pendingActive = null
    }
    val toggleEnabled = {
        val target = !isActive
        pendingActive = target
        onToggleEnabled(profile, target)
    }
    val traffic = profile.trafficPreview
    val expiry = profile.expiryPreview
    val progressColor = if (traffic?.lowTraffic == true) warnColor else colorScheme.primary

    val fallbackInfoText =
        when {
            traffic != null -> ""
            profile.usedBytes > 0 ->
                YumeTxt.Component.ProfileCard.UsedTraffic.format(
                    ByteFormatter.format(profile.usedBytes)
                )
            profile.type == Profile.Type.Url -> YumeTxt.Component.ProfileCard.ClickToUpdate
            else -> YumeTxt.Component.ProfileCard.LocalConfig
        }

    // 弹窗只在被打开的那张卡上挂载:关闭动画跑完(onDismissFinished)再卸载,列表里其余卡片不常驻弹窗。
    var actionsPhase by
        remember(profile.uuid) { mutableStateOf<ProfileActionsPhase>(ProfileActionsPhase.Hidden) }
    val beginActionsCollapse: (toggleWhenSettled: Boolean) -> Unit = { toggleWhenSettled ->
        val collapsing = actionsPhase as? ProfileActionsPhase.Collapsing
        actionsPhase =
            ProfileActionsPhase.Collapsing(
                // 菜单项先收起弹窗再回写开关,两次回调没有固定顺序,这里按「或」合并请求。
                toggleWhenSettled = toggleWhenSettled || collapsing?.toggleWhenSettled == true,
            )
    }
    val actionsMenuAnchor = rememberGlassPopupAnchor()

    AppCard(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(bottom = spacing.space12)
                .horizontalPadding(),
        applyHorizontalPadding = false,
        // 底部留白由 insideMargin 统一提供,进度条不再压到卡片边框。
        insideMargin =
            PaddingValues(
                start = spacing.space12,
                top = spacing.space12,
                end = spacing.space12,
                bottom = spacing.space12,
            ),
        onClick = {
            if (!isDownloading) actionsPhase = ProfileActionsPhase.Open
        },
    ) {
        // 标题 + 右上角启用开关(独立点击,不触发卡片弹窗)。
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(start = spacing.space2, end = spacing.space2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.space8),
        ) {
            Text(
                text = profile.name,
                fontSize = 18.sp,
                fontWeight = FontWeight(650),
                color = colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            ProfileEnabledToggle(
                isActive = isActive,
                enabled = !isDownloading,
                activeColor = progressColor,
                onToggle = toggleEnabled,
                modifier =
                    Modifier.glassPopupAnchor(
                        anchor = actionsMenuAnchor,
                        cornerRadius = 12.dp,
                        floating = true,
                    ),
            )
        }

        ProfileTrafficSummary(
            traffic = traffic,
            expiry = expiry,
            fallbackInfoText = fallbackInfoText,
        )
    }

    if (actionsPhase !is ProfileActionsPhase.Hidden) {
        ProfileActionsMenu(
            show = actionsPhase is ProfileActionsPhase.Open,
            availability =
                ProfileActionsAvailability(
                    isActive = isActive,
                    isDownloading = isDownloading,
                    canUpdate = profile.type == Profile.Type.Url,
                ),
            canOverride = canOverride,
            onDismiss = { beginActionsCollapse(false) },
            onDismissFinished = {
                val settled = actionsPhase as? ProfileActionsPhase.Collapsing
                actionsPhase = ProfileActionsPhase.Hidden
                if (settled?.toggleWhenSettled == true) toggleEnabled()
            },
            onToggleRequest = { beginActionsCollapse(true) },
            onAction = { action -> onAction(profile, action) },
            anchor = actionsMenuAnchor,
            anchorContent = {
                ProfileEnabledToggleVisual(
                    isActive = isActive,
                    enabled = !isDownloading,
                    activeColor = progressColor,
                )
            },
        )
    }
}

/**
 * 卡片操作弹窗的生命周期。
 *
 * 弹窗必须一直挂载到收起动画结束([Collapsing]),否则弹窗会在关闭动画中途被移出组合。
 * [Collapsing.toggleWhenSettled] 记录「收起后还要回写开关」:菜单里的开关项先收起弹窗再回调,
 * 开关动画必须等这次收起结束,否则药丸会在仍在收起的弹窗里抢先动起来。
 */
private sealed interface ProfileActionsPhase {
    object Hidden : ProfileActionsPhase

    object Open : ProfileActionsPhase

    data class Collapsing(val toggleWhenSettled: Boolean) : ProfileActionsPhase
}

/**
 * 开关乐观态的保留时长。
 *
 * 点击开关后本地先按目标态渲染,避免列表刷新把「当前配置」闪回「未启用」:
 * - [ProfileToggleSettleMillis]:列表已经回填了同一状态,再等一小段以吞掉连续多次刷新;
 * - [ProfileToggleTimeoutMillis]:列表迟迟没有回写(失败 / 离线),到点也放弃乐观态,
 *   让药丸回到列表的真实状态,而不是永久停在用户点选的位置。
 */
private const val ProfileToggleSettleMillis = 1_000L
private const val ProfileToggleTimeoutMillis = 4_000L

/**
 * 剩余流量、到期徽章与波形进度:卡片下半部分只负责排版,数据与配色由调用方给定。
 */
@Composable
private fun ProfileTrafficSummary(
    traffic: ProfileTrafficPreview?,
    expiry: ProfileExpiryPreview?,
    fallbackInfoText: String,
) {
    val spacing = AppTheme.spacing
    val colorScheme = MiuixTheme.colorScheme
    val warnColor = AppTheme.colors.moe.pingWarning
    // 剩余流量 + 到期徽章。
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .padding(top = spacing.space6, start = spacing.space2, end = spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (traffic != null) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(spacing.space4),
            ) {
                Text(
                    text = traffic.remainingNumber,
                    fontSize = 28.sp,
                    fontWeight = FontWeight(800),
                    letterSpacing = (-0.8).sp,
                    color =
                        if (traffic.lowTraffic) warnColor else colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(fontFeatureSettings = "tnum"),
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    text = YumeTxt.Component.ProfileCard.RemainingOfTotal.format(traffic.totalText),
                    fontSize = 13.sp,
                    color = colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(fontFeatureSettings = "tnum"),
                    modifier = Modifier.alignByBaseline(),
                )
            }
        } else {
            Text(
                text = fallbackInfoText,
                fontSize = 14.sp,
                color = colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        if (expiry != null) {
            ProfileExpiryBadge(expiry = expiry)
        }
    }

    // 波形进度保留卡片内边距,不再贴住底边。
    if (traffic != null) {
        TrafficWaveProgress(
            ratio = traffic.ratio,
            warn = traffic.lowTraffic,
            modifier =
                Modifier.fillMaxWidth().padding(
                    top = spacing.space12,
                    start = spacing.space2,
                    end = spacing.space2,
                ),
        )
    }
}
