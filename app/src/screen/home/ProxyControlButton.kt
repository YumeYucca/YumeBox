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

@file:Suppress("UnusedSymbol", "FunctionName")

package com.github.yumeyucca.yumebox.screen.home


import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.Play
import com.github.yumeyucca.yumebox.presentation.icon.yume.Square
import com.github.yumeyucca.yumebox.presentation.theme.AnimationSpecs
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import kotlinx.coroutines.launch
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ProxyControlButton(
    isRunning: Boolean,
    isEnabled: Boolean,
    hasEnabledProfile: Boolean,
    hasProfiles: Boolean,
    profilesLoaded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val scaleAnim = remember { Animatable(1f) }
    val cornerRadius = UiDp.dp32
    val buttonWidthFraction = 0.3f

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(UiDp.dp16),
    ) {
        if (profilesLoaded) {
            if (!hasProfiles) {
                HintText(YumeTxt.Home.Control.HintAddProfile)
            } else if (!hasEnabledProfile) {
                HintText(YumeTxt.Home.Control.HintEnableProfile)
            }
        }

        Button(
            onClick = {
                coroutineScope.launch {
                    scaleAnim.animateTo(
                        targetValue = 0.92f,
                        animationSpec =
                            tween(
                                ControlButtonPressInDurationMillis,
                                easing = AnimationSpecs.EmphasizedAccelerate,
                            ),
                    )
                    scaleAnim.animateTo(
                        targetValue = 1.03f,
                        animationSpec =
                            tween(
                                ControlButtonPressOutDurationMillis,
                                easing = AnimationSpecs.EmphasizedDecelerate,
                            ),
                    )
                    scaleAnim.animateTo(
                        targetValue = 1f,
                        animationSpec =
                            tween(
                                ControlButtonSettleDurationMillis,
                                easing = AnimationSpecs.StandardEasing,
                            ),
                    )
                }
                onClick()
            },
            enabled = isEnabled,
            modifier =
                Modifier
                    .fillMaxWidth(buttonWidthFraction)
                    .scale(scaleAnim.value)
                    .shadow(
                        elevation = UiDp.dp1,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius),
                        clip = false,
                    )
                    .squircleBorder(
                        width = UiDp.dp0_2,
                        color = MiuixTheme.colorScheme.outline,
                        cornerRadius = cornerRadius,
                    ),
            colors = ButtonDefaults.buttonColors(MiuixTheme.colorScheme.background),
            cornerRadius = cornerRadius,
            minHeight = UiDp.dp36,
        ) {
            AnimatedContent(
                targetState = isRunning,
                transitionSpec = {
                    val iconTransition =
                        tween<Float>(
                            ControlButtonIconScaleDurationMillis,
                            easing = AnimationSpecs.Legacy,
                        )
                    val enterTransition =
                        slideInVertically(
                            initialOffsetY = { it / 5 },
                            animationSpec =
                                tween(
                                    ControlButtonIconEnterDurationMillis,
                                    easing = AnimationSpecs.EnterEasing,
                                ),
                        ) +
                                fadeIn(
                                    animationSpec =
                                        tween(
                                            ControlButtonIconEnterDurationMillis,
                                            easing = AnimationSpecs.EnterEasing,
                                        )
                                ) +
                                scaleIn(initialScale = 0.8f, animationSpec = iconTransition)

                    val exitTransition =
                        slideOutVertically(
                            targetOffsetY = { -it / 5 },
                            animationSpec =
                                tween(
                                    ControlButtonIconExitDurationMillis,
                                    easing = AnimationSpecs.ExitEasing,
                                ),
                        ) +
                                fadeOut(
                                    animationSpec =
                                        tween(
                                            ControlButtonIconExitDurationMillis,
                                            easing = AnimationSpecs.ExitEasing,
                                        )
                                ) +
                                scaleOut(targetScale = 0.8f, animationSpec = iconTransition)

                    enterTransition.togetherWith(exitTransition)
                },
                label = "IconTransition",
            ) { running ->
                Icon(
                    imageVector = if (running) Yume.Square else Yume.Play,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun HintText(text: String) {
    Text(
        text = text,
        style = MiuixTheme.textStyles.body2,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
    )
}

private const val ControlButtonPressInDurationMillis = 180
private const val ControlButtonPressOutDurationMillis = 220
private const val ControlButtonSettleDurationMillis = 180
private const val ControlButtonIconEnterDurationMillis = 260
private const val ControlButtonIconExitDurationMillis = 220
private const val ControlButtonIconScaleDurationMillis = 340
