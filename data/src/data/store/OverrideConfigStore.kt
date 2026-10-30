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

@file:Suppress("UnusedSymbol")

package com.github.yumeyucca.yumebox.data.store


import android.content.Context
import com.github.yumeyucca.yumebox.core.model.OverrideInternalConstants
import com.github.yumeyucca.yumebox.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

class OverrideConfigStore(
    private val context: Context,
    private val bindingProvider: ProfileBindingProvider,
) : OverrideConfigProvider {
    companion object {
        const val INTERNAL_RUNTIME_PREFIX = "__runtime__"

        fun isInternalRuntimeConfig(id: String): Boolean = id.startsWith(INTERNAL_RUNTIME_PREFIX)
    }

    private val overridesDir = File(context.filesDir, "overrides")
    private val configsDir = File(overridesDir, "configs")
    private val metadataFile = File(overridesDir, "metadata.yaml")
    private val builtInFiles = BuiltInOverrideFileStore(context, overridesDir)

    private val configsFlow = MutableStateFlow<List<OverrideConfig>>(emptyList())
    private val configsHydrated = java.util.concurrent.atomic.AtomicBoolean(false)
    private val hydrationMutex = Mutex()

    private suspend fun refreshConfigsFlow() {
        configsFlow.value = getAll()
        configsHydrated.set(true)
    }

    private suspend fun ensureConfigsHydrated() {
        if (configsHydrated.get()) return
        hydrationMutex.withLock {
            if (!configsHydrated.get()) refreshConfigsFlow()
        }
    }

    private fun updateConfigsFlowSnapshot(
        metadataIndex: MetadataIndex,
        userConfigsById: Map<String, OverrideConfig>,
    ) {
        configsFlow.value =
            metadataIndex.sortedUserMetadata().mapNotNull { metadata ->
                userConfigsById[metadata.id]
            }
    }

    /** Applies [mutate] to the cached user configs, then re-publishes them with [metadataIndex]. */
    private fun updateConfigsSnapshot(
        metadataIndex: MetadataIndex,
        mutate: (MutableMap<String, OverrideConfig>) -> Unit = {},
    ) {
        val userConfigsById = configsFlow.value.associateBy(OverrideConfig::id).toMutableMap()
        mutate(userConfigsById)
        updateConfigsFlowSnapshot(metadataIndex, userConfigsById)
    }

    override suspend fun getAll(): List<OverrideConfig> =
        withContext(Dispatchers.IO) { loadUserConfigs() }

    override fun getAllFlow(): Flow<List<OverrideConfig>> =
        flow {
            ensureConfigsHydrated()
            emitAll(configsFlow)
        }

    override suspend fun getById(id: String): OverrideConfig? =
        withContext(Dispatchers.IO) {
            if (BuiltInOverrideCatalog.isBuiltIn(id)) {
                return@withContext loadBuiltInConfig(id)
            }
            val metadata = loadMetadataIndex().getById(id) ?: return@withContext null
            loadConfigContent(metadata)
        }

    override suspend fun getUserConfigs(): List<OverrideConfig> =
        withContext(Dispatchers.IO) {
            loadUserConfigs().filter(::isUserOwnedConfig)
        }

    /** Bundled templates, always listed above user imports. Materializes assets on first read. */
    suspend fun getBuiltInConfigs(): List<OverrideConfig> =
        withContext(Dispatchers.IO) {
            BuiltInOverrideCatalog.all.mapNotNull { def -> loadBuiltInConfig(def.id) }
        }

    override fun getUserConfigsFlow(): Flow<List<OverrideConfig>> = flow {
        emit(loadUserConfigs().filter(::isUserOwnedConfig))
    }
        .flowOn(Dispatchers.IO)

    override suspend fun save(config: OverrideConfig) =
        withContext(Dispatchers.IO) {
            if (BuiltInOverrideCatalog.isBuiltIn(config.id)) {
                // Built-ins are immutable APK templates. Customization must use duplicate().
                return@withContext
            }

            synchronized(OverrideMetadataFileLock.monitor) {
                val metadataIndex = loadMetadataIndexForMutation()
                val existingMetadata = metadataIndex.getById(config.id)
                configsDir.mkdirs()
                OverrideMetadataIO.writeTextAtomic(
                    resolveConfigFile(config.id, config.contentType),
                    config.content,
                )
                cleanupStaleConfigFiles(config.id, keepExtension = config.contentType.extension)

                val metadata =
                    config.toMetadata().copy(
                        sortOrder = existingMetadata?.sortOrder ?: metadataIndex.nextUserSortOrder(),
                    )
                val updatedIndex = metadataIndex.upsert(metadata)
                saveMetadataIndex(updatedIndex)

                updateConfigsSnapshot(updatedIndex) { it[config.id] = config }
            }
        }

    override suspend fun delete(id: String): Boolean =
        withContext(Dispatchers.IO) {
            // Built-ins cannot be deleted from the store (UI also hides the action).
            if (BuiltInOverrideCatalog.isBuiltIn(id)) {
                return@withContext false
            }
            val updatedIndex =
                synchronized(OverrideMetadataFileLock.monitor) {
                    val currentIndex = loadMetadataIndexForMutation()
                    if (currentIndex.getById(id) == null) {
                        return@synchronized null
                    }

                    val updated = currentIndex.remove(id).removeOverrideFromProfileChains(id)
                    saveMetadataIndex(updated)
                    cleanupStaleConfigFiles(id)
                    updated
                }
            if (updatedIndex == null) {
                refreshConfigsFlow()
                return@withContext false
            }
            // The persistent snapshot is already clean; this synchronizes the binding StateFlow.
            bindingProvider.removeOverrideFromAllBindings(id)
            updateConfigsSnapshot(updatedIndex) { it.remove(id) }
            true
        }

    override suspend fun duplicate(id: String): OverrideConfig? =
        withContext(Dispatchers.IO) {
            val original = getById(id) ?: return@withContext null
            val newMetadata = original.toMetadata().duplicateAsUser()
            val duplicated =
                original.copy(
                    id = newMetadata.id,
                    name = newMetadata.name,
                    createdAt = newMetadata.createdAt,
                    updatedAt = newMetadata.updatedAt,
                )
            save(duplicated)
            duplicated
        }

    override suspend fun exists(id: String): Boolean =
        withContext(Dispatchers.IO) {
            if (BuiltInOverrideCatalog.isBuiltIn(id)) {
                return@withContext loadBuiltInConfig(id) != null
            }
            loadMetadataIndex().getById(id)?.let(::findConfigFile) != null
        }

    suspend fun loadCustomRoutingContent(): String? =
        withContext(Dispatchers.IO) {
            val file =
                resolveConfigFilePath(OverrideInternalConstants.CUSTOM_ROUTING_OVERRIDE_ID)
                    ?: resolveConfigFile(
                        OverrideInternalConstants.CUSTOM_ROUTING_OVERRIDE_ID,
                        OverrideContentType.Yaml,
                    )
            if (!file.exists()) return@withContext null
            file.readText().takeIf(String::isNotBlank)
        }

    suspend fun saveCustomRoutingContent(content: String) =
        withContext(Dispatchers.IO) {
            if (content.isBlank()) {
                delete(OverrideInternalConstants.CUSTOM_ROUTING_OVERRIDE_ID)
                return@withContext
            }

            val existing = getById(OverrideInternalConstants.CUSTOM_ROUTING_OVERRIDE_ID)
            save(
                OverrideConfig(
                    id = OverrideInternalConstants.CUSTOM_ROUTING_OVERRIDE_ID,
                    name = OverrideInternalConstants.CUSTOM_ROUTING_FILE_NAME,
                    description = null,
                    contentType = OverrideContentType.Yaml,
                    content = content,
                    createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                )
            )
        }

    /** Reads the stored content; this touches disk, so the UI path must not call it inline. */
    suspend fun getConfigContent(id: String): String? =
        withContext(Dispatchers.IO) { readConfigContent(id) }

    private fun readConfigContent(id: String): String? {
        if (BuiltInOverrideCatalog.isBuiltIn(id)) {
            return materializeBuiltInFile(id)?.readTextOrNull()
        }
        val metadata = loadMetadataIndex().getById(id) ?: return null
        val file = findConfigFile(metadata) ?: return null
        return file.readTextOrNull()
    }

    suspend fun saveConfigContent(id: String, content: String): Boolean =
        withContext(Dispatchers.IO) { writeConfigContent(id, content) }

    private fun writeConfigContent(id: String, content: String): Boolean {
        return !BuiltInOverrideCatalog.isBuiltIn(id) && synchronized(OverrideMetadataFileLock.monitor) {
            val index = loadMetadataIndexForMutation()
            val metadata = index.getById(id) ?: return@synchronized false
            runCatching {
                val file = findConfigFile(metadata) ?: resolveConfigFile(id, metadata.contentType)
                file.parentFile?.mkdirs()
                OverrideMetadataIO.writeTextAtomic(file, content)

                val updatedMetadata = metadata.copy(updatedAt = System.currentTimeMillis())
                val updatedIndex = index.upsert(updatedMetadata)
                saveMetadataIndex(updatedIndex)
                updateConfigsSnapshot(updatedIndex) { snapshot ->
                    loadConfigContent(updatedMetadata)?.let { snapshot[id] = it }
                }
                true
            }
                .onFailure { Timber.w(it, "Failed to save override content: %s", id) }
                .isSuccess
        }
    }

    suspend fun getConfigFilePath(id: String): File? =
        withContext(Dispatchers.IO) { resolveConfigFilePath(id) }

    private fun resolveConfigFilePath(id: String): File? {
        if (BuiltInOverrideCatalog.isBuiltIn(id)) {
            return materializeBuiltInFile(id)
        }
        val metadata = loadMetadataIndex().getById(id) ?: return null
        return findConfigFile(metadata)
    }

    suspend fun reorderUserConfigs(orderedIds: List<String>) =
        withContext(Dispatchers.IO) {
            synchronized(OverrideMetadataFileLock.monitor) {
                if (orderedIds.isEmpty()) return@synchronized

                val metadataIndex = loadMetadataIndexForMutation()
                val sortedUserMetadata = metadataIndex.sortedUserMetadata()
                if (sortedUserMetadata.isEmpty()) return@synchronized

                val userMetadataById = sortedUserMetadata.associateBy(OverrideMetadata::id)
                val reorderedIds = orderedIds.filter(userMetadataById::containsKey)
                if (reorderedIds.isEmpty()) return@synchronized

                val remainingIds =
                    sortedUserMetadata.map(OverrideMetadata::id).filterNot(reorderedIds::contains)
                val finalOrder = reorderedIds + remainingIds
                val updatedConfigs = metadataIndex.configs.toMutableMap()
                var hasChanges = false

                finalOrder.forEachIndexed { index, id ->
                    val metadata = userMetadataById[id] ?: return@forEachIndexed
                    val newSortOrder = index.toLong() + 1L
                    if (metadata.sortOrder != newSortOrder) {
                        updatedConfigs[id] = metadata.copy(sortOrder = newSortOrder)
                        hasChanges = true
                    }
                }

                if (hasChanges) {
                    val updatedIndex = metadataIndex.copy(configs = updatedConfigs)
                    saveMetadataIndex(updatedIndex)
                    updateConfigsSnapshot(updatedIndex)
                }
            }
        }

    private fun File.readTextOrNull(): String? = runCatching { readText() }
        .onFailure { Timber.w(it, "Failed to read override file: %s", absolutePath) }
        .getOrNull()

    private fun loadUserConfigs(): List<OverrideConfig> {
        // Never short-circuit on a missing configsDir — imported overrides live in metadata and
        // may still resolve after the directory is recreated (e.g. first built-in materialize).
        if (!configsDir.exists()) {
            configsDir.mkdirs()
        }
        return loadMetadataIndex().sortedUserMetadata().mapNotNull { metadata ->
            if (
                isInternalRuntimeConfig(metadata.id) ||
                BuiltInOverrideCatalog.isBuiltIn(metadata.id)
            ) {
                return@mapNotNull null
            }
            loadConfigContent(metadata)
        }
    }

    private fun isUserOwnedConfig(config: OverrideConfig): Boolean =
        config.id != OverrideInternalConstants.CUSTOM_ROUTING_OVERRIDE_ID &&
                !BuiltInOverrideCatalog.isBuiltIn(config.id) &&
                !isInternalRuntimeConfig(config.id)

    private fun loadBuiltInConfig(id: String): OverrideConfig? {
        val def = BuiltInOverrideCatalog.find(id) ?: return null
        // The materialized file is synchronized with the packaged asset on every resolution.
        val file = materializeBuiltInFile(def.id)
        val content =
            file?.readTextOrNull()
                ?: readBuiltInAsset(def)
                ?: return null
        val now = System.currentTimeMillis()
        return OverrideConfig(
            id = def.id,
            name = def.name,
            description = def.contentType.displayName,
            contentType = def.contentType,
            content = content,
            createdAt = now,
            updatedAt = file?.lastModified()?.takeIf { it > 0L } ?: now,
        )
    }

    private fun readBuiltInAsset(def: BuiltInOverrideDefinition): String? = runCatching {
        context.assets.open(def.assetPath).bufferedReader().use { it.readText() }
    }
        .onFailure { error ->
            Timber.w(error, "Failed to read built-in override asset: %s", def.assetPath)
        }
        .getOrNull()

    private fun materializeBuiltInFile(id: String): File? {
        return builtInFiles.sync(id)
    }

    private fun loadConfigContent(metadata: OverrideMetadata): OverrideConfig? {
        val file = findConfigFile(metadata) ?: return null
        val content = file.readTextOrNull() ?: return null
        return OverrideConfig(
            id = metadata.id,
            name = metadata.name,
            description = metadata.description,
            contentType = metadata.contentType,
            content = content,
            createdAt = metadata.createdAt,
            updatedAt = metadata.updatedAt,
        )
    }

    private fun loadMetadataIndex(): MetadataIndex =
        synchronized(OverrideMetadataFileLock.monitor) {
            when (val loaded = OverrideMetadataIO.load(metadataFile)) {
                // Never normalize-write an empty shell over a non-empty corrupt file.
                MetadataIndexLoad.Missing,
                is MetadataIndexLoad.Corrupt,
                -> MetadataIndex()

                is MetadataIndexLoad.Ok -> normalizeAndPersist(loaded.index)
            }
        }

    /** Mutation entry: refuses to proceed when on-disk metadata is corrupt. */
    private fun loadMetadataIndexForMutation(): MetadataIndex =
        synchronized(OverrideMetadataFileLock.monitor) {
            normalizeAndPersist(OverrideMetadataIO.loadForMutation(metadataFile))
        }

    /** Drops legacy/built-in leftovers and renumbers user order, persisting only when it changed. */
    private fun normalizeAndPersist(index: MetadataIndex): MetadataIndex {
        val normalized = sanitizeMetadataIndex(index).normalizeUserSortOrders()
        if (normalized != index) {
            OverrideMetadataIO.save(metadataFile, normalized)
        }
        return normalized
    }

    private fun saveMetadataIndex(index: MetadataIndex) {
        synchronized(OverrideMetadataFileLock.monitor) {
            overridesDir.mkdirs()
            OverrideMetadataIO.save(metadataFile, index)
        }
    }

    private fun resolveConfigFile(id: String, contentType: OverrideContentType): File =
        configsDir.resolve("$id.${contentType.extension}")

    private fun findConfigFile(metadata: OverrideMetadata): File? {
        val expectedFile = resolveConfigFile(metadata.id, metadata.contentType)
        if (expectedFile.exists()) return expectedFile

        return OverrideContentType.knownExtensions
            .asSequence()
            .map { extension -> configsDir.resolve("${metadata.id}.$extension") }
            .firstOrNull(File::exists)
    }

    private fun cleanupStaleConfigFiles(id: String, keepExtension: String? = null) {
        OverrideContentType.knownExtensions.forEach { extension ->
            if (keepExtension != null && extension == keepExtension) {
                return@forEach
            }
            runCatching { configsDir.resolve("$id.$extension").delete() }
        }
    }

    private fun sanitizeMetadataIndex(index: MetadataIndex): MetadataIndex {
        val sanitizedConfigs =
            index.configs.filterValues { metadata ->
                !OverrideMetadata.isLegacyPresetId(metadata.id) &&
                        !BuiltInOverrideCatalog.isBuiltIn(metadata.id)
            }
        return index.copy(configs = sanitizedConfigs).withSanitizedBindings()
    }

    private fun OverrideConfig.toMetadata(): OverrideMetadata =
        OverrideMetadata(
            id = id,
            name = name,
            description = description,
            contentType = contentType,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}
