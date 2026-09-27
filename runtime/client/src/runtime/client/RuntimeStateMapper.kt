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

package com.github.yumeyucca.yumebox.runtime.client

import com.github.yumeyucca.yumebox.core.model.RunMode
import com.github.yumeyucca.yumebox.runtime.api.RuntimeOwner
import com.github.yumeyucca.yumebox.runtime.api.RuntimePhase
import com.github.yumeyucca.yumebox.runtime.api.RuntimeSnapshot

object RuntimeStateMapper {
    fun isRunningOrStarting(snapshot: RuntimeSnapshot): Boolean =
        snapshot.phase == RuntimePhase.Starting || snapshot.phase == RuntimePhase.Running

    fun isActuallyRunning(snapshot: RuntimeSnapshot): Boolean =
        snapshot.phase == RuntimePhase.Running

    fun resolveDisplayMode(snapshot: RuntimeSnapshot, configuredMode: RunMode): RunMode =
        if (isRunningOrStarting(snapshot)) snapshot.runMode else configuredMode

    fun idleSnapshot(
        configuredMode: RunMode,
        generation: Long = 0L,
        lastError: String? = null,
    ): RuntimeSnapshot =
        RuntimeSnapshot(
            owner = RuntimeOwner.None,
            phase = if (lastError.isNullOrBlank()) RuntimePhase.Idle else RuntimePhase.Failed,
            runMode = configuredMode,
            lastError = lastError,
            generation = generation,
        )
}
