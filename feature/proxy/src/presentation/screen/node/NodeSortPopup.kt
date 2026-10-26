/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
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

package com.github.yumeyucca.yumebox.presentation.screen.node

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import com.github.yumeyucca.yumebox.data.model.ProxySortMode
import com.github.yumeyucca.yumebox.presentation.theme.LocalGlassBackdrop
import com.github.yumeyucca.yumebox.presentation.theme.LocalTopBarBackdrop
import io.github.yumeyucca.lucide.Lucide
import io.github.yumeyucca.lucide.lucide.ArrowUpDown
import io.github.yumeyucca.lucide.lucide.Locate
import io.github.yumeyucca.lucide.lucide.Search
import io.github.yumeyucca.lucide.lucide.Waypoints
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.glass.GlassPopup
import top.yukonga.miuix.kmp.glass.GlassPopupAnchor
import top.yukonga.miuix.kmp.glass.GlassPopupDefaults
import top.yukonga.miuix.kmp.glass.GlassPopupItem
import top.yukonga.miuix.kmp.glass.GlassPopupSizing
import top.yukonga.miuix.kmp.glass.GlassSecondaryPopup
import top.yukonga.miuix.kmp.glass.GlassTransformPopup
import top.yukonga.miuix.kmp.layout.CascadingPopupDefaults
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils.Companion.PopupLayout

internal val NodeSortModes =
    listOf(ProxySortMode.DEFAULT, ProxySortMode.BY_NAME, ProxySortMode.BY_LATENCY)

@Composable
internal fun NodeActionsMenu(
    show: Boolean,
    onDismiss: () -> Unit,
    sortMode: ProxySortMode,
    onLocateCurrentProxy: (() -> Unit)? = null,
    onOpenRouting: (() -> Unit)? = null,
    searchVisible: Boolean = false,
    onToggleSearch: (() -> Unit)? = null,
    onSortSelected: (ProxySortMode) -> Unit,
    anchor: GlassPopupAnchor,
    anchorContent: @Composable () -> Unit,
) {
    // PopupLayout relocates this content; capture the page backdrop before leaving its provider.
    val backdrop = LocalGlassBackdrop.current ?: LocalTopBarBackdrop.current
    val popupVisible = remember { mutableStateOf(false) }
    val popupVisuals = GlassPopupDefaults.visuals()
    var submenu by remember { mutableStateOf(false) }
    var submenuPresent by remember { mutableStateOf(false) }
    var submenuAnchor by remember { mutableStateOf(Rect.Zero) }
    var menuWidth by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    val arrowTurn = CascadingPopupDefaults.arrowRotation(LocalLayoutDirection.current)
    val submenuArrow by animateFloatAsState(
        targetValue = if (submenu) arrowTurn else 0f,
        animationSpec = CascadingPopupDefaults.arrowSpring(submenu),
        label = "nodeActionsSubmenuArrow",
    )
    LaunchedEffect(show) {
        if (show) {
            popupVisible.value = true
        } else {
            submenu = false
        }
    }
    PopupLayout(
        visible = popupVisible,
        enterTransition = EnterTransition.None,
        exitTransition = ExitTransition.None,
        enableWindowDim = false,
        enableBackHandler = false,
    ) {
        Box(Modifier.fillMaxSize()) {
            // Compose the component closed first, just like the persistent upstream sample.
            // The outer host otherwise creates it with show=true and skips its entering transition.
            var expanded by remember { mutableStateOf(false) }
            LaunchedEffect(show) { expanded = show }
            GlassTransformPopup(
                show = expanded,
                onDismissRequest = {
                    submenu = false
                    onDismiss()
                },
                anchor = anchor,
                backdrop = backdrop,
                anchorContent = anchorContent,
                // The pill anchor supplies the shared surface; this remains the no-surface fallback.
                anchorAlpha = 0f,
                sizing = GlassPopupSizing(minWidth = 188.dp, maxWidth = 188.dp),
                visuals = popupVisuals,
                stacked = submenu,
                onDismissFinished = { if (!show) popupVisible.value = false },
                onMeasured = { menuWidth = with(density) { it.width.toDp() } },
            ) {
                onOpenRouting?.let { openRouting ->
                    GlassPopupItem(
                        text = YumeTxt.MetaFeature.Title,
                        icon = Lucide.Waypoints,
                        onClick = {
                            openRouting()
                            onDismiss()
                        },
                    )
                }
                onToggleSearch?.let { toggleSearch ->
                    GlassPopupItem(
                        text = YumeTxt.Proxy.Action.SearchNodes,
                        icon = Lucide.Search,
                        selected = searchVisible,
                        onClick = {
                            toggleSearch()
                            onDismiss()
                        },
                    )
                }
                onLocateCurrentProxy?.let { locateCurrentProxy ->
                    GlassPopupItem(
                        text = YumeTxt.Proxy.Action.LocateCurrent,
                        icon = Lucide.Locate,
                        onClick = {
                            locateCurrentProxy()
                            onDismiss()
                        },
                    )
                }
                GlassPopupItem(
                    text = YumeTxt.Proxy.Action.Sort,
                    icon = Lucide.ArrowUpDown,
                    showArrow = true,
                    arrowRotation = { submenuArrow * (1f - anchor.secondaryBackProgress) },
                    modifier = Modifier.onGloballyPositioned {
                        if (!submenuPresent) submenuAnchor = it.boundsInRoot()
                    },
                    onClick = {
                        submenuPresent = true
                        submenu = true
                    },
                )
            }
            GlassSecondaryPopup(
                show = expanded && submenu,
                onDismissRequest = { submenu = false },
                anchorBounds = submenuAnchor,
                backdrop = backdrop,
                materialAnchor = anchor,
                sizing = GlassPopupSizing(minWidth = menuWidth, maxWidth = 188.dp),
                visuals = popupVisuals,
                onDismissFinished = { submenuPresent = false },
            ) {
                GlassPopupItem(
                    text = YumeTxt.Proxy.Action.Sort,
                    showArrow = true,
                    arrowRotation = { submenuArrow * (1f - anchor.secondaryBackProgress) },
                    onClick = { submenu = false },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(
                        horizontal = GlassPopupDefaults.DividerPaddingHorizontal,
                        vertical = GlassPopupDefaults.DividerPaddingVertical,
                    ),
                    thickness = GlassPopupDefaults.DividerThickness,
                )
                NodeSortModes.forEach { mode ->
                    GlassPopupItem(
                        text = mode.displayName,
                        selected = mode == sortMode,
                        onClick = {
                            if (mode != sortMode) onSortSelected(mode)
                            submenu = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
internal fun NodeSortPopupOverlay(
    show: Boolean,
    onDismiss: () -> Unit,
    anchorBounds: Rect,
    backdrop: Backdrop?,
    sortMode: ProxySortMode,
    onSortSelected: (ProxySortMode) -> Unit,
) {
    val popupVisible = remember { mutableStateOf(show) }
    LaunchedEffect(show) { popupVisible.value = show }
    PopupLayout(
        visible = popupVisible,
        enableWindowDim = false,
        enableBackHandler = false,
    ) {
        Box(Modifier.fillMaxSize()) {
            GlassPopup(
                show = show,
                onDismissRequest = onDismiss,
                anchorBounds = anchorBounds,
                backdrop = backdrop,
                sizing = GlassPopupSizing(minWidth = 188.dp, maxWidth = 188.dp),
            ) {
                NodeSortModes.forEach { mode ->
                    GlassPopupItem(
                        text = mode.displayName,
                        selected = mode == sortMode,
                        onClick = {
                            if (mode != sortMode) onSortSelected(mode)
                            onDismiss()
                        },
                    )
                }
            }
        }
    }
}
