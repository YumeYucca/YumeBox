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
import rikka.sui.Sui
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object ShizukuManager {
    private const val TAG = "YumeBoxShizuku"
    private const val XMSF_PACKAGE = "com.xiaomi.xmsf"
    private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    private const val FIREWALL_CHAIN_OEM_DENY = 9
    private const val PERMISSION_REQUEST_CODE = 1001

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

    /**
     * HyperOS signal that the Xiaomi Super Island is available. Read once: the system property is
     * fixed for the lifetime of the process, and the reflection lookup is not cheap enough to run
     * on every notification refresh.
     */
    private val islandSupported: Boolean by lazy {
        runCatching {
            val clazz = Class.forName("android.os.SystemProperties")
            val method =
                clazz.getMethod(
                    "getBoolean",
                    String::class.java,
                    Boolean::class.javaPrimitiveType,
                )
            method.invoke(null, "persist.sys.feature.island", false) as Boolean
        }.getOrDefault(false)
    }

    fun isIslandSupported(): Boolean = islandSupported

    private var privilegedService: IPrivilegedService? = null
    private var serviceConnected = false
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

    @Volatile private var suiInitialized = false

    fun init(context: Context) {
        // Sui only needs to be initialized once per process (NexioSchedule does this from
        // Application/Activity onCreate). Live Shizuku state is re-queried by isRunning() /
        // hasPermission(), so a Shizuku service started later is still picked up on resume.
        if (suiInitialized) return
        runCatching {
            Sui.init(context.packageName)
            suiInitialized = true
            Log.d(TAG, "Shizuku initialized, running=${Shizuku.pingBinder()}")
        }.onFailure { error -> Log.w(TAG, "Shizuku init failed", error) }
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
        requestPermissionFromBinder(callback)
    }

    fun openShizuku(context: Context): Boolean =
        runCatching {
            val intent =
                context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
                    ?: return@runCatching false
            context.startActivity(intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }.getOrDefault(false)

    private fun requestPermissionFromBinder(callback: (Boolean) -> Unit) {
        if (!isRunning()) {
            callback(false)
            return
        }
        if (runCatching {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            }.getOrDefault(false)
        ) {
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

    suspend fun <T> withXmsfNetworkingDisabled(
        context: Context,
        block: () -> T,
    ): T {
        if (!isAvailable()) return block()

        return bypassMutex.withLock {
            val disabled =
                runCatching {
                    setXmsfNetworkingEnabled(context, enabled = false)
                }.getOrDefault(false)
            if (!disabled) return@withLock block()

            try {
                block().also { delay(100L) }
            } finally {
                runCatching { setXmsfNetworkingEnabled(context, enabled = true) }
                    .onFailure { error -> Log.e(TAG, "Failed to restore XMSF networking", error) }
            }
        }
    }

    private fun setXmsfNetworkingEnabled(
        context: Context,
        enabled: Boolean,
    ): Boolean {
        val xmsfUid = context.packageManager.getPackageUid(XMSF_PACKAGE, 0)
        getPrivilegedService(context)?.let { service ->
            return service.setPackageNetworkingEnabled(xmsfUid, enabled)
        }

        return runCatching {
            val connectivityBinder =
                SystemServiceHelper.getSystemService("connectivity")
                    ?: return@runCatching false
            val wrappedBinder = ShizukuBinderWrapper(connectivityBinder)
            val connectivityManager =
                Class
                    .forName("android.net.IConnectivityManager\$Stub")
                    .getMethod("asInterface", IBinder::class.java)
                    .invoke(null, wrappedBinder)
            val setChainEnabled =
                connectivityManager.javaClass.getMethod(
                    "setFirewallChainEnabled",
                    Int::class.javaPrimitiveType,
                    Boolean::class.javaPrimitiveType,
                )
            setChainEnabled.invoke(connectivityManager, FIREWALL_CHAIN_OEM_DENY, true)
            val setUidRule =
                connectivityManager.javaClass.getMethod(
                    "setUidFirewallRule",
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                )
            setUidRule.invoke(
                connectivityManager,
                FIREWALL_CHAIN_OEM_DENY,
                xmsfUid,
                if (enabled) 0 else 2,
            )
            true
        }.getOrDefault(false)
    }

    private fun getPrivilegedService(context: Context): IPrivilegedService? {
        if (privilegedService != null && serviceConnected) return privilegedService

        return runCatching {
            bindLatch = CountDownLatch(1)
            val args =
                Shizuku
                    .UserServiceArgs(
                        ComponentName(context.packageName, userServiceClass),
                    ).daemon(false)
                    .processNameSuffix("privileged")
                    .debuggable(false)
                    .version(1)
            Shizuku.bindUserService(args, serviceConnection)
            bindLatch.await(3L, TimeUnit.SECONDS)
            privilegedService
        }.getOrNull()
    }
}
