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

package com.github.yumeyucca.yumebox.runtime.service.core

import android.os.SystemClock
import com.topjohnwu.superuser.Shell
import java.io.File

/**
 * Root pid checks go through libsu's persistent shell. [CoreProcess] still owns the lifecycle
 * lock and the controller endpoint.
 */
internal object RootDaemonProbe {
    fun trackedAlive(): Boolean {
        val pid = RootDaemonState.load()?.pid ?: return false
        if (pid <= 0) return false
        return exec("kill -0 $pid")?.isSuccess == true
    }

    fun isAlive(): Boolean {
        val record = RootDaemonState.load() ?: return false
        val now = SystemClock.elapsedRealtime()
        val cached = liveness
        if (
            cached != null &&
                cached.pid == record.pid &&
                cached.startTimeTicks == record.startTimeTicks &&
                now - cached.checkedAt <= CACHE_MS
        ) {
            return cached.alive
        }
        val alive =
            identity(record)
                ?: return cached != null && cached.pid == record.pid && cached.alive
        liveness = Liveness(record.pid, record.startTimeTicks, alive, now)
        return alive
    }

    /**
     * Null when the root shell itself failed. False is a dead or reused pid. Only a definite
     * answer is cached, so a failed shell does not mark the daemon stopped.
     */
    fun identity(record: RootDaemonState.Record): Boolean? =
        when (val state = pidState(record.pid)) {
            PidState.Unreachable -> null
            PidState.Gone -> false
            is PidState.Present ->
                if (state.executable in EXECUTABLE_NAMES) startTimeMatches(record) else false
        }

    fun reap(record: RootDaemonState.Record): Boolean =
        when (val state = pidState(record.pid)) {
            PidState.Unreachable -> false
            PidState.Gone -> true
            is PidState.Present ->
                when (state.executable) {
                    null -> false
                    in EXECUTABLE_NAMES -> signal(record.pid)
                    else -> true
                }
        }

    /** Persists [record] and the liveness cache together. */
    fun commit(record: RootDaemonState.Record) {
        RootDaemonState.save(record)
        liveness =
            Liveness(
                pid = record.pid,
                startTimeTicks = record.startTimeTicks,
                alive = true,
                checkedAt = SystemClock.elapsedRealtime(),
            )
    }

    /** Drops the persisted record and the liveness cache together. */
    fun discard() {
        RootDaemonState.clear()
        liveness = null
    }

    fun startTimeTicks(pid: Int): Long? {
        val stat = exec("cat /proc/$pid/stat") ?: return null
        if (!stat.isSuccess) return null
        return startTimeTicks(stat.out.joinToString(" "))
    }

    private fun signal(pid: Int): Boolean {
        val terminated = exec("kill -TERM $pid") ?: return false
        if (!terminated.isSuccess || waitUntilDead(pid, TERM_ATTEMPTS)) return true
        exec("kill -KILL $pid")
        return waitUntilDead(pid, KILL_ATTEMPTS)
    }

    private fun waitUntilDead(pid: Int, attempts: Int): Boolean {
        repeat(attempts) { attempt ->
            val result = exec("kill -0 $pid") ?: return false
            if (!result.isSuccess) return true
            if (attempt < attempts - 1) Thread.sleep(POLL_MS)
        }
        return false
    }

    private fun pidState(pid: Int): PidState {
        val alive = exec("kill -0 $pid") ?: return PidState.Unreachable
        if (!alive.isSuccess) return PidState.Gone
        val link = exec("readlink /proc/$pid/exe") ?: return PidState.Unreachable
        if (!link.isSuccess) return PidState.Present(executable = null)
        val name = link.out.firstOrNull()?.substringBefore(" (deleted)")?.let(::File)?.name
        return PidState.Present(name)
    }

    private fun startTimeMatches(record: RootDaemonState.Record): Boolean? {
        val stat = exec("cat /proc/${record.pid}/stat") ?: return null
        if (!stat.isSuccess) return false
        val ticks = startTimeTicks(stat.out.joinToString(" ")) ?: return false
        return record.startTimeTicks <= 0L || ticks == record.startTimeTicks
    }

    private fun exec(command: String) = runCatching { Shell.cmd(command).exec() }.getOrNull()

    private fun startTimeTicks(stat: String): Long? =
        stat.substringAfterLast(") ", missingDelimiterValue = "")
            .split(Regex("\\s+"))
            .getOrNull(START_TIME_INDEX_AFTER_COMM)
            ?.toLongOrNull()

    private sealed interface PidState {
        data object Unreachable : PidState

        data object Gone : PidState

        data class Present(val executable: String?) : PidState
    }

    private data class Liveness(
        val pid: Int,
        val startTimeTicks: Long,
        val alive: Boolean,
        val checkedAt: Long,
    )

    @Volatile private var liveness: Liveness? = null

    private val EXECUTABLE_NAMES = setOf(CoreArtifacts.SHELL_NAME, "mihomo")
    // After stripping "pid (comm) ", index 0 is field 3 (state), so field 22 is index 19.
    private const val START_TIME_INDEX_AFTER_COMM = 19
    private const val CACHE_MS = 400L
    private const val POLL_MS = 50L
    private const val TERM_ATTEMPTS = 40
    private const val KILL_ATTEMPTS = 10
}
