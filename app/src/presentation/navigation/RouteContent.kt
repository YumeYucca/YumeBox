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

import androidx.compose.runtime.Composable
import com.github.yumeyucca.yumebox.MainScreen
import com.github.yumeyucca.yumebox.presentation.component.Navigator
import com.github.yumeyucca.yumebox.screen.about.AboutScreen
import com.github.yumeyucca.yumebox.screen.about.OpenSourceLicensesScreen
import com.github.yumeyucca.yumebox.screen.connection.ConnectionDetailScreen
import com.github.yumeyucca.yumebox.screen.connection.ConnectionScreen
import com.github.yumeyucca.yumebox.screen.extensions.ExtensionsScreen
import com.github.yumeyucca.yumebox.screen.log.LogScreen
import com.github.yumeyucca.yumebox.screen.navigation.*
import com.github.yumeyucca.yumebox.screen.profiles.ProfileEditScreen
import com.github.yumeyucca.yumebox.screen.profiles.ProfileOverrideScreen
import com.github.yumeyucca.yumebox.screen.rules.RulesAndProvidersScreen
import com.github.yumeyucca.yumebox.screen.settings.*
import com.github.yumeyucca.yumebox.screen.settings.backup.BackupRestoreScreen
import com.github.yumeyucca.yumebox.screen.traffic.TrafficStatisticsScreen

@Composable
internal fun RouteContent(route: Route, navigator: Navigator) {
    // TEMPORARY DIAGNOSTIC: names the page being composed when a crash happens.
    com.github.yumeyucca.yumebox.presentation.component.NavTrace.record("compose $route")
    when (route) {
        Route.Main -> {
            val args = LocalMainRouteArgs.current
            MainScreen(
                navigator = navigator,
                initialDestination = args.destination(),
                onDestinationSettled = args.onDestinationSettled,
                rootStack = args.rootStack,
                detailStack = args.detailStack,
            )
        }

        is Route.MoeWallpaperCrop ->
            MoeWallpaperCropScreen(
                navigator,
                route.wallpaperUri,
                route.initialZoom,
                route.initialBiasX,
                route.initialBiasY,
            )

        Route.Settings -> SettingPager()
        Route.AppSettings -> AppSettingsScreen(navigator)
        Route.NotificationSettings -> NotificationSettingsScreen()
        Route.NetworkSettings -> NetworkSettingsScreen(navigator)
        Route.VpnServiceOptions -> VpnServiceOptionsScreen()
        Route.TunServiceOptions -> TunServiceOptionsScreen()
        Route.EbpfServiceOptions -> EbpfServiceOptionsScreen()
        Route.AccessControl -> AccessControlScreen(navigator)
        Route.AccessControlAppList -> AccessControlAppListScreen()
        Route.MetaFeature -> MetaFeatureScreen(navigator)
        Route.Connection -> ConnectionScreen(navigator)
        is Route.ConnectionDetail ->
            ConnectionDetailScreen(
                navigator = navigator,
                connectionId = route.connectionId,
                canInterrupt = route.canInterrupt,
            )
        Route.TrafficStatistics -> TrafficStatisticsScreen()
        Route.Log -> LogScreen(navigator)
        Route.About -> AboutScreen(navigator)
        Route.OpenSourceLicenses -> OpenSourceLicensesScreen(navigator)
        Route.Override -> OverrideScreen(navigator)
        is Route.OverrideConfigPreview -> OverrideConfigPreviewRoute(navigator, route.previewId)
        is Route.ProfileEdit -> ProfileEditScreen(navigator, route.profileId)
        is Route.ProfileOverride -> ProfileOverrideScreen(navigator, route.profileId)
        is Route.RulesAndProviders -> RulesAndProvidersScreen(navigator, route.tab)
        Route.Feature -> FeatureScreen(navigator)
        Route.Extensions -> ExtensionsScreen()
        Route.BackupRestore -> BackupRestoreScreen()
        Route.CustomRouting -> CustomRoutingRoute(navigator)
    }
}
