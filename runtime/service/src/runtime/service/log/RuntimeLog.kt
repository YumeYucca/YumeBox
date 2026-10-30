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

package com.github.yumeyucca.yumebox.runtime.service.log

import android.content.Context
import com.github.yumeyucca.yumebox.core.model.RunMode
import com.github.yumeyucca.yumebox.runtime.api.appContextOrSelf
import com.github.yumeyucca.yumebox.runtime.service.log.RuntimeLog.lock
import java.io.File
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Runtime diagnostics for the current app process.
 *
 * Everything that explains the current VPN or root startup lands in one file: launchers, services,
 * transports, session state, and verification results. Profile operations, live logs, and raw core
 * output do not belong in this file.
 *
 * A fresh file is created for every runtime start. Writes inside that start are append-only; the
 * previous start is discarded instead of being mixed into the new one. Lines use compact columns:
 * ```
 * 07-26 09:12:33 I [LOCAL/SESSION]       started elapsedMs=812
 * 07-26 09:12:34 W [LOCAL/VERIFY]        actualGroups=0 controller=down attempt=1/10
 * 07-26 09:12:35 E [ROOT/LAUNCHER]       root start failed | IllegalStateException: no controller
 * ```
 *
 * `<month-day time> <level> [<source>/<type>] <message>[ | <cause chain>]`. The source and type are
 * declared values rather than arbitrary strings, so tags remain stable and searchable.
 *
 * Writes are serialized and completed before returning so a process failure cannot discard queued
 * startup diagnostics.
 */
object RuntimeLog {

    enum class Level(val symbol: Char) {
        Info('I'),
        Warn('W'),
        Error('E'),
    }

    /**
     * Where an entry came from. One short keyword each, so a reader can filter a whole subsystem
     * out by eye or by `grep`.
     */
    enum class Source(val tag: String) {
        /** [RunMode.VpnService] session: launcher, service, transport, session runtime. */
        LocalTun("LOCAL"),

        /** Root `tun` daemon session, same span of stages as [LocalTun]. */
        RootTun("ROOT"),

        /** Root mihomo session using its native eBPF listener. */
        RootEbpf("EBPF");

        companion object {
            fun forMode(mode: RunMode): Source =
                when (mode) {
                    RunMode.VpnService -> LocalTun
                    RunMode.Tun -> RootTun
                    RunMode.Ebpf -> RootEbpf
                }
        }
    }

    /** Stable event families shown after the level. */
    enum class Type(val tag: String) {
        Launcher("LAUNCHER"),
        Session("SESSION"),
        Reload("RELOAD"),
        Verify("VERIFY"),
        Transport("TRANSPORT"),
        AutoStart("AUTO_START"),
    }

    /** Bound writer, so call sites don't repeat the context and source on every line. */
    class Writer internal constructor(
        private val context: Context,
        val source: Source,
    ) {
        fun i(type: Type, message: String) =
            RuntimeLog.write(context, Level.Info, source, type, message)

        fun w(type: Type, message: String, error: Throwable? = null) =
            RuntimeLog.write(context, Level.Warn, source, type, message, error)

        fun e(type: Type, message: String, error: Throwable? = null) =
            RuntimeLog.write(context, Level.Error, source, type, message, error)

        /** Starts a fresh append-only file containing only this runtime attempt. */
        fun beginSession(type: Type, message: String) {
            RuntimeLog.startNewFile(context)
            i(type, message)
        }

    }

    fun writer(context: Context, source: Source): Writer = Writer(context.appContextOrSelf, source)

    fun writer(context: Context, mode: RunMode): Writer = writer(context, Source.forMode(mode))

    private fun startNewFile(context: Context) {
        val appContext = context.appContextOrSelf
        synchronized(lock) {
            runCatching {
                clearPreviousLogs(appContext)
                val file = file(appContext)
                file.parentFile?.mkdirs()
                file.createNewFile()
            }
        }
    }

    fun write(
        context: Context,
        level: Level,
        source: Source,
        type: Type,
        message: String,
        error: Throwable? = null,
    ) {
        if (message.isBlank() && error == null) return
        append(context, format(level, source, type, message, error))
    }

    /** The whole log, oldest first. Used by the About screen's export. */
    fun snapshot(context: Context): String =
        runCatching {
                synchronized(lock) {
                    val file = file(context)
                    if (file.exists()) file.readText(StandardCharsets.UTF_8) else ""
                }
            }
            .getOrDefault("")

    private fun file(context: Context): File =
        File(context.appContextOrSelf.filesDir, FILE_NAME)

    private fun format(
        level: Level,
        source: Source,
        type: Type,
        message: String,
        error: Throwable?,
    ): String {
        val timestamp = TIMESTAMP.format(Instant.now().atZone(ZoneId.systemDefault()))
        val origin = "[${source.tag}/${type.tag}]"
        val body = message.trimEnd().ifBlank { "(no message)" }
        return buildString {
            append(timestamp)
            append(' ')
            append(level.symbol)
            append(' ')
            append(origin.padEnd(TYPE_COLUMN_WIDTH))
            append(body)
            error?.let { append(" | ").append(describe(it)) }
        }
    }

    /**
     * Full cause chain, because the outermost message is routinely the least informative one — a
     * generic "runtime start failed" wrapping the actual parse error reported by the core.
     */
    private fun describe(error: Throwable): String {
        val chain = StringBuilder()
        var current: Throwable? = error
        var depth = 0
        while (current != null && depth < MAX_CAUSE_DEPTH) {
            val node = current
            if (depth > 0) chain.append(" <- ")
            chain.append(node::class.java.simpleName)
            node.message?.takeIf(String::isNotBlank)?.let { chain.append(": ").append(it) }
            val cause = node.cause
            current = if (cause === node) null else cause
            depth++
        }
        return chain.toString().take(MAX_ERROR_CHARS)
    }

    private fun append(context: Context, line: String) {
        val appContext = context.appContextOrSelf
        val normalized = "${line.trimEnd()}\n"
        synchronized(lock) {
            runCatching {
                val file = file(appContext)
                file.parentFile?.mkdirs()
                file.appendText(normalized, StandardCharsets.UTF_8)
            }
        }
    }

    /** Caller holds [lock]. Only the file for the current start may remain. */
    private fun clearPreviousLogs(context: Context) {
        context.filesDir
            .listFiles { file ->
                file.isFile &&
                    file.name.startsWith(OLD_FILE_PREFIX) &&
                    file.extension == FILE_EXTENSION
            }
            ?.forEach { runCatching { it.delete() } }
        LEGACY_FILE_NAMES.forEach { name ->
            runCatching { File(context.filesDir, name).delete() }
        }
    }

    const val FILE_NAME = "runtime.log"
    private const val OLD_FILE_PREFIX = "runtime_"
    private const val FILE_EXTENSION = "log"
    private const val MAX_CAUSE_DEPTH = 5
    private const val MAX_ERROR_CHARS = 2_000

    // Fits the widest current tag plus two spaces; longer tags simply extend the message column.
    private const val TYPE_COLUMN_WIDTH = 22

    private val LEGACY_FILE_NAMES =
        listOf(FILE_NAME, "local_tun_startup.log", "root_tun_startup.log")

    // Local wall-clock, seconds precision: enough to order a startup, and what a person actually
    // reads. Sub-second churn and a zone suffix on every line were noise.
    private val TIMESTAMP: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MM-dd HH:mm:ss", Locale.ROOT)
    private val lock = Any()
}
