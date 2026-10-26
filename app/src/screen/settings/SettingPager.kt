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


import android.annotation.SuppressLint
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.github.yumeyucca.yumebox.BuildInfo
import com.github.yumeyucca.yumebox.WebViewActivity
import com.github.yumeyucca.yumebox.common.util.toast
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.*
import com.github.yumeyucca.yumebox.presentation.navigation.Route
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.presentation.viewmodel.SettingEvent
import com.github.yumeyucca.yumebox.presentation.viewmodel.SettingViewModel
import io.github.yumeyucca.lucide.Lucide
import io.github.yumeyucca.lucide.lucide.Archive
import io.github.yumeyucca.lucide.lucide.FileBracesCorner
import io.github.yumeyucca.lucide.lucide.ShieldCheck
import io.github.yumeyucca.lucide.lucide.WifiPen
import org.koin.androidx.compose.koinViewModel
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@SuppressLint("LocalContextResourcesRead")
@Composable
fun SettingPager(
    mainInnerPadding: PaddingValues,
    @Suppress("UNUSED_PARAMETER") windowLayoutMode: WindowLayoutMode = WindowLayoutMode.Compact,
) {
    val viewModel = koinViewModel<SettingViewModel>()
    val scrollBehavior = MiuixScrollBehavior()
    val rootNavigator = LocalNavigator.current
    val detailNavigator = LocalDetailNavigator.current
    val context = LocalContext.current
    val versionInfo = "v${BuildInfo.BASE_VERSION}"

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingEvent.OpenWebView -> {
                    runCatching { WebViewActivity.start(context, event.url) }
                        .getOrElse { throwable ->
                            context.toast(
                                YumeTxt.Settings.Error.WebviewFailed.format(throwable.message)
                            )
                        }
                }
            }
        }
    }

    // Phone: push on root navigator. Tablet shell: replace right pane.
    // No persistent selected highlight on the list — opens should not leave a tint.
    val openRoot: (Route) -> Unit = { route ->
        if (detailNavigator != null) {
            detailNavigator.replaceAll(listOf(route))
        } else {
            rootNavigator.push(route)
        }
    }

    SettingsMasterList(
        mainInnerPadding = mainInnerPadding,
        scrollBehavior = scrollBehavior,
        versionInfo = versionInfo,
        onOpen = openRoot,
    )
}

@Composable
private fun SettingsMasterList(
    mainInnerPadding: PaddingValues,
    scrollBehavior: top.yukonga.miuix.kmp.basic.ScrollBehavior,
    versionInfo: String,
    onOpen: (Route) -> Unit,
) {
    Scaffold(
        topBar = { SmallTopBar(title = YumeTxt.Settings.Title, scrollBehavior = scrollBehavior) }
    ) { innerPadding ->
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = combinePaddingValues(innerPadding, mainInnerPadding),
        ) {
            item {
                Title(YumeTxt.Settings.Section.UiSettings, topPadding = UiDp.dp0)
                SettingsSegmentedColumn(
                    modifier =
                        Modifier.padding(
                            start = UiDp.dp16,
                            end = UiDp.dp16,
                            bottom = UiDp.dp13,
                        ),
                    content =
                        listOf(
                            {
                                SettingsListItem(
                                    title = YumeTxt.Settings.UiSettings.AppAppearance,
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
                    modifier =
                        Modifier.padding(
                            start = UiDp.dp16,
                            end = UiDp.dp16,
                            bottom = UiDp.dp13,
                        ),
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
                    modifier =
                        Modifier.padding(
                            start = UiDp.dp16,
                            end = UiDp.dp16,
                            bottom = UiDp.dp13,
                        ),
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
                    modifier = Modifier.padding(start = UiDp.dp16, end = UiDp.dp16),
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
