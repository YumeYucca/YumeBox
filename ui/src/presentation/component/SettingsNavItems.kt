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
import androidx.compose.foundation.layout.Box
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
import io.github.yumeyucca.lucide.Lucide
import io.github.yumeyucca.lucide.lucide.ChevronRight
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Flat icon-list row for the settings hub cards: leading lucide icon, title, and a small
 * trailing chevron. Deliberately summary-free — group context comes from the section title
 * above each card instead of per-item descriptions.
 */
@Composable
fun SettingsNavItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = false,
    endContent: @Composable () -> Unit = { SettingsNavChevron() },
) {
    val spacing = AppTheme.spacing
    val componentSizes = AppTheme.sizes
    val opacity = AppTheme.opacity
    val colorScheme = MiuixTheme.colorScheme

    Column(modifier = modifier) {
        if (showDivider) {
            HorizontalDivider(
                modifier =
                    Modifier.padding(
                        start = spacing.space20 + componentSizes.settingsIconGlyphSize + spacing.space16,
                        end = spacing.space20,
                    ),
                thickness = componentSizes.thinDividerThickness,
                color = colorScheme.outline.copy(alpha = opacity.medium),
            )
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = UiDp.dp56)
                    .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.space16),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colorScheme.onSurface,
                modifier =
                    Modifier
                        .padding(start = spacing.space20)
                        .size(componentSizes.settingsIconGlyphSize),
            )
            Text(
                text = title,
                style = MiuixTheme.textStyles.body1,
                color = colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Box(modifier = Modifier.padding(end = spacing.space20)) { endContent() }
        }
    }
}

/** Small muted trailing chevron of [SettingsNavItem]; replaces the miuix big arrow. */
@Composable
fun SettingsNavChevron(modifier: Modifier = Modifier) {
    val colorScheme = MiuixTheme.colorScheme
    Icon(
        imageVector = Lucide.ChevronRight,
        contentDescription = null,
        tint = colorScheme.onSurfaceVariantActions,
        modifier = modifier.size(UiDp.dp18),
    )
}
