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
 * Copyright (c) YumeYucca 2025 - Present
 *
 */

package com.github.yumeyucca.yumebox.runtime.client.session

import com.github.yumeyucca.yumebox.core.model.Proxy
import com.github.yumeyucca.yumebox.core.model.ProxyGroup
import com.github.yumeyucca.yumebox.domain.model.ProxyDelayStore
import com.github.yumeyucca.yumebox.domain.model.ProxyGroupInfo
import com.github.yumeyucca.yumebox.domain.model.ProxyGroupOverlay
import com.github.yumeyucca.yumebox.domain.model.membersByGroup
import com.github.yumeyucca.yumebox.domain.model.resolvePrimaryNode
import com.github.yumeyucca.yumebox.domain.model.toInfo
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
    private val delayStore: ProxyDelayStore = ProxyDelayStore(),
) {
    private val _groups = MutableStateFlow<List<ProxyGroupInfo>>(emptyList())
    val groups: StateFlow<List<ProxyGroupInfo>> = _groups.asStateFlow()

    private val _resolvedPrimaryNode = MutableStateFlow<Proxy?>(null)
    val resolvedPrimaryNode: StateFlow<Proxy?> = _resolvedPrimaryNode.asStateFlow()

    private var lastSummary: String? = null
    private val overlay = ProxyGroupOverlay(nowNanos, directDelayGraceNanos)
    private var sourceGroups: List<ProxyGroupInfo> = emptyList()
    // Reverse index: proxy name → indices of groups that contain it, rebuilt on every publish.
    private var proxyToGroupIndex: Map<String, List<Int>> = emptyMap()

    fun toInfo(group: ProxyGroup): ProxyGroupInfo = group.toInfo()

    @Synchronized
    fun publish(groups: List<ProxyGroupInfo>) {
        sourceGroups = groups
        proxyToGroupIndex = buildProxyToGroupIndex(groups)
        val resolved = overlay.paint(groups)
        val summary = summarize(resolved)
        if (summary != lastSummary) {
            _groups.value = resolved
            lastSummary = summary
        }
        delayStore.publish(resolved)
        onGroupsReady(resolved.isNotEmpty())
        updateResolvedPrimaryNode(resolved)
    }

    @Synchronized
    fun recordSelectedNow(groupName: String, proxyName: String) {
        overlay.recordNow(groupName, proxyName)
        publish(sourceGroups.ifEmpty { _groups.value })
    }

    @Synchronized
    fun clearSelectedNow(groupName: String, expected: String? = null) {
        overlay.clearNow(groupName, expected)
        publish(sourceGroups.ifEmpty { _groups.value })
    }

    @Synchronized
    fun upsert(updated: ProxyGroupInfo): List<ProxyGroupInfo> {
        val currentGroups = _groups.value
        if (currentGroups.isEmpty()) return listOf(updated)
        if (currentGroups.none { it.name == updated.name }) {
            return currentGroups + updated
        }
        return currentGroups.map { group -> if (group.name == updated.name) updated else group }
    }

    /**
     * Publishes a batch of direct measurements to the per-node delay flows. The immutable group
     * list stays untouched until [commitDirectDelays] runs once at the end of the test.
     */
    @Synchronized
    fun applyDirectDelays(delays: Map<String, Int>) {
        val measuredDelays = delays.filterValues { delay -> delay != 0 }
        if (measuredDelays.isEmpty()) return
        val currentGroups = _groups.value
        if (currentGroups.isEmpty()) return
        val index =
            proxyToGroupIndex.ifEmpty {
                buildProxyToGroupIndex(currentGroups).also { proxyToGroupIndex = it }
            }
        val affectedMembers = HashMap<String, MutableSet<String>>()
        measuredDelays.keys.forEach { name ->
            index[name]?.forEach { idx ->
                affectedMembers.getOrPut(currentGroups[idx].name) { mutableSetOf() } += name
            }
        }
        if (affectedMembers.isEmpty()) return
        overlay.recordDelays(measuredDelays, affectedMembers)
        delayStore.apply(measuredDelays)
    }

    /** Reconciles the immutable group list once, after a test session has finished. */
    @Synchronized
    fun commitDirectDelays() {
        publish(sourceGroups.ifEmpty { _groups.value })
    }

    fun proxyDelayFlow(name: String): StateFlow<Int?> = delayStore.delayFlow(name)

    private fun buildProxyToGroupIndex(groups: List<ProxyGroupInfo>): Map<String, List<Int>> {
        val index = HashMap<String, MutableList<Int>>()
        groups.forEachIndexed { i, group ->
            group.proxies.forEach { proxy ->
                index.getOrPut(proxy.name) { mutableListOf() } += i
            }
        }
        return index
    }

    /**
     * The controller's aggregate proxy snapshot can lag behind a direct `/delay` or selector write.
     * Keep the direct result until an equal snapshot arrives or a short grace window expires.
     */
    @Synchronized
    fun mergeReportedDelays(incoming: List<ProxyGroupInfo>): List<ProxyGroupInfo> {
        overlay.prune(membersByGroup(incoming.map { group -> group.name to group.proxies }))
        val previousByGroup = _groups.value.associateBy(ProxyGroupInfo::name)
        return incoming.map { group ->
            val previousByProxy = previousByGroup[group.name]?.proxies?.associateBy(Proxy::name)
            group.copy(
                now = overlay.mergeNow(group.name, group.now.trim()),
                proxies =
                    group.proxies.map { proxy ->
                        val previousDelay = previousByProxy?.get(proxy.name)?.delay
                        proxy.copy(
                            delay =
                                overlay.mergeDelay(
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

    @Synchronized
    fun clear(resetGroups: Boolean) {
        overlay.clear()
        delayStore.clear()
        sourceGroups = emptyList()
        if (resetGroups) {
            _groups.value = emptyList()
            lastSummary = null
        }
        _resolvedPrimaryNode.value = null
    }

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
        _resolvedPrimaryNode.value = groups.takeIf { isRuntimeRunning() }?.resolvePrimaryNode()
    }
}
