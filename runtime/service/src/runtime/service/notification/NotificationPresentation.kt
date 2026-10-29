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
import com.github.yumeyucca.yumebox.data.model.NotificationDisplaySettings
import com.github.yumeyucca.yumebox.data.model.NotificationLineValues
import com.github.yumeyucca.yumebox.data.model.resolve
import com.github.yumeyucca.yumebox.runtime.service.profile.Imported
import java.time.Instant
import java.time.ZoneId
import com.github.yumeyucca.yumebox.core.locale.YumeTxt

/** What the ongoing notification renders. */
internal sealed class NotificationPresentation {
    abstract val title: String
    abstract val content: String
    abstract val expandedText: String

    /** Traffic figures; also feeds the Super Island payload. */
    data class Running(
        override val title: String,
        override val content: String,
        override val expandedText: String,
        val compactTraffic: String,
        val currentNode: String?,
    ) : NotificationPresentation()

    /** No traffic figures (traffic notification disabled). */
    data class Status(
        override val title: String,
        override val content: String,
        override val expandedText: String,
    ) : NotificationPresentation()
}

internal object NotificationPresentationFactory {
    fun createRunning(
        profileName: String,
        profile: Imported?,
        currentNode: String?,
        trafficNow: Long,
        trafficTotal: Long = 0L,
        displaySettings: NotificationDisplaySettings = NotificationDisplaySettings(),
    ): NotificationPresentation.Running {
        val used = profile?.let { (it.upload + it.download).coerceAtLeast(0L) } ?: 0L
        val total = profile?.total?.coerceAtLeast(0L) ?: 0L
        val expire = profile?.expire?.takeIf { it > 0L }
        val node = currentNode ?: YumeTxt.Service.Notification.NoNode
        val speedLine = buildSpeedLine(trafficNow)
        val values =
            NotificationLineValues(
                profileName = profileName,
                currentNode = node,
                liveTraffic = speedLine,
                usedTraffic = formatBytes(used),
                sessionTraffic = buildSessionTrafficLine(trafficTotal),
                totalTraffic = if (total > 0L) formatBytes(total) else "—",
                remainingTraffic =
                    if (total > 0L) formatBytes((total - used).coerceAtLeast(0L)) else "—",
                subscriptionUsage = buildUsageLine(profile),
                expiryDate = expire?.let(::expireDate) ?: "—",
                status = YumeTxt.Service.Notification.Running,
            )
        val title =
            displaySettings.firstLine
                .resolve(values, displaySettings.firstLineText)
                .ifBlank { profileName }
        val content = displaySettings.secondLine.resolve(values, displaySettings.secondLineText)
        return NotificationPresentation.Running(
            title = title,
            content = content,
            expandedText =
                listOf(content, YumeTxt.Service.Notification.CurrentNode.format(node))
                    .filter { it.isNotBlank() }
                    .joinToString("\n"),
            compactTraffic = buildCompactTrafficLine(trafficNow),
            currentNode = currentNode,
        )
    }

    fun createStatus(
        profileName: String,
        status: String,
    ): NotificationPresentation.Status =
        NotificationPresentation.Status(
            title = profileName,
            content = status,
            expandedText = status,
        )

    /**
     * Values only, no labels, so the island can render it: `1.2 GB / 100 GB | 2026-08-31`.
     */
    private fun buildUsageLine(profile: Imported?): String {
        val used = profile?.let { (it.upload + it.download).coerceAtLeast(0L) } ?: 0L
        val total = profile?.total ?: 0L
        val usage =
            when {
                total > 0L -> "${formatBytes(used)} / ${formatBytes(total)}"
                used > 0L -> formatBytes(used)
                else -> ""
            }
        val expire = profile?.expire?.takeIf { it > 0L }?.let { expireDate(it) }
        return listOfNotNull(usage.takeIf { it.isNotEmpty() }, expire)
            .joinToString(" | ")
            .ifBlank { "—" }
    }

    private fun expireDate(expireAt: Long): String =
        Instant.ofEpochMilli(expireAt).atZone(ZoneId.systemDefault()).toLocalDate().toString()

    private fun buildCompactTrafficLine(trafficNow: Long): String {
        val upNow = decodeTrafficHalf(trafficNow ushr 32)
        val downNow = decodeTrafficHalf(trafficNow and 0xFFFFFFFFL)
        return formatSpeed((upNow + downNow).coerceAtLeast(0L))
    }

    private fun buildSpeedLine(trafficNow: Long): String {
        val upNow = decodeTrafficHalf(trafficNow ushr 32)
        val downNow = decodeTrafficHalf(trafficNow and 0xFFFFFFFFL)
        return YumeTxt.Service.Notification.SpeedLine.format(
            formatSpeed(downNow),
            formatSpeed(upNow),
        )
    }

    private fun buildSessionTrafficLine(trafficTotal: Long): String {
        val upTotal = decodeTrafficHalf(trafficTotal ushr 32)
        val downTotal = decodeTrafficHalf(trafficTotal and 0xFFFFFFFFL)
        return YumeTxt.Service.Notification.TotalTraffic.format(
            formatBytes((upTotal + downTotal).coerceAtLeast(0L)),
        )
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
