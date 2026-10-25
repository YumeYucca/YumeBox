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

package com.github.yumeyucca.yumebox.screen.extensions

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.github.yumeyucca.yumebox.common.util.requiresLocalNetworkPermission
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import com.github.yumeyucca.yumebox.data.model.RemoteBackend
import com.github.yumeyucca.yumebox.presentation.component.combinePaddingValues
import com.github.yumeyucca.yumebox.presentation.component.rememberStandalonePageMainPadding
import com.github.yumeyucca.yumebox.presentation.component.ScreenLazyColumn
import com.github.yumeyucca.yumebox.presentation.component.TopBar
import com.github.yumeyucca.yumebox.screen.feature.RemoteControllerSection
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold

/** Standalone extensions page: the remote controller section moved out of the Lab screen. */
@Composable
fun ExtensionsScreen() {
    val scrollBehavior = MiuixScrollBehavior()
    val context = LocalContext.current

    var pendingLocalNetworkAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val localNetworkPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            pendingLocalNetworkAction?.takeIf { granted }?.invoke()
            pendingLocalNetworkAction = null
        }
    val requestLocalNetworkPermission: (RemoteBackend, () -> Unit) -> Unit = { backend, action ->
        val permission = "android.permission.ACCESS_LOCAL_NETWORK"
        if (
            Build.VERSION.SDK_INT < 37 ||
            !backend.requiresLocalNetworkPermission() ||
            ContextCompat.checkSelfPermission(context, permission) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            action()
        } else {
            pendingLocalNetworkAction = action
            localNetworkPermissionLauncher.launch(permission)
        }
    }

    Scaffold(
        topBar = { TopBar(title = YumeTxt.Settings.Section.Extensions, scrollBehavior = scrollBehavior) }
    ) { innerPadding ->
        val mainLikePadding = rememberStandalonePageMainPadding()
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = combinePaddingValues(innerPadding, mainLikePadding),
        ) {
            item {
                RemoteControllerSection(
                    onRequestLocalNetworkPermission = requestLocalNetworkPermission,
                )
            }
        }
    }
}
