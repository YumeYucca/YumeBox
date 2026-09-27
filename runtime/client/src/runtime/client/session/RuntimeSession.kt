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
import com.github.yumeyucca.yumebox.core.util.AppVisibilityTracker
import com.github.yumeyucca.yumebox.runtime.api.Profile
import com.github.yumeyucca.yumebox.runtime.api.RuntimeOwner
import com.github.yumeyucca.yumebox.runtime.api.RuntimePhase
import com.github.yumeyucca.yumebox.runtime.api.RuntimeSnapshot
import com.github.yumeyucca.yumebox.runtime.api.RuntimeStartSource
import com.github.yumeyucca.yumebox.runtime.api.RuntimeState
import com.github.yumeyucca.yumebox.runtime.api.appContextOrSelf
import com.github.yumeyucca.yumebox.runtime.client.ProxyGroupSyncPriority
import com.github.yumeyucca.yumebox.runtime.client.RuntimeStartRequest
import com.github.yumeyucca.yumebox.runtime.client.RuntimeStateMapper
import com.github.yumeyucca.yumebox.runtime.client.RuntimeStopRequest
import com.github.yumeyucca.yumebox.runtime.client.access.RuntimeAccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

/**
 * UI-side view of the runtime: mirrors [RuntimeSessionDeps.control] into a [RuntimeSnapshot]
 * enriched with profile / group / traffic readiness, and drives payload refreshes and traffic
 * polling. Lifecycle decisions stay in the control; remote-controller takeover lives in
 * [RuntimeRemoteSwitch].
 */
internal class RuntimeSession(private val deps: RuntimeSessionDeps) {
    private val scope
        get() = deps.scope

    private val control
        get() = deps.control

    private val networkSettingsStorage
        get() = deps.networkSettingsStorage

    private val remoteControllerStore
        get() = deps.remoteControllerStore

    private companion object {
        const val TRAFFIC_TOTAL_POLL_TICKS = 10
    }

    private val appContext = deps.context.appContextOrSelf

    /** Serializes snapshot publication between the local state mirror and the remote switch. */
    private val operationMutex = Mutex()
    private var generationCounter = 0L

    /** Local-state generation whose payload refresh already ran. */
    private var refreshedGeneration = -1L

    private val _runtimeSnapshot =
        MutableStateFlow(RuntimeStateMapper.idleSnapshot(networkSettingsStorage.runMode.value))
    val runtimeSnapshot: StateFlow<RuntimeSnapshot> = _runtimeSnapshot.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _isConfigReloading = MutableStateFlow(false)
    val isConfigReloading: StateFlow<Boolean> = _isConfigReloading.asStateFlow()

    private val _currentProfile = MutableStateFlow<Profile?>(null)
    val currentProfile: StateFlow<Profile?> = _currentProfile.asStateFlow()

    private val _trafficNow = MutableStateFlow(0L)
    val trafficNow: StateFlow<Long> = _trafficNow.asStateFlow()

    private val _trafficTotal = MutableStateFlow(0L)
    val trafficTotal: StateFlow<Long> = _trafficTotal.asStateFlow()

    private val polling =
        RuntimePolling(
            scope = scope,
            isRunning = { _runtimeSnapshot.value.running },
            onTrafficTick = { tick ->
                runCatching {
                    queryTrafficNow(deps.queryTrafficNowAction)
                    if (tick % TRAFFIC_TOTAL_POLL_TICKS == 0) {
                        queryTrafficTotal(deps.queryTrafficTotalAction)
                    }
                }
                    .onFailure { error ->
                        if (error is CancellationException) throw error
                        Timber.d(error, "Traffic polling skipped")
                    }
                deps.onTrafficTickExtra(tick)
            },
            onGroupTick = { deps.onGroupTick() },
        )

    private val eventBridge =
        RuntimeEventBridge(
            context = appContext,
            onConfigChanged = { scope.launch { onConfigChanged() } },
        )

    private val remoteSwitch =
        RuntimeRemoteSwitch(
            deps = deps,
            operationMutex = operationMutex,
            snapshot = { _runtimeSnapshot.value },
            publishRemoteRunning = {
                publishSnapshot(
                    RuntimeSnapshot(
                        owner = RuntimeOwner.RemoteController,
                        phase = RuntimePhase.Running,
                        runMode = networkSettingsStorage.runMode.value,
                        generation = nextGeneration(),
                        startedAt = System.currentTimeMillis(),
                    )
                )
            },
            reconcile = { reconcile(refreshPayload = false) },
            startLocal = { mode -> start(RuntimeStartRequest(mode = mode)) },
            startTrafficPolling = { startTrafficPolling() },
            stopTrafficPolling = { stopTrafficPolling() },
            connectBackend = { connectBackend() },
        )

    fun bootstrap() {
        eventBridge.register()
        if (remoteControllerStore.isWanted()) applyRemoteControllerState()
        scope.launch { control.state.collect { state -> mirrorLocal(state) } }
        scope.launch {
            remoteControllerStore.controllerEnabled.state.collect { applyRemoteControllerState() }
        }
    }

    fun isRemoteControllerActive(): Boolean = remoteControllerStore.isActive()

    fun snapshotValue(): RuntimeSnapshot = _runtimeSnapshot.value

    private fun publishSnapshot(snapshot: RuntimeSnapshot) {
        val normalized = snapshot.copy(running = snapshot.phase.running)
        _runtimeSnapshot.value = normalized
        _isRunning.value = normalized.running
    }

    private fun nextGeneration(): Long {
        generationCounter += 1L
        return generationCounter
    }

    fun startTrafficPolling() {
        if (AppVisibilityTracker.isForeground.value) {
            polling.startTraffic()
        }
    }

    fun stopTrafficPolling() = polling.stopTraffic()

    fun startGroupPolling(priority: ProxyGroupSyncPriority) = polling.startGroups(priority)

    private fun clearRuntimePayload() {
        _currentProfile.value = null
        deps.onClearGroups(false)
        _trafficNow.value = 0L
        _trafficTotal.value = 0L
    }

    fun updateProfileReady(profile: Profile?) {
        val snapshot = _runtimeSnapshot.value
        publishSnapshot(
            snapshot.copy(
                profileReady = profile != null,
                profileUuid = profile?.uuid?.toString() ?: snapshot.profileUuid,
                profileName = profile?.name ?: snapshot.profileName,
            )
        )
    }

    fun updateGroupsReady(ready: Boolean) {
        publishSnapshot(_runtimeSnapshot.value.copy(groupsReady = ready))
    }

    private fun updateTrafficReady() {
        if (!_runtimeSnapshot.value.trafficReady) {
            publishSnapshot(_runtimeSnapshot.value.copy(trafficReady = true))
        }
    }

    fun applyRemoteControllerState() = remoteSwitch.apply()

    private suspend fun mirrorLocal(state: RuntimeState) {
        operationMutex.withLock { applyLocalLocked(state, forceRefresh = false) }
    }

    /**
     * Publishes [state] unless the remote controller holds the snapshot, and refreshes the
     * payload once per settled state: each Running generation (outside a reload) and each idle
     * one.
     */
    private fun applyLocalLocked(state: RuntimeState, forceRefresh: Boolean) {
        _isConfigReloading.value = state.reloading
        if (isRemoteControllerActive() ||
            _runtimeSnapshot.value.owner == RuntimeOwner.RemoteController
        ) {
            return
        }
        val previous = _runtimeSnapshot.value
        val next = mapLocal(state, previous)
        if (previous.running && !next.running && !state.reloading) clearRuntimePayload()
        publishSnapshot(next)

        val settled =
            (state.phase == RuntimePhase.Running && !state.reloading) ||
                state.phase == RuntimePhase.Idle ||
                state.phase == RuntimePhase.Failed
        if (next.running) startTrafficPolling() else stopTrafficPolling()
        if (!settled || (!forceRefresh && state.generation == refreshedGeneration)) return
        refreshedGeneration = state.generation
        scope.launch { if (next.running) deps.onAfterRunning() else deps.onAfterIdle() }
    }

    private fun mapLocal(state: RuntimeState, previous: RuntimeSnapshot): RuntimeSnapshot {
        val configuredMode = networkSettingsStorage.runMode.value
        if (state.owner == RuntimeOwner.None) {
            return RuntimeStateMapper.idleSnapshot(
                configuredMode = configuredMode,
                generation = nextGeneration(),
                lastError = state.lastError,
            )
        }
        // Readiness survives only while the same session keeps running (reload, error notes).
        val keep =
            previous.owner == state.owner &&
                previous.phase == RuntimePhase.Running &&
                state.phase == RuntimePhase.Running
        val profile = _currentProfile.value
        return RuntimeSnapshot(
            owner = state.owner,
            phase = state.phase,
            runMode = state.mode ?: configuredMode,
            profileReady = keep && previous.profileReady,
            groupsReady = keep && previous.groupsReady,
            trafficReady = keep && previous.trafficReady,
            profileUuid = profile?.uuid?.toString() ?: previous.profileUuid,
            profileName = profile?.name ?: previous.profileName,
            lastError = state.lastError,
            startedAt = state.startedAt,
            generation = nextGeneration(),
        )
    }

    /** Re-checks core liveness, republishes the local state and optionally refreshes payload. */
    suspend fun reconcile(refreshPayload: Boolean = true) {
        if (isRemoteControllerActive()) {
            applyRemoteControllerState()
            return
        }
        control.verify()
        operationMutex.withLock {
            if (_runtimeSnapshot.value.owner == RuntimeOwner.RemoteController) {
                publishSnapshot(
                    RuntimeStateMapper.idleSnapshot(
                        networkSettingsStorage.runMode.value,
                        generation = nextGeneration(),
                    )
                )
            }
            applyLocalLocked(control.state.value, forceRefresh = refreshPayload)
        }
    }

    suspend fun reconcileAndRefresh() = reconcile(refreshPayload = true)

    /** Idle config edits change what the preview shows; running ones arrive as a reload. */
    private suspend fun onConfigChanged() {
        if (isRemoteControllerActive() || control.state.value.active) return
        deps.onAfterIdle()
    }

    /**
     * Applies changed settings to an active runtime: a reload in the same [mode], an owner switch
     * otherwise. An idle runtime stays idle.
     */
    suspend fun reload(mode: RunMode) {
        if (isRemoteControllerActive()) return
        val state = control.state.value
        when {
            !state.active -> Unit
            state.mode == mode -> control.reload()
            else -> control.start(mode, RuntimeStartSource.Ui)
        }
    }

    suspend fun start(request: RuntimeStartRequest) {
        if (isRemoteControllerActive()) {
            Timber.i("Ignoring startProxy: remote controller mode active")
            return
        }
        Timber.i("Start proxy: mode=${request.mode}")
        RuntimeAccess.connect(appContext)
        val activeProfile = request.profile ?: RuntimeAccess.profile().queryActive()
        check(activeProfile != null) { "No profile selected" }
        _currentProfile.value = activeProfile
        control.start(request.mode, RuntimeStartSource.Ui)
    }

    suspend fun stop(request: RuntimeStopRequest) {
        if (isRemoteControllerActive()) {
            Timber.i("Ignoring stopProxy: remote controller mode active")
            return
        }
        control.stop(request.reason)
    }

    /**
     * The controller did not answer: returns true when that is because the local core itself is
     * gone (the control then already dropped to Idle).
     */
    suspend fun verifyLocalRuntime(snapshot: RuntimeSnapshot): Boolean {
        if (snapshot.owner != RuntimeOwner.VpnService && snapshot.owner != RuntimeOwner.RootDaemon) {
            return false
        }
        control.verify()
        return !control.state.value.active
    }

    suspend fun queryTrafficTotal(query: suspend () -> Long): Long {
        if (!_runtimeSnapshot.value.running) {
            _trafficTotal.value = 0L
            return 0L
        }
        val traffic = query()
        _trafficTotal.value = traffic
        updateTrafficReady()
        return traffic
    }

    suspend fun queryTrafficNow(query: suspend () -> Long): Long {
        if (!_runtimeSnapshot.value.running) {
            _trafficNow.value = 0L
            return 0L
        }
        val traffic = query()
        _trafficNow.value = traffic
        updateTrafficReady()
        return traffic
    }

    fun setCurrentProfile(profile: Profile?) {
        _currentProfile.value = profile
    }

    fun setTrafficNow(value: Long) {
        _trafficNow.value = value
    }

    fun setTrafficTotal(value: Long) {
        _trafficTotal.value = value
    }

    suspend fun connectBackend() {
        RuntimeAccess.connect(appContext)
    }

    fun shouldRefreshRuntimePayload(groupsEmpty: Boolean): Boolean {
        val snapshot = _runtimeSnapshot.value
        return snapshot.phase == RuntimePhase.Running &&
            (!snapshot.profileReady ||
                !snapshot.groupsReady ||
                groupsEmpty ||
                _currentProfile.value == null)
    }
}
