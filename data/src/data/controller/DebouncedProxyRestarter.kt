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

package com.github.yumeyucca.yumebox.data.controller

import com.github.yumeyucca.yumebox.core.util.PollingTimerSpecs
import com.github.yumeyucca.yumebox.core.util.PollingTimers
import kotlinx.coroutines.*

/**
 * Shared debounce engine for the settings controllers that restart the proxy after a change: every [schedule] cancels
 * the pending run, waits out the debounce tick, and only invokes the action while the proxy is still running.
 */
class DebouncedProxyRestarter(
    private val timerName: String,
    private val debounceMillis: Long,
    private val isRunning: () -> Boolean,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) {
    private var job: Job? = null

    fun schedule(action: suspend () -> Unit) {
        job?.cancel()
        job = scope.launch {
            PollingTimers.awaitTick(
                PollingTimerSpecs.dynamic(
                    name = timerName,
                    intervalMillis = debounceMillis,
                    initialDelayMillis = debounceMillis,
                )
            )
            if (!isRunning()) return@launch
            action()
        }
    }
}
