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

import java.net.InetAddress
import java.net.Socket
import javax.net.SocketFactory

/**
 * A [SocketFactory] whose sockets always connect to a fixed UNIX-domain path (the local core's mihomo controller),
 * regardless of the host/port OkHttp resolves from the request URL. Plug this into an OkHttp client to speak HTTP over
 * the controller socket:
 * ```
 * OkHttpClient.Builder().socketFactory(UnixSocketFactory(socketPath)).build()
 * ```
 */
class UnixSocketFactory(private val path: String) : SocketFactory() {
    override fun createSocket(): Socket = UnixDomainSocket(path)

    override fun createSocket(host: String?, port: Int): Socket = connectedSocket(host, port)

    override fun createSocket(
        host: String?,
        port: Int,
        localHost: InetAddress?,
        localPort: Int,
    ): Socket = connectedSocket(host, port)

    override fun createSocket(host: InetAddress?, port: Int): Socket = connectedSocket(host, port)

    override fun createSocket(
        address: InetAddress?,
        port: Int,
        localAddress: InetAddress?,
        localPort: Int,
    ): Socket = connectedSocket(address, port)

    private fun connectedSocket(host: String?, port: Int): Socket =
        UnixDomainSocket(path).apply { connect(java.net.InetSocketAddress.createUnresolved(host ?: "localhost", port)) }

    private fun connectedSocket(address: InetAddress?, port: Int): Socket =
        UnixDomainSocket(path).apply {
            connect(java.net.InetSocketAddress(address ?: InetAddress.getLoopbackAddress(), port))
        }
}
