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

@file:Suppress("ConvertLongToDuration")

package com.github.yumeyucca.yumebox.runtime.client.access

import com.github.yumeyucca.yumebox.core.model.*
import com.github.yumeyucca.yumebox.runtime.api.CoreApi
import com.github.yumeyucca.yumebox.runtime.api.CoreAsyncQueries
import com.github.yumeyucca.yumebox.runtime.api.LogObserver
import com.github.yumeyucca.yumebox.runtime.api.LogSubscription
import kotlinx.coroutines.*

/** Routes [CoreApi] to remote controller when active, otherwise the local controller. */
class CoreRouter(
    private val local: CoreApi,
    private val remote: CoreApi,
    private val isRemoteControllerActive: () -> Boolean,
) : CoreApi, CoreAsyncQueries {
    private val logScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var logRoutingJob: Job? = null
    private var activeLogToken: Any? = null
    private var routedLogSubscription: LogSubscription? = null

    private fun pick(): CoreApi = if (isRemoteControllerActive()) remote else local

    private suspend fun <T> routeAsync(
        block: suspend CoreAsyncQueries.() -> T,
        sync: CoreApi.() -> T,
    ): T {
        val target = pick()
        val async = target as? CoreAsyncQueries
        return if (async != null) async.block() else sync(target)
    }

    override suspend fun queryTunnelStateAsync(): TunnelState =
        routeAsync({ queryTunnelStateAsync() }, { queryTunnelState() })

    override suspend fun queryTrafficNowAsync(): Long =
        routeAsync({ queryTrafficNowAsync() }, { queryTrafficNow() })

    override suspend fun queryTrafficTotalAsync(): Long =
        routeAsync({ queryTrafficTotalAsync() }, { queryTrafficTotal() })

    override suspend fun queryConnectionsAsync(): ConnectionSnapshot =
        routeAsync({ queryConnectionsAsync() }, { queryConnections() })

    override suspend fun queryAllProxyGroupsAsync(excludeNotSelectable: Boolean): List<ProxyGroup> =
        routeAsync(
            { queryAllProxyGroupsAsync(excludeNotSelectable) },
            { queryAllProxyGroups(excludeNotSelectable) },
        )

    override suspend fun queryProxyGroupNamesAsync(excludeNotSelectable: Boolean): List<String> =
        routeAsync(
            { queryProxyGroupNamesAsync(excludeNotSelectable) },
            { queryProxyGroupNames(excludeNotSelectable) },
        )

    override suspend fun queryProxyGroupAsync(name: String, proxySort: ProxySort): ProxyGroup =
        routeAsync({ queryProxyGroupAsync(name, proxySort) }, { queryProxyGroup(name, proxySort) })

    override suspend fun queryConfigurationAsync(): UiConfiguration =
        routeAsync({ queryConfigurationAsync() }, { queryConfiguration() })

    override suspend fun queryProvidersAsync(): ProviderList =
        routeAsync({ queryProvidersAsync() }, { queryProviders() })

    override suspend fun queryRulesAsync(): List<RuntimeRule> =
        routeAsync({ queryRulesAsync() }, { queryRules() })

    override suspend fun patchSelectorAsync(group: String, name: String): Boolean =
        routeAsync({ patchSelectorAsync(group, name) }, { patchSelector(group, name) })

    override suspend fun closeConnectionAsync(id: String): Boolean =
        routeAsync({ closeConnectionAsync(id) }, { closeConnection(id) })

    override suspend fun closeAllConnectionsAsync() =
        routeAsync({ closeAllConnectionsAsync() }, { closeAllConnections() })

    override fun queryTunnelState(): TunnelState = pick().queryTunnelState()

    override fun queryTrafficNow(): Long = pick().queryTrafficNow()

    override fun queryTrafficTotal(): Long = pick().queryTrafficTotal()

    override fun queryConnections(): ConnectionSnapshot = pick().queryConnections()

    override fun queryAllProxyGroups(excludeNotSelectable: Boolean): List<ProxyGroup> =
        pick().queryAllProxyGroups(excludeNotSelectable)

    override fun queryProxyGroupNames(excludeNotSelectable: Boolean): List<String> =
        pick().queryProxyGroupNames(excludeNotSelectable)

    override fun queryProxyGroup(name: String, proxySort: ProxySort): ProxyGroup =
        pick().queryProxyGroup(name, proxySort)

    override fun queryConfiguration(): UiConfiguration = pick().queryConfiguration()

    override fun queryProviders(): ProviderList = pick().queryProviders()

    override fun queryRules(): List<RuntimeRule> = pick().queryRules()

    override suspend fun setRuleDisabled(rule: RuntimeRule, disabled: Boolean): List<RuntimeRule> {
        val target = pick()
        return target.setRuleDisabled(rule, disabled)
    }

    override fun patchSelector(group: String, name: String): Boolean =
        pick().patchSelector(group, name)

    override fun closeConnection(id: String): Boolean = pick().closeConnection(id)

    override fun closeAllConnections() = pick().closeAllConnections()

    override suspend fun healthCheck(group: String): Map<String, Int> = pick().healthCheck(group)

    override suspend fun healthCheckProxy(group: String, proxyName: String): Int =
        pick().healthCheckProxy(group, proxyName)

    override suspend fun updateProvider(type: Provider.Type, name: String) =
        pick().updateProvider(type, name)

    override fun requestStop() = pick().requestStop()

    @Synchronized
    override fun subscribeLogs(observer: LogObserver): LogSubscription {
        clearActiveLogSubscription()
        val token = Any()
        activeLogToken = token

        var target = pick()
        routedLogSubscription = target.subscribeLogs(observer)
        logRoutingJob = logScope.launch {
            while (isActive) {
                delay(LOG_ROUTE_POLL_MS)
                synchronized(this@CoreRouter) {
                    if (activeLogToken !== token) return@launch
                    val nextTarget = pick()
                    if (nextTarget !== target) {
                        runCatching { nextTarget.subscribeLogs(observer) }
                            .onSuccess { nextSubscription ->
                                routedLogSubscription?.close()
                                routedLogSubscription = nextSubscription
                                target = nextTarget
                            }
                    }
                }
            }
        }
        return LogSubscription {
            synchronized(this@CoreRouter) {
                if (activeLogToken === token) clearActiveLogSubscription()
            }
        }
    }

    private fun clearActiveLogSubscription() {
        activeLogToken = null
        logRoutingJob?.cancel()
        logRoutingJob = null
        routedLogSubscription?.close()
        routedLogSubscription = null
    }

    private companion object {
        const val LOG_ROUTE_POLL_MS = 500L
    }
}
