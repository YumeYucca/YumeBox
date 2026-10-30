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

package com.github.yumeyucca.yumebox.feature.editor.component

import androidx.compose.runtime.Composable
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import com.github.yumeyucca.yumebox.presentation.component.AppDialog
import com.github.yumeyucca.yumebox.presentation.component.DialogButtonRow

/**
 * The one "discard unsaved edits?" confirmation every editor surface shows, so the wording and the
 * dismiss/discard split cannot drift between the preview, the config editor and fullscreen mode.
 */
@Composable
fun DiscardChangesDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onDiscard: () -> Unit,
) {
    AppDialog(
        show = show,
        title = YumeTxt.Editor.Discard.Title,
        summary = YumeTxt.Editor.Discard.Summary,
        onDismissRequest = onDismiss,
    ) {
        DialogButtonRow(
            onCancel = onDismiss,
            onConfirm = {
                onDismiss()
                onDiscard()
            },
            cancelText = YumeTxt.Component.Button.Cancel,
            confirmText = YumeTxt.Editor.Discard.Confirm,
        )
    }
}
