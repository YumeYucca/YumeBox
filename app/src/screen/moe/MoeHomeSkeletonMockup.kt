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

package com.github.yumeyucca.yumebox.screen.moe


import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Which guided demo the home skeleton mockup plays. */
internal enum class HomeMockupDemo {
    /** The bottom launch capsule is highlighted and taps in a repeating rhythm. */
    StartButton,

    /** A plain launch row; instead the hero is long-pressed and a photo-picker sheet slides up. */
    Wallpaper,
}

@Composable
internal fun MoeHomeSkeletonMockup(
    demo: HomeMockupDemo = HomeMockupDemo.StartButton,
    animateLaunch: Boolean = true,
    markWallpaper: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val loop = rememberMockupWallpaperLoop()
    val wallpaper = demo == HomeMockupDemo.Wallpaper

    MockupPhoneFrame(palette = mockupPalette(), modifier = modifier) {
        Row(modifier = Modifier.fillMaxSize()) {
            MockupSidebar(
                modifier =
                    Modifier
                        .fillMaxHeight()
                        .weight(0.22f),
            )
            MockupHomeContent(
                loop = loop,
                wallpaper = wallpaper,
                animateLaunch = animateLaunch,
                markWallpaper = markWallpaper,
                modifier =
                    Modifier
                        .fillMaxHeight()
                        .weight(0.78f),
            )
        }

        if (wallpaper) {
            MockupWallpaperSheetOverlay(loop = loop)
        }
    }
}

/**
 * The four looping values the wallpaper demo draws with.
 *
 * They are handed over as [State] and never read here, so the composables that paint them read
 * them inside `graphicsLayer` at draw time instead of paying a recomposition per frame.
 */
private class MockupWallpaperLoop(
    val sheetProgress: State<Float>,
    val heroLongPressScale: State<Float>,
    val heroRippleScale: State<Float>,
    val heroRippleAlpha: State<Float>,
)

/** The single infinite transition every wallpaper-demo timing is keyed on. */
@Composable
private fun rememberMockupWallpaperLoop(): MockupWallpaperLoop {
    val transition = rememberInfiniteTransition(label = "home_wallpaper")

    return MockupWallpaperLoop(
        sheetProgress = rememberWallpaperSheetProgress(transition),
        heroLongPressScale = rememberWallpaperHeroLongPressScale(transition),
        heroRippleScale = rememberWallpaperHeroRippleScale(transition),
        heroRippleAlpha = rememberWallpaperHeroRippleAlpha(transition),
    )
}

/** Sheet slide: hidden, up, hold, back down. */
@Composable
private fun rememberWallpaperSheetProgress(transition: InfiniteTransition): State<Float> =
    transition.loopKeyframes(
        label = "wallpaper_sheet_progress",
        initialValue = 0f,
        targetValue = 0f,
        durationMillis = WallpaperCycleMillis,
    ) {
        0f at 0
        0f at 1500
        1f at 1900 using FastOutSlowInEasing
        1f at 3500
        0f at 3900 using FastOutSlowInEasing
        0f at 4200
    }

/** Hero long-press: the hero shrinks, holds, then springs back. */
@Composable
private fun rememberWallpaperHeroLongPressScale(transition: InfiniteTransition): State<Float> =
    transition.loopKeyframes(
        label = "wallpaper_hero_long_press_scale",
        initialValue = 1f,
        targetValue = 1f,
        durationMillis = WallpaperCycleMillis,
    ) {
        1f at 0
        1f at 500
        0.82f at 760 using FastOutSlowInEasing
        0.82f at 1450
        1f at 1680 using FastOutSlowInEasing
        1f at 4200
    }

/** Hero ripple: expands under the long-press. */
@Composable
private fun rememberWallpaperHeroRippleScale(transition: InfiniteTransition): State<Float> =
    transition.loopKeyframes(
        label = "wallpaper_hero_ripple_scale",
        initialValue = 0.6f,
        targetValue = 2.1f,
        durationMillis = WallpaperCycleMillis,
    ) {
        0.6f at 0
        0.6f at 760
        2.1f at 1500 using FastOutSlowInEasing
        2.1f at 4200
    }

/** Hero ripple alpha: fades out as the ripple expands. */
@Composable
private fun rememberWallpaperHeroRippleAlpha(transition: InfiniteTransition): State<Float> =
    transition.loopKeyframes(
        label = "wallpaper_hero_ripple_alpha",
        initialValue = 0.5f,
        targetValue = 0f,
        durationMillis = WallpaperCycleMillis,
    ) {
        0f at 0
        0f at 650
        0.45f at 760
        0f at 1500 using FastOutSlowInEasing
        0f at 4200
    }

/** Left sidebar (~22%): vertical clock near the top, nav icon rail pinned to the bottom. */
@Composable
private fun MockupSidebar(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(10.dp))
        MockupVerticalClock()
        Spacer(Modifier.weight(1f))
        MockupNavIconRail()
        Spacer(Modifier.height(10.dp))
    }
}

/** Vertical clock: one continuous stroke standing in for the hour and minute digits. */
@Composable
private fun MockupVerticalClock(modifier: Modifier = Modifier) {
    val palette = mockupPalette()

    Box(
        modifier =
            modifier
                .width(13.dp)
                .height(72.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(palette.maskStrong)
    )
}

/** Nav icon rail: three icons stacked near the bottom of the sidebar. */
@Composable
private fun MockupNavIconRail(modifier: Modifier = Modifier) {
    val palette = mockupPalette()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        repeat(3) {
            Box(
                Modifier
                    .size(13.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(palette.mask)
            )
        }
    }
}

/** Right content (~78%): hero card on top, quote + launch controls block underneath. */
@Composable
private fun MockupHomeContent(
    loop: MockupWallpaperLoop,
    wallpaper: Boolean,
    animateLaunch: Boolean,
    markWallpaper: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MockupSectionGap),
    ) {
        MockupHeroCard(
            loop = loop,
            wallpaper = wallpaper,
            markWallpaper = markWallpaper,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(2f),
        )

        MockupLaunchPanel(
            wallpaper = wallpaper,
            animateLaunch = animateLaunch,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
        )
    }
}

/** Hero card: traffic strip + node info bottom-aligned, the demo overlay painted over them. */
@Composable
private fun MockupHeroCard(
    loop: MockupWallpaperLoop,
    wallpaper: Boolean,
    markWallpaper: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = mockupPalette()

    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(13.dp))
                .background(palette.maskStrong)
    ) {
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(7.dp),
            verticalArrangement = Arrangement.spacedBy(MockupSectionGap),
        ) {
            MockupTrafficStrip()
            MockupNodeInfoRow()
        }

        // Wallpaper demo: a looping long-press effect centered on the hero.
        if (wallpaper) {
            MockupHeroLongPressRipple(
                loop = loop,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        if (markWallpaper) {
            MockupWallpaperTag(modifier = Modifier.align(Alignment.TopEnd))
        }
    }
}

/** Traffic strip: UP on the left, DOWN on the right. */
@Composable
private fun MockupTrafficStrip(modifier: Modifier = Modifier) {
    val palette = mockupPalette()

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            Modifier
                .width(30.dp)
                .height(9.dp)
                .clip(MockupBarShape)
                .background(palette.onHero)
        )
        Box(
            Modifier
                .width(30.dp)
                .height(9.dp)
                .clip(MockupBarShape)
                .background(palette.onHero)
        )
    }
}

/** Node info: flag circle + node name on the left, ping on the right. */
@Composable
private fun MockupNodeInfoRow(modifier: Modifier = Modifier) {
    val palette = mockupPalette()

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(palette.onHero)
            )
            Box(
                Modifier
                    .width(46.dp)
                    .height(7.dp)
                    .clip(MockupBarShape)
                    .background(palette.onHero)
            )
        }
        Box(
            Modifier
                .width(20.dp)
                .height(7.dp)
                .clip(MockupBarShape)
                .background(palette.onHero)
        )
    }
}

/** Wallpaper demo: the shrinking hold dot with its expanding ripple, centered on the hero. */
@Composable
private fun MockupHeroLongPressRipple(
    loop: MockupWallpaperLoop,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MiuixTheme.colorScheme

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .graphicsLayer {
                        scaleX = loop.heroRippleScale.value
                        scaleY = loop.heroRippleScale.value
                        alpha = loop.heroRippleAlpha.value
                    }
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(colorScheme.primary)
        )
        Box(
            modifier =
                Modifier
                    .graphicsLayer {
                        scaleX = loop.heroLongPressScale.value
                        scaleY = loop.heroLongPressScale.value
                    }
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(colorScheme.surface)
                    .border(2.dp, colorScheme.primary, CircleShape)
        )
    }
}

/** Wallpaper demo: the "System Wallpaper" tag pinned to the hero's top-end corner. */
@Composable
private fun MockupWallpaperTag(modifier: Modifier = Modifier) {
    Text(
        text = "System Wallpaper",
        color = Color.White,
        style = MiuixTheme.textStyles.footnote1.copy(fontSize = 11.sp),
        modifier = modifier.padding(10.dp),
    )
}

/** Bottom third: quote block on top, launch controls row (config circle + capsule) at the bottom. */
@Composable
private fun MockupLaunchPanel(
    wallpaper: Boolean,
    animateLaunch: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MockupSectionGap),
    ) {
        MockupQuoteBlock(modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.weight(1f))

        if (wallpaper || !animateLaunch) {
            LaunchRow()
        } else {
            HighlightedLaunchRow()
        }

        Spacer(Modifier.height(5.dp))
    }
}

/** Quote block: three left-aligned lines and a right-aligned author line. */
@Composable
private fun MockupQuoteBlock(modifier: Modifier = Modifier) {
    val palette = mockupPalette()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth(0.92f)
                .height(8.dp)
                .clip(MockupBarShape)
                .background(palette.mask)
        )
        Box(
            Modifier
                .fillMaxWidth(0.8f)
                .height(8.dp)
                .clip(MockupBarShape)
                .background(palette.mask)
        )
        Box(
            Modifier
                .fillMaxWidth(0.5f)
                .height(8.dp)
                .clip(MockupBarShape)
                .background(palette.mask)
        )
        Box(
            Modifier
                .align(Alignment.End)
                .fillMaxWidth(0.3f)
                .height(6.dp)
                .clip(MockupBarShape)
                .background(palette.mask)
        )
    }
}

/**
 * Wallpaper demo: the dim scrim and the photo-picker bottom sheet sliding up. Both are clipped
 * to the phone-frame's inner radius so they follow the rounded corners instead of sharp edges.
 */
@Composable
private fun MockupWallpaperSheetOverlay(
    loop: MockupWallpaperLoop,
    modifier: Modifier = Modifier,
) {
    val sheetProgress by loop.sheetProgress

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(15.dp))
    ) {
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.40f * sheetProgress))
        )
        PhotoPickerSheet(
            sheetProgress = sheetProgress,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun HighlightedLaunchRow(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "start_chip")
    // A press that reads as a tap: hold, snap down, snap back, then rest until the next cycle.
    val animatedPressScale by
    transition.loopKeyframes(
        label = "press_scale",
        initialValue = 1f,
        targetValue = 1f,
        durationMillis = LaunchTapCycleMillis,
    ) {
        1f at 0
        1f at 200
        0.9f at 360 using FastOutSlowInEasing
        1f at 560
        1f at 1500
    }
    val animatedRippleScale by
    transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 2.6f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "ripple_scale",
    )
    val animatedRippleAlpha by
    transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "ripple_alpha",
    )

    LaunchRow(
        modifier = modifier,
        pressScale = animatedPressScale,
        behind = {
            MockupLaunchTapRipple(
                scale = animatedRippleScale,
                alpha = animatedRippleAlpha,
            )
        },
        front = {
            // Prominent touch dot over the capsule's center, pressing in sync with it.
            MockupLaunchTapDot(scale = animatedPressScale)
        },
    )
}

/**
 * Shared launch strip: the bordered config circle on the left and the start capsule filling the
 * rest. [behind] paints under the capsule (tap ripple) and [front] above it (touch dot), so the
 * animated demo keeps its original stacking order.
 */
@Composable
private fun LaunchRow(
    modifier: Modifier = Modifier,
    pressScale: Float = 1f,
    behind: @Composable BoxScope.() -> Unit = {},
    front: @Composable BoxScope.() -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        LaunchConfigButton()
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            behind()
            LaunchCapsule(pressScale = pressScale)
            front()
        }
    }
}

@Composable
private fun LaunchConfigButton(modifier: Modifier = Modifier) {
    val colorScheme = MiuixTheme.colorScheme
    val palette = mockupPalette()

    // Config button: a bordered surface circle with an icon glyph in the middle.
    Box(
        modifier =
            modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(colorScheme.surface)
                .border(1.dp, palette.frameBorder, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier
            .size(8.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(palette.maskStrong))
    }
}

@Composable
private fun LaunchCapsule(
    modifier: Modifier = Modifier,
    pressScale: Float = 1f,
) {
    val colorScheme = MiuixTheme.colorScheme
    val palette = mockupPalette()

    // Launch capsule filling the remaining width, optionally pressing on each tap.
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }
                .height(22.dp)
                .clip(RoundedCornerShape(50))
                .background(colorScheme.surface)
                .border(1.dp, palette.frameBorder, RoundedCornerShape(50)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = YumeTxt.Home.Control.Start,
            color = colorScheme.onSurface.copy(alpha = 0.72f),
            style =
                MiuixTheme.textStyles.footnote1.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
        )
    }
}

@Composable
private fun PhotoPickerSheet(sheetProgress: Float, modifier: Modifier = Modifier) {
    val colorScheme = MiuixTheme.colorScheme

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .graphicsLayer { translationY = (1f - sheetProgress) * size.height }
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(colorScheme.surface)
                .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            MockupPickerGrabber()

            // Tab pills: the "All" tab highlighted, the "Albums" tab muted.
            MockupPickerTabPills()

            // Notice bar.
            MockupPickerNoticeBar()

            // Photo grid: 3 rows of 3 tiles filling the freed space; the first tile is the camera.
            MockupPickerPhotoGrid()
        }
    }
}

/** The placeholder bar shape every mockup strip is clipped to. */
private val MockupBarShape = RoundedCornerShape(3.dp)

/** Vertical gap between the mockup's stacked regions. */
private val MockupSectionGap = 6.dp

/** One wallpaper-demo cycle: long-press the hero, hold, raise the picker, fade it back. */
private const val WallpaperCycleMillis = 4200

/** One launch-chip demo cycle: hold, snap down, snap back, rest. */
private const val LaunchTapCycleMillis = 1500

/**
 * One looped value of a shared [InfiniteTransition].
 *
 * Every mockup animation is "describe a cycle, repeat it forever"; the repeat wrapper and the
 * keyframe duration live here so each choreography block is only its keyframes.
 */
@Composable
private fun InfiniteTransition.loopKeyframes(
    label: String,
    initialValue: Float,
    targetValue: Float,
    durationMillis: Int,
    spec: KeyframesSpec.KeyframesSpecConfig<Float>.() -> Unit,
): State<Float> =
    animateFloat(
        initialValue = initialValue,
        targetValue = targetValue,
        animationSpec =
            infiniteRepeatable(
                animation =
                    keyframes {
                        this.durationMillis = durationMillis
                        spec()
                    },
                repeatMode = RepeatMode.Restart,
            ),
        label = label,
    )

/** Launch demo: the ripple painted behind the capsule, expanding as it fades. */
@Composable
private fun MockupLaunchTapRipple(
    scale: Float,
    alpha: Float,
) {
    Box(
        modifier =
            Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
                .size(26.dp)
                .clip(CircleShape)
                .background(MiuixTheme.colorScheme.primary)
    )
}

/** Launch demo: the touch dot painted over the capsule, pressing with it. */
@Composable
private fun MockupLaunchTapDot(scale: Float) {
    Box(
        modifier =
            Modifier
                .offset(x = 4.dp, y = 4.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .size(20.dp)
                .clip(CircleShape)
                .background(MiuixTheme.colorScheme.primary)
                .border(2.dp, MiuixTheme.colorScheme.surface, CircleShape)
    )
}

/** Photo-picker sheet: the drag handle above the tab row. */
@Composable
private fun MockupPickerGrabber() {
    val palette = mockupPalette()

    Box(
        Modifier
            .width(22.dp)
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(palette.mask)
    )
}

/** Photo-picker sheet: the "All" and "Albums" tab pills. */
@Composable
private fun MockupPickerTabPills() {
    val colorScheme = MiuixTheme.colorScheme
    val opacity = AppTheme.opacity
    val palette = mockupPalette()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .weight(1f)
                .height(15.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(colorScheme.primary.copy(alpha = opacity.subtleStrong))
        )
        Box(
            Modifier
                .weight(1f)
                .height(15.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(palette.mask)
        )
    }
}

/** Photo-picker sheet: the notice bar under the tabs. */
@Composable
private fun MockupPickerNoticeBar() {
    val palette = mockupPalette()

    Box(
        Modifier
            .fillMaxWidth()
            .height(18.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(palette.mask)
    )
}

/** Photo-picker sheet: the 3x3 tile grid filling the space the sheet chrome leaves. */
@Composable
private fun ColumnScope.MockupPickerPhotoGrid() {
    val palette = mockupPalette()

    Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(3) { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                repeat(3) { col ->
                    val isCamera = row == 0 && col == 0
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    if (isCamera) palette.mask else palette.maskStrong
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isCamera) {
                            Box(
                                Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(palette.maskStrong)
                            )
                        }
                    }
                }
            }
        }
    }
}
