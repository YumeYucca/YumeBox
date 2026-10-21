/*
 * This file is part of YumeBox.
 *
 * Copyright (c) YumeYucca 2025 - Present
 */

package com.github.yumeyucca.yumebox.runtime.service.preview

import android.content.Context
import com.github.yumeyucca.yumebox.core.model.Proxy
import com.github.yumeyucca.yumebox.core.model.ProxyGroup
import com.github.yumeyucca.yumebox.core.model.ProxySort
import com.github.yumeyucca.yumebox.domain.model.ProxyDelayStore
import com.github.yumeyucca.yumebox.domain.model.ProxyDelayTestProgressCallback
import com.github.yumeyucca.yumebox.domain.model.ProxyDelayTestSession
import com.github.yumeyucca.yumebox.domain.model.ProxyGroupOverlay
import com.github.yumeyucca.yumebox.domain.model.membersByGroup
import com.github.yumeyucca.yumebox.domain.model.planProxyDelayTests
import com.github.yumeyucca.yumebox.domain.model.withDelays
import com.github.yumeyucca.yumebox.runtime.service.config.ServiceStore
import com.github.yumeyucca.yumebox.runtime.service.core.PreviewCoreProcess
import com.github.yumeyucca.yumebox.runtime.service.profile.ImportedDao
import com.github.yumeyucca.yumebox.runtime.service.session.CompiledConfigPipeline
import com.github.yumeyucca.yumebox.runtime.service.session.SessionRuntimeSpecFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicLong

/** Snapshot published by the inspect-only process. Empty groups are a valid, but non-displayable, config. */
data class PreviewNodeState(
    val fingerprint: String = "",
    val groups: List<ProxyGroup> = emptyList(),
    val ready: Boolean = false,
)

/**
 * Foreground-only owner for the preview shell. Callers explicitly suspend it before a real core
 * launch and resume it after an idle transition; this keeps preview independent from VPN/Root
 * ownership and makes the handoff observable instead of racing two controllers.
 */
class PreviewRuntimeManager(
    context: Context,
    private val delayStore: ProxyDelayStore,
) {
    private val context = context.applicationContext
    private val factory = SessionRuntimeSpecFactory(this.context)
    private val pipeline = CompiledConfigPipeline(this.context)
    private val process = PreviewCoreProcess(this.context)
    private val serviceStore = ServiceStore()
    private val mutex = Mutex()
    private val generation = AtomicLong(0L)
    private val _state = MutableStateFlow(PreviewNodeState())
    private val delayOverlay = ProxyGroupOverlay()
    private var indexedGroups: List<ProxyGroup>? = null
    private var proxyToGroupIndex: Map<String, List<Int>> = emptyMap()
    val state: StateFlow<PreviewNodeState> = _state.asStateFlow()

    /** Local storage only: preview must not depend on the service/controller backend being alive. */
    fun hasActiveProfile(): Boolean =
        serviceStore.activeProfile?.let(ImportedDao::queryByUUID) != null

    suspend fun ensureRunning() {
        val requestGeneration = generation.get()
        mutex.withLock {
            if (generation.get() != requestGeneration) return@withLock
            // Startup, the home page, and the node page can request the first snapshot together.
            // Compile under the same transaction as launch/readiness so they reuse one result.
            val compiled = pipeline.compile(factory.createPreviewSpec())
            if (generation.get() != requestGeneration) return@withLock
            // Several UI surfaces ask for the initial node snapshot at once. Keep the launch and
            // first controller read in one transaction: otherwise a second caller can replace a
            // still-booting PID before it has created preview.sock.
            if (process.isAlive() && _state.value.ready && _state.value.fingerprint == compiled.fingerprint) {
                return@withLock
            }
            if (!process.isAlive() || _state.value.fingerprint != compiled.fingerprint) {
                delayOverlay.clear()
                process.start(compiled.finalYaml)
            }
            val groups = awaitGroups(requestGeneration)
            if (generation.get() == requestGeneration && process.isAlive()) {
                _state.value =
                    PreviewNodeState(fingerprint = compiled.fingerprint, groups = groups, ready = true)
                delayStore.publishProxyGroups(groups)
            }
        }
    }

    /** Never wait for config compilation or a controller readiness retry on the real-core handoff. */
    fun stop() {
        generation.incrementAndGet()
        delayOverlay.clear()
        process.stop()
    }

    fun reset() {
        stop()
        _state.value = PreviewNodeState()
    }

    /** Preview is read-only for selection, but mihomo's delay probes are safe and useful here. */
    suspend fun healthCheck(group: String, onProgress: ProxyDelayTestProgressCallback? = null) =
        runDelayTests(onProgress) { listOf(group) }

    suspend fun healthCheckAll(onProgress: ProxyDelayTestProgressCallback? = null) =
        runDelayTests(onProgress) { proxiesByGroup -> proxiesByGroup.keys.toList() }

    suspend fun healthCheckProxy(proxyName: String): Int {
        val previewGeneration = generation.get()
        return mutex.withLock {
            withContext(Dispatchers.Default) {
                if (!isCurrentGeneration(previewGeneration)) return@withContext -1
                val measured = process.controller().healthCheckProxy(proxyName)
                val batch = mapOf(proxyName to measured)
                recordDirectDelays(previewGeneration, batch)
                commitDirectDelays(previewGeneration, batch)
                measured
            }
        }
    }

    suspend fun refreshGroup(name: String, sort: ProxySort) {
        val previewGeneration = generation.get()
        mutex.withLock {
            if (!isCurrentGeneration(previewGeneration)) return@withLock
            val refreshed =
                mergeReportedDelays(listOf(process.controller().queryProxyGroup(name, sort))).first()
            if (!isCurrentGeneration(previewGeneration)) return@withLock
            val previous = _state.value
            val merged =
                previous.groups.let { groups ->
                    if (groups.none { it.name == name }) groups + refreshed
                    else groups.map { group -> if (group.name == name) refreshed else group }
                }
            _state.value = previous.copy(groups = merged, ready = true)
            delayStore.publishProxyGroups(merged)
        }
    }

    private suspend fun refreshGroups(previewGeneration: Long) {
        val incoming = process.controller().queryAllProxyGroups(false)
        if (!isCurrentGeneration(previewGeneration)) {
            return
        }
        val previous = _state.value
        val merged = mergeReportedDelays(incoming)
        _state.value = previous.copy(groups = merged, ready = true)
        delayStore.publishProxyGroups(merged)
    }

    /** Keep a direct delay probe from being overwritten by the controller's lagging history. */
    private fun mergeReportedDelays(incoming: List<ProxyGroup>): List<ProxyGroup> {
        delayOverlay.prune(membersByGroup(incoming.map { group -> group.name to group.proxies }))
        val previousByGroup = _state.value.groups.associateBy(ProxyGroup::name)
        return incoming.map { group ->
            val previousByProxy = previousByGroup[group.name]?.proxies?.associateBy { it.name }
            group.copy(
                proxies =
                    group.proxies.map { proxy ->
                        val previousDelay = previousByProxy?.get(proxy.name)?.delay
                        proxy.copy(
                            delay =
                                delayOverlay.mergeDelay(
                                    groupName = group.name,
                                    proxyName = proxy.name,
                                    reportedDelay = proxy.delay,
                                    previousDelay = previousDelay,
                                )
                        )
                    }
            )
        }.let(delayOverlay::paintProxyGroups)
    }

    /** Refreshes the groups under the lock, then probes outside it so selection reads stay live. */
    private suspend fun runDelayTests(
        onProgress: ProxyDelayTestProgressCallback?,
        groupNames: (proxiesByGroup: Map<String, List<Proxy>>) -> List<String>,
    ) {
        val previewGeneration = generation.get()
        val (controller, proxiesByGroup) =
            withContext(Dispatchers.Default) {
                mutex.withLock {
                    if (!isCurrentGeneration(previewGeneration)) return@withContext null
                    refreshGroups(previewGeneration)
                    if (!isCurrentGeneration(previewGeneration)) return@withContext null
                    process.controller() to _state.value.groups.associate { it.name to it.proxies }
                }
            } ?: return
        withContext(Dispatchers.IO) {
            runCatching { controller.warmUpProviderSnapshot() }
                .onFailure { error -> if (error is CancellationException) throw error }
        }
        val measured =
            withContext(Dispatchers.Default) {
                ProxyDelayTestSession(
                    targets = planProxyDelayTests(groupNames(proxiesByGroup), proxiesByGroup),
                    isActive = { isCurrentGeneration(previewGeneration) },
                    onResult = { batch -> recordDirectDelays(previewGeneration, batch) },
                    measureNode = { proxyName -> controller.healthCheckProxy(proxyName) },
                    measureGroup = { groupName -> controller.healthCheck(groupName) },
                    onProgress = onProgress,
                )
                    .run()
            } ?: return
        mutex.withLock { commitDirectDelays(previewGeneration, measured) }
    }

    /** Publishes one measured batch to the live flows and shields it from stale snapshots. */
    @Synchronized
    private fun recordDirectDelays(
        previewGeneration: Long,
        delays: Map<String, Int>,
    ) {
        if (!isCurrentGeneration(previewGeneration)) return
        val measured = delays.filterValues { delay -> delay != 0 }
        if (measured.isEmpty()) return
        val currentGroups = _state.value.groups
        if (currentGroups.isNotEmpty()) {
            val index = proxyGroupIndex(currentGroups)
            val affectedMembers = HashMap<String, MutableSet<String>>()
            measured.keys.forEach { name ->
                index[name]?.forEach { groupIndex ->
                    affectedMembers.getOrPut(currentGroups[groupIndex].name) { mutableSetOf() } +=
                        name
                }
            }
            if (affectedMembers.isNotEmpty()) delayOverlay.recordDelays(measured, affectedMembers)
        }
        delayStore.apply(measured)
    }

    private fun proxyGroupIndex(groups: List<ProxyGroup>): Map<String, List<Int>> {
        if (indexedGroups !== groups) {
            val index = HashMap<String, MutableList<Int>>()
            groups.forEachIndexed { groupIndex, group ->
                group.proxies.forEach { proxy ->
                    index.getOrPut(proxy.name) { mutableListOf() } += groupIndex
                }
            }
            indexedGroups = groups
            proxyToGroupIndex = index
        }
        return proxyToGroupIndex
    }

    /** Folds a finished run into the immutable preview group list exactly once, then republishes. */
    @Synchronized
    private fun commitDirectDelays(
        previewGeneration: Long,
        measured: Map<String, Int>,
    ) {
        if (!isCurrentGeneration(previewGeneration)) return
        val previous = _state.value
        val merged =
            if (measured.isEmpty()) {
                previous.groups
            } else {
                previous.groups.map { group ->
                    group.copy(proxies = group.proxies.withDelays(measured))
                }
            }
        val painted = delayOverlay.paintProxyGroups(merged)
        _state.value = previous.copy(groups = painted)
        delayStore.publishProxyGroups(painted)
    }

    private fun isCurrentGeneration(previewGeneration: Long): Boolean =
        generation.get() == previewGeneration && process.isAlive()

    private suspend fun awaitGroups(requestGeneration: Long): List<ProxyGroup> =
        withTimeout(CONTROLLER_READY_TIMEOUT_MS) {
            var groups: List<ProxyGroup>? = null
            while (groups == null) {
                if (generation.get() != requestGeneration || !process.isAlive()) {
                    throw CancellationException("preview handoff requested")
                }
                groups = runCatching { process.controller().queryAllProxyGroups(false) }.getOrNull()
                if (groups == null) delay(CONTROLLER_RETRY_MS)
            }
            groups
        }

    private companion object {
        const val CONTROLLER_READY_TIMEOUT_MS = 8_000L
        const val CONTROLLER_RETRY_MS = 120L
    }
}
