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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import com.github.yumeyucca.yumebox.presentation.component.AgeSecretKeyField
import com.github.yumeyucca.yumebox.presentation.component.AppCard
import com.github.yumeyucca.yumebox.presentation.component.OemTextField
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.PackageCheck
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.presentation.util.ProfileImportType
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.preference.WindowSpinnerPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun DownloadProgressContent(
    downloadProgress: DownloadProgress?,
    stableSheetHeightPx: Int,
    stableSheetHeight: Dp,
    downloadSheetContentHeight: Dp,
    downloadCompleteSheetContentHeight: Dp,
) {
    val isCompleted = downloadProgress?.isCompleted == true
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(
                    if (isCompleted) {
                        downloadCompleteSheetContentHeight
                    } else if (stableSheetHeightPx > 0) {
                        stableSheetHeight
                    } else {
                        downloadSheetContentHeight
                    }
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(UiDp.dp16, Alignment.CenterVertically),
    ) {
        AnimatedContent(
            targetState = isCompleted,
            modifier = Modifier.size(UiDp.dp48),
            contentAlignment = Alignment.Center,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            },
            label = "ProgressIcon",
        ) { complete ->
            if (complete) {
                Icon(
                    imageVector = Yume.PackageCheck,
                    contentDescription = YumeTxt.ProfilesPage.Sheet.Complete,
                    tint = MiuixTheme.colorScheme.onPrimary,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(UiDp.dp16))
                            .background(MiuixTheme.colorScheme.primary)
                            .padding(UiDp.dp10),
                )
            } else {
                InfiniteProgressIndicator(modifier = Modifier.size(UiDp.dp32))
            }
        }

        downloadProgress?.message?.let { message ->
            Text(
                text = message,
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        downloadProgress?.itemProgress?.let { itemProgress ->
            Text(
                text = itemProgress,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
internal fun ProfileFormContent(
    selectedImportType: ProfileImportType,
    profileLocked: Boolean,
    nameTextFieldValue: TextFieldValue,
    urlTextFieldValue: TextFieldValue,
    fileNameTextFieldValue: TextFieldValue,
    ageSecretKeyTextFieldValue: TextFieldValue,
    error: String,
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
        ProfileTypeSelectorCard(
            selectedImportType = selectedImportType,
            profileLocked = profileLocked,
            onTypeSelected = onTypeSelected,
        )

        Crossfade(
            targetState = selectedImportType,
            animationSpec = tween(200),
            label = "ProfileTypeContent",
        ) { importType ->
            when (importType) {
                ProfileImportType.Qr ->
                    QrScannerContent(
                        hasCameraPermission = hasCameraPermission,
                        showCameraPreview = showCameraPreview,
                        onSelectQrImage = onSelectQrImage,
                        onQrScanned = onQrScanned,
                    )

                ProfileImportType.Url,
                ProfileImportType.LocalFile ->
                    ManualProfileContent(
                        importType = importType,
                        profileLocked = profileLocked,
                        nameTextFieldValue = nameTextFieldValue,
                        urlTextFieldValue = urlTextFieldValue,
                        fileNameTextFieldValue = fileNameTextFieldValue,
                        ageSecretKeyTextFieldValue = ageSecretKeyTextFieldValue,
                        error = error,
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
private fun ProfileTypeSelectorCard(
    selectedImportType: ProfileImportType,
    profileLocked: Boolean,
    onTypeSelected: (ProfileImportType) -> Unit,
) {
    AppCard(applyHorizontalPadding = false) {
        Box(
            modifier =
                Modifier
                    .alpha(if (profileLocked) 0.5f else 1f)
                    .clickable(
                        enabled = profileLocked,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = {},
                    )
        ) {
            WindowSpinnerPreference(
                title = YumeTxt.ProfilesPage.Type.Title,
                items = ProfileImportType.entries.map { type -> DropdownItem(title = type.spinnerTitle()) },
                selectedIndex = selectedImportType.ordinal,
                onSelectedIndexChange = { index ->
                    onTypeSelected(ProfileImportType.fromSpinnerIndex(index))
                },
            )
        }
    }
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
    profileLocked: Boolean,
    nameTextFieldValue: TextFieldValue,
    urlTextFieldValue: TextFieldValue,
    fileNameTextFieldValue: TextFieldValue,
    ageSecretKeyTextFieldValue: TextFieldValue,
    error: String,
    onNameChange: (TextFieldValue) -> Unit,
    onUrlChange: (TextFieldValue) -> Unit,
    onAgeSecretKeyChange: (TextFieldValue) -> Unit,
    onPickFile: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(UiDp.dp16),
    ) {
        OemTextField(
            value = nameTextFieldValue,
            onValueChange = onNameChange,
            label = YumeTxt.ProfilesPage.Input.ProfileName,
            useLabelAsPlaceholder = true,
            modifier = Modifier.fillMaxWidth(),
        )

        if (importType == ProfileImportType.Url) {
            OemTextField(
                value = urlTextFieldValue,
                onValueChange = onUrlChange,
                label = YumeTxt.ProfilesPage.Input.SubscriptionUrl,
                useLabelAsPlaceholder = true,
                maxLines = 2,
                readOnly = profileLocked,
                enabled = !profileLocked,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Box(modifier = Modifier.fillMaxWidth()) {
                OemTextField(
                    value = fileNameTextFieldValue,
                    onValueChange = {},
                    label = YumeTxt.ProfilesPage.Input.SelectFile,
                    useLabelAsPlaceholder = true,
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                )
                Box(
                    modifier =
                        Modifier
                            .matchParentSize()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = onPickFile,
                            ),
                )
            }
        }

        AgeSecretKeyField(
            value = ageSecretKeyTextFieldValue,
            onValueChange = onAgeSecretKeyChange,
            label = YumeTxt.ProfilesPage.Input.AgeSecretKey,
            modifier = Modifier.fillMaxWidth(),
        )
        if (error.isNotEmpty()) {
            Text(
                text = error,
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
