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

@file:Suppress("UnusedSymbol")

package com.github.yumeyucca.yumebox.runtime.service.config


import com.github.yumeyucca.yumebox.core.model.TunDnsMode
import com.tencent.mmkv.MMKV
import kotlinx.serialization.json.Json
import java.util.*

class ServiceStore {
    private val store = Store(MMKV.mmkvWithID("service", MMKV.MULTI_PROCESS_MODE).asStoreProvider())
    private val networkSettings = MMKV.mmkvWithID("network_settings", MMKV.MULTI_PROCESS_MODE)

    private fun readBoolean(newKey: String, legacyKey: String, defaultValue: Boolean): Boolean =
        when {
            networkSettings.containsKey(newKey) -> networkSettings.decodeBool(newKey, defaultValue)
            else -> store.provider.getBoolean(legacyKey, defaultValue)
        }

    private fun readString(newKey: String, legacyKey: String, defaultValue: String): String =
        when {
            networkSettings.containsKey(newKey) ->
                networkSettings.decodeString(newKey, defaultValue) ?: defaultValue

            else -> store.provider.getString(legacyKey, defaultValue)
        }

    private fun readInt(newKey: String, legacyKey: String, defaultValue: Int): Int =
        when {
            networkSettings.containsKey(newKey) -> networkSettings.decodeInt(newKey, defaultValue)
            else -> store.provider.getInt(legacyKey, defaultValue)
        }

    private fun readStringSet(
        newKey: String,
        legacyKey: String,
        defaultValue: Set<String>,
    ): Set<String> =
        when {
            networkSettings.containsKey(newKey) ->
                networkSettings.decodeStringSet(newKey, defaultValue) ?: defaultValue

            else -> store.provider.getStringSet(legacyKey, defaultValue)
        }

    private fun readStringList(newKey: String, defaultValue: List<String>): List<String> {
        if (!networkSettings.containsKey(newKey)) return defaultValue
        return runCatching {
                networkSettings.decodeString(newKey)?.let {
                    json.decodeFromString<List<String>>(it)
                } ?: defaultValue
            }
            .getOrDefault(defaultValue)
    }

    private fun readIntList(newKey: String, defaultValue: List<Int>): List<Int> {
        if (!networkSettings.containsKey(newKey)) return defaultValue
        return runCatching {
                networkSettings.decodeString(newKey)?.let { json.decodeFromString<List<Int>>(it) }
                    ?: defaultValue
            }
            .getOrDefault(defaultValue)
    }

    private inline fun <reified T : Enum<T>> readEnum(newKey: String, defaultValue: T): T {
        if (!networkSettings.containsKey(newKey)) return defaultValue
        val raw = networkSettings.decodeString(newKey, defaultValue.name) ?: defaultValue.name
        return runCatching { enumValueOf<T>(raw) }.getOrDefault(defaultValue)
    }

    var activeProfile: UUID? by
        store.typedString(
            key = "active_profile",
            from = { if (it.isBlank()) null else UUID.fromString(it) },
            to = { it?.toString() ?: "" },
        )

    var bypassPrivateNetwork: Boolean
        get() =
            readBoolean(
                newKey = "bypassPrivateNetwork",
                legacyKey = "bypass_private_network",
                defaultValue = true,
            )
        set(value) {
            networkSettings.encode("bypassPrivateNetwork", value)
            store.provider.setBoolean("bypass_private_network", value)
        }

    var ebpfBypassCn: Boolean
        get() = readBoolean(newKey = "ebpfBypassCn", legacyKey = "ebpf_bypass_cn", defaultValue = true)
        set(value) {
            networkSettings.encode("ebpfBypassCn", value)
            store.provider.setBoolean("ebpf_bypass_cn", value)
        }

    private var accessControlModeRaw: String
        get() =
            readString(
                newKey = "accessControlMode",
                legacyKey = "access_control_mode",
                defaultValue = AccessControlMode.AcceptAll.name,
            )
        set(value) {
            networkSettings.encode("accessControlMode", value)
            store.provider.setString("access_control_mode", value)
        }

    var accessControlMode: AccessControlMode
        get() =
            when (accessControlModeRaw) {
                AccessControlMode.AcceptAll.name,
                "ALLOW_ALL" -> AccessControlMode.AcceptAll

                AccessControlMode.AcceptSelected.name,
                "ALLOW_SPECIFIC" -> AccessControlMode.AcceptSelected

                AccessControlMode.RejectAll.name,
                "DENY_ALL" -> AccessControlMode.RejectAll

                AccessControlMode.RejectSelected.name,
                "DENY_SPECIFIC" -> AccessControlMode.RejectSelected

                else -> AccessControlMode.AcceptAll
            }
        set(value) {
            accessControlModeRaw = value.name
        }

    var accessControlPackages: Set<String>
        get() =
            readStringSet(
                newKey = "accessControlPackages",
                legacyKey = "access_control_packages",
                defaultValue = emptySet(),
            )
        set(value) {
            networkSettings.encode("accessControlPackages", value)
            store.provider.setStringSet("access_control_packages", value)
        }

    var dnsHijacking: Boolean
        get() = readBoolean(newKey = "dnsHijack", legacyKey = "dns_hijacking", defaultValue = true)
        set(value) {
            networkSettings.encode("dnsHijack", value)
            store.provider.setBoolean("dns_hijacking", value)
        }

    var systemProxy: Boolean
        get() = readBoolean(newKey = "systemProxy", legacyKey = "system_proxy", defaultValue = true)
        set(value) {
            networkSettings.encode("systemProxy", value)
            store.provider.setBoolean("system_proxy", value)
        }

    var allowBypass: Boolean
        get() = readBoolean(newKey = "allowBypass", legacyKey = "allow_bypass", defaultValue = true)
        set(value) {
            networkSettings.encode("allowBypass", value)
            store.provider.setBoolean("allow_bypass", value)
        }

    var allowIpv6: Boolean
        get() =
            when {
                networkSettings.containsKey("enableIPv6") ->
                    networkSettings.decodeBool("enableIPv6", false)

                else -> store.provider.getBoolean("allow_ipv6", false)
            }
        set(value) {
            networkSettings.encode("enableIPv6", value)
            store.provider.setBoolean("allow_ipv6", value)
        }

    var tunStackMode: String
        get() {
            if (networkSettings.containsKey("tunStack")) {
                return when (networkSettings.decodeString("tunStack", "System")) {
                    "System",
                    "system" -> "system"

                    "GVisor",
                    "gvisor" -> "gvisor"

                    "Mixed",
                    "mixed" -> "mixed"

                    "Mips",
                    "mips" -> "mips"

                    else -> "system"
                }
            }
            return store.provider.getString("tun_stack_mode", "gvisor")
        }
        set(value) {
            val normalized = value.lowercase()
            store.provider.setString("tun_stack_mode", normalized)
            networkSettings.encode(
                "tunStack",
                when (normalized) {
                    "system" -> "System"
                    "gvisor" -> "GVisor"
                    "mixed" -> "Mixed"
                    "mips" -> "Mips"
                    else -> "System"
                },
            )
        }

    var tunRouteExcludeAddress: List<String>
        get() = readStringList("tunRouteExcludeAddress", emptyList())
        set(value) {
            networkSettings.encode("tunRouteExcludeAddress", json.encodeToString(value))
        }

    var tunIfName: String
        get() = readString("tunIfName", "tun_if_name", "Yume")
        set(value) {
            networkSettings.encode("tunIfName", value)
            store.provider.setString("tun_if_name", value)
        }

    var tunMtu: Int
        get() = readInt("tunMtu", "tun_mtu", 1500)
        set(value) {
            networkSettings.encode("tunMtu", value)
            store.provider.setInt("tun_mtu", value)
        }

    var tunAutoRoute: Boolean
        get() = readBoolean("tunAutoRoute", "tun_auto_route", true)
        set(value) {
            networkSettings.encode("tunAutoRoute", value)
            store.provider.setBoolean("tun_auto_route", value)
        }

    var tunStrictRoute: Boolean
        get() = readBoolean("tunStrictRoute", "tun_strict_route", true)
        set(value) {
            networkSettings.encode("tunStrictRoute", value)
            store.provider.setBoolean("tun_strict_route", value)
        }

    var tunAutoRedirect: Boolean
        get() = readBoolean("tunAutoRedirect", "tun_auto_redirect", true)
        set(value) {
            networkSettings.encode("tunAutoRedirect", value)
            store.provider.setBoolean("tun_auto_redirect", value)
        }

    var tunIncludeAndroidUser: List<Int>
        get() = readIntList("tunIncludeAndroidUser", emptyList())
        set(value) {
            networkSettings.encode("tunIncludeAndroidUser", json.encodeToString(value))
        }

    var tunDnsMode: TunDnsMode
        get() = readEnum("tunDnsMode", TunDnsMode.RedirHost)
        set(value) {
            networkSettings.encode("tunDnsMode", value.name)
        }

    var tunFakeIpRange: String
        get() = readString("tunFakeIpRange", "tun_fake_ip_range", "198.18.0.1/16")
        set(value) {
            networkSettings.encode("tunFakeIpRange", value)
            store.provider.setString("tun_fake_ip_range", value)
        }

    var tunFakeIpRange6: String
        get() = readString("tunFakeIpRange6", "tun_fake_ip_range6", "fc00::/18")
        set(value) {
            networkSettings.encode("tunFakeIpRange6", value)
            store.provider.setString("tun_fake_ip_range6", value)
        }

    var showTrafficNotification by
        store.boolean(key = "show_traffic_notification", defaultValue = true)

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
