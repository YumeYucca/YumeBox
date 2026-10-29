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
 */

package com.github.yumeyucca.yumebox.feature.editor.screen

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import com.github.yumeyucca.yumebox.presentation.theme.LocalTopBarBackdrop
import io.github.yumeyucca.lucide.Lucide
import io.github.yumeyucca.lucide.lucide.Redo2
import io.github.yumeyucca.lucide.lucide.RemoveFormatting
import io.github.yumeyucca.lucide.lucide.Undo2
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.glass.GlassPopupAnchor
import top.yukonga.miuix.kmp.glass.GlassPopupDefaults
import top.yukonga.miuix.kmp.glass.GlassPopupItem
import top.yukonga.miuix.kmp.glass.GlassPopupSizing
import top.yukonga.miuix.kmp.glass.GlassTransformPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils.Companion.PopupLayout

internal val EditorFontSizes = listOf(12f, 14f, 16f, 18f, 20f, 24f)

@Composable
internal fun EditorActionsMenu(
    show: Boolean,
    onDismiss: () -> Unit,
    anchor: GlassPopupAnchor,
    anchorContent: @Composable () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    fontSizeSp: Float,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onFormat: () -> Unit,
    onFontSizeSelected: (Float) -> Unit,
) {
    val popupVisible = remember { mutableStateOf(false) }
    val currentShow by rememberUpdatedState(show)
    LaunchedEffect(show) {
        if (show) popupVisible.value = true
    }
    val popupVisuals = GlassPopupDefaults.visuals()

    PopupLayout(
        visible = popupVisible,
        enterTransition = EnterTransition.None,
        exitTransition = ExitTransition.None,
        enableWindowDim = false,
        enableBackHandler = false,
    ) {
        Box(Modifier.fillMaxSize()) {
            var expanded by remember { mutableStateOf(false) }
            LaunchedEffect(show) { expanded = show }
            GlassTransformPopup(
                show = expanded,
                onDismissRequest = onDismiss,
                anchor = anchor,
                backdrop = LocalTopBarBackdrop.current,
                anchorContent = anchorContent,
                anchorAlpha = 0f,
                sizing = GlassPopupSizing(minWidth = 188.dp, maxWidth = 188.dp),
                visuals = popupVisuals,
                onDismissFinished = {
                    if (!currentShow) popupVisible.value = false
                },
            ) {
                GlassPopupItem(
                    text = YumeTxt.Editor.Action.Redo,
                    icon = Lucide.Redo2,
                    iconSize = 18.dp,
                    iconGap = 15.dp,
                    enabled = canRedo,
                    onClick = {
                        onRedo()
                        onDismiss()
                    },
                )
                GlassPopupItem(
                    text = YumeTxt.Editor.Action.Undo,
                    icon = Lucide.Undo2,
                    iconSize = 18.dp,
                    iconGap = 15.dp,
                    enabled = canUndo,
                    onClick = {
                        onUndo()
                        onDismiss()
                    },
                )
                GlassPopupItem(
                    text = YumeTxt.Editor.Action.Format,
                    icon = Lucide.RemoveFormatting,
                    iconSize = 18.dp,
                    iconGap = 15.dp,
                    onClick = {
                        onFormat()
                        onDismiss()
                    },
                )
                HorizontalDivider(
                    modifier =
                        Modifier.padding(
                            horizontal = GlassPopupDefaults.DividerPaddingHorizontal,
                            vertical = GlassPopupDefaults.DividerPaddingVertical,
                        ),
                    thickness = GlassPopupDefaults.DividerThickness,
                )
                Text(
                    text = YumeTxt.Editor.Action.FontSize,
                    modifier =
                        Modifier.padding(
                            start = GlassPopupDefaults.ItemPaddingHorizontal,
                            end = GlassPopupDefaults.ItemPaddingHorizontal,
                            top = 6.dp,
                            bottom = 2.dp,
                        ),
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                EditorFontSizes.forEach { size ->
                    GlassPopupItem(
                        text = "${size.toInt()} sp",
                        selected = size == fontSizeSp,
                        onClick = {
                            onFontSizeSelected(size)
                            onDismiss()
                        },
                    )
                }
            }
        }
    }
}
