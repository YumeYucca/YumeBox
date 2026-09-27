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

import com.github.yumeyucca.yumebox.core.model.FetchStatus
import com.github.yumeyucca.yumebox.core.presentation.LoadableState
import com.github.yumeyucca.yumebox.core.locale.YumeTxt

sealed interface ProfilesUiEffect {
    data class ShowMessage(val message: String) : ProfilesUiEffect

    data class ShowError(val message: String) : ProfilesUiEffect
}

data class ProfilesUiState(
    override val isLoading: Boolean = false,
    override val error: String? = null,
    override val message: String? = null,
) : LoadableState<ProfilesUiState> {
    override fun withLoading(loading: Boolean): ProfilesUiState = copy(isLoading = loading)

    override fun withError(error: String?): ProfilesUiState = copy(error = error)

    override fun withMessage(message: String?): ProfilesUiState = copy(message = message)
}

data class DownloadProgress(
    val percent: Int?,
    val message: String,
    val itemProgress: String? = null,
    val isCompleted: Boolean = false,
)

internal fun FetchStatus.toDownloadProgress(): DownloadProgress {
    val percent = if (max > 0) ((progress * 100) / max).coerceIn(0, 100) else null
    val detail = args.firstOrNull().orEmpty().trim()
    val message =
        when (action) {
            FetchStatus.Action.FetchConfiguration ->
                if (percent == null || percent <= 5) {
                    YumeTxt.ProfilesVM.Progress.Preparing
                } else {
                    detail.ifBlank { YumeTxt.ProfilesPage.Progress.Downloading }
                }

            FetchStatus.Action.FetchProviders ->
                detail.ifBlank { YumeTxt.ProfilesPage.Progress.Downloading }

            FetchStatus.Action.SubscriptionInfo -> ""
            FetchStatus.Action.Verifying -> detail.ifBlank { YumeTxt.ProfilesVM.Progress.Verifying }
        }
    val itemProgress =
        if (action == FetchStatus.Action.FetchProviders && progress > 0 && max > 0) {
            "${progress.coerceAtMost(max)} / $max"
        } else {
            null
        }

    return DownloadProgress(percent = percent, message = message, itemProgress = itemProgress)
}
