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


import android.content.Intent
import androidx.core.content.FileProvider
import com.github.yumeyucca.yumebox.App
import com.github.yumeyucca.yumebox.common.util.toast
import com.github.yumeyucca.yumebox.runtime.api.Profile
import com.github.yumeyucca.yumebox.common.util.startActivitySafely
import com.github.yumeyucca.yumebox.core.locale.YumeTxt

internal fun shareProfileFile(profile: Profile) {
    val context = App.instance
    val file = importedConfigFile(profile)
    if (!file.exists()) {
        context.toast(
            YumeTxt.ProfilesPage.ShareDialog.ImportedConfigMissing.format(file.absolutePath)
        )
        return
    }

    runCatching {
        val uri =
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
        val shareIntent =
            Intent(Intent.ACTION_SEND).apply {
                type = "application/x-yaml"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(
            Intent.createChooser(shareIntent, YumeTxt.ProfilesPage.ShareDialog.ShareFile).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
        .onFailure { error -> context.toast(error.message ?: "Share failed") }
}

internal fun shareProfileLink(profile: Profile) {
    val context = App.instance
    val url = profile.source.takeIf { profile.type == Profile.Type.Url && it.isNotBlank() }
    if (url == null) {
        context.toast(YumeTxt.ProfilesPage.ShareDialog.NoLink)
        return
    }

    val shareIntent =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    context.startActivitySafely(
        Intent.createChooser(shareIntent, YumeTxt.ProfilesPage.ShareDialog.ShareLink).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )
}
