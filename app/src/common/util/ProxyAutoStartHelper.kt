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

@file:Suppress("SortModifiers")

package com.github.yumeyucca.yumebox.common.util

import android.content.Context
import android.net.VpnService
import com.github.yumeyucca.yumebox.core.model.RunMode
import com.github.yumeyucca.yumebox.core.util.AutoStartSessionGate
import com.github.yumeyucca.yumebox.runtime.api.Profile
import com.github.yumeyucca.yumebox.runtime.service.RuntimeCoordinator
import com.github.yumeyucca.yumebox.runtime.service.util.AutoStartExecutionGate
import kotlinx.coroutines.CancellationException
import timber.log.Timber

object ProxyAutoStartHelper {
    private const val TAG = "ProxyAutoStartHelper"

    // Fault barrier: auto start is best-effort across IPC/store/native seams; any failure is
    // logged and skipped (CancellationException is rethrown at each catch site).
    context(deps: AutoStartDependencies)
    @Suppress("TooGenericExceptionCaught")
    suspend fun checkAndAutoStart(context: Context) {
        if (deps.proxyFacade.isRemoteControllerActive()) {
            Timber.tag(TAG).i("Skip auto start: external controller mode active")
            deps.proxyFacade.applyRemoteControllerState()
            return
        }
        if (AutoStartSessionGate.shouldSkipAutoStart()) {
            Timber.tag(TAG).i("Skip auto start: manual pause gate is active in current session")
            return
        }
        if (AutoStartExecutionGate.isExecuting(deps.serviceCache)) {
            Timber.tag(TAG).i("Skip auto start: background auto-restart is still executing")
            return
        }
        if (deps.featureStore.consumePostUpdateColdStartPending()) {
            Timber.tag(TAG).i("Skip auto start/update: post-update cold-start protection is active")
            return
        }

        val activeProfile =
            try {
                deps.profilesRepository.queryActiveProfile()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                Timber.tag(TAG).e(error, "Failed to load active profile")
                return
            }

        tryUpdateActiveProfileOnStart(activeProfile)

        val automaticRestart = deps.appSettingsStorage.automaticRestart.value
        if (!automaticRestart) {
            return
        }

        if (deps.proxyFacade.runtimeSnapshot.value.running || RuntimeCoordinator.state.value.active) {
            return
        }

        if (activeProfile == null) {
            Timber.tag(TAG).w("No active profile for auto start")
            return
        }

        val mode = deps.networkSettingsStorage.runMode.value
        if (mode == RunMode.VpnService && VpnService.prepare(context) != null) {
            Timber.tag(TAG).i("Skip auto start: VPN permission is missing for VpnService mode")
            return
        }

        try {
            deps.profilesRepository.setActiveProfile(activeProfile.uuid)
            deps.proxyFacade.startProxy(mode)
            Timber.tag(TAG).i("Auto start ok: profile=${activeProfile.uuid}, mode=$mode")
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            Timber.tag(TAG).e(error, "Auto start failed: ${error.message}")
        }
    }

    // Fault barrier: profile auto update on start is best-effort; failure only logs a warning.
    context(deps: AutoStartDependencies)
    @Suppress("TooGenericExceptionCaught")
    private suspend fun tryUpdateActiveProfileOnStart(activeProfile: Profile?) {
        if (!deps.appSettingsStorage.autoUpdateCurrentProfileOnStart.value) return
        val target = activeProfile ?: return
        if (target.type != Profile.Type.Url) {
            Timber.tag(TAG).d("Skip auto update: unsupported profile type=${target.type}")
            return
        }
        try {
            deps.profilesRepository.updateProfile(target.uuid)
            Timber.tag(TAG).i("Auto update on start ok: ${target.uuid}")
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            Timber.tag(TAG).w(error, "Auto update on start failed")
        }
    }
}
