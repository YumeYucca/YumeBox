/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 */

@file:Suppress("FunctionName")

package com.github.yumeyucca.yumebox.presentation.component


import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * NetEase / YumeFuwa style dual-pane shell.
 *
 * Left pane holds the main pager at a phone-like width so Moe home is not stretched. Right pane
 * hosts detail destinations. The center divider may be shown and optionally dragged.
 *
 * Width policy:
 * - Keep `leftRatio` of free width (container minus divider) so window resize scales both panes.
 * - Clamp the live left width into absolute `minLeftWidth, maxLeftWidth` while reserving
 *   `minRightWidth`. Absolute bounds are the source of truth; the old fraction-only clamp could
 *   collapse drag range to zero when `minFraction * width > maxLeftWidth` on wide windows.
 */
@Composable
fun DualPaneLayout(
    left: @Composable () -> Unit,
    right: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    initialLeftFraction: Float = 0.42f,
    minLeftWidth: Dp = 280.dp,
    maxLeftWidth: Dp = 420.dp,
    minRightWidth: Dp = 320.dp,
    onLeftFractionChange: ((Float) -> Unit)? = null,
) {
    // Ratio of free width. Survives window resize so both panes scale together, and survives
    // configuration changes via rememberSaveable; [onLeftFractionChange] mirrors it to storage
    // so a fresh process restores the user's divider position (issue #151 item 1).
    var leftRatio by rememberSaveable {
        mutableFloatStateOf(initialLeftFraction.coerceIn(0.2f, 0.8f))
    }
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val containerWidthPx = with(density) { maxWidth.toPx().coerceAtLeast(1f) }
        val dividerPx = with(density) { DividerHitWidth.toPx() }
        val freeWidthPx = (containerWidthPx - dividerPx).coerceAtLeast(1f)

        val rawMinLeftPx = with(density) { minLeftWidth.toPx() }
        val rawMaxLeftPx = with(density) { maxLeftWidth.toPx() }
        val rawMinRightPx = with(density) { minRightWidth.toPx() }

        // Soft-shrink preferred floors when the window cannot host both.
        val minsTotal = rawMinLeftPx + rawMinRightPx
        val minScale =
            if (minsTotal > freeWidthPx && minsTotal > 0f) freeWidthPx / minsTotal else 1f
        val minLeftPx = rawMinLeftPx * minScale
        val minRightPx = rawMinRightPx * minScale

        val minBoundPx = minLeftPx.coerceIn(0f, freeWidthPx)
        val maxFromRight = (freeWidthPx - minRightPx).coerceAtLeast(0f)
        val maxBoundPx = maxOf(minBoundPx, minOf(rawMaxLeftPx, maxFromRight, freeWidthPx))

        val desiredLeftPx = (leftRatio * freeWidthPx).coerceIn(minBoundPx, maxBoundPx)
        val leftWidthDp = with(density) { desiredLeftPx.toDp() }
        val canDrag = maxBoundPx > minBoundPx + 0.5f

        val dragState = rememberDraggableState { deltaPx ->
            if (!canDrag) return@rememberDraggableState
            val signed = if (layoutDirection == LayoutDirection.Rtl) -deltaPx else deltaPx
            val nextLeft = (leftRatio * freeWidthPx + signed).coerceIn(minBoundPx, maxBoundPx)
            leftRatio = (nextLeft / freeWidthPx).coerceIn(0.05f, 0.95f)
            onLeftFractionChange?.invoke(leftRatio)
        }

        Row(modifier = Modifier.fillMaxSize()) {
            Box(Modifier
                .width(leftWidthDp)
                .fillMaxHeight()) {
                left()
            }

            Box(
                modifier =
                    Modifier
                        .fillMaxHeight()
                        .width(DividerHitWidth)
                        .then(
                            if (canDrag) {
                                Modifier.draggable(
                                    state = dragState,
                                    orientation = Orientation.Horizontal,
                                )
                            } else {
                                Modifier
                            }
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(MiuixTheme.colorScheme.outline.copy(alpha = 0.55f))
                )
            }


            Box(Modifier
                .weight(1f)
                .fillMaxHeight()) {
                right()
            }
        }
    }
}

/** Grab area of the divider: wide enough to hit comfortably, narrow enough not to steal taps. */
private val DividerHitWidth = 16.dp
