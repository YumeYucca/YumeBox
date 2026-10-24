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
 * Copyright (c) YumeYucca 2025 - Present
 *
 */
package com.github.yumeyucca.yumebox.presentation.screen.node

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import com.github.yumeyucca.yumebox.data.model.ProxySortMode
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.ListChevronsUpDown
import com.github.yumeyucca.yumebox.presentation.icon.yume.Speed
import com.github.yumeyucca.yumebox.presentation.theme.AnimationSpecs
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Search
import top.yukonga.miuix.kmp.icon.basic.SearchCleanup
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun NodeSearchToolbar(
    query: String,
    onQueryChange: (String) -> Unit,
    sortMode: ProxySortMode,
    showMorePopup: Boolean,
    onShowMorePopupChange: (Boolean) -> Unit,
    onSortSelected: (ProxySortMode) -> Unit,
    onTestDelay: () -> Unit,
    onLocateCurrentProxy: (() -> Unit)?,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val colorScheme = MiuixTheme.colorScheme
    // 胶囊与按钮共用同一层"悬浮白"材质,叠在 Scaffold 的 surface 灰底上;
    // 遮罩颜色必须与页面底色(surface)一致,否则会变成一条突兀的色带。
    val toolbarSurface = colorScheme.background
    val pageSurface = colorScheme.surface
    var inputFocused by remember { mutableStateOf(false) }
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    val showActionIcons = !inputFocused || !imeVisible
    var searchHeight by remember { mutableStateOf(0) }
    val actionSize = with(density) { searchHeight.toDp() }
    val iconPushSpec =
        tween<IntSize>(
            durationMillis = AnimationSpecs.DURATION_FAST,
            easing = AnimationSpecs.Legacy,
        )

    fun collapseSearch() {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    LaunchedEffect(imeVisible) {
        if (!imeVisible && inputFocused) {
            focusManager.clearFocus(force = true)
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .drawWithContent {
                    drawContent()
                    // 底部雾化遮罩:与页面底色同色的渐隐,内容滚过时像从页面里浮现。
                    val fadePx = UiDp.dp24.toPx()
                    drawRect(
                        brush =
                            Brush.verticalGradient(
                                colors = listOf(pageSurface, pageSurface.copy(alpha = 0f)),
                                startY = size.height,
                                endY = size.height + fadePx,
                            ),
                        topLeft = Offset(0f, size.height),
                        size = Size(size.width, fadePx),
                    )
                },
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = UiDp.dp4, bottom = UiDp.dp8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MiuixTheme(colors = colorScheme.copy(surfaceContainerHigh = toolbarSurface)) {
                InputField(
                    query = query,
                    onQueryChange = onQueryChange,
                    onSearch = { collapseSearch() },
                    expanded = false,
                    onExpandedChange = { requesting ->
                        if (requesting) {
                            onShowMorePopupChange(false)
                        } else {
                            collapseSearch()
                        }
                    },
                    label = YumeTxt.Component.Editor.Action.Search,
                    leadingIcon = {
                        Icon(
                            imageVector = MiuixIcons.Basic.Search,
                            contentDescription = YumeTxt.Component.Editor.Action.Search,
                            modifier =
                                Modifier
                                    .size(UiDp.dp44)
                                    .padding(start = UiDp.dp16, end = UiDp.dp8),
                        )
                    },
                    trailingIcon = {
                        AnimatedVisibility(visible = query.isNotEmpty()) {
                            Icon(
                                imageVector = MiuixIcons.Basic.SearchCleanup,
                                contentDescription = YumeTxt.Component.Button.Clear,
                                modifier =
                                    Modifier
                                        .size(UiDp.dp44)
                                        .padding(start = UiDp.dp8, end = UiDp.dp16)
                                        .clickable { onQueryChange("") },
                            )
                        }
                    },
                    modifier =
                        Modifier
                            .weight(1f)
                            .shadow(
                                elevation = UiDp.dp6,
                                shape = CircleShape,
                                clip = false,
                                ambientColor = Color.Black.copy(alpha = 0.06f),
                                spotColor = Color.Black.copy(alpha = 0.12f),
                            )
                            .onSizeChanged { size ->
                                if (size.height > 0 && size.height != searchHeight) {
                                    searchHeight = size.height
                                }
                            }
                            .onFocusChanged { focusState ->
                                inputFocused = focusState.isFocused
                                if (focusState.isFocused) {
                                    onShowMorePopupChange(false)
                                }
                            },
                )
            }
            AnimatedVisibility(
                visible = showActionIcons,
                enter =
                    fadeIn(animationSpec = tween(durationMillis = 160)) +
                        expandHorizontally(animationSpec = iconPushSpec, expandFrom = Alignment.End),
                exit =
                    fadeOut(animationSpec = tween(durationMillis = 120)) +
                        shrinkHorizontally(animationSpec = iconPushSpec, shrinkTowards = Alignment.End),
            ) {
                Row(
                    modifier = Modifier.padding(start = UiDp.dp8),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(UiDp.dp8),
                ) {
                    NodeToolbarIconButton(
                        size = actionSize,
                        color = toolbarSurface,
                        onClick = {
                            onShowMorePopupChange(false)
                            onTestDelay()
                        },
                    ) {
                        Icon(
                            imageVector = Yume.Speed,
                            contentDescription = YumeTxt.Proxy.Action.Test,
                            modifier = Modifier.size(UiDp.dp22),
                        )
                    }
                    Box {
                        NodeToolbarIconButton(
                            size = actionSize,
                            color = toolbarSurface,
                            onClick = { onShowMorePopupChange(!showMorePopup) },
                        ) {
                            Icon(
                                imageVector = Yume.ListChevronsUpDown,
                                contentDescription = YumeTxt.Proxy.Action.Sort,
                                modifier = Modifier.size(UiDp.dp22),
                            )
                        }
                        NodeSortPopup(
                            show = showMorePopup,
                            onDismiss = { onShowMorePopupChange(false) },
                            sortMode = sortMode,
                            alignment = PopupPositionProvider.Align.BottomEnd,
                            onLocateCurrentProxy = onLocateCurrentProxy,
                            onSortSelected = onSortSelected,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NodeToolbarIconButton(
    size: Dp,
    color: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (size <= UiDp.dp0) return
    Box(
        modifier =
            Modifier
                .size(size)
                .shadow(
                    elevation = UiDp.dp6,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.06f),
                    spotColor = Color.Black.copy(alpha = 0.12f),
                )
                .clip(CircleShape)
                .background(color),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(
            onClick = onClick,
            backgroundColor = color,
            minHeight = size,
            minWidth = size,
        ) {
            content()
        }
    }
}
