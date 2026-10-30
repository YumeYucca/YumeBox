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

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference

@Composable
fun PreferenceSwitchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    summary: String? = null,
    enabled: Boolean = true,
) {
    SwitchPreference(
        title = title,
        summary = summary,
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
    )
}

@Composable
fun PreferenceArrowItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    holdDownState: Boolean = false,
    endActions: @Composable (RowScope.() -> Unit)? = null,
    bottomAction: @Composable (() -> Unit)? = null,
) {
    ArrowPreference(
        title = title,
        modifier = modifier,
        summary = summary,
        enabled = enabled,
        holdDownState = holdDownState,
        endActions = endActions ?: {},
        bottomAction = bottomAction,
        onClick = onClick,
    )
}

/**
 * A preference row that picks one entry of [values]. Only the ordered entries and their labels are
 * given, so a label can never drift out of step with the value it describes.
 */
@Composable
fun <T> PreferenceEnumItem(
    title: String,
    currentValue: T,
    values: List<T>,
    onValueChange: (T) -> Unit,
    label: (T) -> String,
    summary: String? = null,
) {
    GlassDropdownPreference(
        title = title,
        summary = summary,
        options = values.map { GlassDropdownOption(it, label(it)) },
        selected = currentValue,
        onSelected = onValueChange,
    )
}
