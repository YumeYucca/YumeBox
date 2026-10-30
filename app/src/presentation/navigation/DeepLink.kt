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

import android.net.Uri
import com.github.yumeyucca.yumebox.presentation.component.BottomBarDestination
import com.github.yumeyucca.yumebox.screen.rules.RulesProvidersTab

/** A `yumebox://` link, already resolved to the thing it opens. */
sealed interface DeepLink {
    /** A bottom-bar page; the shell scrolls to it. */
    data class Page(
        val destination: BottomBarDestination,
    ) : DeepLink

    /** A screen route; the shell opens or replaces it like any other navigation. */
    data class Screen(
        val route: Route,
    ) : DeepLink
}

/**
 * Parses `yumebox://page/<name>` and `yumebox://screen/<name>`.
 *
 * Returns `null` for anything unrecognised, so an old link from a previous release simply does
 * nothing instead of landing the user on an arbitrary page.
 */
internal fun deepLinkOf(uri: Uri): DeepLink? =
    when (uri.host) {
        "page" ->
            when (uri.lastPathSegment) {
                "home" -> DeepLink.Page(BottomBarDestination.Home)
                "proxy" -> DeepLink.Page(BottomBarDestination.Proxy)
                "profiles" -> DeepLink.Page(BottomBarDestination.Config)
                "settings" -> DeepLink.Screen(Route.Settings)
                else -> null
            }

        "screen" -> screenRouteOf(uri.lastPathSegment)?.let(DeepLink::Screen)
        else -> null
    }

private fun screenRouteOf(name: String?): Route? =
    when (name) {
        "appsettings" -> Route.AppSettings
        "network" -> Route.NetworkSettings
        "about" -> Route.About
        "access" -> Route.AccessControl
        "traffic" -> Route.TrafficStatistics
        "connection" -> Route.Connection
        "log" -> Route.Log
        "override" -> Route.Override
        "providers" -> Route.RulesAndProviders(RulesProvidersTab.Providers)
        else -> null
    }
