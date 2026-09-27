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

package com.github.yumeyucca.yumebox.runtime.client.session

import com.github.yumeyucca.yumebox.core.model.ConnectionSnapshot
import com.github.yumeyucca.yumebox.core.model.ProxyGroup
import com.github.yumeyucca.yumebox.core.model.ProxySort
import com.github.yumeyucca.yumebox.runtime.api.CoreApi
import com.github.yumeyucca.yumebox.runtime.client.access.RuntimeAccess

/** Connects the backend on demand, then queries the routed controller. */
internal class RuntimeCoreOps(private val connect: suspend () -> Unit = {}) {
    private suspend fun api(): CoreApi {
        connect()
        return RuntimeAccess.core()
    }

    suspend fun queryTrafficNow(): Long = api().queryTrafficNow()

    suspend fun queryTrafficTotal(): Long = api().queryTrafficTotal()

    suspend fun queryConnections(): ConnectionSnapshot = api().queryConnections()

    suspend fun queryAllProxyGroups(excludeNotSelectable: Boolean): List<ProxyGroup> =
        api().queryAllProxyGroups(excludeNotSelectable)

    suspend fun queryProxyGroup(name: String, sort: ProxySort): ProxyGroup =
        api().queryProxyGroup(name, sort)

    suspend fun patchSelector(group: String, name: String): Boolean =
        api().patchSelector(group, name)

    suspend fun healthCheck(group: String): Map<String, Int> = api().healthCheck(group)

    suspend fun healthCheckProxy(group: String, proxyName: String): Int =
        api().healthCheckProxy(group, proxyName)
}
