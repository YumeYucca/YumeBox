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
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.navigate
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.value.Value

/**
 * Sends navigation commands to one Decompose child stack.
 *
 * There is deliberately no Compose mirror of the route history. The child stack is the only source
 * of truth; UI code that needs to observe it subscribes to the stack's [Value] directly.
 */
@Stable
class Navigator {
    val navigation = StackNavigation<Any>()

    private var stack: Value<ChildStack<Any, *>>? = null

    /** Called once by the component that creates the child stack. */
    fun bind(stack: Value<ChildStack<Any, *>>) {
        this.stack = stack
    }

    private fun appliedStack(): List<Any> =
        stack?.value?.items?.map { it.configuration }.orEmpty()

    /** The configuration currently on top, read from Decompose at call time. */
    val currentRoute: Any?
        get() = appliedStack().lastOrNull()

    /**
     * Pushes [key] with Decompose's own `pushNew` operation.
     *
     * Do not add another top-of-stack check here: `pushNew` already performs it against the applied
     * stack. A second check against a cached list is what made re-entering a page fail.
     */
    fun push(key: Any) {
        NavTrace.record("push $key")
        navigation.pushNew(key)
    }

    /** Pops the active configuration, keeping the root configuration. */
    fun pop(): Boolean {
        if (appliedStack().size <= 1) return false
        NavTrace.record("pop $currentRoute")
        navigation.pop()
        return true
    }

    /** Replaces the whole stack atomically. */
    fun replaceAll(keys: List<Any>) {
        val target = keys.distinct()
        require(target.isNotEmpty()) { "A child stack must keep a root configuration" }
        navigation.navigate { target }
    }

    /** Replaces only the active configuration. */
    fun replace(key: Any) {
        navigation.navigate { current ->
            when {
                current.isEmpty() -> listOf(key)
                current.last() == key -> current
                else -> current.dropLast(1) + key
            }
        }
    }

    /** Pops configurations until [predicate] accepts the active one, always keeping the root. */
    fun popUntil(predicate: (Any) -> Boolean) {
        navigation.navigate { current ->
            current.toMutableList().apply {
                while (size > 1 && !predicate(last())) {
                    removeAt(lastIndex)
                }
            }
        }
    }

    fun navigateUp(): Boolean = pop()

    fun popBackStack(): Boolean = pop()
}

val LocalNavigator = compositionLocalOf<Navigator> { error("LocalNavigator is not provided") }

/** Right-pane navigator, non-null only while the split shell is active. */
val LocalDetailNavigator = compositionLocalOf<Navigator?> { null }
