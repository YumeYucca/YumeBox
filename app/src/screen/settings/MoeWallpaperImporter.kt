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

package com.github.yumeyucca.yumebox.screen.settings

import android.content.Context
import androidx.core.net.toUri
import com.github.yumeyucca.yumebox.core.util.moeWallpaperFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Copies the Moe wallpaper from an external `sourceUri` (typically a `content://` URI from the
 * photo picker) into the app-private files dir, so rendering survives deletion/permission loss of
 * the original source.
 *
 * The copy is written to a temp file then renamed over the stable slot, so a half-written copy
 * never replaces a good one (storage-failure edge case).
 *
 * @return a `file://` URI pointing at the local copy on success, or `null` if the source could not
 *   be read (caller should fall back to the original source URI in that case).
 */
object MoeWallpaperImporter {
    suspend fun importToLocal(context: Context, sourceUri: String): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val target = context.moeWallpaperFile()
                val tmp = File(target.parentFile, "wallpaper.tmp")
                context.contentResolver.openInputStream(sourceUri.toUri()).use { input ->
                    requireNotNull(input) { "Unable to open Moe wallpaper source: $sourceUri" }
                    tmp.outputStream().use { input.copyTo(it) }
                }
                if (!tmp.renameTo(target)) {
                    target.delete()
                    check(tmp.renameTo(target)) { "Failed to swap Moe wallpaper temp file" }
                }
                "file://${target.absolutePath}"
            }
                .onFailure { error ->
                    // Cancellation is not an import failure: reporting null here would make the
                    // caller keep the old wallpaper as if the copy had simply failed.
                    if (error is CancellationException) throw error
                }
                .getOrNull()
        }
}
