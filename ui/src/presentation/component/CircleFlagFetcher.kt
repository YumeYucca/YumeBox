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

package com.github.yumeyucca.yumebox.presentation.component

import android.content.Context
import androidx.annotation.WorkerThread
import com.github.panpf.sketch.ComponentRegistry
import com.github.panpf.sketch.fetch.FetchResult
import com.github.panpf.sketch.fetch.Fetcher
import com.github.panpf.sketch.request.RequestContext
import com.github.panpf.sketch.source.ByteArrayDataSource
import com.github.panpf.sketch.source.DataFrom
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipFile

private const val CIRCLE_FLAG_SCHEME = "circle-flag"
private const val CIRCLE_FLAG_ARCHIVE = "circle-flags/country-flags.zip"
private const val CIRCLE_FLAG_CACHE_FILE = "country-flags-379588b5.zip"
private const val UNKNOWN_FLAG_PATH = "other/earth.svg"
private const val MAX_FLAG_BYTES = 64 * 1024

internal class CircleFlagFetcher(
    private val context: Context,
    private val path: String,
) : Fetcher {
    @WorkerThread
    override suspend fun fetch(): Result<FetchResult> = runCatching {
        val bytes =
            readFlag(path) ?: readFlag(UNKNOWN_FLAG_PATH)
                ?: error("Bundled fallback flag is missing")
        FetchResult(ByteArrayDataSource(bytes, DataFrom.LOCAL), "image/svg+xml")
    }

    private fun readFlag(path: String): ByteArray? {
        val archive = File(context.cacheDir, "circle-flags/$CIRCLE_FLAG_CACHE_FILE")
        if (!archive.isFile) {
            synchronized(CircleFlagFetcher::class.java) {
                if (!archive.isFile) {
                    archive.parentFile?.mkdirs()
                    val temporary = File.createTempFile("country-flags", ".zip", archive.parentFile)
                    try {
                        context.assets.open(CIRCLE_FLAG_ARCHIVE).use { input ->
                            temporary.outputStream().buffered().use(input::copyTo)
                        }
                        if (!temporary.renameTo(archive)) {
                            temporary.delete()
                            check(archive.isFile) { "Unable to initialize bundled flags" }
                        }
                    } finally {
                        temporary.delete()
                    }
                }
            }
        }
        ZipFile(archive).use { zip ->
            val entry = zip.getEntry(path) ?: return null
            require(entry.size in 1..MAX_FLAG_BYTES) { "Invalid bundled flag size" }
            return zip.getInputStream(entry).buffered().use { input ->
                val output = ByteArrayOutputStream(entry.size.toInt())
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    require(output.size() + read <= MAX_FLAG_BYTES) {
                        "Invalid bundled flag data"
                    }
                    output.write(buffer, 0, read)
                }
                output.toByteArray().also { bytes ->
                    require(bytes.isNotEmpty()) { "Invalid bundled flag data" }
                }
            }
        }
    }

    internal object Factory : Fetcher.Factory {
        override val sortWeight: Int = 0

        override fun create(requestContext: RequestContext): Fetcher? {
            val request = requestContext.request
            val uri = request.uri
            if (!uri.scheme.equals(CIRCLE_FLAG_SCHEME, ignoreCase = true)) return null
            val path = uri.path?.removePrefix("/")?.removePrefix("flags/")
            if (path.isNullOrEmpty() || path.contains("..") || path.contains('\\')) return null
            return CircleFlagFetcher(request.context, path)
        }

        override fun equals(other: Any?): Boolean = other === this

        override fun hashCode(): Int = Factory::class.hashCode()

        override fun toString(): String = "CircleFlagFetcher.Factory"
    }

    companion object {
        val components: ComponentRegistry = ComponentRegistry { add(Factory) }
    }
}
