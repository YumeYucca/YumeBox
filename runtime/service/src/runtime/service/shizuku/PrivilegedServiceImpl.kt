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

package com.github.yumeyucca.yumebox.runtime.service.shizuku

import android.os.IBinder
import android.util.Log
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Shizuku UserService used for the short XMSF firewall bypass around island updates. */
class PrivilegedServiceImpl : IPrivilegedService.Stub() {
    companion object {
        private const val TAG = "YumeBoxPrivilegedService"
        private const val FIREWALL_CHAIN_OEM_DENY = 9
        private const val TIMEOUT_SECONDS = 3L
    }

    override fun setPackageNetworkingEnabled(uid: Int, enabled: Boolean): Boolean {
        return try {
            val serviceManagerClass = Class.forName("android.os.ServiceManager")
            val getServiceMethod = serviceManagerClass.getMethod("getService", String::class.java)
            val connectivityBinder = getServiceMethod.invoke(null, "connectivity") as? IBinder
                ?: throw IllegalStateException("Connectivity service not found")
            val connectivityManager = Class.forName("android.net.IConnectivityManager\$Stub")
                .getMethod("asInterface", IBinder::class.java)
                .invoke(null, connectivityBinder)

            val latch = CountDownLatch(1)
            var result = false
            Thread {
                try {
                    val setChainEnabled = connectivityManager.javaClass.getMethod(
                        "setFirewallChainEnabled",
                        Int::class.javaPrimitiveType,
                        Boolean::class.javaPrimitiveType,
                    )
                    setChainEnabled.invoke(connectivityManager, FIREWALL_CHAIN_OEM_DENY, true)

                    val setUidRule = connectivityManager.javaClass.getMethod(
                        "setUidFirewallRule",
                        Int::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType,
                    )
                    val rule = if (enabled) 0 else 2
                    setUidRule.invoke(
                        connectivityManager,
                        FIREWALL_CHAIN_OEM_DENY,
                        uid,
                        rule,
                    )
                    result = true
                } catch (error: Throwable) {
                    Log.e(TAG, "Failed to update package firewall rule", error)
                } finally {
                    latch.countDown()
                }
            }.start()

            if (!latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                Log.w(TAG, "Firewall update timed out")
                false
            } else {
                result
            }
        } catch (error: Throwable) {
            Log.e(TAG, "Failed to update package networking", error)
            false
        }
    }
}
