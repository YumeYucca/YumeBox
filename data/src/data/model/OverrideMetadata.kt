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


import kotlinx.serialization.Serializable

@Serializable
data class OverrideMetadata(
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    val contentType: OverrideContentType = OverrideContentType.Yaml,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val sortOrder: Long = 0L,
) {
    companion object {
        const val ID_PREFIX = "cfg-"
        const val LEGACY_SYSTEM_PREFIX = "preset-"

        /** Whether [id] is a leftover from the old bundled-preset scheme. */
        fun isLegacyPresetId(id: String): Boolean = id.startsWith(LEGACY_SYSTEM_PREFIX)

        fun generateId(): String =
            "$ID_PREFIX${System.currentTimeMillis()}-${(1000..9999).random()}"
    }

    fun duplicateAsUser(): OverrideMetadata {
        val now = System.currentTimeMillis()
        return OverrideMetadata(
            id = generateId(),
            name = "$name (副本)",
            description = description,
            contentType = contentType,
            createdAt = now,
            updatedAt = now,
            sortOrder = 0L,
        )
    }
}

@Serializable
data class MetadataIndex(
    val configs: Map<String, OverrideMetadata> = emptyMap(),
    val profileChains: Map<String, ProfileBinding> = emptyMap(),
) {
    fun getById(id: String): OverrideMetadata? = configs[id]

    fun upsert(metadata: OverrideMetadata): MetadataIndex =
        copy(configs = configs + (metadata.id to metadata))

    fun remove(id: String): MetadataIndex = copy(configs = configs - id)

    fun removeOverrideFromProfileChains(overrideId: String): MetadataIndex =
        copy(
            profileChains =
                profileChains.mapValues { (_, binding) -> binding.removeOverride(overrideId) }
        )

    /**
     * Drops override references [against] cannot satisfy: legacy preset leftovers and ids whose
     * config is gone. A stale id must never survive a metadata write, and both metadata owners
     * (configs and profile bindings) must agree on that filter.
     */
    fun withSanitizedBindings(
        against: Map<String, OverrideMetadata> = configs,
    ): MetadataIndex {
        val sanitized =
            profileChains.mapValues { (_, binding) ->
                binding.copy(
                    overrideIds =
                        binding.overrideIds.filterNot { overrideId ->
                            OverrideMetadata.isLegacyPresetId(overrideId) ||
                                    (overrideId.startsWith(OverrideMetadata.ID_PREFIX) &&
                                            overrideId !in against)
                        }
                )
            }
        return if (sanitized == profileChains) this else copy(profileChains = sanitized)
    }

    fun sortedUserMetadata(): List<OverrideMetadata> =
        configs.values.sortedWith(
            compareBy<OverrideMetadata> { if (it.sortOrder > 0L) 0 else 1 }
                .thenBy { if (it.sortOrder > 0L) it.sortOrder else Long.MAX_VALUE }
                .thenByDescending(OverrideMetadata::updatedAt)
                .thenBy(OverrideMetadata::createdAt)
        )

    fun nextUserSortOrder(): Long =
        sortedUserMetadata().maxOfOrNull(OverrideMetadata::sortOrder)?.plus(1L) ?: 1L

    fun normalizeUserSortOrders(): MetadataIndex {
        val updatedConfigs = configs.toMutableMap()
        var hasChanges = false

        sortedUserMetadata().forEachIndexed { index, metadata ->
            val normalizedOrder = index.toLong() + 1L
            if (metadata.sortOrder != normalizedOrder) {
                updatedConfigs[metadata.id] = metadata.copy(sortOrder = normalizedOrder)
                hasChanges = true
            }
        }

        return if (hasChanges) {
            copy(configs = updatedConfigs)
        } else {
            this
        }
    }
}
