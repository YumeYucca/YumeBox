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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.vector.PathParser
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun EmptyResourceIllustration(modifier: Modifier = Modifier) {
    val paths = remember { EmptyResourcePaths.create() }
    val outline = MiuixTheme.colorScheme.primary.copy(alpha = 0.55f)
    val fill = MiuixTheme.colorScheme.primary.copy(alpha = 0.14f)

    Canvas(modifier = modifier.size(UiDp.dp220)) {
        val viewportWidth = 142f
        val viewportHeight = 166.58142f
        val scale = minOf(size.width / viewportWidth, size.height / viewportHeight)
        val left = (size.width - viewportWidth * scale) / 2f
        val top = (size.height - viewportHeight * scale) / 2f

        translate(left = left, top = top) {
            scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero) {
                drawCircle(color = Color.Transparent, radius = 71f, center = Offset(71f, 71f))
                clipPath(paths.mask) {
                    drawPath(paths.topPaper, Color.Transparent)
                    drawPath(paths.topPaperOutline, outline)
                    drawOval(
                        color = fill,
                        topLeft = Offset(80.53735f, 80.05969f),
                        size = Size(10.597015f, 10.597015f),
                    )
                    drawPath(paths.smallRing, outline, style = Fill)
                    drawPath(
                        paths.smallPatch,
                        fill,
                    )
                    drawPath(
                        paths.smallPatch,
                        fill,
                        style =
                            Stroke(
                                width = 3f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                            ),
                    )
                    drawPath(paths.leftPaper, Color.Transparent)
                    drawPath(paths.leftPaperOutline, outline)
                    drawPath(paths.rightPaper, Color.Transparent)
                    drawPath(paths.rightPaperOutline, outline)
                    rotate(degrees = -55f, pivot = Offset(104.06152f, 87.72424f)) {
                        drawRect(
                            color = fill,
                            topLeft = Offset(104.06152f, 87.72424f),
                            size = Size(11f, 21.72388f),
                        )
                    }
                    drawOval(
                        color = fill,
                        topLeft = Offset(30.07434f, 66.28357f),
                        size = Size(9.865805f, 9.865805f),
                    )
                    drawOval(
                        color = outline,
                        topLeft = Offset(30.07434f, 66.28357f),
                        size = Size(9.865805f, 9.865805f),
                        style =
                            Stroke(
                                width = 3f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                            ),
                    )
                    drawPath(
                        paths.waveBase,
                        fill,
                        style =
                            Stroke(
                                width = 3f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                            ),
                    )
                    drawPathLine(paths.shortLineA, outline)
                    drawPathLine(paths.shortLineB, outline)
                    drawPathLine(paths.shortLineC, outline)
                    drawPathLine(paths.handleA, outline)
                    drawPathLine(paths.handleB, outline)
                    drawPath(
                        paths.centerHand,
                        fill,
                        style =
                            Stroke(
                                width = 3f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                            ),
                    )
                    drawPath(
                        paths.leftConnector,
                        outline,
                        style = Stroke(width = 3f, join = StrokeJoin.Round),
                    )
                    drawPathLine(paths.leftHandle, outline)
                    rotate(degrees = 35f, pivot = Offset(46.7207f, 38.18323f)) {
                        drawRect(
                            color = outline,
                            topLeft = Offset(46.7207f, 38.18323f),
                            size = Size(14.83582f, 8.477612f),
                            style =
                                Stroke(
                                    width = 3f,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round,
                                ),
                        )
                    }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPathLine(
    path: Path,
    color: Color,
) {
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

private data class EmptyResourcePaths(
    val mask: Path,
    val topPaper: Path,
    val topPaperOutline: Path,
    val smallRing: Path,
    val smallPatch: Path,
    val leftPaper: Path,
    val leftPaperOutline: Path,
    val rightPaper: Path,
    val rightPaperOutline: Path,
    val waveBase: Path,
    val shortLineA: Path,
    val shortLineB: Path,
    val shortLineC: Path,
    val handleA: Path,
    val handleB: Path,
    val centerHand: Path,
    val leftConnector: Path,
    val leftHandle: Path,
) {
    companion object {
        fun create(): EmptyResourcePaths =
            EmptyResourcePaths(
                mask =
                    Path().apply {
                        addOval(
                            androidx.compose.ui.geometry.Rect(
                                left = 0f,
                                top = 0f,
                                right = 142f,
                                bottom = 142f,
                            )
                        )
                    },
                topPaper =
                    svgPath(
                        "M45.3022 75.8263L88.7522 107.616L105.172 82.9763L73.2622 61.2063C71.8976 60.2755 70.103 60.2798 68.7422 61.2163L50.6022 73.6963L45.3022 75.8263"
                    ),
                topPaperOutline =
                    svgPath(
                        "M49.7506 72.4655L67.8917 59.9803L68.7421 61.216L67.8917 59.9803L68.7421 61.216L67.8917 59.9803Q68.5856 59.5028 69.3888 59.2551Q70.1749 59.0127 70.9985 59.011Q71.822 59.0093 72.6091 59.2485Q73.4133 59.4928 74.1092 59.9675L73.2639 61.2066L74.1092 59.9675L106.021 81.7358L106.423 83.807L90.0003 108.444L87.8665 108.823L44.4165 77.0332L44.7447 74.4301L50.0435 72.3086L50.601 73.7011L49.7506 72.4655ZM51.4514 74.9368L51.1585 75.0937L45.8598 77.2151L45.3022 75.8226L46.188 74.612L89.6379 106.401L88.7522 107.612L87.5041 106.78L103.927 82.143L105.175 82.975L104.33 84.2141L72.4186 62.4458L72.4186 62.4458Q71.779 62.0094 71.0047 62.011Q70.2303 62.0126 69.5925 62.4516L69.5925 62.4516L69.5925 62.4516L51.4514 74.9368ZM51.1585 75.0937L50.601 73.7011L51.4514 74.9368L51.3137 75.0316L51.1585 75.0937ZM106.423 83.807L105.175 82.975L106.021 81.7358L107.247 82.5721L106.423 83.807ZM87.8665 108.823L88.7522 107.612L90.0003 108.444L89.1311 109.748L87.8665 108.823ZM44.7447 74.4301L45.3022 75.8226L44.4165 77.0332L42.233 75.4357L44.7447 74.4301Z"
                    ),
                smallRing =
                    svgPath(
                        "M85.8359 90.6567C88.7621 90.6567 91.1344 88.2845 91.1344 85.3582C91.1344 82.4319 88.7621 80.0597 85.8359 80.0597C82.9096 80.0597 80.5374 82.4319 80.5374 85.3582C80.5374 88.2845 82.9096 90.6567 85.8359 90.6567ZM85.8359 83.0597C87.1053 83.0597 88.1344 84.0888 88.1344 85.3582C88.1344 86.6276 87.1053 87.6567 85.8359 87.6567C84.5664 87.6567 83.5374 86.6276 83.5374 85.3582C83.5374 84.0888 84.5664 83.0597 85.8359 83.0597Z"
                    ),
                smallPatch =
                    svgPath(
                        "M100 90C97.9998 88.5374 97.3566 88.5374 96.5482 88.5374C94.436 88.5374 92.7239 90.25 92.7239 92.362C92.7239 93.6053 92.9996 95 94.9998 96L100 90Z"
                    ),
                leftPaper =
                    svgPath(
                        "M21.4642 111.858L79.7442 113.178L83.9842 103.638C83.9842 103.638 62.7826 88.2777 55.8942 83.5084C49.7497 79.2541 49.0859 65.7213 49.0142 62.8484C49.0142 62.5087 48.8398 62.1925 48.5542 62.0084L28.8742 49.3284C8.47713 75.5567 24.6342 104.168 24.6342 104.168L21.4642 111.858"
                    ),
                leftPaperOutline =
                    svgPath(
                        "M23.3288 104.904M23.3288 104.904Q23.0525 104.415 22.6294 103.538Q21.7993 101.816 21.03 99.8276Q19.9561 97.0518 19.1518 94.1291Q18.1462 90.4751 17.6206 86.7952Q16.2793 77.4046 18.1393 68.6502Q20.4668 57.6956 27.6899 48.4075L29.6867 48.0676L49.3639 60.7524L48.5512 62.0131L49.3639 60.7524L48.5512 62.0131L49.3639 60.7524Q49.8924 61.093 50.1983 61.641Q50.502 62.1851 50.5176 62.8105L49.018 62.8479L50.5176 62.8105L49.018 62.8479L50.5176 62.8105Q50.5616 64.5757 50.7522 66.545Q51.0526 69.649 51.6454 72.3356Q52.4094 75.7982 53.5903 78.2324Q54.9426 81.0199 56.7517 82.2724L55.8978 83.5057L56.7517 82.2724L55.8978 83.5057L56.7517 82.2724Q61.0676 85.2607 73.3994 94.1425Q79.5621 98.5811 84.8616 102.422L85.3521 104.246L81.1131 113.785L79.7083 114.675L21.4282 113.353L20.0756 111.281L23.2484 103.594L24.6349 104.167L23.3288 104.904ZM25.941 103.429L26.0215 104.739L22.8487 112.426L21.4622 111.854L21.4962 110.354L79.7764 111.676L79.7423 113.175L78.3716 112.566L82.6106 103.027L83.9814 103.637L83.1011 104.851Q77.8051 101.013 71.6461 96.5768Q59.337 87.7113 55.0439 84.7389L55.0439 84.7389L55.0439 84.7389Q52.5872 83.038 50.8911 79.5419Q49.556 76.7897 48.7158 72.982Q48.084 70.1184 47.7661 66.834Q47.5652 64.7579 47.5185 62.8853L47.5185 62.8853L47.5185 62.8853Q47.5248 63.1361 47.7385 63.2738L47.7385 63.2738L47.7385 63.2738L28.0612 50.5891L28.874 49.3284L30.058 50.2492Q23.2581 58.9932 21.0738 69.2737Q19.3245 77.5074 20.5905 86.371Q21.0892 89.8625 22.0443 93.3331Q22.8085 96.1102 23.8279 98.7453Q24.5538 100.622 25.3316 102.234Q25.7124 103.024 25.941 103.429ZM26.0215 104.739L24.6349 104.167L25.941 103.429C26.0536 103.63 26.1173 103.845 26.1321 104.075C26.1456 104.304 26.1087 104.526 26.0215 104.739ZM29.6867 48.0676L28.874 49.3284L27.6899 48.4075C27.9309 48.1051 28.2417 47.9191 28.6222 47.8496C29.0043 47.7893 29.3591 47.8619 29.6867 48.0676ZM85.3521 104.246L83.9814 103.637L84.8616 102.422C85.1456 102.632 85.335 102.907 85.4299 103.247C85.5184 103.589 85.4925 103.922 85.3521 104.246ZM79.7083 114.675L79.7423 113.175L81.1131 113.785C80.9888 114.06 80.7995 114.279 80.5454 114.442C80.2894 114.602 80.0103 114.68 79.7083 114.675ZM20.0756 111.281L21.4622 111.854L21.4282 113.353C20.9088 113.328 20.5014 113.102 20.2061 112.674C19.9331 112.231 19.8896 111.767 20.0756 111.281Z"
                    ),
                rightPaper =
                    svgPath(
                        "M88.4851 109.734L115.775 111.854L119.745 106.024L134.565 84.3136C134.871 83.8656 134.765 83.2506 134.325 82.9336L116.065 69.7836C115.613 69.4583 114.985 69.5683 114.665 70.0236L91.3951 103.104L88.4851 109.734"
                    ),
                rightPaperOutline =
                    svgPath(
                        "M90.1712 102.245L113.436 69.1608L113.436 69.1608C114.236 68.0224 115.812 67.7569 116.942 68.5701L116.065 69.7874L116.942 68.5701L135.199 81.7177L135.199 81.7177L135.199 81.7177Q135.604 82.0092 135.865 82.4324Q136.117 82.8405 136.201 83.3137Q136.284 83.787 136.186 84.2566Q136.084 84.7434 135.803 85.1555L134.564 84.3101L135.803 85.1555L120.987 106.869L120.987 106.869L117.01 112.696L115.655 113.346L88.3688 111.225L87.1121 109.125L90.0252 102.504L90.1712 102.245ZM92.6253 103.971L91.3982 103.108L92.7712 103.712L89.8581 110.334L88.4851 109.729L88.6014 108.234L115.888 110.355L115.771 111.851L114.533 111.005L118.509 105.178L119.748 106.024L118.509 105.178L133.325 83.4646L133.325 83.4646Q133.211 83.6326 133.246 83.833Q133.281 84.0333 133.446 84.1522L134.323 82.9349L133.446 84.1522L134.323 82.9349L133.446 84.1522L115.188 71.0046L115.188 71.0046C115.414 71.1673 115.73 71.1142 115.89 70.8865L114.663 70.0237L115.89 70.8865L92.6253 103.971ZM90.1712 102.245L91.3982 103.108L90.0252 102.504L90.0854 102.367L90.1712 102.245ZM120.987 106.869L119.748 106.024L120.987 106.869L120.987 106.869L120.987 106.869ZM115.655 113.346L115.771 111.851L117.01 112.696L116.521 113.414L115.655 113.346ZM87.1121 109.125L88.4851 109.729L88.3688 111.225L86.2606 111.061L87.1121 109.125Z"
                    ),
                waveBase =
                    svgPath(
                        "M130.34 104.43C130.34 104.43 140.056 106.268 140.408 109.465C140.76 112.668 132.194 134.372 132.194 134.372L49.8082 164.836C49.8082 164.836 -3.71243 109.732 6.35568 104.43C16.4238 99.1345 26.2213 108.143 31.526 107.348C36.8307 106.553 37.0878 103.375 41.8647 103.375C46.6281 103.375 48.7527 108.143 54.3145 108.143C59.8763 108.143 60.1335 103.635 64.3826 103.635C68.6183 103.635 70.4722 107.348 75.7634 107.348C81.0681 107.348 82.3943 103.375 87.4283 103.375C92.4624 103.375 93.5179 107.348 98.8226 107.348C104.114 107.348 104.384 103.108 110.203 103.375C116.036 103.635 116.036 107.081 120.542 107.081C125.048 107.081 126.104 104.43 130.34 104.43Z"
                    ),
                shortLineA = svgPath("M84.7437 119.343L102.794 119.343"),
                shortLineB = svgPath("M41.7573 127.343L77.2341 127.343"),
                shortLineC = svgPath("M27 116L52 116"),
                handleA = svgPath("M103.056 86.9478L123.19 100.989"),
                handleB = svgPath("M109.944 77.1455L130.078 90.9216"),
                centerHand =
                    svgPath(
                        "M65.0008 64.0006L76.9277 73.0701C79.326 74.7494 79.9082 78.0518 78.2293 80.4495C76.551 82.8464 73.246 83.4282 70.8498 81.7504L55.1045 70.7254L65.0008 64.0006Z"
                    ),
                leftConnector = svgPath("M43.9775 103.903L57.2239 84.8284"),
                leftHandle = svgPath("M35.5 53.5671L49.8095 32.4924"),
            )
    }
}

private fun svgPath(pathData: String): Path =
    PathParser().parsePathString(pathData).toPath().apply { fillType = PathFillType.NonZero }
