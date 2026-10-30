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

package com.github.yumeyucca.yumebox.feature.editor.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import com.github.yumeyucca.yumebox.presentation.component.GlassMenuItem
import com.github.yumeyucca.yumebox.presentation.component.GlassPopupMenuSizing
import com.github.yumeyucca.yumebox.presentation.component.GlassPopupHost
import com.github.yumeyucca.yumebox.presentation.component.fixedWidth
import com.github.yumeyucca.yumebox.presentation.theme.LocalTopBarBackdrop
import io.github.yumeyucca.lucide.Lucide
import io.github.yumeyucca.lucide.lucide.ClipboardPaste
import io.github.yumeyucca.lucide.lucide.Copy
import io.github.yumeyucca.lucide.lucide.Scissors
import io.github.yumeyucca.lucide.lucide.TextCursor
import io.github.rosemoe.sora.widget.CodeEditor
import top.yukonga.miuix.kmp.glass.glassPopupAnchor
import top.yukonga.miuix.kmp.glass.glassPopupAnchorContent
import top.yukonga.miuix.kmp.glass.rememberGlassPopupAnchor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal enum class SelectionAction {
    Cut,
    Copy,
    Paste,
    SelectAll,
}

internal data class SelectionMenuState(
    val editor: CodeEditor,
    val anchorX: Int,
    val anchorY: Int,
    val canCut: Boolean,
    val canPaste: Boolean,
    val canSelectAll: Boolean,
)

internal class SelectionMenuController {
    var state by mutableStateOf<SelectionMenuState?>(null)
        private set

    var isVisible by mutableStateOf(false)
        private set

    private var suppressUntil = 0L

    fun update(next: SelectionMenuState?) {
        if (next == null) {
            dismiss()
            return
        }
        if (System.currentTimeMillis() < suppressUntil) return
        state = next
        isVisible = true
    }

    fun dismiss() {
        isVisible = false
    }

    fun finishDismiss() {
        if (!isVisible) state = null
    }

    fun clear() {
        isVisible = false
        state = null
    }

    fun dismissForAction() {
        suppressUntil = System.currentTimeMillis() + 350L
        dismiss()
    }
}

internal fun buildSelectionMenuState(editor: CodeEditor): SelectionMenuState? {
    val cursor = editor.getCursor()
    if (!cursor.isSelected()) return null

    val rowHeight = editor.getRowHeight()
    val leftX = editor.getCharOffsetX(cursor.getLeftLine(), cursor.getLeftColumn())
    val rightX = editor.getCharOffsetX(cursor.getRightLine(), cursor.getRightColumn())
    val leftY = editor.getCharOffsetY(cursor.getLeftLine(), cursor.getLeftColumn())
    val rightY = editor.getCharOffsetY(cursor.getRightLine(), cursor.getRightColumn())
    val selectionTop = min(leftY, rightY) - rowHeight
    val selectionBottom = max(leftY, rightY)
    val canCut = editor.isEditable
    val canPaste = editor.isEditable && editor.hasClip()
    val canSelectAll = editor.getText().toString().isNotEmpty()
    // Copy is always offered; the other rows depend on the selection.
    val actionCount = 1 + listOf(canCut, canPaste, canSelectAll).count { it }
    val topGuard = editor.getDpUnit() * (actionCount * 48f + 24f)
    val anchorY =
        if (selectionTop > topGuard) {
            selectionTop - rowHeight / 2f
        } else {
            selectionBottom + rowHeight / 2f
        }

    return SelectionMenuState(
        editor = editor,
        anchorX =
            ((leftX + rightX) / 2f)
                .coerceIn(0f, editor.getWidth().toFloat())
                .roundToInt(),
        anchorY = anchorY.coerceIn(0f, editor.getHeight().toFloat()).roundToInt(),
        canCut = canCut,
        canPaste = canPaste,
        canSelectAll = canSelectAll,
    )
}

internal fun CodeEditor.performSelectionAction(action: SelectionAction) {
    when (action) {
        SelectionAction.Cut -> if (getCursor().isSelected()) cutText()
        SelectionAction.Copy -> {
            copyText()
            setSelection(getCursor().getRightLine(), getCursor().getRightColumn())
        }
        SelectionAction.Paste -> {
            pasteText()
            setSelection(getCursor().getRightLine(), getCursor().getRightColumn())
        }
        SelectionAction.SelectAll -> selectAll()
    }
}

@Composable
internal fun TextSelectionActionsMenu(
    state: SelectionMenuState,
    show: Boolean,
    onDismiss: () -> Unit,
    onDismissFinished: () -> Unit,
    onAction: (SelectionAction) -> Unit,
) {
    val anchor = rememberGlassPopupAnchor()

    Box(Modifier.fillMaxSize()) {
        Box(
            modifier =
                Modifier
                    .offset { IntOffset(state.anchorX, state.anchorY) }
                    .size(1.dp)
                    .glassPopupAnchor(
                        anchor = anchor,
                        cornerRadius = 1.dp,
                        floating = true,
                    )
                    .glassPopupAnchorContent(anchor)
        )

        GlassPopupHost(
            show = show,
            onDismissRequest = onDismiss,
            anchor = anchor,
            anchorContent = { Box(Modifier.size(1.dp)) },
            backdrop = LocalTopBarBackdrop.current,
            sizing = GlassPopupMenuSizing.fixedWidth(150.dp),
            dismissOnBack = true,
            onDismissFinished = onDismissFinished,
        ) {
            if (state.canCut) {
                GlassMenuItem(
                    text = YumeTxt.Editor.Action.Cut,
                    icon = Lucide.Scissors,
                    onClick = { onAction(SelectionAction.Cut) },
                )
            }
            GlassMenuItem(
                text = YumeTxt.Editor.Action.Copy,
                icon = Lucide.Copy,
                onClick = { onAction(SelectionAction.Copy) },
            )
            if (state.canPaste) {
                GlassMenuItem(
                    text = YumeTxt.Editor.Action.Paste,
                    icon = Lucide.ClipboardPaste,
                    onClick = { onAction(SelectionAction.Paste) },
                )
            }
            if (state.canSelectAll) {
                GlassMenuItem(
                    text = YumeTxt.Editor.Action.SelectAll,
                    icon = Lucide.TextCursor,
                    onClick = { onAction(SelectionAction.SelectAll) },
                )
            }
        }
    }
}
