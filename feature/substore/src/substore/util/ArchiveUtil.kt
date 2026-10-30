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

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

object ArchiveUtil {
    fun unzipZip(zipFile: File, destination: File): Boolean =
        extractArchive(zipFile, destination) { input ->
            ZipInputStream(input).use { zip ->
                generateSequence { zip.nextEntry }.forEach { entry ->
                    handleEntry(destination, entry.name, entry.isDirectory) { writeEntry(zip, it) }
                }
            }
        }

    fun untar(tarFile: File, destination: File): Boolean =
        extractArchive(tarFile, destination) { input ->
            TarArchiveInputStream(input).use { tar ->
                generateSequence { tar.nextEntry }.forEach { entry ->
                    handleEntry(destination, entry.name, entry.isDirectory) { writeEntry(tar, it) }
                }
            }
        }

    fun untarGz(tarGzFile: File, destination: File): Boolean =
        extractArchive(tarGzFile, destination) { input ->
            GzipCompressorInputStream(input).use { gzip ->
                TarArchiveInputStream(gzip).use { tar ->
                    generateSequence { tar.nextEntry }.forEach { entry ->
                        handleEntry(destination, entry.name, entry.isDirectory) {
                            writeEntry(tar, it)
                        }
                    }
                }
            }
        }

    private inline fun extractArchive(
        archiveFile: File,
        destination: File,
        readEntries: (InputStream) -> Unit,
    ): Boolean {
        if (!archiveFile.isFile) return false

        return runCatching {
            prepareDestination(destination)
            FileInputStream(archiveFile).use(readEntries)
            true
        }
            .getOrDefault(false)
    }

    private inline fun handleEntry(
        destination: File,
        entryName: String,
        isDirectory: Boolean,
        write: (File) -> Unit,
    ) {
        val target = resolveEntryTarget(destination, entryName)
        if (isDirectory) {
            ensureDirectory(target)
        } else {
            write(target)
        }
    }

    private fun prepareDestination(destination: File) {
        if (!destination.exists()) {
            if (!destination.mkdirs()) {
                throw IllegalStateException("Cannot create directory: ${destination.absolutePath}")
            }
        }
        if (!destination.isDirectory) {
            throw IllegalStateException("Destination is not a directory: ${destination.absolutePath}")
        }
    }

    private fun resolveEntryTarget(destination: File, entryName: String): File {
        val canonicalDestination = destination.canonicalFile
        val canonicalTarget = File(canonicalDestination, entryName).canonicalFile
        if (!canonicalTarget.toPath().startsWith(canonicalDestination.toPath())) {
            throw SecurityException("Path traversal detected: $entryName")
        }
        return canonicalTarget
    }

    private fun ensureDirectory(directory: File) {
        if (directory.exists()) {
            if (!directory.isDirectory) {
                throw IllegalStateException("Path exists but is not a directory: ${directory.absolutePath}")
            }
        } else if (!directory.mkdirs()) {
            throw IllegalStateException("Cannot create directory: ${directory.absolutePath}")
        }
    }

    private fun writeEntry(input: InputStream, targetFile: File) {
        targetFile.parentFile?.let { ensureDirectory(it) }
        FileOutputStream(targetFile).use { output -> input.copyTo(output) }
    }
}
