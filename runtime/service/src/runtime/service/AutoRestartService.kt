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

package com.github.yumeyucca.yumebox.runtime.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.github.yumeyucca.yumebox.core.util.AutoStartSessionGate
import com.github.yumeyucca.yumebox.core.util.StartupTaskCoordinator
import com.github.yumeyucca.yumebox.data.model.RunMode
import com.github.yumeyucca.yumebox.data.store.*
import com.github.yumeyucca.yumebox.runtime.api.Profile
import com.github.yumeyucca.yumebox.runtime.api.RuntimeOwner
import com.github.yumeyucca.yumebox.runtime.api.RuntimePhase
import com.github.yumeyucca.yumebox.runtime.api.RuntimeStartSource
import com.github.yumeyucca.yumebox.runtime.service.log.RuntimeLog
import com.github.yumeyucca.yumebox.runtime.service.profile.ProfileService
import com.github.yumeyucca.yumebox.runtime.service.util.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

class AutoRestartService : Service() {
    companion object {
        private const val TAG = "AutoRestartService"
        private const val NOTIFICATION_ID = 1101
        private const val CHANNEL_ID = "auto_restart_channel"
        const val EXTRA_REASON = "auto_restart_reason"
        const val REASON_BOOT_COMPLETED = "boot_completed"
        const val REASON_PACKAGE_REPLACED = "package_replaced"
        private const val ACTIVATION_TIMEOUT_MS = 60_000L
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mmkvProvider by lazy { MMKVProvider() }
    private val appSettingsStorage by lazy { AppSettingsStore(mmkvProvider.getMMKV("settings")) }
    private val featureStore by lazy { FeatureStore(mmkvProvider.getMMKV("substore")) }
    private val networkSettingsStorage by lazy {
        NetworkSettingsStore(mmkvProvider.getMMKV("network_settings"))
    }
    private val serviceCache by lazy { mmkvProvider.getMMKV("service_cache") }
    private val profileManager by lazy { ProfileService(applicationContext) }
    private val foregroundStarted = AtomicBoolean(false)
    private var autoStartJob: Job? = null

    // Duplicate triggers (boot + replaced racing) are merged into one job; the finally block
    // must stop with the latest delivered startId or the service lingers in foreground forever.
    @Volatile private var lastStartId = -1

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureForegroundStarted()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureForegroundStarted()
        lastStartId = startId
        if (autoStartJob?.isActive == true) {
            return START_NOT_STICKY
        }
        AutoStartExecutionGate.markStarted(serviceCache)

        val reason = intent?.getStringExtra(EXTRA_REASON).orEmpty().ifBlank { "unknown" }
        autoStartJob = serviceScope.launch {
            try {
                runCatching { checkAndAutoStart(reason) }
                    .onFailure { error ->
                        Timber.tag(TAG).e(error, "Auto start failed: ${error.message}")
                    }
            } finally {
                AutoStartExecutionGate.clear(serviceCache)
                ServiceCompat.stopForeground(
                    this@AutoRestartService,
                    ServiceCompat.STOP_FOREGROUND_REMOVE,
                )
                stopSelf(lastStartId)
            }
        }

        return START_NOT_STICKY
    }

    private fun ensureForegroundStarted() {
        if (!foregroundStarted.compareAndSet(false, true)) return

        createNotificationChannel()
        val notification = createNotification()
        val foregroundFlags =
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE

                else -> 0
            }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, foregroundFlags)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private suspend fun checkAndAutoStart(reason: String) {
        // An APK replacement kills app-owned services but not the detached root daemon. Tear it
        // down before checking the restart preference so no process keeps running replaced code.
        // verify() also reattaches a surviving root daemon before anything below reads state.
        RuntimeCoordinator.verify()
        if (reason == REASON_PACKAGE_REPLACED &&
            RuntimeCoordinator.state.value.owner == RuntimeOwner.RootDaemon
        ) {
            RuntimeCoordinator.stop()
        }
        if (!appSettingsStorage.automaticRestart.value) return
        if (RemoteControllerStore.isActive()) {
            Timber.tag(TAG).i("Skip auto start: remote controller mode active")
            return
        }
        if (AutoStartSessionGate.shouldSkipAutoStart()) {
            Timber.tag(TAG).i("Skip auto start: manual pause gate is active in current session")
            return
        }
        StartupTaskCoordinator.awaitWarmup()
        val skipUpdateOnPostUpdateColdStart = featureStore.consumePostUpdateColdStartPending()

        val activeProfile = profileManager.queryActive()
        if (activeProfile == null) {
            Timber.tag(TAG).w("No active profile for auto start")
            return
        }

        tryUpdateActiveProfileOnStart(
            activeProfile = activeProfile,
            reason = reason,
            skipForPostUpdateColdStart = skipUpdateOnPostUpdateColdStart,
        )

        val runMode = networkSettingsStorage.runMode.value
        // A restart against an already-active runtime would tear down the live core.
        if (RuntimeCoordinator.state.value.active) {
            Timber.tag(TAG).i("Skip auto start: ${runMode.name} runtime already active")
            return
        }
        if (runMode == RunMode.VpnService && VpnService.prepare(this) != null) {
            Timber.tag(TAG).i("Skip auto start: VPN permission is missing")
            return
        }

        val source =
            when (reason) {
                REASON_BOOT_COMPLETED -> RuntimeStartSource.AutoRestartBoot
                REASON_PACKAGE_REPLACED -> RuntimeStartSource.AutoRestartReplaced
                else -> RuntimeStartSource.AutoRestart
            }
        val log = RuntimeLog.writer(this, runMode)
        try {
            withTimeout(ACTIVATION_TIMEOUT_MS) { RuntimeCoordinator.start(runMode, source) }
        } catch (error: TimeoutCancellationException) {
            RuntimeCoordinator.stop()
            log.e(RuntimeLog.Type.AutoStart, "timeout reason=$reason mode=$runMode")
            throw IllegalStateException("runtime activation timed out", error)
        } catch (error: CancellationException) {
            throw error
        } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
            log.e(RuntimeLog.Type.AutoStart, "failed reason=$reason error=${error.message}")
            throw error
        }
        if (RuntimeCoordinator.state.value.phase == RuntimePhase.Running) {
            log.i(RuntimeLog.Type.AutoStart, "success: running reason=$reason mode=$runMode")
            Timber.tag(TAG)
                .i("Auto start active: reason=$reason profile=${activeProfile.name}, mode=$runMode")
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun tryUpdateActiveProfileOnStart(
        activeProfile: Profile,
        reason: String,
        skipForPostUpdateColdStart: Boolean,
    ) {
        when (
            AutoStartUpdatePolicy.decide(
                autoUpdateEnabled = appSettingsStorage.autoUpdateCurrentProfileOnStart.value,
                activeProfile = activeProfile,
                skipForPostUpdateColdStart = skipForPostUpdateColdStart,
                startupReason = reason,
                coldStartReasons = setOf(REASON_BOOT_COMPLETED, REASON_PACKAGE_REPLACED),
            )
        ) {
            AutoStartUpdatePolicy.Decision.Proceed -> Unit
            AutoStartUpdatePolicy.Decision.AutoUpdateDisabled -> return
            AutoStartUpdatePolicy.Decision.SkipPostUpdateColdStart -> {
                Timber.tag(TAG).d("Skip auto update: post-update cold-start marker consumed")
                return
            }

            AutoStartUpdatePolicy.Decision.SkipColdStartReason -> {
                Timber.tag(TAG).d("Skip auto update on cold-start reason=$reason")
                return
            }

            AutoStartUpdatePolicy.Decision.UnsupportedProfileType -> {
                Timber.tag(TAG)
                    .d("Skip boot update: unsupported profile type=${activeProfile.type}")
                return
            }

            AutoStartUpdatePolicy.Decision.NoActiveProfile -> return
        }

        try {
            profileManager.update(activeProfile.uuid, null)
            Timber.tag(TAG).i("Boot update ok: ${activeProfile.uuid}")
        } catch (error: Exception) {
            // fault barrier: best-effort boot update goes through the core fetch bridge; any
            // failure must not block the auto restart itself.
            Timber.tag(TAG).w(error, "Boot update failed")
        }
    }

    private fun createNotificationChannel() {
        val channel =
            NotificationChannel(
                    CHANNEL_ID,
                    "Auto Restart Service",
                    NotificationManager.IMPORTANCE_LOW,
                )
                .apply {
                    description = "Used to restart proxy service automatically"
                    setShowBadge(false)
                }

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("YumeBox")
            .setContentText("Checking auto-start...")
            .setSmallIcon(ServiceLogoIcons.resId())
            .setOngoing(true)
            .build()

    override fun onDestroy() {
        AutoStartExecutionGate.clear(serviceCache)
        serviceScope.cancel()
        super.onDestroy()
    }
}
