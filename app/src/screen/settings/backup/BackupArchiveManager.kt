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

package com.github.yumeyucca.yumebox.screen.settings.backup

import com.github.yumeyucca.yumebox.core.util.PrettyPersistedJson
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupArchiveManager(
    private val json: Json = PrettyPersistedJson,
) {
    data class BackupFiles(
        val importedDir: File,
        val overridesDir: File,
        val subStoreDataDir: File,
        val moeWallpaperFile: File,
    )

    data class ExtractedBackup(
        val rootDir: File,
        val manifest: BackupManifest,
        val payload: BackupPayload,
    ) {
        val importedDir: File
            get() = rootDir.resolve(FILES_DIR).resolve(IMPORTED_DIR)

        val overridesDir: File
            get() = rootDir.resolve(FILES_DIR).resolve(OVERRIDES_DIR)

        val subStoreDataDir: File
            get() = rootDir.resolve(FILES_DIR).resolve(SUBSTORE_DATA_DIR)

        val moeWallpaperFile: File
            get() = rootDir.resolve(FILES_DIR).resolve(MOE_WALLPAPER_DIR).resolve(WALLPAPER_FILE)
    }

    fun writeArchive(
        output: OutputStream,
        manifest: BackupManifest,
        payload: BackupPayload,
        files: BackupFiles,
    ) {
        ZipOutputStream(output.buffered()).use { zip ->
            zip.writeText(MANIFEST_FILE, json.encodeToString(BackupManifest.serializer(), manifest))
            zip.writeText(PAYLOAD_FILE, json.encodeToString(BackupPayload.serializer(), payload))
            zip.writeDirectory(files.importedDir, "$FILES_DIR/$IMPORTED_DIR")
            zip.writeDirectory(files.overridesDir, "$FILES_DIR/$OVERRIDES_DIR")
            zip.writeDirectory(files.subStoreDataDir, "$FILES_DIR/$SUBSTORE_DATA_DIR")
            if (files.moeWallpaperFile.isFile) {
                zip.writeFile(
                    file = files.moeWallpaperFile,
                    entryName = "$FILES_DIR/$MOE_WALLPAPER_DIR/$WALLPAPER_FILE",
                )
            }
        }
    }

    fun readArchive(input: InputStream, targetDir: File): ExtractedBackup {
        targetDir.deleteRecursively()
        targetDir.mkdirs()

        val canonicalRoot = targetDir.canonicalFile
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val target = canonicalRoot.resolve(entry.name).canonicalFile
                require(
                    target.path == canonicalRoot.path ||
                            target.path.startsWith(canonicalRoot.path + File.separator)
                ) {
                    "Backup contains an unsafe path: ${entry.name}"
                }

                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()
                    target.outputStream().use { output -> zip.copyTo(output) }
                }
                zip.closeEntry()
            }
        }

        val manifestFile = targetDir.resolve(MANIFEST_FILE)
        val payloadFile = targetDir.resolve(PAYLOAD_FILE)
        require(manifestFile.isFile) { "Backup manifest is missing" }
        require(payloadFile.isFile) { "Backup payload is missing" }

        val manifest = json.decodeFromString(BackupManifest.serializer(), manifestFile.readText())
        require(manifest.formatVersion <= BACKUP_FORMAT_VERSION) {
            "Backup format ${manifest.formatVersion} is newer than this app supports"
        }

        val payload = json.decodeFromString(BackupPayload.serializer(), payloadFile.readText())
        return ExtractedBackup(rootDir = targetDir, manifest = manifest, payload = payload)
    }

    private fun ZipOutputStream.writeText(entryName: String, text: String) {
        putNextEntry(ZipEntry(entryName))
        write(text.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun ZipOutputStream.writeDirectory(source: File, entryPrefix: String) {
        if (!source.exists()) return
        source.walkTopDown().forEach { file ->
            if (file == source) return@forEach
            val relative = file.relativeTo(source).invariantSeparatorsPath
            val entryName = "$entryPrefix/$relative"
            if (file.isDirectory) {
                putNextEntry(ZipEntry("$entryName/"))
                closeEntry()
            } else {
                writeFile(file, entryName)
            }
        }
    }

    private fun ZipOutputStream.writeFile(file: File, entryName: String) {
        putNextEntry(ZipEntry(entryName))
        file.inputStream().use { input -> input.copyTo(this) }
        closeEntry()
    }

    companion object {
        private const val MANIFEST_FILE = "manifest.json"
        private const val PAYLOAD_FILE = "payload.json"
        private const val FILES_DIR = "files"
        private const val IMPORTED_DIR = "imported"
        private const val OVERRIDES_DIR = "overrides"
        private const val SUBSTORE_DATA_DIR = "substore-data"
        private const val MOE_WALLPAPER_DIR = "moe-wallpaper"
        private const val WALLPAPER_FILE = "wallpaper.dat"
    }
}
