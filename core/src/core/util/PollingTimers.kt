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

@file:Suppress("UnusedSymbol", "ConvertLongToDuration")

package com.github.yumeyucca.yumebox.core.util

import android.os.SystemClock
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

data class PollingTimerSpec(
    val name: String,
    val intervalMillis: Long,
    val initialDelayMillis: Long = intervalMillis,
) {
    init {
        require(name.isNotBlank()) { "Timer name must not be blank" }
        require(intervalMillis > 0L) { "Timer interval must be > 0" }
        require(initialDelayMillis >= 0L) { "Timer initial delay must be >= 0" }
    }
}

object PollingTimerSpecs {
    val MoeElapsedClock = PollingTimerSpec("acg_elapsed_clock", 1_000L, 0L)
    val ConnectionsPolling = PollingTimerSpec("connections_polling", 1_000L, 0L)
    val RuntimeTrafficPolling = PollingTimerSpec("runtime_traffic_polling", 1_000L, 0L)
    val RuntimeProxyGroupSyncFast = PollingTimerSpec("runtime_proxy_group_sync_fast", 1_000L, 0L)
    val RuntimeProxyGroupSyncSlow = PollingTimerSpec("runtime_proxy_group_sync_slow", 3_000L, 0L)
    val ServiceTrafficNotification = PollingTimerSpec("service_traffic_notification", 1_000L, 0L)
    val SessionConnectionTracking = PollingTimerSpec("session_connection_tracking", 5_000L, 0L)
    val RemoteControllerProbe = PollingTimerSpec("remote_controller_probe", 250L, 250L)
    val TrafficStatsCollection = PollingTimerSpec("traffic_stats_collection", 15_000L, 0L)
    val ProxyHealthcheckRefresh = PollingTimerSpec("proxy_healthcheck_refresh", 1_500L, 1_500L)

    fun dynamic(
        name: String,
        intervalMillis: Long,
        initialDelayMillis: Long = intervalMillis,
    ): PollingTimerSpec =
        PollingTimerSpec(
            name = "dynamic_$name",
            intervalMillis = intervalMillis,
            initialDelayMillis = initialDelayMillis,
        )
}

object PollingTimers {
    private const val STOP_TIMEOUT_MILLIS = 5_000L

    // One lightweight scheduler lane for all periodic tick emission in this process.
    private val schedulerScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))
    private val tickerCache = ConcurrentHashMap<PollingTimerSpec, SharedFlow<Long>>()

    fun ticks(spec: PollingTimerSpec): Flow<Long> =
        // `computeIfAbsent`, not `getOrPut`: the latter reads then writes, so two first callers can
        // each build (and subscribe to) their own shared flow for the same spec.
        tickerCache.computeIfAbsent(spec) {
            flow {
                if (spec.initialDelayMillis > 0L) {
                    delay(spec.initialDelayMillis.milliseconds)
                }
                while (currentCoroutineContext().isActive) {
                    emit(SystemClock.elapsedRealtime())
                    delay(spec.intervalMillis.milliseconds)
                }
            }
                .shareIn(
                    scope = schedulerScope,
                    started =
                        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS.milliseconds),
                    replay = 0,
                )
        }

    suspend fun awaitTick(spec: PollingTimerSpec) {
        ticks(spec).first()
    }
}
