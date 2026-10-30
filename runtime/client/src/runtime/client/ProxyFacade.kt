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
import com.github.yumeyucca.yumebox.domain.model.ProxyDelayStore
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
    ;

    /**
     * The more urgent of the two. The constants are declared in ascending urgency, so the ordering
     * lives here instead of in a call-site `ordinal` comparison that a reordering would silently flip.
     */
    fun max(other: ProxyGroupSyncPriority): ProxyGroupSyncPriority =
        if (ordinal >= other.ordinal) this else other
}

/**
 * Who currently wants proxy groups synced. Requests are merged per source: the highest requested
 * [ProxyGroupSyncPriority] wins, and OFF drops that source's entry so a screen that went away
 * cannot leave a stale FAST behind.
 */
enum class ProxyGroupSyncSource {
    /** The app-wide subscription (home screen). */
    Home,

    /** The proxy page, or the left pane of the phone split view. */
    Page,

    /** The node list in the right pane of the phone split view. */
    Detail,

    /** The proxy sheet overlay. */
    Sheet,
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
    }

    private val appContext: Context = context.appContextOrSelf
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val delayStore = ProxyDelayStore()
    private val session: RuntimeSession
    private val coreOps = RuntimeCoreOps(connect = { session.connectBackend() })
    private val groups: RuntimeGroupHub
    private val preview = PreviewRuntimeManager(appContext, delayStore)
    private val _nodeSession = MutableStateFlow(NodeSessionState())

    private var previewWarmupJob: Job? = null
    private val syncPriorityRequests =
        MutableStateFlow<Map<ProxyGroupSyncSource, ProxyGroupSyncPriority>>(emptyMap())

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
                delayStore = delayStore,
            )
        runtimeSnapshot = session.runtimeSnapshot
        isRunning =
            session.runtimeSnapshot
                .map { snapshot -> snapshot.running }
                .stateIn(scope, SharingStarted.Eagerly, false)
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

    fun proxyDelayFlow(name: String) = delayStore.delayFlow(name)

    fun applyRemoteControllerState() = session.applyRemoteControllerState()

    fun setProxyGroupSyncPriority(
        priority: ProxyGroupSyncPriority,
        source: ProxyGroupSyncSource = ProxyGroupSyncSource.Home,
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
        // Stopping the inspect-only process kills and reaps a real process, and this is called from
        // the UI scope while the node list is on screen.
        withContext(Dispatchers.IO) { preview.stop() }
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
    ) = onNodeOwner(
        onActive = { groups.healthCheck(group, onProgress) },
        onPreview = { preview.healthCheck(group, onProgress) },
        onNone = {},
    )

    suspend fun healthCheckAll(onProgress: ProxyDelayTestProgressCallback? = null) = onNodeOwner(
        onActive = { groups.healthCheckAll(onProgress) },
        onPreview = { preview.healthCheckAll(onProgress) },
        onNone = {},
    )

    suspend fun healthCheckProxy(proxyName: String): Int =
        onNodeOwner(
            onActive = { groups.healthCheckProxy(proxyName) },
            onPreview = { preview.healthCheckProxy(proxyName) },
            onNone = { 0 },
        )

    /**
     * Sends a node operation to whichever backend currently publishes the node list: the real
     * runtime, or the preview runtime that stands in while the real one is down.
     */
    private inline fun <T> onNodeOwner(
        onActive: () -> T,
        onPreview: () -> T,
        onNone: () -> T,
    ): T =
        when (_nodeSession.value.source) {
            NodeDataSource.Active -> onActive()
            NodeDataSource.Preview -> onPreview()
            NodeDataSource.None -> onNone()
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
        bestEffort("Failed to refresh current profile") {
            session.connectBackend()
            session.setCurrentProfile(RuntimeAccess.profile().queryActive())
        }
    }

    suspend fun refreshAll() {
        refreshCurrentProfile()
        refreshProxyGroups()
        queryTrafficNow()
        queryTrafficTotal()
    }

    private fun launchPreviewWarmup(): Job = scope.launch {
        bestEffort("Warm up proxy groups skipped") { refreshProxyGroups() }
    }

    private suspend fun refreshAllSafely() {
        if (!session.snapshotValue().servesNodes) return
        bestEffort("Refresh runtime data skipped") { refreshAll() }
    }

    private suspend fun refreshPreviewStateSafely() {
        bestEffort("Refresh preview data skipped") {
            refreshCurrentProfile()
            resumePreviewWhenEligible()
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

    /**
     * Runs a best-effort runtime step. Cancellation always propagates; every other failure is logged
     * with [message] and dropped, because the next tick or the next visibility change retries it.
     */
    private inline fun bestEffort(message: String, block: () -> Unit) {
        runCatching(block).onFailure { error ->
            if (error is CancellationException) throw error
            Timber.w(error, message)
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
                    // The profile probe is a SQLite read; keep it off whatever thread published the
                    // snapshot, since this loop also runs while the node page is on screen.
                    val hasActiveProfile = withContext(Dispatchers.IO) { preview.hasActiveProfile() }
                    when {
                        !foreground || realCoreOwnsRuntime -> withContext(Dispatchers.IO) { preview.stop() }
                        !hasActiveProfile -> preview.reset()
                        else -> bestEffort("Preview runtime unavailable") { preview.ensureRunning() }
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

    /**
     * Whether the node list should be served by the inspect-only process.
     *
     * The profile check is a SQLite read, and every caller is a UI coroutine, so it hops to IO
     * instead of trusting the caller's dispatcher.
     */
    private suspend fun shouldUsePreviewRuntime(): Boolean =
        !session.snapshotValue().phase.isActiveOrStopping &&
            !session.isRemoteControllerActive() &&
            withContext(Dispatchers.IO) { preview.hasActiveProfile() }

    /** Callers only publish non-empty groups. */
    private fun publishNodeSession(source: NodeDataSource, nodeGroups: List<ProxyGroupInfo>) {
        // Compared against the published session itself: a mirror field survives the reset below and
        // then skips a first publish after the session comes back with the same source.
        if (source != _nodeSession.value.source) {
            delayStore.publish(nodeGroups)
        }
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
        requests: Map<ProxyGroupSyncSource, ProxyGroupSyncPriority>,
        isForeground: Boolean,
    ): ProxyGroupSyncPriority {
        if (!isForeground || !snapshot.servesNodes) return ProxyGroupSyncPriority.OFF
        return requests.values.reduceOrNull(ProxyGroupSyncPriority::max) ?: ProxyGroupSyncPriority.OFF
    }
}
