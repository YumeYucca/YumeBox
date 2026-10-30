/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * YumeBox is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with YumeBox. If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) YumeYucca 2025 - Present
 */

package com.github.yumeyucca.yumebox.data.store

import android.content.Context
import com.github.yumeyucca.yumebox.data.model.BuiltInOverrideCatalog
import com.github.yumeyucca.yumebox.data.model.OverrideContentType
import timber.log.Timber
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/** Keeps materialized built-ins byte-for-byte aligned with the APK assets. */
class BuiltInOverrideFileStore(
    private val context: Context,
    private val overridesDir: File = File(context.filesDir, "overrides"),
) {
    private val configsDir = overridesDir.resolve("configs")

    fun sync(id: String): File? {
        val definition = BuiltInOverrideCatalog.find(id) ?: return null
        return synchronized(lock) {
            runCatching {
                val assetBytes = context.assets.open(definition.assetPath).use { it.readBytes() }
                val assetHash = sha256(assetBytes)
                val target =
                    configsDir.resolve("${definition.id}.${definition.contentType.extension}")
                OverrideContentType.knownExtensions
                    .filterNot { it == definition.contentType.extension }
                    .forEach { extension ->
                        configsDir.resolve("${definition.id}.$extension").delete()
                    }
                val currentHash =
                    target.takeIf(File::isFile)?.let { file ->
                        runCatching { sha256(file.readBytes()) }.getOrNull()
                    }
                if (currentHash == null || !currentHash.contentEquals(assetHash)) {
                    OverrideMetadataIO.writeTextAtomic(
                        target,
                        String(assetBytes, StandardCharsets.UTF_8),
                    )
                }
                target
            }
                .onFailure { error ->
                    Timber.w(error, "Failed to synchronize built-in override: %s", id)
                }
                .getOrNull()
        }
    }

    private companion object {
        private val lock = Any()

        private fun sha256(bytes: ByteArray): ByteArray =
            MessageDigest.getInstance("SHA-256").digest(bytes)
    }
}
