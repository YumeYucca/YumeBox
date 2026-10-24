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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
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
import kotlin.math.PI
import kotlin.math.sin
import com.github.yumeyucca.yumebox.presentation.icon.yume.CircleFadingArrowUp
import com.github.yumeyucca.yumebox.presentation.icon.yume.Delete
import com.github.yumeyucca.yumebox.presentation.icon.yume.Edit
import com.github.yumeyucca.yumebox.presentation.icon.yume.Share
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.presentation.theme.horizontalPadding
import com.github.yumeyucca.yumebox.presentation.util.*
import com.github.yumeyucca.yumebox.runtime.api.Profile
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.File

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
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
) {
    val spacing = AppTheme.spacing
    val opacity = AppTheme.opacity
    val componentSizes = AppTheme.sizes

    val colorScheme = MiuixTheme.colorScheme
    val warnColor = AppTheme.colors.moe.pingWarning

    val isDark = isSystemInDarkTheme()
    val secondaryContainer = colorScheme.secondaryContainer.copy(alpha = opacity.strong)
    val actionIconTint =
        remember(isDark, opacity) {
            colorScheme.onSurface.copy(
                alpha = if (isDark) opacity.subtleText else opacity.prominentText
            )
        }

    val isConfigSaved = remember(profile.uuid, profile.updatedAt) { profile.isConfigSaved(workDir) }

    val updateBg =
        remember(colorScheme, opacity) { colorScheme.primary.copy(alpha = opacity.subtle) }
    val updateTint = remember(colorScheme) { colorScheme.primary }

    val isActive = profile.active
    val updatedText = profile.getRelativeUpdatedTime()
    val updatedIsFresh = updatedText != null && updatedText == YumeTxt.Component.ProfileCard.JustNow
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

    // Selection ring hugs the card edge exactly: page side margins sit outside the ring Box, and
    // the ring is a later-sibling overlay at the card's own bounds (squircleBorder paints behind
    // its node, so an overlay paints it on top of the card) — toggling selection never shifts layout.
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(bottom = spacing.space12)
                .horizontalPadding(),
    ) {
        AppCard(
            modifier = Modifier.fillMaxWidth(),
            applyHorizontalPadding = false,
            insideMargin = PaddingValues(spacing.space12),
            onClick = { if (!isDownloading) onToggleEnabled(profile) },
        ) {
        // Header: name and "type · updated" pill.
        Row(
            modifier =
                Modifier.fillMaxWidth().padding(horizontal = spacing.space2),
            horizontalArrangement = Arrangement.spacedBy(spacing.space6),
            verticalAlignment = Alignment.CenterVertically,
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
            ProfileUpdatedTime(
                updatedText = updatedText,
                updatedIsFresh = updatedIsFresh,
            )
        }

        // Main: remaining traffic headline + expiry badge.
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

        if (traffic != null) {
            TrafficWaveProgress(
                ratio = traffic.ratio,
                warn = traffic.lowTraffic,
                modifier =
                    Modifier.fillMaxWidth().padding(
                        top = spacing.space6,
                        start = spacing.space2,
                        end = spacing.space2,
                    ),
            )
        }

        // Footer: share / delete on the left, update / edit on the right.
        Row(
            modifier = Modifier.padding(top = spacing.space10),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                backgroundColor = secondaryContainer,
                minHeight = componentSizes.compactActionButtonSize,
                minWidth = componentSizes.compactActionButtonSize,
                enabled = isConfigSaved && !isDownloading,
                onClick = { if (isConfigSaved && !isDownloading) onExport(profile) },
            ) {
                Icon(
                    modifier =
                        Modifier
                            .size(spacing.space20)
                            .alpha(if (isConfigSaved) 1f else opacity.disabledSecondary),
                    imageVector = Yume.Share,
                    tint =
                        actionIconTint.copy(
                            alpha = if (isConfigSaved) 1f else opacity.disabledSecondary
                        ),
                    contentDescription = YumeTxt.Component.ProfileCard.Export,
                )
            }

            Spacer(Modifier.width(spacing.space8))

            IconButton(
                backgroundColor = secondaryContainer,
                minHeight = componentSizes.compactActionButtonSize,
                minWidth = componentSizes.compactActionButtonSize,
                enabled = !isDownloading,
                onClick = { if (!isDownloading) onDelete(profile) },
            ) {
                Icon(
                    modifier = Modifier.size(spacing.space20),
                    imageVector = Yume.Delete,
                    tint = actionIconTint,
                    contentDescription = YumeTxt.Component.ProfileCard.Delete,
                )
            }

            Spacer(Modifier.weight(1f))

            if (profile.shouldShowUpdateButton()) {
                IconButton(
                    modifier = Modifier.padding(end = spacing.space8),
                    backgroundColor = updateBg,
                    minHeight = componentSizes.compactActionButtonSize,
                    minWidth = componentSizes.compactActionButtonSize,
                    enabled = !isDownloading,
                    onClick = { if (!isDownloading) onUpdate(profile) },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = spacing.space10),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                    ) {
                        Icon(
                            modifier = Modifier.size(spacing.space20),
                            imageVector = Yume.CircleFadingArrowUp,
                            tint = updateTint,
                            contentDescription = YumeTxt.Component.ProfileCard.Update,
                        )
                        Text(
                            modifier =
                                Modifier.padding(end = componentSizes.textLineCompactSpacing),
                            text = YumeTxt.Component.ProfileCard.Update,
                            color = updateTint,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                        )
                    }
                }
            }

            IconButton(
                minHeight = componentSizes.compactActionButtonSize,
                minWidth = componentSizes.compactActionButtonSize,
                enabled = !isDownloading,
                onClick = { if (!isDownloading) onEdit(profile) },
                backgroundColor = secondaryContainer,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = spacing.space10),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        modifier = Modifier.size(spacing.space20),
                        imageVector = Yume.Edit,
                        tint = actionIconTint,
                        contentDescription = YumeTxt.Component.ProfileCard.Edit,
                    )
                    Text(
                        modifier =
                            Modifier.padding(
                                start = spacing.space4,
                                end = componentSizes.textLineCompactSpacing,
                            ),
                        text = YumeTxt.Component.ProfileCard.Edit,
                        color = actionIconTint,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                    )
                }
            }
        }
    }
        if (isActive) {
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .squircleBorder(
                            width = 1.5.dp,
                            color = colorScheme.primary,
                            cornerRadius = 24.dp,
                        ),
            )
        }
    }
}

@Composable
private fun ProfileUpdatedTime(
    updatedText: String?,
    updatedIsFresh: Boolean,
) {
    if (updatedText == null) return
    val colorScheme = MiuixTheme.colorScheme

    // 素文字形态:不套胶囊壳,紧跟标题行右侧的次要色相对时间;仅"刚刚更新"用主题色点一下。
    // 右侧留 8dp,与下方到期徽章内部的日期文字右缘对齐(徽章自身有 8dp 内边距)。
    Text(
        text = updatedText,
        fontSize = 12.sp,
        fontWeight = FontWeight(500),
        color =
            if (updatedIsFresh) colorScheme.primary
            else colorScheme.onSurfaceVariantSummary.copy(alpha = AppTheme.opacity.subtleText),
        maxLines = 1,
        modifier = Modifier.padding(start = AppTheme.spacing.space8, end = AppTheme.spacing.space8),
    )
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
                        colorScheme.surfaceContainer
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
            Text(
                text = expiry.dateText,
                fontSize = 10.sp,
                color =
                    if (expiry.warning) {
                        warnColor.copy(alpha = opacity.mediumStrong)
                    } else {
                        colorScheme.onSurfaceVariantSummary
                    },
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
