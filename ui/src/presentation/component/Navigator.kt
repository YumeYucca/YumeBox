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

import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.navigate
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew

/**
 * Thin wrapper over the Decompose navigation stack with Compose-observable route history.
 *
 * Replaces compose-destinations' DestinationsNavigator. The [push]/[pop]/[replaceAll] methods cover
 * every navigation pattern used in YumeBox. The [navigateUp] alias keeps call sites that came from
 * the compose-destinations API churn-free.
 */
@Stable
class Navigator(initial: List<Any> = emptyList()) {
    /**
     * Route history, mirrored from the Decompose stack by the host that renders it.
     *
     * The Decompose stack is the source of truth: the host writes the applied stack back through
     * [syncBackStack] after every navigation transaction. Writing this list from `pop()` as well used
     * to give two writers that could disagree — a pop performed by the stack itself (a screen's own
     * back handler, a deep link) left the mirror one entry too long, which then latched "there is a
     * page to close" forever.
     */
    var backStack: List<Any> by mutableStateOf(initial)
        private set

    val navigation = StackNavigation<Any>()

    /** Pushes [key] onto the stack unless it is already on top (mirrors launchSingleTop). */
    fun push(key: Any) {
        NavTrace.record("push $key (stack=$backStack)")
        if (backStack.lastOrNull() == key) return
        // Recorded eagerly so a second tap in the same frame still sees it on top; the host confirms
        // the list with what the stack actually applied.
        backStack = backStack + key
        navigation.pushNew(key)
    }

    /** Pops the top entry. The root entry is never popped. Returns true if a pop happened. */
    fun pop(): Boolean {
        NavTrace.record("pop (stack=$backStack)")
        if (backStack.size <= 1) return false
        navigation.pop()
        return true
    }

    /**
     * Records the stack Decompose actually applied.
     *
     * [push] and [replaceAll] write [backStack] optimistically so a second tap in the same frame sees
     * the change; this has the last word over both.
     */
    fun syncBackStack(configurations: List<Any>) {
        if (backStack != configurations) backStack = configurations
    }

    /** Replaces the entire stack with [keys]. */
    fun replaceAll(keys: List<Any>) {
        val uniqueKeys = keys.distinct()
        if (backStack == uniqueKeys) return
        backStack = uniqueKeys
        navigation.navigate { uniqueKeys }
    }

    /** Replaces the top entry with [key]. */
    fun replace(key: Any) {
        if (backStack.lastOrNull() == key) return
        val target = if (backStack.isEmpty()) listOf(key) else backStack.dropLast(1) + key
        backStack = target
        navigation.navigate { target }
    }

    /** Pops entries until [predicate] accepts the top entry, without removing the root. */
    fun popUntil(predicate: (Any) -> Boolean) {
        val target = backStack.toMutableList()
        while (target.size > 1 && !predicate(target.last())) {
            target.removeAt(target.lastIndex)
        }
        if (target == backStack) return
        backStack = target
        navigation.navigate { target }
    }

    /** Compatibility alias for [pop] (compose-destinations API). */
    fun navigateUp(): Boolean = pop()

    /** Compatibility alias for [pop] (compose-destinations API). */
    fun popBackStack(): Boolean = pop()
}

/**
 * Opens [route] where the user expects to see it.
 *
 * In the dual-pane shell the page belongs next to what is already on screen, so it goes onto the
 * detail pane's own stack (and back returns to the pane's resting page); on a phone there is no second
 * pane and it becomes a new page. Pushing rather than replacing is what gives the pane a back stack —
 * a replaced stack cannot be popped, which left every pane page a dead end. Root pages call this
 * instead of re-deriving the shell mode themselves.
 */
fun Navigator.openRoute(route: Any, inDetailPane: Navigator? = null) {
    if (inDetailPane != null) inDetailPane.push(route) else push(route)
}

val LocalNavigator = compositionLocalOf<Navigator> { error("LocalNavigator is not provided") }

/**
 * Right-pane navigator for the tablet dual-pane shell.
 *
 * Non-null only when [WindowLayoutMode.usesSplitShell] is active. Treat non-null as the single
 * source of truth for "we are in split shell" — do not re-derive shell mode from width in modules.
 */
val LocalDetailNavigator = compositionLocalOf<Navigator?> { null }
