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

package com.github.yumeyucca.yumebox.core.util

/** A byte count scaled down the shared 1024 ladder: [value] in the unit at [rank] (0 = bytes, 1 = K…, 5 = P…). */
data class ScaledBytes(val value: Double, val rank: Int)

/**
 * Walks the 1024 ladder shared by every byte formatter (SI-labelled "KB" UI text and IEC-labelled "KiB" core traffic
 * text). [maxRank] caps the unit range, e.g. 3 keeps everything at GB and below.
 */
fun scaleBytes(bytes: Long, maxRank: Int = 5): ScaledBytes {
    var value = bytes.coerceAtLeast(0L).toDouble()
    var rank = 0
    while (rank < maxRank && value >= 1024.0) {
        value /= 1024.0
        rank++
    }
    return ScaledBytes(value, rank)
}
