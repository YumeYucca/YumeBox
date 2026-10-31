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
 * Copyright (c) YumeYucca 2025 - Present
 */

package com.github.yumeyucca.yumebox.data.update

import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber

class AppUpdateRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    suspend fun fetchLatest(baseUrl: String, channel: String): AppUpdateManifest? =
        withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val url = URL("${baseUrl.trimEnd('/')}/releases/$channel/latest.json")
                val opened = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 5_000
                    readTimeout = 5_000
                    instanceFollowRedirects = true
                    useCaches = false
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "YumeBox")
                    setRequestProperty("Cache-Control", "no-cache")
                }
                connection = opened
                if (opened.responseCode !in 200..299) return@withContext null
                val body = opened.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                json.decodeFromString<AppUpdateManifest>(body)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Timber.w(error, "Failed to fetch the app update manifest")
                null
            } finally {
                connection?.disconnect()
            }
        }

    /**
     * Streams the update package to [destination] and reports progress as bytes are written.
     *
     * [expectedSize] comes from the manifest and backs the progress ratio when the server does not
     * send a usable `Content-Length`; when neither is available the callback reports a non-positive
     * total so the UI can fall back to an indeterminate indicator.
     */
    suspend fun download(
        downloadUrl: String,
        destination: OutputStream,
        expectedSize: Long = 0L,
        onProgress: (bytesRead: Long, totalBytes: Long) -> Unit,
    ) {
        withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val opened = (URL(downloadUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    instanceFollowRedirects = true
                    useCaches = false
                    setRequestProperty("User-Agent", "YumeBox")
                }
                connection = opened
                if (opened.responseCode !in 200..299) error("HTTP " + opened.responseCode)
                val contentLength = opened.contentLengthLong
                val totalBytes = if (contentLength > 0) contentLength else expectedSize
                opened.inputStream.use { input ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        destination.write(buffer, 0, read)
                        bytesRead += read
                        onProgress(bytesRead, totalBytes)
                    }
                    destination.flush()
                }
            } finally {
                connection?.disconnect()
            }
        }
    }

    private companion object {
        const val BUFFER_SIZE = 64 * 1024
    }
}
