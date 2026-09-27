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

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.github.yumeyucca.yumebox.runtime.api.Components
import com.github.yumeyucca.yumebox.runtime.service.util.ServiceLogoIcons
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import timber.log.Timber

class DialerReceiver : BroadcastReceiver() {
    companion object {
        private const val NOTIFICATION_ID = 1102
        private const val CHANNEL_ID = "secret_code"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.provider.Telephony.SECRET_CODE") return
        // Background activity starts are blocked since Android 10, so the secret code surfaces
        // a notification whose tap opens the app instead of launching it directly.
        postOpenNotification(context)
    }

    private fun postOpenNotification(context: Context) {
        runCatching {
            val manager = NotificationManagerCompat.from(context)
            if (!manager.areNotificationsEnabled()) return

            manager.createNotificationChannel(
                NotificationChannelCompat.Builder(
                        CHANNEL_ID,
                        NotificationManagerCompat.IMPORTANCE_HIGH,
                    )
                    .setName(YumeTxt.Service.Tile.ClickToOpen)
                    .build()
            )

            val launchIntent =
                Intent(Intent.ACTION_MAIN).apply {
                    component = Components.MAIN_ACTIVITY
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
            val pendingIntent =
                PendingIntent.getActivity(
                    context,
                    0,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

            val notification =
                NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(ServiceLogoIcons.resId())
                    .setContentTitle(YumeTxt.Service.Tile.ClickToOpen)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build()
            if (
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED
            ) {
                manager.notify(NOTIFICATION_ID, notification)
            }
        }
            .onFailure { error -> Timber.e(error, "Secret code notification failed") }
    }
}
