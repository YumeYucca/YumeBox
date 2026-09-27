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

package com.github.yumeyucca.yumebox.substore.model

import com.github.yumeyucca.yumebox.core.locale.YumeTxt

enum class AutoCloseMode(val minutes: Int?) {
    ALWAYS_ON(null),
    MINUTES_5(5),
    MINUTES_10(10);

    fun getDisplayName(): String =
        when (this) {
            ALWAYS_ON -> YumeTxt.Feature.ServiceStatus.AutoCloseModeAlwaysOn
            MINUTES_5 -> YumeTxt.Feature.ServiceStatus.AutoCloseMode5Min
            MINUTES_10 -> YumeTxt.Feature.ServiceStatus.AutoCloseMode10Min
        }
}
