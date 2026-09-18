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

    val isActive: Boolean
        get() = active.get()

    suspend fun <T> session(
        flush: suspend (Map<String, Int>) -> Unit,
        block: suspend () -> T,
    ): T {
        mutex.withLock {
            pending.clear()
            lastFlushAtMs = nowMs()
            active.set(true)
        }
        try {
            return block()
        } finally {
            try {
                drain(flush)
            } finally {
                mutex.withLock {
                    pending.clear()
                    active.set(false)
                }
            }
        }
    }

    suspend fun enqueue(
        delays: Map<String, Int>,
        flush: suspend (Map<String, Int>) -> Unit,
    ): Boolean {
        val measured = delays.filterValues { delay -> delay != 0 }
        if (measured.isEmpty()) return true
        val toFlush =
            mutex.withLock {
                if (!active.get()) {
                    measured
                } else {
                    pending.putAll(measured)
                    takeIfDue()
                }
            }
        if (toFlush != null) {
            flush(toFlush)
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

    private suspend fun drain(flush: suspend (Map<String, Int>) -> Unit) {
        val leftover =
            mutex.withLock {
                if (pending.isEmpty()) {
                    null
                } else {
                    lastFlushAtMs = nowMs()
                    pending.toMap().also { pending.clear() }
                }
            } ?: return
        flush(leftover)
    }

    private companion object {
        const val DEFAULT_FLUSH_INTERVAL_MS = 250L
    }
}
