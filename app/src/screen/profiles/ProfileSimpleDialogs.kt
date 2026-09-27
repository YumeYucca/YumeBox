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

package com.github.yumeyucca.yumebox.screen.profiles


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.github.yumeyucca.yumebox.presentation.component.AppDialog
import com.github.yumeyucca.yumebox.presentation.component.AppTextFieldDialog
import com.github.yumeyucca.yumebox.presentation.component.DialogButtonRow
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.runtime.api.Profile
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.layout.DialogDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun EditProfileNameDialog(
    show: Boolean,
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var editName by
    remember(show, currentName) {
        mutableStateOf(TextFieldValue(currentName, TextRange(currentName.length)))
    }

    AppTextFieldDialog(
        show = show,
        title = YumeTxt.ProfilesPage.EditDialog.Title,
        textFieldValue = editName,
        onTextFieldValueChange = { editName = it },
        onDismissRequest = onDismiss,
        onConfirm = { onConfirm(editName.text) },
        label = YumeTxt.ProfilesPage.Input.ProfileName,
        useLabelAsPlaceholder = true,
        singleLine = true,
    )
}

@Composable
internal fun DeleteConfirmDialog(
    show: Boolean,
    profileName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onDismissFinished: (() -> Unit)? = null,
) {
    AppDialog(
        show = show,
        modifier = Modifier,
        title = YumeTxt.ProfilesPage.DeleteDialog.Title,
        titleColor = DialogDefaults.titleColor(),
        summary = YumeTxt.ProfilesPage.DeleteDialog.Message.format(profileName),
        summaryColor = DialogDefaults.summaryColor(),
        backgroundColor = DialogDefaults.backgroundColor(),
        enableWindowDim = true,
        onDismissRequest = onDismiss,
        onDismissFinished = onDismissFinished,
        outsideMargin = DialogDefaults.outsideMargin,
        insideMargin = DialogDefaults.insideMargin,
        defaultWindowInsetsPadding = true,
        content = {
            DialogButtonRow(
                onCancel = onDismiss,
                onConfirm = onConfirm,
                cancelText = YumeTxt.ProfilesPage.Button.Cancel,
                confirmText = YumeTxt.ProfilesPage.DeleteDialog.Confirm,
            )
        },
    )
}

@Composable
internal fun ShareOptionsDialog(
    show: Boolean,
    profile: Profile,
    onDismiss: () -> Unit,
    onDismissFinished: (() -> Unit)? = null,
    onShareFile: (Profile) -> Unit,
    onShareLink: (Profile) -> Unit,
) {
    val spacing = AppTheme.spacing

    AppDialog(
        show = show,
        modifier = Modifier,
        title = YumeTxt.ProfilesPage.ShareDialog.Title,
        titleColor = DialogDefaults.titleColor(),
        summary = null,
        summaryColor = DialogDefaults.summaryColor(),
        backgroundColor = DialogDefaults.backgroundColor(),
        enableWindowDim = true,
        onDismissRequest = onDismiss,
        onDismissFinished = onDismissFinished,
        outsideMargin = DialogDefaults.outsideMargin,
        insideMargin = DialogDefaults.insideMargin,
        defaultWindowInsetsPadding = true,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.space12)) {
                if (profile.type == Profile.Type.Url) {
                    Button(
                        onClick = { onShareLink(profile) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColorsPrimary(),
                    ) {
                        Text(
                            YumeTxt.ProfilesPage.ShareDialog.ShareLink,
                            color = MiuixTheme.colorScheme.onPrimary,
                        )
                    }
                }
                Button(
                    onClick = { onShareFile(profile) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(),
                ) {
                    Text(YumeTxt.ProfilesPage.ShareDialog.ShareFile)
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(),
                ) {
                    Text(YumeTxt.ProfilesPage.Button.Cancel)
                }
            }
        },
    )
}
