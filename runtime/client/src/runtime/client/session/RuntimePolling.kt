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

package com.github.yumeyucca.yumebox.runtime.client.session

import com.github.yumeyucca.yumebox.core.util.PollingTimerSpecs
import com.github.yumeyucca.yumebox.core.util.PollingTimers
import com.github.yumeyucca.yumebox.runtime.client.ProxyGroupSyncPriority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Owns traffic + proxy-group polling loops for the runtime client. Keeps timer policy out of the
 * facade body.
 */
internal class RuntimePolling(
    private val scope: CoroutineScope,
    private val isRunning: () -> Boolean,
    private val onTrafficTick: suspend (tick: Int) -> Unit,
    private val onGroupTick: suspend () -> Unit,
) {
    private var trafficJob: Job? = null
    private var groupJob: Job? = null
    private var activeGroupPriority: ProxyGroupSyncPriority = ProxyGroupSyncPriority.OFF

    fun startTraffic() {
        if (trafficJob?.isActive == true) return
        trafficJob = scope.launch {
            var tick = 0
            PollingTimers.ticks(PollingTimerSpecs.RuntimeTrafficPolling).collect {
                if (!isRunning()) return@collect
                onTrafficTick(tick)
                tick++
            }
        }
    }

    fun stopTraffic() {
        trafficJob?.cancel()
        trafficJob = null
    }

    fun startGroups(priority: ProxyGroupSyncPriority) {
        if (activeGroupPriority == priority && groupJob?.isActive == true) {
            return
        }
        activeGroupPriority = priority
        groupJob?.cancel()
        groupJob = null
        val timerSpec =
            when (priority) {
                ProxyGroupSyncPriority.FAST -> PollingTimerSpecs.RuntimeProxyGroupSyncFast
                ProxyGroupSyncPriority.SLOW -> PollingTimerSpecs.RuntimeProxyGroupSyncSlow
                ProxyGroupSyncPriority.OFF -> return
            }
        groupJob = scope.launch {
            PollingTimers.ticks(timerSpec).collect { onGroupTick() }
        }
    }
}
