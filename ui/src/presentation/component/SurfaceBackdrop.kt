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

package com.github.yumeyucca.yumebox.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

/**
 * Backdrop that samples its own content on top of [containerColor].
 *
 * Glass controls need something to sample. A sheet passes `backdrop = null` to the library (the panel
 * paints opaque colour, it does not blur the page) and a page body may be translucent, so both would
 * leave their glass empty at rest and only pick up the effect once content scrolled in. Painting the
 * container colour first gives them a base layer; keying on the colour re-records on a theme switch.
 */
@Composable
fun rememberSurfaceBackdrop(containerColor: Color): LayerBackdrop =
    key(containerColor) {
        rememberLayerBackdrop {
            drawRect(containerColor)
            drawContent()
        }
    }
