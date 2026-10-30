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

interface StoreProvider {
    fun getString(key: String, defaultValue: String): String

    fun setString(key: String, value: String)

    fun getStringSet(key: String, defaultValue: Set<String>): Set<String>

    fun setStringSet(key: String, value: Set<String>)

    fun getBoolean(key: String, defaultValue: Boolean): Boolean

    fun setBoolean(key: String, value: Boolean)
}
