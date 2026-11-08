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

package com.github.yumeyucca.yumebox.common.util

import android.net.Uri
import com.github.yumeyucca.yumebox.data.model.RemoteBackend

/** Builds a dashboard URL that opens its controller setup with the selected backend. */
fun buildRemotePanelUrl(
    panelUrl: String,
    backend: RemoteBackend,
): String {
    val query =
        Uri.Builder()
            .appendQueryParameter(backend.protocol.scheme, "true")
            .appendQueryParameter("hostname", backend.host)
            .appendQueryParameter("port", backend.port.toString())
            .appendQueryParameter("secret", backend.secret)
            .build()
            .encodedQuery
            .orEmpty()
    return "${panelUrl.trimEnd('/')}/#/setup?$query"
}

/** Android 17 local-network protection does not apply to connections within this process. */
fun RemoteBackend.requiresLocalNetworkPermission(): Boolean =
    host.trim().lowercase() !in setOf("127.0.0.1", "::1", "[::1]", "localhost")
