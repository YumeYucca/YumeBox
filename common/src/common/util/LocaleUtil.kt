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

import java.util.Locale

object LocaleUtil {
    const val UNKNOWN_FLAG_CODE = "xx"

    private const val FLAG_BASE_URI = "circle-flag:///flags/"
    private const val UNKNOWN_FLAG_PATH = "other/earth.svg"

    @Volatile
    private var override: Locale? = null

    fun setCurrentLocale(locale: Locale?) {
        override = locale
    }

    fun currentLocale(): Locale = override ?: Locale.getDefault()

    fun unknownFlagUri(baseUri: String = FLAG_BASE_URI): String = "$baseUri$UNKNOWN_FLAG_PATH"

    fun normalizeFlagUri(
        countryCode: String?,
        baseUri: String = FLAG_BASE_URI,
    ): String {
        val normalizedCode =
            countryCode
                ?.trim()
                ?.takeIf { it.isNotEmpty() && it.all(Char::isLetter) }
                ?: return unknownFlagUri(baseUri)
        if (normalizedCode.equals(UNKNOWN_FLAG_CODE, ignoreCase = true)) {
            return unknownFlagUri(baseUri)
        }
        val code = normalizedCode.lowercase(Locale.ROOT)
        return "$baseUri$code.svg"
    }
}
