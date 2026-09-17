/*
 * This file is part of YumeBox.
 *
 * Copyright (c) YumeYucca 2025 - Present
 */

package com.github.yumeyucca.yumebox.runtime.client.session

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

/** Three attempts take at most 700 ms, leaving room for the 250 ms watchdog interval. */
internal object RemoteControllerProbe {
    private const val ATTEMPTS = 3
    private const val ATTEMPT_TIMEOUT_MS = 200L
    private const val RETRY_DELAY_MS = 50L

    suspend fun isReachable(probe: suspend () -> Boolean): Boolean {
        repeat(ATTEMPTS) { attempt ->
            val reachable =
                try {
                    withTimeoutOrNull(ATTEMPT_TIMEOUT_MS) { probe() } == true
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    Timber.d(error, "Remote controller probe skipped")
                    false
                }
            if (reachable) return true
            if (attempt < ATTEMPTS - 1) delay(RETRY_DELAY_MS)
        }
        return false
    }
}
