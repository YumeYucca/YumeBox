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

package com.github.yumeyucca.yumebox.update

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import com.github.yumeyucca.yumebox.common.util.startActivitySafely
import com.github.yumeyucca.yumebox.data.update.AppUpdateRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Downloads the update package into the public `Download/YumeBox` folder through MediaStore and
 * hands the resulting content URI to the system package installer.
 *
 * The entry is removed again whenever the download fails or is cancelled, so a retry never
 * accumulates half-written APK files.
 */
internal object AppUpdateInstaller {
    private const val UPDATE_MIME_TYPE = "application/vnd.android.package-archive"
    private const val UPDATE_RELATIVE_PATH = "Download/YumeBox"

    /** Returns true when the installer was started; the caller owns progress reporting. */
    suspend fun downloadAndInstall(
        context: Context,
        repository: AppUpdateRepository,
        update: AppUpdateInfo,
        onProgress: (Float?) -> Unit,
    ): Boolean = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val displayName = update.fileName.substringAfterLast('/')
        deleteExistingEntry(resolver, displayName)
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, UPDATE_MIME_TYPE)
            put(MediaStore.MediaColumns.RELATIVE_PATH, UPDATE_RELATIVE_PATH)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        if (uri == null) {
            Timber.w("Failed to create the update download entry")
            return@withContext false
        }

        var completed = false
        try {
            val output = resolver.openOutputStream(uri)
            if (output == null) {
                Timber.w("Failed to open the update download stream")
                return@withContext false
            }
            output.use { stream ->
                repository.download(update.downloadUrl, stream, update.sizeBytes) { bytesRead, total ->
                    onProgress(
                        if (total > 0) {
                            (bytesRead.toDouble() / total).toFloat().coerceIn(0f, 1f)
                        } else {
                            null
                        }
                    )
                }
            }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            completed = true
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Timber.w(error, "Failed to download the app update")
            return@withContext false
        } finally {
            if (!completed) resolver.delete(uri, null, null)
        }
        launchInstaller(context, uri)
    }

    /** Avoids MediaStore's auto-renamed duplicates when the same version is downloaded twice. */
    private fun deleteExistingEntry(resolver: ContentResolver, displayName: String) {
        runCatching {
            resolver.delete(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                "${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
                arrayOf(displayName),
            )
        }
    }

    private fun launchInstaller(context: Context, uri: Uri): Boolean =
        runCatching {
            context.startActivitySafely(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, UPDATE_MIME_TYPE)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            )
        }.getOrDefault(false)
}
