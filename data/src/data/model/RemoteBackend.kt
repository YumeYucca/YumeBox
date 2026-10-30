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

package com.github.yumeyucca.yumebox.data.model

import kotlinx.serialization.Serializable

/**
 * A saved external mihomo controller backend (RESTful API endpoint).
 *
 * When external-controller mode is active, the app steers this backend via its REST API instead of
 * running a local core. [secret] is sent as a `Authorization: Bearer <secret>` header (blank = no
 * auth configured).
 */
@Serializable
enum class RemoteProtocol(
    val scheme: String,
) {
    HTTP("http"),
    HTTPS("https"),
}

@Serializable
data class RemoteBackend(
    val id: String,
    val name: String,
    val host: String,
    val port: Int,
    val secret: String = "",
    val protocol: RemoteProtocol = RemoteProtocol.HTTP,
) {
    /** Base URL assembled from [protocol], [host], and [port]. */
    val baseUrl: String
        get() = "${protocol.scheme}://${host.trim()}:$port"

    /** Base URL with any trailing slash stripped so paths can be appended directly. */
    val normalizedBaseUrl: String
        get() = baseUrl.trimEnd('/')

    /** Human-facing label: the user's own name, or `host:port` when they never gave one. */
    val displayName: String
        get() = name.ifBlank { "$host:$port" }

    companion object {
        fun newId(): String =
            java.util.UUID
                .randomUUID()
                .toString()
    }
}
