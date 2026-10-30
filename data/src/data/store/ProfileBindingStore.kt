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

@file:Suppress("IfThenToElvis")

package com.github.yumeyucca.yumebox.data.store

import android.content.Context
import com.github.yumeyucca.yumebox.data.model.MetadataIndex
import com.github.yumeyucca.yumebox.data.model.ProfileBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

class ProfileBindingStore(context: Context) : ProfileBindingProvider {
    private val metadataFile = File(context.filesDir, "overrides/metadata.yaml")

    private val bindingsStateFlow = MutableStateFlow<Map<String, ProfileBinding>>(emptyMap())

    init {
        bindingsStateFlow.value = loadBindings()
    }

    override suspend fun getBinding(profileId: String): ProfileBinding? =
        withContext(Dispatchers.IO) { loadBindings()[profileId] }

    override fun getBindingFlow(profileId: String): Flow<ProfileBinding?> =
        bindingsStateFlow.asStateFlow().map { bindings -> bindings[profileId] }

    override suspend fun setBinding(binding: ProfileBinding) =
        withContext(Dispatchers.IO) {
            synchronized(OverrideMetadataFileLock.monitor) {
                val index = loadMetadataIndexForMutation()
                val next =
                    index.copy(profileChains = index.profileChains + (binding.profileId to binding))
                        .withSanitizedBindings()
                saveMetadataIndex(next)
                bindingsStateFlow.value = next.profileChains
            }
        }

    override suspend fun removeBinding(profileId: String) =
        withContext(Dispatchers.IO) {
            synchronized(OverrideMetadataFileLock.monitor) {
                val index = loadMetadataIndexForMutation()
                val bindings = index.profileChains - profileId
                saveMetadataIndex(index.copy(profileChains = bindings))
                bindingsStateFlow.value = bindings
            }
        }

    override suspend fun getAllBindings(): List<ProfileBinding> =
        withContext(Dispatchers.IO) { loadBindings().values.toList() }

    override fun getAllBindingsFlow(): Flow<List<ProfileBinding>> =
        bindingsStateFlow.asStateFlow().map { bindings -> bindings.values.toList() }

    override suspend fun getProfilesUsingOverride(overrideId: String): List<String> =
        withContext(Dispatchers.IO) {
            loadBindings()
                .values
                .filter { binding -> isOverrideApplied(binding, overrideId) }
                .map { it.profileId }
        }

    override suspend fun isOverrideInUse(overrideId: String): Boolean =
        withContext(Dispatchers.IO) {
            loadBindings().values.any { binding -> isOverrideApplied(binding, overrideId) }
        }

    override suspend fun getOverrideUsageCount(overrideId: String): Int =
        withContext(Dispatchers.IO) {
            loadBindings().values.count { binding -> isOverrideApplied(binding, overrideId) }
        }

    /** Single locked RMW so concurrent editors of the same profileId cannot clobber each other. */
    override suspend fun addOverride(profileId: String, overrideId: String, index: Int?) =
        withContext(Dispatchers.IO) {
            synchronized(OverrideMetadataFileLock.monitor) {
                mutateProfileBinding(profileId) { existing ->
                    existing?.addOverride(overrideId, index)
                        ?: ProfileBinding.withOverride(profileId, overrideId)
                }
            }
        }

    override suspend fun removeOverride(profileId: String, overrideId: String) =
        withContext(Dispatchers.IO) {
            synchronized(OverrideMetadataFileLock.monitor) {
                mutateProfileBinding(profileId) { existing ->
                    existing?.removeOverride(overrideId)
                }
            }
        }

    override suspend fun removeOverrideFromAllBindings(overrideId: String) =
        withContext(Dispatchers.IO) {
            synchronized(OverrideMetadataFileLock.monitor) {
                val currentIndex = loadMetadataIndexForMutation()
                val updatedIndex = currentIndex.removeOverrideFromProfileChains(overrideId)
                if (updatedIndex != currentIndex) {
                    saveMetadataIndex(updatedIndex)
                }
                bindingsStateFlow.value = updatedIndex.profileChains
            }
        }

    override suspend fun clearOverrides(profileId: String) =
        withContext(Dispatchers.IO) {
            synchronized(OverrideMetadataFileLock.monitor) {
                mutateProfileBinding(profileId) { existing -> existing?.clearOverrides() }
            }
        }

    /** Caller must hold [OverrideMetadataFileLock.monitor]. */
    private fun mutateProfileBinding(
        profileId: String,
        transform: (ProfileBinding?) -> ProfileBinding?,
    ) {
        val index = loadMetadataIndexForMutation()
        val existing = index.profileChains[profileId]
        val transformed = transform(existing) ?: return
        val next =
            index.copy(profileChains = index.profileChains + (profileId to transformed))
                .withSanitizedBindings()
        saveMetadataIndex(next)
        bindingsStateFlow.value = next.profileChains
    }

    @Suppress("TooGenericExceptionCaught")
    private fun loadBindings(): Map<String, ProfileBinding> =
        try {
            // Read-only path: corrupt metadata degrades to empty without wiping the file.
            loadMetadataIndexForRead().profileChains
        } catch (
            error: Exception
        ) { // fault barrier: any metadata read/decode failure degrades to empty
            Timber.w(error, "Failed to load bindings from metadata.yaml, returning empty map")
            emptyMap()
        }

    private fun loadMetadataIndexForRead(): MetadataIndex =
        synchronized(OverrideMetadataFileLock.monitor) {
            when (val loaded = OverrideMetadataIO.load(metadataFile)) {
                is MetadataIndexLoad.Ok -> {
                    val sanitized = sanitizeMetadataIndex(loaded.index)
                    // Only persist sanitization when the file decoded cleanly.
                    if (sanitized != loaded.index) {
                        OverrideMetadataIO.save(metadataFile, sanitized)
                    }
                    sanitized
                }

                MetadataIndexLoad.Missing,
                is MetadataIndexLoad.Corrupt -> MetadataIndex()
            }
        }

    private fun loadMetadataIndexForMutation(): MetadataIndex =
        // Already under monitor from callers; re-entrant lock is fine.
        synchronized(OverrideMetadataFileLock.monitor) {
            val index = OverrideMetadataIO.loadForMutation(metadataFile)
            val sanitized = sanitizeMetadataIndex(index)
            if (sanitized != index) {
                OverrideMetadataIO.save(metadataFile, sanitized)
            }
            sanitized
        }

    private fun saveMetadataIndex(index: MetadataIndex) {
        synchronized(OverrideMetadataFileLock.monitor) {
            OverrideMetadataIO.save(metadataFile, index)
        }
    }

    private fun sanitizeMetadataIndex(index: MetadataIndex): MetadataIndex {
        val normalizedConfigs =
            index.configs.mapValues { (id, metadata) ->
                if (metadata.id.isBlank()) metadata.copy(id = id) else metadata
            }
        val normalizedIndex =
            index.copy(
                configs = normalizedConfigs,
                profileChains =
                    index.profileChains.mapValues { (profileId, binding) ->
                        if (binding.profileId.isBlank()) binding.copy(profileId = profileId)
                        else binding
                    },
            )
        return normalizedIndex.withSanitizedBindings()
    }

    private fun isOverrideApplied(binding: ProfileBinding, overrideId: String): Boolean =
        binding.overrideIds.contains(overrideId)
}
