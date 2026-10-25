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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val LocalSettingsItemShape =
    compositionLocalOf<Shape> { RoundedCornerShape(24.dp) }

private val SettingsSegmentOuterRadius = 24.dp
private val SettingsSegmentInnerRadius = 4.dp
private val SettingsSegmentGap = 2.dp

/**
 * KSU-style Material 3 settings group. The separation between rows is intentional whitespace,
 * not a divider: outer rows use 24dp corners and inner rows use 4dp corners.
 */
@Composable
fun SettingsSegmentedColumn(
    modifier: Modifier = Modifier,
    content: List<@Composable () -> Unit>,
) {
    if (content.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SettingsSegmentGap),
    ) {
        content.forEachIndexed { index, itemContent ->
            val shape =
                RoundedCornerShape(
                    topStart = if (index == 0) SettingsSegmentOuterRadius else SettingsSegmentInnerRadius,
                    topEnd = if (index == 0) SettingsSegmentOuterRadius else SettingsSegmentInnerRadius,
                    bottomStart = if (index == content.lastIndex) SettingsSegmentOuterRadius else SettingsSegmentInnerRadius,
                    bottomEnd = if (index == content.lastIndex) SettingsSegmentOuterRadius else SettingsSegmentInnerRadius,
                )
            CompositionLocalProvider(LocalSettingsItemShape provides shape) {
                itemContent()
            }
        }
    }
}

/**
 * One-line Material 3 settings item with a leading icon and trailing chevron.
 *
 * The item intentionally has no independent card. Its background and shape are supplied by the
 * surrounding segmented group, matching KernelSU's `SegmentedListItem` layout.
 */
@Composable
fun SettingsListItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val colorScheme = MiuixTheme.colorScheme
    val itemColor = colorScheme.surfaceContainer
    val titleColor = smallTitleColor()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = LocalSettingsItemShape.current,
        color = itemColor,
    ) {
        ListItem(
            modifier = Modifier.clickable(onClick = onClick),
            headlineContent = {
                Text(
                    text = title,
                    color = colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingContent = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = titleColor,
                    modifier = Modifier.size(UiDp.dp24),
                )
            },
            trailingContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(UiDp.dp8),
                ) {
                    trailingContent?.invoke()
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = titleColor,
                        modifier = Modifier.size(UiDp.dp24),
                    )
                }
            },
            colors =
                ListItemDefaults.colors(
                    containerColor = Color.Transparent,
                    headlineColor = colorScheme.onSurface,
                    leadingIconColor = titleColor,
                    trailingIconColor = colorScheme.onSurfaceVariantActions,
                ),
        )
    }
}
