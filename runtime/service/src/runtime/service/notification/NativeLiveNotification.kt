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

import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.github.yumeyucca.yumebox.common.util.LiveUpdateSupport

/**
 * Android 16 live updates. The app only requests promotion; the system decides whether the
 * ongoing notification appears as a status-bar chip and an expanded lock-screen card.
 */
internal object NativeLiveNotification {
    private const val EXTRA_REQUEST_PROMOTED_ONGOING = "android.requestPromotedOngoing"

    fun apply(
        builder: NotificationCompat.Builder,
        manager: NotificationManager?,
        criticalText: String,
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) return false
        if (!LiveUpdateSupport.isPromotionAllowed(manager)) return false
        applyPromotion(builder, criticalText)
        return true
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun applyPromotion(
        builder: NotificationCompat.Builder,
        criticalText: String,
    ) {
        builder.setShortCriticalText(criticalText)
        // ColorOS also reads this extra when deciding whether to promote the card.
        builder.addExtras(Bundle().apply { putBoolean(EXTRA_REQUEST_PROMOTED_ONGOING, true) })
        builder.setRequestPromotedOngoing(true)
    }
}
