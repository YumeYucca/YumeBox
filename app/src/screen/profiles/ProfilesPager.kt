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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.*
import com.github.yumeyucca.yumebox.MainActivity
import com.github.yumeyucca.yumebox.screen.home.HomeViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun ProfilesPager(mainInnerPadding: PaddingValues) {
    val profilesViewModel = koinViewModel<ProfilesViewModel>()
    val homeViewModel = koinViewModel<HomeViewModel>()
    val profiles by profilesViewModel.profiles.collectAsState()
    val isRunning by homeViewModel.isRunning.collectAsState()
    val dialogs = rememberProfilesDialogState()
    val pendingImportUrl by MainActivity.pendingImportUrl.collectAsState()
    LaunchedEffect(pendingImportUrl) {
        if (pendingImportUrl != null) {
            dialogs.addRequest = AddProfileRequest(importUrl = pendingImportUrl)
            MainActivity.clearPendingImportUrl()
        }
    }

    ProfilesPageContent(
        mainInnerPadding = mainInnerPadding,
        profiles = profiles,
        state = dialogs,
        profilesViewModel = profilesViewModel,
        homeViewModel = homeViewModel,
        isRunning = isRunning,
    )
}
