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

package com.github.yumeyucca.yumebox.runtime.service.session

import android.os.SystemClock
import com.github.yumeyucca.yumebox.core.model.ProxyGroup
import com.github.yumeyucca.yumebox.core.model.UiConfiguration
import com.github.yumeyucca.yumebox.runtime.api.RuntimeOwner
import com.github.yumeyucca.yumebox.runtime.api.RuntimePhase
import com.github.yumeyucca.yumebox.runtime.api.RuntimeSnapshot
import com.github.yumeyucca.yumebox.runtime.api.appContextOrSelf
import com.github.yumeyucca.yumebox.runtime.service.core.CoreProcess
import com.github.yumeyucca.yumebox.runtime.service.log.RuntimeLog
import kotlinx.coroutines.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

class SessionRuntime(
    private val host: RuntimeHost,
    private val transport: RuntimeTransport,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val compiledConfigPipeline = CompiledConfigPipeline(host.context.appContextOrSelf)

    private val rest
        get() =
            CoreProcess.controller(
                host.context.appContextOrSelf
            )

    private val proxyGroupResolver =
        RuntimeProxyGroupResolver(compiledConfigPipeline, host.context.appContextOrSelf)
    private val lock = Any()
    private val interruptSignal = Semaphore(0)
    private val snapshotLock = Any()

    @Volatile private var interruptReason: String? = null
    @Volatile private var currentSpec: RuntimeSpec? = null
    @Volatile private var currentSnapshot: RuntimeSnapshot = RuntimeSnapshot(runMode = host.mode)
    private val observers = SessionRuntimeObservers(host.context, transport)
    @Volatile private var snapshotRefreshJob: Job? = null
    @Volatile private var coreWatchJob: Job? = null
    private val failureReported = AtomicBoolean(false)
    private val queryCache = SessionRuntimeQueryCache()
    private val telemetry =
        SessionRuntimeTelemetry(host = host, scope = scope) { ready ->
            updateSnapshot { it.copy(logReady = ready) }
        }

    fun start(spec: RuntimeSpec): RuntimeOperationResult = startFresh(spec, name = "start")

    fun reload(spec: RuntimeSpec): RuntimeOperationResult {
        val previous = currentSpec
        return runGuarded(
            spec,
            name = "reload",
            onFailure = { message, error ->
                previous?.let {
                    host.restoreActiveProfile(it.profileUuid, it.profileName)
                    host.onProfileLoaded(it.profileUuid)
                }
                startupError(spec, "reload failed: $message", error)
            },
        ) {
            startupLog(spec, "reload begin")
            reloadInternal(spec)
        }
    }

    fun restart(spec: RuntimeSpec): RuntimeOperationResult = startFresh(spec, name = "restart")

    private fun startFresh(spec: RuntimeSpec, name: String): RuntimeOperationResult =
        runGuarded(
            spec,
            name = name,
            onFailure = { message, error -> rollback(spec, message, error, name) },
        ) {
            failureReported.set(false)
            stopInternal(reason = null, notifyHost = false)
            startupLog(spec, "$name begin")
            startInternal(spec)
        }

    /**
     * Shared interrupt-aware wrapper for the lifecycle entry points: serializes on [lock], treats
     * [RuntimeInterruptedException] as a successful no-op, and routes any other failure through
     * [onFailure] before surfacing it in the result.
     *
     * [onFailure] receives the raw [Throwable] as well as the flattened message: the message alone
     * is routinely a generic wrapper, while the cause chain holds the reason the log needs.
     */
    private fun runGuarded(
        spec: RuntimeSpec,
        name: String,
        onFailure: (String, Throwable) -> Unit,
        body: () -> Unit,
    ): RuntimeOperationResult =
        synchronized(lock) {
            clearInterruptRequest()
            runCatching {
                body()
                RuntimeOperationResult(success = true)
            }
                .getOrElse { error ->
                    if (error is RuntimeInterruptedException) {
                        startupLog(spec, "$name interrupted reason=${error.message}")
                        RuntimeOperationResult(success = true)
                    } else {
                        val message = error.message?.takeIf(String::isNotBlank)
                            ?: "$name runtime failed"
                        onFailure(message, error)
                        RuntimeOperationResult(success = false, error = message)
                    }
                }
        }

    fun stop(reason: String? = null): RuntimeOperationResult {
        requestStop(reason)
        return synchronized(lock) {
            runCatching {
                stopInternal(reason = reason, notifyHost = true)
                RuntimeOperationResult(success = true)
            }
                .getOrElse { error ->
                    RuntimeOperationResult(
                        success = false,
                        error = error.message ?: "stop runtime failed",
                    )
                }
        }
    }

    fun requestStop(reason: String? = null) {
        interruptReason = reason ?: "runtime stop requested"
        stopCoreWatch()
        currentSpec?.let { startupLog(it, "stop requested") }
        snapshotRefreshJob?.cancel()
        interruptSignal.release()
    }

    fun destroy() {
        requestStop("runtime destroyed")
        synchronized(lock) {
            runCatching { stopInternal(reason = "runtime destroyed", notifyHost = false) }
        }
        scope.cancel()
    }

    fun snapshot(): RuntimeSnapshot = currentSnapshot


    private fun startInternal(spec: RuntimeSpec) {
        val startedAt = System.currentTimeMillis()
        val startedAtNanos = SystemClock.elapsedRealtimeNanos()
        val prepared = prepareCompiledSpec(spec)
        startupLog(prepared, "compiled elapsedMs=${elapsedMillis(startedAtNanos)}")
        currentSpec = prepared
        publishSnapshot(
            RuntimeSnapshot(
                owner = prepared.owner,
                phase = RuntimePhase.Starting,
                runMode = host.mode,
                profileUuid = prepared.profileUuid,
                profileName = prepared.profileName,
                profileReady = true,
                startedAt = startedAt,
                effectiveFingerprint = prepared.effectiveFingerprint,
            )
        )
        host.onStarting(prepared)
        ensureNotInterrupted(prepared)

        claimCoreAndTeardownPrevious()
        ensureNotInterrupted(prepared)
        startObservers()
        startConnectionTracking()
        ensureNotInterrupted(prepared)

        transport.prepare(prepared)
        transport.start(prepared)
        startCoreWatch(prepared)
        startupLog(
            prepared,
            "core launched elapsedMs=${elapsedMillis(startedAtNanos)} " +
                "profile=${prepared.profileName} overrides=${prepared.overrideSpecs.size}",
        )
        val initialGroups = awaitProxyGroupsReady(prepared)
        startupLog(
            prepared,
            "controller ready elapsedMs=${elapsedMillis(startedAtNanos)} groups=${initialGroups.size}",
        )
        ensureNotInterrupted(prepared)
        queryCache.replaceProxyGroups(initialGroups)
        startLogStream()

        updateSnapshot {
            it.copy(
                phase = RuntimePhase.Running,
                profileReady = true,
                groupsReady = initialGroups.isNotEmpty(),
                trafficReady = false,
                configReady = true,
                transportReady = true,
                logReady = telemetry.isLogStreaming(),
                startedAt = startedAt,
                effectiveFingerprint = prepared.effectiveFingerprint,
            )
        }
        host.onProfileLoaded(prepared.profileUuid)
        host.onStarted(prepared)
        startupLog(prepared, "success: started elapsedMs=${elapsedMillis(startedAtNanos)}")
        scheduleRuntimeSnapshotRefresh(prepared)
    }

    private fun reloadInternal(spec: RuntimeSpec) {
        val previous = checkNotNull(currentSpec) { "runtime not started" }
        val previousSnapshot = currentSnapshot
        failureReported.set(false)
        val prepared = prepareCompiledSpec(spec)
        updateSnapshot {
            it.copy(
                phase = RuntimePhase.Starting,
                profileUuid = prepared.profileUuid,
                profileName = prepared.profileName,
                effectiveFingerprint = prepared.effectiveFingerprint,
                groupsReady = false,
                trafficReady = false,
            )
        }

        stopCoreWatch()
        stopLogStream()
        runCatching { transport.stop() }
        try {
            currentSpec = prepared
            transport.start(prepared)
            startCoreWatch(prepared)
            val initialGroups = awaitProxyGroupsReady(prepared)
            ensureNotInterrupted(prepared)
            queryCache.replaceProxyGroups(initialGroups)
            startLogStream()
            updateSnapshot {
                it.copy(
                    phase = RuntimePhase.Running,
                    profileReady = true,
                    groupsReady = initialGroups.isNotEmpty(),
                    trafficReady = false,
                    configReady = true,
                    transportReady = true,
                    logReady = telemetry.isLogStreaming(),
                    effectiveFingerprint = prepared.effectiveFingerprint,
                    lastError = null,
                )
            }
            host.onProfileLoaded(prepared.profileUuid)
            startupLog(prepared, "success: reload done")
            scheduleRuntimeSnapshotRefresh(prepared)
        } catch (reloadError: Throwable) {
            if (reloadError is CancellationException || interruptReason != null) throw reloadError
            restorePreviousAfterReloadFailure(previous, previousSnapshot, reloadError)
            throw reloadError
        }
    }

    private fun restorePreviousAfterReloadFailure(
        previous: RuntimeSpec,
        previousSnapshot: RuntimeSnapshot,
        reloadError: Throwable,
    ) {
        stopCoreWatch()
        runCatching { transport.stop() }
        val restoreResult =
            runCatching {
                currentSpec = previous
                transport.start(previous)
                startCoreWatch(previous)
                val groups = awaitProxyGroupsReady(previous)
                ensureNotInterrupted(previous)
                queryCache.replaceProxyGroups(groups)
                startLogStream()
                publishSnapshot(
                    previousSnapshot.copy(
                        phase = RuntimePhase.Running,
                        groupsReady = groups.isNotEmpty(),
                        trafficReady = false,
                        transportReady = true,
                        logReady = telemetry.isLogStreaming(),
                    )
                )
                scheduleRuntimeSnapshotRefresh(previous)
            }
        restoreResult.onSuccess {
            startupError(previous, "reload rejected; previous config restored", reloadError)
        }
        restoreResult.exceptionOrNull()?.let { restoreError ->
            val reason =
                "reload failed and previous runtime restore failed: " +
                    (restoreError.message ?: restoreError::class.java.simpleName)
            rollback(previous, reason, restoreError, "reload")
            throw IllegalStateException(reason, reloadError)
        }
    }

    private fun stopInternal(reason: String?, notifyHost: Boolean) {
        stopCoreWatch()
        snapshotRefreshJob?.cancel()
        snapshotRefreshJob = null
        if (currentSnapshot.phase == RuntimePhase.Idle && currentSpec == null) {
            clearInterruptRequest()
            return
        }

        updateSnapshot {
            it.copy(
                phase =
                    if (it.phase == RuntimePhase.Idle) {
                        RuntimePhase.Idle
                    } else {
                        RuntimePhase.Stopping
                    },
                transportReady = false,
                groupsReady = false,
                trafficReady = false,
                configReady = false,
                logReady = false,
                lastError = reason,
            )
        }
        stopLogStream()
        stopConnectionTracking()
        stopObservers()
        teardownTransportAndCore()
        currentSpec = null
        queryCache.clear()
        publishSnapshot(
            RuntimeSnapshot(
                owner = RuntimeOwner.None,
                phase = RuntimePhase.Idle,
                runMode = host.mode,
                lastError = reason,
            )
        )
        if (notifyHost) {
            host.onStopped(reason)
        }
        clearInterruptRequest()
    }

    private fun rollback(
        spec: RuntimeSpec,
        reason: String,
        error: Throwable? = null,
        operation: String,
    ) {
        stopCoreWatch()
        stopLogStream()
        stopConnectionTracking()
        stopObservers()
        teardownTransportAndCore()
        currentSpec = null
        queryCache.clear()
        publishSnapshot(
            RuntimeSnapshot(
                owner = spec.owner,
                phase = RuntimePhase.Failed,
                runMode = host.mode,
                profileUuid = spec.profileUuid,
                profileName = spec.profileName,
                profileReady = false,
                lastError = reason,
                effectiveFingerprint = spec.effectiveFingerprint,
            )
        )
        startupError(spec, "$operation failed: $reason", error)
        if (failureReported.compareAndSet(false, true)) {
            host.reportFailure(reason)
        }
    }

    private fun startCoreWatch(spec: RuntimeSpec) {
        stopCoreWatch()
        if (spec.owner != RuntimeOwner.VpnService) return
        coreWatchJob =
            scope.launch(Dispatchers.IO) {
                while (isActive) {
                    delay(CORE_WATCH_INTERVAL_MS)
                    if (currentSpec !== spec || interruptReason != null) return@launch
                    if (
                        currentSnapshot.phase == RuntimePhase.Idle ||
                            currentSnapshot.phase == RuntimePhase.Stopping ||
                            currentSnapshot.phase == RuntimePhase.Failed
                    ) {
                        return@launch
                    }
                    if (CoreProcess.isLocalCoreAlive()) {
                        continue
                    }

                    val coreTail = CoreProcess.coreLogTail(host.context.appContextOrSelf)
                    val reason =
                        "core process exited unexpectedly" +
                            (coreTail?.let { ": $it" } ?: "")
                    if (!failureReported.compareAndSet(false, true)) return@launch

                    interruptReason = reason
                    interruptSignal.release()
                    publishSnapshot(
                        RuntimeSnapshot(
                            owner = spec.owner,
                            phase = RuntimePhase.Failed,
                            runMode = host.mode,
                            profileUuid = spec.profileUuid,
                            profileName = spec.profileName,
                            lastError = reason,
                            effectiveFingerprint = spec.effectiveFingerprint,
                        )
                    )
                    startupError(spec, reason)
                    host.reportFailure(reason)
                    return@launch
                }
            }
    }

    private fun stopCoreWatch() {
        coreWatchJob?.cancel()
        coreWatchJob = null
    }

    private fun awaitProxyGroupsReady(spec: RuntimeSpec): List<ProxyGroup> {
        ensureNotInterrupted(spec)
        val expectedGroups = readExpectedGroupNames(spec)
        verifyLog(
            spec,
            "expectedGroups=${expectedGroups.size}" +
                expectedGroups
                    .takeIf { it.isNotEmpty() }
                    ?.let { " sample=${it.take(5)}" }
                    .orEmpty(),
        )

        var lastControllerError: String? = null
        repeat(PROXY_GROUP_READY_RETRY_COUNT) { attempt ->
            ensureNotInterrupted(spec)

            if (
                spec.owner == RuntimeOwner.VpnService &&
                    !CoreProcess.isLocalCoreAlive()
            ) {
                val coreTail =
                    CoreProcess.coreLogTail(
                        host.context.appContextOrSelf
                    )
                error(
                    "core process exited before controller became ready" +
                        (coreTail?.let { " ($it)" } ?: "")
                )
            }

            val groups = runCatching {
                rest.queryAllProxyGroups(false)
            }
                .onFailure { error ->
                    lastControllerError = error.message ?: error::class.simpleName
                }
                .getOrDefault(emptyList())
            val names = groups.map(ProxyGroup::name).filter(String::isNotBlank)
            if (names.isNotEmpty()) {
                verifyLog(spec, "success: actualGroups=${names.size} sample=${names.take(5)}")
                return groups
            }
            val tunnelOk = runCatching {
                rest.queryTunnelState()
                true
            }
                .onFailure { error ->
                    lastControllerError = error.message ?: error::class.simpleName
                }
                .getOrDefault(false)
            if (tunnelOk && expectedGroups.isEmpty()) {
                verifyLog(spec, "success: controller ok with 0 expected groups")
                return emptyList()
            }
            if (attempt == 0 || attempt == PROXY_GROUP_READY_RETRY_COUNT - 1) {
                verifyWarn(
                    spec,
                    "actualGroups=0 controller=${if (tunnelOk) "ok" else "down"}" +
                        (lastControllerError?.let { " err=$it" } ?: "") +
                        " attempt=${attempt + 1}/$PROXY_GROUP_READY_RETRY_COUNT",
                )
            }
            if (attempt < PROXY_GROUP_READY_RETRY_COUNT - 1) {
                waitForRetryOrInterrupt(PROXY_GROUP_READY_RETRY_DELAY_MS)
            }
        }

        ensureNotInterrupted(spec)
        val coreTail =
            CoreProcess.coreLogTail(
                host.context.appContextOrSelf
            )
        if (expectedGroups.isNotEmpty()) {
            error(
                "runtime loaded but exposed 0 proxy groups; expected=${expectedGroups.size} " +
                    "sample=${expectedGroups.take(min(5, expectedGroups.size))}" +
                    (coreTail?.let { " core=$it" } ?: "")
            )
        }
        error(
            "core controller unavailable or exposed 0 proxy groups during startup" +
                (lastControllerError?.let { ": $it" } ?: "") +
                (coreTail?.let { " ($it)" } ?: "")
        )
    }

    private fun scheduleRuntimeSnapshotRefresh(spec: RuntimeSpec) {
        snapshotRefreshJob?.cancel()
        snapshotRefreshJob =
            scope.launch(Dispatchers.IO) {
                refreshRuntimeSnapshot(spec)
            }
    }

    private fun readExpectedGroupNames(spec: RuntimeSpec): List<String> {
        if (spec.expectedProxyGroupNames.isNotEmpty()) {
            return spec.expectedProxyGroupNames
        }
        if (spec.compiledFinalYaml.isNotBlank()) {
            return compiledConfigPipeline.extractProxyGroupNames(spec.compiledFinalYaml)
        }
        return runCatching { proxyGroupResolver.expectedGroupNames(spec, false) }
            .getOrElse { error ->
                verifyWarn(spec, "expected group inspect failed: ${error.message}")
                emptyList()
            }
    }

    /**
     * Ensures [RuntimeSpec.compiledFinalYaml] / [RuntimeSpec.expectedProxyGroupNames] are populated
     * with a single nativeCompile so transport handoff and readiness share the same result.
     */
    private fun prepareCompiledSpec(spec: RuntimeSpec): RuntimeSpec {
        if (spec.compiledFinalYaml.isNotBlank()) {
            if (spec.expectedProxyGroupNames.isNotEmpty()) {
                return spec
            }
            val names = compiledConfigPipeline.extractProxyGroupNames(spec.compiledFinalYaml)
            return spec.copy(expectedProxyGroupNames = names)
        }
        val compiled =
            kotlinx.coroutines.runBlocking { compiledConfigPipeline.compileDetailed(spec) }
        startupLog(
            spec,
            "compiled once groups=${compiled.proxyGroupNames.size} warnings=${compiled.warnings.size}",
        )
        return spec.copy(
            compiledFinalYaml = compiled.finalYaml,
            expectedProxyGroupNames = compiled.proxyGroupNames,
        )
    }

    private fun waitForRetryOrInterrupt(delayMs: Long) {
        if (interruptReason == null) {
            interruptSignal.tryAcquire(delayMs, TimeUnit.MILLISECONDS)
        }
    }

    private fun startObservers() = observers.start()

    private fun stopObservers() = observers.stop()

    private fun claimCoreAndTeardownPrevious() {
        coreOwner = this
        teardownCore()
    }

    private fun teardownTransportAndCore() {
        runCatching { transport.stop() }
        teardownCore()
    }

    private fun teardownCore() {
        val owner = coreOwner
        if (owner !== this && owner != null) return
        coreOwner = null
        CompiledTunPackages.clear()
    }

    private fun refreshRuntimeSnapshot(expectedSpec: RuntimeSpec? = currentSpec) {
        if (
            currentSnapshot.phase != RuntimePhase.Running &&
                currentSnapshot.phase != RuntimePhase.Starting
        ) {
            queryCache.clear()
            return
        }

        val configuration = runCatching {
            rest.queryConfiguration()
        }.getOrDefault(UiConfiguration())
        val providers = runCatching { rest.queryProviders() }.getOrDefault(emptyList())
        val proxyGroups = runCatching {
            resolveRuntimeProxyGroups(excludeNotSelectable = false)
        }.getOrDefault(emptyList())
        val trafficNow = runCatching { rest.queryTrafficNow() }.getOrDefault(0L)
        val trafficTotal = runCatching { rest.queryTrafficTotal() }.getOrDefault(0L)
        if (currentSpec !== expectedSpec || currentSnapshot.phase != RuntimePhase.Running) {
            return
        }
        queryCache.replace(
            configuration = configuration,
            providers = providers,
            proxyGroups = proxyGroups,
            trafficNow = trafficNow,
            trafficTotal = trafficTotal,
        )
    }

    private fun resolveRuntimeProxyGroups(excludeNotSelectable: Boolean): List<ProxyGroup> =
        proxyGroupResolver.resolvedGroups(currentSpec, excludeNotSelectable, enrichLive = true)

    private fun ensureRuntimeSnapshot(): SessionRuntimeQuerySnapshot {
        val snapshot = queryCache.snapshot()
        if (snapshot.proxyGroups.isNotEmpty()) {
            return snapshot
        }
        refreshRuntimeSnapshot()
        return queryCache.snapshot()
    }

    private fun startLogStream() {
        telemetry.startLogStream(rest::subscribeLogs)
    }

    private fun stopLogStream() {
        telemetry.stopLogStream()
    }

    private fun startConnectionTracking() {
        telemetry.startConnectionTracking()
    }

    private fun stopConnectionTracking() {
        telemetry.stopConnectionTracking()
    }

    private fun publishSnapshot(snapshot: RuntimeSnapshot) {
        synchronized(snapshotLock) {
            currentSnapshot = snapshot.copy(running = snapshot.phase.running)
            host.onSnapshotChanged(currentSnapshot)
        }
    }

    private inline fun updateSnapshot(transform: (RuntimeSnapshot) -> RuntimeSnapshot) {
        synchronized(snapshotLock) {
            val next = transform(currentSnapshot)
            currentSnapshot = next.copy(running = next.phase.running)
            host.onSnapshotChanged(currentSnapshot)
        }
    }

    /** Null for owners that run no local session of their own (remote controller / idle). */
    private fun logWriter(spec: RuntimeSpec): RuntimeLog.Writer? {
        val source =
            when (spec.owner) {
                RuntimeOwner.VpnService -> RuntimeLog.Source.LocalTun
                RuntimeOwner.RootDaemon -> RuntimeLog.Source.forMode(spec.runMode)
                RuntimeOwner.RemoteController,
                RuntimeOwner.None -> return null
            }
        return RuntimeLog.writer(host.context.appContextOrSelf, source)
    }

    private fun startupLog(spec: RuntimeSpec, message: String) {
        logWriter(spec)?.i(RuntimeLog.Type.Session, message)
    }

    private fun startupError(spec: RuntimeSpec, message: String, error: Throwable? = null) {
        logWriter(spec)?.e(RuntimeLog.Type.Session, message, error)
    }

    private fun verifyLog(spec: RuntimeSpec, message: String) {
        logWriter(spec)?.i(RuntimeLog.Type.Verify, message)
    }

    private fun verifyWarn(spec: RuntimeSpec, message: String) {
        logWriter(spec)?.w(RuntimeLog.Type.Verify, message)
    }

    private fun elapsedMillis(startedAtNanos: Long): Long =
        (SystemClock.elapsedRealtimeNanos() - startedAtNanos) / 1_000_000L

    private fun ensureNotInterrupted(spec: RuntimeSpec) {
        val reason = interruptReason ?: return
        startupLog(spec, "session: interrupted reason=$reason")
        if (failureReported.get()) {
            throw RuntimeFailureException(reason)
        }
        throw RuntimeInterruptedException(reason)
    }

    private fun clearInterruptRequest() {
        interruptReason = null
        interruptSignal.drainPermits()
    }

    private companion object {
        private const val PROXY_GROUP_READY_RETRY_COUNT = 10
        private const val PROXY_GROUP_READY_RETRY_DELAY_MS = 200L
        private const val CORE_WATCH_INTERVAL_MS = 500L

        /** The session instance currently owning the process-wide Go core. */
        @Volatile private var coreOwner: SessionRuntime? = null
    }

    private class RuntimeInterruptedException(message: String) : CancellationException(message)

    private class RuntimeFailureException(message: String) : IllegalStateException(message)
}
