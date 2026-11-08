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

@file:Suppress("UnusedSymbol")

package com.github.yumeyucca.yumebox.core.bridge

import androidx.annotation.Keep

/**
 * A handle to the PIE core shell running as a child process, launched via fork+execve with a socketpair control
 * channel. YumeBox's equivalent of CFA's `AndroidProcess`.
 *
 * [channelFd] is the parent-side end of the socketpair; wrap it in a [Channel] to exchange control messages (and to
 * hand the TUN fd to the core via SCM_RIGHTS in the non-root path). The core reads its own end from the `CHANNEL`
 * environment variable.
 *
 * NOTE: this is the *non-root* launch primitive (the app fork+execs the binary directly). The root path launches the
 * same binary through `su` (libsu `Shell`) instead and talks to it purely over the UNIX controller socket; it does not
 * use [start].
 */
@Keep
class NativeProcess
private constructor(
    val pid: Int,
    val channelFd: Int,
) {
    /** Ask the child to shut down cleanly so its persistent state can be flushed. */
    fun terminate() = nativeTerminate(pid)

    /** Send SIGKILL to the child. Safe to call more than once. */
    fun kill() = nativeKill(pid)

    companion object {
        init {
            CompatNative.ensureLoaded()
        }

        /**
         * Fork and execve [path] (which must be readable and executable — i.e. an extracted-and-chmod'd copy under the
         * app's storage, or a file living in `nativeLibraryDir`). [args] are appended after `argv[0] = path`. The child
         * `chdir`s into [workdir] (the core home) before exec, redirects stdout/stderr to `<workdir>/core.log`, and
         * receives the channel socket fd through `CHANNEL=<fd>`.
         *
         * The call blocks until the child has execve'd (or failed to): a pre-exec failure in the child is reported back
         * and rethrown here, so a returned handle always means the core is running.
         *
         * @throws java.io.IOException if the file is not executable, chdir/socketpair/fork fails, or the child could
         *   not exec [path].
         */
        fun start(path: String, args: Array<String>, workdir: String): NativeProcess {
            val handle = nativeStart(path, args, workdir)
            return NativeProcess(pid = handle[0], channelFd = handle[1])
        }

        @JvmStatic
        private external fun nativeStart(
            path: String,
            args: Array<String>,
            workdir: String,
        ): IntArray

        @JvmStatic private external fun nativeTerminate(pid: Int)

        @JvmStatic private external fun nativeKill(pid: Int)
    }
}
