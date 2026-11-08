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

package com.github.yumeyucca.yumebox.core.model

import android.os.Parcel
import android.os.Parcelable
import com.github.yumeyucca.yumebox.core.util.Parcelizer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TunnelState(val mode: Mode) : Parcelable {
    @Serializable
    enum class Mode {
        @SerialName("direct") Direct,
        @SerialName("global") Global,
        @SerialName("rule") Rule,
        @SerialName("script") Script,
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        Parcelizer.encodeToParcel(serializer(), parcel, this)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<TunnelState> {
        override fun createFromParcel(parcel: Parcel): TunnelState = Parcelizer.decodeFromParcel(serializer(), parcel)

        override fun newArray(size: Int): Array<TunnelState?> = arrayOfNulls(size)
    }
}
