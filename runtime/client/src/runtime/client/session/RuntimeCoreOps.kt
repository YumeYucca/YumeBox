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
import com.github.yumeyucca.yumebox.runtime.client.CoreQueries
import com.github.yumeyucca.yumebox.runtime.client.access.RuntimeAccess

/** Shared CoreApi resolve + query helpers used by the runtime client facade. */
internal class RuntimeCoreOps(private val connect: suspend () -> Unit = {}) {
    suspend fun api(): CoreApi {
        connect()
        return RuntimeAccess.core()
    }

    suspend fun queryTrafficNow(): Long = CoreQueries.queryTrafficNow(api())

    suspend fun queryTrafficTotal(): Long = CoreQueries.queryTrafficTotal(api())

    suspend fun queryConnections(): ConnectionSnapshot = CoreQueries.queryConnections(api())

    suspend fun queryAllProxyGroups(excludeNotSelectable: Boolean): List<ProxyGroup> =
        CoreQueries.queryAllProxyGroups(api(), excludeNotSelectable)

    suspend fun queryProxyGroup(name: String, sort: ProxySort): ProxyGroup =
        CoreQueries.queryProxyGroup(api(), name, sort)

    suspend fun patchSelector(group: String, name: String): Boolean =
        CoreQueries.patchSelector(api(), group, name)

    suspend fun healthCheck(group: String): Map<String, Int> = CoreQueries.healthCheck(api(), group)

    suspend fun healthCheckProxy(group: String, proxyName: String): Int =
        CoreQueries.healthCheckProxy(api(), group, proxyName)
}
