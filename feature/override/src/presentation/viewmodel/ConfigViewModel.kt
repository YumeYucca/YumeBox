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

@file:Suppress("UnusedSymbol", "DestructuringDeclaration")

package com.github.yumeyucca.yumebox.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumeyucca.yumebox.core.model.OverrideInternalConstants
import com.github.yumeyucca.yumebox.data.controller.ActiveProfileOverrideReloader
import com.github.yumeyucca.yumebox.data.controller.OverrideResolver
import com.github.yumeyucca.yumebox.data.model.OverrideConfig
import com.github.yumeyucca.yumebox.data.model.OverrideContentType
import com.github.yumeyucca.yumebox.data.model.OverrideMetadata
import com.github.yumeyucca.yumebox.data.store.OverrideConfigStore
import com.github.yumeyucca.yumebox.data.store.ProfileBindingProvider
import com.github.yumeyucca.yumebox.runtime.api.Profile
import com.github.yumeyucca.yumebox.runtime.client.ProfilesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.util.*

class OverrideConfigViewModel(
    private val configRepo: OverrideConfigStore,
    private val resolver: OverrideResolver,
    private val bindingProvider: ProfileBindingProvider,
    private val activeProfileOverrideReloader: ActiveProfileOverrideReloader,
    private val profilesRepository: ProfilesRepository,
) : ViewModel() {
    companion object {
        private const val TAG = "OverrideConfigViewModel"
        private const val NETWORK_IMPORT_CONNECT_TIMEOUT_MS = 15_000
        private const val NETWORK_IMPORT_READ_TIMEOUT_MS = 30_000
        private const val NETWORK_IMPORT_MAX_BYTES = 5L * 1024 * 1024
    }

    private val _configs = MutableStateFlow<List<OverrideConfig>>(emptyList())
    val configs: StateFlow<List<OverrideConfig>> = _configs.asStateFlow()

    private val _builtInConfigs = MutableStateFlow<List<OverrideConfig>>(emptyList())
    val builtInConfigs: StateFlow<List<OverrideConfig>> = _builtInConfigs.asStateFlow()

    private val _userConfigs = MutableStateFlow<List<OverrideConfig>>(emptyList())
    val userConfigs: StateFlow<List<OverrideConfig>> = _userConfigs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _usageCountMap = MutableStateFlow<Map<String, Int>>(emptyMap())
    val usageCountMap: StateFlow<Map<String, Int>> = _usageCountMap.asStateFlow()

    private val _pendingRevealConfigId = MutableStateFlow<String?>(null)
    val pendingRevealConfigId: StateFlow<String?> = _pendingRevealConfigId.asStateFlow()
    private val refreshMutex = Mutex()

    /**
     * Snapshot of which subscriptions currently bind [overrideId]. Used by the apply sheet so
     * checkboxes can seed without racing the async profile list load.
     */
    data class OverrideApplySnapshot(
        val overrideId: String,
        val profiles: List<Profile>,
        val selectedProfileIds: Set<String>,
    )

    init {
        refresh()
        viewModelScope.launch {
            bindingProvider.getAllBindingsFlow().collectLatest { loadUsageCounts() }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    fun refresh() {
        viewModelScope.launch { refreshNow() }
    }

    suspend fun refreshAndAwait() {
        refreshNow()
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun refreshNow() {
        refreshMutex.withLock {
            _isLoading.value = true
            try {
                // Load independently: a built-in asset hiccup must not wipe imported overrides
                // (and vice versa). First open used to fail the whole try when materialize threw.
                val builtIns = runCatching {
                    configRepo.getBuiltInConfigs()
                }
                    .onFailure { error ->
                        Timber.tag(TAG).e(error, "Failed to load built-in overrides")
                    }
                    .getOrDefault(_builtInConfigs.value)
                val users = runCatching {
                    configRepo.getUserConfigs()
                }
                    .onFailure { error ->
                        Timber.tag(TAG).e(error, "Failed to load imported overrides")
                    }
                    .getOrDefault(_userConfigs.value)
                _builtInConfigs.value = builtIns
                _userConfigs.value = users
                _configs.value = builtIns + users
                loadUsageCounts()
            } catch (
                error:
                Exception
            ) { // fault barrier: top-level ViewModel load handler, log and reset
                // loading
                Timber.tag(TAG).e(error, "Failed to load overrides")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getConfigById(id: String): OverrideConfig? = _configs.value.find { it.id == id }

    fun getConfigContent(configId: String): String? = configRepo.getConfigContent(configId)

    fun isBuiltInConfig(id: String): Boolean = OverrideInternalConstants.isBuiltInOverrideId(id)

    fun saveConfigContent(configId: String, content: String): Boolean {
        val saved = configRepo.saveConfigContent(configId, content)
        if (!saved) return false

        viewModelScope.launch {
            activeProfileOverrideReloader.reapplyActiveProfileIfUsingOverride(configId)
            refreshNow()
        }
        return true
    }

    fun createConfig(name: String, description: String? = null, contentType: OverrideContentType) {
        viewModelScope.launch {
            runCatching {
                val now = System.currentTimeMillis()
                val config =
                    OverrideConfig(
                        id = OverrideMetadata.generateId(),
                        name = name,
                        description = description,
                        contentType = contentType,
                        content = "",
                        createdAt = now,
                        updatedAt = now,
                    )
                configRepo.save(config)
                _pendingRevealConfigId.value = config.id
                refreshNow()
            }
                .onFailure { error -> Timber.tag(TAG).e(error, "Failed to create override") }
        }
    }

    fun deleteConfig(id: String) {
        if (isBuiltInConfig(id)) return
        viewModelScope.launch {
            runCatching {
                val shouldResyncRuntime =
                    activeProfileOverrideReloader.isActiveProfileUsingOverride(id)
                val deleted = configRepo.delete(id)
                if (deleted && shouldResyncRuntime) {
                    activeProfileOverrideReloader.reapplyActiveProfileOverride()
                }
                refreshNow()
            }
                .onFailure { error -> Timber.tag(TAG).e(error, "Failed to delete override") }
        }
    }

    fun duplicateConfig(id: String) {
        viewModelScope.launch {
            runCatching {
                val source = getConfigById(id) ?: configRepo.getById(id) ?: return@runCatching
                // Built-ins have no metadata row — materialize a user copy instead of
                // store.duplicate.
                val duplicated =
                    if (isBuiltInConfig(id)) {
                        val now = System.currentTimeMillis()
                        OverrideConfig(
                            id = OverrideMetadata.generateId(),
                            name = YumeTxt.Override.BuiltIn.CopyName.format(source.name),
                            description = source.description,
                            contentType = source.contentType,
                            content = source.content,
                            createdAt = now,
                            updatedAt = now,
                        )
                            .also { configRepo.save(it) }
                    } else {
                        configRepo.duplicate(id)
                    }
                if (duplicated != null) {
                    _pendingRevealConfigId.value = duplicated.id
                }
                refreshNow()
            }
                .onFailure { error -> Timber.tag(TAG).e(error, "Failed to duplicate override") }
        }
    }

    fun reorderUserConfigs(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val currentConfigs = _userConfigs.value
            if (fromIndex !in currentConfigs.indices || fromIndex == toIndex) return@launch

            val reorderedConfigs =
                currentConfigs.toMutableList().also { configs ->
                    val moving = configs.removeAt(fromIndex)
                    configs.add(toIndex.coerceIn(0, configs.size), moving)
                }
            _userConfigs.value = reorderedConfigs
            _configs.value = _builtInConfigs.value + reorderedConfigs

            runCatching { configRepo.reorderUserConfigs(reorderedConfigs.map(OverrideConfig::id)) }
                .onFailure { error -> Timber.tag(TAG).e(error, "Failed to reorder overrides") }
            refreshNow()
        }
    }

    suspend fun importConfig(content: String, sourceName: String?): Result<OverrideConfig> {
        return importConfig(
            content = content,
            sourceName = sourceName,
            fallbackContentType = null,
        )
    }

    private suspend fun importConfig(
        content: String,
        sourceName: String?,
        fallbackContentType: OverrideContentType?,
    ): Result<OverrideConfig> {
        val contentType =
            OverrideContentType.fromFileName(sourceName)
                ?: fallbackContentType
                ?: return Result.failure(
                    IllegalArgumentException(
                        YumeTxt.Override.Import.Failed.format(
                            YumeTxt.Override.Import.UnsupportedType
                        )
                    )
                )
        if (contentType == OverrideContentType.JavaScript && content.isBlank()) {
            return Result.failure(
                IllegalArgumentException(
                    YumeTxt.Override.Import.Failed.format(YumeTxt.Override.Import.EmptyJavaScript)
                )
            )
        }

        val config =
            OverrideConfig(
                id = OverrideMetadata.generateId(),
                name =
                    normalizeImportedConfigSourceName(sourceName)
                        ?: YumeTxt.Override.Save.ImportDefaultName,
                description = null,
                contentType = contentType,
                content = content,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            )

        return runCatching {
            configRepo.save(config)
            _pendingRevealConfigId.value = config.id
            refreshNow()
            config
        }
            .onFailure { error -> Timber.tag(TAG).e(error, "Failed to import override") }
    }

    suspend fun importConfigFromUrl(rawUrl: String): Result<OverrideConfig> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = URL(rawUrl.trim())
                require(
                    url.protocol.equals("http", ignoreCase = true) ||
                            url.protocol.equals("https", ignoreCase = true)
                ) {
                    YumeTxt.Override.Import.InvalidUrl
                }

                val connection =
                    (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = NETWORK_IMPORT_CONNECT_TIMEOUT_MS
                        readTimeout = NETWORK_IMPORT_READ_TIMEOUT_MS
                        instanceFollowRedirects = true
                        requestMethod = "GET"
                    }

                try {
                    val responseCode = connection.responseCode
                    require(responseCode in 200..299) {
                        YumeTxt.Override.Import.HttpError.format(responseCode)
                    }

                    require(
                        connection.contentLengthLong < 0L ||
                                connection.contentLengthLong <= NETWORK_IMPORT_MAX_BYTES
                    ) {
                        "远程配置超过 ${NETWORK_IMPORT_MAX_BYTES / (1024 * 1024)}MB 限制"
                    }

                    val contentTypeHeader = connection.contentType.orEmpty()
                    val sourceName =
                        resolveNetworkImportSourceName(
                            url = url,
                            contentDisposition =
                                connection.getHeaderField("Content-Disposition"),
                            contentType = contentTypeHeader,
                        )
                    val content =
                        connection.inputStream.use { input ->
                            readUtf8TextLimited(input, NETWORK_IMPORT_MAX_BYTES)
                        }
                    val inferredContentType =
                        OverrideContentType.fromFileName(sourceName)
                            ?: inferContentTypeFromHeader(contentTypeHeader)
                            ?: inferContentTypeFromContent(content)
                    val normalizedSourceName =
                        ensureSourceNameExtension(sourceName, inferredContentType)
                    importConfig(
                        content = content,
                        sourceName = normalizedSourceName,
                        fallbackContentType = inferredContentType,
                    )
                        .getOrThrow()
                } finally {
                    connection.disconnect()
                }
            }
                .fold(
                    onSuccess = { Result.success(it) },
                    onFailure = { Result.failure(it) },
                )
        }

    /**
     * Load every subscription + which of them currently include [overrideId] in their chain.
     * Profiles without a binding are treated as unbound (unchecked).
     */
    suspend fun loadApplySnapshot(overrideId: String): Result<OverrideApplySnapshot> =
        withContext(Dispatchers.IO) {
            runCatching {
                val profiles = profilesRepository.queryAllProfiles()
                val selectedProfileIds = mutableSetOf<String>()
                for (profile in profiles) {
                    val profileId = profile.uuid.toString()
                    val isBound =
                        bindingProvider.getBinding(profileId)?.overrideIds?.contains(overrideId) ==
                                true
                    if (isBound) {
                        selectedProfileIds += profileId
                    }
                }
                OverrideApplySnapshot(
                    overrideId = overrideId,
                    profiles = profiles,
                    selectedProfileIds = selectedProfileIds,
                )
            }
                .onFailure { error ->
                    Timber.tag(TAG).e(error, "Failed to load apply snapshot for %s", overrideId)
                }
        }

    /**
     * Rewrite every subscription binding so [overrideId] is present exactly for
     * [selectedProfileIds]. Order of existing chain entries is preserved; newly added ids are
     * appended (chain order = application order).
     *
     * Persisting the bindings is the whole operation. The runtime reapply is fired afterwards on
     * [viewModelScope] and is deliberately NOT awaited: it needs the runtime service and a full
     * override-chain resolve, and letting that gate the apply left the confirm action stuck behind
     * work the user did not ask for. A reload that fails only means the running session keeps its
     * previous chain until the next start/reload — the bindings on disk are already correct.
     */
    suspend fun applyOverrideToProfiles(
        overrideId: String,
        selectedProfileIds: Set<String>,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                // Resolved from local metadata rather than the runtime profile list: unbinding only
                // ever concerns profiles that already reference this override, so the write path
                // never has to reach the service.
                val boundProfileIds = bindingProvider.getProfilesUsingOverride(overrideId).toSet()

                (selectedProfileIds - boundProfileIds).forEach { profileId ->
                    bindingProvider.addOverride(profileId, overrideId)
                }
                (boundProfileIds - selectedProfileIds).forEach { profileId ->
                    bindingProvider.removeOverride(profileId, overrideId)
                }

                val changedProfileIds =
                    (selectedProfileIds - boundProfileIds) + (boundProfileIds - selectedProfileIds)
                if (changedProfileIds.isNotEmpty()) {
                    reapplyActiveProfileInBackground(overrideId, changedProfileIds)
                }
                loadUsageCounts()
            }
                .onFailure { error ->
                    Timber.tag(TAG)
                        .e(error, "Failed to apply override %s to selected profiles", overrideId)
                }
        }

    /** Best-effort: never surfaced to the caller, never allowed to fail the binding write. */
    private fun reapplyActiveProfileInBackground(
        overrideId: String,
        changedProfileIds: Set<String>,
    ) {
        viewModelScope.launch {
            runCatching {
                // Resolving the active profile is itself a runtime call, so it happens here and
                // not on the write path. Only a change to the active profile's own chain is worth
                // a reload.
                val activeProfileId = profilesRepository.queryActiveProfile()?.uuid?.toString()
                if (activeProfileId in changedProfileIds) {
                    activeProfileOverrideReloader.reapplyActiveProfileOverride()
                }
            }
                .onFailure { error ->
                    Timber.tag(TAG)
                        .w(error, "Deferred runtime reload failed after applying %s", overrideId)
                }
        }
    }

    suspend fun isConfigInUse(id: String): Boolean = resolver.isOverrideInUse(id)

    fun consumePendingRevealConfig(configId: String) {
        if (_pendingRevealConfigId.value == configId) {
            _pendingRevealConfigId.value = null
        }
    }

    private suspend fun loadUsageCounts() {
        val countMap = mutableMapOf<String, Int>()
        _configs.value.forEach { config ->
            countMap[config.id] = resolver.getOverrideUsageCount(config.id)
        }
        _usageCountMap.value = countMap
    }
}

internal fun readUtf8TextLimited(input: InputStream, maxBytes: Long): String {
    require(maxBytes in 0..Int.MAX_VALUE.toLong()) { "maxBytes is out of range" }
    val output = ByteArrayOutputStream(minOf(DEFAULT_BUFFER_SIZE.toLong(), maxBytes).toInt())
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        total += read.toLong()
        require(total <= maxBytes) {
            "远程配置超过 ${maxBytes / (1024 * 1024)}MB 限制"
        }
        output.write(buffer, 0, read)
    }
    return output.toString(Charsets.UTF_8.name())
}

private fun resolveNetworkImportSourceName(
    url: URL,
    contentDisposition: String?,
    contentType: String,
): String {
    parseFilenameFromContentDisposition(contentDisposition)?.let {
        return it
    }

    val pathName =
        url.path
            .substringAfterLast('/')
            .takeIf(String::isNotBlank)
            ?.let { URLDecoder.decode(it, Charsets.UTF_8.name()) }
            ?.trim()
            .orEmpty()

    if (pathName.isNotBlank()) return pathName

    val extension =
        inferContentTypeFromHeader(contentType)?.extension ?: OverrideContentType.Yaml.extension
    return "${url.host.ifBlank { YumeTxt.Override.Save.ImportDefaultName }}.$extension"
}

private fun parseFilenameFromContentDisposition(contentDisposition: String?): String? {
    val value = contentDisposition?.takeIf(String::isNotBlank) ?: return null
    val encodedMatch =
        Regex("""filename\*=([^']*)'[^']*'([^;]+)""", RegexOption.IGNORE_CASE).find(value)
    if (encodedMatch != null) {
        val charset = encodedMatch.groupValues[1].ifBlank { Charsets.UTF_8.name() }
        val encodedName = encodedMatch.groupValues[2].trim().trim('"', '\'')
        return runCatching { URLDecoder.decode(encodedName, charset).trim() }
            .getOrNull()
            ?.takeIf(String::isNotBlank)
    }

    return Regex("""filename=([^;]+)""", RegexOption.IGNORE_CASE)
        .find(value)
        ?.groupValues
        ?.getOrNull(1)
        ?.trim()
        ?.trim('"', '\'')
        ?.takeIf(String::isNotBlank)
}

private fun inferContentTypeFromHeader(contentType: String): OverrideContentType? {
    val normalized = contentType.lowercase(Locale.ROOT)
    return when {
        "javascript" in normalized || "ecmascript" in normalized -> OverrideContentType.JavaScript
        "yaml" in normalized || "yml" in normalized -> OverrideContentType.Yaml
        else -> null
    }
}

private fun inferContentTypeFromContent(content: String): OverrideContentType =
    if (content.trimStart().startsWith("function") || "module.exports" in content) {
        OverrideContentType.JavaScript
    } else {
        OverrideContentType.Yaml
    }

private fun ensureSourceNameExtension(
    sourceName: String,
    contentType: OverrideContentType,
): String =
    if (OverrideContentType.fromFileName(sourceName) != null) {
        sourceName
    } else {
        "$sourceName.${contentType.extension}"
    }

internal fun normalizeImportedConfigSourceName(sourceName: String?): String? {
    var normalizedName =
        sourceName
            ?.substringAfterLast('/')
            ?.substringAfterLast('\\')
            ?.trim()
            ?.takeIf(String::isNotBlank) ?: return null

    val removableSuffixes = listOf(".yaml", ".yml", ".js")
    while (true) {
        val matchedSuffix =
            removableSuffixes.firstOrNull { suffix ->
                normalizedName.length > suffix.length &&
                        normalizedName.endsWith(suffix, ignoreCase = true)
            } ?: break
        normalizedName = normalizedName.dropLast(matchedSuffix.length).trimEnd()
    }

    return normalizedName.takeIf(String::isNotBlank)
}
