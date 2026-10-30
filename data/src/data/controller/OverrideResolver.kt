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

import com.github.yumeyucca.yumebox.core.model.OverrideSpec
import com.github.yumeyucca.yumebox.data.model.OverrideMetadata
import com.github.yumeyucca.yumebox.data.model.ProfileBinding
import com.github.yumeyucca.yumebox.data.store.OverrideConfigStore
import com.github.yumeyucca.yumebox.data.store.ProfileBindingProvider

class OverrideResolver(
    private val configStore: OverrideConfigStore,
    private val bindingProvider: ProfileBindingProvider,
) {
    suspend fun resolveIds(profileId: String): List<String> =
        resolveBindingIds(bindingProvider.getBinding(profileId))

    suspend fun resolveSpecs(overrideIds: List<String>): List<OverrideSpec> {
        // Plain loop rather than mapNotNull: resolving a spec reads the override's file path off
        // disk, and a collection lambda cannot suspend.
        val specs = mutableListOf<OverrideSpec>()
        for (overrideId in overrideIds) {
            val config = configStore.getById(overrideId) ?: continue
            val file = configStore.getConfigFilePath(overrideId) ?: continue
            specs += OverrideSpec(path = file.absolutePath, ext = config.contentType.extension)
        }
        return specs
    }

    suspend fun getOverrideUsageCount(overrideId: String): Int =
        bindingProvider.getOverrideUsageCount(overrideId)

    private fun resolveBindingIds(binding: ProfileBinding?): List<String> {
        if (binding == null) {
            return emptyList()
        }
        // Keep every bound id (except legacy/internal). Do not drop missing files here — callers
        // compare resolved specs vs ids so a missing override surfaces as a failed apply instead of
        // silently compiling a shorter chain.
        return binding.overrideIds
            .asSequence()
            .filterNot { overrideId ->
                OverrideMetadata.isLegacyPresetId(overrideId) ||
                        OverrideConfigStore.isInternalRuntimeConfig(overrideId)
            }
            .distinct()
            .toList()
    }
}
