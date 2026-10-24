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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Flat list row for the settings hub cards: plain leading lucide icon, title with an optional
 * muted summary beneath it, and a full-width hairline divider between rows. Trailing content is
 * opt-in (e.g. the version badge) — rows otherwise end with open space, no chevron.
 */
@Composable
fun SettingsNavItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    showDivider: Boolean = false,
    endContent: (@Composable () -> Unit)? = null,
) {
    val spacing = AppTheme.spacing
    val componentSizes = AppTheme.sizes
    val opacity = AppTheme.opacity
    val colorScheme = MiuixTheme.colorScheme

    Column(modifier = modifier) {
        if (showDivider) {
            HorizontalDivider(
                thickness = componentSizes.thinDividerThickness,
                color = colorScheme.outline.copy(alpha = opacity.medium),
            )
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = UiDp.dp60)
                    .clickable(onClick = onClick)
                    .padding(horizontal = spacing.space20, vertical = spacing.space14),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.space16),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colorScheme.onSurface,
                modifier = Modifier.size(UiDp.dp24),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.space2),
            ) {
                Text(
                    text = title,
                    style = MiuixTheme.textStyles.body1,
                    color = colorScheme.onSurface,
                )
                if (!summary.isNullOrEmpty()) {
                    Text(
                        text = summary,
                        style = MiuixTheme.textStyles.footnote1,
                        color = colorScheme.onSurfaceVariantActions,
                    )
                }
            }
            if (endContent != null) endContent()
        }
    }
}
