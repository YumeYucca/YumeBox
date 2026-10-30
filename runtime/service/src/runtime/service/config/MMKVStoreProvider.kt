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

package com.github.yumeyucca.yumebox.runtime.service.config

import com.tencent.mmkv.MMKV

class MMKVStoreProvider(private val mmkv: MMKV) : StoreProvider {
    override fun getString(key: String, defaultValue: String): String =
        mmkv.decodeString(key, defaultValue) ?: defaultValue

    override fun setString(key: String, value: String) {
        mmkv.encode(key, value)
    }

    override fun getStringSet(key: String, defaultValue: Set<String>): Set<String> =
        mmkv.decodeStringSet(key, defaultValue) ?: defaultValue

    override fun setStringSet(key: String, value: Set<String>) {
        mmkv.encode(key, value)
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        mmkv.decodeBool(key, defaultValue)

    override fun setBoolean(key: String, value: Boolean) {
        mmkv.encode(key, value)
    }
}

fun MMKV.asStoreProvider(): StoreProvider = MMKVStoreProvider(this)
