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

package com.github.yumeyucca.yumebox.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.github.yumeyucca.yumebox.data.model.OverrideConfig
import com.github.yumeyucca.yumebox.presentation.component.AppCard
import com.github.yumeyucca.yumebox.presentation.component.OverrideCardActionIconButton
import com.github.yumeyucca.yumebox.presentation.component.OverrideStatusBadge
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.*
import com.github.yumeyucca.yumebox.presentation.theme.Spacing
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import sh.calvin.reorderable.ReorderableCollectionItemScope
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

private val overrideConfigItemGap = Spacing().space12

@Composable
internal fun ReorderableCollectionItemScope.OverrideConfigCard(
    config: OverrideConfig,
    isDragging: Boolean,
    isInUse: Boolean,
    isBuiltIn: Boolean,
    onApply: () -> Unit,
    onExport: () -> Unit,
    onEdit: () -> Unit,
    onDelete: (() -> Unit)?,
    enableDrag: Boolean,
) {
    OverrideConfigCardContent(
        config = config,
        isDragging = isDragging,
        isInUse = isInUse,
        isBuiltIn = isBuiltIn,
        onApply = onApply,
        onExport = onExport,
        onEdit = onEdit,
        onDelete = onDelete,
        modifier =
            if (enableDrag) {
                Modifier.longPressDraggableHandle()
            } else {
                Modifier
            },
    )
}

@Composable
internal fun OverrideConfigCard(
    config: OverrideConfig,
    isDragging: Boolean,
    isInUse: Boolean,
    isBuiltIn: Boolean,
    onApply: () -> Unit,
    onExport: () -> Unit,
    onEdit: () -> Unit,
    onDelete: (() -> Unit)?,
    enableDrag: Boolean,
) {
    OverrideConfigCardContent(
        config = config,
        isDragging = isDragging,
        isInUse = isInUse,
        isBuiltIn = isBuiltIn,
        onApply = onApply,
        onExport = onExport,
        onEdit = onEdit,
        onDelete = onDelete,
        modifier = Modifier,
    )
    @Suppress("UNUSED_EXPRESSION") enableDrag
}

@Composable
private fun OverrideConfigCardContent(
    config: OverrideConfig,
    isDragging: Boolean,
    isInUse: Boolean,
    isBuiltIn: Boolean,
    onApply: () -> Unit,
    onExport: () -> Unit,
    onEdit: () -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier,
) {
    val accentTintColor = colorScheme.primary

    AppCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = overrideConfigItemGap / 2)
                .then(modifier)
                .alpha(if (isDragging) 0.92f else 1f),
        insideMargin = PaddingValues(UiDp.dp16),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(UiDp.dp12),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(UiDp.dp8),
                ) {
                    Text(
                        text = config.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight(550),
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = config.contentType.label,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = colorScheme.onSurfaceVariantSummary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (isBuiltIn) {
                    OverrideBuiltInBadge()
                } else {
                    OverrideConfigStateIndicator(inUse = isInUse)
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = UiDp.dp12),
                thickness = UiDp.dp0_5,
                color = colorScheme.outline.copy(alpha = 0.5f),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(UiDp.dp8)) {
                    OverrideCardActionIconButton(
                        imageVector = Yume.Share,
                        contentDescription = YumeTxt.Override.Card.Export,
                        onClick = onExport,
                    )
                    if (onDelete != null) {
                        OverrideCardActionIconButton(
                            imageVector = Yume.Delete,
                            contentDescription = YumeTxt.Override.Card.Delete,
                            onClick = onDelete,
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    modifier = Modifier.padding(end = UiDp.dp8),
                    backgroundColor = colorScheme.secondaryContainer.copy(alpha = 0.78f),
                    minHeight = UiDp.dp35,
                    minWidth = UiDp.dp35,
                    onClick = onApply,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = UiDp.dp10),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(UiDp.dp2),
                    ) {
                        Icon(
                            modifier = Modifier.size(UiDp.dp20),
                            imageVector = Yume.Diff,
                            tint = colorScheme.onSurface.copy(alpha = 0.85f),
                            contentDescription = YumeTxt.Override.Card.Apply,
                        )
                        Text(
                            modifier = Modifier.padding(end = UiDp.dp3),
                            text = YumeTxt.Override.Card.ApplyButton,
                            color = colorScheme.onSurface.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                        )
                    }
                }

                IconButton(
                    backgroundColor = colorScheme.primary.copy(alpha = 0.1f),
                    minHeight = UiDp.dp35,
                    minWidth = UiDp.dp35,
                    onClick = onEdit,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = UiDp.dp10),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(UiDp.dp2),
                    ) {
                        Icon(
                            modifier = Modifier.size(UiDp.dp20),
                            imageVector = Yume.Edit,
                            tint = accentTintColor,
                            contentDescription = YumeTxt.Override.Card.Edit,
                        )
                        Text(
                            modifier = Modifier.padding(end = UiDp.dp3),
                            text = YumeTxt.Override.Card.EditButton,
                            color = accentTintColor,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverrideConfigStateIndicator(inUse: Boolean) {
    val tint = if (inUse) colorScheme.primary else colorScheme.onSurfaceVariantSummary
    OverrideStatusBadge(
        imageVector = if (inUse) Yume.ShieldCheck else Yume.ShieldMinus,
        contentDescription =
            if (inUse) YumeTxt.Override.Status.InUse else YumeTxt.Override.Status.NotInUse,
        tint = tint,
        backgroundColor =
            if (inUse) {
                colorScheme.primary.copy(alpha = 0.1f)
            } else {
                colorScheme.secondaryContainer.copy(alpha = 0.78f)
            },
    )
}

@Composable
private fun OverrideBuiltInBadge() {
    OverrideStatusBadge(
        imageVector = Yume.ShieldCheck,
        contentDescription = YumeTxt.Override.Status.BuiltIn,
        tint = colorScheme.onSurfaceVariantSummary,
        backgroundColor = colorScheme.secondaryContainer.copy(alpha = 0.78f),
    )
}
