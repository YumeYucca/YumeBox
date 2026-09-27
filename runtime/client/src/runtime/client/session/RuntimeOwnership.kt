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

package com.github.yumeyucca.yumebox.runtime.client.session


import com.github.yumeyucca.yumebox.core.model.RunMode
import com.github.yumeyucca.yumebox.runtime.api.*
import com.github.yumeyucca.yumebox.runtime.client.RuntimeStateMapper

/**
 * Ownership / liveness / snapshot transitions for the cohesive runtime session. Uses platform seams
 * ([RuntimeStatusStore], [ProcessController]) so desktop can swap bindings.
 */
internal class RuntimeOwnership(
    private val statusStore: RuntimeStatusStore,
    private val processController: ProcessController,
    private val currentSnapshot: () -> RuntimeSnapshot,
) {
    fun detectOwner(): RuntimeOwner =
        when {
            isVpnSessionActive() -> RuntimeOwner.VpnService
            isRootDaemonActive() -> RuntimeOwner.RootDaemon
            else -> RuntimeOwner.None
        }

    fun detectActiveOwner(): RuntimeOwner {
        statusStore.reconcilePersistedRuntimeState()
        return detectOwner()
    }

    fun isVpnSessionActive(): Boolean = statusStore.isRuntimeActive(RunMode.VpnService.name)

    fun isRootDaemonActive(): Boolean = processController.isRootDaemonAlive()

    fun localRuntimePhaseForOwner(owner: RuntimeOwner): RuntimePhase =
        when (owner) {
            RuntimeOwner.VpnService -> statusStore.queryRuntimePhase(RunMode.VpnService.name)
            RuntimeOwner.RootDaemon ->
                processController.rootDaemonMode()
                    ?.let { statusStore.queryRuntimePhase(it.name) }
                    ?.takeIf { isRootDaemonActive() }
                    ?: RuntimePhase.Idle

            RuntimeOwner.RemoteController,
            RuntimeOwner.None -> RuntimePhase.Idle
        }

    fun localRuntimeStartedAtForOwner(owner: RuntimeOwner): Long? {
        val snapshot = currentSnapshot()
        return when (owner) {
            RuntimeOwner.VpnService ->
                statusStore.queryRuntimeStartedAt(RunMode.VpnService.name)
                    ?: snapshot.startedAt?.takeIf { snapshot.owner == owner }

            RuntimeOwner.RootDaemon ->
                processController.rootDaemonMode()
                    ?.let { statusStore.queryRuntimeStartedAt(it.name) }
                    ?: snapshot.startedAt?.takeIf { snapshot.owner == owner }

            RuntimeOwner.RemoteController,
            RuntimeOwner.None -> null
        }
    }

    fun localModeForOwner(owner: RuntimeOwner): RunMode? =
        when (owner) {
            RuntimeOwner.RootDaemon -> processController.rootDaemonMode()
            else -> RuntimeStateMapper.modeForOwner(owner)
        }

    fun ownerForMode(mode: RunMode): RuntimeOwner =
        when (mode) {
            RunMode.VpnService -> RuntimeOwner.VpnService
            RunMode.Tun -> RuntimeOwner.RootDaemon
            RunMode.Ebpf -> RuntimeOwner.RootDaemon
        }

    fun startingSnapshot(
        owner: RuntimeOwner,
        runMode: RunMode,
        profile: Profile,
        generation: Long,
    ): RuntimeSnapshot =
        RuntimeSnapshot(
            owner = owner,
            phase = RuntimePhase.Starting,
            runMode = runMode,
            profileReady = true,
            profileUuid = profile.uuid.toString(),
            profileName = profile.name,
            startedAt = System.currentTimeMillis(),
            generation = generation,
        )

    fun activeSnapshot(
        owner: RuntimeOwner,
        runMode: RunMode,
        localPhase: RuntimePhase = RuntimePhase.Idle,
        localStartedAt: Long? = null,
    ): RuntimeSnapshot =
        RuntimeSnapshot(
            owner = owner,
            phase =
                when (owner) {
                    RuntimeOwner.VpnService,
                    RuntimeOwner.RootDaemon -> localPhase

                    RuntimeOwner.RemoteController -> RuntimePhase.Running
                    RuntimeOwner.None -> RuntimePhase.Idle
                },
            runMode = runMode,
            startedAt =
                when (owner) {
                    RuntimeOwner.VpnService,
                    RuntimeOwner.RootDaemon -> localStartedAt

                    RuntimeOwner.RemoteController,
                    RuntimeOwner.None -> null
                },
        )

    /** The root daemon is still the runtime. Used when a replacement start fails before the swap. */
    fun liveRootSnapshot(profile: Profile, generation: Long, lastError: String?): RuntimeSnapshot? {
        if (!isRootDaemonActive()) return null
        val mode = localModeForOwner(RuntimeOwner.RootDaemon) ?: return null
        return activeSnapshot(
            owner = RuntimeOwner.RootDaemon,
            runMode = mode,
            localPhase = localRuntimePhaseForOwner(RuntimeOwner.RootDaemon),
            localStartedAt = localRuntimeStartedAtForOwner(RuntimeOwner.RootDaemon),
        ).copy(
            profileReady = true,
            profileUuid = profile.uuid.toString(),
            profileName = profile.name,
            generation = generation,
            lastError = lastError,
        )
    }

    fun startedSnapshot(
        current: RuntimeSnapshot,
        owner: RuntimeOwner,
        runMode: RunMode,
    ): RuntimeSnapshot =
        current.copy(
            owner = owner,
            phase = RuntimePhase.Running,
            runMode = runMode,
            lastError = null,
        )

    fun remoteRunningSnapshot(
        runMode: RunMode,
        generation: Long,
        startedAt: Long = System.currentTimeMillis(),
    ): RuntimeSnapshot =
        RuntimeSnapshot(
            owner = RuntimeOwner.RemoteController,
            phase = RuntimePhase.Running,
            runMode = runMode,
            generation = generation,
            startedAt = startedAt,
        )

}
