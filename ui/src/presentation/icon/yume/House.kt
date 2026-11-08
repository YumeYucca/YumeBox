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

@file:Suppress("UnusedReceiverParameter")

package com.github.yumeyucca.yumebox.presentation.icon.yume

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType.Companion.NonZero
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap.Companion.Round
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.github.yumeyucca.yumebox.presentation.icon.Yume

val Yume.House: ImageVector
    get() {
        if (houseVector != null) {
            return houseVector!!
        }
        houseVector =
            Builder(
                    name = "House",
                    defaultWidth = 24.0.dp,
                    defaultHeight = 24.0.dp,
                    viewportWidth = 24.0f,
                    viewportHeight = 24.0f,
                )
                .apply {
                    path(
                        fill = SolidColor(Color(0x00000000)),
                        stroke = SolidColor(Color(0xFF000000)),
                        strokeLineWidth = 2.0f,
                        strokeLineCap = Round,
                        strokeLineJoin = StrokeJoin.Round,
                        strokeLineMiter = 4.0f,
                        pathFillType = NonZero,
                    ) {
                        moveTo(15.0f, 21.0f)
                        verticalLineToRelative(-8.0f)
                        arcToRelative(1.0f, 1.0f, 0.0f, isMoreThanHalf = false, isPositiveArc = false, -1.0f, -1.0f)
                        horizontalLineToRelative(-4.0f)
                        arcToRelative(1.0f, 1.0f, 0.0f, isMoreThanHalf = false, isPositiveArc = false, -1.0f, 1.0f)
                        verticalLineToRelative(8.0f)
                    }
                    path(
                        fill = SolidColor(Color(0x00000000)),
                        stroke = SolidColor(Color(0xFF000000)),
                        strokeLineWidth = 2.0f,
                        strokeLineCap = Round,
                        strokeLineJoin = StrokeJoin.Round,
                        strokeLineMiter = 4.0f,
                        pathFillType = NonZero,
                    ) {
                        moveTo(3.0f, 10.0f)
                        arcToRelative(2.0f, 2.0f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, 0.709f, -1.528f)
                        lineToRelative(7.0f, -6.0f)
                        arcToRelative(2.0f, 2.0f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, 2.582f, 0.0f)
                        lineToRelative(7.0f, 6.0f)
                        arcTo(2.0f, 2.0f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, 21.0f, 10.0f)
                        verticalLineToRelative(9.0f)
                        arcToRelative(2.0f, 2.0f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, -2.0f, 2.0f)
                        horizontalLineTo(5.0f)
                        arcToRelative(2.0f, 2.0f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, -2.0f, -2.0f)
                        close()
                    }
                }
                .build()
        return houseVector!!
    }

private var houseVector: ImageVector? = null
