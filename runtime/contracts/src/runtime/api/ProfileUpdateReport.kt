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

package com.github.yumeyucca.yumebox.runtime.api

/**
 * Best-effort summary of external proxy/rule provider prefetch during profile update.
 * Failures here must never fail the main subscription import.
 */
data class ProviderPrefetchReport(
    val attempted: Int = 0,
    val failedNames: List<String> = emptyList(),
    /** Config text declares providers + http urls, but collector found zero downloadable items. */
    val discoveryAnomaly: Boolean = false,
    /** Text-scan / incomplete header parse path; download may miss custom headers. */
    val headerDegraded: Boolean = false,
)

/** Result of a successful profile update (main config committed). */
data class ProfileUpdateReport(
    val providers: ProviderPrefetchReport = ProviderPrefetchReport(),
) {
    companion object {
        val Empty = ProfileUpdateReport()
    }
}