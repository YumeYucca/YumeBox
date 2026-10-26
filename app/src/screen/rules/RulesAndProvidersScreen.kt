/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
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

package com.github.yumeyucca.yumebox.screen.rules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import com.github.yumeyucca.yumebox.presentation.component.GlassScaffold
import com.github.yumeyucca.yumebox.presentation.component.Navigator
import com.github.yumeyucca.yumebox.presentation.component.SegmentedTabRow
import com.github.yumeyucca.yumebox.presentation.component.TopBar
import com.github.yumeyucca.yumebox.presentation.component.combinePaddingValues
import com.github.yumeyucca.yumebox.presentation.component.rememberStandalonePageMainPadding
import com.github.yumeyucca.yumebox.presentation.theme.LocalTopBarBackdrop
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.screen.navigation.ProvidersScreen
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val TabBandInset = 66.dp

@Composable
fun RulesAndProvidersScreen(
    navigator: Navigator,
    initialTab: Int = 0,
) {
    val scrollBehavior = MiuixScrollBehavior()
    var selectedTab by rememberSaveable(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 1)) }
    val tabs = listOf(YumeTxt.Rules.Title, YumeTxt.Providers.Title)
    val rulesListState = rememberLazyListState()
    val providersListState = rememberLazyListState()
    val activeListState = if (selectedTab == 0) rulesListState else providersListState

    GlassScaffold(
        topBar = {
            TopBar(
                title = YumeTxt.Providers.TabTitle,
                scrollBehavior = scrollBehavior,
            )
        },
        contentOverlay = { scaffoldPadding ->
            val density = LocalDensity.current
            val tabRestTopPx =
                with(density) { (scaffoldPadding.calculateTopPadding() + UiDp.dp20).toPx() }
            val tabZonePx = with(density) { UiDp.dp46.toPx() }
            val tabScrollPx by remember(activeListState, tabRestTopPx) {
                derivedStateOf {
                    if (activeListState.firstVisibleItemIndex == 0) {
                        activeListState.firstVisibleItemScrollOffset.toFloat()
                    } else {
                        tabRestTopPx + tabZonePx
                    }
                }
            }
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .graphicsLayer { translationY = tabRestTopPx - tabScrollPx }
                        .background(MiuixTheme.colorScheme.surface)
                        .padding(
                            start = UiDp.dp12,
                            end = UiDp.dp12,
                            bottom = UiDp.dp4,
                        ),
            ) {
                SegmentedTabRow(
                    tabs = tabs,
                    selectedTabIndex = selectedTab,
                    onTabSelected = { selectedTab = it },
                    tabHeight = UiDp.dp42,
                    backdrop = LocalTopBarBackdrop.current,
                )
            }
        },
    ) { innerPadding ->
        val basePadding =
            combinePaddingValues(innerPadding, rememberStandalonePageMainPadding())
        val contentPadding =
            PaddingValues(
                top = basePadding.calculateTopPadding() + TabBandInset,
                bottom = basePadding.calculateBottomPadding(),
                start = basePadding.calculateStartPadding(LayoutDirection.Ltr),
                end = basePadding.calculateEndPadding(LayoutDirection.Ltr),
            )
        when (selectedTab) {
            0 -> RulesScreen(
                navigator = navigator,
                embedded = true,
                contentPadding = contentPadding,
                scrollBehaviorOverride = scrollBehavior,
                listState = rulesListState,
            )

            else -> ProvidersScreen(
                navigator = navigator,
                embedded = true,
                contentPadding = contentPadding,
                scrollBehaviorOverride = scrollBehavior,
                listState = providersListState,
            )
        }
    }
}
