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

import com.github.yumeyucca.yumebox.common.util.ByteFormatter
import com.github.yumeyucca.yumebox.common.util.formatEpochDate
import com.github.yumeyucca.yumebox.data.model.NotificationDisplaySettings
import com.github.yumeyucca.yumebox.data.model.NotificationLineValues
import com.github.yumeyucca.yumebox.data.model.resolve
import com.github.yumeyucca.yumebox.domain.model.TrafficData
import com.github.yumeyucca.yumebox.runtime.service.profile.Imported
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
                usedTraffic = ByteFormatter.format(used),
                sessionTraffic = buildSessionTrafficLine(trafficTotal),
                totalTraffic = if (total > 0L) ByteFormatter.format(total) else "—",
                remainingTraffic =
                    if (total > 0L) ByteFormatter.format((total - used).coerceAtLeast(0L)) else "—",
                subscriptionUsage = buildUsageLine(used, total, expire),
                expiryDate = expire?.let(::formatEpochDate) ?: "—",
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
    private fun buildUsageLine(used: Long, total: Long, expire: Long?): String {
        val usage =
            when {
                total > 0L -> "${ByteFormatter.format(used)} / ${ByteFormatter.format(total)}"
                used > 0L -> ByteFormatter.format(used)
                else -> ""
            }
        val expiry = expire?.let(::formatEpochDate)
        return listOfNotNull(usage.takeIf { it.isNotEmpty() }, expiry)
            .joinToString(" | ")
            .ifBlank { "—" }
    }


    private fun buildCompactTrafficLine(trafficNow: Long): String {
        val traffic = TrafficData.from(trafficNow)
        return ByteFormatter.formatSpeed((traffic.upload + traffic.download).coerceAtLeast(0L))
    }

    private fun buildSpeedLine(trafficNow: Long): String {
        val traffic = TrafficData.from(trafficNow)
        return YumeTxt.Service.Notification.SpeedLine.format(
            ByteFormatter.formatSpeed(traffic.download),
            ByteFormatter.formatSpeed(traffic.upload),
        )
    }

    private fun buildSessionTrafficLine(trafficTotal: Long): String {
        val traffic = TrafficData.from(trafficTotal)
        return YumeTxt.Service.Notification.TotalTraffic.format(
            ByteFormatter.format((traffic.upload + traffic.download).coerceAtLeast(0L)),
        )
    }
}
