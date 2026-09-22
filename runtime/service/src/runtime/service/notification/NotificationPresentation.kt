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

package com.github.yumeyucca.yumebox.runtime.service.notification

import com.github.yumeyucca.yumebox.common.util.formatBytes
import com.github.yumeyucca.yumebox.common.util.formatSpeed
import com.github.yumeyucca.yumebox.runtime.service.profile.Imported
import java.time.Instant
import java.time.ZoneId

internal data class NotificationPresentation(
    val title: String,
    val content: String,
    val expandedText: String,
    val subText: String? = null,
    val compactText: String = content,
    val isRunning: Boolean = false,
    val currentNode: String? = null,
)

internal object NotificationPresentationFactory {
    fun createRunning(
        profileName: String,
        profile: Imported?,
        currentNode: String?,
        trafficNow: Long,
    ): NotificationPresentation {
        val usageLine = buildUsageLine(profile)
        val node = currentNode ?: "未选择"
        return NotificationPresentation(
            title = profileName,
            content = usageLine,
            expandedText = "$usageLine\n当前节点：$node",
            subText = null,
            compactText = buildCompactTrafficLine(trafficNow),
            isRunning = true,
            currentNode = currentNode,
        )
    }

    fun createStatus(profileName: String, status: String): NotificationPresentation =
        NotificationPresentation(
            title = profileName,
            content = status,
            expandedText = status,
            subText = null,
        )

    /**
     * Usage line, second line of the island. Values only, no labels, so the island can render it:
     * `1.2 GB / 100 GB | 2026-08-31`.
     *
     * Without a traffic limit only the used amount is shown, without an expiry only the usage part;
     * a config that carries no subscription information falls back to its own name.
     */
    private fun buildUsageLine(profile: Imported?): String {
        val used = profile?.let { (it.upload + it.download).coerceAtLeast(0L) } ?: 0L
        val total = profile?.total ?: 0L
        val usage =
            when {
                total > 0L -> "${formatBytes(used)} / ${formatBytes(total)}"
                used > 0L -> formatBytes(used)
                else -> profile?.name?.takeIf { it.isNotBlank() }.orEmpty()
            }
        val expire = profile?.expire?.takeIf { it > 0L }?.let { expireDate(it) }
        return listOfNotNull(usage.takeIf { it.isNotEmpty() }, expire).joinToString(" | ")
    }

    private fun expireDate(expireAt: Long): String =
        Instant.ofEpochMilli(expireAt).atZone(ZoneId.systemDefault()).toLocalDate().toString()

    private fun buildCompactTrafficLine(trafficNow: Long): String {
        val upNow = decodeTrafficHalf(trafficNow ushr 32)
        val downNow = decodeTrafficHalf(trafficNow and 0xFFFFFFFFL)
        return formatSpeed((upNow + downNow).coerceAtLeast(0L))
    }

    private fun decodeTrafficHalf(encoded: Long): Long {
        val type = (encoded ushr 30) and 0x3L
        val payload = encoded and 0x3FFFFFFFL
        return when (type.toInt()) {
            0 -> payload
            1 -> (payload * 1024L) / 100L
            2 -> (payload * 1024L * 1024L) / 100L
            3 -> (payload * 1024L * 1024L * 1024L) / 100L
            else -> 0L
        }
    }
}
