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

@file:Suppress("UnusedReceiverParameter", "DuplicatedCode")

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

val Yume.GitMerge: ImageVector
    get() {
        if (gitMergeVector != null) {
            return gitMergeVector!!
        }
        gitMergeVector =
            Builder(
                    name = "GitMerge",
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
                        moveTo(18.0f, 18.0f)
                        moveToRelative(-3.0f, 0.0f)
                        arcToRelative(3.0f, 3.0f, 0.0f, isMoreThanHalf = true, isPositiveArc = true, 6.0f, 0.0f)
                        arcToRelative(3.0f, 3.0f, 0.0f, isMoreThanHalf = true, isPositiveArc = true, -6.0f, 0.0f)
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
                        moveTo(6.0f, 6.0f)
                        moveToRelative(-3.0f, 0.0f)
                        arcToRelative(3.0f, 3.0f, 0.0f, isMoreThanHalf = true, isPositiveArc = true, 6.0f, 0.0f)
                        arcToRelative(3.0f, 3.0f, 0.0f, isMoreThanHalf = true, isPositiveArc = true, -6.0f, 0.0f)
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
                        moveTo(6.0f, 21.0f)
                        verticalLineTo(9.0f)
                        arcToRelative(9.0f, 9.0f, 0.0f, isMoreThanHalf = false, isPositiveArc = false, 9.0f, 9.0f)
                    }
                }
                .build()
        return gitMergeVector!!
    }

private var gitMergeVector: ImageVector? = null
