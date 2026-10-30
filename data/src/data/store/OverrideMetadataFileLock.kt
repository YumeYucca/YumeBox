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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c)  YumeYucca 2025 - Present
 *
 */

package com.github.yumeyucca.yumebox.data.store

import com.github.yumeyucca.yumebox.core.util.YamlCodec
import com.github.yumeyucca.yumebox.data.model.MetadataIndex
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Shared monitor for the two stores that read and rewrite overrides/metadata.yaml. */
internal object OverrideMetadataFileLock {
    val monitor = Any()
}

/**
 * Decode outcome for metadata.yaml.
 *
 * Corrupt must never be replaced by an empty index write — that permanently wipes overrides.
 */
internal sealed class MetadataIndexLoad {
    data class Ok(val index: MetadataIndex) : MetadataIndexLoad()

    /** File missing or zero-length — safe to treat as empty and allow first write. */
    data object Missing : MetadataIndexLoad()

    data class Corrupt(val cause: Throwable) : MetadataIndexLoad()
}

internal object OverrideMetadataIO {
    fun load(file: File): MetadataIndexLoad {
        if (!file.exists() || file.length() == 0L) {
            return MetadataIndexLoad.Missing
        }
        return runCatching {
            MetadataIndexLoad.Ok(
                YamlCodec.decode(MetadataIndex.serializer(), file.readText())
            )
        }
            .getOrElse { error ->
                Timber.w(error, "Failed to decode override metadata: %s", file.absolutePath)
                MetadataIndexLoad.Corrupt(error)
            }
    }

    /**
     * Index for mutation. Corrupt non-empty file throws so callers cannot save an empty wipe.
     */
    fun loadForMutation(file: File): MetadataIndex =
        when (val loaded = load(file)) {
            is MetadataIndexLoad.Ok -> loaded.index
            MetadataIndexLoad.Missing -> MetadataIndex()
            is MetadataIndexLoad.Corrupt ->
                throw IOException(
                    "Refuse to mutate corrupt override metadata: ${file.absolutePath}",
                    loaded.cause,
                )
        }

    fun save(file: File, index: MetadataIndex) {
        val parent = file.parentFile ?: error("metadata file has no parent: ${file.path}")
        parent.mkdirs()
        val encoded = YamlCodec.encode(MetadataIndex.serializer(), index)
        writeTextAtomic(file, encoded)
    }

    fun writeTextAtomic(file: File, text: String) {
        val parent = file.parentFile ?: error("target file has no parent: ${file.path}")
        check(parent.exists() || parent.mkdirs()) { "Failed to create directory: ${parent.path}" }
        val tmp = File.createTempFile("${file.name}.", ".tmp", parent)
        try {
            FileOutputStream(tmp).bufferedWriter().use { writer ->
                writer.write(text)
                writer.flush()
            }
            FileOutputStream(tmp, true).use { output -> output.fd.sync() }
            val tmpPath = tmp.toPath()
            val targetPath = file.toPath()
            try {
                Files.move(
                    tmpPath,
                    targetPath,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE,
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(tmpPath, targetPath, StandardCopyOption.REPLACE_EXISTING)
            }
        } catch (error: Exception) {
            runCatching { tmp.delete() }
            throw error
        }
    }
}
