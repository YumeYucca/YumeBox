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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
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
import io.github.yumeyucca.lucide.lucide.FileBracesCorner
import io.github.yumeyucca.lucide.lucide.FileSymlink
import io.github.yumeyucca.lucide.lucide.Link
import io.github.yumeyucca.lucide.lucide.ToggleRight
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.glass.*
import top.yukonga.miuix.kmp.layout.CascadingPopupDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils.Companion.PopupLayout
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

/**
 * 订阅卡片。
 *
 * 交互约定:点右上角开关 = 启停该配置;点卡片其余位置 = 弹出操作列表(启用 / 编辑 / 覆写 / 更新 / 分享 / 删除)。
 * 卡片本身只保留「名称 + 启用开关 + 流量 + 到期 + 波形进度」,更新时间等次要信息不再上卡。
 */
@Composable
fun ProfileCard(
    profile: Profile,
    isDownloading: Boolean = false,
    onShareFile: (Profile) -> Unit,
    onShareLink: (Profile) -> Unit,
    onUpdate: (Profile) -> Unit,
    onDelete: (Profile) -> Unit,
    onEdit: (Profile) -> Unit,
    onToggleEnabled: (Profile, Boolean) -> Unit,
    onOverrideSettings: ((Profile) -> Unit)? = null,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
) {
    val spacing = AppTheme.spacing
    val colorScheme = MiuixTheme.colorScheme
    val warnColor = AppTheme.colors.moe.pingWarning

    // Keep the pill on the user's chosen state while the profile list refreshes. A stale refresh
    // must not flip "当前配置" back to "未启用" for a frame.
    var pendingActive by remember(profile.uuid) { mutableStateOf<Boolean?>(null) }
    val isActive = pendingActive ?: profile.active
    LaunchedEffect(profile.active, pendingActive) {
        if (pendingActive == profile.active) {
            // Let a burst of refresh emissions settle before handing the pill back to the list.
            delay(1000)
            if (pendingActive == profile.active) pendingActive = null
        }
    }
    LaunchedEffect(pendingActive) {
        val pending = pendingActive ?: return@LaunchedEffect
        delay(4000)
        if (pendingActive == pending) pendingActive = null
    }
    val toggleEnabled = {
        val target = !isActive
        pendingActive = target
        onToggleEnabled(profile, target)
    }
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
                onToggle = toggleEnabled,
                modifier =
                    Modifier.glassPopupAnchor(
                        anchor = actionsMenuAnchor,
                        cornerRadius = 12.dp,
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
            isActive = isActive,
            isDownloading = isDownloading,
            canUpdate = profile.shouldShowUpdateButton(),
            onDismiss = { actionsOpen = false },
            onDismissFinished = { actionsMounted = false },
            onToggleEnabled = toggleEnabled,
            onEdit = { onEdit(profile) },
            onOverrideSettings =
                onOverrideSettings?.let { callback -> { callback(profile) } },
            onUpdate = { onUpdate(profile) },
            onShareFile = { onShareFile(profile) },
            onShareLink = { onShareLink(profile) },
            onDelete = { onDelete(profile) },
            anchor = actionsMenuAnchor,
            anchorContent = {
                ProfileEnabledToggleVisual(
                    isActive = isActive,
                    enabled = !isDownloading,
                )
            },
        )
    }
}

/** 右上角启用开关:自绘胶囊 + 滑动圆点,与卡片下方的紧凑信息行保持同一视觉重量。 */
@Composable
private fun ProfileEnabledToggle(
    isActive: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    ProfileEnabledToggleVisual(
        isActive = isActive,
        enabled = enabled,
        modifier =
            modifier.toggleable(
                value = isActive,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = interactionSource,
                indication = null,
                onValueChange = { onToggle() },
            ),
    )
}

@Composable
private fun ProfileEnabledToggleVisual(
    isActive: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MiuixTheme.colorScheme
    val strings = YumeTxt.Component.ProfileCard
    val inactiveContainer =
        if (colorScheme.background.luminance() < 0.5f) {
            Color(0xFF131415)
        } else {
            Color(0xFFF7F7F7)
        }
    val containerColor by animateColorAsState(
        targetValue =
            if (isActive) {
                colorScheme.primary.copy(alpha = AppTheme.opacity.subtle)
            } else {
                inactiveContainer
            },
        animationSpec = tween(180),
        label = "profileEnabledContainer",
    )
    val contentColor by animateColorAsState(
        targetValue =
            if (isActive) {
                colorScheme.primary
            } else {
                colorScheme.onSurfaceVariantSummary
            },
        animationSpec = tween(180),
        label = "profileEnabledContent",
    )

    Box(
        modifier =
            modifier
                .height(24.dp)
                .widthIn(min = 64.dp)
                .clip(CircleShape)
                .background(containerColor)
                .alpha(if (enabled) 1f else 0.45f)
                .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (isActive) strings.CurrentProfile else strings.NotEnabled,
            fontSize = 12.sp,
            fontWeight = FontWeight(600),
            color = contentColor,
            maxLines = 1,
        )
    }
}

/** 卡片操作弹窗:节点页右上角同款的玻璃展开菜单。 */
@Composable
private fun ProfileActionsMenu(
    show: Boolean,
    isActive: Boolean,
    isDownloading: Boolean,
    canUpdate: Boolean,
    onDismiss: () -> Unit,
    onDismissFinished: () -> Unit,
    onToggleEnabled: () -> Unit,
    onEdit: () -> Unit,
    onOverrideSettings: (() -> Unit)?,
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
                    text = if (isActive) strings.Disable else strings.Enable,
                    icon = Lucide.ToggleRight,
                    iconSize = 18.dp,
                    iconGap = 15.dp,
                    enabled = actionsEnabled,
                    selected = isActive,
                    onClick = {
                        onDismiss()
                        onToggleEnabled()
                    },
                )
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
                val overrideAction = onOverrideSettings
                if (overrideAction != null) {
                    GlassPopupItem(
                        text = YumeTxt.ProfilesPage.SettingsDialog.SectionOverride,
                        icon = Lucide.FileBracesCorner,
                        iconSize = 18.dp,
                        iconGap = 15.dp,
                        enabled = actionsEnabled,
                        onClick = {
                            onDismiss()
                            overrideAction()
                        },
                    )
                }
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
                    icon = Yume.Share,
                    iconSize = 18.dp,
                    iconGap = 15.dp,
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
                    icon = Lucide.FileSymlink,
                    iconSize = 18.dp,
                    iconGap = 15.dp,
                    enabled = actionsEnabled,
                    onClick = {
                        shareExpanded = false
                        onDismiss()
                        onShareFile()
                    },
                )
                GlassPopupItem(
                    text = strings.ShareLink,
                    icon = Lucide.Link,
                    iconSize = 18.dp,
                    iconGap = 15.dp,
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

    Column(
        modifier = modifier.padding(start = spacing.space8),
        horizontalAlignment = Alignment.End,
    ) {
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
