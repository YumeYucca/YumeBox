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

package com.github.yumeyucca.yumebox.runtime.client.access

import com.github.yumeyucca.yumebox.core.model.ConnectionSnapshot
import com.github.yumeyucca.yumebox.core.model.Provider
import com.github.yumeyucca.yumebox.core.model.ProviderList
import com.github.yumeyucca.yumebox.core.model.ProxyGroup
import com.github.yumeyucca.yumebox.core.model.ProxySort
import com.github.yumeyucca.yumebox.core.model.RuntimeRule
import com.github.yumeyucca.yumebox.runtime.api.CoreApi
import com.github.yumeyucca.yumebox.runtime.api.LogObserver
import com.github.yumeyucca.yumebox.runtime.api.LogSubscription
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Routes [CoreApi] to remote controller when active, otherwise the local controller. */
class CoreRouter(
    private val local: CoreApi,
    private val remote: CoreApi,
    private val isRemoteControllerActive: () -> Boolean,
) : CoreApi {
    private val logScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var logRoutingJob: Job? = null
    private var activeLogToken: Any? = null
    private var routedLogSubscription: LogSubscription? = null

    private fun pick(): CoreApi = if (isRemoteControllerActive()) remote else local

    override suspend fun queryTrafficNow(): Long = pick().queryTrafficNow()

    override suspend fun queryTrafficTotal(): Long = pick().queryTrafficTotal()

    override suspend fun queryConnections(): ConnectionSnapshot = pick().queryConnections()

    override suspend fun queryAllProxyGroups(excludeNotSelectable: Boolean): List<ProxyGroup> =
        pick().queryAllProxyGroups(excludeNotSelectable)

    override suspend fun queryProxyGroup(name: String, proxySort: ProxySort): ProxyGroup =
        pick().queryProxyGroup(name, proxySort)

    override suspend fun queryProviders(): ProviderList = pick().queryProviders()

    override suspend fun queryRules(): List<RuntimeRule> = pick().queryRules()

    override suspend fun setRuleDisabled(rule: RuntimeRule, disabled: Boolean): List<RuntimeRule> =
        pick().setRuleDisabled(rule, disabled)

    override suspend fun patchSelector(group: String, name: String): Boolean =
        pick().patchSelector(group, name)

    override suspend fun closeConnection(id: String): Boolean = pick().closeConnection(id)

    override suspend fun healthCheck(group: String): Map<String, Int> = pick().healthCheck(group)

    override suspend fun healthCheckProxy(group: String, proxyName: String): Int =
        pick().healthCheckProxy(group, proxyName)

    override suspend fun updateProvider(type: Provider.Type, name: String) =
        pick().updateProvider(type, name)

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
