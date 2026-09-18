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
package com.github.yumeyucca.yumebox.domain.model

import com.github.yumeyucca.yumebox.core.model.Proxy
import com.github.yumeyucca.yumebox.core.model.ProxyGroup
import java.util.concurrent.ConcurrentHashMap

fun membersByGroup(entries: Iterable<Pair<String, List<Proxy>>>): Map<String, Set<String>> =
    entries.associate { (name, proxies) -> name to proxies.mapTo(HashSet(), Proxy::name) }

/**
 * Keeps direct delay and selector writes authoritative until the aggregate controller snapshot
 * catches up, or a short grace window expires.
 */
class ProxyGroupOverlay(
    private val nowNanos: () -> Long = System::nanoTime,
    private val graceNanos: Long = DEFAULT_GRACE_NANOS,
) {
    private data class Key(val groupName: String, val proxyName: String)

    private data class DirectDelay(
        val delay: Int,
        val expiresAtNanos: Long,
    )

    private data class DirectNow(
        val proxyName: String,
        val expiresAtNanos: Long,
    )

    private val directDelays = ConcurrentHashMap<Key, DirectDelay>()
    private val directNow = ConcurrentHashMap<String, DirectNow>()

    /**
     * Records direct results for every displayed group that contains the measured proxy.
     * A zero is not a valid direct measurement and must not displace a prior result.
     */
    fun recordDelays(
        measuredDelays: Map<String, Int>,
        members: Map<String, Set<String>>,
    ) {
        val nonZeroDelays = measuredDelays.filterValues { delay -> delay != 0 }
        if (nonZeroDelays.isEmpty()) return

        val expiresAtNanos = nowNanos() + graceNanos
        members.forEach { (groupName, proxyNames) ->
            proxyNames.forEach { proxyName ->
                nonZeroDelays[proxyName]?.let { delay ->
                    directDelays[Key(groupName, proxyName)] = DirectDelay(delay, expiresAtNanos)
                }
            }
        }
    }

    fun recordNow(groupName: String, proxyName: String) {
        val name = proxyName.trim()
        if (name.isEmpty()) return
        directNow[groupName] = DirectNow(name, nowNanos() + graceNanos)
    }

    fun clearNow(groupName: String, expected: String? = null) {
        directNow.compute(groupName) { _, current ->
            when {
                current == null -> null
                expected == null || current.proxyName == expected -> null
                else -> current
            }
        }
    }

    /**
     * Merges one aggregate delay with a direct measurement, if it is still authoritative.
     * An aggregate value equal to the direct response confirms the controller has caught up.
     */
    fun mergeDelay(
        groupName: String,
        proxyName: String,
        reportedDelay: Int,
        previousDelay: Int?,
    ): Int {
        val key = Key(groupName, proxyName)
        val directDelay = directDelays[key]
        if (directDelay != null) {
            if (reportedDelay != directDelay.delay) return directDelay.delay
            directDelays.remove(key, directDelay)
        }
        return if (reportedDelay == 0 && previousDelay != null && previousDelay != 0) {
            previousDelay
        } else {
            reportedDelay
        }
    }

    fun mergeNow(groupName: String, reportedNow: String): String {
        val pending = directNow[groupName] ?: return reportedNow
        if (pending.expiresAtNanos <= nowNanos()) {
            directNow.remove(groupName, pending)
            return reportedNow
        }
        if (reportedNow == pending.proxyName) {
            directNow.remove(groupName, pending)
            return reportedNow
        }
        return pending.proxyName
    }

    /** Overlay wins without confirming, so a local write cannot drop protection against a stale snapshot. */
    fun paint(groups: List<ProxyGroupInfo>): List<ProxyGroupInfo> {
        prune(membersByGroup(groups.map { group -> group.name to group.proxies }))
        if (directDelays.isEmpty() && directNow.isEmpty()) return groups
        return groups.map { group ->
            group.copy(
                now = paintedNow(group.name, group.now),
                proxies =
                    group.proxies.map { proxy ->
                        paintedDelay(group.name, proxy.name)?.let { delay -> proxy.copy(delay = delay) }
                            ?: proxy
                    },
            )
        }
    }

    fun paintProxyGroups(groups: List<ProxyGroup>): List<ProxyGroup> {
        prune(membersByGroup(groups.map { group -> group.name to group.proxies }))
        if (directDelays.isEmpty()) return groups
        return groups.map { group ->
            group.copy(
                proxies =
                    group.proxies.map { proxy ->
                        paintedDelay(group.name, proxy.name)?.let { delay -> proxy.copy(delay = delay) }
                            ?: proxy
                    },
            )
        }
    }

    /** Drops expired results and entries for proxies no longer present in refreshed groups. */
    fun prune(members: Map<String, Set<String>>) {
        val now = nowNanos()
        directDelays.forEach { (key, directDelay) ->
            if (
                directDelay.expiresAtNanos <= now ||
                    members[key.groupName]?.let { key.proxyName !in it } == true
            ) {
                directDelays.remove(key, directDelay)
            }
        }
        directNow.forEach { (groupName, pending) ->
            if (pending.expiresAtNanos <= now || members[groupName] == null) {
                directNow.remove(groupName, pending)
            }
        }
    }

    fun clear() {
        directDelays.clear()
        directNow.clear()
    }

    private fun paintedDelay(groupName: String, proxyName: String): Int? {
        val directDelay = directDelays[Key(groupName, proxyName)] ?: return null
        if (directDelay.expiresAtNanos <= nowNanos()) {
            directDelays.remove(Key(groupName, proxyName), directDelay)
            return null
        }
        return directDelay.delay
    }

    private fun paintedNow(groupName: String, reportedNow: String): String {
        val pending = directNow[groupName] ?: return reportedNow
        if (pending.expiresAtNanos <= nowNanos()) {
            directNow.remove(groupName, pending)
            return reportedNow
        }
        return pending.proxyName
    }

    private companion object {
        const val DEFAULT_GRACE_NANOS = 10_000_000_000L
    }
}
