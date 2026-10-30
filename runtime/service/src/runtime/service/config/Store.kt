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

package com.github.yumeyucca.yumebox.runtime.service.config


import kotlin.reflect.KProperty

class Store(val provider: StoreProvider) {
    interface Delegate<T> {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): T

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T)
    }

    fun boolean(key: String, defaultValue: Boolean): Delegate<Boolean> =
        object : Delegate<Boolean> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean =
                provider.getBoolean(key, defaultValue)

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
                provider.setBoolean(key, value)
            }
        }

    fun <T> typedString(key: String, from: (String) -> T?, to: (T?) -> String): Delegate<T?> {
        return object : Delegate<T?> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): T? {
                val value = provider.getString(key, to(null))
                return from(value)
            }

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
                provider.setString(key, to(value))
            }
        }
    }
}
