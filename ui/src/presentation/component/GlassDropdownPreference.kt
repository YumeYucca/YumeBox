/* Copyright (c) YumeYucca 2026 - Present. SPDX-License-Identifier: AGPL-3.0-or-later */

package com.github.yumeyucca.yumebox.presentation.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import com.github.yumeyucca.yumebox.presentation.theme.glassSurfaceBackdrop
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.glass.GlassDropdownPreference as MiuixGlassDropdownPreference
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference

/**
 * One option in [GlassDropdownPreference].
 *
 * The option carries the [value] it stands for, so the row selects a model instead of an index and
 * no call site has to translate its own enum through `indexOf`/`getOrNull` first.
 */
data class GlassDropdownOption<T>(
    val value: T,
    val label: String,
    val icon: ImageVector? = null,
    val enabled: Boolean = true,
)

/**
 * Uses the upstream popup only with a clean page recording; preserves the old window otherwise.
 *
 * @param options Options in display order. A disabled option is greyed out and ignores taps while
 *   the row itself stays usable.
 * @param selected The option whose value matches this shows as selected; a value that no option
 *   carries leaves every row unselected rather than pointing at the wrong one.
 */
@Composable
fun <T> GlassDropdownPreference(
    options: List<GlassDropdownOption<T>>,
    selected: T?,
    title: String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    maxHeight: Dp? = null,
) {
    val backdrop = glassSurfaceBackdrop()
    val selectedIndex = options.indexOfFirst { it.value == selected }
    // An empty dropdown has nothing to offer, so it reads as unavailable in both branches.
    val rowEnabled = enabled && options.isNotEmpty()
    if (backdrop != null && isRuntimeShaderSupported()) {
        MiuixGlassDropdownPreference(
            items = options.map { it.label },
            icons = options.map { it.icon },
            itemEnabled = options.map { it.enabled },
            itemIconSize = GlassPopupMenuDefaults.ItemIconSize,
            itemIconGap = GlassPopupMenuDefaults.ItemIconGap,
            selectedIndex = selectedIndex,
            title = title,
            summary = summary,
            backdrop = backdrop,
            modifier = modifier,
            enabled = rowEnabled,
            sizing = maxHeight?.let { GlassPopupMenuSizing.copy(maxHeight = it) } ?: GlassPopupMenuSizing,
            onSelectedIndexChange = { index ->
                options.getOrNull(index)?.let { onSelected(it.value) }
            },
        )
    } else {
        val entry =
            DropdownEntry(
                items =
                    options.mapIndexed { index, option ->
                        val optionIcon: (@Composable (Modifier) -> Unit)? =
                            option.icon?.let { icon ->
                                // The window popup hands over a 26dp icon cell with its own
                                // trailing padding, which would make the gap differ from the
                                // glass list; lay the glyph out here instead.
                                { _: Modifier ->
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier =
                                            Modifier
                                                .padding(end = GlassPopupMenuDefaults.ItemIconGap)
                                                .size(GlassPopupMenuDefaults.ItemIconSize),
                                    )
                                }
                            }
                        DropdownItem(
                            text = option.label,
                            enabled = option.enabled,
                            selected = index == selectedIndex,
                            icon = optionIcon,
                            onClick = { onSelected(option.value) },
                        )
                    },
            )
        WindowDropdownPreference(
            entry = entry,
            title = title,
            summary = summary,
            modifier = modifier,
            enabled = rowEnabled,
            maxHeight = maxHeight,
        )
    }
}
