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


import android.annotation.SuppressLint
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.*
import com.github.yumeyucca.yumebox.MainActivity
import com.github.yumeyucca.yumebox.presentation.component.WindowLayoutMode
import com.github.yumeyucca.yumebox.presentation.viewmodel.OverrideConfigViewModel
import com.github.yumeyucca.yumebox.screen.home.HomeViewModel
import org.koin.androidx.compose.koinViewModel

@SuppressLint("UseKtx")
private data class ProfilesPagerUi(
    val profiles: List<com.github.yumeyucca.yumebox.runtime.api.Profile>,
    val isRunning: Boolean,
    val builtInConfigs: List<com.github.yumeyucca.yumebox.data.model.OverrideConfig>,
    val userConfigs: List<com.github.yumeyucca.yumebox.data.model.OverrideConfig>,
)

@Composable
private fun rememberProfilesPagerUi(
    profilesViewModel: ProfilesViewModel,
    homeViewModel: HomeViewModel,
    overrideConfigViewModel: OverrideConfigViewModel,
): ProfilesPagerUi {
    val profiles by profilesViewModel.profiles.collectAsState()
    val isRunning by homeViewModel.isRunning.collectAsState()
    val builtInConfigs by overrideConfigViewModel.builtInConfigs.collectAsState()
    val userConfigs by overrideConfigViewModel.userConfigs.collectAsState()
    return remember(profiles, isRunning, builtInConfigs, userConfigs) {
        ProfilesPagerUi(
            profiles = profiles,
            isRunning = isRunning,
            builtInConfigs = builtInConfigs,
            userConfigs = userConfigs,
        )
    }
}

@Composable
fun ProfilesPager(
    mainInnerPadding: PaddingValues,
    windowLayoutMode: WindowLayoutMode = WindowLayoutMode.Compact,
) {
    val profilesViewModel = koinViewModel<ProfilesViewModel>()
    val homeViewModel = koinViewModel<HomeViewModel>()
    val overrideConfigViewModel = koinViewModel<OverrideConfigViewModel>()
    val screen = rememberProfilesPagerUi(profilesViewModel, homeViewModel, overrideConfigViewModel)
    val dialogs = rememberProfilesDialogState()
    val pendingImportUrl by MainActivity.pendingImportUrl.collectAsState()
    LaunchedEffect(pendingImportUrl) {
        if (pendingImportUrl != null) {
            dialogs.importUrl = pendingImportUrl
            dialogs.profileToEdit = null
            dialogs.showAdd.value = true
            MainActivity.clearPendingImportUrl()
        }
    }

    ProfilesPageHost(
        mainInnerPadding = mainInnerPadding,
        profiles = screen.profiles,
        state = dialogs,
        profilesViewModel = profilesViewModel,
        homeViewModel = homeViewModel,
        isRunning = screen.isRunning,
        windowLayoutMode = windowLayoutMode,
        sheetHost = {
            ProfilesDialogHost(
                state = dialogs,
                profiles = screen.profiles,
                profilesViewModel = profilesViewModel,
                homeViewModel = homeViewModel,
                builtInConfigs = screen.builtInConfigs,
                userConfigs = screen.userConfigs,
                refreshOverrides = overrideConfigViewModel::refreshAndAwait,
                isRunning = screen.isRunning,
            )
        },
    )
}
