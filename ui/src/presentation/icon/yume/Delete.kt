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

val Yume.Delete: ImageVector
    get() {
        if (deleteVector != null) {
            return deleteVector!!
        }
        deleteVector =
            ImageVector.Builder(
                    name = "Trash",
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
                        moveTo(19f, 6f)
                        verticalLineToRelative(14f)
                        arcToRelative(
                            2f,
                            2f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = true,
                            -2f,
                            2f,
                        )
                        horizontalLineTo(7f)
                        arcToRelative(
                            2f,
                            2f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = true,
                            -2f,
                            -2f,
                        )
                        verticalLineTo(6f)
                    }
                    path(
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    ) {
                        moveTo(3f, 6f)
                        horizontalLineToRelative(18f)
                    }
                    path(
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    ) {
                        moveTo(8f, 6f)
                        verticalLineTo(4f)
                        arcToRelative(
                            2f,
                            2f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = true,
                            2f,
                            -2f,
                        )
                        horizontalLineToRelative(4f)
                        arcToRelative(
                            2f,
                            2f,
                            0f,
                            isMoreThanHalf = false,
                            isPositiveArc = true,
                            2f,
                            2f,
                        )
                        verticalLineToRelative(2f)
                    }
                }
                .build()

        return deleteVector!!
    }

@Suppress("ObjectPropertyName") private var deleteVector: ImageVector? = null
