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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

@Composable
fun AppTextFieldDialog(
    show: Boolean,
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    singleLine: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onImeAction: (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    var localTextFieldValue by
    remember(show) {
        mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length)))
    }
    // The caller's [value] is authoritative whenever it stops matching what is on screen: that covers
    // both the echo of an edit and a rewrite the caller made to it.
    LaunchedEffect(value, show) {
        if (!show) return@LaunchedEffect
        if (value != localTextFieldValue.text) {
            localTextFieldValue = TextFieldValue(text = value, selection = TextRange(value.length))
        }
    }

    AppTextFieldDialog(
        show = show,
        title = title,
        textFieldValue = localTextFieldValue,
        onTextFieldValueChange = { updatedTextFieldValue ->
            localTextFieldValue = updatedTextFieldValue
            onValueChange(updatedTextFieldValue.text)
        },
        onDismissRequest = onDismissRequest,
        onConfirm = onConfirm,
        modifier = modifier,
        summary = summary,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        onImeAction = onImeAction,
        trailingIcon = trailingIcon,
    )
}

@Composable
fun AppTextFieldDialog(
    show: Boolean,
    title: String,
    textFieldValue: TextFieldValue,
    onTextFieldValueChange: (TextFieldValue) -> Unit,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    singleLine: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onImeAction: (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    AppInputDialog(
        show = show,
        title = title,
        onDismissRequest = onDismissRequest,
        onConfirm = onConfirm,
        modifier = modifier,
        summary = summary,
    ) { dismissInput ->
        OemTextField(
            modifier = Modifier.fillMaxWidth(),
            value = textFieldValue,
            onValueChange = onTextFieldValueChange,
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            onImeAction =
                onImeAction?.let { action ->
                    {
                        dismissInput()
                        action()
                    }
                },
            trailingIcon = trailingIcon,
        )
    }
}
