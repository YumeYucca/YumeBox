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

package com.github.yumeyucca.yumebox.runtime.client

import android.content.Context
import android.content.Intent
import com.github.yumeyucca.yumebox.core.data.RepositoryUtils.safeApiCall
import com.github.yumeyucca.yumebox.runtime.api.*
import com.github.yumeyucca.yumebox.runtime.client.access.RuntimeAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.*

/** Profile CRUD via `RuntimeAccess` / `ProfileApi`. */
class ProfilesRepository(private val context: Context) {
    private val appContext = context.appContextOrSelf

    suspend fun createProfile(
        type: Profile.Type,
        name: String,
        source: String = "",
        ageSecretKey: String? = null,
    ): UUID =
        apiCall("createProfile") {
            Timber.d("Creating profile: type=$type, name=$name")
            RuntimeAccess.connect(context)
            RuntimeAccess.profile().create(type, name, source, ageSecretKey)
        }

    suspend fun cloneProfile(uuid: UUID): UUID =
        apiCall("cloneProfile") {
            Timber.d("Cloning profile: uuid=$uuid")
            RuntimeAccess.connect(context)
            RuntimeAccess.profile().clone(uuid)
        }

    suspend fun deleteProfile(uuid: UUID) {
        apiCall("deleteProfile") {
            Timber.d("Deleting profile: uuid=$uuid")
            RuntimeAccess.connect(context)
            RuntimeAccess.profile().delete(uuid)
        }
    }

    suspend fun queryAllProfiles(): List<Profile> =
        apiCall("queryAllProfiles") {
            RuntimeAccess.connect(context)
            RuntimeAccess.profile().queryAll()
        }

    suspend fun queryActiveProfile(): Profile? =
        apiCall("queryActiveProfile") {
            RuntimeAccess.connect(context)
            RuntimeAccess.profile().queryActive()
        }

    suspend fun queryProfileByUUID(uuid: UUID): Profile? =
        apiCall("queryProfileByUUID") {
            RuntimeAccess.connect(context)
            RuntimeAccess.profile().queryByUUID(uuid)
        }

    suspend fun setActiveProfile(uuid: UUID) {
        apiCall("setActiveProfile") {
            val startedAt = System.currentTimeMillis()
            Timber.d("Setting active profile: uuid=$uuid")
            RuntimeAccess.connect(context)

            val profile =
                RuntimeAccess.profile().queryByUUID(uuid)
                    ?: throw IllegalArgumentException("Profile not found: $uuid")

            RuntimeAccess.profile().setActive(profile)

            notifyRuntimeOverrideChanged()

            Timber.d(
                "Active profile applied: uuid=$uuid cost=${System.currentTimeMillis() - startedAt}ms"
            )
        }
    }

    suspend fun clearActiveProfile(profile: Profile) {
        apiCall("clearActiveProfile") {
            Timber.d("Clearing active profile: uuid=${profile.uuid}")
            RuntimeAccess.connect(context)
            RuntimeAccess.profile().clearActive(profile)
            notifyRuntimeOverrideChanged()
        }
    }

    suspend fun reorderProfiles(uuids: List<UUID>) {
        apiCall("reorderProfiles") {
            Timber.d("Reordering profiles: count=${uuids.size}")
            RuntimeAccess.connect(context)
            RuntimeAccess.profile().reorder(uuids)
        }
    }

    suspend fun updateProfile(
        uuid: UUID,
        callback: FetchObserver? = null,
    ): ProfileUpdateReport =
        apiCall("updateProfile") {
            Timber.d("Updating profile: uuid=$uuid")
            RuntimeAccess.connect(context)
            RuntimeAccess.profile().update(uuid, callback)
        }

    suspend fun patchProfile(uuid: UUID, patch: ProfilePatch) {
        apiCall("patchProfile") {
            Timber.d("Patching profile: uuid=$uuid")
            RuntimeAccess.connect(context)
            RuntimeAccess.profile()
                .patch(
                    uuid = uuid,
                    name = patch.name,
                    source = patch.source,
                    interval = patch.interval,
                    updateAgeSecretKey = patch.updateAgeSecretKey,
                    ageSecretKey = patch.ageSecretKey,
                )
        }
    }

    /** Every call here is binder IPC: keep it off the caller's dispatcher, then surface the failure. */
    private suspend fun <T> apiCall(operation: String, block: suspend () -> T): T =
        withContext(Dispatchers.IO) { safeApiCall(TAG, operation, block) }.getOrThrow()

    private fun notifyRuntimeOverrideChanged() {
        appContext.sendBroadcast(
            Intent(Intents.actionOverrideChanged(appContext.packageName))
                .setPackage(appContext.packageName)
        )
    }

    companion object {
        private const val TAG = "ProfilesRepository"
    }
}
