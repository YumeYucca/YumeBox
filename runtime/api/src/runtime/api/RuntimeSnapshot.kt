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

package com.github.yumeyucca.yumebox.runtime.api


import com.github.yumeyucca.yumebox.core.model.RunMode
import kotlinx.serialization.Serializable

@Serializable
enum class RuntimeOwner {
    None,

    /** The non-root Android VpnService path (a service-hosted fork+exec child core). */
    VpnService,

    /**
     * The root libsu daemon running Tun mode.
     */
    RootDaemon,
    RemoteController,
}

@Serializable
enum class RuntimePhase {
    Idle,
    Starting,
    Running,
    Stopping,
    Failed;

    val running: Boolean
        get() = this == Running

    val isNotIdle: Boolean
        get() = this != Idle

    val isActiveOrStopping: Boolean
        get() = this == Starting || this == Running || this == Stopping

    val isRecovering: Boolean
        get() = this == Starting || this == Stopping
}

@Serializable
data class RuntimeSnapshot(
    val owner: RuntimeOwner = RuntimeOwner.None,
    val phase: RuntimePhase = RuntimePhase.Idle,
    val runMode: RunMode = RunMode.VpnService,
    val profileReady: Boolean = false,
    val groupsReady: Boolean = false,
    val trafficReady: Boolean = false,
    val profileUuid: String? = null,
    val profileName: String? = null,
    val lastError: String? = null,
    val startedAt: Long? = null,
    val generation: Long = 0L,
    val running: Boolean = phase.running,
) {
    val payloadReady: Boolean
        get() = profileReady && groupsReady && trafficReady
}
