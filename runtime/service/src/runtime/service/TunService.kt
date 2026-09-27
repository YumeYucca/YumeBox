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

import android.content.Intent
import android.net.VpnService
import androidx.core.app.ServiceCompat
import com.github.yumeyucca.yumebox.runtime.api.appContextOrSelf
import com.github.yumeyucca.yumebox.runtime.api.initializeServiceGlobal
import com.github.yumeyucca.yumebox.runtime.service.notification.ServiceNotificationManager
import com.github.yumeyucca.yumebox.runtime.service.session.VpnSession
import com.github.yumeyucca.yumebox.runtime.service.util.cancelAndJoinBlocking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

/**
 * Android host of the VPN core. It only owns what Android requires of a VpnService (the
 * foreground notification, revoke and destroy callbacks); [RuntimeCoordinator] decides when the
 * [session] starts, reloads and stops.
 */
class TunService : VpnService(), CoroutineScope by CoroutineScope(Dispatchers.Default) {
    internal val session by lazy { VpnSession(this) }
    private val notificationManager by lazy {
        ServiceNotificationManager(this, ServiceNotificationManager.vpnConfig)
    }
    private var notificationJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        initializeServiceGlobal(appContextOrSelf)
        notificationManager.createChannel()
        startForeground(
            ServiceNotificationManager.vpnConfig.notificationId,
            notificationManager.createInitialNotification(),
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (notificationJob?.isActive != true) {
            notificationJob = notificationManager.startTrafficUpdate(this)
        }
        RuntimeCoordinator.onVpnServiceStarted(this)
        // A killed service is recreated with its command; the coordinator adopts it as a start.
        return START_REDELIVER_INTENT
    }

    /** Drops the notification and ends this instance; the session is already stopped. */
    internal fun finish() {
        notificationJob?.cancel()
        notificationJob = null
        notificationManager.release()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onRevoke() {
        RuntimeCoordinator.onVpnRevoked(this)
        super.onRevoke()
    }

    override fun onDestroy() {
        notificationJob?.cancel()
        notificationJob = null
        notificationManager.release()
        RuntimeCoordinator.onVpnServiceDestroyed(this)
        super.onDestroy()
        cancelAndJoinBlocking()
    }
}
