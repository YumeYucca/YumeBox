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

package com.github.yumeyucca.yumebox.runtime.service

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.SystemClock
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import com.github.yumeyucca.yumebox.core.Global
import com.github.yumeyucca.yumebox.core.model.RunMode
import com.github.yumeyucca.yumebox.core.util.requireBuiltinGeoAssets
import com.github.yumeyucca.yumebox.data.store.RemoteControllerStore
import com.github.yumeyucca.yumebox.runtime.api.Intents
import com.github.yumeyucca.yumebox.runtime.api.RuntimeControl
import com.github.yumeyucca.yumebox.runtime.api.RuntimeOwner
import com.github.yumeyucca.yumebox.runtime.api.RuntimePhase
import com.github.yumeyucca.yumebox.runtime.api.RuntimeStartSource
import com.github.yumeyucca.yumebox.runtime.api.RuntimeState
import com.github.yumeyucca.yumebox.runtime.api.VpnPermissionRequired
import com.github.yumeyucca.yumebox.runtime.service.config.ServiceStore
import com.github.yumeyucca.yumebox.runtime.service.core.CoreProcess
import com.github.yumeyucca.yumebox.runtime.service.core.RootDaemonProbe
import com.github.yumeyucca.yumebox.runtime.service.core.RootDaemonState
import com.github.yumeyucca.yumebox.runtime.service.log.RuntimeLog
import com.github.yumeyucca.yumebox.runtime.service.session.ConnectionTracker
import com.github.yumeyucca.yumebox.runtime.service.session.ReloadRejectedException
import com.github.yumeyucca.yumebox.runtime.service.session.RootRuntime
import com.github.yumeyucca.yumebox.runtime.service.session.SessionRuntimeSpecFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The single owner of the local core lifecycle, in-process for every caller (UI, tile, boot,
 * Wi-Fi automation, config changes). Operations run one at a time under [mutex] on [scope], so a
 * caller that goes away never leaves a half-started core behind, and [state] is the only source
 * of truth — nothing is inferred from service liveness or persisted phase slots.
 */
object RuntimeCoordinator : RuntimeControl {
    private const val TAG = "RuntimeCoordinator"
    private const val VPN_ATTACH_TIMEOUT_MS = 10_000L
    private const val VPN_DESTROY_TIMEOUT_MS = 3_000L
    private const val CORE_WATCH_INTERVAL_MS = 500L
    private const val RELOAD_DEBOUNCE_MS = 150L
    private const val BACKEND_UNAVAILABLE = "runtime backend unavailable"

    private val context: Context
        get() = Global.application

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private val bootstrapRequested = AtomicBoolean(false)
    private var bootstrapped = false
    private val _state = MutableStateFlow(RuntimeState())

    /** The started TunService instance, from onStartCommand until onDestroy. */
    @Volatile private var vpnService: TunService? = null
    /** The instance whose session currently carries the VPN core. */
    @Volatile private var activeVpn: TunService? = null
    @Volatile private var vpnAttach: CompletableDeferred<TunService>? = null
    @Volatile private var vpnDestroy: CompletableDeferred<Unit>? = null
    @Volatile private var startJob: Job? = null
    private var reloadJob: Job? = null
    private var watchJob: Job? = null
    /** elapsedRealtime right before the running owner last read its spec. */
    @Volatile private var specReadAt = 0L
    private val connectionTracker by lazy { ConnectionTracker(context, scope) }
    private val publicState = _state.asStateFlow()

    /** First access reattaches a surviving root daemon or restores the last failure. */
    override val state: StateFlow<RuntimeState>
        get() {
            if (bootstrapRequested.compareAndSet(false, true)) {
                scope.launch { runOp {} }
            }
            return publicState
        }

    override suspend fun start(mode: RunMode, source: RuntimeStartSource) {
        try {
            runOp { startLocked(mode, source) }
        } catch (error: CancellationException) {
            // A stop cancels the start it supersedes; only the caller's own cancellation escapes.
            currentCoroutineContext().ensureActive()
        }
    }

    override suspend fun stop(reason: String?) {
        startJob?.cancel()
        runOp { stopLocked(reason) }
    }

    override suspend fun reload() = runOp { reloadLocked(changedAt = null) }

    override suspend fun verify() = runOp { verifyLocked() }

    private suspend fun <T> runOp(block: suspend () -> T): T =
        scope
            .async {
                mutex.withLock {
                    bootstrapLocked()
                    block()
                }
            }
            .await()

    // ---- TunService callbacks (main thread) ----

    internal fun onVpnServiceStarted(service: TunService) {
        vpnService = service
        val attach = vpnAttach
        if (attach != null) {
            attach.complete(service)
        } else if (activeVpn !== service) {
            scope.launch { adoptVpnService(service) }
        }
    }

    internal fun onVpnRevoked(service: TunService) {
        if (activeVpn !== service) return
        scope.launch { stop("VPN connection revoked by system") }
    }

    internal fun onVpnServiceDestroyed(service: TunService) {
        if (vpnService === service) vpnService = null
        vpnDestroy?.complete(Unit)
        if (activeVpn === service) {
            scope.launch {
                runOp {
                    if (activeVpn !== service) return@runOp
                    Timber.tag(TAG).w("TunService destroyed while its session was active")
                    teardownLocked(RuntimeOwner.VpnService)
                    publish(RuntimeOwner.None, _state.value.mode, RuntimePhase.Idle)
                }
            }
        }
    }

    /** A TunService the system recreated on its own (START_REDELIVER_INTENT) becomes a start. */
    private suspend fun adoptVpnService(service: TunService) {
        runCatching { start(RunMode.VpnService, RuntimeStartSource.System) }
            .onFailure { error -> Timber.tag(TAG).w(error, "Recreated TunService not adopted") }
        runOp {
            if (activeVpn !== service && vpnService === service) {
                withContext(Dispatchers.Main) { service.finish() }
            }
        }
    }

    // ---- operations (called with mutex held) ----

    private suspend fun bootstrapLocked() {
        if (bootstrapped) return
        bootstrapped = true
        bootstrapRequested.set(true)
        registerConfigReceiver()
        val rootMode =
            withContext(Dispatchers.IO) {
                runCatching { CoreProcess.reconnectRoot(context) }.getOrNull()
            }
                ?.let(RunMode::fromCoreArg)
        if (rootMode != null) {
            val launchedAt = RootDaemonState.load()?.launchedAt?.takeIf { it > 0L }
            publish(RuntimeOwner.RootDaemon, rootMode, RuntimePhase.Running, startedAt = launchedAt)
            runCatching { RootForegroundService.start(context) }
            onRunning(RuntimeOwner.RootDaemon)
            return
        }
        RuntimeFailureRecord.load()?.let { failure ->
            publish(
                RuntimeOwner.None,
                failure.mode,
                RuntimePhase.Failed,
                lastError = failure.error,
            )
        }
    }

    private suspend fun startLocked(mode: RunMode, source: RuntimeStartSource) {
        val previous = _state.value
        if (previous.phase == RuntimePhase.Running && previous.mode == mode) return
        if (RemoteControllerStore.isActive()) {
            RuntimeLog.writer(context, mode)
                .i(RuntimeLog.Type.Launcher, "skipped: remote controller active source=$source")
            return
        }
        if (mode == RunMode.VpnService) {
            VpnService.prepare(context)?.let { throw VpnPermissionRequired(it) }
        }
        context.requireBuiltinGeoAssets()

        val owner = ownerOf(mode)
        val swapsRoot = owner == RuntimeOwner.RootDaemon && previous.owner == RuntimeOwner.RootDaemon
        if (previous.owner != RuntimeOwner.None && !swapsRoot) {
            // Straight from Stopping to Starting: an Idle in between would wake idle-only work.
            publish(previous.owner, previous.mode, RuntimePhase.Stopping, startedAt = previous.startedAt)
            withContext(NonCancellable) { teardownLocked(previous.owner) }
        }
        launchLocked(owner, mode, source, previous, swapsRoot)
    }

    /** [swapsRoot]: [previous] is a root daemon that keeps serving until the new exec. */
    private suspend fun launchLocked(
        owner: RuntimeOwner,
        mode: RunMode,
        source: RuntimeStartSource,
        previous: RuntimeState,
        swapsRoot: Boolean,
        reloading: Boolean = false,
    ) {
        RuntimeFailureRecord.clear()
        startJob = currentCoroutineContext()[Job]
        val startedAt = System.currentTimeMillis()
        specReadAt = SystemClock.elapsedRealtime()
        publish(
            RuntimeState(
                owner = owner,
                mode = mode,
                phase = RuntimePhase.Starting,
                startedAt = startedAt,
                reloading = reloading,
            )
        )
        try {
            when (owner) {
                RuntimeOwner.VpnService -> startVpnLocked(source)
                else -> {
                    RootForegroundService.start(context)
                    withContext(Dispatchers.IO) { RootRuntime.start(context, mode) }
                }
            }
            publish(owner, mode, RuntimePhase.Running, startedAt = startedAt)
            onRunning(owner)
        } catch (error: CancellationException) {
            withContext(NonCancellable) {
                if (!keepServingRoot(swapsRoot, previous, lastError = null)) {
                    teardownLocked(owner)
                    publish(RuntimeOwner.None, mode, RuntimePhase.Idle)
                }
            }
            throw error
        } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
            val message = error.message?.takeIf(String::isNotBlank) ?: "runtime start failed"
            if (!keepServingRoot(swapsRoot, previous, lastError = message)) {
                teardownLocked(owner)
                fail(mode, message)
            }
            throw error
        } finally {
            startJob = null
        }
    }

    /** A root swap that failed before its exec leaves the previous daemon serving. */
    private suspend fun keepServingRoot(
        swapsRoot: Boolean,
        previous: RuntimeState,
        lastError: String?,
    ): Boolean {
        if (!swapsRoot || !withContext(Dispatchers.IO) { RootDaemonProbe.trackedAlive() }) {
            return false
        }
        publish(
            RuntimeOwner.RootDaemon,
            previous.mode,
            RuntimePhase.Running,
            startedAt = previous.startedAt,
            lastError = lastError,
        )
        onRunning(RuntimeOwner.RootDaemon)
        return true
    }

    private suspend fun startVpnLocked(source: RuntimeStartSource) {
        RuntimeLog.writer(context, RuntimeLog.Source.LocalTun)
            .beginSession(RuntimeLog.Type.Launcher, "request start source=$source mode=VpnService")
        val service = vpnService ?: awaitVpnService()
        activeVpn = service
        val spec = withContext(Dispatchers.IO) { SessionRuntimeSpecFactory(context).createVpnSpec() }
        service.session.start(spec)
        StatusProvider.currentProfile = spec.profileName
    }

    private suspend fun awaitVpnService(): TunService {
        val attach = CompletableDeferred<TunService>()
        vpnAttach = attach
        try {
            ContextCompat.startForegroundService(context, Intent(context, TunService::class.java))
            return withTimeout(VPN_ATTACH_TIMEOUT_MS) { attach.await() }
        } finally {
            vpnAttach = null
        }
    }

    private suspend fun stopLocked(reason: String?) {
        val current = _state.value
        if (current.owner == RuntimeOwner.None) {
            if (reason != null) publish(RuntimeOwner.None, current.mode, RuntimePhase.Idle, lastError = reason)
            return
        }
        RuntimeFailureRecord.clear()
        publish(current.owner, current.mode, RuntimePhase.Stopping, startedAt = current.startedAt)
        withContext(NonCancellable) { teardownLocked(current.owner) }
        publish(RuntimeOwner.None, current.mode, RuntimePhase.Idle, lastError = reason)
    }

    /** [changedAt]: when the triggering config change arrived; null for an explicit reload. */
    private suspend fun reloadLocked(changedAt: Long?) {
        val current = _state.value
        if (current.phase != RuntimePhase.Running) return
        val mode = current.mode ?: return
        // The running spec was read after that change arrived, so it already carries it.
        if (changedAt != null && changedAt < specReadAt) return
        when (current.owner) {
            RuntimeOwner.VpnService -> reloadVpnLocked(current, mode)
            RuntimeOwner.RootDaemon ->
                try {
                    launchLocked(
                        RuntimeOwner.RootDaemon,
                        mode,
                        RuntimeStartSource.System,
                        current,
                        swapsRoot = true,
                        reloading = true,
                    )
                } catch (error: CancellationException) {
                    throw error
                } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
                    Timber.tag(TAG).w(error, "Root reload failed")
                }
            else -> Unit
        }
    }

    private suspend fun reloadVpnLocked(current: RuntimeState, mode: RunMode) {
        val service = activeVpn ?: return
        publish(current.copy(reloading = true))
        specReadAt = SystemClock.elapsedRealtime()
        try {
            val spec =
                withContext(Dispatchers.IO) { SessionRuntimeSpecFactory(context).createVpnSpec() }
            service.session.reload(spec)
            StatusProvider.currentProfile = spec.profileName
            publish(current.copy(reloading = false, lastError = null))
        } catch (error: ReloadRejectedException) {
            ServiceStore().activeProfile = UUID.fromString(error.restored.profileUuid)
            StatusProvider.currentProfile = error.restored.profileName
            publish(current.copy(reloading = false, lastError = error.message))
        } catch (error: CancellationException) {
            throw error
        } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
            Timber.tag(TAG).e(error, "VPN reload failed and nothing is serving")
            withContext(NonCancellable) { teardownLocked(RuntimeOwner.VpnService) }
            fail(mode, error.message ?: "runtime reload failed")
        }
    }

    private suspend fun verifyLocked() {
        val current = _state.value
        if (current.phase != RuntimePhase.Running) return
        val alive =
            when (current.owner) {
                RuntimeOwner.VpnService -> CoreProcess.isLocalCoreAlive()
                RuntimeOwner.RootDaemon ->
                    withContext(Dispatchers.IO) { RootDaemonProbe.isAlive() }
                else -> true
            }
        if (alive) return
        Timber.tag(TAG).w("%s core vanished", current.owner)
        withContext(NonCancellable) { teardownLocked(current.owner) }
        publish(RuntimeOwner.None, current.mode, RuntimePhase.Idle, lastError = BACKEND_UNAVAILABLE)
    }

    private fun onRunning(owner: RuntimeOwner) {
        connectionTracker.start()
        watchJob?.cancel()
        watchJob = if (owner == RuntimeOwner.VpnService) watchVpnCore() else null
    }

    /** The VPN child has no other exit signal; a dead pid turns the session into Failed. */
    private fun watchVpnCore(): Job =
        scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(CORE_WATCH_INTERVAL_MS)
                if (CoreProcess.isLocalCoreAlive()) continue
                val reason =
                    "core process exited unexpectedly" +
                        (CoreProcess.coreLogTail(context)?.let { ": $it" }.orEmpty())
                scope.launch { runOp { failIfVpnCoreDead(reason) } }
                return@launch
            }
        }

    /** Re-checked under the lock: a reload has no child for a moment by design. */
    private suspend fun failIfVpnCoreDead(reason: String) {
        val current = _state.value
        if (current.owner != RuntimeOwner.VpnService || current.phase != RuntimePhase.Running) {
            return
        }
        if (CoreProcess.isLocalCoreAlive()) {
            watchJob = watchVpnCore()
            return
        }
        teardownLocked(RuntimeOwner.VpnService)
        fail(RunMode.VpnService, reason)
    }

    private suspend fun teardownLocked(owner: RuntimeOwner) {
        watchJob?.cancel()
        watchJob = null
        connectionTracker.stop()
        when (owner) {
            RuntimeOwner.VpnService -> stopVpnLocked()
            RuntimeOwner.RootDaemon -> {
                withContext(Dispatchers.IO) {
                    runCatching { CoreProcess.stopRoot() }
                        .onFailure { error -> Timber.tag(TAG).e(error, "Root stop failed") }
                }
                RootForegroundService.stop(context)
            }
            else -> Unit
        }
    }

    /** Returns only after the child core is gone and the TunService instance is destroyed. */
    private suspend fun stopVpnLocked() {
        val service = activeVpn ?: vpnService
        activeVpn = null
        if (service != null) {
            withContext(Dispatchers.IO) { service.session.stop() }
            if (vpnService === service) {
                val destroyed = CompletableDeferred<Unit>()
                vpnDestroy = destroyed
                withContext(Dispatchers.Main) { service.finish() }
                if (withTimeoutOrNull(VPN_DESTROY_TIMEOUT_MS) { destroyed.await() } == null) {
                    Timber.tag(TAG).w("TunService did not reach onDestroy in time")
                }
                vpnDestroy = null
            }
        }
        if (CoreProcess.isLocalCoreAlive()) {
            withContext(Dispatchers.IO) { CoreProcess.killRunning() }
        }
    }

    private fun fail(mode: RunMode, message: String) {
        Timber.tag(TAG).e("%s runtime failed: %s", mode, message)
        RuntimeFailureRecord.save(mode, message)
        publish(RuntimeOwner.None, mode, RuntimePhase.Failed, lastError = message)
    }

    private fun publish(
        owner: RuntimeOwner,
        mode: RunMode?,
        phase: RuntimePhase,
        startedAt: Long? = null,
        lastError: String? = null,
    ) {
        publish(
            RuntimeState(
                owner = owner,
                mode = mode,
                phase = phase,
                startedAt = startedAt,
                lastError = lastError,
            )
        )
    }

    private fun publish(next: RuntimeState) {
        val previous = _state.value
        _state.value = next.copy(generation = previous.generation + 1)
        if (previous.phase != next.phase || previous.owner != next.owner) {
            Timber.tag(TAG).i("%s %s -> %s %s", previous.owner, previous.phase, next.owner, next.phase)
            requestTileRefresh()
        }
    }

    private fun ownerOf(mode: RunMode): RuntimeOwner =
        when (mode) {
            RunMode.VpnService -> RuntimeOwner.VpnService
            RunMode.Tun,
            RunMode.Ebpf -> RuntimeOwner.RootDaemon
        }

    // ---- config changes ----

    private fun registerConfigReceiver() {
        val filter =
            IntentFilter().apply {
                addAction(Intents.ACTION_PROFILE_CHANGED)
                addAction(Intents.ACTION_OVERRIDE_CHANGED)
            }
        ContextCompat.registerReceiver(
            context,
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val affectsRuntime =
                        intent?.action != Intents.ACTION_PROFILE_CHANGED ||
                            intent.getBooleanExtra(Intents.EXTRA_AFFECTS_RUNTIME, true)
                    if (affectsRuntime) scheduleReload(SystemClock.elapsedRealtime())
                }
            },
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    /** Main thread. Bursts of changes collapse into one reload. */
    private fun scheduleReload(changedAt: Long) {
        if (!_state.value.active) return
        reloadJob?.cancel()
        reloadJob =
            scope.launch {
                delay(RELOAD_DEBOUNCE_MS)
                runCatching { runOp { reloadLocked(changedAt) } }
                    .onFailure { error -> Timber.tag(TAG).w(error, "Config reload failed") }
            }
    }

    /** The QS tile only re-reads state while listening; ask the system to bind it now. */
    private fun requestTileRefresh() {
        runCatching {
            TileService.requestListeningState(
                context,
                ComponentName(context, ProxyTileService::class.java),
            )
        }
    }
}
