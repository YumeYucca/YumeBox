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
 * Copyright (c) YumeYucca 2025 - Present
 *
 */

package com.github.yumeyucca.yumebox.presentation.navigation

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.*
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.github.yumeyucca.yumebox.presentation.component.LocalNavigator
import com.github.yumeyucca.yumebox.presentation.component.Navigator

@Immutable
internal class SecondaryDetailStack(
    val value: Value<ChildStack<Any, DetailRouteChild>>,
)

@Composable
internal fun rememberSecondaryDetailStack(navigator: Navigator): SecondaryDetailStack {
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
    val childStack =
        remember(componentContext, navigator) {
            componentContext.childStack(
                source = navigator.navigation,
                initialConfiguration = Route.About,
                serializer = null,
                handleBackButton = false,
            ) { rawRoute, _ ->
                DetailRouteChild(rawRoute as Route, navigator)
            }
        }
    return remember(childStack) { SecondaryDetailStack(childStack) }
}

@Composable
internal fun SecondaryDetailHost(stack: SecondaryDetailStack) {
    val childStack by stack.value.subscribeAsState()
    val animation: StackAnimation<Any, DetailRouteChild> =
        remember {
            stackAnimation(fade(tween(300)) + slide(tween(400)) + scale(tween(500)))
        }
    Children(
        stack = childStack,
        modifier = Modifier.fillMaxSize(),
        animation = animation,
    ) { child -> child.instance.Content() }
}

internal class DetailRouteChild(private val route: Route, private val navigator: Navigator) {
    @Composable
    fun Content() {
        CompositionLocalProvider(LocalNavigator provides navigator) {
            RouteContent(route, navigator)
        }
    }
}
