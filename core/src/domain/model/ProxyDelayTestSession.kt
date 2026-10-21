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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicInteger

const val PROXY_DELAY_TEST_PARALLELISM = 5

typealias ProxyDelayTestProgressCallback = (completed: Int, total: Int) -> Unit

/** One measurable unit of a batch run: a leaf node, or a group that exposes no testable leaf. */
sealed interface ProxyDelayTarget {
    data class Node(val name: String) : ProxyDelayTarget

    data class Group(val name: String) : ProxyDelayTarget

    val key: String
        get() =
            when (this) {
                is Node -> "node:$name"
                is Group -> "group:$name"
            }
}

/**
 * Flattens [groupNames] into one probe plan: every reachable leaf is probed once, no matter how
 * many requested groups contain it. A group with no reachable leaf falls back to a group probe.
 */
fun planProxyDelayTests(
    groupNames: List<String>,
    proxiesByGroup: Map<String, List<Proxy>>,
): List<ProxyDelayTarget> {
    if (groupNames.isEmpty()) return emptyList()
    val targets = LinkedHashMap<String, ProxyDelayTarget>()
    for (groupName in groupNames) {
        val leaves = resolveLeafProxyNames(groupName, proxiesByGroup)
        if (leaves.isEmpty()) {
            val target = ProxyDelayTarget.Group(groupName)
            targets.putIfAbsent(target.key, target)
        } else {
            leaves.forEach { leaf ->
                val target = ProxyDelayTarget.Node(leaf)
                targets.putIfAbsent(target.key, target)
            }
        }
    }
    return targets.values.toList()
}

private fun resolveLeafProxyNames(
    groupName: String,
    proxiesByGroup: Map<String, List<Proxy>>,
): List<String> {
    val resolved = LinkedHashSet<String>()
    val visitedGroups = HashSet<String>()

    fun visit(name: String) {
        val proxies = proxiesByGroup[name] ?: return
        if (!visitedGroups.add(name)) return
        proxies.forEach { proxy ->
            val nested = proxiesByGroup[proxy.name]
            when {
                nested != null -> visit(proxy.name)
                proxy.isGroup -> Unit
                proxy.name.isNotBlank() -> resolved += proxy.name
            }
        }
    }

    visit(groupName)
    return resolved.toList()
}

/**
 * One batch delay test.
 *
 * [run] probes the plan with bounded parallelism, hands every measurement to [onResult] as soon as
 * it lands, and returns the complete result set once the run finishes. Callers reconcile their
 * group snapshot from that return value, so the final state never depends on how long per-node
 * values were cached. Returns null when the run was abandoned (inactive generation, empty plan).
 */
class ProxyDelayTestSession(
    targets: List<ProxyDelayTarget>,
    private val isActive: () -> Boolean,
    private val onResult: (Map<String, Int>) -> Unit,
    private val measureNode: suspend (String) -> Int,
    private val measureGroup: suspend (String) -> Map<String, Int>,
    private val onProgress: ProxyDelayTestProgressCallback? = null,
    private val parallelism: Int = PROXY_DELAY_TEST_PARALLELISM,
) {
    private val targets = targets.distinctBy(ProxyDelayTarget::key)
    private val results = LinkedHashMap<String, Int>()
    private val resultsMutex = Mutex()
    private val completed = AtomicInteger()

    suspend fun run(): Map<String, Int>? {
        if (targets.isEmpty() || !isActive()) return null
        onProgress?.invoke(0, targets.size)
        coroutineScope {
            val limiter = Semaphore(parallelism)
            targets
                .map { target ->
                    async {
                        if (!isActive()) return@async
                        limiter.withPermit {
                            if (!isActive()) return@withPermit
                            val batch = measure(target)
                            if (batch.isNotEmpty()) {
                                resultsMutex.withLock { results.putAll(batch) }
                                onResult(batch)
                            }
                            // Failed probes still finish a unit; only successful ones carry a delay.
                            onProgress?.invoke(completed.incrementAndGet(), targets.size)
                        }
                    }
                }
                .awaitAll()
        }
        return if (isActive()) resultsMutex.withLock { LinkedHashMap(results) } else null
    }

    private suspend fun measure(target: ProxyDelayTarget): Map<String, Int> =
        when (target) {
            is ProxyDelayTarget.Node -> {
                val delay = measureSafely { measureNode(target.name) } ?: 0
                if (delay == 0) emptyMap() else mapOf(target.name to delay)
            }

            is ProxyDelayTarget.Group ->
                measureSafely { measureGroup(target.name) }
                    .orEmpty()
                    .filterValues { delay -> delay != 0 }
        }

    private suspend fun <T> measureSafely(block: suspend () -> T): T? =
        try {
            block()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // One unreachable probe must not abort the rest of the batch.
            null
        }
}
