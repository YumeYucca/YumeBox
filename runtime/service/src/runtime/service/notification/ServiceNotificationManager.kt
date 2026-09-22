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
import android.graphics.drawable.Icon
import android.os.Bundle
import android.os.SystemClock
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.github.yumeyucca.yumebox.core.model.ProxyGroup
import com.github.yumeyucca.yumebox.core.util.PollingTimerSpecs
import com.github.yumeyucca.yumebox.core.util.PollingTimers
import com.github.yumeyucca.yumebox.domain.model.ProxyGroupInfo
import com.github.yumeyucca.yumebox.domain.model.resolveTerminalProxy
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
import kotlinx.coroutines.launch
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
    private val notificationManager by lazy { NotificationManagerCompat.from(service) }
    // Once released, the traffic updater must never notify() again — otherwise a tick that was mid
    // queryTrafficNow() (IPC to the core) when the service stopped can re-post the ongoing
    // notification AFTER stopForeground(REMOVE), leaving it stuck on screen.
    @Volatile private var released = false

    // Terminal node of the selected group, cached so the island payload does not trigger a core
    // IPC on every traffic tick.
    private var currentNode: String? = null
    private var currentNodeUpdatedAt = 0L

    fun createChannel() {
        // The island extras are only rendered while Shizuku is available, so bind/initialize it
        // together with the notification channel of the foreground service.
        ShizukuManager.init(service)
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
            PollingTimers.ticks(PollingTimerSpecs.ServiceTrafficNotification).collect {
                refreshRunningNotification()
            }
        }

    /**
     * Stop updating and clear the notification. After this the traffic updater never notifies
     * again.
     */
    fun release() {
        released = true
        runCatching { notificationManager.cancel(config.notificationId) }
    }

    private fun buildRunningNotification(): Notification {
        val profileName = resolveProfileName()
        if (!shouldShowTrafficNotification()) {
            return buildNotification(
                NotificationPresentationFactory.createStatus(
                    profileName = profileName,
                    status = YumeTxt.Service.Notification.Running,
                )
            )
        }

        val core = com.github.yumeyucca.yumebox.runtime.service.core.CoreProcess.controller(service)
        val now = runCatching { core.queryTrafficNow() }.getOrDefault(0L)
        return buildNotification(
            NotificationPresentationFactory.createRunning(
                profileName = profileName,
                profile = resolveProfile(),
                currentNode = resolveCurrentNode(core),
                trafficNow = now,
            )
        )
    }

    private suspend fun refreshRunningNotification() {
        // This notification belongs to a running foreground service. Re-post it through
        // startForeground() instead of NotificationManager.notify(): Android may defer ordinary
        // notify() updates after the app leaves the foreground, while startForeground() remains
        // the service-owned update path even when POST_NOTIFICATIONS is denied.
        if (released) {
            return
        }
        val notification = buildRunningNotification()
        if (islandActive()) {
            // HyperOS only keeps the island entry of a notification app whose network access is
            // blocked; the XMSF bypass is restored right after the update.
            ShizukuManager.withXmsfNetworkingDisabled(service) {
                // Re-check after the (possibly slow) core query: the service may have stopped
                // while we were building the notification, and a startForeground() now would
                // resurrect it.
                if (!released) {
                    service.startForeground(config.notificationId, notification)
                }
            }
            return
        }
        // Re-check after the (possibly slow) core query: the service may have stopped while we
        // were building the notification, and a notify() now would resurrect it.
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

        // Keep the small icon free of hard failures so startForeground() never trips before the
        // first frame; preference reads fall back to the default logo inside ServiceLogoIcons.
        val smallIcon = runCatching { ServiceLogoIcons.resId() }.getOrDefault(R.drawable.ic_logo_service)

        return NotificationCompat.Builder(service, config.channelId)
            .setContentTitle(presentation.title)
            .setContentText(presentation.content)
            .setSubText(presentation.subText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(presentation.expandedText)
                    .setSummaryText(presentation.subText)
            )
            .setSmallIcon(smallIcon)
            .setColor(service.getColor(R.color.color_yumebox))
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
            .also { addIslandExtras(it, presentation) }
    }

    /**
     * HyperOS picks up the island entry from the notification extras: the icon bundle is used for
     * the island artwork, [MiuiIslandParams] carries the text and the tap intent.
     */
    private fun addIslandExtras(
        notification: Notification,
        presentation: NotificationPresentation,
    ) {
        if (!presentation.isRunning || !islandActive()) {
            return
        }
        val icon = runCatching { ServiceLogoIcons.resId() }.getOrDefault(R.drawable.ic_logo_service)
        val pics =
            Bundle().apply {
                putParcelable("miui.focus.pic_app_icon", Icon.createWithResource(service, icon))
                putParcelable("miui.focus.pic_app_icon_dark", Icon.createWithResource(service, icon))
                putParcelable("miui.focus.pic_small", Icon.createWithResource(service, icon))
                putParcelable("miui.focus.pic_small_dark", Icon.createWithResource(service, icon))
            }
        notification.extras.putBundle("miui.focus.pics", pics)
        notification.extras.putString(
            "miui.focus.param",
            MiuiIslandParams.build(
                profileName = presentation.title,
                usageText = presentation.content,
                compactText = presentation.compactText,
                currentNode = presentation.currentNode,
            ),
        )
    }

    private fun resolveProfile(): Imported? =
        serviceStore.activeProfile?.let { ImportedDao.queryByUUID(it) }

    private fun resolveProfileName(): String =
        resolveProfile()?.name?.takeIf { it.isNotBlank() }
            ?: YumeTxt.Service.Notification.UnknownProfile

    private fun resolveCurrentNode(core: CoreApi): String? {
        val now = SystemClock.elapsedRealtime()
        if (now - currentNodeUpdatedAt < CURRENT_NODE_REFRESH_MS) {
            return currentNode
        }
        currentNodeUpdatedAt = now
        runCatching { core.queryAllProxyGroups(false) }.onSuccess { groups ->
            currentNode = resolveTerminalNode(groups)
        }
        return currentNode
    }

    /**
     * Resolves the proxy the selected group finally dials: the main group is named "Proxy" on a
     * stock config, any other config falls back to its first group.
     */
    private fun resolveTerminalNode(groups: List<ProxyGroup>): String? {
        val infos =
            groups.map { group ->
                ProxyGroupInfo(
                    name = group.name,
                    type = group.type,
                    proxies = group.proxies,
                    now = group.now.trim(),
                    icon = group.icon,
                    hidden = group.hidden,
                )
            }
        val group =
            infos.firstOrNull { it.name.equals("Proxy", ignoreCase = true) }
                ?: infos.firstOrNull()
                ?: return null
        val now = group.now.takeIf { it.isNotBlank() } ?: return null
        return infos.resolveTerminalProxy(now)?.name
    }

    private fun shouldShowTrafficNotification(): Boolean {
        val settings = settingsStore
        if (settings.containsKey("showTrafficNotification")) {
            return settings.decodeBool("showTrafficNotification", true)
        }
        return serviceStore.showTrafficNotification
    }

    /** Super Island toggle of the app side; written in the shared settings store. */
    private fun isSuperIslandEnabled(): Boolean =
        settingsStore.decodeBool("superIslandEnabled", true)

    private fun islandActive(): Boolean =
        isSuperIslandEnabled() &&
            ShizukuManager.isIslandSupported() &&
            shouldShowTrafficNotification()

    companion object {
        // Channel ids shipped before the YumeBox rebrand; deleted on channel creation so
        // upgraded installs don't keep orphaned "Clash ..." entries in notification settings.
        private val legacyChannelIds = listOf("clash_vpn_service", "clash_http_service")

        // The terminal node is only refreshed when the cached value is older than this: resolving
        // it costs a core IPC listing all proxy groups.
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
