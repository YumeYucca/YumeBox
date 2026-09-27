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

package com.github.yumeyucca.yumebox.presentation.util

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.github.yumeyucca.yumebox.runtime.api.Profile
import java.io.File

enum class ProfileImportType {
    Url,
    LocalFile,
    Qr,
    ;

    companion object {
        fun fromSpinnerIndex(index: Int): ProfileImportType = entries.getOrNull(index) ?: Url

        fun fromProfile(profileType: Profile.Type): ProfileImportType =
            when (profileType) {
                Profile.Type.Url -> ProfileImportType.Url
                Profile.Type.File -> ProfileImportType.LocalFile
                Profile.Type.External -> ProfileImportType.LocalFile
            }
    }
}

private val subscriptionUrlPattern =
    Regex(pattern = "^https?://\\S+$", options = setOf(RegexOption.IGNORE_CASE))

fun isSubscriptionUrl(value: String): Boolean = subscriptionUrlPattern.matches(value.trim())

fun readClipboardSubscriptionUrl(context: Context): String? {
    return runCatching {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = clipboard.primaryClip ?: return null
        if (clipData.itemCount <= 0) return null
        clipData.getItemAt(0)?.text?.toString()?.trim()
    }
        .getOrNull()
        ?.takeIf { it.isNotBlank() && isSubscriptionUrl(it) }
}

fun isYamlConfigFileName(fileName: String): Boolean =
    when (fileName.substringAfterLast(".", "").lowercase()) {
        "yaml",
        "yml" -> true

        else -> false
    }

fun profileNameFromConfigFileName(fileName: String, fallback: String): String =
    fileName.substringBeforeLast(".").ifBlank {
        fallback
    }

fun readDisplayName(context: Context, uri: Uri, fallback: String): String =
    context.contentResolver
        .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex < 0 || !cursor.moveToFirst()) {
                fallback
            } else {
                cursor.getString(nameIndex)
            }
        } ?: fallback

fun sourceFileName(source: String): String =
    source.takeIf(String::isNotEmpty)?.let { File(it).name }.orEmpty()
