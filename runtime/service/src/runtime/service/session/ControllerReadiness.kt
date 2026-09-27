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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

/**
 * Startup wait shared by the VPN child and the root daemon: the core usually answers within a few
 * tens of milliseconds, so the retry pause starts at 25ms and backs off to 100ms inside a 2s
 * budget. Cancellation of the caller aborts the wait immediately.
 */
internal object ControllerReadiness {
    private val BACKOFF_MS = longArrayOf(25L, 50L, 100L)
    private const val TIMEOUT_MS = 2_000L

    /**
     * Returns once [probe] reports ready. [ensureAlive] runs before every attempt and throws when
     * the core process is already gone; [onTimeout] builds the failure from the last probe error.
     */
    suspend fun await(
        ensureAlive: () -> Unit,
        probe: suspend () -> Boolean,
        onTimeout: (lastError: Throwable?) -> Nothing,
    ) {
        val deadline = SystemClock.elapsedRealtime() + TIMEOUT_MS
        var lastError: Throwable? = null
        var attempt = 0
        while (true) {
            ensureAlive()
            val remaining = deadline - SystemClock.elapsedRealtime()
            if (remaining <= 0L) onTimeout(lastError)
            try {
                if (withTimeout(remaining) { probe() }) return
            } catch (error: TimeoutCancellationException) {
                lastError = error
            } catch (error: CancellationException) {
                throw error
            } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
                lastError = error
            }
            val pause =
                minOf(
                    BACKOFF_MS[minOf(attempt, BACKOFF_MS.lastIndex)],
                    deadline - SystemClock.elapsedRealtime(),
                )
            attempt++
            if (pause > 0L) delay(pause)
        }
    }
}
