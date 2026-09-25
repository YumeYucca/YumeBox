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
 * Copyright (c) YumeYucca 2025 - Present
 *
 */
package com.github.yumeyucca.yumebox.presentation.screen.node

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import com.github.yumeyucca.yumebox.presentation.theme.AnimationSpecs
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.presentation.viewmodel.ProxyDelayTestProgress
import kotlinx.coroutines.flow.StateFlow
import tf.gal.yumebox.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

@Composable
internal fun GroupDelayRefreshIndicator(
    groupName: String,
    testingGroupNames: StateFlow<Set<String>>,
    progress: StateFlow<ProxyDelayTestProgress?>,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MiuixTheme.textStyles.subtitle,
) {
    val testing by testingGroupNames.collectAsState()
    NodeDelayRefreshIndicator(
        visible = groupName in testing,
        progress = progress,
        modifier = modifier,
        textStyle = textStyle,
    )
}

@Composable
internal fun NodeDelayRefreshIndicator(
    visible: Boolean,
    progress: StateFlow<ProxyDelayTestProgress?>,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MiuixTheme.textStyles.subtitle,
) {
    val reveal by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec =
            tween(
                durationMillis = AnimationSpecs.Proxy.RefreshIndicatorDuration,
                easing =
                    if (visible) {
                        AnimationSpecs.EmphasizedDecelerate
                    } else {
                        AnimationSpecs.EmphasizedAccelerate
                    },
            ),
        label = "delay_refresh_reveal",
    )
    if (!visible && reveal == 0f) return
    // Measure the bar once at its full height, then report only the revealed slice. Children keep
    // stable constraints, so the lazy list repositions instead of remeasuring every row.
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .clipToBounds()
                .revealDown(reveal),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = reveal }
                    .padding(vertical = UiDp.dp12),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(UiDp.dp6),
        ) {
            DelayTestSpinner()
            NodeDelayRefreshProgressLine(progress = progress, style = textStyle)
        }
    }
}

private fun Modifier.revealDown(reveal: Float): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val height = (placeable.height * reveal).roundToInt().coerceIn(0, placeable.height)
        layout(placeable.width, height) {
            placeable.place(0, 0)
        }
    }

@Composable
private fun DelayTestSpinner() {
    InfiniteProgressIndicator(modifier = Modifier.size(UiDp.dp24))
}

@Composable
private fun NodeDelayRefreshProgressLine(
    progress: StateFlow<ProxyDelayTestProgress?>,
    style: TextStyle,
) {
    val current by progress.collectAsState()
    val running = current as? ProxyDelayTestProgress.Running
    val text =
        if (running != null) {
            YumeTxt.Proxy.Testing.Progress.format(running.completed, running.total)
        } else {
            YumeTxt.Proxy.Testing.InProgress
        }
    Text(
        text = text,
        style = style,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
    )
}
