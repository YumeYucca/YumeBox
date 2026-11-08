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

@file:Suppress("ConstPropertyName")

package com.github.yumeyucca.yumebox.core.model

import android.os.Parcel
import android.os.Parcelable
import com.github.yumeyucca.yumebox.core.util.Parcelizer
import kotlinx.serialization.Serializable

@Serializable
data class Proxy(
    val name: String,
    val title: String,
    val subtitle: String,
    val type: String,
    val delay: Int,
    val isGroup: Boolean = type in Type.groupTypes,
) : Parcelable {
    @Suppress("unused")
    object Type {
        const val Direct = "Direct"
        const val Reject = "Reject"
        const val RejectDrop = "RejectDrop"
        const val Compatible = "Compatible"
        const val Pass = "Pass"
        const val Shadowsocks = "Shadowsocks"
        const val ShadowsocksR = "ShadowsocksR"
        const val Snell = "Snell"
        const val Socks5 = "Socks5"
        const val Http = "Http"
        const val Vmess = "Vmess"
        const val Vless = "Vless"
        const val Trojan = "Trojan"
        const val Hysteria = "Hysteria"
        const val Hysteria2 = "Hysteria2"
        const val Tuic = "Tuic"
        const val WireGuard = "WireGuard"
        const val Dns = "Dns"
        const val Ssh = "Ssh"
        const val Mieru = "Mieru"
        const val AnyTLS = "AnyTLS"
        const val Sudoku = "Sudoku"
        const val Masque = "Masque"
        const val TrustTunnel = "TrustTunnel"
        const val OpenVPN = "OpenVPN"
        const val Tailscale = "Tailscale"
        const val GostRelay = "GostRelay"
        const val Relay = "Relay"
        const val Selector = "Selector"
        const val Fallback = "Fallback"
        const val URLTest = "URLTest"
        const val LoadBalance = "LoadBalance"
        const val Smart = "Smart"
        const val PassRule = "PassRule"
        const val Unknown = "Unknown"

        val groupTypes = setOf(Relay, Selector, Fallback, URLTest, LoadBalance, Smart)
        val manuallySelectable = setOf(Selector, URLTest, Fallback)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        Parcelizer.encodeToParcel(serializer(), parcel, this)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Proxy> {
        override fun createFromParcel(parcel: Parcel): Proxy = Parcelizer.decodeFromParcel(serializer(), parcel)

        override fun newArray(size: Int): Array<Proxy?> = arrayOfNulls(size)
    }
}

val Proxy.isProxyGroup: Boolean
    get() = isGroup || type in Proxy.Type.groupTypes

internal val String.isManuallySelectable: Boolean
    get() = this in Proxy.Type.manuallySelectable
