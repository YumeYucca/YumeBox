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
 */

package com.github.yumeyucca.yumebox.screen.profiles

import android.net.Uri
import androidx.core.net.toUri
import com.github.yumeyucca.yumebox.presentation.util.ProfileImportType
import com.github.yumeyucca.yumebox.runtime.api.Profile
import tf.gal.yumebox.locale.YumeTxt
import java.util.*

internal typealias AddProfile = (String, String, Profile.Type, Long, Uri?, String) -> Unit

internal typealias UpdateProfile = (UUID, String, String, Long) -> Unit

internal data class ProfileDraft(
    val importType: ProfileImportType,
    val name: String,
    val url: String,
    val filePath: String,
    val ageSecretKey: String,
    val profileToEdit: Profile?,
    val isDownloading: Boolean,
)

internal class ProfileSubmissionActions(
    val hideKeyboard: () -> Unit,
    val clearError: () -> Unit,
    val startDownload: () -> Unit,
    val showError: (String) -> Unit,
    val addProfile: AddProfile,
    val updateProfile: UpdateProfile,
)

context(actions: ProfileSubmissionActions)
internal fun submitProfile(draft: ProfileDraft) {
    if (draft.importType == ProfileImportType.Qr || draft.isDownloading) return
    actions.hideKeyboard()
    if (draft.importType == ProfileImportType.Url && draft.url.isBlank()) {
        actions.showError(YumeTxt.ProfilesPage.Validation.EnterUrl)
        return
    }
    if (draft.importType == ProfileImportType.LocalFile && draft.filePath.isBlank()) {
        actions.showError(YumeTxt.ProfilesPage.Validation.SelectFile)
        return
    }

    actions.clearError()
    actions.startDownload()
    val profile = draft.profileToEdit
    if (profile != null) {
        val source = if (draft.importType == ProfileImportType.Url) draft.url else profile.source
        actions.updateProfile(profile.uuid, draft.name, source, profile.interval)
    } else if (draft.importType == ProfileImportType.Url) {
        actions.addProfile(
            draft.name.ifBlank { YumeTxt.ProfilesPage.Input.NewProfile },
            draft.url,
            Profile.Type.Url,
            0L,
            null,
            draft.ageSecretKey.trim(),
        )
    } else {
        actions.addProfile(
            draft.name.ifBlank { YumeTxt.ProfilesPage.Input.NewProfile },
            draft.filePath,
            Profile.Type.File,
            0L,
            draft.filePath.toUri(),
            draft.ageSecretKey.trim(),
        )
    }
}
