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
 * Copyright (c) YumeYucca 2025 - Present
 */

package com.github.yumeyucca.yumebox.data.update

import kotlinx.serialization.Serializable

@Serializable
data class AppUpdateManifest(
    val schemaVersion: Int = 1,
    val channel: String = "",
    val versionName: String = "",
    val versionCode: Int = 0,
    val commitSha: String = "",
    val publishedAt: String = "",
    val notes: String = "",
    val releaseUrl: String = "",
    val packages: List<AppUpdatePackage> = emptyList(),
)

@Serializable
data class AppUpdatePackage(
    val id: String = "",
    val abi: String = "",
    val geoBundled: Boolean = false,
    val fileName: String = "",
    val downloadUrl: String = "",
    val size: Long = 0,
    val sha256: String = "",
)
