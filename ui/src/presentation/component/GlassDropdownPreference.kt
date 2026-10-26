/* Copyright (c) YumeYucca 2026 - Present. SPDX-License-Identifier: AGPL-3.0-or-later */

package com.github.yumeyucca.yumebox.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.github.yumeyucca.yumebox.presentation.theme.LocalTopBarBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.glass.GlassDropdownPreference as MiuixGlassDropdownPreference
import top.yukonga.miuix.kmp.glass.GlassPopupDefaults
import top.yukonga.miuix.kmp.glass.GlassPopupSizing
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference

/** Uses the upstream popup only with a clean page recording; preserves the old window otherwise. */
@Composable
fun GlassDropdownPreference(
    items: List<String>,
    selectedIndex: Int,
    title: String,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    showValue: Boolean = true,
    maxHeight: Dp? = null,
) {
    val backdrop = LocalTopBarBackdrop.current
    if (backdrop != null && isRuntimeShaderSupported()) {
        MiuixGlassDropdownPreference(
            items = items,
            selectedIndex = selectedIndex,
            title = title,
            summary = summary,
            backdrop = backdrop,
            modifier = modifier,
            enabled = enabled,
            showValue = showValue,
            sizing = GlassPopupSizing(maxHeight = maxHeight ?: GlassPopupDefaults.MaxHeight),
            onSelectedIndexChange = onSelectedIndexChange,
        )
    } else {
        WindowDropdownPreference(
            items = items,
            selectedIndex = selectedIndex,
            title = title,
            summary = summary,
            modifier = modifier,
            enabled = enabled && items.isNotEmpty(),
            showValue = showValue,
            maxHeight = maxHeight,
            onSelectedIndexChange = onSelectedIndexChange,
        )
    }
}
