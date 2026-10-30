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

package com.github.yumeyucca.yumebox.substore.util

import android.app.Application
import com.github.yumeyucca.yumebox.common.util.SubscriptionUserAgentDefaults
import com.github.yumeyucca.yumebox.data.store.AppSettingsStore
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

private enum class ArchiveFormat(val suffix: String, private val extractor: (File, File) -> Boolean) {
    Zip(".zip", ArchiveUtil::unzipZip),
    TarGz(".tar.gz", ArchiveUtil::untarGz),
    Tgz(".tgz", ArchiveUtil::untarGz),
    Tar(".tar", ArchiveUtil::untar),
    ;

    fun extract(archive: File, targetDir: File): Boolean = extractor(archive, targetDir)

    companion object {
        fun fromUrl(url: String): ArchiveFormat =
            entries.firstOrNull { url.endsWith(it.suffix, ignoreCase = true) } ?: Zip
    }
}

class SubStoreDownloadClient(
    private val application: Application,
    private val appSettings: AppSettingsStore,
) {
    private val client: HttpClient by lazy {
        HttpClient(Android) {
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 60_000
            }
            followRedirects = true
        }
    }

    @Suppress("TooGenericExceptionCaught")
    suspend fun download(
        url: String,
        targetFile: File,
    ): Boolean =
        withContext(Dispatchers.IO) {
            try {
                targetFile.parentFile?.mkdirs()
                if (targetFile.exists()) targetFile.delete()

                client
                    .prepareGet(url) { header(HttpHeaders.UserAgent, resolveUserAgent()) }
                    .execute { response ->
                        if (!response.status.isSuccess()) {
                            return@execute false
                        }

                        val inputStream = response.bodyAsChannel().toInputStream()

                        targetFile.outputStream().buffered().use { output ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int

                            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                            }
                            output.flush()
                        }

                        true
                    }
            } catch (error: Exception) {
                // fault barrier: ktor/IO/URL failures all map to download-failed
                Timber.e(error, "Download failed: %s", url)
                if (targetFile.exists()) targetFile.delete()
                false
            }
        }

    suspend fun downloadAndExtract(
        url: String,
        targetDir: File,
    ): Boolean =
        withContext(Dispatchers.IO) {
            val format = ArchiveFormat.fromUrl(url)
            val tempFile =
                File(application.cacheDir, "temp_${System.currentTimeMillis()}${format.suffix}")
            val downloadSuccess = download(url, tempFile)
            if (!downloadSuccess) {
                return@withContext false
            }

            val extractSuccess = format.extract(tempFile, targetDir)

            tempFile.delete()

            if (extractSuccess) {
                flattenRootDirectory(targetDir)
            }

            extractSuccess
        }

    private fun resolveUserAgent(): String {
        val customUA = appSettings.customUserAgent.value
        return customUA.ifEmpty { SubscriptionUserAgentDefaults.DEFAULT }
    }

    private fun flattenRootDirectory(targetDir: File) {
        val subDirs = targetDir.listFiles { it.isDirectory } ?: return
        if (subDirs.size != 1) return

        val rootDir = subDirs[0]
        val commonRootNames = setOf("dist", "build", "public", "www", "static")
        if (rootDir.name !in commonRootNames) return

        rootDir.listFiles()?.forEach { file ->
            val newFile = File(targetDir, file.name)
            if (file.isDirectory) {
                moveDirectoryRecursively(file, newFile)
            } else {
                file.renameTo(newFile)
            }
        }
        rootDir.delete()
    }

    private fun moveDirectoryRecursively(source: File, destination: File) {
        if (destination.exists()) {
            destination.deleteRecursively()
        }
        destination.mkdirs()

        source.listFiles()?.forEach { file ->
            val destFile = File(destination, file.name)
            if (file.isDirectory) {
                moveDirectoryRecursively(file, destFile)
                file.delete()
            } else {
                file.renameTo(destFile)
            }
        }

        source.delete()
    }
}
