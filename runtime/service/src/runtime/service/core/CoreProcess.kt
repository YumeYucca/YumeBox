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

@file:Suppress("SimplifiableCallChain", "CanConvertToMultiDollarString", "CanUnescapeDollarLiteral")

package com.github.yumeyucca.yumebox.runtime.service.core

import android.annotation.SuppressLint
import android.content.Context
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.system.Os
import com.github.yumeyucca.yumebox.core.bridge.Channel
import com.github.yumeyucca.yumebox.core.bridge.NativeProcess
import com.github.yumeyucca.yumebox.core.model.RunMode
import com.github.yumeyucca.yumebox.core.util.runtimeHomeDir
import com.github.yumeyucca.yumebox.runtime.api.CoreApi
import com.github.yumeyucca.yumebox.runtime.service.controller.CoreController
import com.github.yumeyucca.yumebox.runtime.service.util.SocketOwnerResolver
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.*

/** The running core's UNIX REST controller: socket path + bearer secret. */
data class CoreEndpoint(val sock: String, val secret: String)

/**
 * Launches and owns the out-of-process mihomo core. A tiny `libmihomo.so` PIE shell is fork+exec'd
 * from nativeLibraryDir and dlopens the compressed `libmihomocore.so` payload, then the existing
 * socketpair and REST protocols operate unchanged.
 *
 * Egress: the tun uses a userspace stack (gVisor or MIPS), so excluding the app's own uid from
 * the VpnService tunnel keeps the core's egress off it — no per-socket protect needed.
 */
class CoreProcess(private val context: Context) {

    private var process: NativeProcess? = null
    private var ownerChannel: Channel? = null
    private var ownerQueryThread: Thread? = null

    /** Fork the core for VpnService mode, deliver [config] and [tunFd], and publish [current]. */
    fun startVpn(
        tunFd: Int,
        gateway: String,
        dns: String,
        config: String,
        stack: String,
    ): CoreEndpoint {
        val home = context.runtimeHomeDir.apply { mkdirs() }
        prepareSelectorCache(home)
        // Fresh core.log for this launch (libcompat redirects stdout/stderr there after chdir).
        File(home, CORE_LOG).delete()
        // Drop any leftover controller node so readiness probes cannot hit a dead socket.
        File(home, SOCK).delete()
        val sock = File(home, SOCK).absolutePath
        // Keep the controller secret out of argv (visible to other processes / root shell).
        // Profiles often omit `secret:`; mint one so REST auth and readiness stay consistent.
        val (runtimeConfig, secret) = ensureControllerSecret(config)

        val args =
            arrayOf(
                "--home",
                home.absolutePath,
                "--controller",
                sock,
                "--gateway",
                gateway,
                "--dns",
                dns,
                "--stack",
                stack,
                "--mode",
                "vpn",
                "--sdk",
                Build.VERSION.SDK_INT.toString(),
            )
        Timber.tag(TAG).i("launch core, tunFd=%d", tunFd)
        val proc = spawn(home, args)
        process = proc
        running = proc

        // Stream config over the socketpair (in memory), then the TUN fd as a terminating
        // SCM_RIGHTS
        // message. The core dups the fd; the app closes its own copy.
        val handoff =
            runCatching {
                val channel = Channel(proc.channelFd)
                var ownershipRpcStarted = false
                try {
                    val bytes = runtimeConfig.toByteArray(Charsets.UTF_8)
                    var offset = 0
                    while (offset < bytes.size) {
                        val len = minOf(CHUNK, bytes.size - offset)
                        channel.writeMessage(bytes, offset, len)
                        offset += len
                    }
                    channel.writeMessage(END, 0, END.size, attachFd = tunFd)
                    startOwnerQueryLoop(channel)
                    ownershipRpcStarted = true
                } finally {
                    if (!ownershipRpcStarted) {
                        channel.close()
                    }
                }
            }
        runCatching { ParcelFileDescriptor.adoptFd(tunFd).close() }
        handoff.getOrElse { error ->
            runCatching { proc.kill() }
            stopOwnerQueryLoop()
            if (running === proc) running = null
            if (process === proc) process = null
            current = null
            throw IllegalStateException("config/fd handoff failed", error)
        }

        return CoreEndpoint(sock, secret).also { current = it }
    }

    private fun startOwnerQueryLoop(channel: Channel) {
        val resolver = SocketOwnerResolver(context)
        ownerChannel = channel
        ownerQueryThread =
            Thread {
                    val buffer = ByteArray(OWNER_QUERY_BUFFER_SIZE)
                    try {
                        while (true) {
                            val result = channel.readMessage(buffer, 0, buffer.size)
                            if (result.count <= 0) break
                            val request = buffer.decodeToString(0, result.count)
                            val response =
                                if (result.fd >= 0) {
                                    if (request == PROTECT_SOCKET_REQUEST) {
                                        protectCoreSocket(result.fd)
                                    } else {
                                        closeReceivedFd(result.fd)
                                        PROTECT_SOCKET_DENIED
                                    }
                                } else {
                                    resolveOwnerQuery(resolver, request)
                                }
                            val responseBytes = response.toByteArray(Charsets.UTF_8)
                            channel.writeMessage(responseBytes, 0, responseBytes.size)
                        }
                    } catch (error: Throwable) {
                        if (ownerChannel === channel && isLocalCoreAlive()) {
                            Timber.tag(TAG).w(error, "socket owner RPC stopped unexpectedly")
                        }
                    } finally {
                        if (ownerChannel === channel) {
                            ownerChannel = null
                            ownerQueryThread = null
                        }
                        runCatching { channel.close() }
                    }
                }
                .apply {
                    name = "Core-SocketOwner"
                    isDaemon = true
                    start()
                }
    }

    /** Protect only the child core's socket; regular app traffic must remain inside the VPN. */
    private fun protectCoreSocket(fd: Int): String =
        try {
            if ((context as? VpnService)?.protect(fd) == true) {
                PROTECT_SOCKET_OK
            } else {
                Timber.tag(TAG).w("VpnService refused core socket protection")
                PROTECT_SOCKET_DENIED
            }
        } finally {
            closeReceivedFd(fd)
        }

    private fun closeReceivedFd(fd: Int) {
        runCatching { ParcelFileDescriptor.adoptFd(fd).close() }
    }

    private fun resolveOwnerQuery(resolver: SocketOwnerResolver, request: String): String {
        val fields = request.split('\t', limit = 3)
        if (fields.size != 3) return UNKNOWN_SOCKET_OWNER
        val protocol = fields[0].toIntOrNull() ?: return UNKNOWN_SOCKET_OWNER
        val source = parseSocketAddress(fields[1]) ?: return UNKNOWN_SOCKET_OWNER
        val target = parseSocketAddress(fields[2]) ?: return UNKNOWN_SOCKET_OWNER
        return resolver.queryOwner(protocol, source, target)
    }

    private fun parseSocketAddress(value: String): InetSocketAddress? = runCatching {
        val (host, portText) =
            if (value.startsWith('[')) {
                val closingBracket = value.indexOf(']')
                require(closingBracket > 1 && value.getOrNull(closingBracket + 1) == ':')
                value.substring(1, closingBracket) to value.substring(closingBracket + 2)
            } else {
                val separator = value.lastIndexOf(':')
                require(separator > 0)
                value.substring(0, separator) to value.substring(separator + 1)
            }
        InetSocketAddress(InetAddress.getByName(host), portText.toInt())
    }.getOrNull()

    /**
     * Launch the core as a detached ROOT Tun daemon via `su`: it runs in the root
     * SELinux domain (free to open a kernel TUN and program routes) and, unlike the VPN
     * child core, outlives the app process — reattached over the REST socket ([reconnectRoot]).
     * [mode] = "tun" or "ebpf".
     */
    fun startRoot(mode: String, config: String): CoreEndpoint {
        awaitRootStopGrace()
        val home = context.runtimeHomeDir.apply { mkdirs() }
        prepareSelectorCache(home)
        File(home, SOCK).delete()
        val sock = File(home, SOCK).absolutePath
        val (runtimeConfig, secret) = ensureControllerSecret(config)

        // A detached `su` daemon can't inherit the config socketpair the VPN core streams over, so
        // hand the compiled config (proxy secrets) through a named pipe instead of a file: the core
        // reads it once via --config and nothing is ever written to disk — the same
        // no-plaintext-at-
        // rest posture as VPN. Drop any legacy plaintext run.yaml an older build left behind.
        File(home, LEGACY_ROOT_CONFIG).delete()
        val fifo = File(home, ROOT_CONFIG_PIPE).apply { delete() }
        Os.mkfifo(fifo.absolutePath, ROOT_PIPE_MODE)

        val shell = CoreArtifacts.shell(context).absolutePath
        val coreLibrary = CoreArtifacts.library(context).absolutePath
        val logFile = File(home, CORE_LOG).absolutePath
        val command =
            "exec ${quote(shell)} ${CoreArtifacts.LIBRARY_OPTION} ${quote(coreLibrary)} " +
                "--mode $mode --home ${quote(home.absolutePath)} " +
                "--sdk ${Build.VERSION.SDK_INT} " +
                "--controller ${quote(sock)} " +
                "--config ${quote(fifo.absolutePath)} " +
                "</dev/null >${quote(logFile)} 2>&1 & echo \$!"

        Timber.tag(TAG).i("launch root core, mode=%s", mode)
        val result = Shell.cmd(command).exec()
        val pid = result.out.asSequence().mapNotNull { it.trim().toIntOrNull() }.firstOrNull()
        if (!(result.isSuccess && pid != null && pid > 0)) {
            fifo.delete()
            error("root core launch failed (success=${result.isSuccess} out=${result.out})")
        }

        // Feed the config into the pipe; the core's ReadFile blocks until we open+write. Run it on
        // a
        // daemon thread with a timeout so a core that died on launch (no reader) can't block the
        // caller forever; then unlink the pipe node (it holds nothing at rest either way).
        val writer = Thread {
            runCatching {
                    FileOutputStream(fifo).use {
                        it.write(runtimeConfig.toByteArray(Charsets.UTF_8))
                    }
                }
                .onFailure { Timber.tag(TAG).w(it, "root config pipe write failed") }
        }
            .apply {
                isDaemon = true
                start()
            }
        writer.join(FIFO_WRITE_TIMEOUT_MS)
        if (writer.isAlive) {
            // No reader turned up: open one ourselves to release the blocked writer thread. The
            // dead
            // daemon then surfaces via the launcher's startup probe (core.log shows the read
            // failure).
            Timber.tag(TAG).w("root config handoff timed out; core likely died on launch")
            runCatching { FileInputStream(fifo).use { it.readBytes() } }
        }
        fifo.delete()

        RootDaemonState.save(
            RootDaemonState.Record(
                pid = pid,
                secret = secret,
                mode = mode,
                startTimeTicks = rootProcessStartTimeTicks(pid) ?: 0L,
            )
        )
        Timber.tag(TAG).i("root core launched, pid=%d mode=%s", pid, mode)
        return CoreEndpoint(sock, secret).also { current = it }
    }

    /**
     * Both root and VPN cores use this home directory. The cache must remain owned by the app UID:
     * root can read it, while the VPN child cannot recover from a root-owned BoltDB file.
     */
    private fun prepareSelectorCache(home: File) {
        val cache = File(home, SELECTOR_CACHE)
        if (cache.canRead() && cache.canWrite()) return

        if (cache.exists()) {
            val uid = context.applicationInfo.uid
            val repaired =
                runCatching {
                        Shell.cmd(
                                "chown $uid:$uid ${quote(cache.absolutePath)} && " +
                                    "chmod 0600 ${quote(cache.absolutePath)}"
                            )
                            .exec()
                            .isSuccess
                    }
                    .getOrDefault(false)
            if (repaired && cache.canRead() && cache.canWrite()) {
                Timber.tag(TAG).i("restored selector cache ownership to app uid=%d", uid)
                return
            }

            // The app owns the parent directory, so it can replace a stale root-owned cache even
            // when root access has since been revoked. This only discards an inaccessible cache.
            check(cache.delete()) { "Unable to replace inaccessible selector cache" }
            Timber.tag(TAG).w("discarded inaccessible selector cache after ownership repair failed")
        }

        check(cache.createNewFile()) { "Unable to create selector cache" }
        cache.setReadable(true, true)
        cache.setWritable(true, true)
        check(cache.canRead() && cache.canWrite()) { "Selector cache is not accessible to app" }
    }

    fun stop() {
        val stoppedProcess = process
        stoppedProcess?.let(::stopVpnProcess)
        stopOwnerQueryLoop()
        if (running === stoppedProcess) running = null
        process = null
        current = null
    }

    /**
     * A selector change is persisted by mihomo during its orderly shutdown. The VpnService child
     * used to be killed here, which bypassed that flush and lost every selected node on restart.
     */
    private fun stopVpnProcess(process: NativeProcess) {
        runCatching { process.terminate() }
        val deadline = SystemClock.elapsedRealtime() + VPN_STOP_GRACE_MS
        while (isVpnProcessAlive(process.pid) && SystemClock.elapsedRealtime() < deadline) {
            Thread.sleep(VPN_STOP_POLL_MS)
        }
        if (isVpnProcessAlive(process.pid)) {
            Timber.tag(TAG).w("VPN core did not exit after SIGTERM; sending SIGKILL")
            runCatching { process.kill() }
        }
    }

    private fun isVpnProcessAlive(pid: Int): Boolean =
        runCatching {
                Os.kill(pid, 0)
                true
            }
            .getOrDefault(false)

    private fun stopOwnerQueryLoop() {
        val channel = ownerChannel
        ownerChannel = null
        runCatching { channel?.close() }
        ownerQueryThread = null
    }

    /**
     * Guarantee a non-empty controller bearer for local REST. Reuses the profile secret when
     * present; otherwise mints one and patches the runtime config.
     */
    private fun ensureControllerSecret(config: String): Pair<String, String> {
        secretFromConfig(config)?.let {
            return config to it
        }
        val secret = UUID.randomUUID().toString().replace("-", "")
        val line = "secret: \"$secret\""
        val lines = config.lineSequence().toMutableList()
        val idx = lines.indexOfFirst { it.trimStart().startsWith("secret:") }
        if (idx >= 0) {
            lines[idx] = line
        } else {
            lines.add(0, line)
        }
        return lines.joinToString("\n") to secret
    }

    /** The top-level `secret:` from the compiled config, or null if the profile sets none. */
    private fun secretFromConfig(config: String): String? {
        val raw =
            config
                .lineSequence()
                .firstOrNull { it.trimStart().startsWith("secret:") }
                ?.substringAfter("secret:")
                ?.trim() ?: return null
        return raw.trim('"', '\'').trim().takeIf { it.isNotEmpty() }
    }

    /**
     * Fork the core in [home]: the nativeLibraryDir copy first (non-root exec path), then an
     * extracted, chmod'd copy if the kernel refuses that exec (real exec failures now propagate).
     */
    private fun spawn(home: File, args: Array<String>): NativeProcess {
        val wd = home.absolutePath
        val bundled = CoreArtifacts.shell(context).absolutePath
        val launchArgs = CoreArtifacts.arguments(context, args)
        return try {
            NativeProcess.start(bundled, launchArgs, workdir = wd)
        } catch (t: Throwable) {
            Timber.tag(TAG).w(t, "exec from nativeLibraryDir failed; retrying from extracted copy")
            NativeProcess.start(extractBin().absolutePath, launchArgs, workdir = wd)
        }
    }

    /** Copy the PIE shell out of nativeLibraryDir into app storage and chmod it executable. */
    @SuppressLint("SetWorldReadable")
    private fun extractBin(): File {
        val src = CoreArtifacts.shell(context)
        val dst = File(context.filesDir, "bin/mihomo")
        dst.parentFile?.mkdirs()
        if (!dst.exists() || dst.length() != src.length()) {
            src.copyTo(dst, overwrite = true)
        }
        dst.setReadable(true, false)
        dst.setExecutable(true, false)
        return dst
    }

    companion object {
        /**
         * The endpoint of the core currently running (null when stopped). Read by the controller
         * client.
         */
        @Volatile
        var current: CoreEndpoint? = null
            private set

        /**
         * The running core child, tracked statically for timeout recovery (see [killRunning]).
         */
        @Volatile private var running: NativeProcess? = null

        /** Last-resort SIGKILL of the VPN child after its normal SIGTERM shutdown timed out. */
        fun killRunning() {
            running?.let { runCatching { it.kill() } }
            running = null
        }

        /** True if the persisted mihomo root process still has the recorded process identity. */
        fun isRootCoreAlive(): Boolean {
            val record = RootDaemonState.load() ?: return false
            return isRootRecordAlive(record)
        }

        /** True if the persisted root daemon still has the recorded process identity. */
        fun isRootDaemonAlive(): Boolean = isRootCoreAlive()

        private fun isRootRecordAlive(record: RootDaemonState.Record): Boolean {
            val alive =
                runCatching { Shell.cmd("kill -0 ${record.pid}").exec().isSuccess }
                    .getOrDefault(false)
            if (!alive) return false

            val executable =
                runCatching {
                        Shell.cmd("readlink /proc/${record.pid}/exe")
                            .exec()
                            .out
                            .firstOrNull()
                            ?.substringBefore(" (deleted)")
                            ?.let(::File)
                            ?.name
                    }
                    .getOrNull()
            if (executable !in ROOT_CORE_EXECUTABLE_NAMES) return false

            val recordedStartTime = record.startTimeTicks
            return recordedStartTime <= 0L ||
                rootProcessStartTimeTicks(record.pid) == recordedStartTime
        }

        private fun rootProcessStartTimeTicks(pid: Int): Long? =
            runCatching {
                    val stat = Shell.cmd("cat /proc/$pid/stat").exec().out.joinToString(" ")
                    stat.substringAfterLast(") ", missingDelimiterValue = "")
                        .split(Regex("\\s+"))
                        .getOrNull(PROC_STAT_START_TIME_INDEX_AFTER_COMM)
                        ?.toLongOrNull()
                }
                .getOrNull()

        /**
         * True if the non-root VPN child core is still alive. Used by LOCAL_TUN startup verify so a
         * dead process fails immediately instead of spinning on a missing clash.sock.
         */
        fun isLocalCoreAlive(): Boolean {
            val pid = running?.pid ?: return false
            return runCatching {
                    Os.kill(pid, 0)
                    true
                }
                .getOrDefault(false)
        }

        /**
         * The run mode of the persisted root daemon ("tun"/"ebpf" → [RunMode]), or null when
         * none.
         */
        fun rootDaemonMode(): RunMode? = RunMode.fromCoreArg(RootDaemonState.load()?.mode)

        /** Last non-blank line of `<runtimeHome>/core.log`. */
        fun coreLogTail(context: Context): String? = runCatching {
            context.runtimeHomeDir
                .resolve(CORE_LOG)
                .takeIf { it.exists() }
                ?.readLines()
                ?.lastOrNull { it.isNotBlank() }
                ?.trim()
                ?.take(300)
        }
            .getOrNull()

        /** Full `core.log` (Go already pins log-level=error + boot markers). */
        fun coreDiagnosticLog(context: Context): String = runCatching {
            val file = context.runtimeHomeDir.resolve(CORE_LOG)
            if (!file.exists()) return@runCatching ""
            file.readText().trimEnd()
        }
            .getOrDefault("")

        /**
         * Reattach to a live root daemon after an app restart: probe liveness and republish
         * [current] from the persisted secret without relaunching. Returns the mode, or null
         * (clearing stale state).
         */
        fun reconnectRoot(context: Context): String? {
            val record = RootDaemonState.load() ?: return null
            if (!isRootRecordAlive(record)) {
                RootDaemonState.clear()
                return null
            }
            current = CoreEndpoint(context.runtimeHomeDir.resolve(SOCK).absolutePath, record.secret)
            return record.mode
        }

        /** Stops the detached root runtime. */
        fun stopRoot(context: Context) {
            stopRoot()
        }

        // The su kill returns fast, but libsu's shell round-trip + mihomo's SIGTERM teardown
        // (Tun route/rule cleanup) adds latency the stop path must not
        // block on.
        private val stopScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /** Pid of a daemon whose SIGTERM teardown is still running; see [awaitRootStopGrace]. */
        @Volatile private var dyingRootPid: Int? = null

        /**
         * Explicitly stop the root daemon: SIGTERM so mihomo tears down its tun/iptables state, a
         * bounded grace for that teardown, then SIGKILL to close the window for good.
         */
        fun stopRoot() {
            val record = RootDaemonState.load()
            // Clear state FIRST so isRootDaemonAlive() reports "stopped" immediately; the UI
            // must never wait on the kill.
            RootDaemonState.clear()
            current = null
            record ?: return
            if (!isRootRecordAlive(record)) return
            dyingRootPid = record.pid
            stopScope.launch {
                try {
                    runCatching { Shell.cmd("kill -TERM ${record.pid}").exec() }
                    val deadline = SystemClock.elapsedRealtime() + ROOT_STOP_GRACE_MS
                    while (
                        SystemClock.elapsedRealtime() < deadline &&
                            isRootRecordAlive(record)
                    ) {
                        delay(ROOT_STOP_POLL_MS)
                    }
                    if (isRootRecordAlive(record)) {
                        runCatching { Shell.cmd("kill -KILL ${record.pid}").exec() }
                    }
                } finally {
                    dyingRootPid = null
                }
            }
        }

        /**
         * Block (bounded) until a dying predecessor has finished tearing down. The daemon's ip
         * rules, nftables table and iptables chains all carry fixed names, so a teardown that
         * outlives the stop can dismantle what a freshly launched successor just set up.
         */
        fun awaitRootStopGrace() {
            val deadline = SystemClock.elapsedRealtime() + ROOT_STOP_GRACE_MS + ROOT_STOP_POLL_MS
            while (dyingRootPid != null && SystemClock.elapsedRealtime() < deadline) {
                Thread.sleep(ROOT_STOP_POLL_MS)
            }
        }

        private const val ROOT_STOP_GRACE_MS = 2_000L
        private const val ROOT_STOP_POLL_MS = 100L
        private const val VPN_STOP_GRACE_MS = 2_000L
        private const val VPN_STOP_POLL_MS = 25L

        // Config is delivered over this named pipe (never persisted); LEGACY_ROOT_CONFIG is the old
        // plaintext file, deleted on launch. 0600 = owner-only (app creates it, root reads it).
        private const val ROOT_CONFIG_PIPE = "run.pipe"
        private const val LEGACY_ROOT_CONFIG = "run.yaml"
        private const val ROOT_PIPE_MODE = 384
        private const val FIFO_WRITE_TIMEOUT_MS = 5000L
        private const val SELECTOR_CACHE = "cache.db"

        /** Core stdout/stderr log under [runtimeHomeDir]; launcher redirects both modes here. */
        const val CORE_LOG = "core.log"

        private fun quote(value: String): String = "'" + value.replace("'", "'\\''") + "'"

        /** Controller socket filename under the runtime home dir. Read by the client controller. */
        const val SOCK = "clash.sock"

        @Volatile private var controller: CoreController? = null

        /** Shared local-core controller client (unix socket path fixed, secret from [current]). */
        fun controller(context: Context): CoreApi = sharedController(context)

        /** Suspendable startup probe so launch deadlines are not hidden by the synchronous API. */
        internal suspend fun probeController(context: Context) {
            sharedController(context).queryTunnelStateAsync()
        }

        private fun sharedController(context: Context): CoreController =
            controller
                ?: CoreController(
                        local =
                            CoreController.Local(
                                socketPath = context.runtimeHomeDir.resolve(SOCK).absolutePath,
                                secret = { current?.secret.orEmpty() },
                            )
                    )
                    .also { controller = it }

        private const val TAG = "CoreProcess"
        private val ROOT_CORE_EXECUTABLE_NAMES = setOf(CoreArtifacts.SHELL_NAME, "mihomo")
        // After stripping "pid (comm) ", index 0 is field 3 (state), so field 22 is index 19.
        private const val PROC_STAT_START_TIME_INDEX_AFTER_COMM = 19
        private const val CHUNK = 32 * 1024
        private const val OWNER_QUERY_BUFFER_SIZE = 4096
        private const val UNKNOWN_SOCKET_OWNER = "-1\t"
        private const val PROTECT_SOCKET_REQUEST = "protect"
        private const val PROTECT_SOCKET_OK = "protected"
        private const val PROTECT_SOCKET_DENIED = "denied"
        private val END = byteArrayOf(1)
    }
}
