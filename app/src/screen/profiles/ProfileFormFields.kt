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

package com.github.yumeyucca.yumebox.screen.profiles

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.github.yumeyucca.yumebox.common.util.ByteFormatter
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import com.github.yumeyucca.yumebox.presentation.component.AppTextFieldRow
import com.github.yumeyucca.yumebox.presentation.component.AppTextFieldRowIcon
import com.github.yumeyucca.yumebox.presentation.component.PreferenceArrowItem
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.Eye
import com.github.yumeyucca.yumebox.runtime.api.Profile
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun ProfileNameField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    enabled: Boolean = true,
    hint: String? = null,
) {
    AppTextFieldRow(
        label = YumeTxt.ProfilesPage.Input.ProfileName,
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        hint = hint,
    )
}

@Composable
internal fun ProfileSourceField(
    sourceType: Profile.Type?,
    urlValue: TextFieldValue,
    onUrlChange: (TextFieldValue) -> Unit,
    fileSizeText: String,
    onPickFile: () -> Unit,
    enabled: Boolean = true,
    hint: String? = null,
) {
    when (sourceType) {
        Profile.Type.Url ->
            AppTextFieldRow(
                label = YumeTxt.ProfilesPage.Input.SubscriptionUrl,
                value = urlValue,
                onValueChange = onUrlChange,
                enabled = enabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                hint = hint,
            )
        Profile.Type.File ->
            PreferenceArrowItem(
                title = YumeTxt.ProfilesPage.Input.SelectFile,
                enabled = enabled,
                endActions = {
                    if (fileSizeText.isNotEmpty()) {
                        Text(
                            text = fileSizeText,
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            maxLines = 1,
                        )
                    }
                },
                onClick = onPickFile,
            )
        // External profiles carry their own source: no URL row, no file picker.
        Profile.Type.External, null -> Unit
    }
}

@Composable
internal fun ProfileAgeSecretField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    visible: Boolean,
    onVisibilityChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    hint: String? = null,
) {
    AppTextFieldRow(
        label = YumeTxt.ProfilesPage.Input.AgeSecretKey,
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        hint = hint,
        keyboardOptions =
            KeyboardOptions(
                keyboardType = if (visible) KeyboardType.Text else KeyboardType.Password,
            ),
        trailingIcon = {
            AppTextFieldRowIcon(
                icon = Yume.Eye,
                contentDescription = YumeTxt.ProfilesPage.Input.AgeSecretKey,
                tint = if (visible) MiuixTheme.colorScheme.primary else Color.Unspecified,
                onClick = { onVisibilityChange(!visible) },
            )
        },
    )
}

// 0 means "size unknown", which the file row renders as no text at all.
internal fun profileFileSizeText(bytes: Long): String = if (bytes > 0L) ByteFormatter.format(bytes) else ""
