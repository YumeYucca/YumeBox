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

package com.github.yumeyucca.yumebox.core.model

import kotlinx.serialization.Serializable

/**
 * One entry from the mihomo `GET /rules` endpoint (runtime rule table). [disabled] is temporary and cleared when the
 * core restarts.
 */
@Serializable
data class RuntimeRule(
    val index: Int,
    val type: String,
    val payload: String = "",
    val proxy: String = "",
    val size: Int = -1,
    val disabled: Boolean = false,
    val hitCount: Long = 0L,
    val missCount: Long = 0L,
)
