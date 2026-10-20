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

import com.github.yumeyucca.yumebox.core.model.ProxyGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Live delay values keyed by proxy name. Node rows observe only their own flow, so a batch of
 * delay results updates the affected badges without replacing the proxy-group list.
 */
class ProxyDelayStore {
    private val delays = ConcurrentHashMap<String, MutableStateFlow<Int?>>()

    fun delayFlow(name: String): StateFlow<Int?> =
        delays.computeIfAbsent(name) { MutableStateFlow(null) }

    fun apply(measured: Map<String, Int>) {
        measured.forEach { (name, delay) -> set(name, delay) }
    }

    fun publish(groups: List<ProxyGroupInfo>) {
        publishNames(
            groups.asSequence()
                .flatMap { group -> group.proxies.asSequence() }
                .map { proxy -> proxy.name to proxy.delay }
        )
    }

    fun publishProxyGroups(groups: List<ProxyGroup>) {
        publishNames(
            groups.asSequence()
                .flatMap { group -> group.proxies.asSequence() }
                .map { proxy -> proxy.name to proxy.delay }
        )
    }

    fun clear() {
        delays.values.forEach { flow -> flow.value = null }
        delays.clear()
    }

    private fun publishNames(entries: Sequence<Pair<String, Int>>) {
        val seen = HashSet<String>()
        entries.forEach { (name, delay) ->
            if (name.isBlank()) return@forEach
            seen += name
            set(name, delay)
        }
        delays.keys.removeAll { name -> name !in seen }
    }

    private fun set(name: String, delay: Int) {
        if (name.isBlank()) return
        delays.computeIfAbsent(name) { MutableStateFlow(null) }.value = delay
    }
}