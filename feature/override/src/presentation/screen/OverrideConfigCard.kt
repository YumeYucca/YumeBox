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

package com.github.yumeyucca.yumebox.feature.override.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.github.yumeyucca.yumebox.data.model.OverrideConfig
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.*
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.theme.Spacing
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import io.github.yumeyucca.lucide.Lucide
import io.github.yumeyucca.lucide.lucide.TextCursorInput
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.glass.*
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

private val overrideConfigItemGap = Spacing().space12

@Composable
internal fun OverrideConfigCard(
    config: OverrideConfig,
    isDragging: Boolean,
    isInUse: Boolean,
    isBuiltIn: Boolean,
    onExport: () -> Unit,
    onEdit: () -> Unit,
    onRename: () -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var actionsOpen by remember(config.id) { mutableStateOf(false) }
    var actionsMounted by remember(config.id) { mutableStateOf(false) }
    val actionsMenuAnchor = rememberGlassPopupAnchor()

    AppCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = overrideConfigItemGap / 2)
                .then(modifier)
                .alpha(if (isDragging) 0.92f else 1f),
        insideMargin = PaddingValues(UiDp.dp16),
        onClick = {
            actionsMounted = true
            actionsOpen = true
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(UiDp.dp12),
            verticalAlignment = Alignment.CenterVertically,
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
                    text = config.contentType.displayName,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurfaceVariantSummary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier =
                    Modifier.glassPopupAnchor(
                        anchor = actionsMenuAnchor,
                        cornerRadius = AppTheme.sizes.statusCapsuleHeight / 2,
                        floating = true,
                    ),
            ) {
                OverrideConfigStatusBadge(isBuiltIn = isBuiltIn, isInUse = isInUse)
            }
        }
    }

    if (actionsMounted) {
        OverrideConfigActionsMenu(
            show = actionsOpen,
            isInUse = isInUse,
            isBuiltIn = isBuiltIn,
            onDismiss = { actionsOpen = false },
            onDismissFinished = { actionsMounted = false },
            anchor = actionsMenuAnchor,
            onEdit = onEdit,
            onRename = onRename,
            onExport = onExport,
            onDelete = onDelete,
        )
    }
}

@Composable
private fun OverrideConfigActionsMenu(
    show: Boolean,
    isInUse: Boolean,
    isBuiltIn: Boolean,
    onDismiss: () -> Unit,
    onDismissFinished: () -> Unit,
    anchor: GlassPopupAnchor,
    onEdit: () -> Unit,
    onRename: () -> Unit,
    onExport: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    GlassPopupHost(
        show = show,
        onDismissRequest = onDismiss,
        anchor = anchor,
        anchorContent = { OverrideConfigStatusBadge(isBuiltIn = isBuiltIn, isInUse = isInUse) },
        onDismissFinished = onDismissFinished,
    ) {
        GlassMenuItem(
            text = YumeTxt.Override.Card.Edit,
            icon = Yume.Edit,
            onClick = {
                onDismiss()
                onEdit()
            },
        )
        if (!isBuiltIn) {
            GlassMenuItem(
                text = YumeTxt.Override.Card.Rename,
                icon = Lucide.TextCursorInput,
                onClick = {
                    onDismiss()
                    onRename()
                },
            )
        }
        GlassMenuItem(
            text = YumeTxt.Override.Card.Export,
            icon = Yume.Share,
            onClick = {
                onDismiss()
                onExport()
            },
        )
        if (onDelete != null) {
            GlassMenuItem(
                text = YumeTxt.Override.Card.Delete,
                icon = Yume.Delete,
                contentColor = AppTheme.colors.status.destructive,
                onClick = {
                    onDismiss()
                    onDelete()
                },
            )
        }
    }
}

/** The single badge the card and its menu anchor share: built-in wins over in-use. */
@Composable
private fun OverrideConfigStatusBadge(
    isBuiltIn: Boolean,
    isInUse: Boolean,
) {
    val active = isInUse && !isBuiltIn
    val spacing = AppTheme.spacing
    val sizes = AppTheme.sizes

    Box(
        modifier =
            Modifier
                .size(sizes.statusCapsuleHeight + spacing.space4)
                .background(
                    color =
                        if (active) {
                            colorScheme.primary.copy(alpha = AppTheme.opacity.subtle)
                        } else {
                            colorScheme.secondaryContainer.copy(alpha = AppTheme.opacity.surfaceVariant)
                        },
                    shape = CircleShape,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isBuiltIn || isInUse) Yume.ShieldCheck else Yume.ShieldMinus,
            contentDescription =
                when {
                    isBuiltIn -> YumeTxt.Override.Status.BuiltIn
                    isInUse -> YumeTxt.Override.Status.InUse
                    else -> YumeTxt.Override.Status.NotInUse
                },
            tint = if (active) colorScheme.primary else colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.size(UiDp.dp18),
        )
    }
}
