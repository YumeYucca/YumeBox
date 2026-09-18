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


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import com.github.panpf.sketch.rememberAsyncImagePainter
import com.github.panpf.sketch.request.ImageRequest
import com.github.yumeyucca.yumebox.common.util.LocaleUtil
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import tf.gal.yumebox.locale.YumeTxt

@Composable
fun CountryFlagCircle(countryCode: String?, modifier: Modifier = Modifier, size: Dp = UiDp.dp18) {
    val semanticColors = AppTheme.colors
    val flagUrl = remember(countryCode) { LocaleUtil.normalizeFlagUrl(countryCode) }
    val flagLabel =
        remember(countryCode) {
            countryCode?.trim()?.ifEmpty { null } ?: LocaleUtil.UNKNOWN_FLAG_CODE
        }
    val context = LocalContext.current
    val request = remember(context, flagUrl) { ImageRequest(context, flagUrl) }

    Box(
        modifier =
            modifier
                .size(size)
                .clip(CircleShape)
                .background(semanticColors.neutralPlaceholderBackground),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter =
                rememberAsyncImagePainter(
                    request = request,
                    alignment = Alignment.Center,
                    contentScale = ContentScale.Crop,
                ),
            contentDescription = YumeTxt.Component.Flag.ContentDescription(flagLabel),
            modifier =
                Modifier
                    .matchParentSize()
                    // SVG rasterization anti-aliases the circle edge; without the slight
                    // overdraw those translucent edge pixels blend with the light placeholder
                    // background and read as a white ring on dark cards (issue #151 item 11).
                    .graphicsLayer {
                        scaleX = 1.06f
                        scaleY = 1.06f
                    },
            contentScale = ContentScale.Crop,
        )
    }
}
