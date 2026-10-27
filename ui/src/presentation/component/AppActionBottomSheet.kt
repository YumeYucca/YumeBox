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


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.glass.GlassBottomSheet
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.layout.BottomSheetDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

object AppBottomSheetDefaults {

    val insideMargin = DpSize(UiDp.dp24, UiDp.dp16)

    val outsideMargin = DpSize(UiDp.dp0, UiDp.dp0)

    /**
     * Top inset for content that scrolls under the floating 76dp title band, so the first row
     * starts at the band's lower edge instead of beneath it.
     */
    val ContentTopGap = FloatingSheetHeaderDefaults.ContentTopInset

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
    // The node sheet's top-left chrome (42dp glass circle, 22dp icon) in its solid form: the
    // header band is painted, not sampled, so no blur backdrop is involved.
    SheetHeaderIconButton(
        icon = action.icon,
        contentDescription = action.contentDescription,
        onClick = action.onClick,
        enabled = action.enabled,
        tint =
            if (action.tint == Color.Unspecified) {
                AppBottomSheetDefaults.actionIconTint(action.enabled)
            } else {
                action.tint
            },
    )
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
                icon = MiuixIcons.Close,
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
                icon = MiuixIcons.Ok,
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
    scrollableContent: Boolean = false,
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

    val sheetContentBackdrop = rememberLayerBackdrop { drawContent() }
    // miuix-glass bottom sheet. The panel does not sample the page (backdrop = null): it paints
    // the opaque container colour, which is what the reference sheet does ("弹窗本体不做壁纸
    // 玻璃模糊，纯实色").
    //
    // The library's title row is left empty: its 24dp drag handle + 18dp empty title row are the
    // 42dp chrome above content. The 76dp band is drawn by FloatingSheetHeader from the card's top
    // edge (-42dp), with the title and 42dp side actions centred 38dp from that edge.
    //
    // The library's insideMargin is horizontal *and* bottom and pads the whole column, which would
    // pull the band and its circles in from the card edge. The band spans the card and only the
    // content is inset, so the horizontal inset moves down into the content here.
    GlassBottomSheet(
        show = show,
        modifier = modifier,
        backdrop = null,
        containerColor = resolvedBackgroundColor,
        title = null,
        enableWindowDim = enableWindowDim,
        cornerRadius = cornerRadius,
        sheetMaxWidth = resolvedSheetMaxWidth,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        outsideMargin = outsideMargin,
        insideMargin = DpSize(0.dp, insideMargin.height),
        defaultWindowInsetsPadding = defaultWindowInsetsPadding,
        dragHandleColor = resolvedDragHandleColor,
        allowDismiss = allowDismiss,
        enableNestedScroll = enableNestedScroll,
        renderInRootScaffold = resolvedRenderInRoot,
        content = {
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth().layerBackdrop(sheetContentBackdrop)) {
                    if (scrollableContent) {
                        // The content column scrolls itself and wraps up to the sheet's max height. The
                        // sheet column is not scrollable, so a tall non-lazy form (add profile, settings)
                        // would otherwise be clipped with no way to reach the cut-off fields — especially
                        // with the IME consuming half the window.
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = insideMargin.width),
                        ) {
                            Spacer(modifier = Modifier.height(AppBottomSheetDefaults.ContentTopGap))
                            content()
                        }
                    } else {
                        // Self-scrolling content (LazyColumn): the caller pads its own top with
                        // AppBottomSheetDefaults.ContentTopGap so the first row starts under the band.
                        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = insideMargin.width)) {
                            content()
                        }
                    }
                }
                FloatingSheetHeader(
                    backdrop = sheetContentBackdrop,
                    title = title,
                    sheetColor = resolvedBackgroundColor,
                    startAction = startAction,
                    endAction = endAction,
                )
            }
        },
    )
}
