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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun AppDialogColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(UiDp.dp16),
        content = content,
    )
}

@Composable
internal fun AppDialogMessage(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        modifier = modifier,
        style = MiuixTheme.textStyles.body1,
        color = MiuixTheme.colorScheme.onSurface,
    )
}

/**
 * Returns a stable "dismiss the keyboard" action.
 *
 * Dialogs that host a text field must drop focus *and* hide the IME before they leave
 * composition, otherwise the keyboard stays up over the next screen.
 */
@Composable
internal fun rememberInputHider(): () -> Unit {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    return remember(focusManager, keyboardController) {
        {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }
}

/**
 * The shell every input dialog shares: the [AppDialog] chrome, the caller's fields, the validation
 * message and the cancel/confirm row.
 *
 * It exists so the keyboard dismissal cannot drift: every way out of the dialog (cancel, confirm,
 * scrim tap, IME action) has to clear focus and hide the keyboard before the fields leave
 * composition, otherwise the keyboard stays up over the next screen.
 *
 * @param content the dialog's own fields; it receives [rememberInputHider]'s action so an IME
 *   action can dismiss the keyboard the same way the buttons do.
 */
@Composable
internal fun AppInputDialog(
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
    onDismissFinished: (() -> Unit)? = null,
    content: @Composable (dismissInput: () -> Unit) -> Unit,
) {
    val hideInput = rememberInputHider()

    AppDialog(
        show = show,
        modifier = modifier,
        title = title,
        summary = summary,
        onDismissRequest = {
            hideInput()
            onDismissRequest()
        },
        onDismissFinished = onDismissFinished,
    ) {
        AppDialogColumn {
            content(hideInput)
            error
                ?.takeIf { it.isNotBlank() }
                ?.let { message ->
                    Text(
                        text = message,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.error,
                    )
                }
            DialogButtonRow(
                onCancel = {
                    hideInput()
                    onDismissRequest()
                },
                onConfirm = {
                    hideInput()
                    onConfirm()
                },
                cancelText = cancelText,
                confirmText = confirmText,
                confirmEnabled = confirmEnabled,
            )
        }
    }
}
