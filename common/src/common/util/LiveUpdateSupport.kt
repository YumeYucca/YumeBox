/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License.
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

import android.app.NotificationManager
import android.content.Context
import android.os.Build

/** Android 16 promoted-notification capability; the system owns the user-facing toggle. */
object LiveUpdateSupport {
    fun isPromotionAllowed(context: Context): Boolean =
        isPromotionAllowed(context.getSystemService(NotificationManager::class.java))

    fun isPromotionAllowed(manager: NotificationManager?): Boolean {
        if (manager == null) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) return false
        return runCatching { manager.canPostPromotedNotifications() }.getOrDefault(false)
    }
}
