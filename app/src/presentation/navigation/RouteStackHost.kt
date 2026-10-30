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

package com.github.yumeyucca.yumebox.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.StackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.github.yumeyucca.yumebox.presentation.component.BottomBarDestination
import com.github.yumeyucca.yumebox.presentation.component.LocalNavigator
import com.github.yumeyucca.yumebox.presentation.component.Navigator

/**
 * The route arguments of [Route.Main].
 *
 * The bottom pager's page is kept by [AppNavigationComponent] so it survives the screen leaving
 * composition while another Activity is on top, which means the page has to travel as data rather
 * than as screen state. Every stack is hosted below that provider, so the pager route reads them
 * from here instead of each host re-declaring them.
 */
@Immutable
internal data class MainRouteArgs(
    /**
     * The page to seed the pager with, read once. Deliberately a lambda: as a value it re-emitted on
     * every settle, which invalidated [LocalMainRouteArgs] readers and recomposed the whole screen.
     */
    val initialDestination: () -> BottomBarDestination,
    val onDestinationSettled: (BottomBarDestination) -> Unit,
    /** Navigator of the split-shell right pane; owned by the component so its stack survives recreation. */
    val detailNavigator: Navigator,
)

internal val LocalMainRouteArgs = compositionLocalOf<MainRouteArgs> {
    error("LocalMainRouteArgs is not provided")
}

/**
 * The detail pane's resting page.
 *
 * The pane is composed from a stack, so it never goes empty: with nothing open it shows this. Three
 * places have to agree on the value — the pane's initial stack, the reset when the user leaves the
 * proxy tab, and the "nothing is open, so draw the node pane" test — which is why it is named once
 * instead of repeated as a literal route.
 */
internal val PaneRestingRoute: Route = Route.About

/** A Decompose route stack paired with the [Navigator] that drives it. */
@Immutable
internal class RouteStack(val navigator: Navigator, val value: Value<ChildStack<Any, Route>>)

/**
 * The history the caller primed the navigator with. [Navigator] lives in `ui` and cannot name
 * [Route], so the stack starts from whatever routes it was handed — the whole list, not just its top:
 * a pane restored with two entries must come back with two, or its own back gesture has nowhere to go.
 */
private fun Navigator.primedBackStack(): List<Route> =
    backStack.mapNotNull { it as? Route }.ifEmpty { listOf(Route.Main) }

/**
 * A route stack with its own [LifecycleRegistry], for stacks that are not owned by a component.
 *
 * The app's root stack is created by [AppNavigationComponent] and has a real lifecycle; a pane that
 * hosts routes inside an existing screen owns its stack here instead.
 */
internal fun Navigator.routeStack(
    componentContext: ComponentContext,
    handleBackButton: Boolean = false,
): Value<ChildStack<Any, Route>> {
    val navigator = this
    val stack =
        componentContext.childStack(
            source = navigation,
            serializer = null,
            initialStack = { navigator.primedBackStack() },
            handleBackButton = handleBackButton,
        ) { rawRoute, _ ->
            // `Navigator` keeps a `StackNavigation<Any>` because it lives in `ui` and cannot name this
            // module's `Route`; every route pushed through it is a `Route`.
            rawRoute as Route
        }
    return stack
}

@Composable
internal fun rememberRouteStack(navigator: Navigator): RouteStack {
    // The registry exists because a decompose ChildStack needs a ComponentContext, not because the
    // routes react to lifecycle: both stacks are built with `serializer = null` and render plain
    // composables, so it is kept in the resumed state for as long as the host is composed.
    val lifecycle = remember { LifecycleRegistry() }
    val componentContext = remember { DefaultComponentContext(lifecycle) }
    DisposableEffect(lifecycle) {
        lifecycle.onCreate()
        lifecycle.onStart()
        lifecycle.onResume()
        onDispose {
            lifecycle.onPause()
            lifecycle.onStop()
            lifecycle.onDestroy()
        }
    }
    return remember(navigator) { RouteStack(navigator, navigator.routeStack(componentContext)) }
}

/** Renders [stack] with the shared route animation; both the root stack and the split shell use it. */
@Composable
internal fun RouteStackHost(
    stack: RouteStack,
    animation: StackAnimation<Any, Route> = remember { yumeStackAnimation() },
) {
    val childStack by stack.value.subscribeAsState()
    // The one place that writes the navigator's route history: whatever the stack applied — including
    // a pop performed by a screen's own back handler — is mirrored back here, so readers such as the
    // split shell's "is the pane empty?" check can never latch on a stale entry.
    SideEffect { stack.navigator.syncBackStack(childStack.items.map { it.configuration }) }
    // One preview slot per stack: a pane and the root window can then preview different files.
    val configPreviewSlot = remember { ConfigPreviewSlot() }
    CompositionLocalProvider(
        LocalNavigator provides stack.navigator,
        LocalConfigPreviewSlot provides configPreviewSlot,
    ) {
        Children(
            stack = childStack,
            modifier = Modifier.fillMaxSize(),
            animation = animation,
        ) { child -> RouteContent(child.instance, stack.navigator) }
    }
}
