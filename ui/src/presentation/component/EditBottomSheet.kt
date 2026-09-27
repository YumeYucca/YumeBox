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

@file:Suppress("UnusedSymbol", "FunctionName")

package com.github.yumeyucca.yumebox.presentation.component


import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.TextFieldValue
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun TextEditBottomSheet(
    show: MutableState<Boolean>,
    title: String,
    textFieldValue: MutableState<TextFieldValue>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit = { show.value = false },
    secondaryButtonText: String = YumeTxt.Component.Button.Cancel,
    onSecondaryClick: () -> Unit = onDismiss,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    fun hideInput() {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    AppActionBottomSheet(
        show = show.value,
        title = title,
        onDismissRequest = {
            hideInput()
            onDismiss()
        },
    ) {
        Column {
            OemTextField(
                value = textFieldValue.value,
                onValueChange = { textFieldValue.value = it },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(UiDp.dp16))
            Row(horizontalArrangement = Arrangement.spacedBy(UiDp.dp12)) {
                Button(
                    onClick = {
                        hideInput()
                        onSecondaryClick()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(secondaryButtonText)
                }
                Button(
                    onClick = {
                        hideInput()
                        onConfirm(textFieldValue.value.text)
                        show.value = false
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColorsPrimary(),
                ) {
                    Text(YumeTxt.Component.Button.Confirm, color = MiuixTheme.colorScheme.onPrimary)
                }
            }
            Spacer(modifier = Modifier.height(UiDp.dp16))
        }
    }
}

@Composable
fun WarningBottomSheet(
    show: MutableState<Boolean>,
    title: String,
    messages: List<String>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit = { show.value = false },
) {
    AppActionBottomSheet(show = show.value, title = title, onDismissRequest = onDismiss) {
        Column {
            messages.forEachIndexed { index, message ->
                Text(message)
                if (index < messages.lastIndex) {
                    Spacer(modifier = Modifier.height(UiDp.dp8))
                }
            }
            Spacer(modifier = Modifier.height(UiDp.dp16))
            Button(
                onClick = {
                    onConfirm()
                    show.value = false
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(YumeTxt.Component.Button.Confirm, color = MiuixTheme.colorScheme.onPrimary)
            }
            Spacer(modifier = Modifier.height(UiDp.dp16))
        }
    }
}
