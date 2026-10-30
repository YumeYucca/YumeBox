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

package com.github.yumeyucca.yumebox.common.util

import com.github.yumeyucca.yumebox.core.util.scaleBytes
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/** A formatted byte count split into its number and unit, so UIs can style the unit separately. */
data class FormattedBytes(
    val value: String,
    val unit: String,
)

object ByteFormatter {
    private val SI_UNITS = arrayOf("B", "KB", "MB", "GB", "TB", "PB")

    @JvmStatic
    fun format(bytes: Long, decimals: Int? = null): String {
        val scaled = scaleBytes(bytes)
        if (scaled.rank == 0) return "${bytes.coerceAtLeast(0L)} B"
        val digits = decimals ?: if (scaled.rank <= 2) 1 else 2
        return "%.${digits}f ${SI_UNITS[scaled.rank]}".format(scaled.value)
    }

    @JvmStatic
    fun formatSpeed(bytesPerSecond: Long): String {
        val scaled = scaleBytes(bytesPerSecond, maxRank = 3)
        if (scaled.rank == 0) return "${bytesPerSecond.coerceAtLeast(0L)} B/s"
        val digits = if (scaled.rank <= 2) 1 else 2
        return "%.${digits}f ${SI_UNITS[scaled.rank]}/s".format(scaled.value)
    }

    @JvmStatic
    fun formatForDisplay(bytes: Long, isSpeed: Boolean = false): FormattedBytes {
        val suffix = if (isSpeed) "/s" else ""
        val scaled = scaleBytes(bytes, maxRank = 3)
        return when (scaled.rank) {
            0 -> FormattedBytes("${bytes.coerceAtLeast(0L)}", "B$suffix")
            3 -> FormattedBytes("%.2f".format(scaled.value), "GB$suffix")
            else -> FormattedBytes(adaptive(scaled.value), "${SI_UNITS[scaled.rank]}$suffix")
        }
    }

    /** One decimal below 10, none above — keeps the displayed number short. */
    private fun adaptive(num: Double): String =
        if (num < 10) "%.1f".format(num) else "%.0f".format(num)
}

private val dateTimeFormatters = ConcurrentHashMap<String, DateTimeFormatter>()

/**
 * Formats [epochMillis] with [pattern] in the device's zone and locale.
 *
 * Formatters are cached per pattern: the call sites sit in list rows, and building a
 * [DateTimeFormatter] (or the older `SimpleDateFormat`) per row shows up in traces.
 */
fun formatDateTime(
    epochMillis: Long,
    pattern: String,
): String {
    val formatter =
        dateTimeFormatters.computeIfAbsent(pattern) {
            DateTimeFormatter.ofPattern(it, Locale.getDefault())
        }
    return formatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
}

/** Date-only form of [formatDateTime]; the ISO `yyyy-MM-dd` shape used for expiries. */
fun formatEpochDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate().toString()
