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

import androidx.compose.runtime.*
import com.github.yumeyucca.yumebox.runtime.api.Profile

@Stable
internal class ProfilesDialogState {
    val showAdd = mutableStateOf(false)
    val showSortSheet = mutableStateOf(false)
    /** Bumped when the sort sheet commits a new order, so the page can return to the top. */
    val sortCommitCount = mutableStateOf(0)
    var showDelete by mutableStateOf(false)
    var deleteTarget by mutableStateOf<Profile?>(null)
    var profileToEdit by mutableStateOf<Profile?>(null)
    var isDownloading by mutableStateOf(false)
    var importUrl by mutableStateOf<String?>(null)
    var scannedUrl by mutableStateOf<String?>(null)
}

@Composable
internal fun rememberProfilesDialogState(): ProfilesDialogState = remember { ProfilesDialogState() }
