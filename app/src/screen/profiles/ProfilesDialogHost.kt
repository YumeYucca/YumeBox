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


import androidx.compose.runtime.Composable
import com.github.yumeyucca.yumebox.runtime.api.Profile
import com.github.yumeyucca.yumebox.runtime.client.ProfilePatch

@Composable
internal fun ProfilesDialogHost(
    state: ProfilesDialogState,
    profiles: List<Profile>,
    profilesViewModel: ProfilesViewModel,
) {
    ProfilesSortSheet(
        show = state.showSortSheet.value,
        profiles = profiles,
        onOrderCommitted = { ordered ->
            profilesViewModel.commitProfileOrder(ordered.map { it.uuid })
            state.sortCommitCount.value++
        },
        onDismiss = { state.showSortSheet.value = false },
    )

    AddProfileSheet(
        show = state.showAdd,
        profileToEdit = state.profileToEdit,
        importUrl = state.importUrl ?: state.scannedUrl,
        onAddProfile = { name, source, type, interval, fileUri, ageSecretKey ->
            profilesViewModel.createProfile(type, name, source, interval, fileUri, ageSecretKey)
        },
        onUpdateProfile = { uuid, name, source, interval ->
            profilesViewModel.patchProfile(
                uuid,
                ProfilePatch(name = name, source = source, interval = interval),
            )
        },
        onDownloadComplete = {
            state.isDownloading = false
            state.showAdd.value = false
            state.importUrl = null
            state.scannedUrl = null
            profilesViewModel.clearDownloadProgress()
        },
        profilesViewModel = profilesViewModel,
    )

    ProfileDeleteDialog(state, profilesViewModel)
}

@Composable
private fun ProfileDeleteDialog(
    state: ProfilesDialogState,
    profilesViewModel: ProfilesViewModel,
) {
    val profile = state.deleteTarget ?: return
    DeleteConfirmDialog(
        show = state.showDelete,
        profileName = profile.name,
        onConfirm = {
            state.showDelete = false
            profilesViewModel.deleteProfile(profile.uuid)
        },
        onDismiss = { state.showDelete = false },
        onDismissFinished = { state.deleteTarget = null },
    )
}
