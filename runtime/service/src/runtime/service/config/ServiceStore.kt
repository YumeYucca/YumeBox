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


import com.github.yumeyucca.yumebox.core.util.LenientJson
import com.github.yumeyucca.yumebox.data.model.TunStack
import com.tencent.mmkv.MMKV
import java.util.*
import timber.log.Timber

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
            .onFailure { Timber.w(it, "Unreadable %s in the service store, falling back to defaults", newKey) }
            .getOrDefault(defaultValue)
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

    /**
     * The service's view of the access control mode.
     *
     * Read-only: the value is owned by the UI (it writes the data-layer names through
     * `NetworkSettingsStore`), so the raw key is parsed here with both spellings accepted. A
     * stored value this build cannot parse falls back to accept-all, which is what the core
     * does for an unpinned state, and says so in the log instead of doing it silently.
     */
    val accessControlMode: AccessControlMode
        get() {
            val raw =
                readString(
                    newKey = "accessControlMode",
                    legacyKey = "access_control_mode",
                    defaultValue = AccessControlMode.AcceptAll.name,
                )
            return when (raw) {
                AccessControlMode.AcceptAll.name,
                "ALLOW_ALL" -> AccessControlMode.AcceptAll

                AccessControlMode.AcceptSelected.name,
                "ALLOW_SPECIFIC" -> AccessControlMode.AcceptSelected

                AccessControlMode.RejectAll.name,
                "DENY_ALL" -> AccessControlMode.RejectAll

                AccessControlMode.RejectSelected.name,
                "DENY_SPECIFIC" -> AccessControlMode.RejectSelected

                else -> {
                    Timber.w("Unknown access control mode %s, assuming accept-all", raw)
                    AccessControlMode.AcceptAll
                }
            }
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
        get() = readBoolean(newKey = "enableIPv6", legacyKey = "allow_ipv6", defaultValue = false)
        set(value) {
            networkSettings.encode("enableIPv6", value)
            store.provider.setBoolean("allow_ipv6", value)
        }

    /**
     * The stack the user picked, written by the UI through `NetworkSettingsStore` as [TunStack.name].
     * Read-only like [accessControlMode] because the service never owns the setting, and the legacy
     * key held the lowercase core spelling, so both spellings are accepted here.
     */
    val tunStack: TunStack
        get() {
            val raw =
                readString(
                    newKey = "tunStack",
                    legacyKey = "tun_stack_mode",
                    defaultValue = TunStack.GVisor.name,
                )
            return TunStack.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
                ?: run {
                    Timber.w("Unknown tun stack %s, assuming gvisor", raw)
                    TunStack.GVisor
                }
        }

    var tunRouteExcludeAddress: List<String>
        get() = readStringList("tunRouteExcludeAddress", emptyList())
        set(value) {
            networkSettings.encode("tunRouteExcludeAddress", json.encodeToString(value))
        }

    var showTrafficNotification by
        store.boolean(key = "show_traffic_notification", defaultValue = true)

    private companion object {
        val json = LenientJson
    }
}
