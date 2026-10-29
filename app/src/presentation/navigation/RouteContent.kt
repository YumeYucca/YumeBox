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

/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 */

package com.github.yumeyucca.yumebox.presentation.navigation

import androidx.compose.runtime.Composable
import com.github.yumeyucca.yumebox.presentation.component.Navigator
import com.github.yumeyucca.yumebox.presentation.component.rememberStandalonePageMainPadding
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
fun RouteContent(route: Route, navigator: Navigator) {
    when (route) {
        Route.Settings -> SettingPager(mainInnerPadding = rememberStandalonePageMainPadding())
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
        is Route.ConnectionDetail -> ConnectionDetailScreen(navigator, route.connectionId)
        Route.TrafficStatistics -> TrafficStatisticsScreen()
        Route.Log -> LogScreen(navigator)
        Route.Rules -> RulesAndProvidersScreen(navigator, initialTab = 0)
        Route.About -> AboutScreen(navigator)
        Route.OpenSourceLicenses -> OpenSourceLicensesScreen(navigator)
        Route.Override -> OverrideScreen(navigator)
        Route.OverrideConfigPreview -> OverrideConfigPreviewRoute(navigator)
        is Route.ProfileEdit -> ProfileEditScreen(navigator, route.profileId)
        is Route.ProfileOverride -> ProfileOverrideScreen(navigator, route.profileId)
        Route.Providers -> RulesAndProvidersScreen(navigator, initialTab = 1)
        Route.Feature -> FeatureScreen(navigator)
        Route.Extensions -> ExtensionsScreen()
        Route.BackupRestore -> BackupRestoreScreen()
        Route.CustomRouting -> CustomRoutingRoute(navigator)
        Route.StringListEditor -> StringListEditorScreen(navigator)
        Route.KeyValueEditor -> KeyValueEditorScreen(navigator)
        is Route.Main, is Route.MoeWallpaperCrop -> Unit
    }
}

val SettingsDetailRootRoutes: Set<Route> =
    setOf(Route.AppSettings, Route.NotificationSettings, Route.NetworkSettings, Route.Override, Route.MetaFeature, Route.Feature, Route.Extensions, Route.BackupRestore, Route.About)
