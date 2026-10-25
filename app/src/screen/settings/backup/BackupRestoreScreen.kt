/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License.
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

package com.github.yumeyucca.yumebox.screen.settings.backup

import androidx.compose.runtime.Composable
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import com.github.yumeyucca.yumebox.presentation.component.ScreenLazyColumn
import com.github.yumeyucca.yumebox.presentation.component.TopBar
import com.github.yumeyucca.yumebox.presentation.component.combinePaddingValues
import com.github.yumeyucca.yumebox.presentation.component.rememberStandalonePageMainPadding
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold

/** Standalone backup & restore page, moved out of the Lab screen. */
@Composable
fun BackupRestoreScreen() {
    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        topBar = { TopBar(title = YumeTxt.Settings.Section.BackupRestore, scrollBehavior = scrollBehavior) }
    ) { innerPadding ->
        val mainLikePadding = rememberStandalonePageMainPadding()
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = combinePaddingValues(innerPadding, mainLikePadding),
        ) {
            item {
                BackupRestoreSection()
            }
        }
    }
}
