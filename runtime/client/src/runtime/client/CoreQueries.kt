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

package com.github.yumeyucca.yumebox.runtime.client

import com.github.yumeyucca.yumebox.core.model.*
import com.github.yumeyucca.yumebox.runtime.api.CoreApi
import com.github.yumeyucca.yumebox.runtime.api.CoreAsyncQueries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Suspend entry for controller queries. Uses [CoreAsyncQueries] when available so the hot path
 * never nests runBlocking; falls back to [CoreApi] on IO for legacy implementations.
 */
object CoreQueries {
    private suspend fun <T> query(
        api: CoreApi,
        async: suspend CoreAsyncQueries.() -> T,
        sync: CoreApi.() -> T,
    ): T {
        val asyncApi = api as? CoreAsyncQueries
        return if (asyncApi != null) {
            asyncApi.async()
        } else {
            withContext(Dispatchers.IO) { api.sync() }
        }
    }

    suspend fun queryTunnelState(api: CoreApi): TunnelState =
        query(api, { queryTunnelStateAsync() }, { queryTunnelState() })

    suspend fun queryTrafficNow(api: CoreApi): Long =
        query(api, { queryTrafficNowAsync() }, { queryTrafficNow() })

    suspend fun queryTrafficTotal(api: CoreApi): Long =
        query(api, { queryTrafficTotalAsync() }, { queryTrafficTotal() })

    suspend fun queryConnections(api: CoreApi): ConnectionSnapshot =
        query(api, { queryConnectionsAsync() }, { queryConnections() })

    suspend fun queryAllProxyGroups(api: CoreApi, excludeNotSelectable: Boolean): List<ProxyGroup> =
        query(
            api,
            { queryAllProxyGroupsAsync(excludeNotSelectable) },
            { queryAllProxyGroups(excludeNotSelectable) },
        )

    suspend fun queryProxyGroup(api: CoreApi, name: String, sort: ProxySort): ProxyGroup =
        query(api, { queryProxyGroupAsync(name, sort) }, { queryProxyGroup(name, sort) })

    suspend fun queryConfiguration(api: CoreApi): UiConfiguration =
        query(api, { queryConfigurationAsync() }, { queryConfiguration() })

    suspend fun queryProviders(api: CoreApi): ProviderList =
        query(api, { queryProvidersAsync() }, { queryProviders() })

    suspend fun queryRules(api: CoreApi): List<RuntimeRule> =
        query(api, { queryRulesAsync() }, { queryRules() })

    suspend fun patchSelector(api: CoreApi, group: String, name: String): Boolean =
        query(api, { patchSelectorAsync(group, name) }, { patchSelector(group, name) })

    suspend fun closeConnection(api: CoreApi, id: String): Boolean =
        query(api, { closeConnectionAsync(id) }, { closeConnection(id) })

    suspend fun closeAllConnections(api: CoreApi) =
        query(api, { closeAllConnectionsAsync() }, { closeAllConnections() })

    suspend fun healthCheck(api: CoreApi, group: String): Map<String, Int> = api.healthCheck(group)

    suspend fun healthCheckProxy(api: CoreApi, group: String, proxyName: String): Int =
        api.healthCheckProxy(group, proxyName)

    suspend fun updateProvider(api: CoreApi, type: Provider.Type, name: String) =
        api.updateProvider(type, name)
}
