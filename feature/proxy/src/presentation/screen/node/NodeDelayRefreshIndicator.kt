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

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.github.yumeyucca.yumebox.presentation.theme.AnimationSpecs
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.presentation.viewmodel.ProxyDelayTestProgress
import kotlinx.coroutines.flow.StateFlow
import tf.gal.yumebox.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun NodeDelayRefreshIndicator(
    visible: Boolean,
    progress: StateFlow<ProxyDelayTestProgress?>,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MiuixTheme.textStyles.subtitle,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .animateContentSize(
                    animationSpec =
                        tween(
                            durationMillis = AnimationSpecs.Proxy.RefreshIndicatorDuration,
                            easing = AnimationSpecs.Legacy,
                        ),
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (visible) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = UiDp.dp12),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(UiDp.dp6),
            ) {
                DelayTestSpinner()
                NodeDelayRefreshProgressLine(progress = progress, style = textStyle)
            }
        }
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
