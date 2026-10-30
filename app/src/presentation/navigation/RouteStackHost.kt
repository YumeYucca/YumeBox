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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.StackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import com.github.yumeyucca.yumebox.presentation.component.BottomBarDestination
import com.github.yumeyucca.yumebox.presentation.component.LocalNavigator
import com.github.yumeyucca.yumebox.presentation.component.Navigator

/** State and stacks needed by the main pager route. */
@Immutable
internal data class MainRouteArgs(
    val destination: () -> BottomBarDestination,
    val onDestinationSettled: (BottomBarDestination) -> Unit,
    val rootStack: RouteStack,
    val detailStack: RouteStack,
)

internal val LocalMainRouteArgs = compositionLocalOf<MainRouteArgs> {
    error("LocalMainRouteArgs is not provided")
}

/** The route shown by the right pane when no detail page is open. */
internal val PaneRestingRoute: Route = Route.About

/** One real Decompose stack and the object that sends commands to it. */
@Immutable
internal class RouteStack(
    val navigator: Navigator,
    val value: Value<ChildStack<Any, RouteChild>>,
)

/** A component owned by one configuration in a [RouteStack]. */
internal class RouteChild(
    private val route: Route,
    private val navigator: Navigator,
    componentContext: ComponentContext,
) : ComponentContext by componentContext {
    @Composable
    fun Content() {
        RouteContent(route = route, navigator = navigator)
    }
}

/** Creates a child stack on an existing component context; no Compose-owned lifecycle is involved. */
internal fun Navigator.routeStack(
    componentContext: ComponentContext,
    initialRoute: Route,
    handleBackButton: Boolean,
): Value<ChildStack<Any, RouteChild>> {
    val stack =
        componentContext.childStack(
            source = navigation,
            serializer = null,
            initialStack = { listOf(initialRoute) },
            handleBackButton = handleBackButton,
        ) { rawRoute, childContext ->
            RouteChild(
                route = rawRoute as Route,
                navigator = this,
                componentContext = childContext,
            )
        }
    bind(stack)
    return stack
}

/** Renders the current Decompose stack. Navigation never writes a second route history here. */
@Composable
internal fun RouteStackHost(
    stack: RouteStack,
    animation: StackAnimation<Any, RouteChild> = remember { yumeStackAnimation() },
) {
    val childStack by stack.value.subscribeAsState()
    val configPreviewSlot = remember { ConfigPreviewSlot() }
    CompositionLocalProvider(
        LocalNavigator provides stack.navigator,
        LocalConfigPreviewSlot provides configPreviewSlot,
    ) {
        Children(
            stack = childStack,
            modifier = Modifier.fillMaxSize(),
            animation = animation,
        ) { child ->
            child.instance.Content()
        }
    }
}
