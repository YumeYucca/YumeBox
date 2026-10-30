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

/** What the add/edit sheet is showing, derived once per composition instead of per-signal flags. */
internal enum class ProfileSheetPhase {
    /** The form is editable; nothing is in flight. */
    Editing,

    /** An import or update is running and still reporting progress. */
    Downloading,

    /** The work finished; the sheet holds the completed state until it closes. */
    Completed,
    ;

    /** True while the sheet shows progress instead of the form. */
    val showsProgress: Boolean get() = this != Editing
}

@Stable
internal class ProfileAddFormState {
    val importType = mutableStateOf(ProfileImportType.Url)
    val name = mutableStateOf(TextFieldValue())
    val url = mutableStateOf(TextFieldValue())
    val filePath = mutableStateOf("")
    // Shown at the right edge of the file row: a non-zero size is the "file already picked" signal.
    val fileSize = mutableStateOf(0L)
    val ageSecretKey = mutableStateOf(TextFieldValue())
    val error = mutableStateOf("")
    val stableHeightPx = mutableIntStateOf(0)

    fun reset() {
        name.value = textValueAtEnd("")
        url.value = textValueAtEnd("")
        filePath.value = ""
        fileSize.value = 0L
        ageSecretKey.value = TextFieldValue()
        error.value = ""
        // Measured per sheet: a height carried over from a previous form would size the progress view.
        stableHeightPx.intValue = 0
    }

    fun clearTypeInput() {
        when (importType.value) {
            ProfileImportType.Url -> url.value = textValueAtEnd("")
            ProfileImportType.LocalFile -> {
                filePath.value = ""
                fileSize.value = 0L
            }
            ProfileImportType.Qr -> Unit
        }
        error.value = ""
    }
}

/** An import finishes on its download progress, so that one signal decides the phase. */
internal fun profileSheetPhase(
    isDownloading: Boolean,
    downloadCompleted: Boolean,
): ProfileSheetPhase =
    when {
        !isDownloading -> ProfileSheetPhase.Editing
        downloadCompleted -> ProfileSheetPhase.Completed
        else -> ProfileSheetPhase.Downloading
    }

