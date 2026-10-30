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

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.github.yumeyucca.yumebox.common.util.toast
import com.github.yumeyucca.yumebox.runtime.api.Profile

@Composable
internal fun ProfilesDialogHost(
    state: ProfilesDialogState,
    profiles: List<Profile>,
    profilesViewModel: ProfilesViewModel,
) {
    val context = LocalContext.current
    val downloadProgress by profilesViewModel.downloadProgress.collectAsState()
    val uiState by profilesViewModel.uiState.collectAsState()

    // Declared before the sheet so it consumes the message that signalled `Completed` after the
    // sheet's own completion effect ran; clearing it can never cancel a finish.
    LaunchedEffect(uiState.message) {
        if (uiState.message != null) profilesViewModel.clearMessage()
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { errorMessage ->
            // Import failures only toast; do not dump the long core message into the sheet form.
            context.toast(errorMessage, Toast.LENGTH_LONG)
            profilesViewModel.clearError()
        }
    }

    ProfilesSortSheet(
        show = state.showSortSheet,
        profiles = profiles,
        onOrderCommitted = { ordered ->
            profilesViewModel.commitProfileOrder(ordered.map { it.uuid })
            state.sortCommitCount++
        },
        onDismiss = { state.showSortSheet = false },
    )

    AddProfileSheet(
        show = state.showAdd,
        form = state.addForm,
        onDismiss = { state.addRequest = null },
        importUrl = state.addRequest?.importUrl,
        onAddProfile = { name, source, type, interval, fileUri, ageSecretKey ->
            profilesViewModel.createProfile(type, name, source, interval, fileUri, ageSecretKey)
        },
        downloadProgress = downloadProgress,
        onDownloadComplete = {
            state.addRequest = null
            profilesViewModel.clearDownloadProgress()
        },
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
