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
 */

package com.github.yumeyucca.yumebox.substore.engine

import android.annotation.SuppressLint
import android.content.Context
import com.github.yumeyucca.yumebox.feature.substore.BuildConfig
import org.tukaani.xz.XZInputStream
import timber.log.Timber
import java.io.File
import java.security.MessageDigest

@SuppressLint("StaticFieldLeak")
object NativeLibraryManager {
    const val JAVET_LIBRARY_NAME = "libjavet-node-android"
    const val JAVET_LIBRARY_FILE_NAME = "libjavet.so"
    const val JAVET_ARCHIVE_FILE_NAME = "libjavet.so.xz"

    /**
     * Javet version the Java layer inside this APK was compiled against.
     *
     * The native library registers its JNI entry points for one exact Javet version and the V8 /
     * Node build behind it, so a native of any other version must never be handed to `System.load`:
     * the mismatch aborts the process instead of raising a catchable exception. Every check below
     * is therefore keyed on this value, never on "some version that used to be valid".
     */
    val JAVET_VERSION: String = BuildConfig.JAVET_VERSION

    /**
     * Release assets of the Javet native, in preference order.
     *
     * The versioned tag keeps every published native addressable forever, so shipping a new app
     * build never invalidates the asset an older build still points at. The legacy fixed tag is
     * kept last so installs that predate versioned assets keep working during the migration; the
     * installer still rejects whatever does not hash to [JAVET_VERSION].
     */
    val JAVET_ARCHIVE_URLS: List<String> =
        listOf(
            "https://github.com/YumeYucca/libjavet/releases/download/javet-$JAVET_VERSION/$JAVET_ARCHIVE_FILE_NAME",
            "https://github.com/YumeYucca/libjavet/releases/download/libjavet/$JAVET_ARCHIVE_FILE_NAME",
        )

    /**
     * Known-good digests per Javet version, append-only.
     *
     * Only the entry of [JAVET_VERSION] is ever accepted for loading; the older entries exist so a
     * native left on disk by a previous app build can be recognised by digest and reported as
     * "installed 5.0.9, needs 5.0.10" instead of as an anonymous mismatch.
     */
    private val LIBRARY_SHA256_BY_VERSION =
        mapOf(
            "5.0.9" to "126d1569d60c2f20605188001f236d91ba4c9cf7d35a8f02b57d69e47fe4b39d",
        )

    private val ARCHIVE_SHA256_BY_VERSION =
        mapOf(
            "5.0.9" to "31d7606fe3dd9135930a6a9519d90394abd219e535796cee3d5c8747812c6d35",
        )

    private const val LIBRARY_DIR_NAME = "lib"

    private var context: Context? = null

    fun initialize(context: Context) {
        if (this.context != null) return
        this.context = context.applicationContext
        libraryDir?.mkdirs()
    }

    fun getLibraryFile(name: String): File? {
        if (context == null) return null
        return when (name) {
            JAVET_LIBRARY_NAME -> File(requireNotNull(libraryDir), JAVET_LIBRARY_FILE_NAME)
            else -> null
        }
    }

    fun getDownloadTempFile(name: String): File? =
        getLibraryFile(name)?.let { library ->
            File(library.parentFile, "$JAVET_ARCHIVE_FILE_NAME.download")
        }

    fun installDownloadedArchive(
        name: String,
        downloadedArchive: File,
    ): Boolean {
        val targetFile = getLibraryFile(name) ?: return false
        if (!isArchiveFileValid(name, downloadedArchive)) {
            Timber.e("Native library archive hash mismatch: $name (expected Javet $JAVET_VERSION)")
            return false
        }

        val expandedFile = File(targetFile.parentFile, "${targetFile.name}.expanded")
        val installed =
            runCatching {
                if (expandedFile.exists() && !expandedFile.delete()) {
                    error("Unable to clear expanded native library: ${expandedFile.absolutePath}")
                }
                XZInputStream(downloadedArchive.inputStream().buffered()).use { input ->
                    expandedFile.outputStream().buffered().use { output -> input.copyTo(output) }
                }
                check(isLibraryFileValid(name, expandedFile)) {
                    "Expanded native library hash mismatch: $name (expected Javet $JAVET_VERSION)"
                }
                replaceLibrary(targetFile, expandedFile)
            }.getOrElse { error ->
                Timber.e(error, "Native library installation failed: $name")
                false
            }
        downloadedArchive.delete()
        if (!installed) expandedFile.delete()
        return installed
    }

    private fun replaceLibrary(targetFile: File, replacementFile: File): Boolean =
        runCatching {
            targetFile.parentFile?.mkdirs()
            val backupFile = File(targetFile.parentFile, "${targetFile.name}.previous")
            if (backupFile.exists() && !backupFile.delete()) {
                error("Unable to clear native library backup: ${backupFile.absolutePath}")
            }
            val hasPreviousLibrary = targetFile.exists()
            if (hasPreviousLibrary && !targetFile.renameTo(backupFile)) {
                error("Unable to back up native library: ${targetFile.absolutePath}")
            }
            if (!replacementFile.renameTo(targetFile)) {
                if (hasPreviousLibrary) backupFile.renameTo(targetFile)
                error("Unable to install native library: ${targetFile.absolutePath}")
            }
            backupFile.delete()
            targetFile.setReadable(true, false)
            true
        }.getOrElse { error ->
            Timber.e(error, "Native library replacement failed: ${targetFile.name}")
            false
        }

    /**
     * Whether the native on disk is exactly the build this APK can load. A native of another Javet
     * version is reported as unavailable on purpose, so the UI offers a download instead of letting
     * it reach `System.load`.
     */
    fun isLibraryAvailable(name: String): Boolean = identifyInstalledVersion(name) == JAVET_VERSION

    @SuppressLint("UnsafeDynamicallyLoadedCode")
    fun loadJniLibrary(name: String): Boolean {
        // Last line of defence: a mismatched native is not an exception, it is a process abort.
        if (!isLibraryAvailable(name)) {
            Timber.e("Refusing to load native library $name: ${getLibraryStatus(name)}")
            return false
        }

        val path = getLibraryFile(name)?.absolutePath ?: return false

        return runCatching {
            System.load(path)
            true
        }.getOrElse { error ->
            Timber.e(error, "JNI load failed: $name")
            false
        }
    }

    fun getLibraryStatus(name: String): String {
        val libraryFile = getLibraryFile(name) ?: return "Library manager not initialized"
        return when {
            !libraryFile.exists() -> "Library file not found: ${libraryFile.absolutePath}"
            !libraryFile.canRead() -> "Library file is not readable: ${libraryFile.absolutePath}"
            else ->
                when (val installedVersion = identifyInstalledVersion(name)) {
                    null -> "Unknown native library build: ${libraryFile.absolutePath}"
                    JAVET_VERSION ->
                        "Library ready: Javet $installedVersion at ${libraryFile.absolutePath}"
                    else ->
                        "Library version mismatch: installed $installedVersion, " +
                            "required $JAVET_VERSION (${libraryFile.absolutePath})"
                }
        }
    }

    private val libraryDir: File?
        get() = context?.filesDir?.resolve(LIBRARY_DIR_NAME)

    /**
     * Resolves the Javet version of the file on disk from its digest. The digest is authoritative
     * because it describes the exact bytes that would be mapped into the process; a file that
     * matches no known build resolves to null and is never loaded.
     */
    private fun identifyInstalledVersion(name: String): String? {
        val file = getLibraryFile(name) ?: return null
        if (!file.isFile || !file.canRead()) return null
        val digest = sha256(file) ?: return null
        return LIBRARY_SHA256_BY_VERSION.entries.firstOrNull { it.value == digest }?.key
    }

    private fun isLibraryFileValid(name: String, file: File): Boolean =
        file.isFile && file.canRead() && sha256(file) == expectedLibrarySha256(name)

    private fun isArchiveFileValid(name: String, file: File): Boolean =
        file.isFile && file.canRead() && sha256(file) == expectedArchiveSha256(name)

    private fun expectedLibrarySha256(name: String): String? =
        when (name) {
            JAVET_LIBRARY_NAME -> LIBRARY_SHA256_BY_VERSION[JAVET_VERSION]
            else -> null
        }

    private fun expectedArchiveSha256(name: String): String? =
        when (name) {
            JAVET_LIBRARY_NAME -> ARCHIVE_SHA256_BY_VERSION[JAVET_VERSION]
            else -> null
        }

    private fun sha256(file: File): String? =
        runCatching {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().buffered().use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
        }.getOrNull()
}