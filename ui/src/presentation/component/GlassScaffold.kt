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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import com.github.yumeyucca.yumebox.presentation.theme.LocalTopBarBackdrop
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Route-level [Scaffold] whose top bar can progressively blur the body content scrolling under
 * it. Creates a [top.yukonga.miuix.kmp.blur.LayerBackdrop] capturing the body (opaque surface
 * underlay first, so translucent pages don't bleed through; keyed on the surface color so a
 * theme switch re-records), provides it as [LocalTopBarBackdrop] for `TopBar`/`SmallTopBar`, and
 * attaches the capture to the body wrapper. The top bar composes in the Scaffold's topBar slot,
 * outside the captured subtree, satisfying the miuix-blur same-subtree constraint; devices
 * without RuntimeShader keep the scrim-only fallback inside the top bar itself.
 */
@Composable
fun GlassScaffold(
    topBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val surface = MiuixTheme.colorScheme.surface
    val backdrop =
        key(surface) {
            rememberLayerBackdrop {
                drawRect(surface)
                drawContent()
            }
        }
    CompositionLocalProvider(LocalTopBarBackdrop provides backdrop) {
        Scaffold(
            topBar = topBar,
            modifier = modifier,
            floatingActionButton = floatingActionButton,
        ) { innerPadding ->
            Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
                content(innerPadding)
            }
        }
    }
}
