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

@file:Suppress("JavaDefaultMethodsNotOverriddenByDelegation", "UnusedSymbol", "RedundantIf")

package com.github.yumeyucca.yumebox.core.model

import android.os.Parcel
import android.os.Parcelable
import com.github.yumeyucca.yumebox.core.util.createListFromParcelSlice
import com.github.yumeyucca.yumebox.core.util.writeToParcelSlice
import kotlinx.serialization.Serializable

@Serializable
data class ProxyGroup(
    val name: String = "",
    val type: String,
    val proxies: List<Proxy>,
    val now: String,
    val icon: String? = null,
    val hidden: Boolean = false,
) : Parcelable {
    class SliceProxyList(proxies: List<Proxy>) : List<Proxy> by proxies, Parcelable {
        constructor(parcel: Parcel) : this(Proxy.createListFromParcelSlice(parcel, 50))

        override fun describeContents(): Int = 0

        override fun writeToParcel(dest: Parcel, flags: Int) {
            writeToParcelSlice(dest, flags)
        }

        companion object CREATOR : Parcelable.Creator<SliceProxyList> {
            override fun createFromParcel(parcel: Parcel): SliceProxyList = SliceProxyList(parcel)

            override fun newArray(size: Int): Array<SliceProxyList?> = arrayOfNulls(size)
        }
    }

    constructor(
        parcel: Parcel
    ) : this(
        type = parcel.readString().orEmpty(),
        proxies = SliceProxyList(parcel),
        now = parcel.readString().orEmpty(),
        icon = parcel.readString(),
        name = if (parcel.dataAvail() > 0) parcel.readString().orEmpty() else "",
        hidden = if (parcel.dataAvail() > 0) parcel.readByte().toInt() != 0 else false,
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(type)
        SliceProxyList(proxies).writeToParcel(parcel, 0)
        parcel.writeString(now)
        parcel.writeString(icon)
        parcel.writeString(name)
        parcel.writeByte(if (hidden) 1.toByte() else 0.toByte())
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<ProxyGroup> {
        override fun createFromParcel(parcel: Parcel): ProxyGroup = ProxyGroup(parcel)

        override fun newArray(size: Int): Array<ProxyGroup?> = arrayOfNulls(size)
    }
}

val ProxyGroup.isSelectable: Boolean
    get() = type.isManuallySelectable

val ProxyGroup.isProxyGroup: Boolean
    get() = type in Proxy.Type.groupTypes || now.isNotBlank() || proxies.isNotEmpty()
