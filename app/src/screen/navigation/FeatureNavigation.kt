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

package com.github.yumeyucca.yumebox.screen.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.github.yumeyucca.yumebox.WebViewActivity
import com.github.yumeyucca.yumebox.common.util.DashboardShortcutHelper
import com.github.yumeyucca.yumebox.common.util.buildRemotePanelUrl
import com.github.yumeyucca.yumebox.data.store.RemoteControllerStore
import com.github.yumeyucca.yumebox.presentation.component.Navigator
import com.github.yumeyucca.yumebox.presentation.screen.FeatureContent
import com.github.yumeyucca.yumebox.screen.feature.PanelShortcutDialog
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun FeatureScreen(navigator: Navigator) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val remoteControllerStore = koinInject<RemoteControllerStore>()

    var shortcutTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    var shortcutDialogVisible by remember { mutableStateOf(false) }

    FeatureContent(
        onOpenInAppUrl = { url -> WebViewActivity.start(context, url) },
        onCreatePanelShortcut = { url, label ->
            val targetUrl =
                remoteControllerStore.activeBackend()?.let { buildRemotePanelUrl(url, it) } ?: url
            shortcutTarget = targetUrl to label
            shortcutDialogVisible = true
        },
    )

    shortcutTarget?.let { (url, label) ->
        PanelShortcutDialog(
            show = shortcutDialogVisible,
            url = url,
            defaultLabel = label,
            onDismiss = { shortcutDialogVisible = false },
            onConfirm = { name, iconUri ->
                scope.launch {
                    DashboardShortcutHelper.createPanelShortcut(context, url, name, iconUri)
                }
                shortcutDialogVisible = false
            },
            onDismissFinished = { shortcutTarget = null },
        )
    }
}
