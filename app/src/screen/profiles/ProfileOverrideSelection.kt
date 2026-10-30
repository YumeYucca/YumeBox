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

/**
 * The override selection of the profile being edited, together with the gate that keeps an early
 * toggle from overwriting the persisted list before the binding has been read.
 *
 * The page owns the I/O: [toggle] and [reorder] only return the new list when it actually changed,
 * so the composition never has to reason about the flag itself.
 */
@Stable
internal class ProfileOverrideSelection internal constructor() {
    var ids by mutableStateOf(emptyList<String>())
        private set

    /** False until the binding has been read; every mutation before that is dropped on purpose. */
    var isLoaded by mutableStateOf(false)
        private set

    fun bind(loadedIds: List<String>?) {
        ids = loadedIds.orEmpty().distinct()
        isLoaded = true
    }

    /** Adds or removes [overrideId]; returns the new list, or null when nothing changed. */
    fun toggle(overrideId: String, selected: Boolean): List<String>? {
        if (!isLoaded) return null
        val next =
            when {
                selected && overrideId !in ids -> ids + overrideId
                !selected && overrideId in ids -> ids - overrideId
                else -> null
            }
        return next?.also { ids = it }
    }

    /** Moves one row inside [visibleIds], the order the user is dragging in. */
    fun reorder(visibleIds: List<String>, from: Int, to: Int): List<String>? {
        if (!isLoaded) return null
        val next =
            reorderVisibleOverrideIds(
                allIds = ids,
                visibleIds = visibleIds,
                from = from,
                to = to,
            )
        if (next == ids) return null
        ids = next
        return next
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
