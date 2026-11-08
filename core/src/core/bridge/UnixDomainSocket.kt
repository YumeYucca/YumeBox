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

package com.github.yumeyucca.yumebox.core.bridge

import android.system.ErrnoException
import android.system.Os
import java.io.FileDescriptor
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.*

/**
 * A [java.net.Socket] backed by a UNIX-domain connection to the local core's mihomo controller, so an OkHttp
 * `SocketFactory` can speak HTTP over it. YumeBox's analogue of CFA's `UnixSocket`.
 *
 * Constructed with no [SocketImpl] (`super(null as SocketImpl?)`) so the JDK never allocates a real TCP impl; every
 * method OkHttp touches is overridden to delegate to the underlying fd. Stream closes are decoupled from the fd
 * (reads/writes go through [Os] on the raw descriptor, and only [close] releases it) so OkHttp reading the response
 * while the request stream is done does not tear down the connection.
 */
class UnixDomainSocket(private val path: String) : Socket(null as SocketImpl?) {

    private var connection: UnixSocket? = null
    private var soTimeoutMs: Int = 0
    private var connected = false
    private var closed = false

    override fun connect(endpoint: SocketAddress?) = connect(endpoint, 0)

    override fun connect(endpoint: SocketAddress?, timeout: Int) {
        require(endpoint != null) { "endpoint == null" }
        require(timeout >= 0) { "timeout < 0" }
        if (closed) throw SocketException("Socket is closed")
        if (connected) throw SocketException("Socket is already connected")

        val conn = UnixSocket.connect(path, timeout)
        try {
            if (soTimeoutMs > 0) conn.setSoTimeout(soTimeoutMs)
            connection = conn
            connected = true
        } catch (error: Throwable) {
            runCatching { conn.close() }
            throw error
        }
    }

    private fun requireFd(): FileDescriptor =
        (connection ?: throw IOException("UnixDomainSocket not connected")).fileDescriptor

    override fun getInputStream(): InputStream = FdInputStream(requireFd())

    override fun getOutputStream(): OutputStream = FdOutputStream(requireFd())

    override fun setSoTimeout(timeout: Int) {
        require(timeout >= 0) { "timeout < 0" }
        soTimeoutMs = timeout
        connection?.setSoTimeout(timeout)
    }

    override fun getSoTimeout(): Int = connection?.getSoTimeout() ?: soTimeoutMs

    override fun close() {
        if (closed) return
        closed = true
        connection?.let { conn -> runCatching { conn.close() } }
        connection = null
    }

    // --- Socket contract OkHttp probes; unix sockets have no meaningful TCP semantics ---
    override fun setTcpNoDelay(on: Boolean) = Unit

    override fun getTcpNoDelay(): Boolean = true

    override fun setKeepAlive(on: Boolean) = Unit

    override fun getKeepAlive(): Boolean = false

    override fun bind(bindpoint: SocketAddress?) = Unit

    override fun isConnected(): Boolean = connected

    override fun isBound(): Boolean = connected

    override fun isClosed(): Boolean = closed

    override fun isInputShutdown(): Boolean = false

    override fun isOutputShutdown(): Boolean = false

    override fun getInetAddress(): InetAddress = InetAddress.getLoopbackAddress()

    override fun getRemoteSocketAddress(): SocketAddress = InetSocketAddress(InetAddress.getLoopbackAddress(), 0)

    override fun getLocalSocketAddress(): SocketAddress = InetSocketAddress(InetAddress.getLoopbackAddress(), 0)

    private class FdInputStream(private val fd: FileDescriptor) : InputStream() {
        private val one = ByteArray(1)

        override fun read(): Int = if (read(one, 0, 1) <= 0) -1 else one[0].toInt() and 0xff

        override fun read(b: ByteArray, off: Int, len: Int): Int =
            try {
                val n = Os.read(fd, b, off, len)
                if (n == 0) -1 else n
            } catch (e: ErrnoException) {
                throw IOException(e)
            }

        override fun close() = Unit // fd is owned by the socket
    }

    private class FdOutputStream(private val fd: FileDescriptor) : OutputStream() {
        override fun write(b: Int) = write(byteArrayOf(b.toByte()), 0, 1)

        override fun write(b: ByteArray, off: Int, len: Int) {
            var offset = off
            var remaining = len
            while (remaining > 0) {
                val n =
                    try {
                        Os.write(fd, b, offset, remaining)
                    } catch (e: ErrnoException) {
                        throw IOException(e)
                    }
                offset += n
                remaining -= n
            }
        }

        override fun close() = Unit // fd is owned by the socket
    }
}
