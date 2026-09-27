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

import com.github.yumeyucca.yumebox.core.model.RunMode
import com.github.yumeyucca.yumebox.core.util.PollingTimerSpecs
import com.github.yumeyucca.yumebox.core.util.PollingTimers
import com.github.yumeyucca.yumebox.core.util.enumByNameOrNull
import com.github.yumeyucca.yumebox.data.store.PausedLocalRuntime
import com.github.yumeyucca.yumebox.runtime.api.RuntimeOwner
import com.github.yumeyucca.yumebox.runtime.api.RuntimePhase
import com.github.yumeyucca.yumebox.runtime.api.RuntimeSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

/**
 * Probe / attach / fallback for external-controller mode. The wanted preference stays on the store;
 * this object owns the live takeover so [RuntimeSession] does not grow a second lifecycle.
 */
internal class RuntimeRemoteSwitch(
    private val deps: RuntimeSessionDeps,
    private val operationMutex: Mutex,
    private val snapshot: () -> RuntimeSnapshot,
    private val publishRemoteRunning: () -> Unit,
    private val reconcile: suspend () -> Unit,
    private val startLocal: suspend (RunMode) -> Unit,
    private val startTrafficPolling: () -> Unit,
    private val stopTrafficPolling: () -> Unit,
    private val connectBackend: suspend () -> Unit,
) {
    private val scope = deps.scope
    private val store = deps.remoteControllerStore
    private val control = deps.control
    private val probe = deps.probeRemote
    private val mutex = Mutex()
    private var probeJob: Job? = null
    private var refreshJob: Job? = null

    /**
     * Consecutive unreachable probes while a remote session is held. One miss must not restart the
     * local runtime.
     */
    private var unreachableStreak = 0

    fun apply() {
        scope.launch { mutex.withLock { applyLocked() } }
    }

    private suspend fun applyLocked() {
        if (!store.isWanted()) {
            detachIfHolding()
            stopWatch()
            return
        }
        startWatch()
        checkRemote(refresh = true)
    }

    private suspend fun watchdogTick() {
        if (!store.isWanted()) {
            detachIfHolding()
            return
        }
        checkRemote(refresh = false)
    }

    private suspend fun checkRemote(refresh: Boolean) {
        val backend = store.activeBackend()
        val reachable = RemoteControllerProbe.isReachable(probe)
        // A setting change queues another apply; never attach using the previous target's reply.
        if (!store.isWanted() || store.activeBackend() != backend) return
        if (reachable) {
            unreachableStreak = 0
            attach(refresh)
            return
        }
        if (!isHolding()) return
        unreachableStreak += 1
        if (unreachableStreak < DETACH_AFTER_MISSES) return
        unreachableStreak = 0
        detachIfHolding()
    }

    private fun startWatch() {
        if (probeJob?.isActive == true) return
        probeJob =
            scope.launch {
                PollingTimers.ticks(PollingTimerSpecs.RemoteControllerProbe).conflate().collect {
                    mutex.withLock { watchdogTick() }
                }
            }
    }

    private fun stopWatch() {
        probeJob?.cancel()
        probeJob = null
    }

    private fun isWatchingRemote(): Boolean {
        val current = snapshot()
        return current.owner == RuntimeOwner.RemoteController &&
            current.phase == RuntimePhase.Running
    }

    private suspend fun attach(refresh: Boolean) {
        if (store.isActive() && isWatchingRemote() && !refresh) return
        refreshJob?.cancel()
        store.controllerAttached.set(true)
        operationMutex.withLock {
            if (!isWatchingRemote()) {
                pauseLocalIfRunning()
                publishRemoteRunning()
            }
        }
        // Node/traffic refreshes may wait on slow HTTP requests. They must never hold the watchdog.
        refreshJob = scope.launch {
            runCatching {
                connectBackend()
                startTrafficPolling()
                deps.onAfterRunning()
            }.onFailure { error ->
                if (error is CancellationException) throw error
                Timber.d(error, "Remote controller backend refresh skipped")
            }
        }
    }

    private fun isHolding(): Boolean =
        store.controllerAttached.value || snapshot().owner == RuntimeOwner.RemoteController

    private suspend fun detachIfHolding() {
        if (isHolding()) detach()
    }

    private suspend fun detach() {
        refreshJob?.cancel()
        refreshJob = null
        store.controllerAttached.set(false)
        stopTrafficPolling()
        val paused = store.takePausedLocal()
        val pausedMode = paused?.resumableMode()
        if (snapshot().owner == RuntimeOwner.RemoteController) {
            reconcile()
        }
        if (pausedMode == null) {
            refreshJob = scope.launch {
                if (snapshot().phase == RuntimePhase.Running) {
                    deps.onAfterRunning()
                } else {
                    deps.onAfterIdle()
                }
            }
            return
        }
        Timber.i(
            "Controller fallback: resuming local runtime owner=${paused?.ownerName} mode=$pausedMode"
        )
        runCatching { startLocal(pausedMode) }
            .onFailure { error ->
                if (error is CancellationException) throw error
                Timber.w(error, "Failed to resume local runtime after controller fallback")
            }
    }

    private suspend fun pauseLocalIfRunning() {
        runCatching {
            val state = control.state.value
            val owner = state.owner
            if (!state.active ||
                (owner != RuntimeOwner.VpnService && owner != RuntimeOwner.RootDaemon)
            ) {
                return
            }
            val mode = state.mode ?: deps.networkSettingsStorage.runMode.value
            store.rememberPausedLocal(owner.name, mode.name)
            Timber.i("Controller switch: pausing local runtime owner=$owner mode=$mode")
            control.stop()
            stopTrafficPolling()
        }
            .onFailure { error ->
                if (error is CancellationException) throw error
                Timber.w(error, "Failed to pause local runtime on controller switch")
            }
    }

    /** Only a paused local core (VPN or root) is resumed. */
    private fun PausedLocalRuntime.resumableMode(): RunMode? {
        val owner = enumByNameOrNull<RuntimeOwner>(ownerName)
        if (owner != RuntimeOwner.VpnService && owner != RuntimeOwner.RootDaemon) return null
        return enumByNameOrNull<RunMode>(modeName)
    }

    private companion object {
        /** About a few seconds of continuous probe failure before the local runtime is resumed. */
        const val DETACH_AFTER_MISSES = 4
    }
}
