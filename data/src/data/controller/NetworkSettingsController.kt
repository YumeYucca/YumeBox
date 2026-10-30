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

@file:Suppress("UnusedSymbol")

package com.github.yumeyucca.yumebox.data.controller


import com.github.yumeyucca.yumebox.core.model.RunMode
import com.github.yumeyucca.yumebox.data.store.NetworkSettingsStore
import com.github.yumeyucca.yumebox.data.store.Preference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class NetworkSettingsController(
    private val store: NetworkSettingsStore,
    private val isRunning: () -> Boolean,
    private val restartProxy: suspend (RunMode) -> Unit,
    private val beforeRestart: suspend (RunMode) -> Unit = {},
) {
    private val restarter =
        DebouncedProxyRestarter(
            timerName = "network_settings_restart_debounce",
            debounceMillis = RESTART_DEBOUNCE_DELAY_MS,
            isRunning = isRunning,
        )

    fun setRunMode(mode: RunMode) {
        if (store.runMode.value == mode) return
        store.runMode.set(mode)
        // A mode change is a live runtime operation. The RuntimeSession owns the actual
        // stop-old-owner/start-new-owner transaction, so this controller only schedules the same
        // debounced restart used by the other network settings.
        if (isRunning()) {
            scheduleRestart()
        }
    }

    fun <T> setAndRestartIfNeeded(preference: Preference<T>, value: T) {
        if (preference.value == value) return
        preference.set(value)
        scheduleRestart()
    }

    suspend fun startService(mode: RunMode): Result<Unit> =
        try {
            store.runMode.set(mode)
            beforeRestart(mode)
            withContext(Dispatchers.IO) { restartProxy(mode) }
            Result.success(Unit)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            // eBPF is an explicit user-selected mode. An attach failure must remain visible;
            // silently changing the persisted mode would make the UI claim a different datapath.
            Result.failure(error)
        }

    fun requestRestartIfRunning() {
        scheduleRestart()
    }

    private fun scheduleRestart() {
        restarter.schedule {
            // [startService] runs the before-restart hook itself, so it is not repeated here: a
            // hook with a side effect would otherwise run twice for one restart.
            startService(store.runMode.value).onFailure { error ->
                Timber.e(error, "Restart after a network settings change failed")
            }
        }
    }

    companion object {
        private const val RESTART_DEBOUNCE_DELAY_MS = 300L
    }
}
