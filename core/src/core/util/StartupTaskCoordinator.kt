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

package com.github.yumeyucca.yumebox.core.util

import kotlinx.coroutines.*
import timber.log.Timber

object StartupTaskCoordinator {
    private val fallback = CompletableDeferred(Unit)

    @Volatile private var warmup: Deferred<Unit>? = null

    @Suppress("TooGenericExceptionCaught")
    fun startWarmup(scope: CoroutineScope, block: suspend () -> Unit) {
        if (warmup != null) return
        synchronized(this) {
            if (warmup != null) return
            warmup = scope.async {
                try {
                    block()
                } catch (error: Throwable) { // fault barrier: log any warmup failure before rethrowing
                    if (error is CancellationException) throw error
                    Timber.e(error, "Runtime warmup failed")
                    throw error
                }
            }
        }
    }

    suspend fun awaitWarmup() {
        (warmup ?: fallback).await()
    }
}
