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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
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
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val HeroTopInset = 92.dp
private val HeroLogoSize = 128.dp
private const val HeroLogoBlurRadius = 150f
private const val HeroNameFontSizeSp = 35f

private val AboutHeroDefaultHeight = 200.dp
private const val VersionStageDelayFraction = 0.5f
private const val MinimumHeroStagePx = 1f
private const val HeroStageShrinkFraction = 0.05f

private data class AboutHeroBounds(
    val spacerBottom: Float = 0f,
    val iconBottom: Float = 0f,
    val nameBottom: Float = 0f,
    val versionBottom: Float = 0f,
    val initialSpacerBottom: Float = 0f,
)

internal data class AboutHeroProgress(
    val version: Float,
    val name: Float,
    val icon: Float,
)

private fun AboutHeroBounds.progressAt(index: Int, offset: Int): AboutHeroProgress {
    if (index > 0) {
        return AboutHeroProgress(version = 1f, name = 1f, icon = 1f)
    }
    if (offset == 0) {
        return AboutHeroProgress(version = 0f, name = 0f, icon = 0f)
    }

    val logoAreaBottom = initialSpacerBottom.takeIf { it > 0f } ?: spacerBottom
    val versionStage = logoAreaBottom - versionBottom
    val nameStage = versionBottom - nameBottom
    val iconStage = nameBottom - iconBottom
    val versionDelay = versionStage * VersionStageDelayFraction
    val offsetPx = offset.toFloat()

    return AboutHeroProgress(
        version = heroStageProgress(offsetPx, versionDelay, versionStage - versionDelay),
        name = heroStageProgress(offsetPx, versionStage, nameStage),
        icon = heroStageProgress(offsetPx, versionStage + nameStage, iconStage),
    )
}

private fun heroStageProgress(offset: Float, stageStart: Float, stageSize: Float): Float =
    ((offset - stageStart) / stageSize.coerceAtLeast(MinimumHeroStagePx)).coerceIn(0f, 1f)

/**
 * Measurements the hero reports back while it scrolls away; the screen derives the fade stages from
 * them. Shared by [AboutHero] (writes) and the About list (reads).
 */
@Stable
internal class AboutHeroMetrics {
    var heroHeight by mutableStateOf(AboutHeroDefaultHeight)
        private set
    var spacerHeightPx by mutableIntStateOf(0)
        private set
    private var bounds by mutableStateOf(AboutHeroBounds())

    fun updateHeroHeight(height: Dp) {
        if (heroHeight != height) heroHeight = height
    }

    fun updateSpacerHeightPx(heightPx: Int) {
        if (spacerHeightPx != heightPx) spacerHeightPx = heightPx
    }

    fun updateSpacerBottom(bottom: Float, captureInitial: Boolean) {
        val initial = bounds.initialSpacerBottom
        val nextInitial =
            if (captureInitial && initial == 0f && bottom > 0f) bottom else initial
        if (bounds.spacerBottom != bottom || nextInitial != initial) {
            bounds = bounds.copy(
                spacerBottom = bottom,
                initialSpacerBottom = nextInitial,
            )
        }
    }

    fun updateIconBottom(bottom: Float) {
        if (bounds.iconBottom == 0f) {
            bounds = bounds.copy(iconBottom = bottom)
        }
    }

    fun updateNameBottom(bottom: Float) {
        if (bounds.nameBottom == 0f) {
            bounds = bounds.copy(nameBottom = bottom)
        }
    }

    fun updateVersionBottom(bottom: Float) {
        if (bounds.versionBottom == 0f) {
            bounds = bounds.copy(versionBottom = bottom)
        }
    }

    fun progressAt(index: Int, offset: Int): AboutHeroProgress =
        bounds.progressAt(index = index, offset = offset)
}

/** One hero stage dissolving while it shrinks by [HeroStageShrinkFraction]. */
private fun Modifier.heroStage(progress: Float): Modifier =
    graphicsLayer {
        alpha = 1f - progress
        val scale = 1f - progress * HeroStageShrinkFraction
        scaleX = scale
        scaleY = scale
    }

@Composable
internal fun AboutHero(
    name: String,
    version: String,
    topPadding: Dp,
    backdrop: LayerBackdrop?,
    logoBlend: List<BlendColorEntry>,
    progress: AboutHeroProgress,
    metrics: AboutHeroMetrics,
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
                shape = RectangleShape,
                blurRadius = HeroLogoBlurRadius,
                colors = BlurColors(blendColors = logoBlend),
                contentBlendMode = BlendMode.DstIn,
            )
        }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding + HeroTopInset)
            .onSizeChanged { size -> metrics.updateHeroHeight(with(density) { size.height.toDp() }) },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(HeroLogoSize)
                .clipToBounds()
                .heroStage(progress.icon)
                .onGloballyPositioned { coords ->
                    metrics.updateIconBottom(coords.positionInWindow().y + coords.size.height)
                },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                modifier = Modifier.size(HeroLogoSize).then(blur),
                painter = painterResource(R.drawable.ic_logo_service),
                colorFilter = ColorFilter.tint(colors.onBackground),
                contentDescription = name,
            )
        }
        Text(
            text = name,
            modifier = Modifier
                .padding(top = UiDp.dp12, bottom = UiDp.dp5)
                .onGloballyPositioned { coords ->
                    metrics.updateNameBottom(coords.positionInWindow().y + coords.size.height)
                }
                .heroStage(progress.name)
                .then(blur),
            fontSize = HeroNameFontSizeSp.sp,
            fontWeight = FontWeight.ExtraBold,
            color = colors.onBackground,
        )
        Text(
            text = version,
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coords ->
                    metrics.updateVersionBottom(coords.positionInWindow().y + coords.size.height)
                }
                .heroStage(progress.version)
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
