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

package com.github.yumeyucca.yumebox.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.core.locale.YumeTxt

@Composable
fun AppFormDialog(
    show: Boolean,
    title: String,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    error: String? = null,
    confirmEnabled: Boolean = true,
    cancelText: String = YumeTxt.Component.Button.Cancel,
    confirmText: String = YumeTxt.Component.Button.Confirm,
    scrollable: Boolean = true,
    onDismissFinished: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    AppInputDialog(
        show = show,
        title = title,
        onDismissRequest = onDismissRequest,
        onConfirm = onConfirm,
        modifier = modifier,
        summary = summary,
        error = error,
        confirmEnabled = confirmEnabled,
        cancelText = cancelText,
        confirmText = confirmText,
        onDismissFinished = onDismissFinished,
    ) {
        // A form can be taller than the screen, so its fields scroll while the button row stays put.
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .let { base ->
                        if (scrollable) {
                            base.heightIn(max = UiDp.dp420).verticalScroll(rememberScrollState())
                        } else {
                            base
                        }
                    },
            verticalArrangement = Arrangement.spacedBy(UiDp.dp12),
        ) {
            content()
        }
    }
}
