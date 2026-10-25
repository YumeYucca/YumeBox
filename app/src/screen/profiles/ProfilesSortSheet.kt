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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import com.github.yumeyucca.yumebox.data.model.PROXY_SHEET_HEIGHT_FRACTION_DEFAULT
import com.github.yumeyucca.yumebox.presentation.component.FloatingSheetHeader
import com.github.yumeyucca.yumebox.presentation.component.FloatingSheetHeaderDefaults
import com.github.yumeyucca.yumebox.presentation.component.GlassTopAction
import com.github.yumeyucca.yumebox.presentation.component.GlassTopIconButton
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.Check
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
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.window.WindowBottomSheet

/**
 * 拖拽排序配置的底部 sheet(参考系统"自定义排序"弹窗):每行一张卡片,名称在左,
 * 拖拽手柄(≡)在右侧,无勾选态。chrome 与节点弹窗对齐(NexioSchedule BlurBottomSheet
 * 结构):库层标题传 null,标题与关闭/保存玻璃钮渲染进 76dp 悬浮头,列表从常驻渐变
 * 遮罩下滚过;高度固定为屏幕默认比例,长列表内部滚动。拖动只调整本地草稿序,点右上角
 * 保存按钮才通过 [onOrderCommitted] 整份应用一次;关闭(左上角 X / 背板 / 返回)
 * 丢弃未保存的调整。
 */
@Composable
internal fun ProfilesSortSheet(
    show: Boolean,
    profiles: List<Profile>,
    onOrderCommitted: (List<Profile>) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetColor = MiuixTheme.colorScheme.surface
    // 弹窗内容自身的 backdrop(捕获"底色+列表"),供悬浮头玻璃圆钮采样;头在捕获子树之外,
    // 满足 miuix-blur 的 same-subtree 约束(见 LocalGlassBackdrop 文档)。
    val sheetBackdrop =
        key(sheetColor) {
            rememberLayerBackdrop {
                drawRect(sheetColor)
                drawContent()
            }
        }
    // 草稿序:拖动途中只在本地重排预览,保存时整份提交一次。show/profiles 变化即重置草稿
    // (丢弃后重开、外部刷新、自己提交后都回到已提交序),拖动途中二者不变、草稿得以保留。
    var currentDraft by remember(show, profiles) { mutableStateOf(profiles) }

    WindowBottomSheet(
        show = show,
        // 标题与关闭/保存玻璃钮渲染进悬浮头(节点弹窗同款:固定 76dp 带 + 常驻渐变遮罩,
        // 内容从带下滚过);库层标题行传 null,其空行高由悬浮头偏移吸收。
        title = null,
        backgroundColor = sheetColor,
        onDismissRequest = onDismiss,
        onDismissFinished = onDismiss,
        enableWindowDim = true,
        insideMargin = DpSize(UiDp.dp16, UiDp.dp16),
        enableNestedScroll = false,
    ) {
        // 列表区域由 sheetBackdrop 捕获(底色+列表),悬浮头玻璃钮采样它;头在捕获子树之外。
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxWidth().layerBackdrop(sheetBackdrop)) {
                ProfilesSortList(
                    draftProfiles = currentDraft,
                    onDraftChange = { currentDraft = it },
                )
            }
            FloatingSheetHeader(
                title = YumeTxt.ProfilesPage.Action.Sort,
                sheetColor = sheetColor,
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
                endAction = {
                    GlassTopIconButton(
                        action =
                            GlassTopAction(
                                icon = Yume.Check,
                                contentDescription = YumeTxt.Editor.Action.Save,
                                onClick = {
                                    if (currentDraft.map { it.uuid } != profiles.map { it.uuid }) {
                                        onOrderCommitted(currentDraft)
                                    }
                                    onDismiss()
                                },
                            ),
                        backdrop = sheetBackdrop,
                    )
                },
            )
        }
    }
}

@Composable
private fun ProfilesSortList(
    draftProfiles: List<Profile>,
    onDraftChange: (List<Profile>) -> Unit,
) {
    val configuration = LocalConfiguration.current
    // 与节点弹窗(NodeSheetContent)同高:屏幕高度 × 默认比例,固定高度,长列表内部滚动。
    val sheetHeight =
        remember(configuration.screenHeightDp) {
            configuration.screenHeightDp.dp * PROXY_SHEET_HEIGHT_FRACTION_DEFAULT
        }
    val listState = rememberLazyListState()
    // reorderable 库持有的 onDrag 闭包可能停在首帧,经 rememberUpdatedState 中转,
    // 保证拖动换位时读到的是最新草稿序。
    val latestDraft by rememberUpdatedState(draftProfiles)
    val latestOnDraftChange by rememberUpdatedState(onDraftChange)
    val reorderState =
        rememberReorderableLazyListState(listState) { from, to ->
            latestOnDraftChange(
                latestDraft.toMutableList().apply { add(to.index, removeAt(from.index)) },
            )
        }

    LazyColumn(
        state = listState,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(sheetHeight)
                .overScrollVertical(),
        // 顶部让位悬浮头的 76dp 带(58dp 内容让位 + 8dp 呼吸留白):首行从头带之下开始,
        // 向上滚动时行"沉入"常驻渐变遮罩(节点弹窗 NodeSheetContentPadding 同款)。
        contentPadding =
            PaddingValues(
                top = FloatingSheetHeaderDefaults.ContentTopInset + UiDp.dp8,
                bottom = UiDp.dp16,
            ),
        verticalArrangement = Arrangement.spacedBy(UiDp.dp12),
        overscrollEffect = null,
    ) {
        items(items = draftProfiles, key = { it.uuid.toString() }) { profile ->
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
