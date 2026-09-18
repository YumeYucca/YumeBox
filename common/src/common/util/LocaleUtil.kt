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

import java.util.*

object LocaleUtil {
    const val UNKNOWN_FLAG_CODE = "xx"

    private const val FLAG_CDN_BASE_URL = "https://hatscripts.github.io/circle-flags/flags/"
    private const val UNKNOWN_FLAG_PATH = "other/earth.svg"

    @Volatile
    private var override: Locale? = null

    fun setCurrentLocale(locale: Locale?) {
        override = locale
    }

    fun currentLocale(): Locale = override ?: Locale.getDefault()

    fun unknownFlagUrl(baseUrl: String = FLAG_CDN_BASE_URL): String = "$baseUrl$UNKNOWN_FLAG_PATH"

    fun normalizeFlagUrl(
        countryCode: String?,
        baseUrl: String = FLAG_CDN_BASE_URL,
    ): String {
        val code = countryCode?.trim()?.lowercase().orEmpty()
        if (code.isEmpty() || code == UNKNOWN_FLAG_CODE) {
            return unknownFlagUrl(baseUrl)
        }
        return "$baseUrl$code.svg"
    }
}
