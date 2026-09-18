/*
 * This file is part of YumeBox.
 *
 * Copyright (c) YumeYucca 2025 - Present
 */

package com.github.yumeyucca.yumebox.runtime.service.preview

import android.content.Context
import com.github.yumeyucca.yumebox.core.model.ProxyGroup
import com.github.yumeyucca.yumebox.core.model.ProxySort
import com.github.yumeyucca.yumebox.domain.model.ProxyDelayOverlay
import com.github.yumeyucca.yumebox.domain.model.ProxyDelayPublishCoalescer
import com.github.yumeyucca.yumebox.domain.model.ProxyDelayTestProgressCallback
import com.github.yumeyucca.yumebox.domain.model.ProxyGroupInfo
import com.github.yumeyucca.yumebox.domain.model.countPlannedProxyDelayTests
import com.github.yumeyucca.yumebox.domain.model.countPlannedProxyDelayTestsForGroup
import com.github.yumeyucca.yumebox.domain.model.proxyMembersByGroup
import com.github.yumeyucca.yumebox.domain.model.resolveTestableProxyNames
import com.github.yumeyucca.yumebox.domain.model.runParallelProxyDelayTests
import com.github.yumeyucca.yumebox.domain.model.withProxyDelayTestProgress
import com.github.yumeyucca.yumebox.runtime.service.controller.CoreController
import com.github.yumeyucca.yumebox.runtime.service.core.PreviewCoreProcess
import com.github.yumeyucca.yumebox.runtime.service.config.ServiceStore
import com.github.yumeyucca.yumebox.runtime.service.profile.ImportedDao
import com.github.yumeyucca.yumebox.runtime.service.session.CompiledConfigPipeline
import com.github.yumeyucca.yumebox.runtime.service.session.SessionRuntimeSpecFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
class PreviewRuntimeManager(context: Context) {
    private val context = context.applicationContext
    private val factory = SessionRuntimeSpecFactory(this.context)
    private val pipeline = CompiledConfigPipeline(this.context)
    private val process = PreviewCoreProcess(this.context)
    private val serviceStore = ServiceStore()
    private val mutex = Mutex()
    private val generation = AtomicLong(0L)
    private val _state = MutableStateFlow(PreviewNodeState())
    private val delayOverlay = ProxyDelayOverlay()
    private val delayPublish = ProxyDelayPublishCoalescer()
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
            val compiled = pipeline.compileDetailed(factory.createPreviewSpec())
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
        generation.incrementAndGet()
        delayOverlay.clear()
        process.stop()
        _state.value = PreviewNodeState()
    }

    /** Preview is read-only for selection, but mihomo's delay probes are safe and useful here. */
    suspend fun healthCheck(
        group: String,
        onProgress: ProxyDelayTestProgressCallback? = null,
    ) {
        val previewGeneration = generation.get()
        val snapshot = prepareHealthCheckSnapshot(previewGeneration) ?: return
        val total =
            countPlannedProxyDelayTestsForGroup(
                groupName = group,
                proxiesByGroup = snapshot.proxiesByGroup,
            )
        delayPublish.session(
            flush = { delays -> applyDirectDelays(previewGeneration, delays) },
        ) {
            withProxyDelayTestProgress(total = total, onProgress = onProgress) { onStep ->
                healthCheckGroup(
                    previewGeneration = previewGeneration,
                    controller = snapshot.controller,
                    groupName = group,
                    proxiesByGroup = snapshot.proxiesByGroup,
                    onStep = onStep,
                )
            }
        }
    }

    suspend fun healthCheckAll(onProgress: ProxyDelayTestProgressCallback? = null) {
        val previewGeneration = generation.get()
        val snapshot = prepareHealthCheckSnapshot(previewGeneration) ?: return
        val total =
            countPlannedProxyDelayTests(
                groups = snapshot.groupInfos,
                proxiesByGroup = snapshot.proxiesByGroup,
            )
        delayPublish.session(
            flush = { delays -> applyDirectDelays(previewGeneration, delays) },
        ) {
            withProxyDelayTestProgress(total = total, onProgress = onProgress) { onStep ->
                val testedProxyNames = linkedSetOf<String>()
                var ok = true
                for (group in snapshot.groupInfos) {
                    if (
                        !healthCheckGroup(
                            previewGeneration = previewGeneration,
                            controller = snapshot.controller,
                            groupName = group.name,
                            proxiesByGroup = snapshot.proxiesByGroup,
                            testedProxyNames = testedProxyNames,
                            onStep = onStep,
                        )
                    ) {
                        ok = false
                        break
                    }
                }
                ok
            }
        }
    }

    suspend fun healthCheckProxy(group: String, proxyName: String): Int {
        val previewGeneration = generation.get()
        return mutex.withLock {
            if (!isCurrentGeneration(previewGeneration)) return@withLock -1
            val delay = process.controller().healthCheckProxy(group, proxyName)
            publishDirectDelays(previewGeneration, mapOf(proxyName to delay))
            delay
        }
    }

    suspend fun refreshGroup(name: String, sort: ProxySort) {
        val previewGeneration = generation.get()
        mutex.withLock {
            if (!isCurrentGeneration(previewGeneration)) return@withLock
            val refreshed =
                mergeReportedDelays(listOf(process.controller().queryProxyGroupAsync(name, sort))).first()
            if (!isCurrentGeneration(previewGeneration)) return@withLock
            val previous = _state.value
            val merged =
                previous.groups.let { groups ->
                    if (groups.none { it.name == name }) groups + refreshed
                    else groups.map { group -> if (group.name == name) refreshed else group }
                }
            _state.value = previous.copy(groups = merged, ready = true)
        }
    }

    private suspend fun refreshGroups(previewGeneration: Long) {
        val incoming = process.controller().queryAllProxyGroupsAsync(false)
        if (!isCurrentGeneration(previewGeneration)) {
            return
        }
        val previous = _state.value
        _state.value =
            previous.copy(
                groups = mergeReportedDelays(incoming),
                ready = true,
            )
    }

    /** Keep a direct delay probe from being overwritten by the controller's lagging history. */
    private fun mergeReportedDelays(incoming: List<ProxyGroup>): List<ProxyGroup> {
        delayOverlay.prune(incoming.proxyMembersByGroup())
        val previousByGroup = _state.value.groups.associateBy(ProxyGroup::name)
        return incoming.map { group ->
            val previousByProxy = previousByGroup[group.name]?.proxies?.associateBy { it.name }
            group.copy(
                proxies =
                    group.proxies.map { proxy ->
                        val previousDelay = previousByProxy?.get(proxy.name)?.delay
                        proxy.copy(
                            delay =
                                delayOverlay.merge(
                                    groupName = group.name,
                                    proxyName = proxy.name,
                                    reportedDelay = proxy.delay,
                                    previousDelay = previousDelay,
                                )
                        )
                    }
            )
        }
    }

    private data class PreviewHealthCheckSnapshot(
        val controller: CoreController,
        val groupInfos: List<ProxyGroupInfo>,
        val proxiesByGroup: Map<String, List<com.github.yumeyucca.yumebox.core.model.Proxy>>,
    )

    private suspend fun prepareHealthCheckSnapshot(previewGeneration: Long): PreviewHealthCheckSnapshot? =
        mutex.withLock {
            if (!isCurrentGeneration(previewGeneration)) return@withLock null
            refreshGroups(previewGeneration)
            if (!isCurrentGeneration(previewGeneration)) return@withLock null
            val groups = _state.value.groups
            PreviewHealthCheckSnapshot(
                controller = process.controller(),
                groupInfos =
                    groups.map { group ->
                        ProxyGroupInfo(
                            name = group.name,
                            type = group.type,
                            proxies = group.proxies,
                            now = group.now,
                            icon = group.icon,
                            hidden = group.hidden,
                        )
                    },
                proxiesByGroup = groups.associate { group -> group.name to group.proxies },
            )
        }

    private suspend fun healthCheckGroup(
        previewGeneration: Long,
        controller: CoreController,
        groupName: String,
        proxiesByGroup: Map<String, List<com.github.yumeyucca.yumebox.core.model.Proxy>>,
        testedProxyNames: MutableSet<String> = linkedSetOf(),
        onStep: suspend (Int) -> Unit = {},
    ): Boolean {
        if (!isCurrentGeneration(previewGeneration)) return false
        val proxyNames = resolveTestableProxyNames(groupName, proxiesByGroup)
        if (proxyNames.isEmpty()) {
            val delays = controller.healthCheck(groupName)
            val published = publishDirectDelays(previewGeneration, delays)
            if (published) {
                onStep(delays.size.coerceAtLeast(1))
            }
            return published
        }
        return runParallelProxyDelayTests(
            proxyNames = proxyNames,
            testedProxyNames = testedProxyNames,
            isActive = { isCurrentGeneration(previewGeneration) },
            measure = { proxyName -> controller.healthCheckProxy(groupName, proxyName) },
            publish = { proxyName, delay ->
                publishDirectDelays(previewGeneration, mapOf(proxyName to delay))
            },
            onStep = onStep,
        )
    }

    private suspend fun publishDirectDelays(
        previewGeneration: Long,
        delays: Map<String, Int>,
    ): Boolean {
        if (!isCurrentGeneration(previewGeneration)) return false
        if (delays.none { (_, delay) -> delay != 0 }) return true
        return delayPublish.enqueue(delays) { pending ->
            applyDirectDelays(previewGeneration, pending)
        }
    }

    private fun applyDirectDelays(
        previewGeneration: Long,
        delays: Map<String, Int>,
    ) {
        if (!isCurrentGeneration(previewGeneration)) return
        val measuredDelays = delays.filterValues { delay -> delay != 0 }
        if (measuredDelays.isEmpty()) return
        val previous = _state.value
        val updatedGroups =
            previous.groups.map { group ->
                group.copy(
                    proxies =
                        group.proxies.map { proxy ->
                            measuredDelays[proxy.name]?.let { delay -> proxy.copy(delay = delay) }
                                ?: proxy
                        }
                )
            }
        delayOverlay.record(measuredDelays, updatedGroups.proxyMembersByGroup())
        _state.value = previous.copy(groups = updatedGroups)
    }

    private fun isCurrentGeneration(previewGeneration: Long): Boolean =
        generation.get() == previewGeneration && process.isAlive()

    private suspend fun awaitGroups(requestGeneration: Long): List<ProxyGroup> =
        withTimeout(CONTROLLER_READY_TIMEOUT_MS) {
            while (true) {
                if (generation.get() != requestGeneration || !process.isAlive()) {
                    throw kotlinx.coroutines.CancellationException("preview handoff requested")
                }
                try {
                    return@withTimeout process.controller().queryAllProxyGroupsAsync(false)
                } catch (_: Throwable) {
                    if (generation.get() != requestGeneration || !process.isAlive()) {
                        throw kotlinx.coroutines.CancellationException("preview handoff requested")
                    }
                    delay(CONTROLLER_RETRY_MS)
                }
            }
            @Suppress("UNREACHABLE_CODE")
            error("preview controller retry loop ended unexpectedly")
        }

    private companion object {
        const val CONTROLLER_READY_TIMEOUT_MS = 8_000L
        const val CONTROLLER_RETRY_MS = 120L
    }
}
