/* Copyright (c) YumeYucca 2026 - Present. SPDX-License-Identifier: AGPL-3.0-or-later */

package com.github.yumeyucca.yumebox.presentation.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.yumeyucca.yumebox.presentation.theme.LocalGlassBackdrop
import com.github.yumeyucca.yumebox.presentation.theme.LocalTopBarBackdrop
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.glass.GlassDropdownPreference as MiuixGlassDropdownPreference
import top.yukonga.miuix.kmp.glass.GlassPopupDefaults
import top.yukonga.miuix.kmp.glass.GlassPopupSizing
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference

/**
 * Leading-icon metrics of the node-actions popup rows: an 18dp glyph, then a 15dp gap to the
 * label. Both popup paths below use them so this list lines up with that menu.
 */
private val OptionIconSize: Dp = 18.dp
private val OptionIconGap: Dp = 15.dp

/**
 * Uses the upstream popup only with a clean page recording; preserves the old window otherwise.
 *
 * @param icons Optional leading icon per item, aligned with [items] by index.
 * @param itemEnabled Optional per-item enabled state, aligned with [items] by index. A disabled
 *   item is greyed out and ignores taps while the row itself stays usable.
 */
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
    icons: List<ImageVector>? = null,
    itemEnabled: List<Boolean>? = null,
) {
    val backdrop = LocalGlassBackdrop.current ?: LocalTopBarBackdrop.current
    if (backdrop != null && isRuntimeShaderSupported()) {
        MiuixGlassDropdownPreference(
            items = items,
            icons = icons,
            itemEnabled = itemEnabled,
            itemIconSize = OptionIconSize,
            itemIconGap = OptionIconGap,
            selectedIndex = selectedIndex,
            title = title,
            summary = summary,
            backdrop = backdrop,
            modifier = modifier,
            enabled = enabled,
            showValue = showValue,
            sizing =
                GlassPopupSizing(
                    minWidth = 188.dp,
                    maxWidth = 188.dp,
                    maxHeight = maxHeight ?: GlassPopupDefaults.MaxHeight,
                ),
            onSelectedIndexChange = onSelectedIndexChange,
        )
    } else {
        val entry =
            DropdownEntry(
                items =
                    items.mapIndexed { index, text ->
                        val optionIcon: (@Composable (Modifier) -> Unit)? =
                            icons?.getOrNull(index)?.let { icon ->
                                // The window popup hands over a 26dp icon cell with its own
                                // trailing padding, which would make the gap differ from the
                                // glass list; lay the glyph out here instead.
                                { _: Modifier ->
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier =
                                            Modifier
                                                .padding(end = OptionIconGap)
                                                .size(OptionIconSize),
                                    )
                                }
                            }
                        DropdownItem(
                            text = text,
                            enabled = itemEnabled?.getOrNull(index) ?: true,
                            selected = index == selectedIndex,
                            icon = optionIcon,
                            onClick = { onSelectedIndexChange(index) },
                        )
                    },
            )
        WindowDropdownPreference(
            entry = entry,
            title = title,
            summary = summary,
            modifier = modifier,
            enabled = enabled && items.isNotEmpty(),
            showValue = showValue,
            maxHeight = maxHeight,
        )
    }
}
