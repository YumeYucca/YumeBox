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

/** What the panel-shortcut dialog is being opened for. */
@Immutable
private data class PanelShortcutRequest(val url: String, val label: String)

@Composable
fun FeatureScreen(navigator: Navigator) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val remoteControllerStore = koinInject<RemoteControllerStore>()

    // The payload outlives the dialog: the sheet keeps drawing while it animates out, so it may
    // only be cleared from [PanelShortcutDialog.onDismissFinished].
    var shortcutRequest by remember { mutableStateOf<PanelShortcutRequest?>(null) }
    var shortcutDialogVisible by remember { mutableStateOf(false) }

    FeatureContent(
        onOpenInAppUrl = { url -> WebViewActivity.start(context, url) },
        onCreatePanelShortcut = { url, label ->
            val targetUrl =
                remoteControllerStore.activeBackend()?.let { buildRemotePanelUrl(url, it) } ?: url
            shortcutRequest = PanelShortcutRequest(url = targetUrl, label = label)
            shortcutDialogVisible = true
        },
    )

    shortcutRequest?.let { request ->
        PanelShortcutDialog(
            show = shortcutDialogVisible,
            url = request.url,
            defaultLabel = request.label,
            onDismiss = { shortcutDialogVisible = false },
            onConfirm = { name, iconUri ->
                scope.launch {
                    DashboardShortcutHelper.createPanelShortcut(
                        context = context,
                        url = request.url,
                        label = name,
                        iconUri = iconUri,
                    )
                }
                shortcutDialogVisible = false
            },
            onDismissFinished = { shortcutRequest = null },
        )
    }
}
