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

package com.github.yumeyucca.yumebox.runtime.service.session

import android.app.PendingIntent
import android.content.Intent
import android.net.IpPrefix
import android.net.ProxyInfo
import android.net.VpnService
import android.os.Build
import com.github.yumeyucca.yumebox.runtime.api.Components
import com.github.yumeyucca.yumebox.runtime.service.R
import com.github.yumeyucca.yumebox.runtime.service.config.AccessControlMode
import com.github.yumeyucca.yumebox.runtime.service.config.ServiceStore
import com.github.yumeyucca.yumebox.runtime.service.core.CoreProcess
import com.github.yumeyucca.yumebox.runtime.service.log.RuntimeLog
import com.github.yumeyucca.yumebox.runtime.service.util.buildIncludedRoutesFromExcludedCidrs
import com.github.yumeyucca.yumebox.runtime.service.util.parseCIDR
import kotlinx.coroutines.runBlocking
import java.net.InetAddress

class VpnTunTransport(
    private val vpnService: VpnService,
    private val store: ServiceStore = ServiceStore(),
) : RuntimeTransport {
    private val log = RuntimeLog.writer(vpnService, RuntimeLog.Source.LocalTun)
    private val pipeline = CompiledConfigPipeline(vpnService)
    private val core = CoreProcess(vpnService)

    override fun start(spec: RuntimeSpec) {
        log.i(RuntimeLog.Type.Transport, "start begin")
        // Prefer the precompiled YAML attached to the spec (single compile on the start path).
        // Fall back to a local compile only for callers that have not prepared the spec yet.
        val config =
            if (spec.compiledFinalYaml.isNotBlank()) {
                spec.compiledFinalYaml
            } else {
                runBlocking { pipeline.compile(spec) }
            }
        val device =
            with(vpnService.Builder()) {
                val explicitRouteExcludes =
                    store.tunRouteExcludeAddress.map(String::trim).filter(String::isNotEmpty)

                addAddress(TUN_GATEWAY, TUN_SUBNET_PREFIX)
                if (store.allowIpv6) {
                    addAddress(TUN_GATEWAY6, TUN_SUBNET_PREFIX6)
                }

                configureRoutes(explicitRouteExcludes)
                configurePerAppRouting()

                setBlocking(false)
                setMtu(TUN_MTU)
                setSession("YumeBox")
                addDnsServer(TUN_DNS)
                if (store.allowIpv6) {
                    addDnsServer(TUN_DNS6)
                }
                setConfigureIntent(
                    PendingIntent.getActivity(
                        vpnService,
                        R.id.nf_vpn_status,
                        Intent()
                            .setComponent(Components.PROXY_SHEET_ACTIVITY)
                            .addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                    Intent.FLAG_ACTIVITY_NO_ANIMATION
                            ),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                )

                if (Build.VERSION.SDK_INT >= 29) {
                    setMetered(false)
                }

                if (store.allowBypass) {
                    allowBypass()
                }

                // VPN system HTTP proxy: point apps at the core's mixed/http port so apps that
                // honour
                // the system proxy (rather than only the TUN) still route through the core. API
                // 29+.
                if (Build.VERSION.SDK_INT >= 29 && store.systemProxy) {
                    httpProxyPort(config)?.let { port ->
                        setHttpProxy(
                            ProxyInfo.buildDirectProxy("127.0.0.1", port, httpProxyExclusionList)
                        )
                    }
                }

                TunDevice(
                    fd = establish()?.detachFd() ?: error("Establish VPN rejected by system"),
                    gateway =
                        "$TUN_GATEWAY/$TUN_SUBNET_PREFIX" +
                            if (store.allowIpv6) ",$TUN_GATEWAY6/$TUN_SUBNET_PREFIX6" else "",
                    dns =
                        if (store.dnsHijacking) {
                            NET_ANY
                        } else {
                            (TUN_DNS + if (store.allowIpv6) ",$TUN_DNS6" else "")
                        },
                )
            }

        core.startVpn(
            tunFd = device.fd,
            gateway = device.gateway,
            dns = device.dns,
            config = config,
            stack = vpnTunStack(store.tunStackMode),
        )
        log.i(RuntimeLog.Type.Transport, "success: tun attached and core launched")
    }

    /**
     * Route table: explicit config route excludes win (native excludeRoute on API 33+, computed
     * included routes below), then the private-network bypass list, otherwise route everything.
     */
    private fun VpnService.Builder.configureRoutes(explicitRouteExcludes: List<String>) {
        val hasExplicitRouteExcludes = explicitRouteExcludes.isNotEmpty()
        val canUseExcludeRoute = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        val includedRoutes =
            if (hasExplicitRouteExcludes && !canUseExcludeRoute) {
                buildIncludedRoutesFromExcludedCidrs(
                    cidrs = explicitRouteExcludes,
                    includeIpv6 = store.allowIpv6,
                )
            } else {
                null
            }

        if (hasExplicitRouteExcludes && canUseExcludeRoute) {
            addRoute(NET_ANY, 0)
            if (store.allowIpv6) {
                addRoute(NET_ANY6, 0)
            }
            explicitRouteExcludes.map(::parseCIDR).forEach {
                runCatching {
                    excludeRoute(IpPrefix(InetAddress.getByName(it.ip), it.prefix))
                }
            }
            addRoute(TUN_DNS, 32)
            if (store.allowIpv6) {
                addRoute(TUN_DNS6, 128)
            }
        } else if (includedRoutes != null) {
            includedRoutes.ipv4.forEach { addRoute(it.ip, it.prefix) }
            if (store.allowIpv6) {
                includedRoutes.ipv6.forEach { addRoute(it.ip, it.prefix) }
            }
            addRoute(TUN_DNS, 32)
            if (store.allowIpv6) {
                addRoute(TUN_DNS6, 128)
            }
        } else if (store.bypassPrivateNetwork) {
            vpnService.resources
                .getStringArray(R.array.bypass_private_route)
                .map(::parseCIDR)
                .forEach { addRoute(it.ip, it.prefix) }
            if (store.allowIpv6) {
                vpnService.resources
                    .getStringArray(R.array.bypass_private_route6)
                    .map(::parseCIDR)
                    .forEach { addRoute(it.ip, it.prefix) }
            }
            addRoute(TUN_DNS, 32)
            if (store.allowIpv6) {
                addRoute(TUN_DNS6, 128)
            }
        } else {
            addRoute(NET_ANY, 0)
            if (store.allowIpv6) {
                addRoute(NET_ANY6, 0)
            }
        }
    }

    /** Core sockets are protected individually; the app's own traffic must stay in the VPN. */
    private fun VpnService.Builder.configurePerAppRouting() {
        val self = vpnService.packageName
        log.i(RuntimeLog.Type.Transport, "per-app routing: ui mode=${store.accessControlMode}")
        when (store.accessControlMode) {
            AccessControlMode.AcceptAll,
            AccessControlMode.RejectAll -> Unit

            AccessControlMode.AcceptSelected ->
                (store.accessControlPackages + self).forEach {
                    runCatching { addAllowedApplication(it) }
                }

            AccessControlMode.RejectSelected -> {
                (store.accessControlPackages - self).forEach {
                    runCatching { addDisallowedApplication(it) }
                }
            }
        }
    }

    /**
     * The core's HTTP proxy port from the compiled config (mixed-port preferred, else http port).
     */
    private fun httpProxyPort(config: String): Int? =
        portFromConfig(config, "mixed-port") ?: portFromConfig(config, "port")

    private fun portFromConfig(config: String, key: String): Int? =
        config
            .lineSequence()
            .map(String::trimStart)
            .firstOrNull { it.startsWith("$key:") }
            ?.substringAfter("$key:")
            ?.trim()
            ?.toIntOrNull()
            ?.takeIf { it in 1..65535 }

    override fun stop() {
        core.stop()
    }

    override fun onNetworkChanged() {
        if (Build.VERSION.SDK_INT in 22..28) {
            @Suppress("DEPRECATION") vpnService.setUnderlyingNetworks(null)
        }
    }

    private data class TunDevice(
        val fd: Int,
        val gateway: String,
        val dns: String,
    )

    private companion object {
        fun vpnTunStack(mode: String): String =
            if (mode.equals("mips", ignoreCase = true)) "mips" else "gvisor"

        // Keep in lockstep with the core's tun MTU (native/tun/tun.go). 1500 is the standard value;
        // a jumbo 9000 MTU broke path-MTU on some paths, so both sides pin 1500.
        private const val TUN_MTU = 1500
        private const val TUN_SUBNET_PREFIX = 30
        private const val TUN_GATEWAY = "172.19.0.1"
        private const val TUN_SUBNET_PREFIX6 = 126
        private const val TUN_GATEWAY6 = "fdfe:dcba:9876::1"
        private const val TUN_PORTAL = "172.19.0.2"
        private const val TUN_PORTAL6 = "fdfe:dcba:9876::2"
        private const val TUN_DNS = TUN_PORTAL
        private const val TUN_DNS6 = TUN_PORTAL6
        private const val NET_ANY = "0.0.0.0"
        private const val NET_ANY6 = "::"

        private val httpProxyExclusionList =
            listOf(
                "localhost",
                "*.local",
                "127.*",
                "10.*",
                "172.16.*",
                "172.17.*",
                "172.18.*",
                "172.19.*",
                "172.2*",
                "172.30.*",
                "172.31.*",
                "192.168.*",
                "*zhihu.com",
                "*zhimg.com",
                "*jd.com",
                "100ime-iat-api.xfyun.cn",
                "*360buyimg.com",
            )
    }
}
