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

package com.github.yumeyucca.yumebox.data.controller

import com.github.yumeyucca.yumebox.core.model.RunMode
import com.github.yumeyucca.yumebox.data.model.AccessControlMode
import com.github.yumeyucca.yumebox.data.store.NetworkSettingsStore
import kotlinx.coroutines.CancellationException
import timber.log.Timber

/** Merges a burst of package/mode edits into a single proxy restart. */
private const val AccessControlApplyDebounceMillis = 350L

class AccessControlController(
    private val store: NetworkSettingsStore,
    isRunning: () -> Boolean,
    private val resolveActiveMode: () -> RunMode?,
    private val restartProxy: suspend (RunMode) -> Unit,
    private val beforeRestart: suspend (RunMode) -> Unit = {},
) {
    private val restarter =
        DebouncedProxyRestarter(
            timerName = "access_control_apply_packages",
            debounceMillis = AccessControlApplyDebounceMillis,
            isRunning = isRunning,
        )

    fun setAccessControlMode(mode: AccessControlMode) {
        if (store.accessControlMode.value == mode) return
        store.accessControlMode.set(mode)
        scheduleApply()
    }

    fun applyPackages(packages: Set<String>) {
        if (store.accessControlPackages.value == packages) return
        store.accessControlPackages.set(packages)
        scheduleApply()
    }

    private fun scheduleApply() {
        restarter.schedule {
            val targetMode = resolveActiveMode() ?: store.runMode.value
            // Accept-all has no uid list to hand over, so nothing is applied here: the running
            // session keeps the spec it was started with until it is restarted.
            if (store.accessControlMode.value == AccessControlMode.ALLOW_ALL) return@schedule

            try {
                beforeRestart(targetMode)
                restartProxy(targetMode)
            } catch (error: CancellationException) {
                throw error
            } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
                // Best-effort restart: a failed apply keeps the current session running.
                Timber.e(error, "Access control change could not be applied")
            }
        }
    }
}
