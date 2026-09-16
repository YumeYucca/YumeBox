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

package com.github.yumeyucca.yumebox.runtime.service.session

import com.github.yumeyucca.yumebox.core.model.OverrideSpec
import com.github.yumeyucca.yumebox.core.model.TunConfig
import com.github.yumeyucca.yumebox.core.model.TunDnsMode
import com.github.yumeyucca.yumebox.core.util.YamlCodec
import java.io.File

/**
 * Builds the built-in `tun:` fragment for Tun mode from the app-side `TunConfig`. The core opens
 * its OWN kernel TUN from this block (liboverride keeps it authoritative via the Rust `RunMode`
 * flag), so all geometry — interface, MTU, stack, auto-route/redirect, uid scoping — is chosen here
 * in the app.
 *
 * A partial `dns:` block flips `dns.enable` on and SUPPRESSES the compiler's full fake-ip injection
 * (`patch_static_runtime`); the compiler backfills the nameserver whenever this leaves DNS empty,
 * so Tun and VpnService resolve identically and the resolver is never left crippled.
 *
 * Delivered as an ordinary override, NOT a reserved one. Under disable-all in Tun the
 * factory omits this fragment and sets skipRuntimePatches, so the raw subscription wins. A plain
 * `tun:` object flows through liboverride's per-field merge and passes even keys outside the tun
 * schema through untouched.
 */
object TunOverride {
    const val FILE_NAME = "__tun_override__.yaml"

    fun buildYaml(config: TunConfig): String {
        val tun =
            linkedMapOf<String, Any?>(
                "enable" to true,
                "device" to config.ifName,
                "stack" to config.stack,
                "mtu" to config.mtu,
                "auto-route" to config.autoRoute,
                "strict-route" to config.strictRoute,
                "auto-redirect" to config.autoRedirect,
                // Tun mode replaces the VpnService's per-socket protect with auto-detect-interface:
                // the core follows the real default route so its own egress never loops into the
                // tun.
                "auto-detect-interface" to true,
                "dns-hijack" to config.dnsHijack,
                "inet4-address" to config.inet4Address,
            )
        if (config.allowIpv6 && config.inet6Address.isNotEmpty()) {
            tun["inet6-address"] = config.inet6Address
        }
        if (config.includeAndroidUser.isNotEmpty()) {
            tun["include-android-user"] = config.includeAndroidUser
        }
        if (config.includeUid.isNotEmpty()) tun["include-uid"] = config.includeUid
        if (config.excludeUid.isNotEmpty()) tun["exclude-uid"] = config.excludeUid
        if (config.excludeUidRange.isNotEmpty()) tun["exclude-uid-range"] = config.excludeUidRange
        if (config.routeAddress.isNotEmpty()) tun["route-address"] = config.routeAddress
        if (config.routeExcludeAddress.isNotEmpty()) {
            tun["route-exclude-address"] = config.routeExcludeAddress
        }

        // DNS follows the user's mode (redir-host works out of the box; fake-ip needs pool +
        // filter).
        // `enable` here suppresses the compiler's fake-ip injection, so it backfills the nameserver
        // when
        // this leaves it empty (patch_static_runtime) — the resolver is never left crippled.
        val fakeIp = config.dnsMode == TunDnsMode.FakeIp
        val dns =
            linkedMapOf<String, Any?>(
                "enable" to true,
                "enhanced-mode" to if (fakeIp) "fake-ip" else "redir-host",
            )
        if (fakeIp) {
            config.fakeIpRange?.takeIf { it.isNotBlank() }?.let { dns["fake-ip-range"] = it }
            if (config.allowIpv6) {
                config.fakeIpRange6?.takeIf { it.isNotBlank() }?.let { dns["fake-ip-range6"] = it }
            }
        }

        return YamlCodec.dumpMap(linkedMapOf<String, Any?>("tun" to tun, "dns" to dns))
    }

    /** Writes the override YAML into `dir` and returns its `OverrideSpec` for the compile chain. */
    fun materialize(config: TunConfig, dir: File): OverrideSpec {
        dir.mkdirs()
        val file = File(dir, FILE_NAME)
        file.writeText(buildYaml(config))
        return OverrideSpec(path = file.absolutePath, ext = "yaml")
    }
}
