/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.util.ProfileExpiryPreview
import io.github.yumeyucca.lucide.Lucide
import io.github.yumeyucca.lucide.lucide.CalendarCheck2
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun ProfileExpiryBadge(
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

// About ten years or more counts as permanent.
private const val EXPIRY_PERMANENT_DAYS = 3650L

// Scale the countdown unit: days → months → years → permanent, so a value like
// "expires in 3649 days" never shows up.
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
