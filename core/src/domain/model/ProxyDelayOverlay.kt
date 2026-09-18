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
import kotlin.jvm.JvmName

@JvmName("proxyGroupInfoMembersByGroup")
fun List<ProxyGroupInfo>.proxyMembersByGroup(): Map<String, Set<String>> =
    associate { group -> group.name to group.proxies.mapTo(HashSet(), Proxy::name) }

@JvmName("proxyGroupMembersByGroup")
fun List<ProxyGroup>.proxyMembersByGroup(): Map<String, Set<String>> =
    associate { group -> group.name to group.proxies.mapTo(HashSet(), Proxy::name) }

/**
 * Keeps direct delay-test responses authoritative until the aggregate controller snapshot catches
 * up. The overlay is shared by active and preview runtime state so both obey the same rule.
 */
class ProxyDelayOverlay(
    private val nowNanos: () -> Long = System::nanoTime,
    private val graceNanos: Long = DEFAULT_GRACE_NANOS,
) {
    private data class Key(val groupName: String, val proxyName: String)

    private data class DirectDelay(
        val delay: Int,
        val expiresAtNanos: Long,
    )

    private val directDelays = ConcurrentHashMap<Key, DirectDelay>()

    /**
     * Records direct results for every displayed group that contains the measured proxy.
     * A zero is not a valid direct measurement and must not displace a prior result.
     */
    fun record(
        measuredDelays: Map<String, Int>,
        membersByGroup: Map<String, Set<String>>,
    ) {
        val nonZeroDelays = measuredDelays.filterValues { delay -> delay != 0 }
        if (nonZeroDelays.isEmpty()) return

        val expiresAtNanos = nowNanos() + graceNanos
        membersByGroup.forEach { (groupName, proxyNames) ->
            proxyNames.forEach { proxyName ->
                nonZeroDelays[proxyName]?.let { delay ->
                    directDelays[Key(groupName, proxyName)] = DirectDelay(delay, expiresAtNanos)
                }
            }
        }
    }

    /**
     * Merges one aggregate value with a direct measurement, if it is still authoritative.
     * An aggregate value equal to the direct response confirms the controller has caught up.
     */
    fun merge(
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

    /** Drops expired results and entries for proxies no longer present in refreshed groups. */
    fun prune(membersByGroup: Map<String, Set<String>>) {
        val now = nowNanos()
        directDelays.forEach { (key, directDelay) ->
            if (
                directDelay.expiresAtNanos <= now ||
                    membersByGroup[key.groupName]?.let { key.proxyName !in it } == true
            ) {
                directDelays.remove(key, directDelay)
            }
        }
    }

    fun clear() {
        directDelays.clear()
    }

    private companion object {
        const val DEFAULT_GRACE_NANOS = 10_000_000_000L
    }
}
