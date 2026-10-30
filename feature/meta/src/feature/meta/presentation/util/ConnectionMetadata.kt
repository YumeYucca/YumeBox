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

package com.github.yumeyucca.yumebox.feature.meta.presentation.util

import com.github.yumeyucca.yumebox.core.model.ConnectionInfo
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val DEFAULT_NETWORK = "TCP"

/** Raw string of a metadata field; the core omits absent fields entirely. */
internal fun JsonObject.stringOrEmpty(key: String): String =
    get(key)?.jsonPrimitive?.content.orEmpty()

/** Transport of a connection; the core leaves plain TCP implicit. */
internal fun ConnectionInfo.networkName(): String =
    metadata.stringOrEmpty("network").ifEmpty { DEFAULT_NETWORK }

/** `host:port` when the port is known, the bare host otherwise, and empty without a host. */
internal fun hostPort(host: String, port: String): String =
    when {
        host.isEmpty() -> ""
        port.isEmpty() -> host
        else -> "$host:$port"
    }

/**
 * Single-line target identifying a connection: card title, detail header, and the search / sort
 * key. Host wins over destination, which wins over source; the port is appended when known.
 */
internal fun ConnectionInfo.displayTarget(): String {
    val host = metadata.stringOrEmpty("host")
    val destinationPort = metadata.stringOrEmpty("destinationPort")
    val destinationIP = metadata.stringOrEmpty("destinationIP")
    val sourceIP = metadata.stringOrEmpty("sourceIP")
    val sourcePort = metadata.stringOrEmpty("sourcePort")

    return when {
        host.isNotEmpty() -> hostPort(host, destinationPort)
        destinationIP.isNotEmpty() -> hostPort(destinationIP, destinationPort)
        else -> hostPort(sourceIP, sourcePort)
    }
}
