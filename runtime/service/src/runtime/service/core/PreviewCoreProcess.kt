/*
 * This file is part of YumeBox.
 *
 * Copyright (c) YumeYucca 2025 - Present
 */

package com.github.yumeyucca.yumebox.runtime.service.core

import android.content.Context
import android.os.SystemClock
import com.github.yumeyucca.yumebox.core.bridge.Channel
import com.github.yumeyucca.yumebox.core.bridge.NativeProcess
import com.github.yumeyucca.yumebox.core.util.runtimeHomeDir
import com.github.yumeyucca.yumebox.runtime.service.controller.CoreController
import java.io.File
import java.util.UUID
import timber.log.Timber

/**
 * Owns the inspect-only core child. It does not share the root record, the VPN endpoint, or the
 * socket-owner channel. A real core launch reaps this child, and this child does not fork while
 * that core is alive.
 */
class PreviewCoreProcess(private val context: Context) {
    private var process: NativeProcess? = null
    private var controller: CoreController? = null

    fun start(config: String): Unit = CoreProcess.withLifecycleLock {
        synchronized(gate) {
            if (CoreProcess.realCoreReserved()) {
                error("preview refused while a runtime core is active")
            }
            stopLocked()
            // Compiled provider paths and the geo/MMDB assets are rooted at runtimeHomeDir. Keep that
            // as the core home, but use a nested process workdir so preview diagnostics cannot overwrite
            // the real core's core.log.
            val home = context.runtimeHomeDir.apply { mkdirs() }
            val workdir = File(home, PREVIEW_WORKDIR).apply { mkdirs() }
            File(home, SOCK).delete()
            val (runtimeConfig, secret) = ensureControllerSecret(config)
            val nextEndpoint = CoreEndpoint(File(home, SOCK).absolutePath, secret)
            val args =
                arrayOf(
                    "--home",
                    home.absolutePath,
                    "--controller",
                    nextEndpoint.sock,
                    "--mode",
                    "preview",
                )
            val proc = spawn(home, workdir, args)
            try {
                Channel(proc.channelFd).use { channel ->
                    val bytes = runtimeConfig.toByteArray(Charsets.UTF_8)
                    var offset = 0
                    while (offset < bytes.size) {
                        val length = minOf(CHUNK, bytes.size - offset)
                        channel.writeMessage(bytes, offset, length)
                        offset += length
                    }
                    // Closing the parent socket is the complete preview handoff. No descriptor and no
                    // post-start RPC are ever sent to this process.
                }
            } catch (error: Throwable) {
                runCatching { proc.kill() }
                waitUntilExited(proc.pid)
                throw IllegalStateException("preview config handoff failed", error)
            }
            process = proc
            active = this
            controller = CoreController(local = CoreController.Local(nextEndpoint.sock) { nextEndpoint.secret })
            Timber.tag(TAG).i("preview core launched, pid=%d", proc.pid)
        }
    }

    fun stop() {
        synchronized(gate) { stopLocked() }
    }

    private fun stopLocked() {
        if (active === this) active = null
        val previous = process
        process = null
        controller = null
        if (previous != null) {
            reap(previous)
        }
    }

    fun isAlive(): Boolean = synchronized(gate) {
        val pid = process?.pid ?: return@synchronized false
        // On some Android builds `kill(pid, 0)` from the app process is intermittently denied
        // while a just-forked child is still completing exec. The preview PID is app-owned and
        // short-lived, so its proc entry is the reliable liveness signal for this handle.
        File("/proc/$pid").exists()
    }

    fun controller(): CoreController = synchronized(gate) {
        checkNotNull(controller) { "Preview core is not running" }
    }

    private fun spawn(home: File, workdir: File, args: Array<String>): NativeProcess {
        val launchArgs = CoreArtifacts.previewArguments(context, args)
        return try {
            NativeProcess.start(CoreArtifacts.previewShell(context).absolutePath, launchArgs, workdir.absolutePath)
        } catch (error: Throwable) {
            Timber.tag(TAG).w(error, "preview exec from nativeLibraryDir failed; retrying extracted copy")
            NativeProcess.start(extractBin().absolutePath, launchArgs, workdir.absolutePath)
        }
    }

    private fun extractBin(): File {
        val source = CoreArtifacts.previewShell(context)
        val target = File(context.filesDir, "bin/preview")
        target.parentFile?.mkdirs()
        if (!target.exists() || target.length() != source.length()) {
            source.copyTo(target, overwrite = true)
        }
        target.setReadable(true, false)
        target.setExecutable(true, false)
        return target
    }

    private fun ensureControllerSecret(config: String): Pair<String, String> {
        val secret = UUID.randomUUID().toString().replace("-", "")
        val line = "secret: \"$secret\""
        val lines = config.lineSequence().toMutableList()
        val index = lines.indexOfFirst { it.trimStart().startsWith("secret:") }
        if (index >= 0) {
            lines[index] = line
        } else {
            lines.add(0, line)
        }
        return lines.joinToString("\n") to secret
    }

    companion object {
        const val TAG = "PreviewCoreProcess"
        const val SOCK = "preview.sock"
        const val PREVIEW_WORKDIR = "preview"
        const val CHUNK = 32 * 1024
        const val PREVIEW_STOP_GRACE_MS = 300L
        const val PREVIEW_STOP_KILL_WAIT_MS = 200L
        const val PREVIEW_STOP_POLL_MS = 25L

        private val gate = Any()
        private var active: PreviewCoreProcess? = null

        internal fun stopActive() {
            synchronized(gate) { active?.stopLocked() }
        }

        private fun reap(process: NativeProcess) {
            runCatching { process.terminate() }
            if (waitUntilExited(process.pid, PREVIEW_STOP_GRACE_MS)) return
            runCatching { process.kill() }
            waitUntilExited(process.pid, PREVIEW_STOP_KILL_WAIT_MS)
        }

        private fun waitUntilExited(pid: Int, timeoutMs: Long = PREVIEW_STOP_KILL_WAIT_MS): Boolean {
            val deadline = SystemClock.elapsedRealtime() + timeoutMs
            while (File("/proc/$pid").exists() && SystemClock.elapsedRealtime() < deadline) {
                Thread.sleep(PREVIEW_STOP_POLL_MS)
            }
            return !File("/proc/$pid").exists()
        }
    }
}
