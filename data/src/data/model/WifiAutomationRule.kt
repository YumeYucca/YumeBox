/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 */

package com.github.yumeyucca.yumebox.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class WifiAutomationAction {
    Start,
    Stop,
}

/** Action to take when the connected Wi-Fi does not match an SSID rule, or is disconnected. */
@Serializable
enum class WifiAutomationFallbackAction {
    Keep,
    Start,
    Stop,
}

/** A case-sensitive SSID rule. SSIDs are intentionally stored exactly as supplied. */
@Serializable
data class WifiAutomationRule(
    val ssid: String,
    val action: WifiAutomationAction,
    /** Profile UUID to switch to when [action] is [WifiAutomationAction.Start]; null means keep. */
    val profileUuid: String? = null,
)
