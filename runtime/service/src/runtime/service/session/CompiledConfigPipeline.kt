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

package com.github.yumeyucca.yumebox.runtime.service.session

import android.content.Context
import com.github.yumeyucca.yumebox.core.bridge.Compiler
import com.github.yumeyucca.yumebox.core.model.CompileRequest
import com.github.yumeyucca.yumebox.core.model.CompileResult
import com.github.yumeyucca.yumebox.core.model.OverrideSpec
import com.github.yumeyucca.yumebox.core.util.YamlCodec
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

    /** The profile's user override chain, then its runtime-internal override if one exists. */
    fun resolveOverrideSpecs(profileUuid: String): List<OverrideSpec> {
        val overridesDir = context.filesDir.resolve("overrides")
        val metadata = loadMetadataIndex(overridesDir.resolve("metadata.yaml"))
        val userOverrides =
            metadata.profileChains[profileUuid]
                ?.overrideIds
                .orEmpty()
                .filterNot { it.startsWith(LEGACY_PRESET_PREFIX) || it.startsWith(INTERNAL_RUNTIME_PREFIX) }
                .distinct()
                .map { overrideId ->
                    val file =
                        resolveUserOverrideFile(overridesDir, overrideId, metadata)
                            ?: error("Override config not found for profile=$profileUuid id=$overrideId")
                    file.toOverrideSpec()
                }
        val internal = resolveRuntimeInternalOverrideFile(overridesDir, profileUuid)?.toOverrideSpec()
        return userOverrides + listOfNotNull(internal)
    }

    /**
     * Compiles the profile + override chain to the final mihomo config (liboverride). The caller
     * streams [CompileResult.finalYaml] to the core over the socketpair; it is never written to disk.
     */
    suspend fun compile(spec: RuntimeSpec): CompileResult =
        withContext(Dispatchers.Default) {
            val result =
                compilerJson.decodeFromString(
                    CompileResult.serializer(),
                    Compiler.nativeCompile(
                        compilerJson.encodeToString(CompileRequest.serializer(), buildRequest(spec))
                    ),
                )
            check(result.success) { result.error ?: "override compile failed" }
            result
        }

    fun extractProxyGroupNames(finalYaml: String): List<String> =
        runCatching {
                YamlCodec.decode(CompiledGroupConfig.serializer(), finalYaml)
                    .proxyGroups
                    .map { it.name.trim() }
                    .filter { it.isNotEmpty() }
            }
            .getOrDefault(emptyList())

    private fun buildRequest(spec: RuntimeSpec): CompileRequest {
        val profileDir = File(spec.profileDir)
        return CompileRequest(
            profileUuid = spec.profileUuid,
            profileDir = profileDir.absolutePath,
            profilePath = profileDir.resolve("config.yaml").absolutePath,
            overrides = spec.overrideSpecs,
            outputPath = profileDir.resolve("runtime.yaml").absolutePath,
            ageSecretKey = spec.ageSecretKey,
            runMode = spec.runMode,
            skipRuntimePatches = spec.skipRuntimePatches,
            preview = spec.preview,
        )
    }

    private fun loadMetadataIndex(metadataFile: File): MetadataIndexPayload {
        if (!metadataFile.exists()) return MetadataIndexPayload()
        return runCatching { YamlCodec.decode(MetadataIndexPayload.serializer(), metadataFile.readText()) }
            .getOrDefault(MetadataIndexPayload())
    }

    private fun resolveUserOverrideFile(
        overridesDir: File,
        overrideId: String,
        metadataIndex: MetadataIndexPayload,
    ): File? {
        if (BuiltInOverrideCatalog.find(overrideId) != null) {
            builtInOverrideFiles.sync(overrideId)?.let { return it }
        }
        val expectedExtension = metadataIndex.configs[overrideId]?.contentType?.toOverrideExtension()
        if (expectedExtension != null) {
            val expectedFile = overridesDir.resolve("configs/$overrideId.$expectedExtension")
            if (expectedFile.exists()) return expectedFile
        }
        return userOverrideExtensions
            .asSequence()
            .map { extension -> overridesDir.resolve("configs/$overrideId.$extension") }
            .firstOrNull(File::exists)
    }

    /** A blank internal override is stale: it is deleted and skipped. */
    private fun resolveRuntimeInternalOverrideFile(overridesDir: File, profileUuid: String): File? {
        val file = overridesDir.resolve("configs/$INTERNAL_RUNTIME_PREFIX-profile-$profileUuid.yaml")
        if (!file.exists()) return null
        val content =
            runCatching { file.readText() }
                .getOrElse {
                    error(
                        "Runtime override file unreadable id=${profileUuid.sha256Short()} " +
                            "reason=${it.message.safeNativeDiagnostic()}"
                    )
                }
        if (content.isBlank()) {
            runCatching { file.delete() }
            return null
        }
        return file
    }

    private fun File.toOverrideSpec(): OverrideSpec {
        val extension = extension.lowercase().ifBlank { error("Override file missing extension") }
        return OverrideSpec(path = absolutePath, ext = extension)
    }

    private fun String?.safeNativeDiagnostic(): String {
        val raw = this?.takeIf(String::isNotBlank) ?: return "unknown"
        return "len=${raw.length} sha=${raw.sha256Short()}"
    }

    private fun String.sha256Short(): String {
        if (isBlank()) return "empty"
        val digest = MessageDigest.getInstance("SHA-256").digest(toByteArray())
        return digest.take(8).joinToString("") { "%02x".format(it) }
    }

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
        private val compilerJson =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
                coerceInputValues = true
            }
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
