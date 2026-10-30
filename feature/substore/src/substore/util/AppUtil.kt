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

import com.github.yumeyucca.yumebox.core.Global
import com.github.yumeyucca.yumebox.substore.SubStorePaths
import timber.log.Timber
import java.io.File

object AppUtil {
    fun initFirstOpen() {
        SubStorePaths.ensureStructure()
        createRootJson()
        guarded("Extract backend bundle failed") {
            copyAsset("backend/sub-store.bundle.js", SubStorePaths.backendBundle)
        }
        extractFrontendDist()
    }

    private fun createRootJson() {
        guarded("Create root.json failed") {
            val rootJsonFile = File(SubStorePaths.dataDir, "root.json")
            rootJsonFile.parentFile?.mkdirs()
            if (!rootJsonFile.exists()) rootJsonFile.writeText("{}")
        }
    }

    private fun extractFrontendDist() {
        guarded("Extract frontend assets failed") {
            val cacheDir = Global.application.cacheDir

            val zipPath = File(cacheDir, "substore_frontend.zip")
            copyAsset("frontend/dist.zip", zipPath)

            val stagingDir =
                File(cacheDir, "substore_frontend_stage").apply {
                    if (exists()) deleteRecursively()
                    mkdirs()
                }

            val unzipSuccess = ArchiveUtil.unzipZip(zipPath, stagingDir)
            if (!unzipSuccess) {
                throw IllegalStateException("Failed to extract the bundled Sub-Store frontend")
            }

            val extractedRoot = File(stagingDir, "dist").takeIf { it.exists() } ?: stagingDir
            val targetDir = SubStorePaths.frontendDir
            targetDir.parentFile?.mkdirs()
            if (targetDir.exists()) {
                targetDir.deleteRecursively()
            }
            extractedRoot.copyRecursively(targetDir, overwrite = true)

            stagingDir.deleteRecursively()
            zipPath.delete()
        }
    }

    private fun copyAsset(assetPath: String, targetFile: File) {
        targetFile.parentFile?.mkdirs()
        Global.application.assets.open(assetPath).use { input ->
            targetFile.outputStream().use { output -> input.copyTo(output) }
        }
    }

    /** First-open asset installation is best effort: a failure only costs a log line. */
    private inline fun guarded(message: String, block: () -> Unit) {
        runCatching(block).onFailure { error -> Timber.e(error, message) }
    }
}
