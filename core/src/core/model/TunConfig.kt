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

package com.github.yumeyucca.yumebox.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Geometry for the root native-TUN run mode: the core opens its own kernel device from these. */
@Serializable
data class TunConfig(
    val ifName: String = DefaultIfName,
    val mtu: Int = DefaultMtu,
    val stack: String = "gvisor",
    val inet4Address: List<String> = listOf("172.19.0.1/30"),
    val inet6Address: List<String> = listOf("fdfe:dcba:9876::1/126"),
    val dnsHijack: List<String> = listOf("any:53", "tcp://any:53"),
    val autoRoute: Boolean = true,
    // strict-route adds FR_ACT_UNREACHABLE fallback rules that can black-hole traffic; off by
    // default.
    val strictRoute: Boolean = false,
    // auto-redirect programs the iptables TCP redirect that carries app egress, so on by default.
    val autoRedirect: Boolean = true,
    val includeUid: List<Int> = emptyList(),
    val excludeUid: List<Int> = emptyList(),
    // Exclude system uids (0–9999, incl. the root core's own uid 0) or the core's egress loops back
    // into
    // the tun it created and no traffic passes. Grammar is mihomo's "start:end" (colon, NOT a
    // hyphen).
    val excludeUidRange: List<String> = listOf("0:9999"),
    // Empty = do not emit include-android-user; sing-tun then covers every Android user.
    // A non-empty list is a hard allowlist and excludes every other user via ExcludeUID.
    val includeAndroidUser: List<Int> = emptyList(),
    val routeAddress: List<String> = emptyList(),
    val routeExcludeAddress: List<String> = emptyList(),
    val dnsMode: TunDnsMode = TunDnsMode.RedirHost,
    val fakeIpRange: String? = DefaultFakeIpRange,
    val fakeIpRange6: String? = DefaultFakeIpRange6,
    val allowIpv6: Boolean = true,
    val debugLogPath: String? = null,
) {
    companion object {
        /**
         * Defaults shared with the settings store, so a fresh install and a locally built config
         * start from exactly the same values instead of two literals that have to be kept in step.
         */
        const val DefaultIfName: String = "Yume"
        const val DefaultMtu: Int = 1500
        const val DefaultFakeIpRange: String = "198.18.0.1/16"
        const val DefaultFakeIpRange6: String = "fc00::/18"
    }
}

@Serializable
enum class TunDnsMode {
    @SerialName("redir-host")
    RedirHost,
    @SerialName("fake-ip")
    FakeIp,
}
