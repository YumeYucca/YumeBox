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

import androidx.compose.runtime.Composable
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import com.github.yumeyucca.yumebox.presentation.component.GlassMenuDivider
import com.github.yumeyucca.yumebox.presentation.component.GlassMenuItem
import com.github.yumeyucca.yumebox.presentation.component.GlassMenuSectionLabel
import com.github.yumeyucca.yumebox.presentation.component.GlassPopupHost
import com.github.yumeyucca.yumebox.presentation.theme.LocalTopBarBackdrop
import io.github.yumeyucca.lucide.Lucide
import io.github.yumeyucca.lucide.lucide.Redo2
import io.github.yumeyucca.lucide.lucide.RemoveFormatting
import io.github.yumeyucca.lucide.lucide.Undo2
import top.yukonga.miuix.kmp.glass.GlassPopupAnchor

private val EditorFontSizes = listOf(12f, 14f, 16f, 18f, 20f, 24f)

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
    GlassPopupHost(
        show = show,
        onDismissRequest = onDismiss,
        anchor = anchor,
        anchorContent = anchorContent,
        backdrop = LocalTopBarBackdrop.current,
    ) {
        GlassMenuItem(
            text = YumeTxt.Editor.Action.Redo,
            icon = Lucide.Redo2,
            enabled = canRedo,
            onClick = {
                onRedo()
                onDismiss()
            },
        )
        GlassMenuItem(
            text = YumeTxt.Editor.Action.Undo,
            icon = Lucide.Undo2,
            enabled = canUndo,
            onClick = {
                onUndo()
                onDismiss()
            },
        )
        GlassMenuItem(
            text = YumeTxt.Editor.Action.Format,
            icon = Lucide.RemoveFormatting,
            onClick = {
                onFormat()
                onDismiss()
            },
        )
        GlassMenuDivider()
        GlassMenuSectionLabel(YumeTxt.Editor.Action.FontSize)
        EditorFontSizes.forEach { size ->
            GlassMenuItem(
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
