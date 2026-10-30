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

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import com.github.yumeyucca.yumebox.common.util.ByteFormatter
import com.github.yumeyucca.yumebox.runtime.api.Profile
import com.github.yumeyucca.yumebox.runtime.client.ProfilePatch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/** The config file picked as this profile's new source, with the size the file row shows. */
@Immutable
internal data class PickedConfigFile(
    val uri: Uri,
    val sizeBytes: Long,
)

/**
 * The edit-in-progress copy of one profile: the fields the 编辑配置 page renders, the two loads that
 * fill them, and the patch a save sends.
 *
 * The page never assigns these fields — every write goes through a named operation ([seedFrom],
 * [setReplacementFile], [beginSave], [endSave]) so a save cannot be half-applied and a list refresh
 * cannot overwrite edits.
 */
@Stable
internal class ProfileEditDraft internal constructor() {
    var name by mutableStateOf(TextFieldValue())
        private set

    var source by mutableStateOf(TextFieldValue())
        private set

    var ageSecretKey by mutableStateOf(TextFieldValue())
        private set

    /** True once the key was typed in; the patch only carries a key the user actually touched. */
    var ageSecretEdited by mutableStateOf(false)
        private set

    var ageSecretVisible by mutableStateOf(false)
        private set

    var isSaving by mutableStateOf(false)
        private set

    var replacementFile by mutableStateOf<PickedConfigFile?>(null)
        private set

    /** Length of the imported config file; null until it has been read, or when there is none. */
    var configFileSize by mutableStateOf<Long?>(null)
        private set

    /** The picked file wins over the stored one; 0 means "unknown", which the row renders as blank. */
    val fileRowSizeText: String
        get() = profileFileSizeText(replacementFile?.sizeBytes ?: configFileSize ?: 0L)

    /** The stored config file's size, with the page's empty-state dash while it is unknown. */
    val configFileSizeText: String
        get() = configFileSize?.let(ByteFormatter::format) ?: "—"

    fun onNameChange(value: TextFieldValue) {
        name = value
    }

    fun onSourceChange(value: TextFieldValue) {
        source = value
    }

    fun onAgeSecretKeyChange(value: TextFieldValue) {
        ageSecretKey = value
        ageSecretEdited = true
    }

    fun onAgeSecretVisibilityChange(visible: Boolean) {
        ageSecretVisible = visible
    }

    /** Copies the stored profile into the fields; only a URL profile has an editable source. */
    fun seedFrom(profile: Profile) {
        name = textValueAtEnd(profile.name)
        if (profile.type == Profile.Type.Url) {
            source = textValueAtEnd(profile.source)
        }
    }

    fun setReplacementFile(uri: Uri, sizeBytes: Long) {
        replacementFile = PickedConfigFile(uri = uri, sizeBytes = sizeBytes)
    }

    /** Closes the save gate; [endSave] always reopens it, whatever the patch did. */
    fun beginSave() {
        isSaving = true
    }

    fun endSave() {
        isSaving = false
    }

    /**
     * Reads the imported config file's length off the main thread. A profile that is not there (yet,
     * or any more) clears the size instead of leaving the previous profile's number on screen.
     */
    suspend fun loadConfigFileSize(profile: Profile?) {
        if (profile == null) {
            configFileSize = null
            return
        }
        configFileSize =
            withContext(Dispatchers.IO) {
                runCatching {
                    importedConfigFile(profile).takeIf { it.exists() }?.length()
                }.getOrElse { error ->
                    if (error is CancellationException) throw error
                    Timber.w(error, "Could not read the config file size of %s", profile.uuid)
                    null
                }
            }
    }

    /**
     * The patch to send, or null when the form holds nothing worth persisting: an empty name or
     * source, or no edit at all compared with [target].
     */
    fun patchFor(target: Profile): ProfilePatch? {
        val trimmedName = name.text.trim()
        val trimmedSource = source.text.trim()
        val picked = replacementFile
        val targetSource =
            when {
                target.type == Profile.Type.Url && trimmedSource.isNotEmpty() -> trimmedSource
                target.type == Profile.Type.File && picked != null -> picked.uri.toString()
                else -> target.source
            }
        val hasMetaChanges =
            trimmedName != target.name ||
                targetSource != target.source ||
                ageSecretEdited
        if (trimmedName.isEmpty() || targetSource.isEmpty() || !hasMetaChanges) return null
        return ProfilePatch(
            name = trimmedName,
            source = targetSource,
            interval = target.interval,
            updateAgeSecretKey = ageSecretEdited,
            ageSecretKey = if (ageSecretEdited) ageSecretKey.text.trim() else null,
        )
    }
}

/**
 * Remembers the [ProfileEditDraft] of [profileId] and keeps it fed from [profile].
 *
 * Both loads keep the keys the page used to hold inline: the seed runs once per profile id, so a
 * list refresh emitting an equal profile cannot wipe edits in progress, while the config-file size
 * also re-reads after a re-import ([Profile.updatedAt]).
 */
@Composable
internal fun rememberProfileEditDraft(profileId: String, profile: Profile?): ProfileEditDraft {
    val draft = remember(profileId) { ProfileEditDraft() }
    LaunchedEffect(profile?.uuid) {
        profile?.let(draft::seedFrom)
    }
    LaunchedEffect(profile?.uuid, profile?.updatedAt) {
        draft.loadConfigFileSize(profile)
    }
    return draft
}
