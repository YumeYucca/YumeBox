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

import android.content.Context
import android.net.Uri
import com.github.yumeyucca.yumebox.core.model.Provider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class ProvidersController(
    private val context: Context,
    private val queryProvidersAction: suspend () -> List<Provider>,
    private val updateProviderAction: suspend (Provider.Type, String) -> Unit,
) {
    @Suppress("TooGenericExceptionCaught")
    suspend fun queryProviders(): Result<List<Provider>> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(queryProvidersAction())
            } catch (error: Exception) { // fault barrier: injected bridge action may fail arbitrarily
                Result.failure(error)
            }
        }

    suspend fun updateProvider(provider: Provider): Result<Unit> =
        updateProviderInternal(provider.type, provider.name)

    suspend fun updateAllProviders(providers: List<Provider>): Result<UpdateProvidersResult> {
        if (providers.isEmpty()) return Result.success(UpdateProvidersResult(emptyList()))

        val failed = mutableListOf<String>()
        providers.forEach { provider ->
            val result = updateProviderInternal(provider.type, provider.name)
            if (result.isFailure) {
                failed.add(provider.name)
            }
        }
        return Result.success(UpdateProvidersResult(failed))
    }

    @Suppress("TooGenericExceptionCaught")
    suspend fun uploadProviderFile(
        context: Context,
        provider: Provider,
        uri: Uri,
        maxBytes: Long = MAX_UPLOAD_SIZE_BYTES,
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val targetFile = buildTargetFile(provider)
                val inputStream =
                    context.contentResolver.openInputStream(uri)
                        ?: return@withContext Result.failure(IllegalStateException("无法读取文件: $uri"))

                inputStream.use { input ->
                    copyBoundedAtomic(input, targetFile, maxBytes)
                }

                Result.success(Unit)
            } catch (
                error:
                Exception
            ) { // fault barrier: IO and path-validation failures both become
                // Result.failure
                Result.failure(error)
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun updateProviderInternal(type: Provider.Type, name: String): Result<Unit> =
        try {
            updateProviderAction(type, name)
            Result.success(Unit)
        } catch (error: Exception) { // fault barrier: injected update action may fail arbitrarily
            Result.failure(error)
        }

    private fun copyBoundedAtomic(input: InputStream, targetFile: File, maxBytes: Long) {
        require(maxBytes >= 0L) { "maxBytes must not be negative" }
        val parent = targetFile.parentFile ?: error("Provider file has no parent")
        val temporary = File.createTempFile("${targetFile.name}.", ".tmp", parent)
        try {
            FileOutputStream(temporary).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var written = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    written += read.toLong()
                    check(written <= maxBytes) {
                        "文件超过 ${maxBytes / (1024 * 1024)}MB 限制"
                    }
                    output.write(buffer, 0, read)
                }
                output.fd.sync()
            }
            try {
                Files.move(
                    temporary.toPath(),
                    targetFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE,
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(
                    temporary.toPath(),
                    targetFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                )
            }
        } finally {
            runCatching { temporary.delete() }
        }
    }

    private fun buildTargetFile(provider: Provider): File {
        if (provider.path.isBlank()) {
            throw IllegalStateException("Provider path is empty")
        }
        val targetFile = File(provider.path).canonicalFile
        val importedRoot = context.filesDir.resolve("imported").canonicalFile
        val inImportedProviders =
            targetFile.toPath().startsWith(importedRoot.toPath()) &&
                    targetFile
                        .toRelativeString(importedRoot)
                        .replace('\\', '/')
                        .matches(Regex("""^[^/]+/providers/(rules|proxies)/.+"""))
        if (!inImportedProviders) {
            throw IllegalStateException(
                "Provider path must live under profile provider directories: ${provider.path}"
            )
        }
        targetFile.parentFile?.mkdirs()
        return targetFile
    }

    data class UpdateProvidersResult(val failedProviders: List<String>)

    companion object {
        private const val MAX_UPLOAD_SIZE_BYTES = 50L * 1024 * 1024
    }
}
