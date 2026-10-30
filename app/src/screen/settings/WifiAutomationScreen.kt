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

import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import com.github.yumeyucca.yumebox.data.model.WifiAutomationAction
import com.github.yumeyucca.yumebox.data.model.WifiAutomationFallbackAction
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.runtime.api.Profile
import org.koin.androidx.compose.koinViewModel

/**
 * Inline section of the VpnService configuration page. It deliberately has no separate route.
 *
 * It only collects state, remembers the permission flow and renders: the choreography of asking for
 * location access lives in [WifiPermissionFlow].
 */
@Composable
fun WifiAutomationSettingsSection() {
    val context = LocalContext.current
    val viewModel = koinViewModel<WifiAutomationViewModel>()
    val state by viewModel.uiState.collectAsState()
    val permissionFlow =
        rememberWifiPermissionFlow(
            context = context,
            locationRequested = { state.locationRequested },
            onEnable = viewModel::enable,
            onScan = viewModel::scanWifi,
            onPermissionAsked = viewModel::markLocationPermissionRequested,
        )
    var scanSheetVisible by remember { mutableStateOf(false) }
    var editSheetVisible by remember { mutableStateOf(false) }

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

    // Profiles can be added or renamed on another screen, so the picker is rebuilt on every resume.
    LifecycleResumeEffect(Unit) {
        viewModel.refreshProfiles()
        onPauseOrDispose { }
    }

    WifiAutomationToggleSection(
        checked = state.enabled,
        onCheckedChange = { enabled ->
            if (enabled) permissionFlow.request(WifiPermissionAction.Enable) else viewModel.disable()
        },
    )
    WifiNameSection(
        onAddManual = {
            viewModel.resetScan()
            scanSheetVisible = true
            permissionFlow.request(WifiPermissionAction.Scan)
        },
        onEditRules = { editSheetVisible = true },
    )
    WifiNetworkChangeSection(state = state, viewModel = viewModel)

    WifiAutomationSheetHosts(
        scanVisible = scanSheetVisible,
        onScanVisibleChange = { scanSheetVisible = it },
        editVisible = editSheetVisible,
        onEditVisibleChange = { editSheetVisible = it },
        state = state,
        viewModel = viewModel,
    )

    WifiPermissionDialog(
        show = permissionFlow.promptVisible,
        prompt = permissionFlow.prompt,
        onDismissRequest = permissionFlow::dismissPrompt,
        onConfirm = permissionFlow::onPromptConfirmed,
    )
}

/** The master switch, which routes through the permission flow before enabling. */
@Composable
private fun WifiAutomationToggleSection(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Title(YumeTxt.NetworkSettings.WifiAutomation.Title)
    AppCard {
        PreferenceSwitchItem(
            title = YumeTxt.NetworkSettings.WifiAutomation.EnabledTitle,
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

/** The two Wi-Fi name entries: add/scan for a network, or edit the stored rules. */
@Composable
private fun WifiNameSection(
    onAddManual: () -> Unit,
    onEditRules: () -> Unit,
) {
    Title(YumeTxt.NetworkSettings.WifiAutomation.WifiNameHeading)
    AppCard {
        PreferenceArrowItem(
            title = YumeTxt.NetworkSettings.WifiAutomation.ManualDialogTitle,
            onClick = onAddManual,
        )
        PreferenceArrowItem(
            title = YumeTxt.NetworkSettings.WifiAutomation.EditDialogTitle,
            onClick = onEditRules,
        )
    }
}

/** What to do when the other known Wi-Fi or no Wi-Fi is seen. */
@Composable
private fun WifiNetworkChangeSection(
    state: WifiAutomationViewModel.UiState,
    viewModel: WifiAutomationViewModel,
) {
    Title(YumeTxt.NetworkSettings.WifiAutomation.NetworkChangeSection)
    AppCard {
        WifiFallbackActionPreference(
            title = YumeTxt.NetworkSettings.WifiAutomation.OtherWifiTitle,
            action = state.otherWifiAction,
            profileUuid = state.otherWifiProfileUuid,
            profiles = state.profiles,
            onActionChange = viewModel::changeOtherWifiAction,
            onProfileUuidChange = viewModel::changeOtherWifiProfileUuid,
        )
        WifiFallbackActionPreference(
            title = YumeTxt.NetworkSettings.WifiAutomation.NoWifiTitle,
            action = state.noWifiAction,
            profileUuid = state.noWifiProfileUuid,
            profiles = state.profiles,
            onActionChange = viewModel::changeNoWifiAction,
            onProfileUuidChange = viewModel::changeNoWifiProfileUuid,
        )
    }
}

/** The scan and rule-edit bottom sheets, both driven by the screen's visibility flags. */
@Composable
private fun WifiAutomationSheetHosts(
    scanVisible: Boolean,
    onScanVisibleChange: (Boolean) -> Unit,
    editVisible: Boolean,
    onEditVisibleChange: (Boolean) -> Unit,
    state: WifiAutomationViewModel.UiState,
    viewModel: WifiAutomationViewModel,
) {
    WifiScanSheet(
        show = scanVisible,
        phase = state.scanPhase,
        profiles = state.profiles,
        onDismiss = { onScanVisibleChange(false) },
        onDismissFinished = viewModel::resetScan,
        onConfirm = { ssid, action, profileUuid ->
            viewModel.addManualSsid(ssid, action, profileUuid)
            onScanVisibleChange(false)
        },
    )

    WifiRuleEditSheet(
        show = editVisible,
        rules = state.rules,
        profiles = state.profiles,
        onDismiss = { onEditVisibleChange(false) },
        onConfirm = { ssid, action, profileUuid ->
            viewModel.changeRuleAction(ssid, action, profileUuid)
            onEditVisibleChange(false)
        },
        onDelete = viewModel::removeRule,
    )
}

/**
 * One fallback-action preference plus, for [WifiAutomationFallbackAction.Start], the profile it
 * switches to. This is the only place that knows "start needs a target profile".
 */
@Composable
private fun WifiFallbackActionPreference(
    title: String,
    action: WifiAutomationFallbackAction,
    profileUuid: String?,
    profiles: List<Profile>,
    onActionChange: (WifiAutomationFallbackAction) -> Unit,
    onProfileUuidChange: (String?) -> Unit,
) {
    PreferenceEnumItem(
        title = title,
        currentValue = action,
        values = WifiAutomationFallbackAction.entries,
        onValueChange = onActionChange,
        label = ::fallbackActionLabel,
    )
    if (action == WifiAutomationFallbackAction.Start) {
        WifiProfileSwitchSelector(
            profiles = profiles,
            profileUuid = profileUuid,
            onProfileUuidChange = onProfileUuidChange,
        )
    }
}

/** Label of one rule action; the fallback actions reuse the same two verbs. */
internal fun actionLabel(action: WifiAutomationAction): String =
    when (action) {
        WifiAutomationAction.Start -> YumeTxt.NetworkSettings.WifiAutomation.StartAction
        WifiAutomationAction.Stop -> YumeTxt.NetworkSettings.WifiAutomation.StopAction
    }

private fun fallbackActionLabel(action: WifiAutomationFallbackAction): String =
    when (action) {
        WifiAutomationFallbackAction.Keep -> YumeTxt.NetworkSettings.WifiAutomation.KeepAction
        WifiAutomationFallbackAction.Start -> actionLabel(WifiAutomationAction.Start)
        WifiAutomationFallbackAction.Stop -> actionLabel(WifiAutomationAction.Stop)
    }
