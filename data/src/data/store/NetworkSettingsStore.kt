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

package com.github.yumeyucca.yumebox.data.store

import com.github.yumeyucca.yumebox.core.model.TunDnsMode
import com.github.yumeyucca.yumebox.data.model.AccessControlMode
import com.github.yumeyucca.yumebox.data.model.AccessControlSortMode
import com.github.yumeyucca.yumebox.data.model.RunMode
import com.github.yumeyucca.yumebox.data.model.TunStack
import com.github.yumeyucca.yumebox.data.model.WifiAutomationFallbackAction
import com.github.yumeyucca.yumebox.data.model.WifiAutomationRule
import com.tencent.mmkv.MMKV
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class NetworkSettingsStore(externalMmkv: MMKV) : MMKVPreference(externalMmkv = externalMmkv) {
    // Run mode selected in the UI (VpnService / Tun).
    val runMode by enumFlow(RunMode.VpnService)

    val bypassPrivateNetwork by boolFlow(true)
    val ebpfBypassCn by boolFlow(true)
    val dnsHijack by boolFlow(true)
    val allowBypass by boolFlow(true)
    val enableIPv6 by boolFlow(false)
    val systemProxy by boolFlow(true)

    // All modes: drop the user override chain. Root Tun additionally skips mode geometry
    // injection and compiler runtime patches (skipRuntimePatches).
    val disableAllOverride by boolFlow(false)

    // Network stack selected for Root Tun. Root provides the virtual network interface and routes.
    val tunStack by enumFlow(TunStack.GVisor)
    val tunRouteExcludeAddress by stringListFlow(emptyList())
    val tunIfName by strFlow("Yume")
    val tunMtu by intFlow(1500)
    val tunAutoRoute by boolFlow(true)

    // strict-route off (can black-hole traffic); auto-redirect on — it carries the app TCP egress.
    val tunStrictRoute by boolFlow(false)
    val tunAutoRedirect by boolFlow(true)

    // Empty: omit include-android-user so the core includes every Android user automatically.
    val tunIncludeAndroidUser by intListFlow(emptyList())

    // redir-host is the reliable default (real IPs, no fake-ip pool/filter to get wrong); fake-ip
    // stays selectable.
    val tunDnsMode by enumFlow(TunDnsMode.RedirHost)
    val tunFakeIpRange by strFlow("198.18.0.1/16")
    val tunFakeIpRange6 by strFlow("fc00::/18")

    val accessControlMode by enumFlow(AccessControlMode.ALLOW_ALL)
    val accessControlPackages by stringSetFlow(emptySet())
    val accessControlSelectedFirst by boolFlow(true)
    val accessControlShowSystemApps by boolFlow(false)
    val accessControlSortMode by enumFlow(AccessControlSortMode.LABEL)

    // Wi-Fi SSIDs are location-sensitive on Android. This state is only enabled after the user
    // explicitly completes the location-permission flow from the Wi-Fi automation screen.
    val wifiAutomationEnabled by boolFlow(false)
    val wifiAutomationLocationRequested by boolFlow(false)
    val wifiAutomationRules: Preference<List<WifiAutomationRule>> by
        jsonListFlow(
            default = emptyList(),
            decode = { source -> decodeFromString<List<WifiAutomationRule>>(source) },
            encode = { rules -> encodeToString(rules) },
        )

    // These explicitly cover physical Wi-Fi transport changes. They intentionally default to
    // Keep so enabling SSID automation never changes behavior outside the user's rules.
    val wifiAutomationOtherWifiAction by enumFlow(WifiAutomationFallbackAction.Keep)
    val wifiAutomationNoWifiAction by enumFlow(WifiAutomationFallbackAction.Keep)

    // Empty string means "do not switch" when the corresponding fallback action is Start.
    val wifiAutomationOtherWifiProfileUuid by strFlow("")
    val wifiAutomationNoWifiProfileUuid by strFlow("")

}
