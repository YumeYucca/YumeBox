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

package com.github.yumeyucca.yumebox


import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.yumeyucca.yumebox.data.model.PROXY_SHEET_HEIGHT_FRACTION_DEFAULT
import com.github.yumeyucca.yumebox.domain.model.ProxyGroupInfo
import com.github.yumeyucca.yumebox.domain.model.isSelectable
import com.github.yumeyucca.yumebox.presentation.component.GlassTopActionDefaults
import com.github.yumeyucca.yumebox.presentation.component.SheetHeaderIconButton
import com.github.yumeyucca.yumebox.presentation.component.FloatingSheetHeader
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.ListChevronsUpDown
import com.github.yumeyucca.yumebox.presentation.icon.yume.Speed
import com.github.yumeyucca.yumebox.presentation.screen.node.NodeGroupSheetContent
import com.github.yumeyucca.yumebox.presentation.screen.node.NodeSheetContent
import com.github.yumeyucca.yumebox.presentation.screen.node.NodeSortPopupOverlay
import com.github.yumeyucca.yumebox.presentation.screen.node.ProvideProxyDelayFlows
import com.github.yumeyucca.yumebox.presentation.screen.rememberProxyGroupSelectionState
import com.github.yumeyucca.yumebox.presentation.theme.AnimationSpecs
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import com.github.yumeyucca.yumebox.presentation.viewmodel.ProxyViewModel
import org.koin.androidx.compose.koinViewModel
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.glass.glassPopupAnchor
import top.yukonga.miuix.kmp.glass.glassPopupAnchorContent
import top.yukonga.miuix.kmp.glass.rememberGlassPopupAnchor
import top.yukonga.miuix.kmp.glass.GlassBottomSheet

@Composable
fun ProxySheetContent(onDismiss: () -> Unit, proxyViewModel: ProxyViewModel = koinViewModel()) {
    val proxyGroups by proxyViewModel.sortedProxyGroups.collectAsState()
    val testingProxyNames by proxyViewModel.testingProxyNames.collectAsState()
    val sortMode by proxyViewModel.sortMode.collectAsState()

    val showSheet = remember { mutableStateOf(true) }
    val showSortPopup = remember { mutableStateOf(false) }
    val sortPopupAnchor = rememberGlassPopupAnchor()
    val groupSelection =
        rememberProxyGroupSelectionState(
            proxyGroups = proxyGroups,
            onRefreshGroup = proxyViewModel::refreshGroup,
            retainLastKnownGroup = false,
        )
    val selectedGroupName = groupSelection.selectedGroupName
    val selectedGroup = groupSelection.selectedGroup
    val groupListState = rememberLazyListState()
    val nodeListState =
        rememberSaveable(selectedGroupName, saver = LazyListState.Saver) { LazyListState() }
    val sheetColor = MiuixTheme.colorScheme.surface
    // 玻璃钮采样「sheet 底色 + 列表」:没有底色时静止状态下背后是空的,只有列表滚过才透出玻璃与描边。
    val sheetContentBackdrop =
        key(sheetColor) {
            rememberLayerBackdrop {
                drawRect(sheetColor)
                drawContent()
            }
        }

    DisposableEffect(Unit) {
        proxyViewModel.ensureCoreLoaded(true, source = "proxy_sheet")
        onDispose { proxyViewModel.ensureCoreLoaded(false, source = "proxy_sheet") }
    }

    val dismissSheet =
        remember {
            {
                showSortPopup.value = false
                showSheet.value = false
            }
        }
    val triggerTopDelayTest =
        remember(proxyViewModel) {
            { proxyViewModel.testDelay() }
        }
    val triggerSelectedGroupDelayTest =
        remember(proxyViewModel, selectedGroupName) {
            {
                val groupName = selectedGroupName ?: return@remember
                proxyViewModel.testDelay(groupName)
            }
        }
    val headerStartAction: @Composable () -> Unit = {
        AnimatedContent(
            targetState = selectedGroup != null,
            transitionSpec = {
                val motion = AnimationSpecs.Proxy
                val forward = targetState
                proxySheetTransition(
                    enterSlideMillis =
                        if (forward) {
                            motion.SheetSlideInDuration
                        } else {
                            motion.SheetSlideOutDuration
                        },
                    exitSlideMillis = motion.SheetSlideOutDuration,
                    enterOffsetX = { width -> if (forward) width / 3 else -width / 3 },
                    exitOffsetX = { width -> if (forward) -width / 3 else width / 3 },
                )
            },
            label = "notification_node_sheet_start_action",
        ) { showBackAction ->
            if (showBackAction) {
                SheetHeaderIconButton(
                    icon = MiuixIcons.Back,
                    contentDescription = YumeTxt.Component.Navigation.Back,
                    onClick = groupSelection.clearSelection,
                )
            } else {
                Box {
                    Box(
                        modifier =
                            Modifier.glassPopupAnchor(
                                anchor = sortPopupAnchor,
                                cornerRadius = GlassTopActionDefaults.ContainerSize / 2,
                                floating = true,
                            ),
                    ) {
                        SheetHeaderIconButton(
                            icon = Yume.ListChevronsUpDown,
                            contentDescription = YumeTxt.Proxy.Action.Sort,
                            onClick = { showSortPopup.value = true },
                            iconModifier = Modifier.glassPopupAnchorContent(sortPopupAnchor),
                        )
                    }
                    NodeSortPopupOverlay(
                        show = showSortPopup.value,
                        onDismiss = { showSortPopup.value = false },
                        anchor = sortPopupAnchor,
                        anchorContent = {
                            Icon(
                                imageVector = Yume.ListChevronsUpDown,
                                contentDescription = null,
                                modifier = Modifier.size(GlassTopActionDefaults.IconSize),
                                tint = MiuixTheme.colorScheme.onSurface,
                            )
                        },
                        backdrop = null,
                        sortMode = sortMode,
                        onSortSelected = proxyViewModel::setSortMode,
                    )
                }
            }
        }
    }
    val headerEndAction: @Composable () -> Unit = {
        SheetHeaderIconButton(
            icon = Yume.Speed,
            contentDescription = YumeTxt.Proxy.Action.Test,
            onClick = {
                if (selectedGroup == null) {
                    triggerTopDelayTest()
                } else {
                    triggerSelectedGroupDelayTest()
                }
            },
        )
    }
    GlassBottomSheet(
        show = showSheet.value,
        // 面板不采样页面(纯实色)。库层标题行留空(24dp 把手 + 18dp 空行 = 42dp chrome),
        // 标题与左右圆钮由 FloatingSheetHeader 从卡片顶边开始绘制(76dp 带、42dp 圆钮、外侧 18dp)。
        backdrop = null,
        containerColor = sheetColor,
        title = null,
        onDismissRequest = { dismissSheet() },
        onDismissFinished = onDismiss,
        enableWindowDim = true,
        // 水平内缩下放到内容层:库层 insideMargin 会把标题行和两侧圆钮一起缩进,
        // 参考里标题行是满卡片宽、只有内容吃 16dp。
        insideMargin = DpSize(0.dp, UiDp.dp16),
        enableNestedScroll = false,
    ) {
        Box(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().layerBackdrop(sheetContentBackdrop)) {
                    ProvideProxyDelayFlows(proxyViewModel) {
                    AnimatedContent(
                        targetState = selectedGroupName,
                        transitionSpec = {
                            val motion = AnimationSpecs.Proxy
                            if (targetState != null) {
                                proxySheetTransition(
                                    enterSlideMillis = motion.SheetSlideInDuration,
                                    exitSlideMillis = motion.SheetSlideOutDuration,
                                    enterOffsetX = { fullWidth -> fullWidth },
                                    exitOffsetX = { fullWidth -> -fullWidth / 3 },
                                )
                            } else {
                                proxySheetTransition(
                                    enterSlideMillis = motion.SheetSlideOutDuration,
                                    exitSlideMillis = motion.SheetSlideInDuration - 20,
                                    enterOffsetX = { fullWidth -> -fullWidth / 3 },
                                    exitOffsetX = { fullWidth -> fullWidth },
                                    enterFadeMillis = motion.SheetFadeInDuration - 20,
                                )
                            }
                        },
                        label = "notification_node_sheet_content",
                    ) { targetGroupName ->
                        val targetGroup = targetGroupName?.let { name ->
                            proxyGroups.firstOrNull { group -> group.name == name }
                        }
                        if (targetGroup == null) {
                            val testingGroupNames by proxyViewModel.testingGroupNames.collectAsState()
                            NodeGroupSheetContent(
                                groups = proxyGroups,
                                onGroupClick = groupSelection.selectGroup,
                                onGroupTest = { group -> proxyViewModel.testDelay(group.name) },
                                testingGroupNames = testingGroupNames,
                                sheetHeightFraction = PROXY_SHEET_HEIGHT_FRACTION_DEFAULT,
                                listState = groupListState,
                            )
                        } else {
                            ProxySheetNodeContent(
                                proxyViewModel = proxyViewModel,
                                group = targetGroup,
                                testingProxyNames = testingProxyNames,
                                onTestDelay = triggerSelectedGroupDelayTest,
                                sheetHeightFraction = PROXY_SHEET_HEIGHT_FRACTION_DEFAULT,
                                listState = nodeListState,
                            )
                        }
                    }
                }
            }
            FloatingSheetHeader(
                backdrop = sheetContentBackdrop,
                title = selectedGroup?.name ?: YumeTxt.Proxy.Title,
                sheetColor = sheetColor,
                startAction = headerStartAction,
                endAction = headerEndAction,
            )
        }
    }
}

private fun proxySheetTransition(
    enterSlideMillis: Int,
    exitSlideMillis: Int,
    enterOffsetX: (Int) -> Int,
    exitOffsetX: (Int) -> Int,
    enterFadeMillis: Int = AnimationSpecs.Proxy.SheetFadeInDuration,
): ContentTransform =
    (slideInHorizontally(
        animationSpec = tween(durationMillis = enterSlideMillis, easing = AnimationSpecs.Legacy),
        initialOffsetX = enterOffsetX,
    ) + fadeIn(animationSpec = tween(durationMillis = enterFadeMillis))) togetherWith
        (slideOutHorizontally(
            animationSpec = tween(durationMillis = exitSlideMillis, easing = AnimationSpecs.Legacy),
            targetOffsetX = exitOffsetX,
        ) + fadeOut(animationSpec = tween(durationMillis = AnimationSpecs.Proxy.SheetFadeOutDuration)))

@Composable
private fun ProxySheetNodeContent(
    proxyViewModel: ProxyViewModel,
    group: ProxyGroupInfo,
    testingProxyNames: Set<String>,
    onTestDelay: () -> Unit,
    sheetHeightFraction: Float,
    listState: LazyListState,
) {
    val onSelectProxy =
        remember(group.name, group.type, proxyViewModel, onTestDelay) {
            { proxyName: String ->
                if (group.isSelectable) {
                    proxyViewModel.selectProxy(group.name, proxyName)
                } else {
                    onTestDelay()
                }
            }
        }

    NodeSheetContent(
        group = group,
        testingGroupNames = proxyViewModel.testingGroupNames,
        delayTestProgress = proxyViewModel.delayTestProgress,
        onSelectProxy = onSelectProxy,
        onTestDelay = onTestDelay,
        testingProxyNames = testingProxyNames,
        onTestProxyDelay = proxyViewModel::testProxyDelay,
        sheetHeightFraction = sheetHeightFraction,
        listState = listState,
    )
}
