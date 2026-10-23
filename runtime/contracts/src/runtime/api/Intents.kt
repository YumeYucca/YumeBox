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

package com.github.yumeyucca.yumebox.runtime.api

object Intents {
    private fun intentAction(packageName: String, actionName: String): String =
        "$packageName.intent.action.$actionName"

    fun actionProfileChanged(packageName: String): String =
        intentAction(packageName, "PROFILE_CHANGED")

    fun actionOverrideChanged(packageName: String): String =
        intentAction(packageName, "OVERRIDE_CHANGED")

    val ACTION_PROFILE_CHANGED: String
        get() = actionProfileChanged(packageName)

    val ACTION_OVERRIDE_CHANGED: String
        get() = actionOverrideChanged(packageName)

    const val EXTRA_UUID = "uuid"
    const val EXTRA_AFFECTS_RUNTIME = "affects_runtime"
}