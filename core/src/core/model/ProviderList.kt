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


@file:Suppress("JavaDefaultMethodsNotOverriddenByDelegation")

package com.github.yumeyucca.yumebox.core.model

import android.os.Parcel
import android.os.Parcelable
import com.github.yumeyucca.yumebox.core.util.createListFromParcelSlice
import com.github.yumeyucca.yumebox.core.util.writeToParcelSlice

class ProviderList(providers: List<Provider>) : List<Provider> by providers, Parcelable {
    constructor(parcel: Parcel) : this(Provider.createListFromParcelSlice(parcel, 20))

    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) = writeToParcelSlice(parcel, flags)

    companion object CREATOR : Parcelable.Creator<ProviderList> {
        override fun createFromParcel(parcel: Parcel): ProviderList = ProviderList(parcel)

        override fun newArray(size: Int): Array<ProviderList?> = arrayOfNulls(size)
    }
}
