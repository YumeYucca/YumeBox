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

import android.content.Context
import android.os.SystemClock
import com.github.yumeyucca.yumebox.core.model.RunMode
import com.github.yumeyucca.yumebox.core.util.StartupTaskCoordinator
import com.github.yumeyucca.yumebox.runtime.service.core.CoreProcess
import com.github.yumeyucca.yumebox.runtime.service.core.KernelManager
import com.github.yumeyucca.yumebox.runtime.service.core.RootDaemonState
import com.github.yumeyucca.yumebox.runtime.service.log.RuntimeLog
import com.github.yumeyucca.yumebox.runtime.service.root.RootAccessSupport
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Compiles the active profile for a root [RunMode.Tun] / [RunMode.Ebpf] daemon and swaps it in.
 * The daemon that is already serving keeps serving through the compile; [CoreProcess.startRoot]
 * reaps it under the lifecycle lock right before the new exec.
 */
internal object RootRuntime {
    /**
     * On failure the daemon this call launched is reaped again, so afterwards
     * [CoreProcess.isTrackedRootProcessAlive] tells whether the previous daemon still serves.
     */
    suspend fun start(context: Context, mode: RunMode) {
        require(mode == RunMode.Tun || mode == RunMode.Ebpf) {
            "RootRuntime handles root modes only, got $mode"
        }
        val log = RuntimeLog.writer(context, mode)
        log.beginSession(RuntimeLog.Type.Launcher, "root start mode=${mode.name}")
        val previousPid = RootDaemonState.load()?.pid
        try {
            withContext(Dispatchers.IO) {
                RootAccessSupport.requireRootAccess(context)
                if (mode == RunMode.Ebpf) {
                    check(KernelManager.isEbpfKernelActive(context)) {
                        "eBPF mode requires an active kernel with the ebpf capability"
                    }
                }
            }
            StartupTaskCoordinator.awaitWarmup()
            val spec = withContext(Dispatchers.IO) {
                SessionRuntimeSpecFactory(context).createRootSpec(mode)
            }
            log.i(
                RuntimeLog.Type.Launcher,
                "profile=${spec.profileName} overrides=${spec.overrideSpecs.size}",
            )
            val config = CompiledConfigPipeline(context).compile(spec)
            log.i(RuntimeLog.Type.Launcher, "compiled")
            withContext(Dispatchers.IO) { CoreProcess(context).startRoot(mode.coreArg, config) }
            awaitReady(context)
            log.i(RuntimeLog.Type.Launcher, "success: root daemon running mode=${mode.name}")
        } catch (error: Throwable) {
            withContext(NonCancellable + Dispatchers.IO) {
                if (RootDaemonState.load()?.pid != previousPid) {
                    runCatching { CoreProcess.stopRoot() }
                }
            }
            if (error !is CancellationException) {
                log.e(RuntimeLog.Type.Launcher, "root start failed", error)
            }
            throw error
        }
    }

    private suspend fun awaitReady(context: Context) {
        var nextLivenessCheckAt = 0L
        ControllerReadiness.await(
            ensureAlive = {
                // The socket probe is the ready signal; the shell check only fails fast once the
                // process has exited, so it is rate-limited.
                val now = SystemClock.elapsedRealtime()
                if (now >= nextLivenessCheckAt) {
                    nextLivenessCheckAt = now + LIVENESS_INTERVAL_MS
                    if (!CoreProcess.isTrackedRootProcessAlive()) {
                        error(CoreProcess.coreLogTail(context) ?: "root core exited during startup")
                    }
                }
            },
            probe = {
                CoreProcess.probeController(context)
                true
            },
            onTimeout = { lastError ->
                val tail = CoreProcess.coreLogTail(context)
                throw IllegalStateException(
                    "root core controller unavailable: " +
                        (lastError?.message ?: "startup timed out") +
                        (tail?.let { " ($it)" }.orEmpty()),
                    lastError,
                )
            },
        )
    }

    private const val LIVENESS_INTERVAL_MS = 300L
}
