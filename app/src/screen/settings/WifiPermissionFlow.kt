/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 */

package com.github.yumeyucca.yumebox.screen.settings

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat

/** The Wi-Fi automation action that is waiting for location access. */
internal enum class WifiPermissionAction { Enable, Scan }

/**
 * The whole permission choreography of the Wi-Fi automation section: which action is waiting, which
 * dialog is on screen, and the one ladder that turns a permission outcome into either "run the
 * action" or "explain what is still missing".
 *
 * The section composable collects state and renders; every branch that used to sit in five sibling
 * locals and three launcher callbacks lives here instead.
 */
@Stable
internal class WifiPermissionFlow(
    private val context: Context,
    private val locationRequested: () -> Boolean,
    private val onEnable: () -> Unit,
    private val onScan: () -> Unit,
    private val onPermissionAsked: () -> Unit,
) {
    private val activity = context as? Activity

    private var pendingAction by mutableStateOf<WifiPermissionAction?>(null)

    /**
     * `show` of the permission dialog. Only [showPrompt] raises it; it is lowered before the next
     * step starts, so every transition out of the dialog is animated.
     */
    var promptVisible by mutableStateOf(false)
        private set

    /**
     * The prompt last shown. It deliberately outlives [promptVisible] so the dialog still has text
     * to render during its 260ms exit tween; the initial placeholder is never rendered, because
     * [promptVisible] only becomes true through [showPrompt].
     */
    var prompt by mutableStateOf(WifiPermissionPrompt.Explanation)
        private set

    private lateinit var permissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>

    private lateinit var appSettingsLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>

    private lateinit var locationSettingsLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>

    /**
     * Takes the launchers [rememberWifiPermissionFlow] created. They cannot be constructor arguments
     * because each result callback dispatches back into this holder, so they can only exist once it
     * does. Re-attaching on recomposition is a no-op: the launchers are remembered, so the same
     * three instances are assigned again.
     */
    fun attachLaunchers(
        permissions: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>,
        appSettings: ManagedActivityResultLauncher<Intent, ActivityResult>,
        locationSettings: ManagedActivityResultLauncher<Intent, ActivityResult>,
    ) {
        permissionLauncher = permissions
        appSettingsLauncher = appSettings
        locationSettingsLauncher = locationSettings
    }

    /**
     * Asks for whatever [action] is still missing. Location comes first because reading SSIDs needs
     * it on every version that ships runtime location permissions; after that the action either
     * runs or the user is told why it cannot.
     */
    fun request(action: WifiPermissionAction) {
        pendingAction = action
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O && !systemLocationEnabled()) {
            showPrompt(WifiPermissionPrompt.Location)
        } else {
            resumePending(hasSsidPermission()) {
                // Only a request the user has already seen can be permanently denied, so the
                // "open settings" wording is only offered after one was actually made.
                if (locationRequested() && rationaleSuppressed()) {
                    WifiPermissionPrompt.Settings
                } else {
                    WifiPermissionPrompt.Explanation
                }
            }
        }
    }

    /** Confirm action of [prompt]; every branch first hides the dialog it came from. */
    fun onPromptConfirmed() {
        when (prompt) {
            WifiPermissionPrompt.Denied -> showPrompt(WifiPermissionPrompt.Explanation)

            // Only the explanation actually launches the OS grant request, so only it counts as
            // "asked"; the approximate prompt is informational and re-asks every time.
            WifiPermissionPrompt.Explanation,
            WifiPermissionPrompt.Approximate -> {
                promptVisible = false
                if (prompt == WifiPermissionPrompt.Explanation) onPermissionAsked()
                permissionLauncher.launch(requiredLocationPermissions())
            }

            WifiPermissionPrompt.Settings -> {
                promptVisible = false
                appSettingsLauncher.launch(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null),
                    )
                )
            }

            WifiPermissionPrompt.Location -> {
                promptVisible = false
                locationSettingsLauncher.launch(
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                )
            }
        }
    }

    /** Dismisses the dialog without acting; the action it was waiting for is dropped with it. */
    fun dismissPrompt() {
        promptVisible = false
        pendingAction = null
    }

    /** Back from the system location screen: the action can run only if both prerequisites hold. */
    fun onLocationSettingsResult() {
        if (pendingAction != null && systemLocationEnabled()) {
            resumePending(hasSsidPermission()) { WifiPermissionPrompt.Explanation }
        }
    }

    /**
     * Back from the app-settings screen, where the permission can only have been granted by hand.
     * A denial stays silent: the user is already on the screen that explains it, and re-prompting
     * from there would loop, so the guard is also the whole decision.
     */
    fun onAppSettingsResult() {
        if (pendingAction != null && hasSsidPermission() && systemLocationEnabled()) {
            completePendingAction()
        }
    }

    /** The OS answered the grant request: a grant runs the action, a denial names the next step. */
    fun onPermissionsResult() {
        resumePending(hasSsidPermission()) {
            when {
                // A coarse-only grant still identifies the network from S on, so say so before
                // falling back to the denial wording.
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ->
                    WifiPermissionPrompt.Approximate

                rationaleSuppressed() -> WifiPermissionPrompt.Settings
                else -> WifiPermissionPrompt.Denied
            }
        }
    }

    /**
     * The one ladder this class exists for: once [prerequisite] holds, run the action that was
     * waiting; until then, show [blockedPrompt]. Each return path contributes only the prerequisite
     * it can prove and the prompt that fits where the user just came from, instead of repeating the
     * two-branch ladder in every launcher. [blockedPrompt] stays lazy because picking it queries the
     * OS.
     */
    private fun resumePending(prerequisite: Boolean, blockedPrompt: () -> WifiPermissionPrompt) {
        if (prerequisite) completePendingAction() else showPrompt(blockedPrompt())
    }

    private fun showPrompt(next: WifiPermissionPrompt) {
        prompt = next
        promptVisible = true
    }

    private fun completePendingAction() {
        when (pendingAction) {
            WifiPermissionAction.Enable -> onEnable()
            WifiPermissionAction.Scan -> onScan()
            null -> Unit
        }
        pendingAction = null
    }

    private fun hasSsidPermission(): Boolean =
        when {
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.O -> true
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.P -> hasPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) || hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)

            else -> hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        }

    private fun systemLocationEnabled(): Boolean =
        Build.VERSION.SDK_INT <= Build.VERSION_CODES.O ||
            LocationManagerCompat.isLocationEnabled(
                context.getSystemService(LocationManager::class.java)
            )

    /** True once the OS stopped explaining the request, i.e. the user denied it for good. */
    private fun rationaleSuppressed(): Boolean =
        activity?.shouldShowRequestPermissionRationale(requiredLocationPermissions().last()) == false
}

/**
 * Remembers the section's permission flow. The callbacks are read through [rememberUpdatedState] so
 * a lambda that captures composition values cannot go stale behind [remember], and a new lambda
 * instance on recomposition cannot reset a pending action.
 */
@Composable
internal fun rememberWifiPermissionFlow(
    context: Context,
    locationRequested: () -> Boolean,
    onEnable: () -> Unit,
    onScan: () -> Unit,
    onPermissionAsked: () -> Unit,
): WifiPermissionFlow {
    val locationRequestedState = rememberUpdatedState(locationRequested)
    val onEnableState = rememberUpdatedState(onEnable)
    val onScanState = rememberUpdatedState(onScan)
    val onPermissionAskedState = rememberUpdatedState(onPermissionAsked)

    val flow =
        remember(context) {
            WifiPermissionFlow(
                context = context,
                locationRequested = { locationRequestedState.value() },
                onEnable = { onEnableState.value() },
                onScan = { onScanState.value() },
                onPermissionAsked = { onPermissionAskedState.value() },
            )
        }
    val permissions =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            flow.onPermissionsResult()
        }
    val appSettings =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            flow.onAppSettingsResult()
        }
    val locationSettings =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            flow.onLocationSettingsResult()
        }

    flow.attachLaunchers(
        permissions = permissions,
        appSettings = appSettings,
        locationSettings = locationSettings,
    )
    return flow
}

private fun hasPermission(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

private fun requiredLocationPermissions(): Array<String> =
    when {
        Build.VERSION.SDK_INT <= Build.VERSION_CODES.O -> emptyArray()
        Build.VERSION.SDK_INT <= Build.VERSION_CODES.P -> arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)
        Build.VERSION.SDK_INT <= Build.VERSION_CODES.R -> arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        else -> arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
    }
