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

@file:Suppress("FunctionName")

package com.github.yumeyucca.yumebox.screen.settings


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.github.yumeyucca.yumebox.BuildInfo
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.component.AppCard
import com.github.yumeyucca.yumebox.presentation.navigation.Route
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import io.github.yumeyucca.lucide.Lucide
import io.github.yumeyucca.lucide.lucide.Atom
import io.github.yumeyucca.lucide.lucide.ChartColumn
import io.github.yumeyucca.lucide.lucide.FlaskConical
import io.github.yumeyucca.lucide.lucide.GitMerge
import io.github.yumeyucca.lucide.lucide.Info
import io.github.yumeyucca.lucide.lucide.ListChecks
import io.github.yumeyucca.lucide.lucide.Network
import io.github.yumeyucca.lucide.lucide.PackageCheck
import io.github.yumeyucca.lucide.lucide.ScanEye
import io.github.yumeyucca.lucide.lucide.ScrollText
import io.github.yumeyucca.lucide.lucide.Settings2
import io.github.yumeyucca.lucide.lucide.WifiCog
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SettingPager(
    mainInnerPadding: PaddingValues,
    @Suppress("UNUSED_PARAMETER") windowLayoutMode: WindowLayoutMode = WindowLayoutMode.Compact,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val rootNavigator = LocalNavigator.current
    val detailNavigator = LocalDetailNavigator.current
    val versionInfo = "v${BuildInfo.BASE_VERSION}"

    // Phone: push on root navigator. Tablet shell: replace right pane.
    // No persistent selected highlight on the list — opens should not leave a tint.
    val onOpen: (Route) -> Unit = { route ->
        if (detailNavigator != null) {
            detailNavigator.replaceAll(listOf(route))
        } else {
            rootNavigator.push(route)
        }
    }

    GlassScaffold(
        topBar = { TopBar(title = YumeTxt.Settings.Title, scrollBehavior = scrollBehavior) }
    ) { innerPadding ->
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = combinePaddingValues(innerPadding, mainInnerPadding),
        ) {
            item {
                Title(YumeTxt.Settings.Section.More)
                AppCard {
                    SettingsNavItem(
                        icon = Lucide.Network,
                        title = YumeTxt.Connection.Title,
                        onClick = { onOpen(Route.Connection) },
                    )
                    SettingsNavItem(
                        icon = Lucide.ChartColumn,
                        title = YumeTxt.TrafficStatistics.Title,
                        onClick = { onOpen(Route.TrafficStatistics) },
                        showDivider = true,
                    )
                    SettingsNavItem(
                        icon = Lucide.ScrollText,
                        title = YumeTxt.Log.Title,
                        onClick = { onOpen(Route.Log) },
                        showDivider = true,
                    )
                    SettingsNavItem(
                        icon = Lucide.ListChecks,
                        title = YumeTxt.Rules.Title,
                        onClick = { onOpen(Route.Rules) },
                        showDivider = true,
                    )
                    SettingsNavItem(
                        icon = Lucide.PackageCheck,
                        title = YumeTxt.Providers.Title,
                        onClick = { onOpen(Route.Providers) },
                        showDivider = true,
                    )
                }
            }
            item {
                Title(YumeTxt.Settings.Section.Settings)
                AppCard {
                    SettingsNavItem(
                        icon = Lucide.Settings2,
                        title = YumeTxt.Settings.UiSettings.App,
                        onClick = { onOpen(Route.AppSettings) },
                    )
                    SettingsNavItem(
                        icon = Lucide.WifiCog,
                        title = YumeTxt.Settings.UiSettings.Network,
                        onClick = { onOpen(Route.NetworkSettings) },
                        showDivider = true,
                    )
                    SettingsNavItem(
                        icon = Lucide.ScanEye,
                        title = YumeTxt.AccessControl.Title,
                        onClick = { onOpen(Route.AccessControl) },
                        showDivider = true,
                    )
                    SettingsNavItem(
                        icon = Lucide.GitMerge,
                        title = YumeTxt.Settings.UiSettings.Override,
                        onClick = { onOpen(Route.Override) },
                        showDivider = true,
                    )
                    SettingsNavItem(
                        icon = Lucide.Atom,
                        title = YumeTxt.Settings.UiSettings.MetaFeatures,
                        onClick = { onOpen(Route.MetaFeature) },
                        showDivider = true,
                    )
                    SettingsNavItem(
                        icon = Lucide.FlaskConical,
                        title = YumeTxt.Settings.More.Lab,
                        onClick = { onOpen(Route.Feature) },
                        showDivider = true,
                    )
                }
            }
            item {
                Title(YumeTxt.Settings.Section.About)
                AppCard {
                    SettingsNavItem(
                        icon = Lucide.Info,
                        title = YumeTxt.Settings.More.About,
                        onClick = { onOpen(Route.About) },
                        endContent = { VersionBadge(versionInfo) },
                    )
                }
            }
        }
    }
}

@Composable
private fun VersionBadge(versionInfo: String) {
    val spacing = AppTheme.spacing
    val componentSizes = AppTheme.sizes
    val opacity = AppTheme.opacity

    Surface(
        color = MiuixTheme.colorScheme.primary.copy(alpha = opacity.subtle),
        shape = RoundedCornerShape(50),
        modifier =
            Modifier
                .height(componentSizes.versionBadgeHeight),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = spacing.space12),
            horizontalArrangement = Arrangement.spacedBy(spacing.space8),
        ) {
            Text(
                text = versionInfo,
                style =
                    MiuixTheme.textStyles.footnote1.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                color = MiuixTheme.colorScheme.primary,
            )
        }
    }
}
