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

@file:Suppress("DuplicatedCode", "FunctionName")

package com.github.yumeyucca.yumebox


import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.DpSize
import com.github.yumeyucca.yumebox.domain.model.isSelectable
import com.github.yumeyucca.yumebox.presentation.component.AppBottomSheetAction
import com.github.yumeyucca.yumebox.presentation.component.AppBottomSheetIconAction
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.ListChevronsUpDown
import com.github.yumeyucca.yumebox.presentation.icon.yume.Speed
import com.github.yumeyucca.yumebox.presentation.screen.node.NodeGroupSheetContent
import com.github.yumeyucca.yumebox.presentation.screen.node.NodeSheetContent
import com.github.yumeyucca.yumebox.presentation.screen.node.NodeSortPopup
import com.github.yumeyucca.yumebox.presentation.screen.rememberProxyGroupSelectionState
import com.github.yumeyucca.yumebox.presentation.theme.AnimationSpecs
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.presentation.viewmodel.ProxyViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import tf.gal.yumebox.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet

private const val NOTIFICATION_PROXY_SHEET_HEIGHT_FRACTION = 0.55f

private fun LazyListState.isScrolledFromTop(): Boolean =
    firstVisibleItemIndex > 0 || firstVisibleItemScrollOffset > 0

@Composable
fun ProxySheetContent(onDismiss: () -> Unit, proxyViewModel: ProxyViewModel = koinViewModel()) {
    val proxyGroups by proxyViewModel.sortedProxyGroups.collectAsState()
    val testingProxyNames by proxyViewModel.testingProxyNames.collectAsState()
    val sortMode by proxyViewModel.sortMode.collectAsState()

    val showSheet = remember { mutableStateOf(true) }
    val showSortPopup = remember { mutableStateOf(false) }
    val groupSelection =
        rememberProxyGroupSelectionState(
            proxyGroups = proxyGroups,
            onRefreshGroup = proxyViewModel::refreshGroup,
            retainLastKnownGroup = false,
        )
    val selectedGroupName = groupSelection.selectedGroupName
    val selectedGroup = groupSelection.selectedGroup
    val coroutineScope = rememberCoroutineScope()
    val groupListState = rememberLazyListState()
    val nodeListState =
        rememberSaveable(selectedGroupName, saver = LazyListState.Saver) { LazyListState() }

    DisposableEffect(Unit) {
        proxyViewModel.ensureCoreLoaded(true, source = "proxy_sheet")
        onDispose { proxyViewModel.ensureCoreLoaded(false, source = "proxy_sheet") }
    }

    val dismissSheet =
        remember(onDismiss) {
            {
                showSortPopup.value = false
                showSheet.value = false
            }
        }
    val triggerTopDelayTest =
        remember(coroutineScope, groupListState, proxyViewModel) {
            {
                coroutineScope.launch {
                    if (groupListState.isScrolledFromTop()) {
                        groupListState.animateScrollToItem(0)
                    }
                    proxyViewModel.testDelay()
                }
            }
        }
    val triggerSelectedGroupDelayTest =
        remember(coroutineScope, nodeListState, proxyViewModel, selectedGroupName) {
            {
                val groupName = selectedGroupName ?: return@remember
                coroutineScope.launch {
                    if (nodeListState.isScrolledFromTop()) {
                        nodeListState.animateScrollToItem(0)
                    }
                    proxyViewModel.testDelay(groupName)
                }
            }
        }
    LaunchedEffect(showSheet.value) {
        if (!showSheet.value) {
            onDismiss()
        }
    }

    WindowBottomSheet(
        show = showSheet.value,
        title = selectedGroup?.name ?: YumeTxt.Proxy.Title,
        backgroundColor = MiuixTheme.colorScheme.surface,
        startAction = {
            AnimatedContent(
                targetState = selectedGroup != null,
                transitionSpec = {
                    val slideDuration =
                        if (targetState) {
                            AnimationSpecs.Proxy.SheetSlideInDuration
                        } else {
                            AnimationSpecs.Proxy.SheetSlideOutDuration
                        }
                    val initialOffset: (Int) -> Int =
                        if (targetState) {
                            { width -> width / 3 }
                        } else {
                            { width -> -width / 3 }
                        }
                    val targetOffset: (Int) -> Int =
                        if (targetState) {
                            { width -> -width / 3 }
                        } else {
                            { width -> width / 3 }
                        }
                    (slideInHorizontally(
                        animationSpec =
                            tween(
                                durationMillis = slideDuration,
                                easing = AnimationSpecs.Legacy,
                            ),
                        initialOffsetX = initialOffset,
                    ) +
                            fadeIn(
                                animationSpec =
                                    tween(durationMillis = AnimationSpecs.Proxy.SheetFadeInDuration)
                            )) togetherWith
                            (slideOutHorizontally(
                                animationSpec =
                                    tween(
                                        durationMillis = AnimationSpecs.Proxy.SheetSlideOutDuration,
                                        easing = AnimationSpecs.Legacy,
                                    ),
                                targetOffsetX = targetOffset,
                            ) +
                                    fadeOut(
                                        animationSpec =
                                            tween(
                                                durationMillis = AnimationSpecs.Proxy.SheetFadeOutDuration
                                            )
                                    ))
                },
                label = "notification_node_sheet_start_action",
            ) { showBackAction ->
                if (showBackAction) {
                    AppBottomSheetIconAction(
                        action =
                            AppBottomSheetAction(
                                icon = MiuixIcons.Back,
                                contentDescription = YumeTxt.Component.Navigation.Back,
                                onClick = groupSelection.clearSelection,
                            )
                    )
                } else {
                    Box {
                        AppBottomSheetIconAction(
                            action =
                                AppBottomSheetAction(
                                    icon = Yume.ListChevronsUpDown,
                                    contentDescription = YumeTxt.Proxy.Action.Sort,
                                    onClick = { showSortPopup.value = true },
                                )
                        )
                        NodeSortPopup(
                            show = showSortPopup.value,
                            onDismiss = { showSortPopup.value = false },
                            sortMode = sortMode,
                            alignment = PopupPositionProvider.Align.BottomStart,
                            onSortSelected = proxyViewModel::setSortMode,
                        )
                    }
                }
            }
        },
        endAction = {
            AppBottomSheetIconAction(
                action =
                    AppBottomSheetAction(
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
            )
        },
        onDismissRequest = { dismissSheet() },
        enableWindowDim = true,
        insideMargin = DpSize(UiDp.dp16, UiDp.dp16),
        enableNestedScroll = false,
    ) {
        AnimatedContent(
            targetState = selectedGroupName,
            transitionSpec = {
                if (targetState != null) {
                    (slideInHorizontally(
                        animationSpec =
                            tween(
                                durationMillis = AnimationSpecs.Proxy.SheetSlideInDuration,
                                easing = AnimationSpecs.Legacy,
                            ),
                        initialOffsetX = { it },
                    ) +
                            fadeIn(
                                animationSpec =
                                    tween(durationMillis = AnimationSpecs.Proxy.SheetFadeInDuration)
                            )) togetherWith
                            (slideOutHorizontally(
                                animationSpec =
                                    tween(
                                        durationMillis = AnimationSpecs.Proxy.SheetSlideOutDuration,
                                        easing = AnimationSpecs.Legacy,
                                    ),
                                targetOffsetX = { -it / 3 },
                            ) +
                                    fadeOut(
                                        animationSpec =
                                            tween(
                                                durationMillis = AnimationSpecs.Proxy.SheetFadeOutDuration
                                            )
                                    ))
                } else {
                    (slideInHorizontally(
                        animationSpec =
                            tween(
                                durationMillis = AnimationSpecs.Proxy.SheetSlideOutDuration,
                                easing = AnimationSpecs.Legacy,
                            ),
                        initialOffsetX = { -it / 3 },
                    ) +
                            fadeIn(
                                animationSpec =
                                    tween(
                                        durationMillis = AnimationSpecs.Proxy.SheetFadeInDuration - 20
                                    )
                            )) togetherWith
                            (slideOutHorizontally(
                                animationSpec =
                                    tween(
                                        durationMillis = AnimationSpecs.Proxy.SheetSlideInDuration - 20,
                                        easing = AnimationSpecs.Legacy,
                                    ),
                                targetOffsetX = { it },
                            ) +
                                    fadeOut(
                                        animationSpec =
                                            tween(
                                                durationMillis = AnimationSpecs.Proxy.SheetFadeOutDuration
                                            )
                                    ))
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
                    sheetHeightFraction = NOTIFICATION_PROXY_SHEET_HEIGHT_FRACTION,
                    listState = groupListState,
                )
            } else {
                ProxySheetNodeContent(
                    proxyViewModel = proxyViewModel,
                    group = targetGroup,
                    testingProxyNames = testingProxyNames,
                    onTestDelay = triggerSelectedGroupDelayTest,
                    sheetHeightFraction = NOTIFICATION_PROXY_SHEET_HEIGHT_FRACTION,
                    listState = nodeListState,
                )
            }
        }
    }
}

@Composable
private fun ProxySheetNodeContent(
    proxyViewModel: ProxyViewModel,
    group: com.github.yumeyucca.yumebox.domain.model.ProxyGroupInfo,
    testingProxyNames: Set<String>,
    onTestDelay: () -> Unit,
    sheetHeightFraction: Float,
    listState: LazyListState,
) {
    val isDelayTesting by
    remember(group.name, proxyViewModel) {
        proxyViewModel.testingGroupNames
            .map { testingGroupNames -> testingGroupNames.contains(group.name) }
            .distinctUntilChanged()
    }
        .collectAsState(initial = false)
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
        isDelayTesting = isDelayTesting,
        onSelectProxy = onSelectProxy,
        onTestDelay = onTestDelay,
        testingProxyNames = testingProxyNames,
        onTestProxyDelay = { proxyName -> proxyViewModel.testProxyDelay(group.name, proxyName) },
        sheetHeightFraction = sheetHeightFraction,
        listState = listState,
    )
}
