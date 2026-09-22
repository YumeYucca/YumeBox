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
        private const val TIMEOUT_SECONDS = 3L
    }

    override fun setPackageNetworkingEnabled(uid: Int, enabled: Boolean): Boolean {
        val connectivity =
            runCatching {
                Class
                    .forName("android.os.ServiceManager")
                    .getMethod("getService", String::class.java)
                    .invoke(null, "connectivity") as? IBinder
            }.getOrNull()
        if (connectivity == null) {
            Log.e(TAG, "Connectivity service is not available")
            return false
        }

        // The update runs on its own thread so a hanging transaction cannot block the caller's
        // binder call forever.
        val latch = CountDownLatch(1)
        var result = false
        Thread {
            try {
                result = OemDenyFirewall.setPackageDenied(connectivity, uid, denied = !enabled)
            } finally {
                latch.countDown()
            }
        }.start()

        if (!latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            Log.w(TAG, "Firewall update timed out")
            return false
        }
        return result
    }
}
