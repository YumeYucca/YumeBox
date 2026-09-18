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
