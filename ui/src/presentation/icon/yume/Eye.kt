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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.github.yumeyucca.yumebox.presentation.icon.Yume

val Yume.Eye: ImageVector
    get() {
        if (eyeVector != null) {
            return eyeVector!!
        }
        eyeVector =
            ImageVector.Builder(
                    name = "IconName",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 24f,
                    viewportHeight = 24f,
                )
                .apply {
                    path(
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    ) {
                        moveTo(2.062f, 12.348f)
                        arcToRelative(
                            1f,
                            1f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = true,
                            0f,
                            -0.696f,
                        )
                        arcToRelative(
                            10.75f,
                            10.75f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = true,
                            19.876f,
                            0f,
                        )
                        arcToRelative(
                            1f,
                            1f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = true,
                            0f,
                            0.696f,
                        )
                        arcToRelative(
                            10.75f,
                            10.75f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = true,
                            -19.876f,
                            0f,
                        )
                    }
                    path(
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    ) {
                        moveTo(12f, 12f)
                        moveToRelative(-3f, 0f)
                        arcToRelative(
                            3f,
                            3f,
                            0f,
                            isMoreThanHalf = true,
                            isPositiveArc = true,
                            6f,
                            0f,
                        )
                        arcToRelative(
                            3f,
                            3f,
                            0f,
                            isMoreThanHalf = true,
                            isPositiveArc = true,
                            -6f,
                            0f,
                        )
                    }
                }
                .build()

        return eyeVector!!
    }

@Suppress("ObjectPropertyName") private var eyeVector: ImageVector? = null
