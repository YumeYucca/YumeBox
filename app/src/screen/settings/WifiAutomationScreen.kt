/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 */

@file:Suppress("FunctionName")

package com.github.yumeyucca.yumebox.screen.settings

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.github.yumeyucca.yumebox.data.model.WifiAutomationAction
import com.github.yumeyucca.yumebox.data.model.WifiAutomationFallbackAction
import com.github.yumeyucca.yumebox.data.model.WifiAutomationRule
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.Wifi
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.runtime.api.Profile
import org.koin.androidx.compose.koinViewModel
import tf.gal.yumebox.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.RadioButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

private enum class WifiPermissionAction { Enable, Scan }

/** Inline section of the VpnService configuration page. It deliberately has no separate route. */
@Composable
fun WifiAutomationSettingsSection() {
    val context = LocalContext.current
    val activity = context as? Activity
    val viewModel = koinViewModel<WifiAutomationViewModel>()
    val state by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    var pendingAction by remember { mutableStateOf<WifiPermissionAction?>(null) }
    var scanSheetVisible by remember { mutableStateOf(false) }
    var editSheetVisible by remember { mutableStateOf(false) }
    var showPermissionExplanation by remember { mutableStateOf(false) }
    var showApproximateLocation by remember { mutableStateOf(false) }
    var showPermissionDenied by remember { mutableStateOf(false) }
    var showPermissionSettings by remember { mutableStateOf(false) }
    var showLocationSettings by remember { mutableStateOf(false) }

    fun completePendingAction() {
        when (pendingAction) {
            WifiPermissionAction.Enable -> viewModel.enable()
            WifiPermissionAction.Scan -> viewModel.scanWifi()
            null -> Unit
        }
        pendingAction = null
    }

    fun hasSsidPermission(): Boolean =
        when {
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.O -> true
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.P -> hasPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) || hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)

            else -> hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        }

    fun systemLocationEnabled(): Boolean =
        Build.VERSION.SDK_INT <= Build.VERSION_CODES.O ||
            LocationManagerCompat.isLocationEnabled(
                context.getSystemService(android.location.LocationManager::class.java)
            )

    val locationSettingsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (pendingAction != null && systemLocationEnabled()) {
                if (hasSsidPermission()) completePendingAction() else showPermissionExplanation = true
            }
        }
    val appSettingsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (pendingAction != null && hasSsidPermission() && systemLocationEnabled()) {
                completePendingAction()
            }
        }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (hasSsidPermission()) {
                completePendingAction()
            } else if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
            ) {
                showApproximateLocation = true
            } else {
                val permission = requiredLocationPermissions().last()
                if (activity?.shouldShowRequestPermissionRationale(permission) == false) {
                    showPermissionSettings = true
                } else {
                    showPermissionDenied = true
                }
            }
        }

    fun requestSsidAccess(action: WifiPermissionAction) {
        pendingAction = action
        when {
            Build.VERSION.SDK_INT > Build.VERSION_CODES.O && !systemLocationEnabled() -> {
                showLocationSettings = true
            }

            hasSsidPermission() -> completePendingAction()
            state.locationRequested &&
                activity?.shouldShowRequestPermissionRationale(requiredLocationPermissions().last()) == false -> {
                showPermissionSettings = true
            }

            else -> showPermissionExplanation = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            val message =
                when (effect) {
                    WifiAutomationViewModel.Effect.AddedCurrentSsid -> YumeTxt.NetworkSettings.WifiAutomation.Added
                    WifiAutomationViewModel.Effect.SsidAlreadyExists -> YumeTxt.NetworkSettings.WifiAutomation.Duplicate
                    WifiAutomationViewModel.Effect.NoWifi -> YumeTxt.NetworkSettings.WifiAutomation.NoWifi
                    WifiAutomationViewModel.Effect.SsidUnavailable -> YumeTxt.NetworkSettings.WifiAutomation.Unavailable
                    WifiAutomationViewModel.Effect.ScanUnavailable -> YumeTxt.NetworkSettings.WifiAutomation.ScanUnavailable
                }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshProfiles()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Title(YumeTxt.NetworkSettings.WifiAutomation.Title)
    AppCard {
        PreferenceSwitchItem(
            title = YumeTxt.NetworkSettings.WifiAutomation.EnabledTitle,
            checked = state.enabled,
            onCheckedChange = { enabled ->
                if (enabled) requestSsidAccess(WifiPermissionAction.Enable) else viewModel.disable()
            },
        )
    }
    Title(YumeTxt.NetworkSettings.WifiAutomation.WifiNameHeading)
    AppCard {
        PreferenceArrowItem(
            title = YumeTxt.NetworkSettings.WifiAutomation.ManualDialogTitle,
            onClick = {
                viewModel.resetScan()
                scanSheetVisible = true
                requestSsidAccess(WifiPermissionAction.Scan)
            },
        )
        PreferenceArrowItem(
            title = YumeTxt.NetworkSettings.WifiAutomation.EditDialogTitle,
            onClick = { editSheetVisible = true },
        )
    }
    Title(YumeTxt.NetworkSettings.WifiAutomation.NetworkChangeSection)
    AppCard {
        PreferenceEnumItem(
            title = YumeTxt.NetworkSettings.WifiAutomation.OtherWifiTitle,
            currentValue = state.otherWifiAction,
            items = fallbackActionLabels(),
            values = WifiAutomationFallbackAction.entries,
            onValueChange = viewModel::changeOtherWifiAction,
        )
        if (state.otherWifiAction == WifiAutomationFallbackAction.Start) {
            WifiProfileSwitchSelector(
                profiles = state.profiles,
                profileUuid = state.otherWifiProfileUuid,
                onProfileUuidChange = viewModel::changeOtherWifiProfileUuid,
            )
        }
        PreferenceEnumItem(
            title = YumeTxt.NetworkSettings.WifiAutomation.NoWifiTitle,
            currentValue = state.noWifiAction,
            items = fallbackActionLabels(),
            values = WifiAutomationFallbackAction.entries,
            onValueChange = viewModel::changeNoWifiAction,
        )
        if (state.noWifiAction == WifiAutomationFallbackAction.Start) {
            WifiProfileSwitchSelector(
                profiles = state.profiles,
                profileUuid = state.noWifiProfileUuid,
                onProfileUuidChange = viewModel::changeNoWifiProfileUuid,
            )
        }
    }

    WifiScanSheet(
        show = scanSheetVisible,
        scannedNetworks = state.scannedNetworks,
        profiles = state.profiles,
        isScanning = state.isScanning,
        scanCompleted = state.scanCompleted,
        scanUnavailable = state.scanUnavailable,
        onDismiss = { scanSheetVisible = false },
        onDismissFinished = viewModel::resetScan,
        onConfirm = { ssid, action, profileUuid ->
            viewModel.addManualSsid(ssid, action, profileUuid)
            scanSheetVisible = false
        },
    )

    WifiRuleEditSheet(
        show = editSheetVisible,
        rules = state.rules,
        profiles = state.profiles,
        onDismiss = { editSheetVisible = false },
        onConfirm = { ssid, action, profileUuid ->
            viewModel.changeRuleAction(ssid, action, profileUuid)
            editSheetVisible = false
        },
        onDelete = viewModel::removeRule,
    )

    AppConfirmDialog(
        show = showPermissionExplanation,
        title = YumeTxt.NetworkSettings.WifiAutomation.PermissionTitle,
        message = YumeTxt.NetworkSettings.WifiAutomation.PermissionMessage,
        onDismissRequest = {
            pendingAction = null
            showPermissionExplanation = false
        },
        onConfirm = {
            showPermissionExplanation = false
            viewModel.markLocationPermissionRequested()
            permissionLauncher.launch(requiredLocationPermissions())
        },
        confirmText = YumeTxt.NetworkSettings.WifiAutomation.Grant,
    )
    AppConfirmDialog(
        show = showApproximateLocation,
        title = YumeTxt.NetworkSettings.WifiAutomation.ApproximateTitle,
        message = YumeTxt.NetworkSettings.WifiAutomation.ApproximateMessage,
        onDismissRequest = {
            pendingAction = null
            showApproximateLocation = false
        },
        onConfirm = {
            showApproximateLocation = false
            permissionLauncher.launch(requiredLocationPermissions())
        },
        confirmText = YumeTxt.NetworkSettings.WifiAutomation.Grant,
    )
    AppConfirmDialog(
        show = showPermissionDenied,
        title = YumeTxt.NetworkSettings.WifiAutomation.DeniedTitle,
        message = YumeTxt.NetworkSettings.WifiAutomation.DeniedMessage,
        onDismissRequest = {
            pendingAction = null
            showPermissionDenied = false
        },
        onConfirm = {
            showPermissionDenied = false
            showPermissionExplanation = true
        },
        confirmText = YumeTxt.NetworkSettings.WifiAutomation.Grant,
    )
    AppConfirmDialog(
        show = showPermissionSettings,
        title = YumeTxt.NetworkSettings.WifiAutomation.SettingsTitle,
        message = YumeTxt.NetworkSettings.WifiAutomation.SettingsMessage,
        onDismissRequest = {
            pendingAction = null
            showPermissionSettings = false
        },
        onConfirm = {
            showPermissionSettings = false
            appSettingsLauncher.launch(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null),
                )
            )
        },
        confirmText = YumeTxt.NetworkSettings.WifiAutomation.OpenSettings,
    )
    AppConfirmDialog(
        show = showLocationSettings,
        title = YumeTxt.NetworkSettings.WifiAutomation.LocationTitle,
        message = YumeTxt.NetworkSettings.WifiAutomation.LocationMessage,
        onDismissRequest = {
            pendingAction = null
            showLocationSettings = false
        },
        onConfirm = {
            showLocationSettings = false
            locationSettingsLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        },
        confirmText = YumeTxt.NetworkSettings.WifiAutomation.TurnOnLocation,
    )
}

@Composable
private fun WifiScanSheet(
    show: Boolean,
    scannedNetworks: List<com.github.yumeyucca.yumebox.runtime.service.session.WifiSsidNetwork>,
    profiles: List<Profile>,
    isScanning: Boolean,
    scanCompleted: Boolean,
    scanUnavailable: Boolean,
    onDismiss: () -> Unit,
    onDismissFinished: () -> Unit,
    onConfirm: (ssid: String, action: WifiAutomationAction, profileUuid: String?) -> Unit,
) {
    val spacing = AppTheme.spacing
    var selectedSsid by remember { mutableStateOf<String?>(null) }
    var action by remember { mutableStateOf(WifiAutomationAction.Start) }
    var profileUuid by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(show) {
        if (show) {
            selectedSsid = null
            action = WifiAutomationAction.Start
            profileUuid = null
        }
    }

    AppActionBottomSheet(
        show = show,
        title = YumeTxt.NetworkSettings.WifiAutomation.ManualDialogTitle,
        startAction = { AppBottomSheetCloseAction(onClick = onDismiss) },
        endAction = {
            AppBottomSheetConfirmAction(
                enabled = selectedSsid != null,
                onClick = { selectedSsid?.let { onConfirm(it, action, profileUuid) } },
            )
        },
        onDismissRequest = onDismiss,
        onDismissFinished = onDismissFinished,
        insideMargin = DpSize(UiDp.dp12, UiDp.dp8),
        enableNestedScroll = true,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(spacing.space16),
        ) {
            when {
                isScanning -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 176.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(
                            spacing.space12,
                            Alignment.CenterVertically,
                        ),
                    ) {
                        InfiniteProgressIndicator(modifier = Modifier.size(32.dp))
                        Text(
                            text = YumeTxt.NetworkSettings.WifiAutomation.Scanning,
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                }

                scanUnavailable -> BasicComponent(title = YumeTxt.NetworkSettings.WifiAutomation.ScanUnavailable)
                scanCompleted && scannedNetworks.isEmpty() ->
                    BasicComponent(title = YumeTxt.NetworkSettings.WifiAutomation.ScanEmpty)

                scannedNetworks.isNotEmpty() -> {
                    AnimatedVisibility(
                        visible = selectedSsid != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(spacing.space16)) {
                            AppCard {
                                WindowDropdownPreference(
                                    title = YumeTxt.NetworkSettings.WifiAutomation.ActionTitle,
                                    items = listOf(
                                        YumeTxt.NetworkSettings.WifiAutomation.StartAction,
                                        YumeTxt.NetworkSettings.WifiAutomation.StopAction,
                                    ),
                                    selectedIndex = if (action == WifiAutomationAction.Start) 0 else 1,
                                    onSelectedIndexChange = { index ->
                                        action =
                                            if (index == 0) {
                                                WifiAutomationAction.Start
                                            } else {
                                                WifiAutomationAction.Stop
                                            }
                                    },
                                )
                            }
                            if (action == WifiAutomationAction.Start) {
                                AppCard {
                                    WifiProfileSwitchSelector(
                                        profiles = profiles,
                                        profileUuid = profileUuid,
                                        onProfileUuidChange = { profileUuid = it },
                                    )
                                }
                            }
                        }
                    }
                    AppCard {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                        ) {
                            items(scannedNetworks, key = { it.ssid }) { network ->
                                BasicComponent(
                                    title = network.ssid,
                                    summary = network.label,
                                    onClick = { selectedSsid = network.ssid },
                                    startAction = {
                                        Icon(
                                            modifier = Modifier.padding(end = UiDp.dp8),
                                            imageVector = Yume.Wifi,
                                            contentDescription = null,
                                        )
                                    },
                                    endActions = {
                                        RadioButton(
                                            selected = selectedSsid == network.ssid,
                                            onClick = { selectedSsid = network.ssid },
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WifiRuleEditSheet(
    show: Boolean,
    rules: List<WifiAutomationRule>,
    profiles: List<Profile>,
    onDismiss: () -> Unit,
    onConfirm: (ssid: String, action: WifiAutomationAction, profileUuid: String?) -> Unit,
    onDelete: (ssid: String) -> Unit,
) {
    val spacing = AppTheme.spacing
    var selectedSsid by remember { mutableStateOf<String?>(null) }
    var action by remember { mutableStateOf(WifiAutomationAction.Start) }
    var profileUuid by remember { mutableStateOf<String?>(null) }
    val selectedRule = rules.firstOrNull { it.ssid == selectedSsid }

    LaunchedEffect(show) {
        if (show) selectedSsid = null
    }
    LaunchedEffect(selectedRule) {
        selectedRule?.let { rule ->
            action = rule.action
            profileUuid = rule.profileUuid
        }
    }

    AppActionBottomSheet(
        show = show,
        title = YumeTxt.NetworkSettings.WifiAutomation.EditDialogTitle,
        startAction = { AppBottomSheetCloseAction(onClick = onDismiss) },
        endAction = {
            AppBottomSheetConfirmAction(
                enabled = selectedRule != null,
                onClick = { selectedRule?.let { onConfirm(it.ssid, action, profileUuid) } },
            )
        },
        onDismissRequest = onDismiss,
        insideMargin = DpSize(UiDp.dp12, UiDp.dp8),
        enableNestedScroll = true,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = UiDp.dp16),
            verticalArrangement = Arrangement.spacedBy(spacing.space12),
        ) {
            AnimatedVisibility(
                visible = selectedRule != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.space12)) {
                    AppCard {
                        WindowDropdownPreference(
                            title = YumeTxt.NetworkSettings.WifiAutomation.ActionTitle,
                            items = listOf(
                                YumeTxt.NetworkSettings.WifiAutomation.StartAction,
                                YumeTxt.NetworkSettings.WifiAutomation.StopAction,
                            ),
                            selectedIndex = if (action == WifiAutomationAction.Start) 0 else 1,
                            onSelectedIndexChange = { index ->
                                action =
                                    if (index == 0) WifiAutomationAction.Start else WifiAutomationAction.Stop
                            },
                        )
                    }
                    if (action == WifiAutomationAction.Start) {
                        AppCard {
                            WifiProfileSwitchSelector(
                                profiles = profiles,
                                profileUuid = profileUuid,
                                onProfileUuidChange = { profileUuid = it },
                            )
                        }
                    }
                }
            }
            if (rules.isEmpty()) {
                BasicComponent(title = YumeTxt.NetworkSettings.WifiAutomation.EmptyRules)
            } else {
                AppCard {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                    ) {
                        items(rules, key = { it.ssid }) { rule ->
                            BasicComponent(
                                title = rule.ssid,
                                summary = actionLabel(rule.action),
                                onClick = { selectedSsid = rule.ssid },
                                startAction = {
                                    Icon(
                                        modifier = Modifier.padding(end = UiDp.dp8),
                                        imageVector = Yume.Wifi,
                                        contentDescription = null,
                                    )
                                },
                                endActions = {
                                    RadioButton(
                                        selected = selectedSsid == rule.ssid,
                                        onClick = { selectedSsid = rule.ssid },
                                    )
                                },
                            )
                        }
                    }
                }
            }
            AnimatedVisibility(
                visible = selectedRule != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                TextButton(
                    text = YumeTxt.Component.ProfileCard.Delete,
                    onClick = {
                        selectedRule?.let { onDelete(it.ssid) }
                        selectedSsid = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(textColor = MiuixTheme.colorScheme.error),
                )
            }
        }
    }
}

private fun actionLabel(action: WifiAutomationAction): String =
    when (action) {
        WifiAutomationAction.Start -> YumeTxt.NetworkSettings.WifiAutomation.StartAction
        WifiAutomationAction.Stop -> YumeTxt.NetworkSettings.WifiAutomation.StopAction
    }

@Composable
private fun WifiProfileSwitchSelector(
    profiles: List<Profile>,
    profileUuid: String?,
    onProfileUuidChange: (String?) -> Unit,
) {
    val items = remember(profiles) {
        listOf(YumeTxt.NetworkSettings.WifiAutomation.NoSwitchAction) + profiles.map { it.name }
    }
    val selectedIndex = profiles.indexOfFirst { it.uuid.toString() == profileUuid } + 1

    WindowDropdownPreference(
        title = YumeTxt.NetworkSettings.WifiAutomation.ProfileAction,
        items = items,
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { index -> onProfileUuidChange(profiles.getOrNull(index - 1)?.uuid?.toString()) },
    )
}

@Composable
private fun fallbackActionLabels() =
    listOf(
        YumeTxt.NetworkSettings.WifiAutomation.KeepAction,
        YumeTxt.NetworkSettings.WifiAutomation.StartAction,
        YumeTxt.NetworkSettings.WifiAutomation.StopAction,
    )

private fun hasPermission(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

private fun requiredLocationPermissions(): Array<String> =
    when {
        Build.VERSION.SDK_INT <= Build.VERSION_CODES.O -> emptyArray()
        Build.VERSION.SDK_INT <= Build.VERSION_CODES.P -> arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)
        Build.VERSION.SDK_INT <= Build.VERSION_CODES.R -> arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        else -> arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
    }
