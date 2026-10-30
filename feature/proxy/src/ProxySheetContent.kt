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
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.yumeyucca.yumebox.data.model.PROXY_SHEET_HEIGHT_FRACTION_DEFAULT
import com.github.yumeyucca.yumebox.data.model.ProxySortMode
import com.github.yumeyucca.yumebox.domain.model.ProxyGroupInfo
import com.github.yumeyucca.yumebox.presentation.component.GlassTopActionDefaults
import com.github.yumeyucca.yumebox.presentation.component.SheetHeaderIconButton
import com.github.yumeyucca.yumebox.presentation.component.FloatingSheetHeader
import com.github.yumeyucca.yumebox.presentation.component.rememberSurfaceBackdrop
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.ListChevronsUpDown
import com.github.yumeyucca.yumebox.presentation.icon.yume.Speed
import com.github.yumeyucca.yumebox.presentation.screen.SelectedGroupController
import com.github.yumeyucca.yumebox.presentation.screen.node.NodeGroupSheetContent
import com.github.yumeyucca.yumebox.presentation.screen.node.NodeSheetContent
import com.github.yumeyucca.yumebox.presentation.screen.node.NodeSortPopupOverlay
import com.github.yumeyucca.yumebox.presentation.screen.node.ProvideProxyDelayFlows
import com.github.yumeyucca.yumebox.presentation.screen.rememberProxyGroupSelectionState
import com.github.yumeyucca.yumebox.presentation.screen.rememberSelectedGroupController
import com.github.yumeyucca.yumebox.presentation.theme.AnimationSpecs
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.presentation.util.rememberSavedListState
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import com.github.yumeyucca.yumebox.presentation.viewmodel.DelayTestState
import com.github.yumeyucca.yumebox.presentation.viewmodel.ProxyViewModel
import com.github.yumeyucca.yumebox.runtime.client.ProxyGroupSyncSource
import org.koin.androidx.compose.koinViewModel
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.glass.GlassPopupAnchor
import top.yukonga.miuix.kmp.glass.glassPopupAnchor
import top.yukonga.miuix.kmp.glass.glassPopupAnchorContent
import top.yukonga.miuix.kmp.glass.rememberGlassPopupAnchor
import top.yukonga.miuix.kmp.glass.GlassBottomSheet

/** Kept off the tail of the sheet enter/exit so the two panes never overlap mid-flight. */
private const val SheetTransitionTrimMillis = 20

@Composable
fun ProxySheetContent(onDismiss: () -> Unit, proxyViewModel: ProxyViewModel = koinViewModel()) {
    val proxyGroups by proxyViewModel.sortedProxyGroups.collectAsState()
    val delayTests by proxyViewModel.delayTests.collectAsState()
    val sortMode by proxyViewModel.sortMode.collectAsState()

    val showSheet = remember { mutableStateOf(true) }
    val showSortPopup = remember { mutableStateOf(false) }
    val sortPopupAnchor = rememberGlassPopupAnchor()
    val selectionController = rememberSelectedGroupController()
    val groupSelection =
        rememberProxyGroupSelectionState(
            proxyGroups = proxyGroups,
            onRefreshGroup = proxyViewModel::refreshGroup,
            retainLastKnownGroup = false,
            controller = selectionController,
        )
    val selectedGroupName = groupSelection.selectedGroupName
    val selectedGroup = groupSelection.selectedGroup
    val groupListState = rememberLazyListState()
    val nodeListState = rememberSavedListState(selectedGroupName)
    val sheetColor = MiuixTheme.colorScheme.surface
    // The glass buttons sample the sheet surface plus the list, so the surface has to be there.
    val sheetContentBackdrop = rememberSurfaceBackdrop(sheetColor)

    ProxySheetHost(
        proxyViewModel = proxyViewModel,
        showSheet = showSheet,
        showSortPopup = showSortPopup,
        sheetColor = sheetColor,
        onDismissFinished = onDismiss,
    ) {
        Box(Modifier.fillMaxWidth().layerBackdrop(sheetContentBackdrop)) {
            ProxySheetPanes(
                selection = selectionController,
                proxyGroups = proxyGroups,
                delayTests = delayTests,
                groupListState = groupListState,
                nodeListState = nodeListState,
                proxyViewModel = proxyViewModel,
            )
        }
        ProxySheetHeaderRow(
            backdrop = sheetContentBackdrop,
            selectedGroup = selectedGroup,
            sheetColor = sheetColor,
            startAction = {
                SheetSortAction(
                    hasGroup = selectedGroup != null,
                    showSortPopup = showSortPopup,
                    anchor = sortPopupAnchor,
                    backdrop = sheetContentBackdrop,
                    sortMode = sortMode,
                    onSortSelected = proxyViewModel::setSortMode,
                    onClearSelection = selectionController::clear,
                )
            },
            endAction = { SheetTestAction(onTest = { proxyViewModel.testDelay(selectedGroup?.name) }) },
        )
    }
}

/** The sheet shell: enter/exit animation, the pinned header and the list underneath it. */
@Composable
private fun ProxySheetHost(
    proxyViewModel: ProxyViewModel,
    showSheet: MutableState<Boolean>,
    showSortPopup: MutableState<Boolean>,
    sheetColor: Color,
    onDismissFinished: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    DisposableEffect(Unit) {
        proxyViewModel.ensureCoreLoaded(true, source = ProxyGroupSyncSource.Sheet)
        onDispose { proxyViewModel.ensureCoreLoaded(false, source = ProxyGroupSyncSource.Sheet) }
    }

    val dismissSheet =
        remember {
            {
                showSortPopup.value = false
                showSheet.value = false
            }
        }
    GlassBottomSheet(
        show = showSheet.value,
        // The panel never samples the page (solid fill). The library's title row is left empty
        // (24dp handle + 18dp gutter = 42dp of chrome); FloatingSheetHeader draws the title and
        // the round buttons from the card's top edge (76dp band, 42dp buttons, 18dp outboard).
        backdrop = null,
        containerColor = sheetColor,
        title = null,
        onDismissRequest = { dismissSheet() },
        onDismissFinished = onDismissFinished,
        enableWindowDim = true,
        // Horizontal inset is pushed down to the content layer: the library's insideMargin would
        // indent the title row and both round buttons with it, while the design keeps the title
        // row full-card-width and lets only the content take 16dp.
        insideMargin = DpSize(0.dp, UiDp.dp16),
        enableNestedScroll = false,
    ) {
        Box(Modifier.fillMaxWidth()) {
            content()
        }
    }
}

/** Pager between the group list and the nodes of the selected group. */
@Composable
private fun ProxySheetPanes(
    selection: SelectedGroupController,
    proxyGroups: List<ProxyGroupInfo>,
    delayTests: DelayTestState,
    groupListState: LazyListState,
    nodeListState: LazyListState,
    proxyViewModel: ProxyViewModel,
) {
    ProvideProxyDelayFlows(proxyViewModel) {
        AnimatedContent(
            targetState = selection.name,
            transitionSpec = { proxySheetContentTransform(selection.name != null) },
            label = "notification_node_sheet_content",
        ) { targetGroupName ->
            val targetGroup = targetGroupName?.let { name ->
                proxyGroups.firstOrNull { group -> group.name == name }
            }
            if (targetGroup == null) {
                SheetGroupPane(
                    groups = proxyGroups,
                    delayTests = delayTests,
                    listState = groupListState,
                    onGroupClick = selection::selectGroup,
                    onGroupTest = { group -> proxyViewModel.testDelay(group.name) },
                )
            } else {
                SheetNodePane(
                    group = targetGroup,
                    delayTests = delayTests,
                    selectedGroupName = selection.name,
                    listState = nodeListState,
                    proxyViewModel = proxyViewModel,
                )
            }
        }
    }
}

/** Title row of the sheet, drawn over the list. */
@Composable
private fun BoxScope.ProxySheetHeaderRow(
    backdrop: LayerBackdrop,
    selectedGroup: ProxyGroupInfo?,
    sheetColor: Color,
    startAction: @Composable () -> Unit,
    endAction: @Composable () -> Unit,
) {
    FloatingSheetHeader(
        backdrop = backdrop,
        title = selectedGroup?.name ?: YumeTxt.Proxy.Title,
        sheetColor = sheetColor,
        startAction = startAction,
        endAction = endAction,
    )
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

/** Sort button of the sheet header, together with the popup it opens. */
@Composable
private fun SheetSortAction(
    hasGroup: Boolean,
    showSortPopup: MutableState<Boolean>,
    anchor: GlassPopupAnchor,
    backdrop: LayerBackdrop,
    sortMode: ProxySortMode,
    onSortSelected: (ProxySortMode) -> Unit,
    onClearSelection: () -> Unit,
) {
    AnimatedContent(
        targetState = hasGroup,
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
                onClick = onClearSelection,
            )
        } else {
            Box {
                SheetHeaderIconButton(
                    icon = Yume.ListChevronsUpDown,
                    contentDescription = YumeTxt.Proxy.Action.Sort,
                    onClick = { showSortPopup.value = true },
                    iconModifier =
                        Modifier
                            .glassPopupAnchor(
                                anchor = anchor,
                                cornerRadius = GlassTopActionDefaults.IconSize / 2,
                                // This icon sits in the sheet header, not over the page like
                                // the top-bar pill. Non-floating anchors fade the panel with
                                // the icon instead of keeping it alive until geometry reaches 0.
                                floating = false,
                            )
                            .glassPopupAnchorContent(anchor),
                )
                NodeSortPopupOverlay(
                    show = showSortPopup.value,
                    onDismiss = { showSortPopup.value = false },
                    anchor = anchor,
                    backdrop = backdrop,
                    sortMode = sortMode,
                    onSortSelected = onSortSelected,
                )
            }
        }
    }
}

/** Speed-test button of the sheet header. */
@Composable
private fun SheetTestAction(onTest: () -> Unit) {
    SheetHeaderIconButton(
        icon = Yume.Speed,
        contentDescription = YumeTxt.Proxy.Action.Test,
        onClick = onTest,
    )
}

/** Group list shown while no group is selected. */
@Composable
private fun SheetGroupPane(
    groups: List<ProxyGroupInfo>,
    delayTests: DelayTestState,
    listState: LazyListState,
    onGroupClick: (ProxyGroupInfo) -> Unit,
    onGroupTest: (ProxyGroupInfo) -> Unit,
) {
    NodeGroupSheetContent(
        groups = groups,
        onGroupClick = onGroupClick,
        onGroupTest = onGroupTest,
        testingGroupNames = delayTests.groupNames,
        sheetHeightFraction = PROXY_SHEET_HEIGHT_FRACTION_DEFAULT,
        listState = listState,
    )
}

/** Node list of the selected group. */
@Composable
private fun SheetNodePane(
    group: ProxyGroupInfo,
    delayTests: DelayTestState,
    selectedGroupName: String?,
    listState: LazyListState,
    proxyViewModel: ProxyViewModel,
) {
    NodeSheetContent(
        group = group,
        testingGroupNames = delayTests.groupNames,
        delayTestProgress = proxyViewModel.delayTestProgress,
        // Non-selectable groups route the click to a delay test inside NodeSheetContent already.
        onSelectProxy = { proxyName -> proxyViewModel.selectProxy(group.name, proxyName) },
        onTestDelay = { selectedGroupName?.let(proxyViewModel::testDelay) },
        testingProxyNames = delayTests.proxyNames,
        onTestProxyDelay = proxyViewModel::testProxyDelay,
        sheetHeightFraction = PROXY_SHEET_HEIGHT_FRACTION_DEFAULT,
        listState = listState,
    )
}

/** Slide and fade for the sheet's group/node pager, in both directions. */
private fun proxySheetContentTransform(forward: Boolean): ContentTransform {
    val motion = AnimationSpecs.Proxy
    return if (forward) {
        proxySheetTransition(
            enterSlideMillis = motion.SheetSlideInDuration,
            exitSlideMillis = motion.SheetSlideOutDuration,
            enterOffsetX = { fullWidth -> fullWidth },
            exitOffsetX = { fullWidth -> -fullWidth / 3 },
        )
    } else {
        proxySheetTransition(
            enterSlideMillis = motion.SheetSlideOutDuration,
            exitSlideMillis = motion.SheetSlideInDuration - SheetTransitionTrimMillis,
            enterOffsetX = { fullWidth -> -fullWidth / 3 },
            exitOffsetX = { fullWidth -> fullWidth },
            enterFadeMillis = motion.SheetFadeInDuration - SheetTransitionTrimMillis,
        )
    }
}
