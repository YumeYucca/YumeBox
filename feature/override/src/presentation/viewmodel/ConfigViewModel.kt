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

package com.github.yumeyucca.yumebox.feature.override.presentation.viewmodel

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
import kotlinx.coroutines.CancellationException
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
) : ViewModel() {
    companion object {
        private const val TAG = "OverrideConfigViewModel"
        private const val NETWORK_IMPORT_CONNECT_TIMEOUT_MS = 15_000
        private const val NETWORK_IMPORT_READ_TIMEOUT_MS = 30_000
        private const val NETWORK_IMPORT_MAX_BYTES = 5L * BYTES_PER_MEBIBYTE
    }

    private val _builtInConfigs = MutableStateFlow<List<OverrideConfig>>(emptyList())
    val builtInConfigs: StateFlow<List<OverrideConfig>> = _builtInConfigs.asStateFlow()

    private val _userConfigs = MutableStateFlow<List<OverrideConfig>>(emptyList())
    val userConfigs: StateFlow<List<OverrideConfig>> = _userConfigs.asStateFlow()

    private val _usageCountMap = MutableStateFlow<Map<String, Int>>(emptyMap())
    val usageCountMap: StateFlow<Map<String, Int>> = _usageCountMap.asStateFlow()

    private val _pendingRevealConfigId = MutableStateFlow<String?>(null)
    val pendingRevealConfigId: StateFlow<String?> = _pendingRevealConfigId.asStateFlow()
    private val refreshMutex = Mutex()

    init {
        refresh()
        viewModelScope.launch {
            bindingProvider.getAllBindingsFlow().collectLatest { loadUsageCounts() }
        }
    }

    fun refresh() {
        viewModelScope.launch { refreshNow() }
    }

    suspend fun refreshAndAwait() {
        refreshNow()
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun refreshNow() {
        refreshMutex.withLock {
            try {
                // Load independently: a built-in asset hiccup must not wipe imported overrides
                // (and vice versa). First open used to fail the whole try when materialize threw.
                val builtIns = runCatching {
                    configRepo.getBuiltInConfigs()
                }
                    .onFailure { error ->
                        if (error is CancellationException) throw error
                        Timber.tag(TAG).e(error, "Failed to load built-in overrides")
                    }
                    .getOrDefault(_builtInConfigs.value)
                val users = runCatching {
                    configRepo.getUserConfigs()
                }
                    .onFailure { error ->
                        if (error is CancellationException) throw error
                        Timber.tag(TAG).e(error, "Failed to load imported overrides")
                    }
                    .getOrDefault(_userConfigs.value)
                _builtInConfigs.value = builtIns
                _userConfigs.value = users
                loadUsageCounts()
            } catch (error: CancellationException) {
                // A cancelled refresh must not be logged or treated as a load failure.
                throw error
            } catch (error: Exception) {
                // Fault barrier: both loads above already fall back to the cached lists, so
                // only the trailing usage-count refresh can reach here.
                Timber.tag(TAG).e(error, "Failed to load overrides")
            }
        }
    }

    fun getConfigById(id: String): OverrideConfig? =
        _builtInConfigs.value.find { it.id == id } ?: _userConfigs.value.find { it.id == id }

    suspend fun getConfigContent(configId: String): String? = configRepo.getConfigContent(configId)

    fun isBuiltInConfig(id: String): Boolean = OverrideInternalConstants.isBuiltInOverrideId(id)

    suspend fun saveConfigContent(configId: String, content: String): Boolean {
        // Runs on the editor's own save coroutine: the write and the reload stay together, so the
        // caller only learns "saved" once the active profile has actually been re-applied.
        if (!configRepo.saveConfigContent(configId, content)) return false

        activeProfileOverrideReloader.reapplyActiveProfileIfUsingOverride(configId)
        refreshNow()
        return true
    }

    fun createConfig(name: String, contentType: OverrideContentType) {
        viewModelScope.launch {
            runCatching {
                val now = System.currentTimeMillis()
                val config =
                    OverrideConfig(
                        id = OverrideMetadata.generateId(),
                        name = name,
                        contentType = contentType,
                        content = "",
                        createdAt = now,
                        updatedAt = now,
                    )
                configRepo.save(config)
                _pendingRevealConfigId.value = config.id
                refreshNow()
            }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    Timber.tag(TAG).e(error, "Failed to create override")
                }
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
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    Timber.tag(TAG).e(error, "Failed to delete override")
                }
        }
    }

    fun renameConfig(id: String, name: String) {
        if (isBuiltInConfig(id)) return
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                val current = getConfigById(id) ?: configRepo.getById(id) ?: return@runCatching
                if (current.name == trimmedName) return@runCatching
                configRepo.save(current.copy(name = trimmedName))
                refreshNow()
            }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    Timber.tag(TAG).e(error, "Failed to rename override")
                }
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

            runCatching { configRepo.reorderUserConfigs(reorderedConfigs.map(OverrideConfig::id)) }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    Timber.tag(TAG).e(error, "Failed to reorder overrides")
                }
            refreshNow()
        }
    }

    suspend fun importConfig(
        content: String,
        sourceName: String?,
        fallbackContentType: OverrideContentType? = null,
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
            .onFailure { error ->
                // A cancelled import is not an import failure: reporting it as one would hand the
                // caller a Result.failure it turns into a "save failed" message after cancellation.
                if (error is CancellationException) throw error
                Timber.tag(TAG).e(error, "Failed to import override")
            }
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
                        networkImportTooLargeMessage(NETWORK_IMPORT_MAX_BYTES)
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
                .onFailure { error ->
                    // Same as importConfig: the caller must see cancellation, not a failed download.
                    if (error is CancellationException) throw error
                }
        }

    fun consumePendingRevealConfig(configId: String) {
        if (_pendingRevealConfigId.value == configId) {
            _pendingRevealConfigId.value = null
        }
    }

    private suspend fun loadUsageCounts() {
        val countMap = mutableMapOf<String, Int>()
        (_builtInConfigs.value + _userConfigs.value).forEach { config ->
            countMap[config.id] = resolver.getOverrideUsageCount(config.id)
        }
        _usageCountMap.value = countMap
    }
}

private const val BYTES_PER_MEBIBYTE = 1024 * 1024

private fun networkImportTooLargeMessage(maxBytes: Long): String =
    YumeTxt.Override.Import.TooLarge.format(maxBytes / BYTES_PER_MEBIBYTE)

private fun readUtf8TextLimited(input: InputStream, maxBytes: Long): String {
    require(maxBytes in 0..Int.MAX_VALUE.toLong()) { "maxBytes is out of range" }
    val output = ByteArrayOutputStream(minOf(DEFAULT_BUFFER_SIZE.toLong(), maxBytes).toInt())
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        total += read.toLong()
        require(total <= maxBytes) { networkImportTooLargeMessage(maxBytes) }
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

private fun normalizeImportedConfigSourceName(sourceName: String?): String? {
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
