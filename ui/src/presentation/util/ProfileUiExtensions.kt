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

package com.github.yumeyucca.yumebox.presentation.util

import com.github.yumeyucca.yumebox.common.util.ByteFormatter
import com.github.yumeyucca.yumebox.core.util.scaleBytes
import com.github.yumeyucca.yumebox.runtime.api.Profile
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.pow

val Profile.usedBytes: Long
    get() = upload + download

private val Profile.totalBytes: Long?
    get() = if (total > 0) total else null

private val Profile.expireAt: Long?
    get() = if (expire > 0) expire else null

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
                String.format(
                    Locale.ROOT,
                    "%.2f",
                    remainingBytes / 1024.0.pow(scaledTotal.rank),
                )
            }
        // totalBytes is non-null only when total > 0, so the division is safe.
        val ratio = (remainingBytes.toFloat() / totalBytesValue).coerceIn(0f, 1f)
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

