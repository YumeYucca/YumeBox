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

@file:Suppress("FunctionName")

package com.github.yumeyucca.yumebox.screen.profiles

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.github.yumeyucca.yumebox.common.util.toast
import com.github.yumeyucca.yumebox.presentation.util.*
import com.github.yumeyucca.yumebox.runtime.api.Profile
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import java.util.*
import kotlin.math.max

private const val DownloadSheetHeightFraction = 0.3f
private const val DownloadCompleteSheetHeightFraction = 0.42f

@Composable
internal fun AddProfileSheet(
    show: Boolean,
    form: ProfileAddFormState,
    onDismiss: () -> Unit,
    importUrl: String? = null,
    onAddProfile:
        (
        name: String,
        source: String,
        type: Profile.Type,
        interval: Long,
        fileUri: Uri?,
        ageSecretKey: String,
    ) -> Unit,
    downloadProgress: DownloadProgress?,
    onDownloadComplete: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val downloadSheetContentHeight = configuration.screenHeightDp.dp * DownloadSheetHeightFraction
    val downloadCompleteSheetContentHeight =
        configuration.screenHeightDp.dp * DownloadCompleteSheetHeightFraction
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var selectedImportType by form.importType
    var nameTextFieldValue by form.name
    var urlTextFieldValue by form.url
    var filePath by form.filePath
    var fileSize by form.fileSize
    var ageSecretKeyTextFieldValue by form.ageSecretKey
    var error by form.error
    var stableSheetHeightPx by form.stableHeightPx

    // One in-flight signal: the view model raises download progress on submit and clears it when the
    // import fails or the sheet finishes, so the sheet never owns a second copy of that fact.
    val isDownloading = downloadProgress != null

    val phase =
        profileSheetPhase(
            isDownloading = isDownloading,
            downloadCompleted = downloadProgress?.isCompleted == true,
        )

    LaunchedEffect(phase) {
        if (phase == ProfileSheetPhase.Completed) {
            onDownloadComplete()
        }
    }

    val applyNameText: (String) -> Unit = { updatedText ->
        nameTextFieldValue = textValueAtEnd(updatedText)
    }
    val applyUrlText: (String) -> Unit = { updatedText ->
        urlTextFieldValue = textValueAtEnd(updatedText)
    }
    val applyScannedUrl: (String) -> Unit = { url ->
        applyUrlText(url)
        selectedImportType = ProfileImportType.Url
    }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted ->
            hasCameraPermission = isGranted
            if (!isGranted) {
                context.toast(YumeTxt.ProfilesPage.QrScanner.NeedCamera, Toast.LENGTH_LONG)
                selectedImportType = ProfileImportType.Url
            }
        }

    LaunchedEffect(selectedImportType) {
        if (selectedImportType == ProfileImportType.Qr && !hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val showCameraPreview =
        show &&
                selectedImportType == ProfileImportType.Qr &&
                !isDownloading &&
                hasCameraPermission

    LaunchedEffect(show, importUrl) {
        if (show) {
            form.reset()
            if (!importUrl.isNullOrBlank()) {
                selectedImportType = ProfileImportType.Url
                applyUrlText(importUrl)
            } else {
                selectedImportType = ProfileImportType.Url
                readClipboardSubscriptionUrl(context)?.let(applyUrlText)
            }
        }
    }
    val selectQrImage = rememberQrImagePicker(context = context, onQrDecoded = applyScannedUrl)
    val pickConfigFile =
        rememberProfileConfigFilePicker(
            context = context,
            onFileSelected = { uri, fileName ->
                filePath = uri.toString()
                error = ""
                fileSize = readDisplaySize(context, uri)
                if (nameTextFieldValue.text.isBlank() || nameTextFieldValue.text == fileName) {
                    applyNameText(
                        profileNameFromConfigFileName(
                            fileName,
                            YumeTxt.ProfilesPage.Input.NewProfile,
                        )
                    )
                }
            },
            onUnsupportedFile = { error = YumeTxt.ProfilesPage.Validation.YamlOnly },
        )

    val dismissSheet =
        {
            if (!isDownloading) onDismiss()
        }

    val actions =
        ProfileAddSheetActions(
            dismiss = dismissSheet,
            submit = {
                val draft =
                    ProfileDraft(
                        importType = selectedImportType,
                        name = nameTextFieldValue.text,
                        url = urlTextFieldValue.text,
                        filePath = filePath,
                        ageSecretKey = ageSecretKeyTextFieldValue.text,
                    )
                submitProfile(
                    actions =
                        ProfileSubmissionActions(
                            hideKeyboard = { keyboardController?.hide() },
                            clearError = { error = "" },
                            showError = { error = it },
                            addProfile = onAddProfile,
                        ),
                    draft = draft,
                )
            },
            selectType = {
                selectedImportType = it
                form.clearTypeInput()
            },
            changeName = { value ->
                nameTextFieldValue = value
                error = ""
            },
            changeUrl = { value ->
                urlTextFieldValue = value
                error = ""
            },
            changeAgeSecretKey = { ageSecretKeyTextFieldValue = it },
            updateHeight = { height -> stableSheetHeightPx = max(stableSheetHeightPx, height) },
            pickFile = pickConfigFile,
            selectQrImage = selectQrImage,
            qrScanned = applyScannedUrl,
        )
    ProfileAddSheetContent(
        actions = actions,
        show = show,
        phase = phase,
        selectedImportType = selectedImportType,
        form = form,
        hasCameraPermission = hasCameraPermission,
        showCameraPreview = showCameraPreview,
        downloadProgress = downloadProgress,
        stableHeightPx = stableSheetHeightPx,
        downloadHeight = downloadSheetContentHeight,
        downloadCompleteHeight = downloadCompleteSheetContentHeight,
    )
}
