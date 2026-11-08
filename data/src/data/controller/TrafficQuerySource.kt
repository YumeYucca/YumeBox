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

package com.github.yumeyucca.yumebox.data.controller

import com.github.yumeyucca.yumebox.core.model.ConnectionSnapshot
import kotlinx.coroutines.flow.Flow

/**
 * The runtime queries the traffic collector needs, expressed as query source so the collector takes a single dependency
 * instead of four lambdas wired from the same facade.
 */
interface TrafficQuerySource {
    val isRunning: Flow<Boolean>

    fun currentProfileId(): String?

    suspend fun queryConnections(): ConnectionSnapshot

    suspend fun queryActiveProfileId(): String?
}
