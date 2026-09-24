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

package com.github.yumeyucca.yumebox.runtime.service.notification

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.SystemClock
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.github.yumeyucca.yumebox.core.util.PollingTimerSpecs
import com.github.yumeyucca.yumebox.core.util.PollingTimers
import com.github.yumeyucca.yumebox.data.store.AppSettingsStore
import com.github.yumeyucca.yumebox.data.store.RemoteControllerStore
import com.github.yumeyucca.yumebox.domain.model.resolvePrimaryNode
import com.github.yumeyucca.yumebox.domain.model.toInfo
import com.github.yumeyucca.yumebox.runtime.api.Components
import com.github.yumeyucca.yumebox.runtime.api.CoreApi
import com.github.yumeyucca.yumebox.runtime.service.R
import com.github.yumeyucca.yumebox.runtime.service.config.ServiceStore
import com.github.yumeyucca.yumebox.runtime.service.profile.Imported
import com.github.yumeyucca.yumebox.runtime.service.profile.ImportedDao
import com.github.yumeyucca.yumebox.runtime.service.shizuku.ShizukuManager
import com.github.yumeyucca.yumebox.runtime.service.util.ServiceLogoIcons
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tf.gal.yumebox.locale.YumeTxt

class ServiceNotificationManager(
    private val service: Service,
    private val config: Config,
) {
    data class Config(
        val notificationId: Int,
        val channelId: String,
        val channelName: String,
    )

    private val serviceStore by lazy { ServiceStore() }
    private val settingsStore by lazy { MMKV.mmkvWithID("settings", MMKV.MULTI_PROCESS_MODE) }
    private val appSettings by lazy { AppSettingsStore(settingsStore) }
    private val notificationManager by lazy { NotificationManagerCompat.from(service) }
    // Once released, the traffic updater must never notify() again — otherwise a tick that was mid
    // queryTrafficNow() (IPC to the core) when the service stopped can re-post the ongoing
    // notification AFTER stopForeground(REMOVE), leaving it stuck on screen.
    @Volatile private var released = false

    // Cached so the island payload does not trigger a core IPC on every traffic tick.
    private var currentNode: String? = null
    private var currentNodeUpdatedAt = 0L

    private var islandShown = false
    private var bypassAcquired = false
    private var lastFrame: NoticeFrame? = null

    fun createChannel() {
        legacyChannelIds.forEach(notificationManager::deleteNotificationChannel)
        notificationManager.createNotificationChannel(
            NotificationChannelCompat.Builder(
                    config.channelId,
                    NotificationManagerCompat.IMPORTANCE_LOW,
                )
                .setName(config.channelName)
                .build()
        )
    }

    // The initial notification backs startForeground() inside onCreate: any throw before that
    // call crashes the app with a foreground-service contract violation, so this path must
    // stay free of MMKV/DAO reads. The enriched content follows via the traffic updater.
    fun createInitialNotification(): Notification =
        buildNotification(
            NotificationPresentationFactory.createStatus(
                profileName = service.applicationInfo.loadLabel(service.packageManager).toString(),
                status = YumeTxt.Service.Notification.Running,
            )
        )

    fun startTrafficUpdate(scope: CoroutineScope): Job =
        scope.launch(Dispatchers.IO) {
            try {
                PollingTimers.ticks(PollingTimerSpecs.ServiceTrafficNotification).collect {
                    refreshRunningNotification()
                }
            } finally {
                // Cancellation must not skip the restore. A cancelled withLock returns immediately.
                withContext(NonCancellable) { releaseBypass() }
            }
        }

    /**
     * Stop updating and clear the notification. After this the traffic updater never notifies
     * again. The caller cancels the traffic job; that job releases the XMSF bypass.
     */
    fun release() {
        released = true
        islandShown = false
        lastFrame = null
        runCatching { notificationManager.cancel(config.notificationId) }
    }

    private sealed interface NoticeFrame {
        data class Plain(val presentation: NotificationPresentation) : NoticeFrame

        data class Island(val running: NotificationPresentation.Running) : NoticeFrame
    }

    private fun loadFrame(): NoticeFrame {
        // Reading the profile deserializes the whole stored list, so resolve it only once.
        val profile = resolveProfile()
        val profileName =
            profile?.name?.takeIf { it.isNotBlank() }
                ?: YumeTxt.Service.Notification.UnknownProfile
        if (!shouldShowTrafficNotification()) {
            return NoticeFrame.Plain(
                NotificationPresentationFactory.createStatus(
                    profileName = profileName,
                    status = YumeTxt.Service.Notification.Running,
                )
            )
        }
        val running = loadRunning(profile, profileName)
        return if (isIslandEnabled()) NoticeFrame.Island(running) else NoticeFrame.Plain(running)
    }

    private fun loadRunning(
        profile: Imported?,
        profileName: String,
    ): NotificationPresentation.Running {
        val core = com.github.yumeyucca.yumebox.runtime.service.core.CoreProcess.controller(service)
        val now = runCatching { core.queryTrafficNow() }.getOrDefault(0L)
        return NotificationPresentationFactory.createRunning(
            profileName = profileName,
            profile = profile,
            currentNode = resolveCurrentNode(core),
            trafficNow = now,
        )
    }

    private suspend fun refreshRunningNotification() {
        // This notification belongs to a running foreground service. Re-post it through
        // startForeground() instead of NotificationManager.notify(): Android may defer ordinary
        // notify() updates after the app leaves the foreground, while startForeground() remains
        // the service-owned update path even when POST_NOTIFICATIONS is denied.
        if (released) return
        val frame = loadFrame()
        if (released) return
        when (frame) {
            is NoticeFrame.Island -> postIsland(frame)
            is NoticeFrame.Plain -> postPlain(frame)
        }
    }

    private suspend fun postIsland(frame: NoticeFrame.Island) {
        val held = acquireBypass()
        if (released) return
        // Expand once, and only after the bypass is actually held. A failed acquire still posts
        // the text once; the next tick retries the bypass without replaying the expand.
        val promote = held && !islandShown
        if (!promote && frame == lastFrame) return
        deliver(islandNotification(frame.running, promote))
        if (promote) delay(ISLAND_SETTLE_DELAY_MS)
        if (released) return
        if (held) islandShown = true
        lastFrame = frame
    }

    private suspend fun postPlain(frame: NoticeFrame.Plain) {
        if (releaseBypass()) islandShown = false
        if (frame == lastFrame) return
        deliver(buildNotification(frame.presentation))
        lastFrame = frame
    }

    private suspend fun acquireBypass(): Boolean {
        if (bypassAcquired) return true
        if (!ShizukuManager.acquireXmsfBypass(service)) return false
        bypassAcquired = true
        return true
    }

    private suspend fun releaseBypass(): Boolean {
        if (!bypassAcquired) return true
        if (!ShizukuManager.releaseXmsfBypass(service)) return false
        bypassAcquired = false
        return true
    }

    private fun islandNotification(
        running: NotificationPresentation.Running,
        promote: Boolean,
    ): Notification {
        val notification = buildNotification(running)
        HyperOsIsland.applyExtras(service, notification, running, smallIconRes(), promote)
        return notification
    }

    private fun deliver(notification: Notification) {
        // The service may have stopped during the (possibly slow) core query; a startForeground()
        // now would resurrect the notification.
        if (!released) {
            service.startForeground(config.notificationId, notification)
        }
    }

    private fun buildNotification(presentation: NotificationPresentation): Notification {
        val contentIntent =
            PendingIntent.getActivity(
                service,
                0,
                Intent().apply {
                    component = Components.PROXY_SHEET_ACTIVITY
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_NO_ANIMATION
                    )
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        return NotificationCompat.Builder(service, config.channelId)
            .setContentTitle(presentation.title)
            .setContentText(presentation.content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(presentation.expandedText))
            .setSmallIcon(smallIconRes())
            .setColor(service.getColor(R.color.color_yumebox))
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    // startForeground() must never trip before the first frame, so a preference read failure falls
    // back to the default logo.
    private fun smallIconRes(): Int =
        runCatching { ServiceLogoIcons.resId() }.getOrDefault(R.drawable.ic_logo_service)

    private fun resolveProfile(): Imported? =
        serviceStore.activeProfile?.let { ImportedDao.queryByUUID(it) }

    private fun resolveCurrentNode(core: CoreApi): String? {
        val now = SystemClock.elapsedRealtime()
        if (now - currentNodeUpdatedAt < CURRENT_NODE_REFRESH_MS) {
            return currentNode
        }
        currentNodeUpdatedAt = now
        runCatching { core.queryAllProxyGroups(false) }
            .onSuccess { groups ->
                currentNode = groups.map { it.toInfo() }.resolvePrimaryNode()?.name
            }
        return currentNode
    }

    private fun shouldShowTrafficNotification(): Boolean {
        val settings = settingsStore
        if (settings.containsKey("showTrafficNotification")) {
            return settings.decodeBool("showTrafficNotification", true)
        }
        return serviceStore.showTrafficNotification
    }

    private fun isSuperIslandEnabled(): Boolean = appSettings.superIslandEnabled.value

    private fun isIslandEnabled(): Boolean =
        !RemoteControllerStore.isActive() &&
            isSuperIslandEnabled() &&
            HyperOsIsland.isSupported()

    companion object {
        /**
         * Kept past the first island `startForeground()`. HyperOS decides whether to keep the island
         * while that notification is delivered, and the XMSF bypass is already held.
         */
        private const val ISLAND_SETTLE_DELAY_MS = 100L

        // Channel ids shipped before the YumeBox rebrand; deleted on channel creation so
        // upgraded installs don't keep orphaned "Clash ..." entries in notification settings.
        private val legacyChannelIds = listOf("clash_vpn_service", "clash_http_service")

        // Resolving the node costs a core IPC listing all proxy groups.
        private const val CURRENT_NODE_REFRESH_MS = 2500L

        val vpnConfig =
            Config(
                notificationId = 1001,
                channelId = "yumebox_vpn_service",
                channelName = "YumeBox VPN Service",
            )

        val rootConfig =
            Config(
                notificationId = 1002,
                channelId = "yumebox_root_service",
                channelName = "YumeBox Root Service",
            )
    }
}
