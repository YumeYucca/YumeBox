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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.extensions.compose.stack.animation.StackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.androidPredictiveBackAnimatableV1
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.predictiveBackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.scale
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.essenty.backhandler.BackHandler
import com.github.yumeyucca.yumebox.MainActivity
import com.github.yumeyucca.yumebox.presentation.component.BottomBarDestination
import com.github.yumeyucca.yumebox.presentation.component.Navigator
import com.github.yumeyucca.yumebox.presentation.component.rememberWindowLayoutMode

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

/** Owns every app navigation stack and all commands sent to them. */
class AppNavigationComponent(
    val componentContext: ComponentContext,
    val predictiveBackEnabledAtLaunch: Boolean,
) {
    private val mainDestinationState = mutableStateOf(BottomBarDestination.Home)

    val navigator = Navigator()
    val detailNavigator = Navigator()

    internal val routeStack =
        RouteStack(
            navigator = navigator,
            value =
                navigator.routeStack(
                    componentContext = componentContext.childContext("RootNavigation"),
                    initialRoute = Route.Main,
                    handleBackButton = true,
                ),
        )
    internal val detailStack =
        RouteStack(
            navigator = detailNavigator,
            value =
                detailNavigator.routeStack(
                    componentContext = componentContext.childContext("DetailNavigation"),
                    initialRoute = PaneRestingRoute,
                    handleBackButton = false,
                ),
        )

    internal val mainDestination: BottomBarDestination
        get() = mainDestinationState.value

    internal fun updateMainDestination(destination: BottomBarDestination) {
        mainDestinationState.value = destination
    }

    fun navigateBack() {
        navigator.pop()
    }

    fun handleDeepLink(link: DeepLink, inDetailPane: Boolean) {
        when (link) {
            is DeepLink.Page -> updateMainDestination(link.destination)
            is DeepLink.Screen ->
                (if (inDetailPane) detailNavigator else navigator).push(link.route)
        }
    }
}

@Composable
fun AppNavContainer(component: AppNavigationComponent) {
    val predictiveBackEnabled = component.predictiveBackEnabledAtLaunch
    val commitBack = remember(component) { component::navigateBack }
    val animation: StackAnimation<Any, RouteChild> = remember(component) {
        if (predictiveBackEnabled) {
            yumeAnimation(component.componentContext.backHandler, commitBack)
        } else {
            yumeStackAnimation()
        }
    }

    // Deep links belong to the retained navigation component, not to Route.Main's composition.
    // They therefore work even when a child route is currently covering the main pager.
    val usesSplitShell = rememberWindowLayoutMode().usesSplitShell
    val pendingDeepLink by MainActivity.pendingDeepLink.collectAsState()
    LaunchedEffect(pendingDeepLink, usesSplitShell) {
        val pending = pendingDeepLink ?: return@LaunchedEffect
        component.handleDeepLink(pending, inDetailPane = usesSplitShell)
        MainActivity.clearPendingDeepLink(pending)
    }

    val onDestinationSettled = remember(component) { component::updateMainDestination }
    val mainRouteArgs = remember(component) {
        MainRouteArgs(
            destination = { component.mainDestination },
            onDestinationSettled = onDestinationSettled,
            rootStack = component.routeStack,
            detailStack = component.detailStack,
        )
    }
    CompositionLocalProvider(LocalMainRouteArgs provides mainRouteArgs) {
        RouteStackHost(stack = component.routeStack, animation = animation)
    }
}
