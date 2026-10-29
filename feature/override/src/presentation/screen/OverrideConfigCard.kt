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

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.yumeyucca.yumebox.data.model.OverrideConfig
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.*
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.theme.LocalGlassBackdrop
import com.github.yumeyucca.yumebox.presentation.theme.LocalTopBarBackdrop
import com.github.yumeyucca.yumebox.presentation.theme.Spacing
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import io.github.yumeyucca.lucide.Lucide
import io.github.yumeyucca.lucide.lucide.TextCursorInput
import sh.calvin.reorderable.ReorderableCollectionItemScope
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.glass.*
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils.Companion.PopupLayout

private val overrideConfigItemGap = Spacing().space12

@Composable
internal fun ReorderableCollectionItemScope.OverrideConfigCard(
    config: OverrideConfig,
    isDragging: Boolean,
    isInUse: Boolean,
    isBuiltIn: Boolean,
    onExport: () -> Unit,
    onEdit: () -> Unit,
    onRename: () -> Unit,
    onDelete: (() -> Unit)?,
    enableDrag: Boolean,
) {
    OverrideConfigCardContent(
        config = config,
        isDragging = isDragging,
        isInUse = isInUse,
        isBuiltIn = isBuiltIn,
        onExport = onExport,
        onEdit = onEdit,
        onRename = onRename,
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
    onExport: () -> Unit,
    onEdit: () -> Unit,
    onRename: () -> Unit,
    onDelete: (() -> Unit)?,
    enableDrag: Boolean,
) {
    OverrideConfigCardContent(
        config = config,
        isDragging = isDragging,
        isInUse = isInUse,
        isBuiltIn = isBuiltIn,
        onExport = onExport,
        onEdit = onEdit,
        onRename = onRename,
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
    onExport: () -> Unit,
    onEdit: () -> Unit,
    onRename: () -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier,
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
                    text = config.contentType.label,
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
                        cornerRadius = 14.dp,
                        floating = true,
                    ),
            ) {
                if (isBuiltIn) {
                    OverrideBuiltInBadge()
                } else {
                    OverrideConfigStateIndicator(inUse = isInUse)
                }
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
    val backdrop = LocalGlassBackdrop.current ?: LocalTopBarBackdrop.current
    val popupVisible = remember { mutableStateOf(false) }
    LaunchedEffect(show) {
        if (show) popupVisible.value = true
    }
    PopupLayout(
        visible = popupVisible,
        enterTransition = EnterTransition.None,
        exitTransition = ExitTransition.None,
        enableWindowDim = false,
        enableBackHandler = false,
    ) {
        Box(Modifier.fillMaxSize()) {
            var expanded by remember { mutableStateOf(false) }
            LaunchedEffect(show) { expanded = show }
            GlassTransformPopup(
                show = expanded,
                onDismissRequest = onDismiss,
                anchor = anchor,
                backdrop = backdrop,
                anchorContent = {
                    OverrideStatusBadge(
                        imageVector =
                            if (isBuiltIn || isInUse) Yume.ShieldCheck else Yume.ShieldMinus,
                        contentDescription =
                            when {
                                isBuiltIn -> YumeTxt.Override.Status.BuiltIn
                                isInUse -> YumeTxt.Override.Status.InUse
                                else -> YumeTxt.Override.Status.NotInUse
                            },
                        tint =
                            if (!isBuiltIn && isInUse) {
                                colorScheme.primary
                            } else {
                                colorScheme.onSurfaceVariantSummary
                            },
                        backgroundColor =
                            if (!isBuiltIn && isInUse) {
                                colorScheme.primary.copy(alpha = AppTheme.opacity.subtle)
                            } else {
                                colorScheme.secondaryContainer.copy(alpha = 0.78f)
                            },
                    )
                },
                anchorAlpha = 0f,
                sizing = GlassPopupSizing(minWidth = 188.dp, maxWidth = 188.dp),
                visuals = GlassPopupDefaults.visuals(),
                alignToPageEnd = true,
                onDismissFinished = {
                    if (!show) {
                        popupVisible.value = false
                        onDismissFinished()
                    }
                },
            ) {
                GlassPopupItem(
                    text = YumeTxt.Override.Card.Edit,
                    icon = Yume.Edit,
                    iconSize = 18.dp,
                    iconGap = 15.dp,
                    onClick = {
                        onDismiss()
                        onEdit()
                    },
                )
                if (!isBuiltIn) {
                    GlassPopupItem(
                        text = YumeTxt.Override.Card.Rename,
                        icon = Lucide.TextCursorInput,
                        iconSize = 18.dp,
                        iconGap = 15.dp,
                        onClick = {
                            onDismiss()
                            onRename()
                        },
                    )
                }
                GlassPopupItem(
                    text = YumeTxt.Override.Card.Export,
                    icon = Yume.Share,
                    iconSize = 18.dp,
                    iconGap = 15.dp,
                    onClick = {
                        onDismiss()
                        onExport()
                    },
                )
                if (onDelete != null) {
                    GlassPopupItem(
                        text = YumeTxt.Override.Card.Delete,
                        icon = Yume.Delete,
                        iconSize = 18.dp,
                        iconGap = 15.dp,
                        contentColor = AppTheme.colors.status.destructive,
                        onClick = {
                            onDismiss()
                            onDelete()
                        },
                    )
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
