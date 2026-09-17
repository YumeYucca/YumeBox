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
    private val ownership: RuntimeOwnership,
    private val operationMutex: Mutex,
    private val snapshot: () -> RuntimeSnapshot,
    private val publishRemoteRunning: () -> Unit,
    private val reconcile: suspend () -> Unit,
    private val startLocal: suspend (RuntimeOwner, RunMode) -> Unit,
    private val startTrafficPolling: () -> Unit,
    private val stopTrafficPolling: () -> Unit,
    private val connectBackend: suspend () -> Unit,
) {
    private val scope
        get() = deps.scope

    private val store
        get() = deps.remoteControllerStore

    private val launcher
        get() = deps.launcher

    private val statusStore
        get() = deps.statusStore

    private val probe
        get() = deps.probeRemote

    private fun configuredMode(): RunMode = deps.networkSettingsStorage.runMode.value

    private val mutex = Mutex()
    private var probeJob: Job? = null
    private var refreshJob: Job? = null

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
            attach(refresh)
        } else {
            detachIfHolding()
        }
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

    private suspend fun detachIfHolding() {
        if (store.controllerAttached.value ||
                snapshot().owner == RuntimeOwner.RemoteController
        ) {
            detach()
        }
    }

    private suspend fun detach() {
        refreshJob?.cancel()
        refreshJob = null
        store.controllerAttached.set(false)
        stopTrafficPolling()
        val paused = store.takePausedLocal()?.toTypedOrNull()
        if (snapshot().owner == RuntimeOwner.RemoteController) {
            reconcile()
        }
        if (paused == null) {
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
            "Controller fallback: resuming local runtime owner=${paused.owner} mode=${paused.mode}"
        )
        runCatching { startLocal(paused.owner, paused.mode) }
            .onFailure { error ->
                if (error is CancellationException) throw error
                Timber.w(error, "Failed to resume local runtime after controller fallback")
            }
    }

    private suspend fun pauseLocalIfRunning() {
        runCatching {
            val owner = ownership.detectActiveOwner()
            if (owner != RuntimeOwner.VpnService && owner != RuntimeOwner.RootDaemon) return
            val mode = ownership.localModeForOwner(owner) ?: configuredMode()
            store.rememberPausedLocal(owner.name, mode.name)
            Timber.i("Controller switch: pausing local runtime owner=$owner mode=$mode")
            launcher.stop(owner)
            stopTrafficPolling()
            statusStore.reconcilePersistedRuntimeState()
        }
            .onFailure { error ->
                if (error is CancellationException) throw error
                Timber.w(error, "Failed to pause local runtime on controller switch")
            }
    }

    private data class TypedPausedLocal(
        val owner: RuntimeOwner,
        val mode: RunMode,
    )

    private fun PausedLocalRuntime.toTypedOrNull(): TypedPausedLocal? {
        val owner = enumByNameOrNull<RuntimeOwner>(ownerName) ?: return null
        val mode = enumByNameOrNull<RunMode>(modeName) ?: return null
        return when (owner) {
            RuntimeOwner.VpnService,
            RuntimeOwner.RootDaemon -> TypedPausedLocal(owner, mode)
            RuntimeOwner.RemoteController,
            RuntimeOwner.None -> null
        }
    }
}
