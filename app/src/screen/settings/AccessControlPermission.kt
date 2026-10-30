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

package com.github.yumeyucca.yumebox.screen.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/** The MIUI package-visibility permission the app list needs before it can enumerate packages. */
internal const val GET_INSTALLED_APPS_PERMISSION = "com.android.permission.GET_INSTALLED_APPS"

/**
 * Requests [GET_INSTALLED_APPS_PERMISSION] whenever [needsPermission] becomes true.
 *
 * Both access-control screens render the same app list and therefore need the same request; owning
 * it here keeps the launcher, its effect key and the permission name in exactly one place.
 */
@Composable
internal fun InstalledAppsPermissionRequest(
    needsPermission: Boolean,
    onGranted: () -> Unit,
) {
    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { granted -> if (granted) onGranted() },
        )

    LaunchedEffect(needsPermission) {
        if (needsPermission) launcher.launch(GET_INSTALLED_APPS_PERMISSION)
    }
}
