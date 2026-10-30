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

import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.animation.*
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.androidPredictiveBackAnimatableV1
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.predictiveBackAnimation
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandler
import com.github.yumeyucca.yumebox.presentation.component.BottomBarDestination
import com.github.yumeyucca.yumebox.presentation.component.Navigator

internal fun <C : Any, T : Any> yumeStackAnimation(): StackAnimation<C, T> =
    stackAnimation(fade(tween(300)) + slide(tween(400)) + scale(tween(500)))

@OptIn(ExperimentalDecomposeApi::class)
private fun <T : Any> yumeAnimation(
    backHandler: BackHandler,
    onBack: () -> Unit,
): StackAnimation<Any, T> =
    predictiveBackAnimation(
        backHandler = backHandler,
        onBack = onBack,
        fallbackAnimation = yumeStackAnimation(),
        selector = { event, _, _ -> androidPredictiveBackAnimatableV1(event) },
    )

class AppNavigationComponent(
    val componentContext: ComponentContext,
    val predictiveBackEnabledAtLaunch: Boolean,
) {
    // The bottom pager is nested in Route.Main, so keep its semantic destination outside the
    // screen composition while another Activity or a child route is on top.
    private val mainDestinationState = mutableStateOf(BottomBarDestination.Home)

    val navigator = Navigator(listOf(Route.Main))
    // The pane always has a root route: it is composed from a stack, so an empty stack would leave
    // the pane blank and drop a later detail route. About is that resting content. It lives here, not
    // in [MainScreen], because the screen is recreated by every root push and by config changes, which
    // used to reset the pane under the user while the pager page survived.
    val detailNavigator = Navigator(listOf(PaneRestingRoute))
    internal val childStack: Value<ChildStack<Any, Route>> =
        navigator.routeStack(componentContext, handleBackButton = true)
    internal val routeStack = RouteStack(navigator, childStack)

    internal fun updateMainDestination(destination: BottomBarDestination) {
        mainDestinationState.value = destination
    }

    internal val mainDestination: BottomBarDestination
        get() = mainDestinationState.value
}

@Composable
fun AppNavContainer(component: AppNavigationComponent) {
    val componentContext = component.componentContext
    val navigator = component.navigator
    val predictiveBackEnabled = component.predictiveBackEnabledAtLaunch
    val commitBack: () -> Unit = remember(navigator) { { navigator.pop() } }
    val animation: StackAnimation<Any, Route> = remember(component) {
        if (predictiveBackEnabled) {
            yumeAnimation(componentContext.backHandler, commitBack)
        } else {
            yumeStackAnimation()
        }
    }
    // Provided above the stack so every route (root page or split-shell pane) reads the pager's
    // persisted page from one place instead of the host threading it through. The config-preview slot
    // is deliberately NOT provided here: [RouteStackHost] gives each stack its own.
    // A bound reference is a new object per call, which would make an @Immutable argument unstable.
    val onDestinationSettled = remember(component) { component::updateMainDestination }
    // Stable for the life of the component: the page travels as a lambda that is read once, so a tab
    // settle no longer invalidates every [LocalMainRouteArgs] reader.
    val mainRouteArgs = remember(component) {
        MainRouteArgs(
            initialDestination = { component.mainDestination },
            onDestinationSettled = onDestinationSettled,
            detailNavigator = component.detailNavigator,
        )
    }
    CompositionLocalProvider(
        LocalMainRouteArgs provides mainRouteArgs,
    ) {
        RouteStackHost(stack = component.routeStack, animation = animation)
    }
}
