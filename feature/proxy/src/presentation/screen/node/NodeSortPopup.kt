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

package com.github.yumeyucca.yumebox.presentation.screen.node


import androidx.compose.runtime.Composable
import com.github.yumeyucca.yumebox.data.model.ProxySortMode
import tf.gal.yumebox.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.overlay.OverlayCascadingListPopup

internal val NodeSortModes =
    listOf(ProxySortMode.DEFAULT, ProxySortMode.BY_NAME, ProxySortMode.BY_LATENCY)

@Composable
internal fun NodeSortPopup(
    show: Boolean,
    onDismiss: () -> Unit,
    sortMode: ProxySortMode,
    alignment: PopupPositionProvider.Align = PopupPositionProvider.Align.Start,
    onLocateCurrentProxy: (() -> Unit)? = null,
    onSortSelected: (ProxySortMode) -> Unit,
) {
    val entries =
        buildList {
            onLocateCurrentProxy?.let { locateCurrentProxy ->
                add(
                    DropdownEntry(
                        items =
                            listOf(
                                DropdownItem(
                                    text = YumeTxt.Proxy.Action.LocateCurrent,
                                    onClick = locateCurrentProxy,
                                )
                            ),
                    )
                )
            }
            add(
                DropdownEntry(
                    items =
                        NodeSortModes.map { mode ->
                            DropdownItem(
                                text = mode.displayName,
                                selected = mode == sortMode,
                                onClick = {
                                    if (mode != sortMode) onSortSelected(mode)
                                },
                            )
                        },
                )
            )
        }

    OverlayCascadingListPopup(
        show = show,
        entries = entries,
        alignment = alignment,
        onDismissRequest = onDismiss,
    )
}
