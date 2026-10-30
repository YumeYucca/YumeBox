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

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * The "leave the editor?" guard shared by every editor screen: a back press with unsaved edits asks
 * before throwing them away, otherwise it leaves immediately.
 *
 * Extracted because preview, fullscreen and the config editor each owned a copy of the same
 * dialog-state + [BackHandler] + [DiscardChangesDialog] trio and had started to drift in what they
 * considered dirty.
 */
@Composable
internal fun EditorExitGuard(
    hasUnsavedChanges: Boolean,
    onNavigateUp: () -> Unit,
    onDiscard: () -> Unit = {},
) {
    var showDiscardDialog by remember { mutableStateOf(false) }

    BackHandler {
        if (hasUnsavedChanges) {
            showDiscardDialog = true
        } else {
            onNavigateUp()
        }
    }

    DiscardChangesDialog(
        show = showDiscardDialog,
        onDismiss = { showDiscardDialog = false },
        onDiscard = {
            onDiscard()
            onNavigateUp()
        },
    )
}
