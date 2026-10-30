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

package com.github.yumeyucca.yumebox.screen.home

import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import com.github.yumeyucca.yumebox.core.model.RunMode
import com.github.yumeyucca.yumebox.core.model.Traffic
import com.github.yumeyucca.yumebox.presentation.base.LoadableState
import com.github.yumeyucca.yumebox.runtime.api.Profile
import com.github.yumeyucca.yumebox.runtime.api.RuntimeOwner
import com.github.yumeyucca.yumebox.runtime.api.RuntimePhase

enum class HomeProxyControlState {
    Idle,
    Connecting,
    Running,
    Lost,
    Disconnecting;

    val canInteract: Boolean
        get() = this == Idle || this == Running
}

/** In-flight launch bookkeeping; [HomeProxyControlState] is derived from it. */
internal sealed interface StartAttempt {
    data object None : StartAttempt

    data class AwaitingPermission(val request: PendingStartRequest) : StartAttempt

    data class Starting(val request: PendingStartRequest) : StartAttempt

    data object Stopping : StartAttempt
}

/** A launch is on its way, including the VPN permission round-trip. */
internal val StartAttempt.isStartup: Boolean
    get() = this is StartAttempt.Starting || this is StartAttempt.AwaitingPermission

internal data class PendingStartRequest(
    val profileId: String,
    val mode: RunMode,
)

/** Profile repository snapshot; a `null` holder means "not loaded yet". */
internal data class HomeProfiles(val all: List<Profile>, val recommended: Profile?) {
    val hasEnabled: Boolean
        get() = all.any { it.active }
}

data class HomeScreenState(
    val controlState: HomeProxyControlState = HomeProxyControlState.Idle,
    val trafficNow: Traffic = 0L,
    val profiles: List<Profile> = emptyList(),
    val profilesLoaded: Boolean = false,
    val hasEnabledProfile: Boolean = false,
    val recommendedProfile: Profile? = null,
    val currentProfile: Profile? = null,
    val selectedServerName: String? = null,
    val selectedServerPing: Int? = null,
    val isRemoteController: Boolean = false,
    val uiMessage: String? = null,
    val uiError: String? = null,
    val runtimeStartedAt: Long? = null,
)

data class HomeUiState(
    override val isLoading: Boolean = false,
    override val message: String? = null,
    override val error: String? = null,
) : LoadableState<HomeUiState> {
    override fun withLoading(loading: Boolean): HomeUiState = copy(isLoading = loading)

    override fun withError(error: String?): HomeUiState = copy(error = error)

    override fun withMessage(message: String?): HomeUiState = copy(message = message)
}

sealed interface HomeUiEffect {
    data class ShowMessage(val message: String) : HomeUiEffect

    data class ShowError(val message: String) : HomeUiEffect
}

internal fun resolveHomeControlState(
    owner: RuntimeOwner,
    phase: RuntimePhase,
    startAttempt: StartAttempt,
): HomeProxyControlState {
    if (owner == RuntimeOwner.RemoteController && phase == RuntimePhase.Failed) {
        return HomeProxyControlState.Lost
    }
    val phaseStillActive =
        phase != RuntimePhase.Stopping && phase != RuntimePhase.Idle && phase != RuntimePhase.Failed
    if (startAttempt == StartAttempt.Stopping && phaseStillActive) {
        return HomeProxyControlState.Disconnecting
    }
    return when (phase) {
        RuntimePhase.Running -> HomeProxyControlState.Running
        RuntimePhase.Starting -> HomeProxyControlState.Connecting
        RuntimePhase.Stopping -> HomeProxyControlState.Disconnecting
        RuntimePhase.Idle,
        RuntimePhase.Failed ->
            if (startAttempt.isStartup) {
                HomeProxyControlState.Connecting
            } else {
                HomeProxyControlState.Idle
            }
    }
}

/** Turns a raw runtime failure into one short, user-facing line. */
internal fun readableStartupError(raw: String?): String {
    val message = raw?.trim().orEmpty()
    if (message.isEmpty()) {
        return YumeTxt.Util.Error.UnknownError
    }
    if (message.equals("runtime backend unavailable", ignoreCase = true)) {
        return YumeTxt.Home.Message.RuntimeUnavailable
    }
    // Prefer the first actionable line; strip huge core.log tails and stack noise.
    val firstLine =
        message
            .lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() }
            ?: message
    val cleaned =
        firstLine
            .removePrefix("java.lang.IllegalStateException:")
            .removePrefix("java.lang.IllegalArgumentException:")
            .removePrefix("java.lang.RuntimeException:")
            .trim()
    return cleaned.take(280).ifBlank { YumeTxt.Util.Error.UnknownError }
}
