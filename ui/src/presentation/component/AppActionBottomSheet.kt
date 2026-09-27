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


import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpSize
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.Check
import com.github.yumeyucca.yumebox.presentation.icon.yume.Close
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.layout.BottomSheetDefaults
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme

object AppBottomSheetDefaults {

    val insideMargin = DpSize(UiDp.dp24, UiDp.dp16)

    val outsideMargin = DpSize(UiDp.dp0, UiDp.dp0)

    @Composable
    fun backgroundColor(): Color = MiuixTheme.colorScheme.surface

    @Composable
    fun dragHandleColor(): Color = MiuixTheme.colorScheme.onSurfaceVariantActions

    @Composable
    fun actionIconTint(enabled: Boolean): Color =
        if (enabled) {
            MiuixTheme.colorScheme.onSurface
        } else {
            MiuixTheme.colorScheme.onSurface.copy(alpha = AppTheme.opacity.disabled)
        }
}

data class AppBottomSheetAction(
    val icon: ImageVector,
    val contentDescription: String,
    val enabled: Boolean = true,
    val tint: Color = Color.Unspecified,
    val onClick: () -> Unit,
)

@Composable
fun AppBottomSheetIconAction(action: AppBottomSheetAction) {
    IconButton(enabled = action.enabled, onClick = action.onClick) {
        Icon(
            modifier = Modifier.alpha(if (action.enabled) 1f else AppTheme.opacity.medium),
            imageVector = action.icon,
            contentDescription = action.contentDescription,
            tint =
                if (action.tint == Color.Unspecified) {
                    AppBottomSheetDefaults.actionIconTint(action.enabled)
                } else {
                    action.tint
                },
        )
    }
}

@Composable
fun AppBottomSheetCloseAction(
    onClick: () -> Unit,
    enabled: Boolean = true,
    contentDescription: String = YumeTxt.Component.Button.Cancel,
) {
    AppBottomSheetIconAction(
        action =
            AppBottomSheetAction(
                icon = Yume.Close,
                contentDescription = contentDescription,
                enabled = enabled,
                onClick = onClick,
            )
    )
}

@Composable
fun AppBottomSheetConfirmAction(
    onClick: () -> Unit,
    enabled: Boolean = true,
    contentDescription: String = YumeTxt.Component.Button.Confirm,
) {
    AppBottomSheetIconAction(
        action =
            AppBottomSheetAction(
                icon = Yume.Check,
                contentDescription = contentDescription,
                enabled = enabled,
                onClick = onClick,
            )
    )
}

@Composable
fun AppActionBottomSheet(
    show: Boolean,
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    startAction: (@Composable (() -> Unit))? = null,
    endAction: (@Composable (() -> Unit))? = null,
    backgroundColor: Color = Color.Unspecified,
    enableWindowDim: Boolean = true,
    cornerRadius: androidx.compose.ui.unit.Dp = BottomSheetDefaults.cornerRadius,
    sheetMaxWidth: androidx.compose.ui.unit.Dp = BottomSheetDefaults.maxWidth,
    onDismissFinished: (() -> Unit)? = null,
    outsideMargin: DpSize = AppBottomSheetDefaults.outsideMargin,
    insideMargin: DpSize = AppBottomSheetDefaults.insideMargin,
    defaultWindowInsetsPadding: Boolean = true,
    dragHandleColor: Color = Color.Unspecified,
    allowDismiss: Boolean = true,
    enableNestedScroll: Boolean = true,
    renderInRootScaffold: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val resolvedBackgroundColor =
        if (backgroundColor == Color.Unspecified) {
            AppBottomSheetDefaults.backgroundColor()
        } else {
            backgroundColor
        }
    val resolvedDragHandleColor =
        if (dragHandleColor == Color.Unspecified) {
            AppBottomSheetDefaults.dragHandleColor()
        } else {
            dragHandleColor
        }
    // Dual-pane shell: each pane has its own root Scaffold. Rendering in that root lets sheets
    // cover the floating bottom bar while staying inside the left (or right) pane bounds.
    val inSplitShell = LocalDetailNavigator.current.isSplitShell
    val resolvedRenderInRoot = renderInRootScaffold ?: true
    val resolvedSheetMaxWidth =
        if (inSplitShell && sheetMaxWidth == BottomSheetDefaults.maxWidth) {
            // Fill the pane Scaffold width rather than a centered phone card.
            androidx.compose.ui.unit.Dp.Infinity
        } else {
            sheetMaxWidth
        }

    OverlayBottomSheet(
        show = show,
        modifier = modifier,
        title = title,
        startAction = startAction,
        endAction = endAction,
        backgroundColor = resolvedBackgroundColor,
        enableWindowDim = enableWindowDim,
        cornerRadius = cornerRadius,
        sheetMaxWidth = resolvedSheetMaxWidth,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        outsideMargin = outsideMargin,
        insideMargin = insideMargin,
        defaultWindowInsetsPadding = defaultWindowInsetsPadding,
        dragHandleColor = resolvedDragHandleColor,
        allowDismiss = allowDismiss,
        enableNestedScroll = enableNestedScroll,
        renderInRootScaffold = resolvedRenderInRoot,
        content = content,
    )
}
