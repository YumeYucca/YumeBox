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

@file:Suppress("ConstPropertyName")

package com.github.yumeyucca.yumebox.presentation.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing

/** Shared motion vocabulary; every entry here must have a real call site. */
object AnimationSpecs {
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
    val StandardEasing = FastOutSlowInEasing

    // FastOutSlowInEasing is exactly CubicBezierEasing(0.4, 0, 0.2, 1); the legacy sheets ask
    // for that same curve under an older name, so alias it rather than build a second instance.
    val Legacy = StandardEasing

    object Proxy {
        const val SheetSlideInDuration = 340
        const val SheetSlideOutDuration = 300
        const val SheetFadeInDuration = 140
        const val SheetFadeOutDuration = 140

        const val RefreshIndicatorDuration = 200
    }
}
