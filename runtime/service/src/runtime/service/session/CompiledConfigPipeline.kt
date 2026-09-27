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

@file:Suppress("UnusedSymbol", "RedundantSuspendModifier")

package com.github.yumeyucca.yumebox.runtime.service.session

import android.content.Context
import android.util.Log
import com.github.yumeyucca.yumebox.core.bridge.Compiler
import com.github.yumeyucca.yumebox.core.model.*
import com.github.yumeyucca.yumebox.core.util.YamlCodec
import com.github.yumeyucca.yumebox.core.util.runtimeHomeDir
import com.github.yumeyucca.yumebox.data.model.BuiltInOverrideCatalog
import com.github.yumeyucca.yumebox.data.store.BuiltInOverrideFileStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest

class CompiledConfigPipeline(private val context: Context) {
    private val builtInOverrideFiles = BuiltInOverrideFileStore(context)
    fun resolveOverrideSpecs(profileUuid: String): List<OverrideSpec> =
        resolveOverrideBundle(profileUuid, logger = null).overrides

    fun resolveOverrideSpecs(profileUuid: String, logger: ((String) -> Unit)?): List<OverrideSpec> =
        resolveOverrideBundle(profileUuid, logger).overrides

    fun resolveOverrideBundle(profileUuid: String): ResolvedOverrideBundle =
        resolveOverrideBundle(profileUuid, logger = null)

    fun resolveOverrideBundle(
        profileUuid: String,
        logger: ((String) -> Unit)?,
    ): ResolvedOverrideBundle {
        val overridesDir = context.filesDir.resolve("overrides")
        val metadataFile = overridesDir.resolve("metadata.yaml")
        val metadata = loadMetadataIndex(metadataFile, logger)

        val binding = metadata.profileChains[profileUuid]
        logger?.invoke(
            "override resolve: profile=$profileUuid overrideIds=${binding?.overrideIds.orEmpty()}"
        )

        val userOverrides = mutableListOf<OverrideSpec>()
        val overrides = mutableListOf<OverrideSpec>()
        binding
            ?.overrideIds
            .orEmpty()
            .filterNot(::isLegacyPresetId)
            .filterNot(::isInternalRuntimeId)
            .distinct()
            .forEach { overrideId ->
                val file =
                    resolveUserOverrideFile(overridesDir, overrideId, metadata)
                        ?: error(
                            "Override config not found for profile=$profileUuid id=$overrideId"
                        )
                val spec = file.toOverrideSpec()
                logger?.invoke(describeOverrideFile(file, overrideId))
                userOverrides += spec
                overrides += spec
            }

        val runtimeInternalOverride =
            resolveRuntimeInternalOverrideFile(overridesDir, profileUuid)
                ?.also { file ->
                    logger?.invoke(describeOverrideFile(file, INTERNAL_RUNTIME_PREFIX))
                }
                ?.toOverrideSpec()

        runtimeInternalOverride?.let(overrides::add)

        logger?.invoke(
            "override resolve: profile=$profileUuid resolved=${overrides.size} " +
                overrides.joinToString(prefix = "[", postfix = "]") { spec ->
                    "${spec.ext}:${spec.path.safeLogHash()}"
                }
        )

        return ResolvedOverrideBundle(
            profileUuid = profileUuid,
            userOverrides = userOverrides,
            runtimeInternalOverride = runtimeInternalOverride,
            overrides = overrides,
        )
    }

    /**
     * Compiles the profile + override chain to the final mihomo config (liboverride) and returns
     * it. Used by the out-of-process path: the caller streams this to the core over the socketpair,
     * so it is never written to disk. The core reads and applies it itself.
     */
    data class CompiledRuntimeConfig(
        val finalYaml: String,
        val warnings: List<String> = emptyList(),
        val fingerprint: String = "",
    )

    suspend fun compile(spec: RuntimeSpec): String = compileDetailed(spec).finalYaml

    suspend fun compileDetailed(spec: RuntimeSpec): CompiledRuntimeConfig =
        withContext(Dispatchers.Default) {
            val request = buildRequest(spec)
            val result =
                compilerJson.decodeFromString(
                    CompileResult.serializer(),
                    Compiler.nativeCompile(
                        compilerJson.encodeToString(CompileRequest.serializer(), request)
                    ),
                )
            check(result.success) { result.error ?: "override compile failed" }
            CompiledRuntimeConfig(
                finalYaml = result.finalYaml,
                warnings = result.warnings,
                fingerprint = result.fingerprint,
            )
        }

    fun extractProxyGroupNames(finalYaml: String): List<String> =
        runCatching {
                YamlCodec.decode(CompiledGroupConfig.serializer(), finalYaml)
                    .proxyGroups
                    .asSequence()
                    .map { it.name.trim() }
                    .filter { it.isNotEmpty() }
                    .toList()
            }
            .getOrDefault(emptyList())

    /**
     * Returns the compiled YAML for non-encrypted profiles (the user-initiated "view compiled
     * config" export). This is the ONLY path that materialises plaintext finalYaml in Kotlin, and
     * it is intentionally retained: it is out of scope for the runtime.yaml elimination because it
     * is an explicit user export, not a runtime/load path. `Clash.compilePreview` returns the YAML
     * in memory and does NOT write `outputPath` to disk. Throws for encrypted profiles since full
     * YAML must not reach the Kotlin heap.
     */
    suspend fun previewCompiledYaml(
        profileUuid: String,
        profileDir: File,
        overrideSpecs: List<OverrideSpec> = resolveOverrideBundle(profileUuid).overrides,
        ageSecretKey: String? = null,
    ): CompileResult =
        withContext(Dispatchers.Default) {
            require(ageSecretKey == null) {
                "previewCompiledYaml is not supported for encrypted profiles"
            }
            val request =
                CompileRequest(
                    profileUuid = profileUuid,
                    profileDir = profileDir.absolutePath,
                    profilePath = profileDir.resolve("config.yaml").absolutePath,
                    overrides = overrideSpecs,
                    outputPath = profileDir.resolve("runtime.yaml").absolutePath,
                )
            val result =
                compilerJson.decodeFromString(
                    CompileResult.serializer(),
                    Compiler.nativeCompile(
                        compilerJson.encodeToString(CompileRequest.serializer(), request)
                    ),
                )
            check(result.success) { result.error ?: "override preview failed" }
            validateCompiledProviderPaths(result.finalYaml, profileDir)
            result
        }

    private fun buildRequest(spec: RuntimeSpec): CompileRequest {
        val profileDir = File(spec.profileDir)
        return CompileRequest(
            profileUuid = spec.profileUuid,
            profileDir = profileDir.absolutePath,
            profilePath = profileDir.resolve("config.yaml").absolutePath,
            overrides = spec.overrideSpecs,
            outputPath =
                spec.runtimeConfigPath.ifBlank { profileDir.resolve("runtime.yaml").absolutePath },
            ageSecretKey = spec.ageSecretKey,
            runMode = spec.runMode,
            skipRuntimePatches = spec.skipRuntimePatches,
            preview = spec.preview,
        )
    }

    private fun validateCompiledProviderPaths(finalYaml: String, profileDir: File) {
        val invalidPaths = mutableListOf<String>()
        val runtimeHomeDir = context.runtimeHomeDir
        val expectedRuleBase = profileDir.resolve("providers/rules").canonicalFile
        val expectedProxyBase = profileDir.resolve("providers/proxies").canonicalFile
        pathPattern.findAll(finalYaml).forEach { match ->
            val pathValue = match.groupValues[1].replace('\\', '/').trim()
            if (
                !pathValue.endsWith(".yaml") &&
                    !pathValue.endsWith(".yml") &&
                    !pathValue.endsWith(".mrs")
            ) {
                return@forEach
            }
            val isLegacyPath =
                pathValue.startsWith("./ruleset/") ||
                    pathValue.startsWith("ruleset/") ||
                    pathValue.contains("/clash/")
            val resolvedPath = runtimeHomeDir.resolve(pathValue).canonicalFile
            val inProfileProviders =
                resolvedPath.toPath().startsWith(expectedRuleBase.toPath()) ||
                    resolvedPath.toPath().startsWith(expectedProxyBase.toPath())
            if (isLegacyPath || File(pathValue).isAbsolute || !inProfileProviders) {
                invalidPaths += pathValue
            }
        }
        if (invalidPaths.isNotEmpty()) {
            invalidPaths.forEachIndexed { index, invalidPath ->
                Log.e(
                    TAG,
                    "Compiled provider path invalid index=$index path=${invalidPath.safeLogHash()}",
                )
            }
            error("Compiled provider path escaped profile scope")
        }
        if (pathPattern.containsMatchIn(finalYaml)) {
            Log.i(TAG, "Compiled provider paths validated")
        }
    }

    private fun loadMetadataIndex(
        metadataFile: File,
        logger: ((String) -> Unit)?,
    ): MetadataIndexPayload {
        if (!metadataFile.exists()) {
            return MetadataIndexPayload()
        }
        val metadataRaw = metadataFile.readText()
        return runCatching { YamlCodec.decode(MetadataIndexPayload.serializer(), metadataRaw) }
            .getOrElse {
                logger?.invoke(
                    "override resolve: metadata decode failed file=${metadataFile.safeLogHash()} " +
                        "size=${metadataRaw.length} sha=${metadataRaw.sha256Short()}"
                )
                MetadataIndexPayload()
            }
    }

    private fun resolveUserOverrideFile(
        overridesDir: File,
        overrideId: String,
        metadataIndex: MetadataIndexPayload,
    ): File? {
        materializeBuiltInOverride(overrideId)?.let {
            return it
        }

        val expectedExtension =
            metadataIndex.configs[overrideId]?.contentType?.toOverrideExtension()
        if (expectedExtension != null) {
            val expectedFile = overridesDir.resolve("configs/$overrideId.$expectedExtension")
            if (expectedFile.exists()) {
                return expectedFile
            }
        }

        return userOverrideExtensions
            .asSequence()
            .map { extension -> overridesDir.resolve("configs/$overrideId.$extension") }
            .firstOrNull(File::exists)
    }

    private fun materializeBuiltInOverride(overrideId: String): File? {
        if (BuiltInOverrideCatalog.find(overrideId) == null) return null
        return builtInOverrideFiles.sync(overrideId)
    }

    private fun resolveRuntimeInternalOverrideFile(overridesDir: File, profileUuid: String): File? {
        val file =
            overridesDir.resolve("configs/${INTERNAL_RUNTIME_PREFIX}-profile-$profileUuid.yaml")
        if (!file.exists()) return null
        val content = runCatching {
            file.readText()
        }
            .getOrElse {
                error(
                    "Runtime override file unreadable id=${profileUuid.safeLogHash()} reason=${it.message.safeNativeDiagnostic()}"
                )
            }
        if (content.isBlank()) {
            runCatching { file.delete() }
            return null
        }
        return file
    }

    private fun isInternalRuntimeId(overrideId: String): Boolean =
        overrideId.startsWith(INTERNAL_RUNTIME_PREFIX)

    private fun isLegacyPresetId(overrideId: String): Boolean =
        overrideId.startsWith(LEGACY_PRESET_PREFIX)

    private fun describeOverrideFile(file: File, overrideId: String): String {
        // Avoid reading full override bodies just for diagnostics on the start hot path.
        return buildString {
            append("override resolve: file id=")
            append(overrideId)
            append(" file=")
            append(file.safeLogHash())
            append(" exists=")
            append(file.exists())
            append(" size=")
            append(if (file.exists()) file.length() else -1L)
            append(" mtime=")
            append(if (file.exists()) file.lastModified() else -1L)
        }
    }

    private fun File.toOverrideSpec(): OverrideSpec {
        val extension =
            extension.lowercase().ifBlank {
                error("Override file missing extension")
            }
        return OverrideSpec(path = absolutePath, ext = extension)
    }

    private fun File.safeLogHash(): String = absolutePath.safeLogHash()

    private fun String.safeLogHash(): String = sha256Short()

    private fun String?.safeNativeDiagnostic(): String {
        val raw = this?.takeIf(String::isNotBlank) ?: return "unknown"
        return "len=${raw.length} sha=${raw.sha256Short()}"
    }

    private fun String.sha256Short(): String {
        if (isBlank()) return "empty"
        val digest = MessageDigest.getInstance("SHA-256").digest(toByteArray())
        return digest.take(8).joinToString("") { "%02x".format(it) }
    }

    data class ResolvedOverrideBundle(
        val profileUuid: String,
        val userOverrides: List<OverrideSpec>,
        val runtimeInternalOverride: OverrideSpec?,
        val overrides: List<OverrideSpec>,
    )

    @Serializable
    private data class CompiledGroupConfig(
        @SerialName("proxy-groups") val proxyGroups: List<CompiledProxyGroup> = emptyList()
    )

    @Serializable private data class CompiledProxyGroup(val name: String = "")

    @Serializable
    private data class MetadataIndexPayload(
        val configs: Map<String, ConfigMetadataPayload> = emptyMap(),
        val profileChains: Map<String, ProfileChainPayload> = emptyMap(),
    )

    @Serializable private data class ConfigMetadataPayload(val contentType: String = "yaml")

    @Serializable
    private data class ProfileChainPayload(val overrideIds: List<String> = emptyList())

    private companion object {
        private const val TAG = "CompiledConfigPipeline"
        private val compilerJson =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
                coerceInputValues = true
            }
        private val pathPattern = Regex("""(?m)path:\s*["']?([^"'\n]+)["']?""")
        private val userOverrideExtensions = listOf("yaml", "yml", "js")
        const val INTERNAL_RUNTIME_PREFIX = "__runtime__"
        const val LEGACY_PRESET_PREFIX = "preset-"
    }
}

private fun String.toOverrideExtension(): String? =
    when (lowercase()) {
        "yaml",
        "yml" -> "yaml"

        "js",
        "javascript" -> "js"

        else -> null
    }
