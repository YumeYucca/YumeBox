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

/**
 * Adds or removes the OEM deny firewall rule of a uid through the hidden connectivity API. Callers
 * only have to bring a connectivity binder obtained in their own process: [ShizukuManager] wraps
 * the system service through Shizuku, [PrivilegedServiceImpl] runs inside the Shizuku user service.
 *
 * The copy that cannot share this code is `dev.yume.loader.ShizukuUserService` (see :pack): it has
 * to live in the APK's loader DEX and therefore keeps its own version of this call.
 */
internal object OemDenyFirewall {
    private const val TAG = "YumeBoxFirewall"

    /** `FirewallChain.OEM_DENY`, the chain HyperOS uses for its own deny list. */
    private const val CHAIN_OEM_DENY = 9
    private const val RULE_ALLOW = 0
    private const val RULE_DENY = 2

    fun setPackageDenied(
        connectivity: IBinder,
        uid: Int,
        denied: Boolean,
    ): Boolean =
        runCatching {
            val manager =
                Class
                    .forName("android.net.IConnectivityManager\$Stub")
                    .getMethod("asInterface", IBinder::class.java)
                    .invoke(null, connectivity)
                    ?: error("connectivity service binder is not available")

            manager.javaClass
                .getMethod(
                    "setFirewallChainEnabled",
                    Int::class.javaPrimitiveType,
                    Boolean::class.javaPrimitiveType,
                )
                .invoke(manager, CHAIN_OEM_DENY, true)

            manager.javaClass
                .getMethod(
                    "setUidFirewallRule",
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                )
                .invoke(manager, CHAIN_OEM_DENY, uid, if (denied) RULE_DENY else RULE_ALLOW)
            true
        }
            .onFailure { error ->
                Log.e(TAG, "Failed to update the OEM deny rule of uid $uid", error)
            }
            .getOrDefault(false)
}
