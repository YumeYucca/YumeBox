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

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.github.yumeyucca.yumebox.common.util.toast
import com.github.yumeyucca.yumebox.presentation.util.isYamlConfigFileName
import com.github.yumeyucca.yumebox.presentation.util.readDisplayName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import timber.log.Timber

/** Picks a local YAML config file; the add sheet and the 编辑配置 page both use it. */
@Composable
internal fun rememberProfileConfigFilePicker(
    context: Context,
    onFileSelected: (Uri, String) -> Unit,
    onUnsupportedFile: () -> Unit,
): () -> Unit {
    val file =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri ?: return@rememberLauncherForActivityResult
            val fileName = readDisplayName(context, uri, YumeTxt.ProfilesPage.Message.UnknownFile)
            if (!isYamlConfigFileName(fileName)) {
                onUnsupportedFile()
                return@rememberLauncherForActivityResult
            }
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
                .onFailure { error ->
                    Timber.w(error, "Persist profile source permission failed: %s", uri)
                }
            onFileSelected(uri, fileName)
        }
    return { file.launch(arrayOf("*/*")) }
}

/** Picks a QR image and decodes its payload off the main thread. */
@Composable
internal fun rememberQrImagePicker(context: Context, onQrDecoded: (String) -> Unit): () -> Unit {
    val scope = rememberCoroutineScope()
    val qrImage =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri ?: return@rememberLauncherForActivityResult
            scope.launch {
                @Suppress("TooGenericExceptionCaught")
                try {
                    val value = readQrFromImage(context, uri)
                    if (value == null) {
                        context.toast(YumeTxt.ProfilesPage.QrScanner.RecognizeFailed)
                    } else {
                        onQrDecoded(value)
                        context.toast(YumeTxt.ProfilesPage.QrScanner.RecognizeSuccess)
                    }
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    context.toast(
                        YumeTxt.ProfilesPage.QrScanner.RecognizeError.format(error.message ?: "")
                    )
                }
            }
        }
    return { qrImage.launch("image/*") }
}
