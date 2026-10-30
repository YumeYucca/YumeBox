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

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.yumeyucca.yumebox.BuildInfo
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.*
import com.github.yumeyucca.yumebox.presentation.navigation.Route
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import io.github.yumeyucca.lucide.Lucide
import io.github.yumeyucca.lucide.lucide.Archive
import io.github.yumeyucca.lucide.lucide.Feather
import io.github.yumeyucca.lucide.lucide.FileBracesCorner
import io.github.yumeyucca.lucide.lucide.ShieldCheck
import io.github.yumeyucca.lucide.lucide.WifiPen
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Gutter of every master-list section; the last section drops the bottom gap. */
private val SettingsSectionModifier =
    Modifier.padding(
        start = UiDp.dp16,
        end = UiDp.dp16,
        bottom = UiDp.dp13,
    )
private val SettingsSectionModifierFlush = Modifier.padding(start = UiDp.dp16, end = UiDp.dp16)

@Composable
fun SettingPager() {
    val scrollBehavior = MiuixScrollBehavior()
    val rootNavigator = LocalNavigator.current
    val detailNavigator = LocalDetailNavigator.current
    val versionInfo = "v${BuildInfo.BASE_VERSION}"

    // No persistent selected highlight on the list — opens should not leave a tint.
    val targetNavigator = detailNavigator ?: rootNavigator
    val openRoot: (Route) -> Unit = { route -> targetNavigator.push(route) }

    SettingsMasterList(
        scrollBehavior = scrollBehavior,
        versionInfo = versionInfo,
        onOpen = openRoot,
    )
}

@Composable
private fun SettingsMasterList(
    scrollBehavior: ScrollBehavior,
    versionInfo: String,
    onOpen: (Route) -> Unit,
) {
    Scaffold(
        topBar = { SmallTopBar(title = YumeTxt.Settings.Title, scrollBehavior = scrollBehavior) }
    ) { innerPadding ->
        StandalonePageList(
            scrollBehavior = scrollBehavior,
            scaffoldPadding = innerPadding,
        ) {
            item {
                Title(YumeTxt.Settings.Section.UiSettings, topPadding = UiDp.dp0)
                SettingsSegmentedColumn(
                    modifier = SettingsSectionModifier,
                    content =
                        listOf(
                            {
                                SettingsListItem(
                                    title = YumeTxt.Settings.UiSettings.AppSettingsEntry,
                                    onClick = { onOpen(Route.AppSettings) },
                                    icon = Yume.Palette,
                                )
                            },
                            {
                                SettingsListItem(
                                    title = YumeTxt.Settings.UiSettings.Network,
                                    onClick = { onOpen(Route.NetworkSettings) },
                                    icon = Lucide.WifiPen,
                                )
                            },
                            {
                                SettingsListItem(
                                    title = YumeTxt.AccessControl.Title,
                                    onClick = { onOpen(Route.AccessControl) },
                                    icon = Lucide.ShieldCheck,
                                )
                            },
                        )
                )
            }
            item {
                Title(YumeTxt.Settings.Section.Extensions)
                SettingsSegmentedColumn(
                    modifier = SettingsSectionModifier,
                    content =
                        listOf(
                            {
                                SettingsListItem(
                                    title = YumeTxt.Settings.UiSettings.OverrideScript,
                                    onClick = { onOpen(Route.Override) },
                                    icon = Lucide.FileBracesCorner,
                                )
                            },
                            {
                                SettingsListItem(
                                    title = YumeTxt.Settings.UiSettings.MetaFeatures,
                                    onClick = { onOpen(Route.MetaFeature) },
                                    icon = Lucide.Feather,
                                )
                            },
                            {
                                SettingsListItem(
                                    title = YumeTxt.Settings.UiSettings.RemoteControl,
                                    onClick = { onOpen(Route.Extensions) },
                                    icon = Yume.CPU,
                                )
                            },
                        )
                )
            }
            item {
                Title(YumeTxt.Settings.Section.More)
                SettingsSegmentedColumn(
                    modifier = SettingsSectionModifier,
                    content =
                        listOf(
                            {
                                SettingsListItem(
                                    title = YumeTxt.Settings.More.BackupRestore,
                                    onClick = { onOpen(Route.BackupRestore) },
                                    icon = Lucide.Archive,
                                )
                            },
                            {
                                SettingsListItem(
                                    title = YumeTxt.Settings.More.Lab,
                                    onClick = { onOpen(Route.Feature) },
                                    icon = Yume.FlaskConical,
                                )
                            },
                        )
                )
            }
            item {
                Title(YumeTxt.Settings.Section.About)
                SettingsSegmentedColumn(
                    modifier = SettingsSectionModifierFlush,
                    content =
                        listOf {
                            SettingsListItem(
                                title = YumeTxt.Settings.More.About,
                                onClick = { onOpen(Route.About) },
                                icon = Yume.Github,
                                trailingContent = {
                                    Text(
                                        text = versionInfo,
                                        style = MiuixTheme.textStyles.body2,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                    )
                                },
                            )
                        },
                )
            }
        }
    }
}
