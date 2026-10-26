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
 * Static mark adapted from KernelSU AboutMiuix.kt (https://github.com/tiann/KernelSU).
 * Original work: GPL-3.0-or-later.
 */

@file:Suppress("FunctionName")

package com.github.yumeyucca.yumebox.screen.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.yumeyucca.yumebox.R
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun AboutHero(
    name: String,
    version: String,
    topPadding: Dp,
    backdrop: LayerBackdrop?,
    logoBlend: List<BlendColorEntry>,
    versionProgress: Float,
    nameProgress: Float,
    iconProgress: Float,
    onHeight: (Dp) -> Unit,
    onIconBottom: (Float) -> Unit,
    onNameBottom: (Float) -> Unit,
    onVersionBottom: (Float) -> Unit,
    onVersionClick: () -> Unit,
) {
    val density = LocalDensity.current
    val colors = MiuixTheme.colorScheme
    val blur =
        if (backdrop == null) {
            Modifier
        } else {
            Modifier.textureBlur(
                backdrop = backdrop,
                shape = RoundedCornerShape(0.dp),
                blurRadius = 150f,
                colors = BlurColors(blendColors = logoBlend),
                contentBlendMode = BlendMode.DstIn,
            )
        }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding + 92.dp)
            .onSizeChanged { size -> onHeight(with(density) { size.height.toDp() }) },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(128.dp)
                .clipToBounds()
                .graphicsLayer {
                    alpha = 1f - iconProgress
                    scaleX = 1f - iconProgress * 0.05f
                    scaleY = 1f - iconProgress * 0.05f
                }
                .onGloballyPositioned { coords ->
                    onIconBottom(coords.positionInWindow().y + coords.size.height)
                },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                modifier = Modifier.size(128.dp).then(blur),
                painter = painterResource(R.drawable.ic_logo_service),
                colorFilter = ColorFilter.tint(colors.onBackground),
                contentDescription = name,
            )
        }
        Text(
            text = name,
            modifier = Modifier
                .padding(top = 12.dp, bottom = 5.dp)
                .onGloballyPositioned { coords ->
                    onNameBottom(coords.positionInWindow().y + coords.size.height)
                }
                .graphicsLayer {
                    alpha = 1f - nameProgress
                    scaleX = 1f - nameProgress * 0.05f
                    scaleY = 1f - nameProgress * 0.05f
                }
                .then(blur),
            fontSize = 35.sp,
            fontWeight = FontWeight.ExtraBold,
            color = colors.onBackground,
        )
        Text(
            text = version,
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coords ->
                    onVersionBottom(coords.positionInWindow().y + coords.size.height)
                }
                .graphicsLayer {
                    alpha = 1f - versionProgress
                    scaleX = 1f - versionProgress * 0.05f
                    scaleY = 1f - versionProgress * 0.05f
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onVersionClick,
                ),
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            color = colors.onSurfaceVariantSummary,
        )
    }
}