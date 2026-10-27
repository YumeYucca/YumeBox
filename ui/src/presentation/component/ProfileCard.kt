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


import android.annotation.SuppressLint
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.yumeyucca.yumebox.common.util.ByteFormatter
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.CircleFadingArrowUp
import com.github.yumeyucca.yumebox.presentation.icon.yume.Delete
import com.github.yumeyucca.yumebox.presentation.icon.yume.Edit
import com.github.yumeyucca.yumebox.presentation.icon.yume.Share
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.theme.LocalGlassBackdrop
import com.github.yumeyucca.yumebox.presentation.theme.LocalTopBarBackdrop
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.presentation.theme.horizontalPadding
import com.github.yumeyucca.yumebox.presentation.util.*
import com.github.yumeyucca.yumebox.runtime.api.Profile
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import io.github.yumeyucca.lucide.Lucide
import io.github.yumeyucca.lucide.lucide.CalendarCheck2
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.glass.*
import top.yukonga.miuix.kmp.layout.CascadingPopupDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils.Companion.PopupLayout
import java.io.File
import kotlin.math.PI
import kotlin.math.sin

/**
 * 订阅卡片。
 *
 * 交互约定:点勾选按钮 = 启停该配置;点卡片其余位置 = 弹出操作列表(编辑 / 更新 / 分享 / 删除)。
 * 卡片本身只保留「名称 + 启用勾选 + 流量 + 到期 + 波形进度」,更新时间等次要信息不再上卡。
 */
@Composable
fun ProfileCard(
    profile: Profile,
    workDir: File,
    isDownloading: Boolean = false,
    onExport: (Profile) -> Unit,
    onUpdate: (Profile) -> Unit,
    onDelete: (Profile) -> Unit,
    onEdit: (Profile) -> Unit,
    onToggleEnabled: (Profile) -> Unit,
    onOverrideSettings: ((Profile) -> Unit)? = null,
    onShareFile: ((Profile) -> Unit)? = null,
    onShareLink: ((Profile) -> Unit)? = null,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
) {
    val spacing = AppTheme.spacing
    val colorScheme = MiuixTheme.colorScheme
    val warnColor = AppTheme.colors.moe.pingWarning

    val isConfigSaved = remember(profile.uuid, profile.updatedAt) { profile.isConfigSaved(workDir) }

    val isActive = profile.active
    val traffic = profile.trafficPreview
    val expiry = profile.expiryPreview

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
    var actionsOpen by remember(profile.uuid) { mutableStateOf(false) }
    var actionsMounted by remember(profile.uuid) { mutableStateOf(false) }
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
            if (!isDownloading) {
                actionsMounted = true
                actionsOpen = true
            }
        },
    ) {
        // 标题 + 右上角启用勾选(独立点击,不触发卡片弹窗)。
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
                onToggle = { onToggleEnabled(profile) },
                modifier =
                    Modifier.glassPopupAnchor(
                        anchor = actionsMenuAnchor,
                        cornerRadius = 13.dp,
                        floating = true,
                    ),
            )
        }

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

    if (actionsMounted) {
        ProfileActionsMenu(
            show = actionsOpen,
            isDownloading = isDownloading,
            canUpdate = profile.shouldShowUpdateButton(),
            canExport = isConfigSaved,
            onDismiss = { actionsOpen = false },
            onDismissFinished = { actionsMounted = false },
            onEdit = { onEdit(profile) },
            onUpdate = { onUpdate(profile) },
            onShareFile = { (onShareFile ?: onExport)(profile) },
            onShareLink = { (onShareLink ?: onExport)(profile) },
            onDelete = { onDelete(profile) },
            anchor = actionsMenuAnchor,
            anchorContent = {
                Checkbox(
                    state = if (isActive) ToggleableState.On else ToggleableState.Off,
                    onClick = null,
                    enabled = !isDownloading,
                )
            },
        )
    }
}

/** 右上角启用勾选:使用 Miuix Checkbox,与标题行垂直居中。 */
@Composable
private fun ProfileEnabledToggle(
    isActive: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Checkbox(
        state = if (isActive) ToggleableState.On else ToggleableState.Off,
        onClick = if (enabled) onToggle else null,
        modifier = modifier,
        enabled = enabled,
    )
}

/** 卡片操作弹窗:节点页右上角同款的玻璃展开菜单。 */
@Composable
private fun ProfileActionsMenu(
    show: Boolean,
    isDownloading: Boolean,
    canUpdate: Boolean,
    canExport: Boolean,
    onDismiss: () -> Unit,
    onDismissFinished: () -> Unit,
    onEdit: () -> Unit,
    onUpdate: () -> Unit,
    onShareFile: () -> Unit,
    onShareLink: () -> Unit,
    onDelete: () -> Unit,
    anchor: GlassPopupAnchor,
    anchorContent: @Composable () -> Unit,
) {
    val strings = YumeTxt.Component.ProfileCard
    // PopupLayout relocates this content; capture the page backdrop before leaving its provider.
    val backdrop = LocalGlassBackdrop.current ?: LocalTopBarBackdrop.current
    val popupVisible = remember { mutableStateOf(false) }
    val popupVisuals = GlassPopupDefaults.visuals()
    val density = LocalDensity.current
    val arrowTurn = CascadingPopupDefaults.arrowRotation(LocalLayoutDirection.current)

    var shareExpanded by remember { mutableStateOf(false) }
    var sharePresent by remember { mutableStateOf(false) }
    var shareAnchor by remember { mutableStateOf(Rect.Zero) }
    var menuWidth by remember { mutableStateOf(0.dp) }
    val shareArrow by animateFloatAsState(
        targetValue = if (shareExpanded) arrowTurn else 0f,
        animationSpec = CascadingPopupDefaults.arrowSpring(shareExpanded),
        label = "profileShareArrow",
    )

    // 每次收起后重置二级菜单,下次打开从一级开始。
    LaunchedEffect(show) {
        if (show) {
            popupVisible.value = true
        } else {
            shareExpanded = false
        }
    }

    val actionsEnabled = !isDownloading

    PopupLayout(
        visible = popupVisible,
        enterTransition = EnterTransition.None,
        exitTransition = ExitTransition.None,
        enableWindowDim = false,
        enableBackHandler = false,
    ) {
        Box(Modifier.fillMaxSize()) {
            // Compose the component closed first, just like the persistent upstream sample.
            // The outer host otherwise creates it with show=true and skips its entering transition.
            var expanded by remember { mutableStateOf(false) }
            LaunchedEffect(show) { expanded = show }
            GlassTransformPopup(
                show = expanded,
                onDismissRequest = {
                    shareExpanded = false
                    onDismiss()
                },
                anchor = anchor,
                backdrop = backdrop,
                anchorContent = anchorContent,
                anchorAlpha = 0f,
                sizing = GlassPopupSizing(minWidth = 188.dp, maxWidth = 188.dp),
                visuals = popupVisuals,
                stacked = shareExpanded,
                alignToPageEnd = true,
                onMeasured = { menuWidth = with(density) { it.width.toDp() } },
                onDismissFinished = {
                    if (!show) {
                        popupVisible.value = false
                        onDismissFinished()
                    }
                },
            ) {
                GlassPopupItem(
                    text = strings.Edit,
                    icon = Yume.Edit,
                    iconSize = 18.dp,
                    iconGap = 15.dp,
                    enabled = actionsEnabled,
                    onClick = {
                        onDismiss()
                        onEdit()
                    },
                )
                if (canUpdate) {
                    GlassPopupItem(
                        text = strings.Update,
                        icon = Yume.CircleFadingArrowUp,
                        iconSize = 18.dp,
                        iconGap = 15.dp,
                        enabled = actionsEnabled,
                        onClick = {
                            onDismiss()
                            onUpdate()
                        },
                    )
                }
                GlassPopupItem(
                    text = strings.Share,
                    icon = Yume.Share,
                    iconSize = 18.dp,
                    iconGap = 15.dp,
                    enabled = actionsEnabled,
                    showArrow = true,
                    arrowRotation = { shareArrow * (1f - anchor.secondaryBackProgress) },
                    modifier =
                        Modifier.onGloballyPositioned {
                            if (!sharePresent) shareAnchor = it.boundsInRoot()
                        },
                    onClick = {
                        sharePresent = true
                        shareExpanded = true
                    },
                )
                GlassPopupItem(
                    text = strings.Delete,
                    icon = Yume.Delete,
                    iconSize = 18.dp,
                    iconGap = 15.dp,
                    enabled = actionsEnabled,
                    contentColor = AppTheme.colors.status.destructive,
                    onClick = {
                        onDismiss()
                        onDelete()
                    },
                )
            }
            GlassSecondaryPopup(
                show = expanded && shareExpanded,
                onDismissRequest = { shareExpanded = false },
                anchorBounds = shareAnchor,
                backdrop = backdrop,
                materialAnchor = anchor,
                sizing = GlassPopupSizing(minWidth = menuWidth, maxWidth = 188.dp),
                visuals = popupVisuals,
                onDismissFinished = { sharePresent = false },
            ) {
                GlassPopupItem(
                    text = strings.Share,
                    showArrow = true,
                    arrowRotation = { shareArrow * (1f - anchor.secondaryBackProgress) },
                    onClick = { shareExpanded = false },
                )
                HorizontalDivider(
                    modifier =
                        Modifier.padding(
                            horizontal = GlassPopupDefaults.DividerPaddingHorizontal,
                            vertical = GlassPopupDefaults.DividerPaddingVertical,
                        ),
                    thickness = GlassPopupDefaults.DividerThickness,
                )
                GlassPopupItem(
                    text = strings.ShareFile,
                    enabled = actionsEnabled && canExport,
                    onClick = {
                        shareExpanded = false
                        onDismiss()
                        onShareFile()
                    },
                )
                GlassPopupItem(
                    text = strings.ShareLink,
                    enabled = actionsEnabled && canUpdate,
                    onClick = {
                        shareExpanded = false
                        onDismiss()
                        onShareLink()
                    },
                )
            }
        }
    }
}

@Composable
private fun ProfileExpiryBadge(
    expiry: ProfileExpiryPreview,
    modifier: Modifier = Modifier,
) {
    val spacing = AppTheme.spacing
    val opacity = AppTheme.opacity
    val colorScheme = MiuixTheme.colorScheme
    val warnColor = AppTheme.colors.moe.pingWarning

    val headline = formatExpiryHeadline(expiry.daysLeft)

    Box(
        modifier =
            modifier
                .padding(start = spacing.space8)
                .height(UiDp.dp32)
                .clip(RoundedCornerShape(UiDp.dp10))
                .background(
                    if (expiry.warning) {
                        warnColor.copy(alpha = opacity.subtle)
                    } else {
                        colorScheme.primary.copy(alpha = opacity.subtle)
                    }
                )
                .padding(horizontal = spacing.space8),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = headline,
                fontSize = 12.sp,
                fontWeight = FontWeight(700),
                color = if (expiry.warning) warnColor else colorScheme.onSurface,
                maxLines = 1,
            )
            val dateColor =
                if (expiry.warning) {
                    warnColor.copy(alpha = opacity.mediumStrong)
                } else {
                    colorScheme.onSurfaceVariantSummary
                }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.space2),
            ) {
                Icon(
                    imageVector = Lucide.CalendarCheck2,
                    contentDescription = null,
                    tint = dateColor,
                    modifier = Modifier.size(10.dp),
                )
                Text(
                    text = expiry.dateText,
                    fontSize = 10.sp,
                    color = dateColor,
                    maxLines = 1,
                    style = TextStyle(fontFeatureSettings = "tnum"),
                )
            }
        }
    }
}

// 约 10 年及以上视为永久。
private const val EXPIRY_PERMANENT_DAYS = 3650L

// 到期倒计时按量级换算单位:天 → 个月 → 年 → 永久,避免"3649 天后到期"这种长数值。
private fun formatExpiryHeadline(daysLeft: Long): String {
    val strings = YumeTxt.Component.ProfileCard
    return when {
        daysLeft < 0L -> strings.ExpiredShort
        daysLeft == 0L -> strings.ExpireTodayShort
        daysLeft >= EXPIRY_PERMANENT_DAYS -> strings.ExpireInPermanent
        daysLeft < 60L -> strings.ExpireInDays.format(daysLeft.toInt())
        daysLeft < 365L -> strings.ExpireInMonths.format((daysLeft / 30L).toInt())
        else -> strings.ExpireInYears.format((daysLeft / 365L).toInt())
    }
}

@Composable
private fun TrafficWaveProgress(
    ratio: Float,
    warn: Boolean,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MiuixTheme.colorScheme
    val opacity = AppTheme.opacity

    val waveColor = if (warn) AppTheme.colors.moe.pingWarning else colorScheme.primary
    val trackColor = colorScheme.onSurface.copy(alpha = opacity.subtleStrong)
    val animatedRatio by animateFloatAsState(
        targetValue = ratio.coerceIn(0f, 1f),
        animationSpec = WavyProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "profileTrafficRatio",
    )
    // Phase crawls forward one wavelength per cycle so the wave is visibly alive; both ends
    // taper back to the centerline so the start and the head stay clean at any phase.
    val phase by rememberInfiniteTransition(label = "waveCrawl").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "waveCrawlPhase",
    )

    Canvas(
        modifier =
            modifier
                .height(UiDp.dp12)
                .semantics {
                    progressBarRangeInfo =
                        ProgressBarRangeInfo(current = animatedRatio, range = 0f..1f)
                },
    ) {
        val strokePx = UiDp.dp4.toPx()
        val gapPx = UiDp.dp8.toPx()
        val wavelengthPx = UiDp.dp20.toPx()
        val centerY = size.height / 2f
        val amplitude = (size.height - strokePx) / 2f * 0.9f
        val headX = ((size.width - gapPx) * animatedRatio).coerceAtLeast(0f)

        if (headX > strokePx) {
            val taperLength = (wavelengthPx * 0.6f).coerceAtMost(headX / 2f)
            // Pixel-step sampling: a dp-sized step collapses a 20dp wavelength into a handful of
            // segments and the polyline reads as jagged crests. 2px keeps the sine smooth.
            val stepPx = 2f
            val steps = maxOf(8, (headX / stepPx).toInt())
            val halfPi = (PI / 2).toFloat()
            val twoPi = (PI * 2).toFloat()
            val path = Path()
            for (i in 0..steps) {
                val x = headX * i / steps
                val startTaper = sin((x / taperLength).coerceIn(0f, 1f) * halfPi)
                val headTaper =
                    sin(((headX - x) / taperLength).coerceIn(0f, 1f) * halfPi)
                val y =
                    centerY -
                            amplitude *
                                    startTaper *
                                    headTaper *
                                    sin(twoPi * (x / wavelengthPx - phase))
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = waveColor,
                style =
                    Stroke(
                        width = strokePx,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
            )
        }

        val trackStart = headX + gapPx
        if (trackStart < size.width - strokePx / 2f) {
            drawLine(
                color = trackColor,
                start = Offset(trackStart, centerY),
                end = Offset(size.width, centerY),
                strokeWidth = strokePx,
                cap = StrokeCap.Round,
            )
        }
    }
}
