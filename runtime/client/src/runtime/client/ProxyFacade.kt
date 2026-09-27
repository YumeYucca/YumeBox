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

package com.github.yumeyucca.yumebox.runtime.client


import android.content.Context
import com.github.yumeyucca.yumebox.core.model.*
import com.github.yumeyucca.yumebox.core.util.AppVisibilityTracker
import com.github.yumeyucca.yumebox.data.store.MMKVProvider
import com.github.yumeyucca.yumebox.data.store.NetworkSettingsStore
import com.github.yumeyucca.yumebox.data.store.RemoteControllerStore
import com.github.yumeyucca.yumebox.domain.model.ProxyDelayTestProgressCallback
import com.github.yumeyucca.yumebox.domain.model.ProxyGroupInfo
import com.github.yumeyucca.yumebox.domain.model.toInfo
import com.github.yumeyucca.yumebox.runtime.api.*
import com.github.yumeyucca.yumebox.runtime.client.access.RuntimeAccess
import com.github.yumeyucca.yumebox.runtime.client.session.RuntimeCoreOps
import com.github.yumeyucca.yumebox.runtime.client.session.RuntimeGroupHub
import com.github.yumeyucca.yumebox.runtime.client.session.RuntimeSession
import com.github.yumeyucca.yumebox.runtime.client.session.RuntimeSessionDeps
import com.github.yumeyucca.yumebox.runtime.service.preview.PreviewRuntimeManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber

enum class ProxyGroupSyncPriority {
    OFF,
    SLOW,
    FAST,
}

/** The controller that produced the current node snapshot. Preview data is read-only. */
enum class NodeDataSource {
    None,
    Preview,
    Active,
}

/** Keeps the last successful node snapshot through real-core handoff and reload transitions. */
data class NodeSessionState(
    val source: NodeDataSource = NodeDataSource.None,
    val groups: List<ProxyGroupInfo> = emptyList(),
    val available: Boolean = false,
    val everReady: Boolean = false,
)

/**
 * UI-facing runtime facade. Runtime lifecycle/state live in [RuntimeSession]; proxy-group ops live
 * in [RuntimeGroupHub].
 */
class ProxyFacade(
    context: Context,
    private val networkSettingsStorage: NetworkSettingsStore =
        NetworkSettingsStore(MMKVProvider().getMMKV("network_settings")),
    private val remoteControllerStore: RemoteControllerStore =
        RemoteControllerStore(MMKVProvider().getMMKV("remote_controller")),
) {
    private companion object {
        const val RUNTIME_PAYLOAD_REFRESH_TICKS = 15
        const val DEFAULT_SYNC_PRIORITY_SOURCE = "default"
    }

    private val appContext: Context = context.appContextOrSelf
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val session: RuntimeSession
    private val coreOps = RuntimeCoreOps(connect = { session.connectBackend() })
    private val groups: RuntimeGroupHub
    private val preview = PreviewRuntimeManager(appContext)
    private val _nodeSession = MutableStateFlow(NodeSessionState())

    private var previewWarmupJob: Job? = null
    private val syncPriorityRequests =
        MutableStateFlow<Map<String, ProxyGroupSyncPriority>>(emptyMap())

    val runtimeSnapshot: StateFlow<RuntimeSnapshot>
    val isRunning: StateFlow<Boolean>
    val isConfigReloading: StateFlow<Boolean>
    val currentProfile: StateFlow<Profile?>
    val trafficNow: StateFlow<Traffic>
    val trafficTotal: StateFlow<Traffic>
    val proxyGroups: StateFlow<List<ProxyGroupInfo>>
    val nodeSession: StateFlow<NodeSessionState> = _nodeSession.asStateFlow()
    val resolvedPrimaryNode: StateFlow<Proxy?>

    init {
        session =
            RuntimeSession(
                RuntimeSessionDeps(
                    context = context,
                    scope = scope,
                    networkSettingsStorage = networkSettingsStorage,
                    remoteControllerStore = remoteControllerStore,
                    queryTrafficNowAction = { coreOps.queryTrafficNow() },
                    queryTrafficTotalAction = { coreOps.queryTrafficTotal() },
                    onAfterRunning = {
                        preview.stop()
                        if (AppVisibilityTracker.isForeground.value) {
                            refreshAllSafely()
                        }
                    },
                    onAfterIdle = {
                        if (AppVisibilityTracker.isForeground.value) {
                            refreshPreviewStateSafely()
                        }
                    },
                    onGroupTick = { groups.refreshSafely() },
                    onTrafficTickExtra = { tick ->
                        if (
                            tick % RUNTIME_PAYLOAD_REFRESH_TICKS == 0 &&
                                session.shouldRefreshRuntimePayload(groups.isGroupsEmpty())
                        ) {
                            refreshAllSafely()
                        }
                    },
                    onClearGroups = { reset -> groups.clear(reset) },
                    probeRemote = { RuntimeAccess.probeRemoteController() },
                )
            )
        groups =
            RuntimeGroupHub(
                scope = scope,
                session = session,
                coreOps = coreOps,
                isRemoteControllerActive = { session.isRemoteControllerActive() },
            )
        runtimeSnapshot = session.runtimeSnapshot
        isRunning = session.isRunning
        isConfigReloading = session.isConfigReloading
        currentProfile = session.currentProfile
        trafficNow = session.trafficNow
        trafficTotal = session.trafficTotal
        proxyGroups =
            nodeSession
                .map { state -> state.groups }
                .stateIn(scope, SharingStarted.Eagerly, emptyList())
        resolvedPrimaryNode = groups.resolvedPrimaryNode
        session.bootstrap()
        observeNodeSession()
        observePreviewRuntime()
        observeProxyGroupSyncPriority()
        observeAppVisibility()
    }

    fun isRemoteControllerActive(): Boolean = session.isRemoteControllerActive()

    fun applyRemoteControllerState() = session.applyRemoteControllerState()

    fun setProxyGroupSyncPriority(
        priority: ProxyGroupSyncPriority,
        source: String = DEFAULT_SYNC_PRIORITY_SOURCE,
    ) {
        syncPriorityRequests.update { current ->
            if (priority == ProxyGroupSyncPriority.OFF) {
                current - source
            } else {
                current + (source to priority)
            }
        }
    }

    fun warmUpProxyGroups() {
        if (previewWarmupJob?.isActive == true) return
        previewWarmupJob = launchPreviewWarmup()
    }

    suspend fun awaitProxyGroupWarmUp() {
        val existing = previewWarmupJob
        if (existing?.isCompleted == true) return
        val job = existing?.takeIf { it.isActive } ?: launchPreviewWarmup().also { previewWarmupJob = it }
        job.join()
    }

    suspend fun reconcileRuntimeState() = session.reconcile()

    suspend fun reloadProxy(mode: RunMode = networkSettingsStorage.runMode.value) {
        session.reload(mode)
    }

    suspend fun startProxy(request: RuntimeStartRequest) {
        preview.stop()
        try {
            session.start(request)
        } catch (error: Throwable) {
            resumePreviewWhenEligible()
            throw error
        }
    }

    suspend fun startProxy(mode: RunMode = networkSettingsStorage.runMode.value) =
        startProxy(RuntimeStartRequest(mode = mode))

    suspend fun stopProxy(request: RuntimeStopRequest) {
        try {
            session.stop(request)
        } finally {
            resumePreviewWhenEligible()
        }
    }

    suspend fun stopProxy(reason: String? = null) = stopProxy(RuntimeStopRequest(reason = reason))

    suspend fun selectProxy(group: String, proxyName: String): Boolean =
        if (nodeSession.value.source == NodeDataSource.Active) {
            groups.selectProxy(group, proxyName)
        } else {
            false
        }

    suspend fun healthCheck(
        group: String,
        onProgress: ProxyDelayTestProgressCallback? = null,
    ) {
        when (nodeSession.value.source) {
            NodeDataSource.Active -> groups.healthCheck(group, onProgress)
            NodeDataSource.Preview -> preview.healthCheck(group, onProgress)
            NodeDataSource.None -> Unit
        }
    }

    suspend fun healthCheckAll(onProgress: ProxyDelayTestProgressCallback? = null) {
        when (nodeSession.value.source) {
            NodeDataSource.Active -> groups.healthCheckAll(onProgress)
            NodeDataSource.Preview -> preview.healthCheckAll(onProgress)
            NodeDataSource.None -> Unit
        }
    }

    suspend fun healthCheckProxy(group: String, proxyName: String): Int =
        when (nodeSession.value.source) {
            NodeDataSource.Active -> groups.healthCheckProxy(group, proxyName)
            NodeDataSource.Preview -> preview.healthCheckProxy(group, proxyName)
            NodeDataSource.None -> 0
        }

    suspend fun queryConnections(): ConnectionSnapshot {
        if (!session.snapshotValue().running) {
            return ConnectionSnapshot()
        }
        return coreOps.queryConnections()
    }

    suspend fun queryTrafficTotal(): Long = session.queryTrafficTotal {
        coreOps.queryTrafficTotal()
    }

    suspend fun queryTrafficNow(): Long = session.queryTrafficNow { coreOps.queryTrafficNow() }

    suspend fun refreshProxyGroups() {
        if (shouldUsePreviewRuntime()) {
            preview.ensureRunning()
        } else {
            groups.refreshProxyGroups()
        }
    }

    suspend fun refreshProxyGroup(name: String, sort: ProxySort = ProxySort.Default) {
        if (nodeSession.value.source == NodeDataSource.Preview) {
            preview.refreshGroup(name, sort)
        } else {
            groups.refreshProxyGroup(name, sort)
        }
    }

    suspend fun refreshCurrentProfile() {
        if (isRemoteControllerActive()) {
            session.setCurrentProfile(null)
            return
        }
        runCatching {
            session.connectBackend()
            session.setCurrentProfile(RuntimeAccess.profile().queryActive())
        }
            .onFailure { error ->
                if (error is CancellationException) throw error
                Timber.e(error, "Failed to refresh current profile")
            }
    }

    suspend fun refreshAll() {
        refreshCurrentProfile()
        refreshProxyGroups()
        queryTrafficNow()
        queryTrafficTotal()
    }

    private fun launchPreviewWarmup(): Job = scope.launch {
        runCatching { refreshProxyGroups() }
            .onFailure { error ->
                if (error is CancellationException) throw error
                Timber.d(error, "Warm up proxy groups skipped")
            }
    }

    private suspend fun refreshAllSafely() {
        if (!session.snapshotValue().servesNodes) return
        runCatching { refreshAll() }
            .onFailure { error ->
                if (error is CancellationException) throw error
                Timber.d(error, "Refresh runtime data skipped")
            }
    }

    private suspend fun refreshPreviewStateSafely() {
        runCatching {
            refreshCurrentProfile()
            resumePreviewWhenEligible()
        }
            .onFailure { error ->
                if (error is CancellationException) throw error
                Timber.d(error, "Refresh preview data skipped")
            }
    }

    private fun observeProxyGroupSyncPriority() {
        scope.launch {
            combine(
                session.runtimeSnapshot,
                syncPriorityRequests,
                AppVisibilityTracker.isForeground,
            ) { snapshot, requests, isForeground ->
                    resolveEffectiveProxyGroupSyncPriority(snapshot, requests, isForeground)
                }
                .distinctUntilChanged()
                .collect { priority -> session.startGroupPolling(priority) }
        }
    }

    private fun observeAppVisibility() {
        scope.launch {
            AppVisibilityTracker.isForeground
                .collect { isForeground ->
                    if (isForeground) {
                        session.reconcile()
                    } else {
                        session.stopTrafficPolling()
                    }
                }
        }
    }

    private fun observePreviewRuntime() {
        scope.launch {
            combine(
                    AppVisibilityTracker.isForeground,
                    session.runtimeSnapshot,
                ) { foreground, snapshot ->
                    foreground to snapshot
                }
                .collectLatest { (foreground, snapshot) ->
                    val realCoreOwnsRuntime =
                        snapshot.phase.isActiveOrStopping ||
                            snapshot.owner == RuntimeOwner.RemoteController
                    when {
                        !foreground || realCoreOwnsRuntime -> preview.stop()
                        !preview.hasActiveProfile() -> preview.reset()
                        else ->
                            runCatching { preview.ensureRunning() }
                                .onFailure { error ->
                                    if (error is CancellationException) throw error
                                    Timber.d(error, "Preview runtime unavailable")
                                }
                    }
                }
        }
    }

    private fun observeNodeSession() {
        scope.launch {
            combine(
                    session.currentProfile,
                    session.runtimeSnapshot,
                    groups.groups,
                    preview.state,
                ) { profile, snapshot, activeGroups, previewState ->
                    NodeInputs(profile != null, snapshot, activeGroups, previewState.groups, previewState.ready)
                }
                .collect { input ->
                    val previewGroups = input.previewGroups.map(ProxyGroup::toInfo)
                    when {
                        input.activeGroups.isNotEmpty() && input.snapshot.servesNodes ->
                            publishNodeSession(NodeDataSource.Active, input.activeGroups)
                        input.previewReady && previewGroups.isNotEmpty() ->
                            publishNodeSession(NodeDataSource.Preview, previewGroups)
                        !input.hasProfile && input.snapshot.owner != RuntimeOwner.RemoteController ->
                            _nodeSession.value = NodeSessionState()
                        _nodeSession.value.everReady ->
                            _nodeSession.value = _nodeSession.value.copy(available = false)
                    }
                }
        }
    }

    private suspend fun resumePreviewWhenEligible() {
        if (AppVisibilityTracker.isForeground.value && shouldUsePreviewRuntime()) {
            preview.ensureRunning()
        }
    }

    /** The first node refresh happens before [NodeDataSource.Preview] can be published. */
    private fun shouldUsePreviewRuntime(): Boolean =
        !session.snapshotValue().phase.isActiveOrStopping &&
            !session.isRemoteControllerActive() &&
            preview.hasActiveProfile()

    /** Callers only publish non-empty groups. */
    private fun publishNodeSession(source: NodeDataSource, nodeGroups: List<ProxyGroupInfo>) {
        _nodeSession.value =
            NodeSessionState(source = source, groups = nodeGroups, available = true, everReady = true)
    }

    private data class NodeInputs(
        val hasProfile: Boolean,
        val snapshot: RuntimeSnapshot,
        val activeGroups: List<ProxyGroupInfo>,
        val previewGroups: List<ProxyGroup>,
        val previewReady: Boolean,
    )

    private fun resolveEffectiveProxyGroupSyncPriority(
        snapshot: RuntimeSnapshot,
        requests: Map<String, ProxyGroupSyncPriority>,
        isForeground: Boolean,
    ): ProxyGroupSyncPriority {
        if (!isForeground || !snapshot.servesNodes) return ProxyGroupSyncPriority.OFF
        return requests.values.maxByOrNull { it.ordinal } ?: ProxyGroupSyncPriority.OFF
    }
}
