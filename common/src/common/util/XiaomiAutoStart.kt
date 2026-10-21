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

package com.github.yumeyucca.yumebox.common.util

import android.app.Activity
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings

/**
 * The MIUI/HyperOS autostart switch.
 *
 * Xiaomi keeps this switch in private AppOps (10008/10053) instead of the Android permission
 * model and exposes no public getter or setter. An app that is not whitelisted there is dropped
 * from the boot dispatch ("process is not permitted to auto start"), so the only universal fix is
 * to take the user to the system screen that owns the switch.
 *
 * State reads are best effort. When the ROM hides the value, [State.UNKNOWN] is returned instead
 * of claiming the switch is off.
 */
object XiaomiAutoStart {
    enum class State {
        ENABLED,
        DISABLED,
        UNKNOWN,
    }

    private const val MIUI_OP_AUTO_START = 10008
    private const val MIUI_OP_AUTO_START_NAME = "auto_start"
    private const val SECURITY_CENTER_PACKAGE = "com.miui.securitycenter"
    private const val AUTO_START_ACTIVITY =
        "com.miui.permcenter.autostart.AutoStartManagementActivity"
    private const val OP_AUTO_START_ACTION = "miui.intent.action.OP_AUTO_START"
    private const val EXTRA_PACKAGE_NAME = "extra_pkgname"

    private val xiaomi: Boolean by lazy {
        val names = setOf("xiaomi", "redmi", "poco")
        Build.MANUFACTURER.lowercase() in names ||
            Build.BRAND.lowercase() in names ||
            systemProperty("ro.miui.ui.version.name").isNotEmpty() ||
            systemProperty("ro.mi.os.version.name").isNotEmpty()
    }

    fun isSupported(): Boolean = xiaomi

    fun readState(context: Context): State {
        if (!xiaomi) return State.UNKNOWN
        readMiuiState(context)?.let { return it }
        return readAppOpsState(context)
    }

    /**
     * Opens the system autostart screen. Falls back to the app details page when the ROM has
     * removed the MIUI components. Returns false only when every candidate failed to launch.
     */
    fun openSettings(context: Context): Boolean {
        if (!xiaomi) return false
        val candidates =
            listOf(
                Intent(OP_AUTO_START_ACTION).apply {
                    setPackage(SECURITY_CENTER_PACKAGE)
                    addCategory(Intent.CATEGORY_DEFAULT)
                    putExtra(EXTRA_PACKAGE_NAME, context.packageName)
                },
                Intent().setComponent(
                    ComponentName(SECURITY_CENTER_PACKAGE, AUTO_START_ACTIVITY)
                ),
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                },
            )
        return candidates.any { context.startActivitySafely(it) }
    }

    private fun readMiuiState(context: Context): State? =
        runCatching {
            val method =
                Class.forName("android.miui.AppOpsUtils")
                    .getDeclaredMethod(
                        "getApplicationAutoStart",
                        Context::class.java,
                        String::class.java,
                    )
            method.isAccessible = true
            when (method.invoke(null, context, context.packageName) as? Int) {
                0 -> State.ENABLED
                1 -> State.DISABLED
                else -> null
            }
        }.getOrNull()

    // Fallback for ROMs that hide the MIUI helper class: op 10008 is the autostart entry.
    private fun readAppOpsState(context: Context): State =
        runCatching {
            val appOps =
                context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
                    ?: return State.UNKNOWN
            when (checkAutoStartOp(appOps, context.packageName)) {
                AppOpsManager.MODE_ALLOWED -> State.ENABLED
                AppOpsManager.MODE_IGNORED -> State.DISABLED
                else -> State.UNKNOWN
            }
        }.getOrDefault(State.UNKNOWN)

    /**
     * The int overload of [AppOpsManager.checkOpNoThrow] is hidden from the public SDK, so it is
     * called reflectively first. The string overload is the public fallback for MIUI builds that
     * register the op under its name instead of the number.
     */
    @Suppress("TooGenericExceptionCaught")
    private fun checkAutoStartOp(appOps: AppOpsManager, packageName: String): Int {
        val byNumber =
            runCatching {
                AppOpsManager::class.java
                    .getMethod(
                        "checkOpNoThrow",
                        Int::class.javaPrimitiveType!!,
                        Int::class.javaPrimitiveType!!,
                        String::class.java,
                    )
                    .invoke(appOps, MIUI_OP_AUTO_START, Process.myUid(), packageName) as Int
            }.getOrNull()
        if (byNumber != null) return byNumber
        return runCatching {
            appOps.checkOpNoThrow(MIUI_OP_AUTO_START_NAME, Process.myUid(), packageName)
        }.getOrDefault(AppOpsManager.MODE_ERRORED)
    }

    private fun Context.startActivitySafely(intent: Intent): Boolean =
        runCatching {
            if (this !is Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }.isSuccess

    private fun systemProperty(key: String): String =
        runCatching {
            Class.forName("android.os.SystemProperties")
                .getMethod("get", String::class.java)
                .invoke(null, key) as? String
        }.getOrNull().orEmpty()
}
