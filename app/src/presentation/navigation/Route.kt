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

import com.github.yumeyucca.yumebox.screen.rules.RulesProvidersTab

/**
 * Navigation keys for the whole app, replacing compose-destinations' generated `*Destination`
 * objects. The arg-carrying screens are data classes, the rest are objects.
 *
 * Routes are never serialised — the shell keeps them in a retained component and builds the stack
 * with `serializer = null` — so they may carry arguments a serialiser would reject.
 */
sealed interface Route {
    data object Main : Route

    data class MoeWallpaperCrop(
        val wallpaperUri: String,
        val initialZoom: Float = 1f,
        val initialBiasX: Float = 0f,
        val initialBiasY: Float = 0f,
    ) : Route

    data object AppSettings : Route

    data object NotificationSettings : Route

    /** Settings master list (SettingPager) hosted as a standalone pushed page. */
    data object Settings : Route

    data object NetworkSettings : Route

    data object VpnServiceOptions : Route

    data object TunServiceOptions : Route

    data object AccessControl : Route

    data object AccessControlAppList : Route

    data object MetaFeature : Route

    data object Connection : Route

    data class ConnectionDetail(
        val connectionId: String,
        /** Whether the row came from the active list, i.e. whether it can still be closed. */
        val canInterrupt: Boolean = false,
    ) : Route

    data object TrafficStatistics : Route

    data object Log : Route

    /** Routing rules and outbound providers: runtime rules from GET /rules (not custom routing). */
    data class RulesAndProviders(val tab: RulesProvidersTab = RulesProvidersTab.Rules) : Route

    data object About : Route

    data object OpenSourceLicenses : Route

    data object Override : Route

    /**
     * The config editor, showing the handoff [previewId] from the stack's config-preview slot.
     *
     * The id is what makes two consecutive openings two different routes: as a singleton route the
     * second opening was indistinguishable from the first and could be dropped, leaving the editor
     * unopenable until its page was left and re-entered.
     */
    data class OverrideConfigPreview(val previewId: Long) : Route

    /** "编辑配置" page for one profile: profile fields and config-file facts. */
    data class ProfileEdit(val profileId: String) : Route

    /** "覆写" page for one profile: custom routing and override selection. */
    data class ProfileOverride(val profileId: String) : Route

    data object Feature : Route

    data object Extensions : Route

    data object BackupRestore : Route

    data object CustomRouting : Route
}
