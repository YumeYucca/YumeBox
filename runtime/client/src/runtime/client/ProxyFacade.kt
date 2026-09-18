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
        previewWarmupJob?.let { existing ->
            when {
                existing.isActive -> {
                    existing.join()
                    return
                }

                existing.isCompleted -> return
            }
        }
        val job = launchPreviewWarmup()
        previewWarmupJob = job
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
        startProxy(
            RuntimeStartRequest(
                owner = session.ownership.ownerForMode(mode),
                mode = mode,
            )
        )

    suspend fun stopProxy(request: RuntimeStopRequest) {
        try {
            session.stop(request)
        } finally {
            resumePreviewWhenEligible()
        }
    }

    suspend fun stopProxy(mode: RunMode? = null) =
        stopProxy(RuntimeStopRequest(targetMode = mode ?: networkSettingsStorage.runMode.value))

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
            session.updateProfileReady(null)
            return
        }
        runCatching {
            session.connectBackend()
            val profile = RuntimeAccess.profile().queryActive()
            session.setCurrentProfile(profile)
            session.updateProfileReady(profile)
        }
            .onFailure { error ->
                if (error is CancellationException) throw error
                Timber.e(error, "Failed to refresh current profile")
            }
    }

    suspend fun refreshAll() {
        refreshCurrentProfile()
        refreshProxyGroups()
        if (session.snapshotValue().phase == RuntimePhase.Running) {
            queryTrafficNow()
            queryTrafficTotal()
        } else {
            session.setTrafficNow(0L)
            session.setTrafficTotal(0L)
        }
    }

    private fun launchPreviewWarmup(): Job = scope.launch {
        runCatching { refreshProxyGroups() }
            .onFailure { error ->
                if (error is CancellationException) throw error
                Timber.d(error, "Warm up proxy groups skipped")
            }
    }

    private suspend fun refreshAllSafely() {
        val snapshot = session.snapshotValue()
        if (
            snapshot.phase != RuntimePhase.Running &&
                snapshot.owner != RuntimeOwner.RemoteController
        ) {
            return
        }
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
                        session.reconcileAndRefresh()
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
                    val activeAvailable =
                        input.activeGroups.isNotEmpty() &&
                            (input.snapshot.phase == RuntimePhase.Running ||
                                input.snapshot.owner == RuntimeOwner.RemoteController)
                    val previewGroups = input.previewGroups.map(::toProxyGroupInfo)
                    when {
                        activeAvailable ->
                            publishNodeSession(NodeDataSource.Active, input.activeGroups, available = true)
                        input.previewReady && previewGroups.isNotEmpty() ->
                            publishNodeSession(NodeDataSource.Preview, previewGroups, available = true)
                        !input.hasProfile && input.snapshot.owner != RuntimeOwner.RemoteController ->
                            _nodeSession.value = NodeSessionState()
                        _nodeSession.value.everReady ->
                            _nodeSession.value = _nodeSession.value.copy(available = false)
                    }
                }
        }
    }

    private suspend fun resumePreviewWhenEligible() {
        if (
            AppVisibilityTracker.isForeground.value &&
                !session.snapshotValue().phase.isActiveOrStopping &&
                !session.isRemoteControllerActive() &&
                preview.hasActiveProfile()
        ) {
            preview.ensureRunning()
        }
    }

    /** The first node refresh happens before [NodeDataSource.Preview] can be published. */
    private fun shouldUsePreviewRuntime(): Boolean =
        !session.snapshotValue().phase.isActiveOrStopping &&
            !session.isRemoteControllerActive() &&
            preview.hasActiveProfile()

    private fun publishNodeSession(
        source: NodeDataSource,
        nodeGroups: List<ProxyGroupInfo>,
        available: Boolean,
    ) {
        _nodeSession.value =
            NodeSessionState(
                source = source,
                groups = nodeGroups,
                available = available,
                everReady = nodeGroups.isNotEmpty() || _nodeSession.value.everReady,
            )
    }

    private fun toProxyGroupInfo(group: ProxyGroup): ProxyGroupInfo =
        ProxyGroupInfo(
            name = group.name,
            type = group.type,
            proxies = group.proxies,
            now = group.now.trim(),
            icon = group.icon,
            hidden = group.hidden,
        )

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
        if (
            !isForeground ||
            snapshot.phase != RuntimePhase.Running &&
                snapshot.owner != RuntimeOwner.RemoteController
        ) {
            return ProxyGroupSyncPriority.OFF
        }
        return requests.values.maxByOrNull { it.ordinal } ?: ProxyGroupSyncPriority.OFF
    }

}
