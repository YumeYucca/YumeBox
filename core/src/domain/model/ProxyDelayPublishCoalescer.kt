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
 * Copyright (c) YumeYucca 2025 - Present
 *
 */
package com.github.yumeyucca.yumebox.domain.model

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Coalesces direct delay publishes during a group test. Single-node probes flush immediately
 * because they run outside [session].
 */
class ProxyDelayPublishCoalescer(
    private val minFlushCount: Int = PROXY_DELAY_TEST_PARALLELISM,
    private val minFlushIntervalMs: Long = DEFAULT_FLUSH_INTERVAL_MS,
    private val nowMs: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()
    private val pending = linkedMapOf<String, Int>()
    private val active = AtomicBoolean(false)
    private var lastFlushAtMs = 0L
    private var sessionFlush: (suspend (Map<String, Int>) -> Unit)? = null

    val isActive: Boolean
        get() = active.get()

    suspend fun <T> session(
        flush: suspend (Map<String, Int>) -> Unit,
        block: suspend () -> T,
    ): T {
        mutex.withLock {
            pending.clear()
            lastFlushAtMs = nowMs()
            sessionFlush = flush
            active.set(true)
        }
        try {
            return block()
        } finally {
            drainUntilInactive()
        }
    }

    /** @return true if a group-test session accepted the delays. */
    suspend fun enqueue(delays: Map<String, Int>): Boolean {
        val measured = delays.filterValues { delay -> delay != 0 }
        if (measured.isEmpty()) return true
        val batch =
            mutex.withLock {
                val flush = sessionFlush
                if (!active.get() || flush == null) {
                    return@withLock null
                }
                pending.putAll(measured)
                val due = takeIfDue()
                if (due == null) {
                    DrainBatch(emptyMap(), flush, queuedOnly = true)
                } else {
                    DrainBatch(due, flush, queuedOnly = false)
                }
            } ?: return false
        if (!batch.queuedOnly) {
            batch.flush(batch.delays)
        }
        return true
    }

    private fun takeIfDue(): Map<String, Int>? {
        val now = nowMs()
        val due = pending.size >= minFlushCount || now - lastFlushAtMs >= minFlushIntervalMs
        if (!due || pending.isEmpty()) return null
        lastFlushAtMs = now
        return pending.toMap().also { pending.clear() }
    }

    private suspend fun drainUntilInactive() {
        while (true) {
            val batch =
                mutex.withLock {
                    val flush = sessionFlush
                    if (flush == null || pending.isEmpty()) {
                        sessionFlush = null
                        active.set(false)
                        pending.clear()
                        return@withLock null
                    }
                    lastFlushAtMs = nowMs()
                    DrainBatch(pending.toMap().also { pending.clear() }, flush, queuedOnly = false)
                } ?: return
            batch.flush(batch.delays)
        }
    }

    private data class DrainBatch(
        val delays: Map<String, Int>,
        val flush: suspend (Map<String, Int>) -> Unit,
        val queuedOnly: Boolean,
    )

    private companion object {
        const val DEFAULT_FLUSH_INTERVAL_MS = 250L
    }
}
