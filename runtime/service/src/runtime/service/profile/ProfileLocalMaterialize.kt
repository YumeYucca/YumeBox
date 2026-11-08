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
import android.net.Uri
import timber.log.Timber
import java.io.File

/**
 * Materialize a local profile's config.yaml into [stagingDir], matching the old native `FetchAndValid(force=true)` path
 * that re-opened `content://` / `file://` sources.
 *
 * Prefer the original source URI when still readable so a missing or partial copy from import cannot silently leave
 * staging without a config (and therefore without provider prefetch). Fall back to whatever is already staged.
 */
internal fun materializeLocalConfig(
    context: Context,
    stagingDir: File,
    imported: Imported,
) {
    val config = stagingDir.resolve("config.yaml")
    val temporary = stagingDir.resolve(".config.yaml.local")
    val source = imported.source.trim()
    if (source.isEmpty()) {
        if (!config.isFile || config.length() <= 0L) {
            Timber.w("Local profile %s has empty source and no staged config", imported.uuid)
        }
        return
    }

    val reloaded =
        runCatching {
            stagingDir.mkdirs()
            temporary.delete()
            when {
                source.startsWith("content:", ignoreCase = true) || source.startsWith("file:", ignoreCase = true) -> {
                    val uri = Uri.parse(source)
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        temporary.outputStream().use { output -> input.copyTo(output) }
                        true
                    } ?: false
                }

                File(source).isFile -> {
                    File(source).inputStream().use { input ->
                        temporary.outputStream().use { output -> input.copyTo(output) }
                    }
                    true
                }

                else -> {
                    false
                }
            }.also { copied ->
                if (copied) {
                    check(temporary.isFile && temporary.length() > 0L) { "Local profile source is empty: $source" }
                    temporary.copyTo(config, overwrite = true)
                }
            }
        }.onFailure { error -> Timber.w(error, "Failed to re-read local profile source: %s", source) }
            .getOrDefault(false)
    temporary.delete()

    if (!reloaded && (!config.isFile || config.length() <= 0L)) {
        Timber.w(
            "Local profile %s has no config.yaml after materialize (source=%s)",
            imported.uuid,
            source,
        )
    }
}
