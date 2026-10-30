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
    /**
     * Rows that are attached to the profile but not applied, in the same order as [overrideIds].
     * Attaching an override adds it here so it appears on the page without changing the running
     * chain until the user ticks its checkbox.
     */
    val disabledOverrideIds: List<String> = emptyList(),
) {
    /** The ordered overrides that are actually applied. */
    val enabledOverrideIds: List<String>
        get() {
            if (disabledOverrideIds.isEmpty()) return overrideIds
            val disabled = disabledOverrideIds.toSet()
            return overrideIds.filterNot(disabled::contains)
        }

    fun isOverrideEnabled(overrideId: String): Boolean =
        overrideId in overrideIds && overrideId !in disabledOverrideIds

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
        copy(
            overrideIds = overrideIds - overrideId,
            disabledOverrideIds = disabledOverrideIds - overrideId,
        )

    fun clearOverrides(): ProfileBinding =
        copy(overrideIds = emptyList(), disabledOverrideIds = emptyList())
}
