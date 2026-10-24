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

package com.github.yumeyucca.yumebox.screen.profiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import com.github.yumeyucca.yumebox.presentation.component.GlassTopAction
import com.github.yumeyucca.yumebox.presentation.component.GlassTopIconButton
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.Close
import com.github.yumeyucca.yumebox.presentation.icon.yume.List
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.runtime.api.Profile
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet

/**
 * 拖拽排序配置的底部 sheet(参考系统"自定义排序"弹窗):每行一张卡片,名称在左,
 * 拖拽手柄(≡)在右侧,无勾选态。拖动即通过 [onReorder] 实时落库,关闭即完成,
 * 无需确认按钮。
 */
@Composable
internal fun ProfilesSortSheet(
    show: Boolean,
    profiles: List<Profile>,
    onReorder: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetColor = MiuixTheme.colorScheme.surface
    // 弹窗内容自身的 backdrop(捕获"底色+内容"),供标题栏玻璃圆钮采样。
    // 玻璃按钮在 BottomSheetContentLayout 的标题行里,位于被捕获的内容子树之外,
    // 满足 miuix-blur 的 same-subtree 约束(见 LocalGlassBackdrop 文档)。
    val sheetBackdrop =
        key(sheetColor) {
            rememberLayerBackdrop {
                drawRect(sheetColor)
                drawContent()
            }
        }

    WindowBottomSheet(
        show = show,
        title = YumeTxt.ProfilesPage.Action.Sort,
        backgroundColor = sheetColor,
        startAction = {
            GlassTopIconButton(
                action =
                    GlassTopAction(
                        icon = Yume.Close,
                        contentDescription = YumeTxt.ProfilesPage.LinkSettings.Close,
                        onClick = onDismiss,
                    ),
                backdrop = sheetBackdrop,
            )
        },
        onDismissRequest = onDismiss,
        onDismissFinished = onDismiss,
        enableWindowDim = true,
        insideMargin = DpSize(UiDp.dp16, UiDp.dp16),
        enableNestedScroll = false,
    ) {
        Box(modifier = Modifier.fillMaxWidth().layerBackdrop(sheetBackdrop)) {
            ProfilesSortList(
                profiles = profiles,
                onReorder = onReorder,
            )
        }
    }
}

@Composable
private fun ProfilesSortList(
    profiles: List<Profile>,
    onReorder: (Int, Int) -> Unit,
) {
    val listState = rememberLazyListState()
    val reorderState =
        rememberReorderableLazyListState(listState) { from, to ->
            onReorder(from.index, to.index)
        }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(UiDp.dp12),
    ) {
        items(items = profiles, key = { it.uuid.toString() }) { profile ->
            ReorderableItem(reorderState, key = profile.uuid.toString()) { _ ->
                // 设置页偏好行同款(BasicComponent,默认 56dp 最小行高 + 16dp 内边距 + miuix 原生排版),
                // 行高压到 53dp;圆角在原生 16dp 基础上加到 20dp(仍小于 AppCard 全局 24dp,不会胶囊化)。
                Card(cornerRadius = 20.dp) {
                    BasicComponent(
                        modifier = Modifier.height(53.dp),
                        title = profile.name,
                        endActions = {
                            Icon(
                                imageVector = Yume.List,
                                contentDescription = null,
                                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                modifier = Modifier.draggableHandle().size(UiDp.dp24),
                            )
                        },
                    )
                }
            }
        }
    }
}
