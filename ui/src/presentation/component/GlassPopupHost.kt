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
 */

@file:Suppress("FunctionName")

package com.github.yumeyucca.yumebox.presentation.component

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.presentation.theme.glassSurfaceBackdrop
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.glass.GlassPopupAnchor
import top.yukonga.miuix.kmp.glass.GlassPopupDefaults
import top.yukonga.miuix.kmp.glass.GlassPopupItem
import top.yukonga.miuix.kmp.glass.GlassPopupSizing
import top.yukonga.miuix.kmp.glass.GlassPopupVisuals
import top.yukonga.miuix.kmp.glass.GlassSecondaryPopup
import top.yukonga.miuix.kmp.glass.GlassTransformPopup
import top.yukonga.miuix.kmp.layout.CascadingPopupDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils.Companion.PopupLayout

/** Panel sizing shared by the anchored glass menus. */
val GlassPopupMenuSizing: GlassPopupSizing =
    GlassPopupSizing(
        minWidth = UiDp.dp188,
        maxWidth = UiDp.dp188,
    )

object GlassPopupMenuDefaults {
    val ItemIconSize: Dp = UiDp.dp18

    /** Off the [UiDp] scale on purpose: measured against a MIUI menu, not a spacing step. */
    val ItemIconGap: Dp = 15.dp
}

/**
 * Hosts an anchored glass popup and keeps it mounted until its collapse finishes.
 *
 * A menu that owns a cascading page passes it as [submenu]; the host then feeds the page's
 * position, the panel's measured width, the backdrop and the stacked state, so the page and the
 * panel cannot drift apart. [submenuContent] is only composed while the page is open.
 */
@Composable
fun GlassPopupHost(
    show: Boolean,
    onDismissRequest: () -> Unit,
    anchor: GlassPopupAnchor,
    anchorContent: @Composable () -> Unit,
    backdrop: Backdrop? = glassSurfaceBackdrop(),
    sizing: GlassPopupSizing = GlassPopupMenuSizing,
    visuals: GlassPopupVisuals = GlassPopupDefaults.visuals(),
    alignToPageEnd: Boolean = true,
    dismissOnBack: Boolean = false,
    onDismissFinished: () -> Unit = {},
    submenu: GlassSubmenuState? = null,
    submenuContent: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val currentShow by rememberUpdatedState(show)
    val currentOnDismissFinished by rememberUpdatedState(onDismissFinished)
    val density = LocalDensity.current
    val popupVisible = remember { mutableStateOf(false) }
    var menuWidth by remember { mutableStateOf(UiDp.dp0) }

    LaunchedEffect(show) {
        if (show) popupVisible.value = true
    }
    PopupLayout(
        visible = popupVisible,
        enterTransition = EnterTransition.None,
        exitTransition = ExitTransition.None,
        enableWindowDim = false,
        enableBackHandler = dismissOnBack,
    ) {
        Box(Modifier.fillMaxSize()) {
            // Compose the panel closed first so the opening transform actually plays.
            var expanded by remember { mutableStateOf(false) }
            LaunchedEffect(show) { expanded = show }
            GlassTransformPopup(
                show = expanded,
                onDismissRequest = {
                    submenu?.close()
                    onDismissRequest()
                },
                anchor = anchor,
                backdrop = backdrop,
                anchorContent = anchorContent,
                anchorAlpha = 0f,
                sizing = sizing,
                visuals = visuals,
                stacked = submenu?.open == true,
                alignToPageEnd = alignToPageEnd,
                onMeasured = { size ->
                    menuWidth = with(density) { size.width.toDp() }
                },
                onDismissFinished = {
                    if (!currentShow) {
                        popupVisible.value = false
                        currentOnDismissFinished()
                    }
                },
            ) {
                content()
            }
            if (submenu != null) {
                GlassSecondaryPopup(
                    show = expanded && submenu.open,
                    onDismissRequest = submenu::close,
                    anchorBounds = submenu.bounds,
                    backdrop = backdrop,
                    materialAnchor = anchor,
                    sizing = GlassPopupMenuSizing.fixedWidth(menuWidth),
                    visuals = visuals,
                    content = submenuContent,
                )
            }
        }
    }
}

/**
 * Open state and resting row bounds of one cascading menu page.
 *
 * One instance per page: the row that triggers the page calls [openAt] with its own bounds and the
 * host owns everything else. [open] is readable so the trigger row can animate its arrow.
 */
@Stable
class GlassSubmenuState internal constructor() {
    var open: Boolean by mutableStateOf(false)
        private set

    internal var bounds: Rect by mutableStateOf(Rect.Zero)
        private set

    /** Opens the page so it grows out of the row that reported [rowBounds]. */
    fun openAt(rowBounds: Rect) {
        bounds = rowBounds
        open = true
    }

    /** Returns to the parent menu without dismissing it. */
    fun close() {
        open = false
    }
}

/** Remembers a [GlassSubmenuState] and collapses its page whenever [show] turns false. */
@Composable
fun rememberGlassSubmenuState(show: Boolean): GlassSubmenuState {
    val state = remember { GlassSubmenuState() }
    LaunchedEffect(show) {
        if (!show) state.close()
    }
    return state
}

/**
 * A row in one of the app's anchored glass menus.
 *
 * Every menu shares the compact glyph metrics of [GlassPopupMenuDefaults], so this row supplies
 * them and call sites only state what differs. Use [GlassPopupSubmenuItem] for a row that opens a
 * cascading page and [GlassSubmenuPageHeader] for the row that names one.
 */
@Composable
fun GlassMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    selected: Boolean = false,
    contentColor: Color = MiuixTheme.colorScheme.onSurfaceContainer,
    showArrow: Boolean = false,
    arrowRotation: () -> Float = { 0f },
) {
    GlassPopupItem(
        text = text,
        onClick = onClick,
        modifier = modifier,
        icon = icon,
        iconSize = GlassPopupMenuDefaults.ItemIconSize,
        iconGap = GlassPopupMenuDefaults.ItemIconGap,
        enabled = enabled,
        selected = selected,
        contentColor = contentColor,
        showArrow = showArrow,
        arrowRotation = arrowRotation,
    )
}

/**
 * A row that opens a cascading page: its arrow turns while [open] holds, and it hands its resting
 * bounds to [onOpen] so the page can grow out of the row.
 */
@Composable
fun GlassPopupSubmenuItem(
    text: String,
    anchor: GlassPopupAnchor,
    open: Boolean,
    onOpen: (Rect) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    var rowBounds by remember { mutableStateOf(Rect.Zero) }
    GlassMenuItem(
        text = text,
        icon = icon,
        enabled = enabled,
        showArrow = true,
        arrowRotation = submenuArrow(anchor, open),
        modifier = modifier.onGloballyPositioned { rowBounds = it.boundsInRoot() },
        onClick = { onOpen(rowBounds) },
    )
}

/** The row that names a cascading page, drawn above the page's own rows; it returns to the menu. */
@Composable
fun GlassSubmenuPageHeader(
    state: GlassSubmenuState,
    text: String,
    anchor: GlassPopupAnchor,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    GlassMenuItem(
        text = text,
        icon = icon,
        modifier = modifier,
        showArrow = true,
        arrowRotation = submenuArrow(anchor, state.open),
        onClick = state::close,
    )
    // Every page separates its naming row from its rows; drawing it here keeps that from being a
    // line each call site has to remember.
    GlassMenuDivider()
}

/**
 * Rotation of a cascading row's arrow: it turns while the page is open and turns back while the
 * page collapses, because the page drives [GlassPopupAnchor.secondaryBackProgress].
 */
@Composable
private fun submenuArrow(anchor: GlassPopupAnchor, open: Boolean): () -> Float {
    val turn = CascadingPopupDefaults.arrowRotation(LocalLayoutDirection.current)
    val rotation by animateFloatAsState(
        targetValue = if (open) turn else 0f,
        animationSpec = CascadingPopupDefaults.arrowSpring(open),
        label = "glassSubmenuArrow",
    )
    return { rotation * (1f - anchor.secondaryBackProgress) }
}

/** The divider between two sections of one menu. */
@Composable
fun GlassMenuDivider() {
    HorizontalDivider(
        modifier =
            Modifier.padding(
                horizontal = GlassPopupDefaults.DividerPaddingHorizontal,
                vertical = GlassPopupDefaults.DividerPaddingVertical,
            ),
        thickness = GlassPopupDefaults.DividerThickness,
    )
}

/** A quiet caption above a group of [GlassMenuItem] rows, matching the MIUI menu inset. */
@Composable
fun GlassMenuSectionLabel(text: String) {
    Text(
        text = text,
        modifier =
            Modifier.padding(
                start = GlassPopupDefaults.ItemPaddingHorizontal,
                end = GlassPopupDefaults.ItemPaddingHorizontal,
                top = UiDp.dp6,
                bottom = UiDp.dp2,
            ),
        style = MiuixTheme.textStyles.footnote1,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
    )
}

/** The shared menu width, narrowed to one fixed [width]. */
fun GlassPopupSizing.fixedWidth(width: Dp): GlassPopupSizing =
    copy(minWidth = width, maxWidth = width)
