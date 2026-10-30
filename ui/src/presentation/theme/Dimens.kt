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

package com.github.yumeyucca.yumebox.presentation.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object UiDp {
    val dp0: Dp = 0.dp
    val dp0_5: Dp = 0.5.dp
    val dp1: Dp = 1.dp
    val dp2: Dp = 2.dp
    val dp3: Dp = 3.dp
    val dp4: Dp = 4.dp
    val dp5: Dp = 5.dp
    val dp6: Dp = 6.dp
    val dp7: Dp = 7.dp
    val dp8: Dp = 8.dp
    val dp9: Dp = 9.dp
    val dp10: Dp = 10.dp
    val dp12: Dp = 12.dp
    val dp13: Dp = 13.dp
    val dp14: Dp = 14.dp
    val dp16: Dp = 16.dp
    val dp18: Dp = 18.dp
    val dp20: Dp = 20.dp
    val dp22: Dp = 22.dp
    val dp24: Dp = 24.dp
    val dp28: Dp = 28.dp
    val dp30: Dp = 30.dp
    val dp32: Dp = 32.dp
    val dp35: Dp = 35.dp
    val dp40: Dp = 40.dp
    val dp42: Dp = 42.dp
    val dp44: Dp = 44.dp
    val dp45: Dp = 45.dp
    val dp46: Dp = 46.dp
    val dp48: Dp = 48.dp
    val dp52: Dp = 52.dp
    val dp56: Dp = 56.dp
    val dp72: Dp = 72.dp
    val dp84: Dp = 84.dp
    val dp100: Dp = 100.dp
    val dp122: Dp = 122.dp
    val dp176: Dp = 176.dp
    val dp188: Dp = 188.dp
    val dp200: Dp = 200.dp
    val dp220: Dp = 220.dp
    val dp260: Dp = 260.dp
    val dp280: Dp = 280.dp
    val dp320: Dp = 320.dp
    val dp360: Dp = 360.dp
    val dp420: Dp = 420.dp
    val dp560: Dp = 560.dp
    val dp999: Dp = 999.dp
}

data class Spacing(
    val space0: Dp = UiDp.dp0,
    val space2: Dp = UiDp.dp2,
    val space3: Dp = UiDp.dp3,
    val space4: Dp = UiDp.dp4,
    val space6: Dp = UiDp.dp6,
    val space8: Dp = UiDp.dp8,
    val space10: Dp = UiDp.dp10,
    val space12: Dp = UiDp.dp12,
    val space14: Dp = UiDp.dp14,
    val space16: Dp = UiDp.dp16,
    val space18: Dp = UiDp.dp18,
    val space20: Dp = UiDp.dp20,
    val space24: Dp = UiDp.dp24,
    val space28: Dp = UiDp.dp28,
    val space32: Dp = UiDp.dp32,
    val screenHorizontal: Dp = UiDp.dp12,
)

data class Radii(
    val radius4: Dp = UiDp.dp4,
    val radius8: Dp = UiDp.dp8,
    val radius12: Dp = UiDp.dp12,
    val radius14: Dp = UiDp.dp14,
    val radius24: Dp = UiDp.dp24,
    val full: Dp = UiDp.dp999,
)

data class Sizes(
    val listItemVerticalMinimal: Dp = UiDp.dp5,
    val searchBarTopPadding: Dp = UiDp.dp12,
    val searchBarBottomPadding: Dp = UiDp.dp6,
    val iconBadgeMedium: Dp = UiDp.dp42,
    val searchIconTouchTarget: Dp = UiDp.dp44,
    val searchFieldMinHeight: Dp = UiDp.dp45,
    val homeIdleTopPadding: Dp = UiDp.dp122,
    val heroStartButtonSize: Dp = UiDp.dp72,
    val nodeDelayColumnWidth: Dp = UiDp.dp72,
    val trafficChartHeight: Dp = UiDp.dp188,
    val trafficBarChartHeight: Dp = UiDp.dp260,
    val trafficBarWidth: Dp = UiDp.dp22,
    val trafficBarLabelHeight: Dp = UiDp.dp18,
    val settingsIconGlyphSize: Dp = UiDp.dp22,
    val statusCapsuleHeight: Dp = UiDp.dp28,
    val thinDividerThickness: Dp = UiDp.dp0_5,
    val profileSettingsListMaxHeight: Dp = UiDp.dp420,
    val connectionLeadingIconSize: Dp = UiDp.dp44,
    val connectionLeadingIconCornerRadius: Dp = UiDp.dp14,
    val nodeCardBorderWidth: Dp = UiDp.dp1,
    val nodeCardPaddingHorizontal: Dp = UiDp.dp16,
    val nodeCardPaddingVertical: Dp = UiDp.dp12,
    val nodeCardContentGap: Dp = UiDp.dp16,
    val nodeCardTitleGap: Dp = UiDp.dp6,
    val nodeLargeIconSize: Dp = UiDp.dp44,
    val nodeLargeIconCornerRadius: Dp = UiDp.dp14,
    val nodeLargeIconFlagSize: Dp = UiDp.dp28,
    val nodeTagIconSize: Dp = UiDp.dp9,
    val nodeChainNodeHorizontalPadding: Dp = UiDp.dp8,
    val nodeChainNodeVerticalPadding: Dp = UiDp.dp4,
    val nodeChainIndicatorSize: Dp = UiDp.dp6,
)

data class Opacity(
    val ambientShadow: Float = 0.015f,
    val ultraSubtle: Float = 0.045f,
    val verySubtle: Float = 0.08f,
    val subtle: Float = 0.10f,
    val subtleStrong: Float = 0.12f,
    val softOverlay: Float = 0.16f,
    val surfaceSoft: Float = 0.24f,
    val outline: Float = 0.30f,
    val muted: Float = 0.35f,
    val disabled: Float = 0.38f,
    val secondaryText: Float = 0.60f,
    val subtleText: Float = 0.70f,
    val mediumStrong: Float = 0.75f,
    val surfaceVariant: Float = 0.82f,
    val prominentText: Float = 0.90f,
)

object AppTheme {
    // These are constants: YumeTheme has no per-tree override for dimensions, so exposing
    // them as composition locals only charged every read for a lookup that always returned
    // the same instance (and kept non-composable code from reading the tokens at all).
    val spacing: Spacing = Spacing()
    val radii: Radii = Radii()
    val sizes: Sizes = Sizes()
    val opacity: Opacity = Opacity()

    /** Theme-wide palette; a constant for the same reason. */
    val colors: AppColors = AppColors()
}
