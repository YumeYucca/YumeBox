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

package com.github.yumeyucca.yumebox.data.model


import com.github.yumeyucca.yumebox.core.locale.YumeTxt

const val PROXY_SHEET_HEIGHT_FRACTION_MIN = 0.5f
const val PROXY_SHEET_HEIGHT_FRACTION_MAX = 0.8f
const val PROXY_SHEET_HEIGHT_FRACTION_DEFAULT = 0.55f

fun normalizeProxySheetHeightFraction(value: Float): Float =
    value.coerceIn(PROXY_SHEET_HEIGHT_FRACTION_MIN, PROXY_SHEET_HEIGHT_FRACTION_MAX)

enum class ProxyDisplayMode {
    SINGLE_DETAILED,
    SINGLE_SIMPLE,
    DOUBLE_DETAILED,
    DOUBLE_SIMPLE;

    val displayName: String
        get() =
            when (this) {
                SINGLE_DETAILED -> YumeTxt.Proxy.DisplayMode.SingleDetailed
                SINGLE_SIMPLE -> YumeTxt.Proxy.DisplayMode.SingleSimple
                DOUBLE_DETAILED -> YumeTxt.Proxy.DisplayMode.DoubleDetailed
                DOUBLE_SIMPLE -> YumeTxt.Proxy.DisplayMode.DoubleSimple
            }

    val isSingleColumn: Boolean
        get() = this == SINGLE_DETAILED || this == SINGLE_SIMPLE

    val showDetail: Boolean
        get() = this == SINGLE_DETAILED || this == DOUBLE_DETAILED
}

enum class ProxySortMode {
    DEFAULT,
    BY_NAME,
    BY_LATENCY;

    val displayName: String
        get() =
            when (this) {
                DEFAULT -> YumeTxt.Proxy.SortMode.Default
                BY_NAME -> YumeTxt.Proxy.SortMode.ByName
                BY_LATENCY -> YumeTxt.Proxy.SortMode.ByLatency
            }
}
