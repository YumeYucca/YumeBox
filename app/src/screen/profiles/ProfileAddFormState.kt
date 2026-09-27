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

package com.github.yumeyucca.yumebox.screen.profiles

import androidx.compose.runtime.*
import androidx.compose.ui.text.input.TextFieldValue
import com.github.yumeyucca.yumebox.presentation.util.ProfileImportType

@Stable
internal class ProfileAddFormState {
    val importType = mutableStateOf(ProfileImportType.Url)
    val name = mutableStateOf(TextFieldValue())
    val url = mutableStateOf(TextFieldValue())
    val filePath = mutableStateOf("")
    val fileName = mutableStateOf(TextFieldValue())
    val ageSecretKey = mutableStateOf(TextFieldValue())
    val error = mutableStateOf("")
    val isDownloading = mutableStateOf(false)
    val hasShownComplete = mutableStateOf(false)
    val stableHeightPx = mutableIntStateOf(0)

    fun reset() {
        name.value = textValueAtEnd("")
        url.value = textValueAtEnd("")
        filePath.value = ""
        fileName.value = textValueAtEnd("")
        ageSecretKey.value = TextFieldValue()
        error.value = ""
        isDownloading.value = false
        hasShownComplete.value = false
    }

    fun clearTypeInput() {
        when (importType.value) {
            ProfileImportType.Url -> url.value = textValueAtEnd("")
            ProfileImportType.LocalFile -> {
                filePath.value = ""
                fileName.value = textValueAtEnd("")
            }
            ProfileImportType.Qr -> Unit
        }
        error.value = ""
    }
}

@Composable
internal fun rememberProfileAddFormState(): ProfileAddFormState = remember { ProfileAddFormState() }
