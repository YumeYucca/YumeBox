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
import java.io.Closeable

/**
 * The socketpair control channel to the core child process ([NativeProcess.channelFd]). It is a SOCK_SEQPACKET socket,
 * so one write is one read (no framing needed), and each message can carry a single file descriptor via SCM_RIGHTS —
 * this is how the TUN fd is handed to the core in the non-root VpnService path.
 *
 * YumeBox's equivalent of CFA's `AndroidChannel`. This is a thin fd wrapper; message framing and the command vocabulary
 * live in the Kotlin layer above it.
 */
@Keep
class Channel(private val fd: Int) : Closeable {
    @Volatile private var closed = false

    /**
     * Read one datagram into [buffer] at [offset]/[length]. Returns the number of bytes read (0 means the peer closed
     * the channel). If the peer attached a descriptor, it is returned; otherwise the returned fd is -1.
     */
    @Synchronized
    fun readMessage(buffer: ByteArray, offset: Int, length: Int): ReadResult {
        requireOpen()
        requireSlice(buffer, offset, length)
        val fdHolder = intArrayOf(-1)
        val count = nativeReadMessage(fd, buffer, offset, length, fdHolder)
        return ReadResult(count = count, fd = fdHolder[0])
    }

    /**
     * Write one datagram from [buffer] at [offset]/[length], optionally attaching [attachFd] via SCM_RIGHTS (pass -1
     * for no descriptor). Returns the number of bytes written.
     */
    @Synchronized
    fun writeMessage(buffer: ByteArray, offset: Int, length: Int, attachFd: Int = -1): Int {
        requireOpen()
        requireSlice(buffer, offset, length)
        return nativeWriteMessage(fd, buffer, offset, length, attachFd)
    }

    /** Close the channel fd once. Signals EOF to the peer. */
    @Synchronized
    override fun close() {
        if (closed) return
        closed = true
        android.os.ParcelFileDescriptor.adoptFd(fd).close()
    }

    private fun requireOpen() {
        check(!closed) { "Channel is closed" }
    }

    private fun requireSlice(buffer: ByteArray, offset: Int, length: Int) {
        require(offset >= 0 && length >= 0 && offset <= buffer.size - length) {
            "Invalid buffer slice: offset=$offset length=$length size=${buffer.size}"
        }
    }

    data class ReadResult(val count: Int, val fd: Int)

    companion object {
        init {
            CompatNative.ensureLoaded()
        }

        @JvmStatic
        private external fun nativeReadMessage(
            fd: Int,
            buffer: ByteArray,
            offset: Int,
            length: Int,
            fdHolder: IntArray,
        ): Int

        @JvmStatic
        private external fun nativeWriteMessage(
            fd: Int,
            buffer: ByteArray,
            offset: Int,
            length: Int,
            attachFd: Int,
        ): Int
    }
}
