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

import kotlinx.serialization.Serializable

/**
 * Navigation keys for the whole app, replacing compose-destinations' generated `*Destination`
 * objects. The arg-carrying screens are data classes, the rest are objects.
 */
sealed interface Route {
    @Serializable
    data class Main(val initialPage: Int = 0) : Route

    @Serializable
    data class MoeWallpaperCrop(
        val wallpaperUri: String,
        val initialZoom: Float = 1f,
        val initialBiasX: Float = 0f,
        val initialBiasY: Float = 0f,
    ) : Route

    @Serializable
    data object AppSettings : Route

    @Serializable
    data object NotificationSettings : Route

    /** Settings master list (SettingPager) hosted as a standalone pushed page. */
    @Serializable
    data object Settings : Route

    @Serializable
    data object NetworkSettings : Route

    @Serializable
    data object VpnServiceOptions : Route

    @Serializable
    data object TunServiceOptions : Route

    data object EbpfServiceOptions : Route

    @Serializable
    data object AccessControl : Route

    @Serializable
    data object AccessControlAppList : Route

    @Serializable
    data object MetaFeature : Route

    @Serializable
    data object Connection : Route

    @Serializable
    data class ConnectionDetail(val connectionId: String) : Route

    @Serializable
    data object TrafficStatistics : Route

    @Serializable
    data object Log : Route

    /** Runtime rules from GET /rules (not custom routing). */
    @Serializable
    data object Rules : Route

    @Serializable
    data object About : Route

    @Serializable
    data object OpenSourceLicenses : Route

    @Serializable
    data object Override : Route

    @Serializable
    data object OverrideConfigPreview : Route

    /** "编辑配置" page for one profile: profile fields and config-file facts. */
    @Serializable
    data class ProfileEdit(val profileId: String) : Route

    /** "覆写" page for one profile: custom routing and override selection. */
    @Serializable
    data class ProfileOverride(val profileId: String) : Route

    @Serializable
    data object Providers : Route

    @Serializable
    data object Feature : Route

    @Serializable
    data object Extensions : Route

    @Serializable
    data object BackupRestore : Route

    @Serializable
    data object CustomRouting : Route

    @Serializable
    data object StringListEditor : Route

    @Serializable
    data object KeyValueEditor : Route
}
