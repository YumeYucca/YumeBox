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

/** One add request: the sheet opens blank (clipboard guess) or prefilled from an imported URL. */
@Immutable
internal data class AddProfileRequest(val importUrl: String? = null)

@Stable
internal class ProfilesDialogState {
    /**
     * The pending add request, or null when the sheet is closed. It is one nullable value rather than
     * a visibility flag plus a URL: a sheet that closed has nothing to prefill, and a URL that
     * survived a close would silently prefill the next open.
     */
    var addRequest by mutableStateOf<AddProfileRequest?>(null)

    val showAdd: Boolean get() = addRequest != null

    var showSortSheet by mutableStateOf(false)

    /** Bumped when the sort sheet commits a new order, so the page can return to the top. */
    var sortCommitCount by mutableStateOf(0)

    /**
     * Whether the delete dialog is still on screen. It is kept apart from [deleteTarget] because the
     * dialog must keep rendering the profile while it animates out: this flag flips first, the
     * payload is cleared once the exit animation has finished.
     */
    var showDelete by mutableStateOf(false)

    var deleteTarget by mutableStateOf<Profile?>(null)

    /**
     * The add form, owned here rather than inside the sheet: it outlives the sheet's exit animation,
     * and keeping it in one place means reopening the sheet cannot resurrect a half-typed draft.
     * Whether an import is running lives in [ProfilesViewModel.downloadProgress], not here.
     */
    val addForm = ProfileAddFormState()
}

@Composable
internal fun rememberProfilesDialogState(): ProfilesDialogState = remember { ProfilesDialogState() }
