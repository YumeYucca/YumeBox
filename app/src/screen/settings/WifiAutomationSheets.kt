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
import androidx.compose.ui.unit.DpSize
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import com.github.yumeyucca.yumebox.data.model.WifiAutomationAction
import com.github.yumeyucca.yumebox.data.model.WifiAutomationRule
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.Wifi
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.runtime.api.Profile
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.RadioButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Which permission dialog is on screen for the pending Wi-Fi action. */
internal enum class WifiPermissionPrompt { Explanation, Approximate, Denied, Settings, Location }

/** Shared inside margin of the Wi-Fi automation sheets. */
private val WifiSheetInsideMargin = DpSize(UiDp.dp12, UiDp.dp8)

@Composable
internal fun WifiScanSheet(
    show: Boolean,
    phase: WifiScanPhase,
    profiles: List<Profile>,
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
        endAction = {
            AppBottomSheetConfirmAction(
                enabled = selectedSsid != null,
                onClick = { selectedSsid?.let { onConfirm(it, action, profileUuid) } },
            )
        },
        onDismissRequest = onDismiss,
        onDismissFinished = onDismissFinished,
        insideMargin = WifiSheetInsideMargin,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(spacing.space16),
        ) {
            when (phase) {
                WifiScanPhase.Scanning -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().heightIn(min = UiDp.dp176),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(
                            spacing.space12,
                            Alignment.CenterVertically,
                        ),
                    ) {
                        InfiniteProgressIndicator(modifier = Modifier.size(UiDp.dp32))
                        Text(
                            text = YumeTxt.NetworkSettings.WifiAutomation.Scanning,
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                }

                WifiScanPhase.Unavailable -> BasicComponent(title = YumeTxt.NetworkSettings.WifiAutomation.ScanUnavailable)
                WifiScanPhase.Empty -> BasicComponent(title = YumeTxt.NetworkSettings.WifiAutomation.ScanEmpty)

                is WifiScanPhase.Results -> {
                    AnimatedVisibility(
                        visible = selectedSsid != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(spacing.space16)) {
                            WifiActionEditor(
                                action = action,
                                onActionChange = { action = it },
                                profiles = profiles,
                                profileUuid = profileUuid,
                                onProfileUuidChange = { profileUuid = it },
                            )
                        }
                    }
                    AppCard {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = UiDp.dp360),
                        ) {
                            items(phase.networks, key = { it.ssid }) { network ->
                                WifiSsidRow(
                                    ssid = network.ssid,
                                    summary = network.label,
                                    selected = selectedSsid == network.ssid,
                                    onClick = { selectedSsid = network.ssid },
                                )
                            }
                        }
                    }
                }

                WifiScanPhase.Idle -> Unit
            }
        }
    }
}

@Composable
internal fun WifiRuleEditSheet(
    show: Boolean,
    rules: List<WifiAutomationRule>,
    profiles: List<Profile>,
    onDismiss: () -> Unit,
    onConfirm: (ssid: String, action: WifiAutomationAction, profileUuid: String?) -> Unit,
    onDelete: (ssid: String) -> Unit,
) {
    val spacing = AppTheme.spacing
    // One draft rule is the whole editor state: the list row and the editor below it read the same
    // value, so a selection and its pending edits can never disagree.
    var draft by remember { mutableStateOf<WifiAutomationRule?>(null) }

    LaunchedEffect(show) {
        if (show) draft = null
    }

    AppActionBottomSheet(
        show = show,
        title = YumeTxt.NetworkSettings.WifiAutomation.EditDialogTitle,
        endAction = {
            AppBottomSheetConfirmAction(
                enabled = draft != null,
                onClick = { draft?.let { onConfirm(it.ssid, it.action, it.profileUuid) } },
            )
        },
        onDismissRequest = onDismiss,
        insideMargin = WifiSheetInsideMargin,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = UiDp.dp16),
            verticalArrangement = Arrangement.spacedBy(spacing.space12),
        ) {
            AnimatedVisibility(
                visible = draft != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                WifiActionEditor(
                    action = draft?.action ?: WifiAutomationAction.Start,
                    onActionChange = { draft = draft?.copy(action = it) },
                    profiles = profiles,
                    profileUuid = draft?.profileUuid,
                    onProfileUuidChange = { draft = draft?.copy(profileUuid = it) },
                )
            }
            if (rules.isEmpty()) {
                BasicComponent(title = YumeTxt.NetworkSettings.WifiAutomation.EmptyRules)
            } else {
                AppCard {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = UiDp.dp360),
                    ) {
                        items(rules, key = { it.ssid }) { rule ->
                            WifiSsidRow(
                                ssid = rule.ssid,
                                summary = actionLabel(rule.action),
                                selected = draft?.ssid == rule.ssid,
                                onClick = { draft = rule },
                            )
                        }
                    }
                }
            }
            AnimatedVisibility(
                visible = draft != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                TextButton(
                    text = YumeTxt.Component.ProfileCard.Delete,
                    onClick = {
                        draft?.let { onDelete(it.ssid) }
                        draft = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(textColor = MiuixTheme.colorScheme.error),
                )
            }
        }
    }
}

/** One selectable SSID row, shared by the scan list and the saved-rule list. */
@Composable
private fun WifiSsidRow(
    ssid: String,
    summary: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    BasicComponent(
        title = ssid,
        summary = summary,
        onClick = onClick,
        startAction = {
            Icon(
                modifier = Modifier.padding(end = UiDp.dp8),
                imageVector = Yume.Wifi,
                contentDescription = null,
            )
        },
        endActions = {
            RadioButton(
                selected = selected,
                onClick = onClick,
            )
        },
    )
}

@Composable
internal fun WifiProfileSwitchSelector(
    profiles: List<Profile>,
    profileUuid: String?,
    onProfileUuidChange: (String?) -> Unit,
) {
    val options = remember(profiles) {
        listOf(
            GlassDropdownOption<String?>(null, YumeTxt.NetworkSettings.WifiAutomation.NoSwitchAction),
        ) + profiles.map { GlassDropdownOption<String?>(it.uuid.toString(), it.name) }
    }

    GlassDropdownPreference(
        title = YumeTxt.NetworkSettings.WifiAutomation.ProfileAction,
        options = options,
        selected = profileUuid,
        onSelected = onProfileUuidChange,
    )
}

/** Renders the rule action picker plus, for [WifiAutomationAction.Start], the target profile row. */
@Composable
private fun WifiActionEditor(
    action: WifiAutomationAction,
    onActionChange: (WifiAutomationAction) -> Unit,
    profiles: List<Profile>,
    profileUuid: String?,
    onProfileUuidChange: (String?) -> Unit,
) {
    AppCard {
        GlassDropdownPreference(
            title = YumeTxt.NetworkSettings.WifiAutomation.ActionTitle,
            options = WifiAutomationAction.entries.map { GlassDropdownOption(it, actionLabel(it)) },
            selected = action,
            onSelected = onActionChange,
        )
    }
    if (action == WifiAutomationAction.Start) {
        AppCard {
            WifiProfileSwitchSelector(
                profiles = profiles,
                profileUuid = profileUuid,
                onProfileUuidChange = onProfileUuidChange,
            )
        }
    }
}

/**
 * Renders the permission prompt of the pending action.
 *
 * [prompt] is non-null and deliberately outlives [show]: keeping the last prompt's text composed
 * lets the dialog finish its exit tween with readable content.
 */
@Composable
internal fun WifiPermissionDialog(
    show: Boolean,
    prompt: WifiPermissionPrompt,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    val text = wifiPromptText(prompt)
    AppConfirmDialog(
        show = show,
        title = text.title,
        message = text.message,
        confirmText = text.confirmText,
        onDismissRequest = onDismissRequest,
        onConfirm = onConfirm,
    )
}

private class WifiPermissionPromptText(
    val title: String,
    val message: String,
    val confirmText: String,
)

private fun wifiPromptText(prompt: WifiPermissionPrompt): WifiPermissionPromptText =
    when (prompt) {
        WifiPermissionPrompt.Explanation ->
            WifiPermissionPromptText(
                YumeTxt.NetworkSettings.WifiAutomation.PermissionTitle,
                YumeTxt.NetworkSettings.WifiAutomation.PermissionMessage,
                YumeTxt.NetworkSettings.WifiAutomation.Grant,
            )

        WifiPermissionPrompt.Approximate ->
            WifiPermissionPromptText(
                YumeTxt.NetworkSettings.WifiAutomation.ApproximateTitle,
                YumeTxt.NetworkSettings.WifiAutomation.ApproximateMessage,
                YumeTxt.NetworkSettings.WifiAutomation.Grant,
            )

        WifiPermissionPrompt.Denied ->
            WifiPermissionPromptText(
                YumeTxt.NetworkSettings.WifiAutomation.DeniedTitle,
                YumeTxt.NetworkSettings.WifiAutomation.DeniedMessage,
                YumeTxt.NetworkSettings.WifiAutomation.Grant,
            )

        WifiPermissionPrompt.Settings ->
            WifiPermissionPromptText(
                YumeTxt.NetworkSettings.WifiAutomation.SettingsTitle,
                YumeTxt.NetworkSettings.WifiAutomation.SettingsMessage,
                YumeTxt.NetworkSettings.WifiAutomation.OpenSettings,
            )

        WifiPermissionPrompt.Location ->
            WifiPermissionPromptText(
                YumeTxt.NetworkSettings.WifiAutomation.LocationTitle,
                YumeTxt.NetworkSettings.WifiAutomation.LocationMessage,
                YumeTxt.NetworkSettings.WifiAutomation.TurnOnLocation,
            )
    }
