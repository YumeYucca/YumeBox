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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Sheet surface sampled by [SheetHeaderIconButton] so its glass matches the node page chrome. */
private val LocalSheetHeaderBackdrop = compositionLocalOf<LayerBackdrop?> { null }

object FloatingSheetHeaderDefaults {

    /**
     * Fixed title band height. The band is a constant, so the title keeps one spot whether zero,
     * one or both side actions render — the reference sheet's "固定高度使有无操作按钮时标题都垂直居中".
     */
    val Height = 76.dp

    /**
     * Chrome the miuix sheet lays out above its content: the 24dp drag handle plus the empty title
     * row (6dp + 12dp) it still renders when `title` is null. The band is offset up by exactly this
     * much so it starts at the card's top edge, with the handle floating over its upper part.
     */
    val ChromeAboveContent = 42.dp

    /** Top spacer for content that scrolls under the band. */
    val ContentTopInset = Height - ChromeAboveContent

    /**
     * Inset of a side action from the card edge. Only the outer side is inset (the reference pads
     * `start = 18.dp` on the leading button and `end = 18.dp` on the trailing one).
     */
    val ActionInset = 18.dp
}

/**
 * Fixed-height sheet title band, laid over the sheet content: the top bar's progressive band
 * with a 76dp row on top of it — centred title, side actions at the two edges, all vertically
 * centred.
 *
 * Host it as the last child of the sheet content Box, and give scrolling content a
 * [FloatingSheetHeaderDefaults.ContentTopInset] top spacer. The band is drawn from the card's top
 * edge (`offset(-ChromeAboveContent)`), so the title sits 38dp below it and the drag handle floats
 * over the band's upper area.
 */
@Composable
fun BoxScope.FloatingSheetHeader(
    title: String,
    sheetColor: Color,
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop? = null,
    showTopFade: Boolean = true,
    startAction: (@Composable () -> Unit)? = null,
    endAction: (@Composable () -> Unit)? = null,
) {
    // Reuse the sheet content's own recording when the host provides one; otherwise fall back to
    // the sheet surface so the action chrome still has a material to sample.
    val fallbackBackdrop = rememberLayerBackdrop { drawRect(sheetColor) }
    val headerBackdrop = backdrop ?: fallbackBackdrop
    CompositionLocalProvider(LocalSheetHeaderBackdrop provides headerBackdrop) {
        if (showTopFade) {
            Box(
                modifier =
                    modifier
                        .fillMaxWidth()
                        .height(FloatingSheetHeaderDefaults.Height)
                        .offset(y = -FloatingSheetHeaderDefaults.ChromeAboveContent)
                        .zIndex(1f)
                        .then(if (backdrop == null) Modifier.layerBackdrop(fallbackBackdrop) else Modifier)
                        .background(GlassTopAppBarDefaults.bandBrush(sheetColor)),
            )
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(FloatingSheetHeaderDefaults.Height)
                    .offset(y = -FloatingSheetHeaderDefaults.ChromeAboveContent)
                    .zIndex(2f),
        ) {
            Text(
                text = title,
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        // Keep long group names from running under the side actions.
                        .padding(horizontal = UiDp.dp56),
                fontSize = MiuixTheme.textStyles.title4.fontSize,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (startAction != null) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = FloatingSheetHeaderDefaults.ActionInset),
                ) {
                    startAction()
                }
            }
            if (endAction != null) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = FloatingSheetHeaderDefaults.ActionInset),
                ) {
                    endAction()
                }
            }
        }
    }
}

/**
 * A sheet side action: exactly the chrome the node page uses in its top-left / top-right slots —
 * the 42dp glass circle with the edge-light rim and 22dp icon, sampling the sheet surface through
 * [LocalSheetHeaderBackdrop], so a sheet's actions and the page's top bar read as the same control.
 */
@Composable
fun SheetHeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = Color.Unspecified,
    iconModifier: Modifier = Modifier,
) {
    // A disabled action keeps its chrome but stops accepting taps; the glass button has no enabled
    // flag of its own.
    val performClick: () -> Unit = if (enabled) onClick else NoOpClick
    GlassTopIconButton(
        action =
            GlassTopAction(
                icon = icon,
                contentDescription = contentDescription,
                onClick = performClick,
                iconModifier =
                    if (enabled) {
                        iconModifier
                    } else {
                        iconModifier.alpha(AppTheme.opacity.disabled)
                    },
                tint = tint,
            ),
        backdrop = LocalSheetHeaderBackdrop.current,
    )
}

/** Tap sink for a disabled action, kept allocation-free across recompositions. */
private val NoOpClick: () -> Unit = {}
