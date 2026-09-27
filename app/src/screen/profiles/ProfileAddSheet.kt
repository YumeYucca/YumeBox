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
import tf.gal.yumebox.locale.YumeTxt
import java.util.*
import kotlin.math.max

@Composable
internal fun AddProfileSheet(
    show: MutableState<Boolean>,
    profileToEdit: Profile? = null,
    importUrl: String? = null,
    onAddProfile:
        (
        name: String,
        source: String,
        type: Profile.Type,
        interval: Long,
        fileUri: android.net.Uri?,
        ageSecretKey: String,
    ) -> Unit,
    onUpdateProfile: (uuid: UUID, name: String, source: String, interval: Long) -> Unit,
    onDownloadComplete: () -> Unit,
    profilesViewModel: ProfilesViewModel,
) {
    val configuration = LocalConfiguration.current
    val downloadSheetContentHeight = configuration.screenHeightDp.dp * 0.3f
    val downloadCompleteSheetContentHeight = configuration.screenHeightDp.dp * 0.42f
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val form = rememberProfileAddFormState()
    var selectedImportType by form.importType
    var nameTextFieldValue by form.name
    var urlTextFieldValue by form.url
    var filePath by form.filePath
    var fileNameTextFieldValue by form.fileName
    var ageSecretKeyTextFieldValue by form.ageSecretKey
    var error by form.error
    var isDownloading by form.isDownloading

    val downloadProgress by profilesViewModel.downloadProgress.collectAsState()
    val uiState by profilesViewModel.uiState.collectAsState()
    var hasShownCompleteAnimation by form.hasShownComplete
    var stableSheetHeightPx by form.stableHeightPx

    LaunchedEffect(show.value) {
        if (!show.value) {
            hasShownCompleteAnimation = false
            isDownloading = false
        }
    }

    val applyNameText: (String) -> Unit = { updatedText ->
        nameTextFieldValue = textValueAtEnd(updatedText)
    }
    val applyUrlText: (String) -> Unit = { updatedText ->
        urlTextFieldValue = textValueAtEnd(updatedText)
    }
    val applyFileNameText: (String) -> Unit = { updatedText ->
        fileNameTextFieldValue = textValueAtEnd(updatedText)
    }

    val clearAllState = form::reset
    val clearCurrentTypeState = form::clearTypeInput

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

    val showCameraPreview by
    remember(show.value, selectedImportType, isDownloading, hasCameraPermission) {
        derivedStateOf {
            show.value &&
                    selectedImportType == ProfileImportType.Qr &&
                    !isDownloading &&
                    hasCameraPermission
        }
    }

    DisposableEffect(show.value, profileToEdit, importUrl) {
        if (show.value) {
            clearAllState()
            if (profileToEdit != null) {
                applyNameText(profileToEdit.name)
                if (profileToEdit.type == Profile.Type.Url) {
                    selectedImportType = ProfileImportType.Url
                    applyUrlText(profileToEdit.source)
                } else {
                    selectedImportType = ProfileImportType.fromProfile(profileToEdit.type)
                    filePath = profileToEdit.source
                    applyFileNameText(sourceFileName(profileToEdit.source))
                }
            } else if (!importUrl.isNullOrBlank()) {
                selectedImportType = ProfileImportType.Url
                applyUrlText(importUrl)
            } else {
                selectedImportType = ProfileImportType.Url
                readClipboardSubscriptionUrl(context)?.let(applyUrlText)
            }
        }
        onDispose {}
    }
    LaunchedEffect(uiState.error) {
        val errorMessage = uiState.error
        if (errorMessage != null) {
            // Import failures only toast; do not dump the long core message into the sheet form.
            context.toast(errorMessage, Toast.LENGTH_LONG)
            if (isDownloading) {
                isDownloading = false
            }
            profilesViewModel.clearError()
        }
    }

    LaunchedEffect(downloadProgress?.isCompleted, isDownloading) {
        if (isDownloading && downloadProgress?.isCompleted == true && !hasShownCompleteAnimation) {
            hasShownCompleteAnimation = true
            onDownloadComplete()
        }
    }

    LaunchedEffect(uiState.message) {
        if (
            profileToEdit != null &&
            uiState.message != null &&
            isDownloading &&
            !hasShownCompleteAnimation
        ) {
            hasShownCompleteAnimation = true
            onDownloadComplete()
        }
        if (uiState.message != null) {
            profilesViewModel.clearMessage()
        }
    }

    val launchers =
        rememberProfileImportLaunchers(
            context = context,
            onFileSelected = { uri, fileName ->
                filePath = uri.toString()
                error = ""
                applyFileNameText(fileName)
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
            onQrDecoded = { url ->
                applyUrlText(url)
                selectedImportType = ProfileImportType.Url
            },
        )

    val dismissSheet = { dismissProfileAddSheet(show, isDownloading, profilesViewModel) }

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
                        profileToEdit = profileToEdit,
                        isDownloading = isDownloading,
                    )
                with(
                    ProfileSubmissionActions(
                        hideKeyboard = { keyboardController?.hide() },
                        clearError = profilesViewModel::clearError,
                        startDownload = {
                            hasShownCompleteAnimation = false
                            isDownloading = true
                        },
                        showError = { error = it },
                        addProfile = onAddProfile,
                        updateProfile = onUpdateProfile,
                    )
                ) {
                    submitProfile(draft)
                }
            },
            selectType = {
                selectedImportType = it
                clearCurrentTypeState()
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
            pickFile = launchers.pickFile,
            selectQrImage = launchers.selectQrImage,
            qrScanned = { url ->
                applyUrlText(url)
                selectedImportType = ProfileImportType.Url
            },
        )
    with(actions) {
        ProfileAddSheetContent(
            show = show.value,
            isEditing = profileToEdit != null,
            isDownloading = isDownloading,
            selectedImportType = selectedImportType,
            nameValue = nameTextFieldValue,
            urlValue = urlTextFieldValue,
            fileNameValue = fileNameTextFieldValue,
            ageSecretKeyValue = ageSecretKeyTextFieldValue,
            error = error,
            hasCameraPermission = hasCameraPermission,
            showCameraPreview = showCameraPreview,
            downloadProgress = downloadProgress,
            stableHeightPx = stableSheetHeightPx,
            downloadHeight = downloadSheetContentHeight,
            downloadCompleteHeight = downloadCompleteSheetContentHeight,
        )
    }
}
