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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

const val PROXY_DELAY_TEST_PARALLELISM = 5

typealias ProxyDelayTestProgressCallback = (completed: Int, total: Int) -> Unit

/**
 * Returns distinct leaf proxies reachable from [groupName], in group declaration order.
 * Nested groups and cycles are handled without issuing duplicate probes.
 */
fun resolveTestableProxyNames(
    groupName: String,
    proxiesByGroup: Map<String, List<Proxy>>,
): List<String> {
    val resolved = linkedSetOf<String>()
    val visitingGroups = mutableSetOf<String>()

    fun visitGroup(name: String) {
        val proxies = proxiesByGroup[name] ?: return
        if (!visitingGroups.add(name)) return
        proxies.forEach { proxy ->
            val nestedGroup = proxiesByGroup[proxy.name]
            if (proxy.isGroup || nestedGroup != null) {
                nestedGroup?.let { visitGroup(proxy.name) }
            } else if (proxy.name.isNotBlank()) {
                resolved += proxy.name
            }
        }
        visitingGroups.remove(name)
    }

    visitGroup(groupName)
    return resolved.toList()
}

fun countPlannedProxyDelayTests(
    groups: List<ProxyGroupInfo>,
    proxiesByGroup: Map<String, List<Proxy>>,
): Int {
    val tested = linkedSetOf<String>()
    var count = 0
    for (group in groups) {
        val proxyNames = resolveTestableProxyNames(group.name, proxiesByGroup)
        if (proxyNames.isEmpty()) {
            count += 1
            continue
        }
        for (name in proxyNames) {
            if (tested.add(name)) count++
        }
    }
    return count.coerceAtLeast(1)
}

fun countPlannedProxyDelayTestsForGroup(
    groupName: String,
    proxiesByGroup: Map<String, List<Proxy>>,
    testedProxyNames: Set<String> = emptySet(),
): Int {
    val proxyNames = resolveTestableProxyNames(groupName, proxiesByGroup)
    if (proxyNames.isEmpty()) return 1
    return proxyNames.count { it !in testedProxyNames }.coerceAtLeast(0).let { pending ->
        if (pending == 0) 0 else pending
    }
}

suspend fun <T> withProxyDelayTestProgress(
    total: Int,
    onProgress: ProxyDelayTestProgressCallback?,
    block: suspend (onStep: suspend (Int) -> Unit) -> T,
): T {
    if (total > 0) {
        onProgress?.invoke(0, total)
    }
    val mutex = Mutex()
    var completed = 0
    val onStep: suspend (Int) -> Unit = onStep@{ steps ->
        if (steps <= 0 || total <= 0) return@onStep
        mutex.withLock {
            completed = (completed + steps).coerceAtMost(total)
            onProgress?.invoke(completed, total)
        }
    }
    return block(onStep)
}

suspend fun runParallelProxyDelayTests(
    proxyNames: List<String>,
    testedProxyNames: MutableSet<String>,
    isActive: () -> Boolean,
    measure: suspend (proxyName: String) -> Int,
    publish: suspend (proxyName: String, delay: Int) -> Boolean,
    onStep: suspend (Int) -> Unit = {},
): Boolean {
    val pending =
        proxyNames.mapNotNull { name ->
            if (!testedProxyNames.add(name)) null else name
        }
    if (pending.isEmpty()) return isActive()
    coroutineScope {
        val limiter = Semaphore(PROXY_DELAY_TEST_PARALLELISM)
        pending
            .map { proxyName ->
                async {
                    if (!isActive()) return@async false
                    limiter.withPermit {
                        if (!isActive()) return@withPermit false
                        val delay = measure(proxyName)
                        val published = publish(proxyName, delay)
                        if (published) {
                            onStep(1)
                        }
                        published
                    }
                }
            }
            .awaitAll()
    }
    return isActive()
}
