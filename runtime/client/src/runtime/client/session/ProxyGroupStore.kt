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

import com.github.yumeyucca.yumebox.core.model.Proxy
import com.github.yumeyucca.yumebox.core.model.ProxyGroup
import com.github.yumeyucca.yumebox.domain.model.ProxyDelayOverlay
import com.github.yumeyucca.yumebox.domain.model.ProxyGroupInfo
import com.github.yumeyucca.yumebox.domain.model.resolveTerminalProxy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Proxy-group view state extracted from `ProxyFacade`: caches the last published group list,
 * dedupes republishes by content summary, and resolves the primary end node by walking the
 * selection chain. Publishing also reports readiness back so the runtime snapshot can track it.
 */
internal class ProxyGroupStore(
    private val isRuntimeRunning: () -> Boolean,
    private val onGroupsReady: (Boolean) -> Unit,
    private val nowNanos: () -> Long = System::nanoTime,
    private val directDelayGraceNanos: Long = 10_000_000_000L,
) {
    private val _groups = MutableStateFlow<List<ProxyGroupInfo>>(emptyList())
    val groups: StateFlow<List<ProxyGroupInfo>> = _groups.asStateFlow()

    private val _resolvedPrimaryNode = MutableStateFlow<Proxy?>(null)
    val resolvedPrimaryNode: StateFlow<Proxy?> = _resolvedPrimaryNode.asStateFlow()

    private var lastSummary: String? = null
    private val delayOverlay = ProxyDelayOverlay(nowNanos, directDelayGraceNanos)

    fun toInfo(group: ProxyGroup): ProxyGroupInfo =
        ProxyGroupInfo(
            name = group.name,
            type = group.type,
            proxies = group.proxies,
            now = group.now.trim(),
            icon = group.icon,
            hidden = group.hidden,
        )

    fun publish(groups: List<ProxyGroupInfo>) {
        val summary = summarize(groups)
        if (summary != lastSummary) {
            _groups.value = groups
            lastSummary = summary
        }
        onGroupsReady(groups.isNotEmpty())
        updateResolvedPrimaryNode(groups)
    }

    fun upsert(updated: ProxyGroupInfo): List<ProxyGroupInfo> {
        val currentGroups = _groups.value
        if (currentGroups.isEmpty()) return listOf(updated)
        if (currentGroups.none { it.name == updated.name }) {
            return currentGroups + updated
        }
        return currentGroups.map { group -> if (group.name == updated.name) updated else group }
    }

    fun recordDirectDelaysEverywhere(delays: Map<String, Int>): List<ProxyGroupInfo> {
        val measuredDelays = delays.filterValues { delay -> delay != 0 }
        if (measuredDelays.isEmpty()) return _groups.value
        val currentGroups = _groups.value
        val updated =
            currentGroups.map { group ->
                group.copy(
                    proxies =
                        group.proxies.map { proxy ->
                            measuredDelays[proxy.name]?.let { delay -> proxy.copy(delay = delay) }
                                ?: proxy
                        }
                )
            }
        delayOverlay.record(measuredDelays, membersByGroup(updated))
        return updated
    }

    /**
     * The controller's aggregate proxy snapshot can lag behind a direct `/delay` response.
     * Keep the direct result until an equal snapshot arrives or a short grace window expires.
     */
    fun mergeReportedDelays(incoming: List<ProxyGroupInfo>): List<ProxyGroupInfo> {
        delayOverlay.prune(membersByGroup(incoming))
        val previousByGroup = _groups.value.associateBy(ProxyGroupInfo::name)
        return incoming.map { group ->
            val previousByProxy = previousByGroup[group.name]?.proxies?.associateBy(Proxy::name)
            group.copy(
                proxies =
                    group.proxies.map { proxy ->
                        val previousDelay = previousByProxy?.get(proxy.name)?.delay
                        proxy.copy(
                            delay =
                                delayOverlay.merge(
                                    groupName = group.name,
                                    proxyName = proxy.name,
                                    reportedDelay = proxy.delay,
                                    previousDelay = previousDelay,
                                )
                        )
                    }
            )
        }
    }

    fun clear(resetGroups: Boolean) {
        delayOverlay.clear()
        if (resetGroups) {
            _groups.value = emptyList()
            lastSummary = null
        }
        _resolvedPrimaryNode.value = null
    }

    private fun membersByGroup(groups: List<ProxyGroupInfo>): Map<String, Set<String>> =
        groups.associate { group -> group.name to group.proxies.mapTo(HashSet()) { it.name } }

    private fun summarize(groups: List<ProxyGroupInfo>): String =
        groups.joinToString(separator = "\n") { group ->
            buildString {
                append(group.name)
                append('|')
                append(group.type)
                append('|')
                append(group.now)
                append('|')
                append(group.hidden)
                append('|')
                append(group.proxies.size)
                group.proxies.forEach { proxy ->
                    append('|')
                    append(proxy.name)
                    append(':')
                    append(proxy.type)
                    append(':')
                    append(proxy.isGroup)
                    append(':')
                    append(proxy.delay)
                }
            }
        }

    private fun updateResolvedPrimaryNode(groups: List<ProxyGroupInfo>) {
        if (!isRuntimeRunning() || groups.isEmpty()) {
            _resolvedPrimaryNode.value = null
            return
        }
        val mainGroup =
            groups.find { it.name.equals("Proxy", ignoreCase = true) } ?: groups.firstOrNull()
        val targetNode = mainGroup?.now?.trim().orEmpty()
        _resolvedPrimaryNode.value =
            targetNode.takeIf(String::isNotEmpty)?.let { groups.resolveTerminalProxy(it) }
    }
}
