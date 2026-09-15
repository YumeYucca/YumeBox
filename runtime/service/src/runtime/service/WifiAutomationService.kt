/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 */

package com.github.yumeyucca.yumebox.runtime.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.github.yumeyucca.yumebox.data.model.RunMode
import com.github.yumeyucca.yumebox.data.model.WifiAutomationAction
import com.github.yumeyucca.yumebox.data.model.WifiAutomationFallbackAction
import com.github.yumeyucca.yumebox.data.store.MMKVProvider
import com.github.yumeyucca.yumebox.data.store.NetworkSettingsStore
import com.github.yumeyucca.yumebox.data.store.RemoteControllerStore
import com.github.yumeyucca.yumebox.runtime.api.appContextOrSelf
import com.github.yumeyucca.yumebox.runtime.service.profile.ProfileService
import com.github.yumeyucca.yumebox.runtime.service.session.RuntimeServiceLauncher
import com.github.yumeyucca.yumebox.runtime.service.session.WifiSsidObservation
import com.github.yumeyucca.yumebox.runtime.service.session.WifiSsidObserver
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.UUID

/**
 * User-started foreground service that keeps SSID monitoring alive when the VPN is stopped.
 * It intentionally has no boot receiver: background-location permission is not part of v1.
 */
class WifiAutomationService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val settings by lazy {
        NetworkSettingsStore(MMKVProvider().getMMKV(NETWORK_SETTINGS_STORE))
    }
    private val profileService by lazy { ProfileService(this) }
    private var observer: WifiSsidObserver? = null
    private var applyJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!settings.wifiAutomationEnabled.value || settings.runMode.value != RunMode.VpnService) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        if (observer == null) {
            observer = WifiSsidObserver(this, ::onSsidObservation).also(WifiSsidObserver::start)
        } else {
            observer?.refresh()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        applyJob?.cancel()
        observer?.stop()
        observer = null
        serviceScope.cancel()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun onSsidObservation(observation: WifiSsidObservation) {
        applyJob?.cancel()
        applyJob =
            serviceScope.launch {
                delay(RULE_STABILIZATION_MILLIS)
                applyRule(observation)
            }
    }

    private suspend fun applyRule(observation: WifiSsidObservation) {
        if (!settings.wifiAutomationEnabled.value || settings.runMode.value != RunMode.VpnService) return
        if (RemoteControllerStore.isActive()) return

        when (observation) {
            is WifiSsidObservation.Connected -> {
                val rule =
                    settings.wifiAutomationRules.value.firstOrNull { it.ssid == observation.ssid }
                if (rule == null) {
                    applyFallbackAction(
                        settings.wifiAutomationOtherWifiAction.value,
                        settings.wifiAutomationOtherWifiProfileUuid.value.ifBlank { null },
                    )
                } else {
                    applySsidAction(rule.action, rule.profileUuid)
                }
            }

            WifiSsidObservation.NoWifi ->
                applyFallbackAction(
                    settings.wifiAutomationNoWifiAction.value,
                    settings.wifiAutomationNoWifiProfileUuid.value.ifBlank { null },
                )
            // A revoked permission, disabled system location, redacted SSID, or concurrent Wi-Fi
            // connections must never be treated as a switch to mobile data.
            WifiSsidObservation.Unavailable -> Unit
        }
    }

    private suspend fun applySsidAction(action: WifiAutomationAction, profileUuid: String?) {
        when (action) {
            WifiAutomationAction.Start -> {
                switchProfileIfNeeded(profileUuid)
                startVpnIfPossible()
            }

            WifiAutomationAction.Stop -> stopVpn()
        }
    }

    private suspend fun switchProfileIfNeeded(profileUuid: String?) {
        if (profileUuid == null) return
        val uuid = runCatching { UUID.fromString(profileUuid) }.getOrNull() ?: return
        profileService.queryByUUID(uuid)?.takeUnless { it.active }?.let { profileService.setActive(it) }
    }

    private suspend fun applyFallbackAction(action: WifiAutomationFallbackAction, profileUuid: String?) {
        when (action) {
            WifiAutomationFallbackAction.Keep -> Unit
            WifiAutomationFallbackAction.Start -> {
                switchProfileIfNeeded(profileUuid)
                startVpnIfPossible()
            }

            WifiAutomationFallbackAction.Stop -> stopVpn()
        }
    }

    private fun startVpnIfPossible() {
        if (StatusProvider.isRuntimeActive(RunMode.VpnService)) return
        if (VpnService.prepare(this) != null) {
            Timber.i("Wi-Fi automation skipped start: VPN permission missing")
            return
        }
        runCatching {
            RuntimeServiceLauncher.start(
                this,
                RunMode.VpnService,
                RuntimeServiceLauncher.SOURCE_WIFI_AUTOMATION,
            )
        }
            .onFailure { error -> Timber.w(error, "Wi-Fi automation start failed") }
    }

    private fun stopVpn() {
        runCatching { RuntimeServiceLauncher.stop(this, RunMode.VpnService) }
            .onFailure { error -> Timber.w(error, "Wi-Fi automation stop failed") }
    }

    private fun startForegroundNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.wifi_automation_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                )
            )
        }
        val notification =
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_logo_service)
                .setContentTitle(getString(R.string.wifi_automation_notification_title))
                .setContentText(getString(R.string.wifi_automation_notification_text))
                .setOngoing(true)
                .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val NETWORK_SETTINGS_STORE = "network_settings"
        private const val CHANNEL_ID = "wifi_automation"
        private const val NOTIFICATION_ID = 1102
        private const val RULE_STABILIZATION_MILLIS = 1_500L

        fun start(context: Context) {
            val appContext = context.appContextOrSelf
            runCatching {
                appContext.startForegroundService(Intent(appContext, WifiAutomationService::class.java))
            }
                .onFailure { error -> Timber.w(error, "Start Wi-Fi automation service failed") }
        }

        fun stop(context: Context) {
            val appContext = context.appContextOrSelf
            appContext.stopService(Intent(appContext, WifiAutomationService::class.java))
        }

    }
}
