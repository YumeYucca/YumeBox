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

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.theme.resolve
import top.yukonga.miuix.kmp.basic.SmallTitle

@Composable
internal fun smallTitleColor(): Color = AppTheme.colors.title.resolve()

/**
 * App-level wrapper around Miuix `Title`. Keeps YumeBox default spacing while avoiding direct Miuix
 * usage at call sites.
 */
@Composable
fun Title(text: String, topPadding: Dp = AppTheme.spacing.space8) {
    SmallTitle(
        modifier = Modifier.padding(top = topPadding),
        text = text,
        textColor = smallTitleColor(),
    )
}
