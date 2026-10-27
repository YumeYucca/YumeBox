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

package com.github.yumeyucca.yumebox.presentation.component


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.yumeyucca.yumebox.presentation.theme.LocalTopBarBackdrop
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TopAppBarDefaults
import top.yukonga.miuix.kmp.glass.GlassTopAppBar
import top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Route top bar, built the way the miuix glass demo (`GlassPage.kt`) builds its bar:
 * [GlassTopAppBar] over the page's own colour band, with the large title collapsing into the
 * centred compact title as content scrolls under it.
 *
 * The band is [GlassTopAppBarDefaults.bandBrush] — solid where it meets the status bar, fading out
 * past the bar's bottom edge by [GlassTopAppBarDefaults.BandOverhang] — and only shows once page
 * content has slid under the bar, so a page at rest carries no chrome. Controls sit at the demo's
 * 12dp page inset (the same inset page content uses); glass buttons in the slots inherit the bar's
 * material, so hosts that provide [LocalTopBarBackdrop] (see `GlassScaffold`) get frosted chrome
 * and hosts without it get the solid fallback.
 */
@Composable
fun TopBar(
    title: String,
    scrollBehavior: ScrollBehavior,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    bottomContent: @Composable () -> Unit = {},
) {
    GlassTopAppBar(
        title = title,
        modifier = modifier,
        backdrop = LocalTopBarBackdrop.current,
        scrollBehavior = scrollBehavior,
        bandBrush = GlassTopAppBarDefaults.bandBrush(MiuixTheme.colorScheme.surface),
        navigationIcon = navigationIcon,
        actions = actions,
        bottomContent = bottomContent,
    )
}

/**
 * Compact top bar (no collapsing large title) carrying the same demo band as [TopBar]: miuix's
 * `SmallTopAppBar` for the layout, the band drawn behind it with the same overhang, so a page that
 * mixes both bars keeps one continuous chrome.
 */
@Composable
fun SmallTopBar(
    title: String,
    scrollBehavior: ScrollBehavior,
    modifier: Modifier = Modifier,
    titleColor: Color = MiuixTheme.colorScheme.onSurface,
    titlePadding: Dp = TopAppBarDefaults.TitlePadding,
    navigationIconPadding: Dp = UiDp.dp24,
    actionIconPadding: Dp = UiDp.dp24,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    bottomContent: @Composable () -> Unit = {},
) {
    val density = LocalDensity.current
    val overhangPx = with(density) { GlassTopAppBarDefaults.BandOverhang.toPx() }
    val rampPx = with(density) { TopBarBandRamp.toPx() }
    // The demo's signal: content offset rather than the collapse state, so a page that has not
    // scrolled stays chrome-free and the band returns when content comes back under the bar.
    val bandAlpha by
        remember(scrollBehavior, rampPx) {
            derivedStateOf { (-scrollBehavior.state.contentOffset / rampPx).coerceIn(0f, 1f) }
        }
    Box(modifier = modifier) {
        TopBarBand(alpha = bandAlpha, overhangPx = overhangPx)
        SmallTopAppBar(
            title = title,
            color = Color.Transparent,
            titleColor = titleColor,
            titlePadding = titlePadding,
            navigationIconPadding = navigationIconPadding,
            actionIconPadding = actionIconPadding,
            navigationIcon = navigationIcon,
            actions = actions,
            scrollBehavior = scrollBehavior,
            bottomContent = bottomContent,
        )
    }
}

/** Scroll distance over which the top bar band fades in, matching the demo's progressive ramp. */
private val TopBarBandRamp = 48.dp

/**
 * The demo's band: the page colour, solid down to its halfway point and gone by the bar's bottom
 * edge, stretched [overhangPx] beyond the bar so scrolled content dissolves before it reaches the
 * title instead of ending on a hard line.
 */
@Composable
internal fun BoxScope.TopBarBand(alpha: Float, overhangPx: Float) {
    Box(
        modifier =
            Modifier
                .matchParentSize()
                .graphicsLayer {
                    this.alpha = alpha
                    transformOrigin = TransformOrigin(0.5f, 0f)
                    scaleY = if (size.height > 0f) (size.height + overhangPx) / size.height else 1f
                }
                .background(GlassTopAppBarDefaults.bandBrush(MiuixTheme.colorScheme.surface)),
    )
}
