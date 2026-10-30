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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.presentation.theme.horizontalPadding
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.basic.Card as MiuixCard

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = UiDp.dp24,
    insideMargin: PaddingValues = PaddingValues(UiDp.dp0),
    applyHorizontalPadding: Boolean = true,
    onClick: (() -> Unit)? = null,
    colors: CardColors = CardDefaults.defaultColors(),
    pressFeedbackType: PressFeedbackType = PressFeedbackType.None,
    content: @Composable () -> Unit,
) {
    val resolvedModifier = if (applyHorizontalPadding) modifier.horizontalPadding() else modifier
    // Miuix ships two Card overloads (inert and pressable). Calling the pressable one without
    // onClick would be ambiguous at the call site, so the branch stays.
    if (onClick == null) {
        MiuixCard(
            modifier = resolvedModifier,
            cornerRadius = cornerRadius,
            insideMargin = insideMargin,
            colors = colors,
            content = { content() },
        )
    } else {
        MiuixCard(
            modifier = resolvedModifier,
            cornerRadius = cornerRadius,
            insideMargin = insideMargin,
            onClick = onClick,
            colors = colors,
            pressFeedbackType = pressFeedbackType,
            content = { content() },
        )
    }
}
