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

package com.github.yumeyucca.yumebox.core.util

import android.os.Binder
import android.os.Parcel
import android.os.Parcelable

private class SliceParcelableListBpBinder(
    val items: List<Parcelable>,
    val flags: Int,
) : Binder() {
    override fun onTransact(
        code: Int,
        request: Parcel,
        reply: Parcel?,
        transactionFlags: Int,
    ): Boolean {
        when (code) {
            TRANSACTION_GET_ITEMS -> {
                reply ?: return false

                val offset = request.readInt()
                val chunk = request.readInt()
                if (offset !in 0..items.size || chunk <= 0) return false

                val end = (offset.toLong() + chunk).coerceAtMost(items.size.toLong()).toInt()

                reply.writeInt(end - offset)

                for (i in offset until end) {
                    items[i].writeToParcel(reply, flags)
                }

                return true
            }
        }

        return super.onTransact(code, request, reply, transactionFlags)
    }

    companion object {
        const val TRANSACTION_GET_ITEMS = 10
    }
}

fun <T : Parcelable> List<T>.writeToParcelSlice(parcel: Parcel, flags: Int) {
    val bp = SliceParcelableListBpBinder(this, flags)

    parcel.writeInt(size)
    parcel.writeStrongBinder(bp)
}

fun <T : Parcelable> Parcelable.Creator<T>.createListFromParcelSlice(
    parcel: Parcel,
    chunk: Int,
): List<T> {
    val total = parcel.readInt()
    require(total >= 0) { "Negative parcel slice size: $total" }
    require(chunk > 0) { "Parcel slice chunk must be positive" }
    val remote = parcel.readStrongBinder()
    val result = ArrayList<T>(total)

    var offset = 0

    while (offset < total) {
        val request = Parcel.obtain()
        val reply = Parcel.obtain()

        try {
            val requested = minOf(chunk, total - offset)
            request.writeInt(offset)
            request.writeInt(requested)

            check(
                remote.transact(
                    SliceParcelableListBpBinder.TRANSACTION_GET_ITEMS,
                    request,
                    reply,
                    0,
                )
            ) {
                "Parcel slice transaction failed at offset $offset"
            }

            val size = reply.readInt()
            check(size in 1..requested) {
                "Invalid parcel slice size $size at offset $offset (requested $requested)"
            }

            repeat(size) { result.add(createFromParcel(reply)) }
            offset += size
        } finally {
            request.recycle()
            reply.recycle()
        }
    }

    check(result.size == total) {
        "Parcel slice truncated: expected $total items, received ${result.size}"
    }
    return result
}
