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

package com.github.yumeyucca.yumebox.presentation.component

/**
 * The last navigation events, kept so a crash report can say which push died and where.
 *
 * TEMPORARY DIAGNOSTIC: a navigation crash loses its own breadcrumbs the moment the process dies,
 * so they are buffered here (no I/O, no locks worth the name) and dumped by the application's
 * uncaught-exception handler. Remove this file once the navigation fault is understood.
 */
object NavTrace {
    private const val LIMIT = 40
    private val events = ArrayDeque<String>()

    @Synchronized
    fun record(event: String) {
        events.addLast("${System.currentTimeMillis()} $event")
        while (events.size > LIMIT) events.removeFirst()
    }

    @Synchronized
    fun dump(): String = events.joinToString("\n")
}
