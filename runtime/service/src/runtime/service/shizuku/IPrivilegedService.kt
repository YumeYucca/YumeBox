/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 */

package com.github.yumeyucca.yumebox.runtime.service.shizuku

import android.os.Binder
import android.os.IBinder
import android.os.IInterface
import android.os.Parcel

/** Binder contract used by the Shizuku UserService. */
interface IPrivilegedService : IInterface {
    fun setPackageNetworkingEnabled(uid: Int, enabled: Boolean): Boolean

    abstract class Stub : Binder(), IPrivilegedService {
        init {
            attachInterface(this, DESCRIPTOR)
        }

        override fun asBinder(): IBinder = this

        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            if (code == INTERFACE_TRANSACTION) {
                reply?.writeString(DESCRIPTOR)
                return true
            }
            if (code != TRANSACTION_SET_PACKAGE_NETWORKING_ENABLED) {
                return super.onTransact(code, data, reply, flags)
            }

            data.enforceInterface(DESCRIPTOR)
            val uid = data.readInt()
            val enabled = data.readInt() != 0
            val result = setPackageNetworkingEnabled(uid, enabled)
            reply?.writeNoException()
            reply?.writeInt(if (result) 1 else 0)
            return true
        }

        companion object {
            private const val DESCRIPTOR =
                "com.github.yumeyucca.yumebox.runtime.service.shizuku.IPrivilegedService"
            private const val TRANSACTION_SET_PACKAGE_NETWORKING_ENABLED = FIRST_CALL_TRANSACTION

            fun asInterface(binder: IBinder?): IPrivilegedService? {
                if (binder == null) return null
                val local = binder.queryLocalInterface(DESCRIPTOR)
                return if (local is IPrivilegedService) local else Proxy(binder)
            }
        }

        private class Proxy(private val remote: IBinder) : IPrivilegedService {
            override fun asBinder(): IBinder = remote

            override fun setPackageNetworkingEnabled(uid: Int, enabled: Boolean): Boolean {
                val data = Parcel.obtain()
                val reply = Parcel.obtain()
                return try {
                    data.writeInterfaceToken(DESCRIPTOR)
                    data.writeInt(uid)
                    data.writeInt(if (enabled) 1 else 0)
                    remote.transact(
                        TRANSACTION_SET_PACKAGE_NETWORKING_ENABLED,
                        data,
                        reply,
                        0,
                    )
                    reply.readException()
                    reply.readInt() != 0
                } finally {
                    reply.recycle()
                    data.recycle()
                }
            }
        }
    }
}
