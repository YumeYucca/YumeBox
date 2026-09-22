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

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Access to the Shizuku / Sui privileged service.
 *
 * Sui needs no explicit initialization here: `rikka.shizuku.ShizukuProvider` (declared in the app
 * manifest) initializes it in its `onCreate`, which runs before `Application.onCreate`. This app
 * runs in a single process, so the provider also serves the Shizuku binder.
 */
object ShizukuManager {
    private const val TAG = "YumeBoxShizuku"
    private const val XMSF_PACKAGE = "com.xiaomi.xmsf"
    private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    private const val PERMISSION_REQUEST_CODE = 1001
    private const val USER_SERVICE_VERSION = 1
    private const val BIND_TIMEOUT_SECONDS = 3L

    /**
     * How long the XMSF network block is kept after the notification was posted. HyperOS decides
     * whether to show the island entry while the notification is delivered, so the block has to
     * outlive the `startForeground()` call by a small margin.
     */
    private const val ISLAND_SETTLE_DELAY_MS = 100L

    /**
     * UserService Shizuku instantiates in its own process. Packed builds keep the class in the APK's
     * loader DEX (see :pack), because the packed payload below /data/user/0/<pkg> is not readable
     * for the shell user Shizuku runs as. Builds without a loader DEX fall back to the
     * implementation that ships in the APK's own DEX files.
     */
    private const val LOADER_USER_SERVICE_CLASS = "dev.yume.loader.ShizukuUserService"

    private val userServiceClass: String by lazy {
        runCatching { Class.forName(LOADER_USER_SERVICE_CLASS) }
            .fold(
                onSuccess = { LOADER_USER_SERVICE_CLASS },
                onFailure = { PrivilegedServiceImpl::class.java.name },
            )
    }

    // Written by the service connection callback, read from the traffic updater's thread.
    @Volatile private var privilegedService: IPrivilegedService? = null
    @Volatile private var serviceConnected = false
    private var bindLatch = CountDownLatch(1)
    private val bypassMutex = Mutex()

    private val serviceConnection =
        object : android.content.ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                binder: IBinder?,
            ) {
                if (binder != null && binder.pingBinder()) {
                    privilegedService = IPrivilegedService.Stub.asInterface(binder)
                    serviceConnected = true
                    bindLatch.countDown()
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                privilegedService = null
                serviceConnected = false
                bindLatch = CountDownLatch(1)
            }
        }

    fun isAvailable(): Boolean =
        runCatching {
            Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)

    fun isRunning(): Boolean = runCatching { Shizuku.pingBinder() }.getOrDefault(false)

    fun hasPermission(): Boolean =
        runCatching {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)

    fun requestPermission(callback: (Boolean) -> Unit) {
        if (!isRunning()) {
            callback(false)
            return
        }
        if (hasPermission()) {
            callback(true)
            return
        }

        val listener =
            object : Shizuku.OnRequestPermissionResultListener {
                override fun onRequestPermissionResult(
                    requestCode: Int,
                    grantResult: Int,
                ) {
                    Shizuku.removeRequestPermissionResultListener(this)
                    Log.d(TAG, "Shizuku permission result: code=$requestCode grant=$grantResult")
                    callback(grantResult == PackageManager.PERMISSION_GRANTED)
                }
            }
        Shizuku.addRequestPermissionResultListener(listener)
        runCatching { Shizuku.requestPermission(PERMISSION_REQUEST_CODE) }
            .onFailure { error ->
                Log.w(TAG, "Shizuku permission request failed", error)
                Shizuku.removeRequestPermissionResultListener(listener)
                callback(false)
            }
    }

    fun openShizuku(context: Context): Boolean =
        runCatching {
            val intent =
                context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
                    ?: return@runCatching false
            context.startActivity(intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }.getOrDefault(false)

    /** Runs [block] with the network of the XMSF package blocked, and restores it afterwards. */
    suspend fun <T> withXmsfNetworkingDisabled(
        context: Context,
        block: () -> T,
    ): T {
        if (!isAvailable()) return block()

        return bypassMutex.withLock {
            val blocked = setXmsfNetworkingBlocked(context, blocked = true)
            if (!blocked) return@withLock block()

            try {
                block().also { delay(ISLAND_SETTLE_DELAY_MS) }
            } finally {
                setXmsfNetworkingBlocked(context, blocked = false)
            }
        }
    }

    private fun setXmsfNetworkingBlocked(
        context: Context,
        blocked: Boolean,
    ): Boolean {
        val xmsfUid =
            runCatching { context.packageManager.getPackageUid(XMSF_PACKAGE, 0) }
                .onFailure { error -> Log.w(TAG, "$XMSF_PACKAGE is not installed", error) }
                .getOrNull()
                ?: return false

        getPrivilegedService(context)?.let { service ->
            return runCatching { service.setPackageNetworkingEnabled(xmsfUid, !blocked) }
                .onFailure { error -> Log.w(TAG, "User service call failed", error) }
                .getOrDefault(false)
        }

        // Fallback when the user service cannot be bound: drive the connectivity service directly
        // through the Shizuku binder, which runs with the same privileged identity.
        val connectivity = SystemServiceHelper.getSystemService("connectivity") ?: return false
        return OemDenyFirewall.setPackageDenied(
            connectivity = ShizukuBinderWrapper(connectivity),
            uid = xmsfUid,
            denied = blocked,
        )
    }

    private fun getPrivilegedService(context: Context): IPrivilegedService? {
        privilegedService?.takeIf { serviceConnected }?.let { return it }

        return runCatching {
            bindLatch = CountDownLatch(1)
            val args =
                Shizuku
                    .UserServiceArgs(
                        ComponentName(context.packageName, userServiceClass),
                    ).daemon(false)
                    .processNameSuffix("privileged")
                    .debuggable(false)
                    .version(USER_SERVICE_VERSION)
            Shizuku.bindUserService(args, serviceConnection)
            if (bindLatch.await(BIND_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                privilegedService
            } else {
                Log.w(TAG, "Timed out binding the Shizuku user service")
                null
            }
        }
            .onFailure { error -> Log.w(TAG, "Unable to bind the Shizuku user service", error) }
            .getOrNull()
    }
}
