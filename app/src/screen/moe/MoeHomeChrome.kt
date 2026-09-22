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

@file:Suppress("FunctionName", "ConvertLongToDuration", "KotlinConstantConditions")

package com.github.yumeyucca.yumebox.screen.moe

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.Repeat
import com.github.yumeyucca.yumebox.presentation.theme.AnimationSpecs
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.theme.YumeHaze
import com.github.yumeyucca.yumebox.screen.home.HomeProxyControlState
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur
import tf.gal.yumebox.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun MoeSidebarDecoration(
    hazeState: HazeState,
    blurEnabled: Boolean,
    blurProgress: Float,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val spacing = AppTheme.spacing
    val surface = MiuixTheme.colorScheme.surface
    val isDarkSurface = surface.luminance() < 0.5f
    val glassGradientStart =
        if (isDarkSurface) {
            Color.Black.copy(alpha = 0.36f)
        } else {
            surface.copy(alpha = 0.23f)
        }
    val glassGradientEnd =
        if (isDarkSurface) {
            surface.copy(alpha = 0.18f)
        } else {
            surface.copy(alpha = 0.16f)
        }
    val clampedBlurProgress = blurProgress.coerceIn(0f, 1f)
    val blurRadius = lerpDp(30.dp, 52.dp, clampedBlurProgress)
    val blurModifier =
        if (blurEnabled) {
            val effectRadius = blurRadius
            Modifier.hazeBlur(
                input = HazeInput.Sources(hazeState),
                style =
                    HazeBlurStyle {
                        blurRadius(effectRadius)
                        noiseFactor(YumeHaze.ChromeNoiseFactor)
                        backgroundColor(YumeHaze.glassBackgroundColor(surface, isDarkSurface))
                        colorEffects(YumeHaze.glassColorEffects(surface, isDarkSurface))
                        fallbackColorEffect(YumeHaze.sidebarFallbackTint(surface, isDarkSurface))
                    },
            )
        } else {
            Modifier
        }

    Box(
        modifier =
            modifier
                .then(blurModifier)
                .background(
                    brush =
                        Brush.horizontalGradient(
                            colors = listOf(glassGradientStart, glassGradientEnd)
                        ),
                    shape = RectangleShape,
                )
                .padding(
                    horizontal = 0.dp,
                    vertical = spacing.space24,
                ),
        content = content,
    )
}

@Composable
internal fun MoeSidebarContent(
    topValue: String,
    bottomValue: String,
    batteryPercent: Int?,
    icons: List<MoeSidebarIconItem>,
    visibleWidth: Dp,
) {
    // Keep the rail inside the visible sidebar width; the content panel starts immediately after
    // this width, so adding horizontal decoration padding would push digits under the panel.
    val laneWidth = visibleWidth.coerceAtLeast(0.dp)
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(laneWidth),
        contentAlignment = Alignment.TopCenter,
    ) {
        MoeSidebarRail(
            topValue = topValue,
            bottomValue = bottomValue,
            batteryPercent = batteryPercent,
            icons = icons,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private const val MOE_LAUNCH_TEXT_SLIDE_DURATION = 280

private data class MoeLaunchLabel(val text: String, val order: Int)

@Composable
internal fun MoeLaunchControls(
    controlState: HomeProxyControlState,
    enabled: Boolean,
    isRemoteController: Boolean,
    surfaceColor: Color,
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit,
    onLaunchClick: () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MoeUi.Button.controlGap),
    ) {
        MoeLaunchConfigButton(surfaceColor = surfaceColor, onClick = onSettingsClick)
        MoeLaunchButton(
            controlState = controlState,
            enabled = enabled,
            isRemoteController = isRemoteController,
            surfaceColor = surfaceColor,
            modifier = Modifier.weight(1f),
            onClick = onLaunchClick,
        )
    }
}

@Composable
private fun moeLaunchButtonBorderColor(): Color {
    val onBackground = MiuixTheme.colorScheme.onBackground
    val isDark = MiuixTheme.colorScheme.background.luminance() < 0.5f
    return onBackground.copy(alpha = if (isDark) 0.22f else 0.08f)
}

@Composable
private fun MoeLaunchConfigButton(surfaceColor: Color, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by
    animateFloatAsState(
        targetValue = if (isPressed) MoeUi.Button.pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = 520f),
        label = "moe_launch_config_button_press_scale",
    )
    val contentColor = MiuixTheme.colorScheme.onBackground.copy(alpha = 0.62f)
    val borderColor = moeLaunchButtonBorderColor()

    Box(
        modifier =
            Modifier
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }
                .size(MoeUi.Button.circleSize)
                .shadow(
                    elevation = MoeUi.Button.shadowElevation,
                    shape = MoeUi.Shape.launchButton,
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.04f),
                    spotColor = Color.Black.copy(alpha = 0.08f),
                )
                .squircleSurface(surfaceColor, MoeUi.Shape.launchButtonRadius)
                .squircleBorder(
                    width = MoeUi.Button.borderWidth,
                    color = borderColor,
                    cornerRadius = MoeUi.Shape.launchButtonRadius,
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Yume.Repeat,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(MoeUi.Button.iconSize),
        )
    }
}

@Composable
internal fun MoeLaunchButton(
    controlState: HomeProxyControlState,
    enabled: Boolean,
    isRemoteController: Boolean,
    surfaceColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isRunning = controlState == HomeProxyControlState.Running
    val contentColor =
        MiuixTheme.colorScheme.onBackground.copy(alpha = if (enabled) 0.72f else 0.34f)
    val borderColor = moeLaunchButtonBorderColor()
    val pressScale by
    animateFloatAsState(
        targetValue = if (isPressed && enabled) MoeUi.Button.pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = 520f),
        label = "moe_launch_button_press_scale",
    )
    val targetLabel =
        when {
            isRemoteController && isRunning -> MoeLaunchLabel(YumeTxt.Home.Status.Running, order = 2)
            !enabled && controlState == HomeProxyControlState.Idle ->
                MoeLaunchLabel(YumeTxt.Home.Traffic.NoProfile, order = 0)

            else ->
                when (controlState) {
                    HomeProxyControlState.Idle -> MoeLaunchLabel(YumeTxt.Home.Control.Start, order = 0)
                    HomeProxyControlState.Connecting ->
                        MoeLaunchLabel(YumeTxt.Home.Status.Connecting, order = 1)

                    HomeProxyControlState.Running ->
                        MoeLaunchLabel(
                            if (isRemoteController) YumeTxt.Home.Status.Running
                            else YumeTxt.Home.Control.Stop,
                            order = 2,
                        )

                    HomeProxyControlState.Lost -> MoeLaunchLabel(YumeTxt.Home.Status.Lost, order = 0)
                    HomeProxyControlState.Disconnecting ->
                        MoeLaunchLabel(YumeTxt.Home.Status.Disconnecting, order = 1)
                }
        }

    Box(
        modifier =
            modifier
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }
                .fillMaxWidth()
                .shadow(
                    elevation = MoeUi.Button.shadowElevation,
                    shape = MoeUi.Shape.launchButton,
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.04f),
                    spotColor = Color.Black.copy(alpha = 0.08f),
                )
                .height(MoeUi.Button.height)
                .squircleSurface(surfaceColor, MoeUi.Shape.launchButtonRadius)
                .squircleBorder(
                    width = MoeUi.Button.borderWidth,
                    color = borderColor,
                    cornerRadius = MoeUi.Shape.launchButtonRadius,
                )
                .clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                )
                .padding(
                    horizontal = MoeUi.Button.horizontalPadding,
                    vertical = MoeUi.Button.verticalPadding,
                )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .heightIn(min = 28.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = targetLabel,
                modifier = Modifier.fillMaxWidth(),
                transitionSpec = {
                    val forward = targetState.order >= initialState.order
                    val slideSpec =
                        tween<IntOffset>(
                            durationMillis = MOE_LAUNCH_TEXT_SLIDE_DURATION,
                            easing = AnimationSpecs.StandardEasing,
                        )
                    val fadeSpec =
                        tween<Float>(
                            durationMillis = MOE_LAUNCH_TEXT_SLIDE_DURATION,
                            easing = AnimationSpecs.StandardEasing,
                        )
                    (slideInVertically(
                        initialOffsetY = { if (forward) it else -it },
                        animationSpec = slideSpec,
                    ) + fadeIn(animationSpec = fadeSpec))
                        .togetherWith(
                            slideOutVertically(
                                targetOffsetY = { if (forward) -it else it },
                                animationSpec = slideSpec,
                            ) + fadeOut(animationSpec = fadeSpec)
                        )
                },
                label = "moe_launch_button_text",
            ) { label ->
                Text(
                    text = label.text,
                    color = contentColor,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    softWrap = false,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
