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

package com.github.yumeyucca.yumebox.runtime.service.profile

import com.github.yumeyucca.yumebox.common.util.SubscriptionUserAgentDefaults
import com.tencent.mmkv.MMKV

/** Resolves the app-configured subscription User-Agent (settings store). */
object SubscriptionUserAgent {
    const val DEFAULT = SubscriptionUserAgentDefaults.DEFAULT

    private const val SETTINGS_STORE_ID = "settings"
    private const val CUSTOM_USER_AGENT_KEY = "customUserAgent"

    fun resolve(): String {
        val custom = runCatching {
            MMKV.mmkvWithID(SETTINGS_STORE_ID, MMKV.MULTI_PROCESS_MODE).decodeString(CUSTOM_USER_AGENT_KEY)
        }
            .getOrNull()
            ?.trim()
        return custom?.takeIf { it.isNotEmpty() } ?: DEFAULT
    }
}
