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

package com.github.yumeyucca.yumebox.screen.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import com.github.yumeyucca.yumebox.presentation.component.GlassScaffold
import com.github.yumeyucca.yumebox.presentation.component.StandalonePageList
import com.github.yumeyucca.yumebox.presentation.component.TopBar
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior

/**
 * The shared shell of the per-run-mode "service config" sub-pages.
 *
 * Tun, VpnService and eBPF each push the same page: one titled top bar over a scrollable list
 * whose padding already accounts for the floating bottom bar. Only the rows differ, so the shell
 * lives here instead of three times in the screens.
 */
@Composable
internal fun ServiceOptionsPage(
    title: String,
    content: LazyListScope.() -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    // GlassScaffold, not a plain Scaffold: it records the page backdrop the dropdown popups sample.
    // Without it the stack/DNS/Wi-Fi pickers silently fall back to the legacy window popup.
    GlassScaffold(
        topBar = {
            TopBar(
                title = title,
                scrollBehavior = scrollBehavior,
            )
        }
    ) { innerPadding ->
        StandalonePageList(
            scrollBehavior = scrollBehavior,
            scaffoldPadding = innerPadding,
            content = content,
        )
    }
}
