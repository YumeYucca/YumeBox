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

import android.net.VpnService
import android.os.SystemClock
import com.github.yumeyucca.yumebox.runtime.api.appContextOrSelf
import com.github.yumeyucca.yumebox.runtime.service.core.CoreProcess
import com.github.yumeyucca.yumebox.runtime.service.log.RuntimeLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The VPN data plane of one `TunService` instance:
 * one compile per start/reload, one fork, then a `/group` readiness probe. Callers serialize all
 * calls; cancellation aborts between the blocking steps.
 */
internal class VpnSession(service: VpnService) {
    private val context = service.appContextOrSelf
    private val pipeline = CompiledConfigPipeline(context)
    private val transport = VpnTunTransport(service)
    private val log = RuntimeLog.writer(context, RuntimeLog.Source.LocalTun)

    @Volatile
    var loaded: LoadedRuntime? = null
        private set

    suspend fun start(spec: RuntimeSpec) {
        val startedAt = SystemClock.elapsedRealtime()
        log.i(RuntimeLog.Type.Session, "start begin profile=${spec.profileName}")
        val runtime = load(spec)
        log.i(RuntimeLog.Type.Session, "compiled elapsedMs=${elapsedSince(startedAt)}")
        launch(runtime)
        log.i(RuntimeLog.Type.Session, "success: started elapsedMs=${elapsedSince(startedAt)}")
    }

    /**
     * Swaps the running config. If the new one does not come up, the previous one is relaunched
     * and [ReloadRejectedException] reports the rejected config; any other throw means nothing is
     * serving any more.
     */
    suspend fun reload(spec: RuntimeSpec) {
        val previous = checkNotNull(loaded) { "runtime not started" }
        log.i(RuntimeLog.Type.Reload, "begin profile=${spec.profileName}")
        val runtime = load(spec)
        try {
            launch(runtime)
        } catch (error: CancellationException) {
            throw error
        } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
            log.e(RuntimeLog.Type.Reload, "rejected; restoring previous config", error)
            launch(previous)
            throw ReloadRejectedException(previous.spec, error)
        }
        log.i(RuntimeLog.Type.Reload, "success profile=${spec.profileName}")
    }

    /** Blocking: returns after the child core has exited. */
    fun stop() {
        loaded = null
        runCatching { transport.stop() }
            .onFailure { error -> log.e(RuntimeLog.Type.Session, "stop failed", error) }
    }

    private suspend fun load(spec: RuntimeSpec): LoadedRuntime =
        LoadedRuntime(spec, pipeline.compile(spec))

    private suspend fun launch(runtime: LoadedRuntime) {
        withContext(Dispatchers.IO) { transport.start(runtime) }
        loaded = runtime
        awaitReady(runtime)
    }

    private suspend fun awaitReady(runtime: LoadedRuntime) {
        // Parsed only when the core reports zero groups: a normal start never decodes the YAML.
        val expectedGroups by lazy { pipeline.extractProxyGroupNames(runtime.config) }
        var controllerAnswered = false
        ControllerReadiness.await(
            ensureAlive = {
                if (!CoreProcess.isLocalCoreAlive()) {
                    error(
                        "core process exited before controller became ready" +
                            coreTailSuffix()
                    )
                }
            },
            probe = {
                val groups = CoreProcess.probeGroupCount(context)
                controllerAnswered = true
                groups > 0 || expectedGroups.isEmpty()
            },
            onTimeout = { lastError ->
                val detail =
                    if (controllerAnswered) {
                        "runtime loaded but exposed 0 proxy groups; " +
                            "expected=${expectedGroups.size} sample=${expectedGroups.take(5)}"
                    } else {
                        "core controller unavailable during startup" +
                            (lastError?.message?.let { ": $it" }.orEmpty())
                    }
                log.w(RuntimeLog.Type.Verify, detail)
                throw IllegalStateException(detail + coreTailSuffix(), lastError)
            },
        )
        log.i(RuntimeLog.Type.Verify, "success: controller ready")
    }

    private fun coreTailSuffix(): String =
        CoreProcess.coreLogTail(context)?.let { " ($it)" }.orEmpty()

    private fun elapsedSince(startedAt: Long): Long = SystemClock.elapsedRealtime() - startedAt
}

/** The new config failed; [restored] is serving again. */
internal class ReloadRejectedException(val restored: RuntimeSpec, cause: Throwable) :
    IllegalStateException(cause.message ?: "reload rejected", cause)
