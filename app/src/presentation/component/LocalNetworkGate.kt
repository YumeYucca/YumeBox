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

package com.github.yumeyucca.yumebox.presentation.component

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

private const val LocalNetworkPermission = Manifest.permission.ACCESS_LOCAL_NETWORK

/** Android 17 gates reaching another device on the local network behind a runtime permission. */
private fun needsLocalNetworkPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= 37 &&
        ContextCompat.checkSelfPermission(
            context,
            LocalNetworkPermission,
        ) != PackageManager.PERMISSION_GRANTED

/**
 * Runs an action that talks to another device on the LAN, asking for the local-network permission
 * first when the platform requires it.
 *
 * The action is parked while the dialog is up, so callers describe what they want to do once and let
 * the gate resume it — instead of each call site keeping its own "pending action" state next to its
 * own launcher.
 */
@Stable
internal class LocalNetworkGate(
    private val context: Context,
    private val requestPermission: (String) -> Unit,
) {
    private var pendingAction by mutableStateOf<(() -> Unit)?>(null)

    /** Runs [action]; when [requiresPermission] and the grant is missing, [action] waits for it. */
    fun run(requiresPermission: Boolean, action: () -> Unit) {
        if (!requiresPermission || !needsLocalNetworkPermission(context)) {
            action()
            return
        }
        // A newer request replaces the parked one rather than queueing behind it: the dialog is
        // showing for the newest tap, and replaying an older action afterwards would open a screen
        // the user has since moved on from.
        pendingAction = action
        requestPermission(LocalNetworkPermission)
    }

    /** The dialog answered: resume the parked action only when the permission was granted. */
    fun onPermissionResult(granted: Boolean) {
        val action = pendingAction
        pendingAction = null
        if (granted) action?.invoke()
    }
}

@Composable
internal fun rememberLocalNetworkGate(): LocalNetworkGate {
    val context = LocalContext.current
    // The launcher is created below the gate but the gate needs to launch it, so the gate calls
    // through a box the launcher fills in once it exists.
    val launcher = remember { mutableStateOf<(String) -> Unit>({}) }
    val gate = remember(context) { LocalNetworkGate(context) { permission -> launcher.value(permission) } }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            gate.onPermissionResult(granted)
        }
    // A bound method reference is a fresh object per evaluation, so it is remembered rather than
    // re-created on every recomposition that reaches this point.
    val launchPermission = remember<(String) -> Unit>(permissionLauncher) { permissionLauncher::launch }
    SideEffect { launcher.value = launchPermission }
    return gate
}
