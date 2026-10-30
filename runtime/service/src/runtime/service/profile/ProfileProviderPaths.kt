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

package com.github.yumeyucca.yumebox.runtime.service.profile

import android.content.Context
import com.github.yumeyucca.yumebox.core.bridge.Compiler
import com.github.yumeyucca.yumebox.core.model.CompileRequest
import com.github.yumeyucca.yumebox.core.model.CompileResult
import com.github.yumeyucca.yumebox.core.util.PROXY_PROVIDER_SCOPE
import com.github.yumeyucca.yumebox.core.util.PersistedJson
import com.github.yumeyucca.yumebox.core.util.RULE_PROVIDER_SCOPE
import com.github.yumeyucca.yumebox.core.util.YamlCodec
import com.github.yumeyucca.yumebox.core.util.profileProviderScopeDir
import com.github.yumeyucca.yumebox.runtime.service.session.CompiledConfigPipeline
import timber.log.Timber
import java.io.File
import java.security.MessageDigest
import java.util.*

internal data class ExternalProvider(
    val name: String,
    val url: String,
    val target: File,
    val headers: List<Pair<String, String>>,
)

internal data class ProviderDiscovery(
    val providers: List<ExternalProvider>,
    /** YAML parse failed and text-scan recovered providers (headers may be incomplete). */
    val usedTextScan: Boolean,
    /** Age-encrypted or liboverride failure — path map fell back to source paths. */
    val pathMapSourceFallback: Boolean,
)

private val providerPathPrefixes =
    setOf("providers", "provider", "clash", "ruleset", "rules", "proxies")

private val PROVIDER_PATH_TAIL = Regex("""(?:^|/)providers/(proxies|rules)/(.+)$""")

internal val HTTP_URL_IN_TEXT = Regex("""https?://\S+""", RegexOption.IGNORE_CASE)

private val PROVIDER_ENTRY_NAME = Regex("""^([^:#{}\[\]]+)\s*:$""")

/**
 * Compiles the staged config and returns a map of `scope:name -> relative path under
 * providers/{scope}/`. Used only for path alignment with runtime; URL discovery always reads
 * the source config so a liboverride empty result cannot skip downloads.
 */
internal fun loadLiboverrideProviderPaths(
    context: Context,
    uuid: UUID,
    stagingDir: File,
    profileDir: File,
    ageSecretKey: String?,
): Pair<Map<String, String>, Boolean> {
    if (!ageSecretKey.isNullOrBlank()) {
        Timber.i("Encrypted profile %s: skip liboverride path map, use source paths", uuid)
        return emptyMap<String, String>() to true
    }
    var sourceFallback = false
    val map =
        runCatching {
                val overrides =
                    runCatching {
                            CompiledConfigPipeline(context).resolveOverrideSpecs(uuid.toString())
                        }
                        .onFailure { Timber.w(it, "Resolve overrides for provider path map failed") }
                        .getOrDefault(emptyList())
                val request =
                    CompileRequest(
                        profileUuid = uuid.toString(),
                        profileDir = profileDir.absolutePath,
                        profilePath = stagingDir.resolve("config.yaml").absolutePath,
                        overrides = overrides,
                        outputPath = "",
                        ageSecretKey = null,
                    )
                val result =
                    PersistedJson.decodeFromString(
                        CompileResult.serializer(),
                        Compiler.nativeCompile(
                            PersistedJson.encodeToString(CompileRequest.serializer(), request)
                        ),
                    )
                check(result.success) {
                    result.error ?: "liboverride compile failed for provider path map"
                }
                extractProviderPathMap(result.finalYaml)
            }
            .onFailure {
                sourceFallback = true
                Timber.w(it, "liboverride provider path map failed; using source paths")
            }
            .getOrDefault(emptyMap())
    return map to sourceFallback
}

@Suppress("UNCHECKED_CAST")
private fun extractProviderPathMap(finalYaml: String): Map<String, String> {
    val root = parseConfigMap(finalYaml) ?: return emptyMap()
    val out = linkedMapOf<String, String>()
    listOf(
            "proxy-providers" to PROXY_PROVIDER_SCOPE,
            "rule-providers" to RULE_PROVIDER_SCOPE,
        )
        .forEach { (field, scope) ->
            val definitions = root[field] as? Map<*, *> ?: return@forEach
            definitions.forEach { (rawName, rawDefinition) ->
                val name = rawName?.toString()?.trim().orEmpty()
                val definition = rawDefinition as? Map<*, *> ?: return@forEach
                if (name.isEmpty()) return@forEach
                val rewritten = definition["path"]?.toString()?.trim().orEmpty()
                val relative = providerRelativeFromRewritten(rewritten, scope) ?: return@forEach
                out["$scope:$name"] = relative
            }
        }
    return out
}

private fun providerRelativeFromRewritten(rewrittenPath: String, scope: String): String? {
    val normalized = rewrittenPath.replace('\\', '/').trim()
    if (normalized.isEmpty()) return null
    val match = PROVIDER_PATH_TAIL.find(normalized) ?: return null
    if (match.groupValues[1] != scope) return null
    return match.groupValues[2].takeIf { it.isNotBlank() }
}

/**
 * Discovers downloadable providers from the source config. Prefer liboverride-rewritten
 * relative paths when available; otherwise use the same normalization runtime expects.
 */
internal fun collectDownloadableProviders(
    configText: String,
    stagingDir: File,
    profileDir: File,
    rewrittenPaths: Map<String, String>,
    pathMapSourceFallback: Boolean,
): ProviderDiscovery {
    val root = parseConfigMap(configText)
    val fromYaml =
        if (root != null) {
            collectProvidersFromRoot(root, stagingDir, profileDir, rewrittenPaths)
        } else {
            emptyList()
        }
    if (fromYaml.isNotEmpty()) {
        return ProviderDiscovery(
            providers = fromYaml,
            usedTextScan = false,
            pathMapSourceFallback = pathMapSourceFallback,
        )
    }

    val fromScan = collectProvidersByTextScan(configText, stagingDir, profileDir, rewrittenPaths)
    if (fromScan.isNotEmpty()) {
        Timber.w(
            "YAML map parse found 0 providers; text scan recovered %d downloadable providers",
            fromScan.size,
        )
    }
    return ProviderDiscovery(
        providers = fromScan,
        usedTextScan = fromScan.isNotEmpty(),
        pathMapSourceFallback = pathMapSourceFallback,
    )
}

internal fun parseConfigMap(content: String): Map<String, Any?>? =
    runCatching {
            val loaded = YamlCodec.loadMap(content)
            loaded.takeIf { it.isNotEmpty() }
        }
        .onFailure { Timber.w(it, "YAML parse failed for provider collection") }
        .getOrNull()

@Suppress("UNCHECKED_CAST")
private fun collectProvidersFromRoot(
    root: Map<String, Any?>,
    stagingDir: File,
    profileDir: File,
    rewrittenPaths: Map<String, String>,
): List<ExternalProvider> = buildList {
    listOf(
            "proxy-providers" to PROXY_PROVIDER_SCOPE,
            "rule-providers" to RULE_PROVIDER_SCOPE,
        )
        .forEach { (field, scope) ->
            val definitions = asStringKeyedMap(root[field]) ?: return@forEach
            definitions.forEach { (name, rawDefinition) ->
                val definition = asStringKeyedMap(rawDefinition) ?: return@forEach
                val provider =
                    buildExternalProvider(
                        name = name,
                        definition = definition,
                        scope = scope,
                        stagingDir = stagingDir,
                        profileDir = profileDir,
                        rewrittenPaths = rewrittenPaths,
                    ) ?: return@forEach
                add(provider)
            }
        }
}

private fun buildExternalProvider(
    name: String,
    definition: Map<String, Any?>,
    scope: String,
    stagingDir: File,
    profileDir: File,
    rewrittenPaths: Map<String, String>,
): ExternalProvider? {
    val type = definition["type"]?.toString()?.trim().orEmpty()
    val url = definition["url"]?.toString().orEmpty().trim().trim('"', '\'').trim()
    // Old FetchAndValid: any provider with url+path after patch is downloadable.
    // Inline payloads are already in-config; everything else with an http(s) URL is fetched.
    if (!url.isHttpUrl() || type.equals("inline", ignoreCase = true)) return null

    val extension =
        if (
            scope == RULE_PROVIDER_SCOPE &&
                definition["format"]?.toString()?.equals("mrs", ignoreCase = true) == true
        ) {
            "mrs"
        } else {
            "yaml"
        }

    val relative =
        rewrittenPaths["$scope:$name"]
            ?: providerRelativePath(
                path = definition["path"]?.toString().orEmpty(),
                url = url,
                extension = extension,
                profileProviderDir = profileProviderScopeDir(profileDir, scope),
            )
    val target = profileProviderScopeDir(stagingDir, scope).resolve(relative)
    val resolvedName = name.trim().ifEmpty { target.name }
    return ExternalProvider(
        name = resolvedName,
        url = url,
        target = target,
        headers = providerHeaders(definition),
    )
}

/**
 * Last-resort extractor when SnakeYAML cannot load the document as a map (merge keys / odd
 * tags). Walks proxy-providers / rule-providers blocks and pulls `url:` / simple `header:` entries.
 */
private fun collectProvidersByTextScan(
    configText: String,
    stagingDir: File,
    profileDir: File,
    rewrittenPaths: Map<String, String>,
): List<ExternalProvider> {
    val out = linkedMapOf<String, ExternalProvider>()
    listOf(
            "proxy-providers" to PROXY_PROVIDER_SCOPE,
            "rule-providers" to RULE_PROVIDER_SCOPE,
        )
        .forEach { (field, scope) ->
            val section = extractTopLevelSection(configText, field) ?: return@forEach
            var currentName: String? = null
            var currentUrl: String? = null
            var currentPath: String? = null
            var currentFormat: String? = null
            var currentType: String? = null
            var currentHeaders = mutableListOf<Pair<String, String>>()
            var inHeader = false
            var headerBaseIndent = -1
            var headerKey: String? = null

            fun flush() {
                val name = currentName?.trim().orEmpty()
                val url = currentUrl.orEmpty().trim().trim('"', '\'').trim()
                val type = currentType.orEmpty().trim()
                if (name.isNotEmpty() && url.isHttpUrl() && !type.equals("inline", true)) {
                    val extension =
                        if (
                            scope == RULE_PROVIDER_SCOPE &&
                                currentFormat?.equals("mrs", ignoreCase = true) == true
                        ) {
                            "mrs"
                        } else {
                            "yaml"
                        }
                    val relative =
                        rewrittenPaths["$scope:$name"]
                            ?: providerRelativePath(
                                path = currentPath.orEmpty(),
                                url = url,
                                extension = extension,
                                profileProviderDir = profileProviderScopeDir(profileDir, scope),
                            )
                    val target = profileProviderScopeDir(stagingDir, scope).resolve(relative)
                    out["$scope:$name"] =
                        ExternalProvider(
                            name = name,
                            url = url,
                            target = target,
                            headers = currentHeaders.toList(),
                        )
                }
                currentUrl = null
                currentPath = null
                currentFormat = null
                currentType = null
                currentHeaders = mutableListOf()
                inHeader = false
                headerBaseIndent = -1
                headerKey = null
            }

            section.lineSequence().forEach { rawLine ->
                val line = rawLine.trimEnd()
                val indent = line.indexOfFirst { !it.isWhitespace() }.let { if (it < 0) 0 else it }
                val content = line.trim()
                if (content.isEmpty() || content.startsWith("#")) return@forEach

                // Leave header block when indent returns to provider field level.
                if (inHeader && indent <= headerBaseIndent && !content.startsWith("-")) {
                    inHeader = false
                    headerBaseIndent = -1
                    headerKey = null
                }

                // New provider entry at indent 2 (or any non-nested map key with trailing ':')
                val nameMatch = PROVIDER_ENTRY_NAME.find(content)
                if (
                    indent <= 2 &&
                        nameMatch != null &&
                        !content.contains(": ") &&
                        content.endsWith(":")
                ) {
                    flush()
                    currentName = nameMatch.groupValues[1].trim().trim('"', '\'')
                    return@forEach
                }
                if (currentName == null) return@forEach

                if (inHeader) {
                    when {
                        content.startsWith("-") -> {
                            val value = content.removePrefix("-").trim().trim('"', '\'')
                            val key = headerKey
                            if (!key.isNullOrBlank() && value.isNotBlank()) {
                                currentHeaders += key to value
                            }
                        }
                        content.contains(':') -> {
                            val kv = content.split(":", limit = 2)
                            val key = kv[0].trim().trim('"', '\'')
                            val value = kv.getOrNull(1)?.trim()?.trim('"', '\'')?.trim().orEmpty()
                            if (key.isNotBlank()) {
                                headerKey = key
                                if (value.isNotBlank()) {
                                    currentHeaders += key to value
                                }
                            }
                        }
                    }
                    return@forEach
                }

                val kv = content.split(":", limit = 2)
                if (kv.size != 2) return@forEach
                val key = kv[0].trim()
                val value = kv[1].trim().trim('"', '\'')
                when (key) {
                    "url" -> currentUrl = value
                    "path" -> currentPath = value
                    "format" -> currentFormat = value
                    "type" -> currentType = value
                    "header" -> {
                        // Inline map is rare; nested block is the common Clash form.
                        inHeader = true
                        headerBaseIndent = indent
                        headerKey = null
                        if (value.isNotBlank() && value != "|" && value != ">") {
                            // Unsupported inline header form — leave empty (headerDegraded via
                            // text-scan).
                            Timber.d("text-scan header inline value ignored for %s", currentName)
                        }
                    }
                }
            }
            flush()
        }
    return out.values.toList()
}

private fun extractTopLevelSection(configText: String, key: String): String? {
    val header =
        Regex("^$key\\s*:\\s*(?:#.*)?$", RegexOption.MULTILINE).find(configText) ?: return null
    val start = header.range.last + 1
    val rest = configText.substring(start)
    val nextTop = Regex("^[A-Za-z0-9_.-]+\\s*:", RegexOption.MULTILINE).find(rest)
    return if (nextTop == null) rest else rest.substring(0, nextTop.range.first)
}

@Suppress("UNCHECKED_CAST")
private fun asStringKeyedMap(value: Any?): Map<String, Any?>? {
    val map = value as? Map<*, *> ?: return null
    if (map.isEmpty()) return emptyMap()
    val out = LinkedHashMap<String, Any?>(map.size)
    map.forEach { (rawKey, rawValue) ->
        val key = rawKey?.toString()?.trim().orEmpty()
        if (key.isNotEmpty()) out[key] = rawValue
    }
    return out
}

internal fun providerRelativePath(
    path: String,
    url: String,
    extension: String,
    profileProviderDir: File,
): String {
    val raw = path.trim()
    val segments =
        if (raw.isBlank()) {
            emptyList()
        } else if (File(raw).isAbsolute) {
            absoluteProviderPathTail(raw, profileProviderDir) ?: listOf(File(raw).name)
        } else {
            raw.replace('\\', '/').split('/')
        }
    val cleaned = mutableListOf<String>()
    segments.forEach { segment ->
        when (segment) {
            "",
            "." -> Unit
            ".." -> if (cleaned.isNotEmpty()) cleaned.removeAt(cleaned.lastIndex)
            else -> cleaned += segment
        }
    }
    while (cleaned.firstOrNull() in providerPathPrefixes) cleaned.removeAt(0)
    val relative = cleaned.joinToString("/")
    val withExtension =
        when {
            relative.isBlank() -> "${sha256(url)}.$extension"
            File(relative).extension.isNotBlank() -> relative
            else -> "$relative.$extension"
        }
    return withExtension
}

private fun absoluteProviderPathTail(path: String, providerDir: File): List<String>? {
    val normalizedPath = path.replace('\\', '/').trimEnd('/')
    val normalizedBase = providerDir.absolutePath.replace('\\', '/').trimEnd('/')
    if (!normalizedPath.startsWith("$normalizedBase/")) return null
    return normalizedPath.removePrefix("$normalizedBase/").split('/')
}

@Suppress("UNCHECKED_CAST")
internal fun providerHeaders(definition: Map<*, *>): List<Pair<String, String>> {
    val rawHeaders = definition["header"] as? Map<*, *> ?: return emptyList()
    return buildList {
        rawHeaders.forEach { (rawName, rawValue) ->
            val name = rawName?.toString()?.trim().orEmpty()
            if (name.isBlank()) return@forEach
            when (rawValue) {
                is Iterable<*> ->
                    rawValue.forEach { value ->
                        value?.toString()?.takeIf(String::isNotBlank)?.let { add(name to it) }
                    }

                else -> rawValue?.toString()?.takeIf(String::isNotBlank)?.let { add(name to it) }
            }
        }
    }
}

internal fun String.isHttpUrl(): Boolean =
    startsWith("https://", ignoreCase = true) || startsWith("http://", ignoreCase = true)

internal fun readConfigText(config: File): String = config.readText().removePrefix("\uFEFF")

internal fun sha256(value: String): String =
    MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") {
        "%02x".format(it)
    }
