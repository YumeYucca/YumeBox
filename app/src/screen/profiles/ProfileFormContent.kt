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

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntSize
import com.github.yumeyucca.yumebox.presentation.component.AppCard
import com.github.yumeyucca.yumebox.presentation.component.GlassDropdownOption
import com.github.yumeyucca.yumebox.presentation.component.GlassDropdownPreference
import io.github.yumeyucca.lucide.Lucide
import io.github.yumeyucca.lucide.lucide.FileText
import io.github.yumeyucca.lucide.lucide.Link
import io.github.yumeyucca.lucide.lucide.QrCode
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.presentation.util.ProfileImportType
import com.github.yumeyucca.yumebox.runtime.api.Profile
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun ProfileFormContent(
    selectedImportType: ProfileImportType,
    form: ProfileAddFormState,
    hasCameraPermission: Boolean,
    showCameraPreview: Boolean,
    onContainerMeasured: (IntSize) -> Unit,
    onTypeSelected: (ProfileImportType) -> Unit,
    onNameChange: (TextFieldValue) -> Unit,
    onUrlChange: (TextFieldValue) -> Unit,
    onAgeSecretKeyChange: (TextFieldValue) -> Unit,
    onPickFile: () -> Unit,
    onSelectQrImage: () -> Unit,
    onQrScanned: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged(onContainerMeasured),
        verticalArrangement = Arrangement.spacedBy(UiDp.dp16),
    ) {
        Crossfade(
            targetState = selectedImportType,
            animationSpec = tween(200),
            label = "ProfileTypeContent",
        ) { importType ->
            when (importType) {
                ProfileImportType.Qr ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(UiDp.dp16),
                    ) {
                        AppCard(applyHorizontalPadding = false) {
                            ProfileTypeSelector(
                                selectedImportType = selectedImportType,
                                onTypeSelected = onTypeSelected,
                            )
                        }
                        QrScannerContent(
                            hasCameraPermission = hasCameraPermission,
                            showCameraPreview = showCameraPreview,
                            onSelectQrImage = onSelectQrImage,
                            onQrScanned = onQrScanned,
                        )
                    }

                ProfileImportType.Url,
                ProfileImportType.LocalFile ->
                    ManualProfileContent(
                        importType = importType,
                        form = form,
                        onTypeSelected = onTypeSelected,
                        onNameChange = onNameChange,
                        onUrlChange = onUrlChange,
                        onAgeSecretKeyChange = onAgeSecretKeyChange,
                        onPickFile = onPickFile,
                    )
            }
        }
    }
}

@Composable
private fun ProfileTypeSelector(
    selectedImportType: ProfileImportType,
    onTypeSelected: (ProfileImportType) -> Unit,
) {
    val options = remember { profileImportTypeOptions() }
    GlassDropdownPreference(
        title = YumeTxt.ProfilesPage.Type.Title,
        options = options,
        selected = selectedImportType,
        onSelected = onTypeSelected,
    )
}

@Composable
private fun QrScannerContent(
    hasCameraPermission: Boolean,
    showCameraPreview: Boolean,
    onSelectQrImage: () -> Unit,
    onQrScanned: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(UiDp.dp16),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(UiDp.dp200)
                    .clip(RoundedCornerShape(UiDp.dp12))
                    .background(MiuixTheme.colorScheme.surfaceVariant),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (showCameraPreview) {
                key("qr_scanner_stable") { StableQrScanner(onScanned = onQrScanned) }
            } else if (!hasCameraPermission) {
                Text(YumeTxt.ProfilesPage.QrScanner.NeedPermission)
            } else {
                CircularProgressIndicator(modifier = Modifier.size(UiDp.dp32))
            }
        }

        TextButton(
            text = YumeTxt.ProfilesPage.QrScanner.SelectFromAlbum,
            onClick = onSelectQrImage,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ManualProfileContent(
    importType: ProfileImportType,
    form: ProfileAddFormState,
    onTypeSelected: (ProfileImportType) -> Unit,
    onNameChange: (TextFieldValue) -> Unit,
    onUrlChange: (TextFieldValue) -> Unit,
    onAgeSecretKeyChange: (TextFieldValue) -> Unit,
    onPickFile: () -> Unit,
) {
    // The age key is a secret: it types as dots until the row's own eye is tapped.
    var ageSecretVisible by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(UiDp.dp16),
    ) {
        AppCard(applyHorizontalPadding = false) {
            Column {
                ProfileTypeSelector(
                    selectedImportType = importType,
                    onTypeSelected = onTypeSelected,
                )
                ProfileNameField(
                    value = form.name.value,
                    onValueChange = onNameChange,
                    hint = YumeTxt.ProfilesPage.Input.Optional,
                )
            }
        }

        AppCard(applyHorizontalPadding = false) {
            Column {
                ProfileSourceField(
                    sourceType = importType.sourceProfileType(),
                    urlValue = form.url.value,
                    onUrlChange = onUrlChange,
                    fileSizeText = profileFileSizeText(form.fileSize.value),
                    onPickFile = onPickFile,
                    hint = YumeTxt.ProfilesPage.Input.Required,
                )

                ProfileAgeSecretField(
                    value = form.ageSecretKey.value,
                    onValueChange = onAgeSecretKeyChange,
                    visible = ageSecretVisible,
                    onVisibilityChange = { ageSecretVisible = it },
                    hint = YumeTxt.ProfilesPage.Input.Optional,
                )
            }
        }

        if (form.error.value.isNotEmpty()) {
            Text(
                text = form.error.value,
                color = MiuixTheme.colorScheme.error,
                style = MiuixTheme.textStyles.body2,
            )
        }
    }
}

private fun ProfileImportType.spinnerTitle(): String =
    when (this) {
        ProfileImportType.Url -> YumeTxt.ProfilesPage.Type.Subscription
        ProfileImportType.LocalFile -> YumeTxt.ProfilesPage.Type.LocalFile
        ProfileImportType.Qr -> YumeTxt.ProfilesPage.Type.QrScan
    }

private fun ProfileImportType.spinnerIcon(): ImageVector =
    when (this) {
        ProfileImportType.Url -> Lucide.Link
        ProfileImportType.LocalFile -> Lucide.FileText
        ProfileImportType.Qr -> Lucide.QrCode
    }

// The QR scanner yields a subscription URL, so only the two manual types differ here.
private fun ProfileImportType.sourceProfileType(): Profile.Type =
    when (this) {
        ProfileImportType.Url, ProfileImportType.Qr -> Profile.Type.Url
        ProfileImportType.LocalFile -> Profile.Type.File
    }

private fun profileImportTypeOptions(): List<GlassDropdownOption<ProfileImportType>> =
    ProfileImportType.entries.map { type ->
        GlassDropdownOption(
            value = type,
            label = type.spinnerTitle(),
            icon = type.spinnerIcon(),
        )
    }
