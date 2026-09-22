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
) {
    private val _groups = MutableStateFlow<List<ProxyGroupInfo>>(emptyList())
    val groups: StateFlow<List<ProxyGroupInfo>> = _groups.asStateFlow()

    private val _resolvedPrimaryNode = MutableStateFlow<Proxy?>(null)
    val resolvedPrimaryNode: StateFlow<Proxy?> = _resolvedPrimaryNode.asStateFlow()

    private var lastSummary: String? = null
    private val overlay = ProxyGroupOverlay(nowNanos, directDelayGraceNanos)
    private var sourceGroups: List<ProxyGroupInfo> = emptyList()

    fun toInfo(group: ProxyGroup): ProxyGroupInfo = group.toInfo()

    fun publish(groups: List<ProxyGroupInfo>) {
        sourceGroups = groups
        val resolved = overlay.paint(groups)
        val summary = summarize(resolved)
        if (summary != lastSummary) {
            _groups.value = resolved
            lastSummary = summary
        }
        onGroupsReady(resolved.isNotEmpty())
        updateResolvedPrimaryNode(resolved)
    }

    fun recordSelectedNow(groupName: String, proxyName: String) {
        overlay.recordNow(groupName, proxyName)
        publish(sourceGroups.ifEmpty { _groups.value })
    }

    fun clearSelectedNow(groupName: String, expected: String? = null) {
        overlay.clearNow(groupName, expected)
        publish(sourceGroups.ifEmpty { _groups.value })
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
        if (measuredDelays.isEmpty()) return sourceGroups.ifEmpty { _groups.value }
        val currentGroups = sourceGroups.ifEmpty { _groups.value }
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
        overlay.recordDelays(measuredDelays, membersByGroup(updated.map { it.name to it.proxies }))
        return updated
    }

    /**
     * The controller's aggregate proxy snapshot can lag behind a direct `/delay` or selector write.
     * Keep the direct result until an equal snapshot arrives or a short grace window expires.
     */
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

    fun clear(resetGroups: Boolean) {
        overlay.clear()
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
