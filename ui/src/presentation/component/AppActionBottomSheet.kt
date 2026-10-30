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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.theme.LocalGlassBackdrop
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.glass.GlassBottomSheet
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.layout.BottomSheetDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

object AppBottomSheetDefaults {

    val InsideMargin = DpSize(UiDp.dp24, UiDp.dp16)

    val OutsideMargin = DpSize(UiDp.dp0, UiDp.dp0)

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

/**
 * 通用底部弹窗:76dp 浮动标题带 + 标题栏 42dp 图标按钮 + 自滚动内容列。
 *
 * [content] 始终渲染在 `Column(verticalScroll(rememberScrollState()))` 内(带 [insideMargin] 水平内缩
 * 与 [AppBottomSheetDefaults.ContentTopGap] 顶部留白),因此内容里不能放高度不受限的懒加载列表:
 * `LazyColumn` 套在 `verticalScroll` 里会抛
 * "Vertically scrollable component was measured with an infinity maximum height constraints"。
 * 需要列表时给列表一个上界,例如 `Modifier.heightIn(max = 360.dp)`(WifiAutomationSheets 的用法)。
 *
 * [startAction] 默认就是关闭按钮(`AppBottomSheetCloseAction([onDismissRequest])`);需要别的起始动作
 * (ProxySheetContent 的“面板切换”)才传自己的 lambda,完全不需要时传 `null`。
 *
 * 这里刻意不再透传 `GlassBottomSheet` 的尺寸/退场参数(容器色、圆角、最大宽、外边距、拖拽条颜色、
 * 遮罩、`allowDismiss`、嵌套滚动、`renderInRootScaffold`):没有任何调用点需要它们,而透传只会让
 * 每个弹窗各自决定外观。确实需要这些选项时直接用 `GlassBottomSheet`。
 */
@Composable
fun AppActionBottomSheet(
    show: Boolean,
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    startAction: (@Composable () -> Unit)? = { AppBottomSheetCloseAction(onClick = onDismissRequest) },
    endAction: (@Composable () -> Unit)? = null,
    onDismissFinished: (() -> Unit)? = null,
    insideMargin: DpSize = AppBottomSheetDefaults.InsideMargin,
    defaultWindowInsetsPadding: Boolean = true,
    content: @Composable () -> Unit,
) {
    val sheetColor = AppBottomSheetDefaults.backgroundColor()
    // Dual-pane shell: each pane has its own root Scaffold. Rendering in that root lets sheets
    // cover the floating bottom bar while staying inside the left (or right) pane bounds.
    val inSplitShell = LocalDetailNavigator.current != null
    // Fill the pane Scaffold width rather than a centered phone card.
    val sheetMaxWidth = if (inSplitShell) Dp.Infinity else BottomSheetDefaults.maxWidth

    // Without the base colour the side actions read as bare icons at rest and only show their
    // glass once content scrolls underneath.
    val sheetContentBackdrop = rememberSurfaceBackdrop(containerColor = sheetColor)
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
        containerColor = sheetColor,
        title = null,
        cornerRadius = BottomSheetDefaults.cornerRadius,
        sheetMaxWidth = sheetMaxWidth,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        outsideMargin = AppBottomSheetDefaults.OutsideMargin,
        insideMargin = DpSize(0.dp, insideMargin.height),
        defaultWindowInsetsPadding = defaultWindowInsetsPadding,
        dragHandleColor = AppBottomSheetDefaults.dragHandleColor(),
        content = {
            CompositionLocalProvider(LocalGlassBackdrop provides sheetContentBackdrop) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.fillMaxWidth().layerBackdrop(sheetContentBackdrop)) {
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
                    }
                    FloatingSheetHeader(
                        backdrop = sheetContentBackdrop,
                        title = title,
                        sheetColor = sheetColor,
                        startAction = startAction,
                        endAction = endAction,
                    )
                }
            }
        },
    )
}
