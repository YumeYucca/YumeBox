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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.github.yumeyucca.yumebox.BuildConfig
import com.github.yumeyucca.yumebox.WebViewActivity
import com.github.yumeyucca.yumebox.common.util.toast
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.component.AppCard
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.*
import com.github.yumeyucca.yumebox.presentation.navigation.Route
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.viewmodel.SettingEvent
import com.github.yumeyucca.yumebox.presentation.viewmodel.SettingViewModel
import org.koin.androidx.compose.koinViewModel
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
private fun CircularIcon(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    iconSize: Float = 1f,
) {
    val spacing = AppTheme.spacing
    val radii = AppTheme.radii
    val componentSizes = AppTheme.sizes

    Box(
        modifier =
            modifier
                .padding(start = spacing.space4, end = spacing.space16)
                .requiredSize(componentSizes.settingsIconSlotSize),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .layout { measurable, _ ->
                        val containerSize = componentSizes.settingsIconContainerSize.roundToPx()
                        val parentSize = componentSizes.settingsIconSlotSize.roundToPx()
                        val offset = (containerSize - parentSize) / 2
                        val placeable =
                            measurable.measure(
                                androidx.compose.ui.unit.Constraints.fixed(
                                    containerSize,
                                    containerSize,
                                )
                            )
                        layout(parentSize, parentSize) { placeable.place(-offset, -offset) }
                    }
                    .size(componentSizes.settingsIconContainerSize)
                    .clip(RoundedCornerShape(radii.radius16))
                    .background(MiuixTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = MiuixTheme.colorScheme.onPrimary,
                modifier =
                    Modifier
                        .size(componentSizes.settingsIconGlyphSize)
                        .graphicsLayer(
                            scaleX = iconSize,
                            scaleY = iconSize,
                            transformOrigin = TransformOrigin.Center,
                        ),
            )
        }
    }
}

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
    val versionInfo = "v${BuildConfig.BASE_VERSION}"

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
        topBar = { TopBar(title = YumeTxt.Settings.Title, scrollBehavior = scrollBehavior) }
    ) { innerPadding ->
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = combinePaddingValues(innerPadding, mainInnerPadding),
        ) {
            item {
                Title(YumeTxt.Settings.Section.UiSettings)
                AppCard {
                    SettingsRootPreference(
                        title = YumeTxt.Settings.UiSettings.App,
                        summary = YumeTxt.Settings.UiSettings.AppSummary,
                        onClick = { onOpen(Route.AppSettings) },
                        icon = Yume.Settings2,
                    )
                    SettingsRootPreference(
                        title = YumeTxt.Settings.UiSettings.Network,
                        summary = YumeTxt.Settings.UiSettings.NetworkSummary,
                        onClick = { onOpen(Route.NetworkSettings) },
                        icon = Yume.WifiCog,
                    )
                    SettingsRootPreference(
                        title = YumeTxt.Settings.UiSettings.Override,
                        summary = YumeTxt.Settings.UiSettings.OverrideSummary,
                        onClick = { onOpen(Route.Override) },
                        icon = Yume.GitMerge,
                    )
                    SettingsRootPreference(
                        title = YumeTxt.Settings.UiSettings.MetaFeatures,
                        summary = YumeTxt.Settings.UiSettings.MetaFeaturesSummary,
                        onClick = { onOpen(Route.MetaFeature) },
                        icon = Yume.Meta,
                    )
                }
            }
            item {
                Title(YumeTxt.Settings.Section.More)
                AppCard {
                    SettingsRootPreference(
                        title = YumeTxt.Settings.More.Lab,
                        summary = YumeTxt.Settings.More.LabSummary,
                        onClick = { onOpen(Route.Feature) },
                        icon = Yume.FlaskConical,
                    )
                    ArrowPreference(
                        title = YumeTxt.Settings.More.About,
                        summary = YumeTxt.Settings.More.AboutSummary,
                        onClick = { onOpen(Route.About) },
                        startAction = {
                            CircularIcon(imageVector = Yume.Github, contentDescription = null)
                        },
                        endActions = { VersionBadge(versionInfo) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsRootPreference(
    title: String,
    summary: String,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    ArrowPreference(
        title = title,
        summary = summary,
        onClick = onClick,
        startAction = { CircularIcon(imageVector = icon, contentDescription = null) },
    )
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
                .height(componentSizes.versionBadgeHeight)
                .padding(end = spacing.space12),
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
