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

@file:Suppress("UnusedSymbol")

package com.github.yumeyucca.yumebox.domain.model


import com.github.yumeyucca.yumebox.core.model.Proxy
import com.github.yumeyucca.yumebox.core.model.ProxyGroup
import com.github.yumeyucca.yumebox.core.model.isManuallySelectable
import com.github.yumeyucca.yumebox.core.model.isProxyGroup
import kotlinx.serialization.Serializable

@Serializable
data class ProxyGroupInfo(
    val name: String,
    val type: String,
    val proxies: List<Proxy>,
    val now: String,
    val icon: String? = null,
    val hidden: Boolean = false,
)

val ProxyGroupInfo.isSelectable: Boolean
    get() = type.isManuallySelectable

val ProxyGroupInfo.isProxyGroup: Boolean
    get() = type in Proxy.Type.groupTypes || now.isNotBlank() || proxies.isNotEmpty()

/** The group name a stock config uses for its main group; every other config falls back to its first. */
private const val PRIMARY_GROUP_NAME = "Proxy"

fun ProxyGroup.toInfo(): ProxyGroupInfo =
    ProxyGroupInfo(
        name = name,
        type = type,
        proxies = proxies,
        now = now.trim(),
        icon = icon,
        hidden = hidden,
    )

/**
 * Resolves the terminal proxy of the group the runtime is dialing: the main group
 * ([PRIMARY_GROUP_NAME]) when there is one, otherwise the first group.
 */
fun List<ProxyGroupInfo>.resolvePrimaryNode(): Proxy? {
    val group =
        firstOrNull { it.name.equals(PRIMARY_GROUP_NAME, ignoreCase = true) }
            ?: firstOrNull()
            ?: return null
    val selected = group.now.trim()
    return if (selected.isEmpty()) null else resolveTerminalProxy(selected)
}

/**
 * Delay shown beside the selected node on a group card.
 *
 * The label names that node, so the number has to be its measurement. Leaf copies can disagree
 * after a partial publish; the freshest non-zero delay wins. An empty selection stays unknown.
 */
fun List<ProxyGroupInfo>.displayedSelectionDelay(group: ProxyGroupInfo): Int =
    GroupDelayIndex(this).selectedLeafDelay(group.name)

/**
 * Copies group members so each nested group row carries the delay a direct tap would show.
 * Leaf rows stay as they are.
 */
fun ProxyGroupInfo.resolveMemberDelays(allGroups: List<ProxyGroupInfo>): ProxyGroupInfo {
    if (proxies.isEmpty()) return this
    val index = GroupDelayIndex(allGroups)
    var changed = false
    val resolved =
        proxies.map { proxy ->
            val delay = index.displayedMemberDelay(proxy)
            if (delay == proxy.delay) {
                proxy
            } else {
                changed = true
                proxy.copy(delay = delay)
            }
        }
    return if (changed) copy(proxies = resolved) else this
}

/**
 * Resolves what a nested group row should display.
 *
 * A group test measures leaves and does not write a delay onto the group row itself. Prefer this
 * row's own positive delay (a direct probe of its name), then the current selection walked to a
 * leaf, then the fastest measured leaf when that selection is empty or still unmeasured. Timeout
 * is shown only when the row, its selection, and every measured leaf failed.
 */
private class GroupDelayIndex(private val groups: List<ProxyGroupInfo>) {
    private val exactGroups: Map<String, ProxyGroupInfo>
    private val foldedGroups: Map<String, ProxyGroupInfo>
    private val groupNames: Set<String>
    private val delayByName: Map<String, Int>

    init {
        val exact = HashMap<String, ProxyGroupInfo>(groups.size)
        val folded = HashMap<String, ProxyGroupInfo>(groups.size)
        val names = HashSet<String>(groups.size * 2)
        val delays = HashMap<String, Int>()
        for (group in groups) {
            exact[group.name] = group
            folded[group.name.lowercase()] = group
            names.add(group.name)
            names.add(group.name.lowercase())
            for (proxy in group.proxies) {
                delays[proxy.name] = preferDelay(delays[proxy.name], proxy.delay)
            }
        }
        exactGroups = exact
        foldedGroups = folded
        groupNames = names
        delayByName = delays
    }

    fun displayedMemberDelay(proxy: Proxy): Int {
        if (!proxy.isProxyGroup && !isGroupName(proxy.name)) return proxy.delay
        if (proxy.delay > 0) return proxy.delay
        val selected = selectedLeafDelay(proxy.name)
        if (selected > 0) return selected
        val measured = measuredLeafDelay(proxy.name)
        if (measured != null && measured > 0) return measured
        if (proxy.delay < 0 || selected < 0 || (measured != null && measured < 0)) return -1
        return proxy.delay
    }

    fun selectedLeafDelay(groupName: String): Int {
        val selected = findGroup(groupName)?.now?.trim().orEmpty()
        if (selected.isEmpty()) return 0
        val terminal = groups.resolveTerminalProxy(selected) ?: return 0
        return freshest(terminal.name)
    }

    private fun isGroupName(name: String): Boolean =
        name in groupNames || name.lowercase() in groupNames

    private fun freshest(name: String): Int {
        delayByName[name]?.let { return it }
        return delayByName.entries.firstOrNull { (key, _) -> key.equals(name, ignoreCase = true) }?.value
            ?: 0
    }

    private fun findGroup(name: String): ProxyGroupInfo? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return null
        return exactGroups[trimmed] ?: foldedGroups[trimmed.lowercase()]
    }

    /** Fastest positive leaf under [groupName], or -1 when every measured leaf timed out. */
    private fun measuredLeafDelay(groupName: String): Int? {
        var bestPositive: Int? = null
        var sawTimeout = false
        fun consider(delay: Int) {
            if (delay > 0) {
                val current = bestPositive
                if (current == null || delay < current) bestPositive = delay
            } else if (delay < 0) {
                sawTimeout = true
            }
        }
        fun visit(name: String, visiting: MutableSet<String>) {
            val normalized = name.trim()
            if (normalized.isEmpty() || !visiting.add(normalized.lowercase())) return
            val group = findGroup(normalized) ?: return
            for (member in group.proxies) {
                val child = findGroup(member.name)
                if ((member.isProxyGroup || child != null) && child != null) {
                    visit(member.name, visiting)
                } else {
                    consider(freshest(member.name))
                }
            }
        }
        visit(groupName, mutableSetOf())
        return bestPositive ?: if (sawTimeout) -1 else null
    }
}

private fun preferDelay(current: Int?, next: Int): Int {
    if (current == null || current == 0) return next
    if (next > 0 && (current < 0 || next < current)) return next
    return current
}

/** Resolves a selected proxy-group entry to the terminal (non-group) proxy. */
fun List<ProxyGroupInfo>.resolveTerminalProxy(entryName: String): Proxy? {
    fun findGroup(name: String): ProxyGroupInfo? =
        firstOrNull { it.name == name } ?: firstOrNull { it.name.equals(name, ignoreCase = true) }

    fun findProxy(name: String): Proxy? =
        asSequence()
            .flatMap { it.proxies.asSequence() }
            .firstOrNull { it.name == name }
            ?: asSequence()
                .flatMap { it.proxies.asSequence() }
                .firstOrNull { it.name.equals(name, ignoreCase = true) }

    fun resolve(name: String, visited: MutableSet<String>): Proxy? {
        val normalized = name.trim()
        if (normalized.isEmpty() || !visited.add(normalized.lowercase())) return null

        findGroup(normalized)?.let { group ->
            return resolve(group.now, visited)
        }

        val proxy = findProxy(normalized) ?: return null
        val nestedGroup = findGroup(proxy.name)
        if (proxy.isProxyGroup || nestedGroup != null) {
            nestedGroup?.let { group ->
                return resolve(group.now, visited) ?: proxy
            }
        }
        return proxy
    }

    return resolve(entryName, linkedSetOf())
}
