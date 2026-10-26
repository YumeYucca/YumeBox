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

import android.app.Application
import android.content.Intent
import com.github.yumeyucca.yumebox.BuildInfo
import com.github.yumeyucca.yumebox.core.util.moeWallpaperFile
import com.github.yumeyucca.yumebox.runtime.api.Intents
import com.github.yumeyucca.yumebox.runtime.client.ProxyFacade
import com.github.yumeyucca.yumebox.runtime.service.util.importedDir
import com.github.yumeyucca.yumebox.substore.SubStorePaths
import com.github.yumeyucca.yumebox.substore.SubStoreServiceController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class BackupRepository internal constructor(
    private val application: Application,
    private val proxyFacade: ProxyFacade,
    private val storeAdapter: BackupStoreAdapter,
    private val archiveManager: BackupArchiveManager = BackupArchiveManager(),
) {
    suspend fun exportBackup(output: OutputStream) =
        withContext(Dispatchers.IO) {
            writeCurrentBackup(output)
        }

    internal suspend fun restoreBackup(input: InputStream, strategy: BackupRestoreStrategy) =
        withContext(Dispatchers.IO) {
            val extractDir = freshCacheDir("backup-restore")
            val rollbackFile = application.cacheDir.resolve("backup-restore-rollback.zip")
            var rollbackDir: File? = null

            try {
                val extracted = archiveManager.readArchive(input, extractDir)
                require(isCompatibleBackupAppId(extracted.manifest.appId, application.packageName)) {
                    "Backup belongs to ${extracted.manifest.appId}"
                }

                val currentPayload =
                    if (strategy == BackupRestoreStrategy.Merge) storeAdapter.collectPayload()
                    else null
                rollbackFile.outputStream().use(::writeCurrentBackup)
                try {
                    stopRuntimeBeforeRestore()
                    applyBackup(extracted, strategy, currentPayload)
                    notifyRestored()
                } catch (error: Exception) {
                    runCatching {
                        val stagingDir = freshCacheDir("backup-rollback")
                        rollbackDir = stagingDir
                        rollbackFile.inputStream().use { input ->
                            archiveManager
                                .readArchive(input, stagingDir)
                                .also { rollback -> applyBackup(rollback) }
                        }
                    }
                    throw error
                }
            } finally {
                rollbackFile.delete()
                extractDir.deleteRecursively()
                rollbackDir?.deleteRecursively()
            }
        }

    fun defaultBackupFileName(now: Long = System.currentTimeMillis()): String {
        val timestamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date(now))
        return "YumeBox-backup-$timestamp.zip"
    }

    private fun writeCurrentBackup(output: OutputStream) {
        archiveManager.writeArchive(
            output = output,
            manifest =
                BackupManifest(
                    appId = application.packageName,
                    appVersion = BuildInfo.VERSION_NAME,
                    createdAt = System.currentTimeMillis(),
                    plaintext = true,
                    includes =
                        listOf(
                            "settings",
                            "network_settings",
                            "feature_settings",
                            "proxy_display",
                            "profile_links",
                            "remote_controller",
                            "profiles",
                            "active_profile",
                            "overrides",
                            "substore_data",
                            "moe_wallpaper",
                        ),
                ),
            payload = storeAdapter.collectPayload(),
            files = currentFiles(),
        )
    }

    private fun applyBackup(
        extracted: BackupArchiveManager.ExtractedBackup,
        strategy: BackupRestoreStrategy = BackupRestoreStrategy.Overwrite,
        currentPayload: BackupPayload? = null,
    ) {
        val wallpaperFile = extracted.moeWallpaperFile.takeIf(File::isFile)
        val payload =
            when (strategy) {
                BackupRestoreStrategy.Overwrite -> extracted.payload
                BackupRestoreStrategy.Merge ->
                    mergeBackupPayload(
                        local = requireNotNull(currentPayload),
                        backup = extracted.payload,
                    )
            }
        val localWallpaperFile = application.moeWallpaperFile().takeIf(File::isFile)
        val restoredWallpaperFile =
            if (strategy == BackupRestoreStrategy.Merge) localWallpaperFile ?: wallpaperFile
            else wallpaperFile
        val appBackup =
            if (restoredWallpaperFile != null) {
                payload.appSettings.copy(
                    moeWallpaperUri =
                        "file://${application.moeWallpaperFile().absolutePath}"
                )
            } else {
                payload.appSettings
            }

        storeAdapter.clearAndApply(payload.copy(appSettings = appBackup))

        when (strategy) {
            BackupRestoreStrategy.Overwrite -> {
                replaceBackupDirectory(extracted.importedDir, application.importedDir)
                replaceBackupDirectory(
                    extracted.overridesDir,
                    application.filesDir.resolve("overrides"),
                )
                replaceBackupDirectory(extracted.subStoreDataDir, SubStorePaths.dataDir)
            }
            BackupRestoreStrategy.Merge -> {
                mergeBackupDirectory(extracted.importedDir, application.importedDir)
                mergeBackupDirectory(extracted.overridesDir, application.filesDir.resolve("overrides"))
                mergeBackupDirectory(extracted.subStoreDataDir, SubStorePaths.dataDir)
            }
        }

        if (
            wallpaperFile != null &&
                (strategy == BackupRestoreStrategy.Overwrite || localWallpaperFile == null)
        ) {
            application.moeWallpaperFile().also { target ->
                target.parentFile?.mkdirs()
                wallpaperFile.copyTo(target, overwrite = true)
            }
        } else if (strategy == BackupRestoreStrategy.Overwrite && wallpaperFile == null) {
            application.moeWallpaperFile().delete()
        }
    }

    private suspend fun stopRuntimeBeforeRestore() {
        runCatching { proxyFacade.stopProxy() }
        runCatching { SubStoreServiceController.stopService(application) }
    }

    private suspend fun notifyRestored() {
        application.sendBroadcast(
            Intent(Intents.actionProfileChanged(application.packageName))
                .setPackage(application.packageName)
        )
        application.sendBroadcast(
            Intent(Intents.actionOverrideChanged(application.packageName))
                .setPackage(application.packageName)
        )
        runCatching { proxyFacade.reconcileRuntimeState() }
    }

    private fun currentFiles(): BackupArchiveManager.BackupFiles =
        BackupArchiveManager.BackupFiles(
            importedDir = application.importedDir,
            overridesDir = application.filesDir.resolve("overrides"),
            subStoreDataDir = SubStorePaths.dataDir,
            moeWallpaperFile = application.moeWallpaperFile(),
        )

    private fun freshCacheDir(name: String): File =
        application.cacheDir.resolve(name).apply {
            deleteRecursively()
            mkdirs()
        }
}

internal fun isCompatibleBackupAppId(backupAppId: String, currentAppId: String): Boolean =
    backupAppId.isBlank() ||
            backupAppId == currentAppId ||
            backupAppId in LEGACY_BACKUP_APP_IDS

private val LEGACY_BACKUP_APP_IDS = setOf("com.github.yumelira.yumebox")

internal fun replaceBackupDirectory(source: File, target: File) {
    target.deleteRecursively()
    if (!source.exists()) return
    source.copyToBackupTarget(target)
}

internal fun mergeBackupDirectory(source: File, target: File) {
    if (!source.exists()) return
    source.mergeIntoBackupTarget(target)
}

private fun File.copyToBackupTarget(target: File) {
    if (isDirectory) {
        target.mkdirs()
        listFiles().orEmpty().forEach { file ->
            file.copyToBackupTarget(target.resolve(file.name))
        }
    } else {
        target.parentFile?.mkdirs()
        copyTo(target, overwrite = true)
    }
}

private fun File.mergeIntoBackupTarget(target: File) {
    if (isDirectory) {
        if (target.exists() && !target.isDirectory) return
        target.mkdirs()
        listFiles().orEmpty().forEach { file ->
            file.mergeIntoBackupTarget(target.resolve(file.name))
        }
    } else if (!target.exists()) {
        target.parentFile?.mkdirs()
        copyTo(target)
    }
}
