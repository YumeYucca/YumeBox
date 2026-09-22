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
import android.app.Service
import android.graphics.drawable.Icon
import android.os.Bundle

/**
 * Xiaomi HyperOS Super Island support.
 *
 * The island entry is not a notification API: HyperOS picks it up from the extras of the ongoing
 * notification. [applyExtras] attaches the artwork bundle (`miui.focus.pics`) and the island
 * payload (`miui.focus.param`, built by [MiuiIslandParams]) to an already built notification.
 */
object HyperOsIsland {
    /** HyperOS marks devices with the island renderer through this system property. */
    private const val FEATURE_PROPERTY = "persist.sys.feature.island"

    private const val EXTRA_PICS = "miui.focus.pics"
    private const val EXTRA_PARAM = "miui.focus.param"
    private const val PIC_APP_ICON = "miui.focus.pic_app_icon"
    private const val PIC_APP_ICON_DARK = "miui.focus.pic_app_icon_dark"
    private const val PIC_SMALL = "miui.focus.pic_small"
    private const val PIC_SMALL_DARK = "miui.focus.pic_small_dark"

    /**
     * Read once: the system property is fixed for the lifetime of the process, and the reflection
     * lookup is not cheap enough to run on every notification refresh.
     */
    private val supported: Boolean by lazy {
        runCatching {
            Class.forName("android.os.SystemProperties")
                .getMethod("getBoolean", String::class.java, Boolean::class.javaPrimitiveType)
                .invoke(null, FEATURE_PROPERTY, false) as Boolean
        }.getOrDefault(false)
    }

    fun isSupported(): Boolean = supported

    internal fun applyExtras(
        service: Service,
        notification: Notification,
        presentation: NotificationPresentation.Running,
        iconRes: Int,
    ) {
        val icon = Icon.createWithResource(service, iconRes)
        notification.extras.putBundle(
            EXTRA_PICS,
            Bundle().apply {
                putParcelable(PIC_APP_ICON, icon)
                putParcelable(PIC_APP_ICON_DARK, icon)
                putParcelable(PIC_SMALL, icon)
                putParcelable(PIC_SMALL_DARK, icon)
            },
        )
        notification.extras.putString(
            EXTRA_PARAM,
            MiuiIslandParams.build(
                profileName = presentation.title,
                usageText = presentation.content,
                compactText = presentation.compactTraffic,
                currentNode = presentation.currentNode,
            ),
        )
    }
}
