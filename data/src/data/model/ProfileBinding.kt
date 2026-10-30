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

package com.github.yumeyucca.yumebox.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ProfileBinding(
    val profileId: String = "",
    val overrideIds: List<String> = emptyList(),
) {
    companion object {
        fun withOverride(profileId: String, overrideId: String): ProfileBinding =
            ProfileBinding(profileId = profileId, overrideIds = listOf(overrideId))
    }

    fun addOverride(overrideId: String, index: Int? = null): ProfileBinding {
        if (overrideIds.contains(overrideId)) return this

        val newList =
            if (index != null) {
                overrideIds.toMutableList().apply { add(index.coerceAtMost(size), overrideId) }
            } else {
                overrideIds + overrideId
            }

        return copy(overrideIds = newList)
    }

    fun removeOverride(overrideId: String): ProfileBinding =
        copy(overrideIds = overrideIds - overrideId)

    fun clearOverrides(): ProfileBinding = copy(overrideIds = emptyList())
}
