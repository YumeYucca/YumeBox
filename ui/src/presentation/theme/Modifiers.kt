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

@file:Suppress("UnusedSymbol")

package com.github.yumeyucca.yumebox.presentation.theme

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier

/**
 * Symmetric horizontal inset for card bodies. Applied by [com.github.yumeyucca.yumebox.presentation.component.AppCard]
 * so callers never repeat the screen-horizontal token.
 */
fun Modifier.horizontalPadding(): Modifier =
    this.padding(start = AppTheme.spacing.screenHorizontal, end = AppTheme.spacing.screenHorizontal)
