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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Live delay values keyed by proxy name. Node rows observe only their own flow, so one result
 * updates one badge instead of replacing the proxy-group list.
 *
 * Zero means "not measured" and is published as null, which lets the UI fall back to the delay
 * carried by the group snapshot. Flows are never dropped: a composable that resolved a node keeps
 * a live flow for as long as it is composed.
 */
class ProxyDelayStore {
    private val delays = ConcurrentHashMap<String, MutableStateFlow<Int?>>()

    fun delayFlow(name: String): StateFlow<Int?> =
        delays.computeIfAbsent(name) { MutableStateFlow(null) }

    fun apply(measured: Map<String, Int>) {
        measured.forEach { (name, delay) -> set(name, delay) }
    }

    fun publish(groups: List<ProxyGroupInfo>) {
        groups.forEach { group ->
            group.proxies.forEach { proxy -> set(proxy.name, proxy.delay) }
        }
    }

    fun publishProxyGroups(groups: List<ProxyGroup>) {
        groups.forEach { group ->
            group.proxies.forEach { proxy -> set(proxy.name, proxy.delay) }
        }
    }

    fun clear() {
        delays.values.forEach { flow -> flow.value = null }
    }

    private fun set(name: String, delay: Int) {
        if (name.isBlank()) return
        delays.computeIfAbsent(name) { MutableStateFlow(null) }.value = delay.takeIf { it != 0 }
    }
}

/** Overlays measured delays on one group's members, leaving untouched members as-is. */
fun List<Proxy>.withDelays(delays: Map<String, Int>): List<Proxy> =
    if (delays.isEmpty()) {
        this
    } else {
        map { proxy -> delays[proxy.name]?.let { delay -> proxy.copy(delay = delay) } ?: proxy }
    }
