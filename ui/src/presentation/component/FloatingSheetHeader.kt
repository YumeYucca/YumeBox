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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.presentation.theme.YumeBlur
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

object FloatingSheetHeaderDefaults {

    /**
     * NexioSchedule BlurBottomSheet header band: one fixed 76dp row shared by the persistent top
     * fade mask and the title/actions, so the title stays vertically centered at the same spot
     * whether zero, one or both actions render.
     */
    val Height = 76.dp

    /**
     * Upstream miuix 0.9.4 always renders its TitleAndActionsRow paddings (6dp + 12dp) even with
     * a null title and no actions. Sheets render their own floating header instead and absorb
     * that empty strip by offsetting the header up by exactly this amount, so the 76dp band
     * starts right under the 24dp drag handle.
     */
    val LibraryEmptyTitleRowHeight = 18.dp

    /**
     * Top spacer for content that scrolls under the floating header: the header band (76dp)
     * minus the absorbed empty title row (18dp).
     */
    val ContentTopInset = Height - LibraryEmptyTitleRowHeight
}

/**
 * NexioSchedule BlurBottomSheet sheet chrome, laid over the sheet content: a persistent 76dp
 * top-fade gradient (the sheet's own color fading to transparent, always on) with a fixed 76dp
 * header row on top of it — centered title, start/end actions at the sides, all vertically
 * centered. Content scrolls under the band and sinks into the gradient.
 *
 * Host it as the last child of the sheet content Box, and give the scrolling content a
 * [FloatingSheetHeaderDefaults.ContentTopInset] top spacer/content padding. Glass action
 * buttons that sample a [top.yukonga.miuix.kmp.blur.LayerBackdrop] must stay OUTSIDE the
 * captured subtree — host this header as a sibling of the layerBackdrop box, like
 * NexioSchedule hosts its header next to the content backdrop box.
 */
@Composable
fun BoxScope.FloatingSheetHeader(
    title: String,
    sheetColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    startAction: (@Composable () -> Unit)? = null,
    endAction: (@Composable () -> Unit)? = null,
) {
    // Persistent top fade (NexioSchedule mask, same gradient stops as YumeBlur.sheetTopFadeBrush).
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(FloatingSheetHeaderDefaults.Height)
                .offset(y = -FloatingSheetHeaderDefaults.LibraryEmptyTitleRowHeight)
                .zIndex(1f)
                .background(YumeBlur.sheetTopFadeBrush(sheetColor)),
    )
    // Fixed-height header: title and actions share one constant 76dp band (zIndex above the mask).
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(FloatingSheetHeaderDefaults.Height)
                .offset(y = -FloatingSheetHeaderDefaults.LibraryEmptyTitleRowHeight)
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
            Box(modifier = Modifier.align(Alignment.CenterStart)) {
                startAction()
            }
        }
        if (endAction != null) {
            Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                endAction()
            }
        }
    }
}
