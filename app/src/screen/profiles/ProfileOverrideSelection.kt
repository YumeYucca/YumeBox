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

package com.github.yumeyucca.yumebox.screen.profiles

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.github.yumeyucca.yumebox.data.model.ProfileBinding

/**
 * The override selection of the profile being edited, together with the gate that keeps an early
 * toggle from overwriting the persisted list before the binding has been read.
 *
 * The page owns the I/O: [attach], [detach], [setEnabled] and [reorder] only report a change when
 * they actually changed the selection, so a no-op never reaches persistence.
 */
@Stable
internal class ProfileOverrideSelection internal constructor() {
    /** Every row attached to the profile, in drag order. */
    var ids by mutableStateOf(emptyList<String>())
        private set

    /** Attached rows whose apply checkbox is off. */
    var disabledIds by mutableStateOf(emptyList<String>())
        private set

    /** False until the binding has been read; every mutation before that is dropped on purpose. */
    var isLoaded by mutableStateOf(false)
        private set

    fun bind(binding: ProfileBinding?) {
        ids = binding?.overrideIds.orEmpty().distinct()
        val attachedIds = ids.toSet()
        disabledIds = binding?.disabledOverrideIds.orEmpty().filter(attachedIds::contains).distinct()
        isLoaded = true
    }

    fun isEnabled(overrideId: String): Boolean =
        overrideId in ids && overrideId !in disabledIds

    /** Attaches a row without applying it. */
    fun attach(overrideId: String): Boolean {
        if (!isLoaded || overrideId in ids) return false
        ids = ids + overrideId
        disabledIds = disabledIds + overrideId
        return true
    }

    fun detach(overrideId: String): Boolean {
        if (!isLoaded || overrideId !in ids) return false
        ids = ids - overrideId
        disabledIds = disabledIds - overrideId
        return true
    }

    /** Ticks or unticks an attached row; an id that is not attached cannot be enabled. */
    fun setEnabled(overrideId: String, enabled: Boolean): Boolean {
        if (!isLoaded || overrideId !in ids) return false
        val nextDisabled =
            when {
                enabled && overrideId in disabledIds -> disabledIds - overrideId
                !enabled && overrideId !in disabledIds -> disabledIds + overrideId
                else -> return false
            }
        disabledIds = nextDisabled
        return true
    }

    /** 自定义分流 is a normal attached row, but its switch removes it instead of disabling it. */
    fun setCustomRouting(overrideId: String, enabled: Boolean): Boolean {
        if (!isLoaded) return false
        return if (enabled) {
            if (overrideId in ids) {
                setEnabled(overrideId, true)
            } else {
                ids = ids + overrideId
                true
            }
        } else {
            detach(overrideId)
        }
    }

    /** Moves one row inside [visibleIds], the order the user is dragging in. */
    fun reorder(visibleIds: List<String>, from: Int, to: Int): Boolean {
        if (!isLoaded) return false
        val next =
            reorderVisibleOverrideIds(
                allIds = ids,
                visibleIds = visibleIds,
                from = from,
                to = to,
            )
        if (next == ids) return false
        ids = next
        return true
    }
}

@Composable
internal fun rememberProfileOverrideSelection(profileId: String): ProfileOverrideSelection =
    remember(profileId) { ProfileOverrideSelection() }

/**
 * Re-applies the order the user dragged onto [visibleIds] while the ids hidden by the filter keep
 * their slot.
 */
internal fun reorderVisibleOverrideIds(
    allIds: List<String>,
    visibleIds: List<String>,
    from: Int,
    to: Int,
): List<String> {
    if (from !in visibleIds.indices || to !in visibleIds.indices || from == to) return allIds
    val reorderedVisible = visibleIds.toMutableList()
    reorderedVisible.add(to, reorderedVisible.removeAt(from))
    val visibleSet = visibleIds.toSet()
    val replacements = reorderedVisible.iterator()
    return allIds.map { id -> if (id in visibleSet) replacements.next() else id }
}
