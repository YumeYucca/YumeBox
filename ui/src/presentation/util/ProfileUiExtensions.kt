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

@file:Suppress("UnusedReceiverParameter")

package com.github.yumeyucca.yumebox.presentation.util

import com.github.yumeyucca.yumebox.common.util.ByteFormatter
import com.github.yumeyucca.yumebox.core.util.scaleBytes
import com.github.yumeyucca.yumebox.runtime.api.Profile
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.pow

val Profile.enabled: Boolean
    get() = active

val Profile.provider: String?
    get() = null

val Profile.usedBytes: Long
    get() = upload + download

val Profile.totalBytes: Long?
    get() = if (total > 0) total else null

val Profile.expireAt: Long?
    get() = if (expire > 0) expire else null

val Profile.lastUpdatedAt: Long?
    get() = if (updatedAt > 0) updatedAt else null

fun Profile.getDisplayProvider(): String =
    when (type) {
        Profile.Type.Url -> provider ?: YumeTxt.Component.ProfileCard.RemoteSubscription
        Profile.Type.File -> YumeTxt.Component.ProfileCard.LocalFile
        Profile.Type.External -> YumeTxt.Component.ProfileCard.LocalConfig
    }

/** Traffic summary for the card: remaining value expressed in the total's unit, like "0.50 / 100.00 GB". */
data class ProfileTrafficPreview(
    val remainingNumber: String,
    val totalText: String,
    val ratio: Float,
    val lowTraffic: Boolean,
)

val Profile.trafficPreview: ProfileTrafficPreview?
    get() {
        val totalBytesValue = totalBytes ?: return null
        val remainingBytes = (totalBytesValue - usedBytes).coerceAtLeast(0L)
        val scaledTotal = scaleBytes(totalBytesValue)
        val remainingNumber =
            if (scaledTotal.rank == 0) {
                remainingBytes.toString()
            } else {
                "%.2f".format(remainingBytes / 1024.0.pow(scaledTotal.rank))
            }
        val ratio =
            if (totalBytesValue > 0) {
                (remainingBytes.toFloat() / totalBytesValue).coerceIn(0f, 1f)
            } else {
                0f
            }
        return ProfileTrafficPreview(
            remainingNumber = remainingNumber,
            totalText = ByteFormatter.format(totalBytesValue),
            ratio = ratio,
            lowTraffic = ratio <= 0.2f,
        )
    }

/** Expiry summary for the card's right-side badge. */
data class ProfileExpiryPreview(
    val daysLeft: Long,
    val dateText: String,
    val warning: Boolean,
)

val Profile.expiryPreview: ProfileExpiryPreview?
    get() {
        val expireAtValue = expireAt ?: return null
        val date =
            Instant.ofEpochMilli(expireAtValue)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        val daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), date)
        return ProfileExpiryPreview(
            daysLeft = daysLeft,
            dateText = date.toString(),
            warning = daysLeft <= EXPIRY_WARNING_DAYS,
        )
    }

private const val EXPIRY_WARNING_DAYS = 10L

fun Profile.shouldShowUpdateButton(): Boolean = type == Profile.Type.Url

fun Profile.isConfigSaved(workDir: File): Boolean = File(workDir, "$uuid/config.yaml").exists()

fun Profile.getRelativeUpdatedTime(): String? =
    lastUpdatedAt?.let { timestamp -> getRelativeTimeString(timestamp) }

private fun getRelativeTimeString(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val minutes = diff / (1000 * 60)
    val hours = diff / (1000 * 60 * 60)

    return when {
        diff < 60 * 1000 -> YumeTxt.Component.ProfileCard.JustNow
        minutes < 60 -> YumeTxt.Component.ProfileCard.MinutesAgo.format(minutes.toInt())
        hours < 24 -> YumeTxt.Component.ProfileCard.HoursAgo.format(hours.toInt())
        else -> {
            val days = diff / (1000 * 60 * 60 * 24)
            YumeTxt.Component.ProfileCard.DaysAgo.format(days.toInt())
        }
    }
}
