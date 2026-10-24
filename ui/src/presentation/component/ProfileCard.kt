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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
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
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.presentation.util.*
import com.github.yumeyucca.yumebox.runtime.api.Profile
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
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
    val providerText = profile.getDisplayProvider()
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

    AppCard(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(bottom = spacing.space12)
                .then(
                    if (isActive) {
                        Modifier.border(
                            width = 1.5.dp,
                            color = colorScheme.primary,
                            shape = RoundedCornerShape(UiDp.dp22),
                        )
                    } else {
                        Modifier
                    }
                ),
        insideMargin = PaddingValues(spacing.space12),
        onClick = { if (!isDownloading) onToggleEnabled(profile) },
        pressFeedbackType = PressFeedbackType.Sink,
    ) {
        // Header: selection dot, name, "type · updated" pill.
        Row(
            modifier =
                Modifier.fillMaxWidth().padding(horizontal = spacing.space2),
            horizontalArrangement = Arrangement.spacedBy(spacing.space6),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(UiDp.dp8)
                        .clip(CircleShape)
                        .then(
                            if (isActive) {
                                Modifier.background(colorScheme.primary)
                            } else {
                                Modifier.border(
                                    width = 1.5.dp,
                                    color = colorScheme.outline.copy(alpha = opacity.brightOutline),
                                    shape = CircleShape,
                                )
                            }
                        )
            )
            Text(
                text = profile.name,
                fontSize = 18.sp,
                fontWeight = FontWeight(650),
                color = colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            ProfileTypePill(
                providerText = providerText,
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
}

@Composable
private fun ProfileTypePill(
    providerText: String,
    updatedText: String?,
    updatedIsFresh: Boolean,
) {
    val colorScheme = MiuixTheme.colorScheme

    Row(
        modifier =
            Modifier
                .clip(RoundedCornerShape(UiDp.dp999))
                .background(colorScheme.onSurface.copy(alpha = AppTheme.opacity.verySubtle))
                .padding(horizontal = AppTheme.spacing.space8, vertical = AppTheme.spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.space4),
    ) {
        Text(
            text = providerText,
            fontSize = 11.sp,
            fontWeight = FontWeight(600),
            color = colorScheme.onSurfaceVariantSummary,
            maxLines = 1,
        )
        if (updatedText != null) {
            Text(
                text = "· $updatedText",
                fontSize = 11.sp,
                fontWeight = FontWeight(500),
                color =
                    if (updatedIsFresh) colorScheme.primary
                    else colorScheme.onSurfaceVariantSummary.copy(alpha = AppTheme.opacity.subtleText),
                maxLines = 1,
            )
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

    val headline =
        when {
            expiry.daysLeft > 0 ->
                YumeTxt.Component.ProfileCard.ExpireInDays.format(expiry.daysLeft.toInt())
            expiry.daysLeft == 0L -> YumeTxt.Component.ProfileCard.ExpireTodayShort
            else -> YumeTxt.Component.ProfileCard.ExpiredShort
        }

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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TrafficWaveProgress(
    ratio: Float,
    warn: Boolean,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MiuixTheme.colorScheme
    val opacity = AppTheme.opacity
    val density = LocalDensity.current

    val waveColor = if (warn) AppTheme.colors.moe.pingWarning else colorScheme.primary
    val trackColor = colorScheme.onSurface.copy(alpha = opacity.subtleStrong)
    val stroke =
        remember(density) {
            Stroke(with(density) { UiDp.dp4.toPx() }, cap = StrokeCap.Round)
        }
    val animatedRatio by animateFloatAsState(
        targetValue = ratio.coerceIn(0f, 1f),
        animationSpec = WavyProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "profileTrafficRatio",
    )

    LinearWavyProgressIndicator(
        progress = { animatedRatio },
        modifier = modifier.height(UiDp.dp12),
        color = waveColor,
        trackColor = trackColor,
        stroke = stroke,
        trackStroke = stroke,
        gapSize = UiDp.dp8,
        stopSize = UiDp.dp0,
        amplitude = { 0.75f },
        wavelength = UiDp.dp20,
    )
}
