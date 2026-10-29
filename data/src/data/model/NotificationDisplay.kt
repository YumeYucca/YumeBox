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

package com.github.yumeyucca.yumebox.data.model

/** A value that can be placed in one line of the ongoing notification. */
enum class NotificationLineContent {
    ProfileName,
    Status,
    CurrentNode,
    LiveTraffic,
    UsedTraffic,
    SessionTraffic,
    TotalTraffic,
    RemainingTraffic,
    SubscriptionUsage,
    ExpiryDate,
    CustomText,
    Hidden,
}

/**
 * Text values shared by the service renderer and the in-app notification preview. Building the
 * values in each process keeps the selection logic identical without coupling the preview to the
 * runtime service implementation.
 */
data class NotificationLineValues(
    val profileName: String,
    val currentNode: String,
    val liveTraffic: String,
    val usedTraffic: String,
    val sessionTraffic: String,
    val totalTraffic: String,
    val remainingTraffic: String,
    val subscriptionUsage: String,
    val expiryDate: String,
    val status: String,
)

data class NotificationDisplaySettings(
    val firstLine: NotificationLineContent = NotificationLineContent.ProfileName,
    val secondLine: NotificationLineContent = NotificationLineContent.LiveTraffic,
    val firstLineText: String = "",
    val secondLineText: String = "",
)

fun NotificationLineContent.resolve(
    values: NotificationLineValues,
    customText: String = "",
): String =
    when (this) {
        NotificationLineContent.ProfileName -> values.profileName
        NotificationLineContent.Status -> values.status
        NotificationLineContent.CurrentNode -> values.currentNode
        NotificationLineContent.LiveTraffic -> values.liveTraffic
        NotificationLineContent.UsedTraffic -> values.usedTraffic
        NotificationLineContent.SessionTraffic -> values.sessionTraffic
        NotificationLineContent.TotalTraffic -> values.totalTraffic
        NotificationLineContent.RemainingTraffic -> values.remainingTraffic
        NotificationLineContent.SubscriptionUsage -> values.subscriptionUsage
        NotificationLineContent.ExpiryDate -> values.expiryDate
        NotificationLineContent.CustomText -> customText
        NotificationLineContent.Hidden -> ""
    }
