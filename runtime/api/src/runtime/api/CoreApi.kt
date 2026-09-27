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

package com.github.yumeyucca.yumebox.runtime.api

import com.github.yumeyucca.yumebox.core.model.ConnectionSnapshot
import com.github.yumeyucca.yumebox.core.model.Provider
import com.github.yumeyucca.yumebox.core.model.ProviderList
import com.github.yumeyucca.yumebox.core.model.ProxyGroup
import com.github.yumeyucca.yumebox.core.model.ProxySort
import com.github.yumeyucca.yumebox.core.model.RuntimeRule

interface CoreApi {
    suspend fun queryTrafficNow(): Long

    suspend fun queryTrafficTotal(): Long

    suspend fun queryConnections(): ConnectionSnapshot

    suspend fun queryAllProxyGroups(excludeNotSelectable: Boolean): List<ProxyGroup>

    suspend fun queryProxyGroup(name: String, proxySort: ProxySort): ProxyGroup

    suspend fun queryProviders(): ProviderList

    /** Live rules from `GET /rules` (runtime rule list, not custom-routing editor). */
    suspend fun queryRules(): List<RuntimeRule>

    /** Temporarily toggle [rule], then return the controller-confirmed runtime rule list. */
    suspend fun setRuleDisabled(rule: RuntimeRule, disabled: Boolean): List<RuntimeRule>

    suspend fun patchSelector(group: String, name: String): Boolean

    suspend fun closeConnection(id: String): Boolean

    /** Tests every member of [group] and returns its directly measured delays by proxy name. */
    suspend fun healthCheck(group: String): Map<String, Int>

    suspend fun healthCheckProxy(group: String, proxyName: String): Int

    suspend fun updateProvider(type: Provider.Type, name: String)

    fun subscribeLogs(observer: LogObserver): LogSubscription
}
