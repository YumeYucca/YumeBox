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


import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import java.io.File

internal fun openProfileConfigPreview(
    targetFile: File,
    missingMessage: String,
    editable: Boolean,
    onReadFailed: (String) -> Unit,
    onPreviewPrepared: (String, (suspend (String) -> Unit)?) -> Unit,
) {
    if (!targetFile.exists()) {
        onReadFailed(missingMessage)
        return
    }

    val configContent = runCatching {
        targetFile.readText()
    }
        .getOrElse {
            onReadFailed(it.message ?: "Failed to read profile")
            return
        }

    val saveCallback =
        if (editable) {
            { updatedContent: String ->
                runCatching { targetFile.writeText(updatedContent) }
                    .getOrElse {
                        throw IllegalStateException(
                            it.message ?: YumeTxt.ProfilesPage.SettingsDialog.SaveFailed,
                            it,
                        )
                    }
            }
        } else {
            null
        }

    onPreviewPrepared(configContent, saveCallback)
}
