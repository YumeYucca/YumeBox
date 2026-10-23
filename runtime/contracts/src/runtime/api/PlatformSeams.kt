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

package com.github.yumeyucca.yumebox.runtime.api

import com.github.yumeyucca.yumebox.core.model.RunMode
import kotlinx.coroutines.flow.StateFlow

/**
 * The one authoritative lifecycle state of the local core. [owner] is [RuntimeOwner.VpnService]
 * or [RuntimeOwner.RootDaemon] while anything is starting, running or stopping; [mode] is the
 * concrete run mode of that owner. [lastError] survives into Idle/Failed so callers can show why
 * the last session ended. [generation] increases on every published transition.
 */
data class RuntimeState(
    val owner: RuntimeOwner = RuntimeOwner.None,
    val mode: RunMode? = null,
    val phase: RuntimePhase = RuntimePhase.Idle,
    val startedAt: Long? = null,
    val lastError: String? = null,
    val reloading: Boolean = false,
    val generation: Long = 0L,
) {
    val active: Boolean
        get() = phase == RuntimePhase.Starting || phase == RuntimePhase.Running
}

enum class RuntimeStartSource {
    Ui,
    Tile,
    AutoRestart,
    AutoRestartBoot,
    AutoRestartReplaced,
    WifiAutomation,
    System,
}

/**
 * Single entry point for starting, stopping and reloading the local core. Every call is
 * serialized; [start] returns once the core answers on its controller socket and throws (after
 * publishing Failed) when it cannot get there. Starting a different owner stops the current one
 * first.
 */
interface RuntimeControl {
    val state: StateFlow<RuntimeState>

    suspend fun start(mode: RunMode, source: RuntimeStartSource)

    suspend fun stop(reason: String? = null)

    /** Recompile the active profile into the running owner; no-op when nothing runs. */
    suspend fun reload()

    /** Re-check that the published owner still has a live core; drops to Idle if it vanished. */
    suspend fun verify()
}
