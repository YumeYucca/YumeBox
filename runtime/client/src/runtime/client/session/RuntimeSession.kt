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

@file:Suppress("DuplicatedCode", "UnusedSymbol", "RedundantSuspendModifier")

package com.github.yumeyucca.yumebox.runtime.client.session

import android.net.VpnService
import com.github.yumeyucca.yumebox.core.model.RunMode
import com.github.yumeyucca.yumebox.core.util.AppVisibilityTracker
import com.github.yumeyucca.yumebox.runtime.api.*
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
 * High-cohesion runtime host: snapshot/traffic/profile state, lifecycle, event bridge and traffic
 * polling. Remote-controller takeover lives in [RuntimeRemoteSwitch]. Platform differences go
 * through seams.
 */
internal class RuntimeSession(private val deps: RuntimeSessionDeps) {
    private val context
        get() = deps.context

    private val scope
        get() = deps.scope

    private val networkSettingsStorage
        get() = deps.networkSettingsStorage

    private val remoteControllerStore
        get() = deps.remoteControllerStore

    private val statusStore
        get() = deps.statusStore

    private val processController
        get() = deps.processController

    private val launcher
        get() = deps.launcher

    private val queryTrafficNowAction
        get() = deps.queryTrafficNowAction

    private val queryTrafficTotalAction
        get() = deps.queryTrafficTotalAction

    private val onAfterRunning
        get() = deps.onAfterRunning

    private val onAfterIdle
        get() = deps.onAfterIdle

    private val onGroupTick
        get() = deps.onGroupTick

    private val onTrafficTickExtra
        get() = deps.onTrafficTickExtra

    private val onClearGroups
        get() = deps.onClearGroups

    private companion object {
        const val TRAFFIC_TOTAL_POLL_TICKS = 10
    }

    private val appContext = context.appContextOrSelf
    private val operationMutex = Mutex()
    private var generationCounter = 0L

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

    val ownership =
        RuntimeOwnership(
            statusStore = statusStore,
            processController = processController,
            currentSnapshot = { _runtimeSnapshot.value },
        )

    private val polling =
        RuntimePolling(
            scope = scope,
            isRunning = { _runtimeSnapshot.value.running },
            onTrafficTick = { tick ->
                runCatching {
                    queryTrafficNow(queryTrafficNowAction)
                    if (tick % TRAFFIC_TOTAL_POLL_TICKS == 0) {
                        queryTrafficTotal(queryTrafficTotalAction)
                    }
                }
                    .onFailure { error ->
                        if (error is CancellationException) throw error
                        Timber.d(error, "Traffic polling skipped")
                    }
                onTrafficTickExtra(tick)
            },
            onGroupTick = { onGroupTick() },
        )

    private val eventBridge =
        RuntimeEventBridge(
            context = appContext,
            isConfigReloading = { _isConfigReloading.value },
            onRuntimeStarted = {
                scope.launch {
                    try {
                        reconcileAndRefresh()
                    } finally {
                        _isConfigReloading.value = false
                    }
                }
            },
            onRuntimeStopped = { reason -> scope.launch { handleStopped(reason) } },
            onConfigChanged = { scope.launch { onConfigChanged() } },
            onReconcile = { scope.launch { reconcileAndRefresh() } },
            onRootFailed = { error -> scope.launch { handleFailure(error) } },
        )

    private val remoteSwitch =
        RuntimeRemoteSwitch(
            deps = deps,
            ownership = ownership,
            operationMutex = operationMutex,
            snapshot = { _runtimeSnapshot.value },
            publishRemoteRunning = {
                publishSnapshot(
                    ownership.remoteRunningSnapshot(
                        runMode = networkSettingsStorage.runMode.value,
                        generation = nextGeneration(),
                    )
                )
            },
            reconcile = { reconcile(refreshPayload = false) },
            startLocal = { owner, mode -> start(RuntimeStartRequest(owner = owner, mode = mode)) },
            startTrafficPolling = { startTrafficPolling() },
            stopTrafficPolling = { stopTrafficPolling() },
            connectBackend = { connectBackend() },
        )

    fun bootstrap() {
        eventBridge.register()
        scope.launch {
            operationMutex.withLock { initializeSnapshot() }
            remoteControllerStore.controllerEnabled.state.collect { applyRemoteControllerState() }
        }
    }

    fun isRemoteControllerActive(): Boolean = remoteControllerStore.isActive()

    fun snapshotValue(): RuntimeSnapshot = _runtimeSnapshot.value

    fun publishSnapshot(snapshot: RuntimeSnapshot) {
        val normalized = snapshot.copy(running = snapshot.phase.running)
        _runtimeSnapshot.value = normalized
        _isRunning.value = normalized.running
    }

    fun nextGeneration(): Long {
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

    fun stopGroupPolling() = polling.stopGroups()

    fun setConfigReloading(value: Boolean) {
        _isConfigReloading.value = value
    }

    fun clearRuntimePayload(resetGroups: Boolean = true) {
        _currentProfile.value = null
        onClearGroups(resetGroups)
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

    fun updateTrafficReady() {
        if (!_runtimeSnapshot.value.trafficReady) {
            publishSnapshot(_runtimeSnapshot.value.copy(trafficReady = true))
        }
    }

    fun applyRemoteControllerState() = remoteSwitch.apply()

    suspend fun reconcile(refreshPayload: Boolean = true) {
        if (isRemoteControllerActive()) {
            applyRemoteControllerState()
            return
        }
        operationMutex.withLock {
            val configuredMode = networkSettingsStorage.runMode.value
            statusStore.reconcilePersistedRuntimeState()
            val owner = ownership.detectOwner()

            if (owner == RuntimeOwner.None) {
                stopTrafficPolling()
                clearRuntimePayload(resetGroups = false)
                publishSnapshot(
                    RuntimeStateMapper.idleSnapshot(
                        configuredMode,
                        lastError = statusStore.queryRuntimeLastError(configuredMode.name),
                    )
                )
                if (refreshPayload) onAfterIdle()
                return
            }

            publishSnapshot(
                ownership.activeSnapshot(
                    owner = owner,
                    runMode = ownership.localModeForOwner(owner) ?: configuredMode,
                    localPhase = ownership.localRuntimePhaseForOwner(owner),
                    localStartedAt = ownership.localRuntimeStartedAtForOwner(owner),
                )
            )

            if (_runtimeSnapshot.value.phase.running) {
                startTrafficPolling()
                if (refreshPayload) onAfterRunning()
            } else {
                stopTrafficPolling()
                if (refreshPayload) onAfterIdle()
            }
        }
    }

    suspend fun reconcileAndRefresh() {
        reconcile()
        if (_runtimeSnapshot.value.phase == RuntimePhase.Running) {
            onAfterRunning()
        } else {
            onAfterIdle()
        }
    }

    private suspend fun onConfigChanged() {
        if (
            !isRemoteControllerActive() && ownership.detectActiveOwner() == RuntimeOwner.RootDaemon
        ) {
            runCatching { reload(networkSettingsStorage.runMode.value) }
                .onFailure { error -> Timber.w(error, "Root daemon config reload failed") }
        } else {
            reconcileAndRefresh()
        }
    }

    suspend fun reload(mode: RunMode = networkSettingsStorage.runMode.value) {
        if (isRemoteControllerActive()) return
        _isConfigReloading.value = true
        try {
            start(RuntimeStartRequest(owner = ownership.ownerForMode(mode), mode = mode))
        } catch (error: Throwable) {
            _isConfigReloading.value = false
            throw error
        }
    }

    suspend fun start(request: RuntimeStartRequest) {
        if (isRemoteControllerActive()) {
            Timber.i("Ignoring startProxy: remote controller mode active")
            return
        }
        val mode = request.mode
        Timber.i("Start proxy: mode=$mode")
        RuntimeAccess.connect(appContext)

        val activeProfile = request.profile ?: RuntimeAccess.profile().queryActive()
        check(activeProfile != null) { "No profile selected" }

        if (mode == RunMode.VpnService) {
            val vpnIntent = VpnService.prepare(context)
            if (vpnIntent != null) {
                throw VpnPermissionRequired(vpnIntent)
            }
        }

        operationMutex.withLock {
            if (isRemoteControllerActive()) {
                Timber.i("Ignoring startProxy: remote controller mode active")
                return
            }
            val targetOwner =
                request.owner.takeIf { it != RuntimeOwner.None } ?: ownership.ownerForMode(mode)
            val currentOwner =
                ownership.detectActiveOwner().takeIf { it != RuntimeOwner.None }
                    ?: _runtimeSnapshot.value.owner
            if (currentOwner != RuntimeOwner.None) {
                stopInternal(
                    RuntimeStopRequest(
                        owner = currentOwner,
                        targetMode = mode,
                        completeImmediately = true,
                    )
                )
            }

            val generation = nextGeneration()
            clearRuntimePayload(resetGroups = false)
            _currentProfile.value = activeProfile
            publishSnapshot(
                ownership.startingSnapshot(
                    owner = targetOwner,
                    runMode = mode,
                    profile = activeProfile,
                    generation = generation,
                )
            )

            runCatching { launcher.start(targetOwner, mode) }
                .onFailure { error ->
                    clearRuntimePayload(resetGroups = false)
                    publishSnapshot(
                        RuntimeStateMapper.idleSnapshot(
                            configuredMode = mode,
                            generation = generation,
                            lastError = error.message,
                        )
                    )
                    stopTrafficPolling()
                    scope.launch { onAfterIdle() }
                    throw error
                }
        }
    }

    suspend fun stop(request: RuntimeStopRequest) {
        if (isRemoteControllerActive()) {
            Timber.i("Ignoring stopProxy: remote controller mode active")
            return
        }
        operationMutex.withLock { stopInternal(request) }
    }

    private suspend fun stopInternal(request: RuntimeStopRequest) {
        val owner =
            request.owner.takeIf { it != RuntimeOwner.None }
                ?: ownership.detectActiveOwner().takeIf { it != RuntimeOwner.None }
                ?: _runtimeSnapshot.value.owner
        val generation = nextGeneration()
        val targetMode = request.targetMode

        if (owner == RuntimeOwner.None) {
            clearRuntimePayload(resetGroups = false)
            publishSnapshot(RuntimeStateMapper.idleSnapshot(targetMode, generation = generation))
            stopTrafficPolling()
            scope.launch { onAfterIdle() }
            return
        }

        val previousSnapshot = _runtimeSnapshot.value
        publishSnapshot(
            previousSnapshot.copy(
                owner = owner,
                phase = RuntimePhase.Stopping,
                runMode = targetMode,
                profileReady = false,
                groupsReady = false,
                trafficReady = false,
                lastError = request.reason,
                generation = generation,
            )
        )

        runCatching { launcher.stop(owner) }
            .onFailure {
                publishSnapshot(previousSnapshot)
                throw it
            }

        stopTrafficPolling()
        if (!request.completeImmediately) {
            return
        }

        clearRuntimePayload(resetGroups = false)
        publishSnapshot(RuntimeStateMapper.idleSnapshot(targetMode, generation = generation))
        scope.launch { onAfterIdle() }
    }

    private fun initializeSnapshot() {
        if (remoteControllerStore.isWanted()) {
            applyRemoteControllerState()
        }
        if (isRemoteControllerActive()) {
            return
        }
        val configuredMode = networkSettingsStorage.runMode.value
        statusStore.clearLegacyStateFiles()
        statusStore.reconcilePersistedRuntimeState()
        // Root daemon survives app death; re-attach controller endpoint before ownership probe.
        runCatching { processController.reconnectRoot() }
        val owner = ownership.detectOwner()

        if (owner == RuntimeOwner.None) {
            clearRuntimePayload(resetGroups = false)
            publishSnapshot(
                RuntimeStateMapper.idleSnapshot(
                    configuredMode,
                    lastError = statusStore.queryRuntimeLastError(configuredMode.name),
                )
            )
            scope.launch { onAfterIdle() }
            return
        }

        publishSnapshot(
            ownership.activeSnapshot(
                owner = owner,
                runMode = ownership.localModeForOwner(owner) ?: configuredMode,
                localPhase = ownership.localRuntimePhaseForOwner(owner),
                localStartedAt = ownership.localRuntimeStartedAtForOwner(owner),
            )
        )
        if (_runtimeSnapshot.value.phase.running) {
            startTrafficPolling()
            scope.launch { onAfterRunning() }
        } else {
            stopTrafficPolling()
            scope.launch { onAfterIdle() }
        }
    }

    private fun handleStopped(reason: String?) {
        if (isRemoteControllerActive()) {
            applyRemoteControllerState()
            return
        }
        val generation = nextGeneration()
        clearRuntimePayload(resetGroups = false)
        publishSnapshot(
            RuntimeStateMapper.idleSnapshot(
                configuredMode = networkSettingsStorage.runMode.value,
                generation = generation,
                lastError = reason,
            )
        )
        stopTrafficPolling()
        scope.launch { onAfterIdle() }
    }

    private fun handleFailure(error: String?) {
        if (isRemoteControllerActive()) {
            applyRemoteControllerState()
            return
        }
        val generation = nextGeneration()
        clearRuntimePayload(resetGroups = false)
        publishSnapshot(
            RuntimeStateMapper.idleSnapshot(
                configuredMode = networkSettingsStorage.runMode.value,
                generation = generation,
                lastError = error ?: "root runtime failed",
            )
        )
        stopTrafficPolling()
        scope.launch { onAfterIdle() }
    }

    suspend fun handleMissingLocalRuntime(snapshot: RuntimeSnapshot, reason: String?) {
        val mode = ownership.localModeForOwner(snapshot.owner) ?: return
        statusStore.markRuntimeIdle(mode.name)
        clearRuntimePayload(resetGroups = false)
        publishSnapshot(
            RuntimeStateMapper.idleSnapshot(
                configuredMode = networkSettingsStorage.runMode.value,
                generation = nextGeneration(),
                lastError = reason,
            )
        )
        stopTrafficPolling()
    }

    fun isMissingLocalRuntime(snapshot: RuntimeSnapshot): Boolean {
        if (
            snapshot.owner == RuntimeOwner.None || snapshot.owner == RuntimeOwner.RemoteController
        ) {
            return false
        }
        val mode = ownership.localModeForOwner(snapshot.owner) ?: return false
        return !statusStore.isLocalRuntimeServiceAlive(mode.name)
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
