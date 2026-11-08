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

package com.github.yumeyucca.yumebox.screen.profiles

import android.app.Application
import android.net.Uri
import com.github.yumeyucca.yumebox.App
import com.github.yumeyucca.yumebox.runtime.api.Profile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.*

internal fun importedProfileDir(profile: Profile): File = App.instance.filesDir.importedProfileDir(profile.uuid)

internal fun importedConfigFile(profile: Profile): File = importedProfileDir(profile).resolve("config.yaml")

internal suspend fun Application.copyProfileImport(
    uri: Uri,
    uuid: UUID,
) {
    withContext(Dispatchers.IO) {
        val outputFile = filesDir.importedProfileDir(uuid).resolve("config.yaml")
        outputFile.parentFile?.mkdirs()
        contentResolver.openInputStream(uri)?.use { input ->
            outputFile.outputStream().use { output -> input.copyTo(output) }
        } ?: throw IllegalArgumentException("Failed to open file: $uri")
        Timber.d("File copied: ${outputFile.absolutePath}")
    }
}

private fun File.importedProfileDir(uuid: UUID): File = resolve("imported").resolve(uuid.toString())
