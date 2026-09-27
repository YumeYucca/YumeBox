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

package com.github.yumeyucca.yumebox.runtime.service.root

import com.topjohnwu.superuser.Shell

/** Package → uid lookups through the root shell; one full listing serves every query. */
object RootPackageShell {
    private class CacheEntry(val uids: Map<String, Int>, val cachedAt: Long)

    private val packageUidRegex = Regex("""^package:(\S+).*(?:uid|userId):(\d+)\b""")

    @Volatile private var cache: CacheEntry? = null

    fun hasRootAccess(): Boolean = runCatching { Shell.getShell().isRoot }.getOrDefault(false)

    /**
     * Uids of [packages] (every package when null or empty), or null without root. A package
     * missing from the cached listing forces a fresh one, so new installs resolve immediately.
     */
    fun queryPackageUidMap(packages: Set<String>? = null): Map<String, Int>? {
        val wanted =
            packages?.map(String::trim)?.filter(String::isNotEmpty)?.toSet()?.takeIf { it.isNotEmpty() }
        val cached = freshCache()
        val uids =
            if (cached != null && (wanted == null || wanted.all(cached::containsKey))) {
                cached
            } else {
                listPackageUids() ?: return null
            }
        return if (wanted == null) uids else uids.filterKeys(wanted::contains).ifEmpty { null }
    }

    fun queryInstalledPackageNames(): Set<String>? = (freshCache() ?: listPackageUids())?.keys

    private fun freshCache(): Map<String, Int>? =
        cache?.takeIf { System.currentTimeMillis() - it.cachedAt <= CACHE_TTL_MS }?.uids

    private fun listPackageUids(): Map<String, Int>? {
        if (!hasRootAccess()) return null
        val result = Shell.cmd("cmd package list packages -U || pm list packages -U").exec()
        if (!result.isSuccess) return null
        val uids =
            result.out
                .asSequence()
                .mapNotNull { line ->
                    val match = packageUidRegex.find(line.trim()) ?: return@mapNotNull null
                    val uid = match.groupValues[2].toIntOrNull() ?: return@mapNotNull null
                    match.groupValues[1] to uid
                }
                .toMap()
                .ifEmpty { return null }
        cache = CacheEntry(uids, System.currentTimeMillis())
        return uids
    }

    private const val CACHE_TTL_MS = 5 * 60 * 1000L
}
