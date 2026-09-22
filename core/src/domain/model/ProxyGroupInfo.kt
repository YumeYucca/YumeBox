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

/** Maps the core proxy-group payload to its presentation model. */
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
 * Resolves the terminal (non-group) proxy of the group the runtime is currently dialing: the main
 * group ([PRIMARY_GROUP_NAME]) when a config has one, otherwise the first group. Returns `null`
 * when there is no group or the selected entry cannot be resolved.
 */
fun List<ProxyGroupInfo>.resolvePrimaryNode(): Proxy? {
    val group =
        firstOrNull { it.name.equals(PRIMARY_GROUP_NAME, ignoreCase = true) }
            ?: firstOrNull()
            ?: return null
    val selected = group.now.trim()
    return if (selected.isEmpty()) null else resolveTerminalProxy(selected)
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
