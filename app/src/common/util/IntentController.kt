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

package com.github.yumeyucca.yumebox.common.util

import android.content.Intent
import com.github.yumeyucca.yumebox.core.util.AutoStartSessionGate
import com.github.yumeyucca.yumebox.data.store.NetworkSettingsStore
import com.github.yumeyucca.yumebox.runtime.client.ProfilesRepository
import com.github.yumeyucca.yumebox.runtime.client.ProxyFacade
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class IntentController(private val scope: CoroutineScope) : KoinComponent {
    companion object {
        private const val ACTION_START_CLASH = "com.github.yumeyucca.yumebox.action.START_CLASH"
        private const val ACTION_STOP_CLASH = "com.github.yumeyucca.yumebox.action.STOP_CLASH"
    }

    private val proxyFacade: ProxyFacade by inject()
    private val profilesRepository: ProfilesRepository by inject()
    private val networkSettingsStorage: NetworkSettingsStore by inject()

    fun handleIntent(intent: Intent?) {
        intent?.let { safeIntent ->
            when (safeIntent.action) {
                ACTION_START_CLASH -> handleStartRuntime()
                ACTION_STOP_CLASH -> handleStopRuntime()
                else -> {}
            }
        }
    }

    // Fault barrier: external intents must never crash the app; any failure is logged only.
    @Suppress("TooGenericExceptionCaught")
    private fun handleStartRuntime() {
        scope.launch {
            try {
                val activeProfile = profilesRepository.queryActiveProfile()
                if (activeProfile == null) {
                    Timber.w("Skip external start: no active profile")
                    return@launch
                }

                Timber.i("External start: profile=${activeProfile.name}")
                AutoStartSessionGate.clearManualPaused()

                val mode = networkSettingsStorage.runMode.value
                proxyFacade.startProxy(mode)

                Timber.i("External start ok")
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Timber.e(error, "External start failed")
            }
        }
    }

    // Fault barrier: external intents must never crash the app; any failure is logged only.
    @Suppress("TooGenericExceptionCaught")
    private fun handleStopRuntime() {
        scope.launch {
            Timber.i("External stop")
            try {
                AutoStartSessionGate.markManualPaused()
                proxyFacade.stopProxy()
                Timber.i("External stop ok")
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Timber.e(error, "External stop failed")
            }
        }
    }
}
