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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import com.github.yumeyucca.yumebox.common.util.formatBytes
import com.github.yumeyucca.yumebox.presentation.component.AppCard
import com.github.yumeyucca.yumebox.presentation.component.GlassDropdownPreference
import com.github.yumeyucca.yumebox.presentation.component.AppTextFieldRow
import com.github.yumeyucca.yumebox.presentation.component.AppTextFieldRowIcon
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.Eye
import com.github.yumeyucca.yumebox.presentation.icon.yume.PackageCheck
import io.github.yumeyucca.lucide.Lucide
import io.github.yumeyucca.lucide.lucide.FileText
import io.github.yumeyucca.lucide.lucide.Link
import io.github.yumeyucca.lucide.lucide.QrCode
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.presentation.util.ProfileImportType
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.preference.ArrowPreference
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
    fileSize: Long,
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
                        ProfileTypeSelectorCard(
                            selectedImportType = selectedImportType,
                            profileLocked = profileLocked,
                            onTypeSelected = onTypeSelected,
                        )
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
                        profileLocked = profileLocked,
                        nameTextFieldValue = nameTextFieldValue,
                        urlTextFieldValue = urlTextFieldValue,
                        fileSize = fileSize,
                        ageSecretKeyTextFieldValue = ageSecretKeyTextFieldValue,
                        error = error,
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
private fun ProfileTypeSelectorCard(
    selectedImportType: ProfileImportType,
    profileLocked: Boolean,
    onTypeSelected: (ProfileImportType) -> Unit,
) {
    AppCard(applyHorizontalPadding = false) {
        GlassDropdownPreference(
            title = YumeTxt.ProfilesPage.Type.Title,
            items = ProfileImportType.entries.map { type -> type.spinnerTitle() },
            icons = listOf(Lucide.Link, Lucide.FileText, Lucide.QrCode),
            selectedIndex = selectedImportType.ordinal,
            enabled = !profileLocked,
            onSelectedIndexChange = { index ->
                onTypeSelected(ProfileImportType.fromSpinnerIndex(index))
            },
        )
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
    fileSize: Long,
    ageSecretKeyTextFieldValue: TextFieldValue,
    error: String,
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
                GlassDropdownPreference(
                    title = YumeTxt.ProfilesPage.Type.Title,
                    items = ProfileImportType.entries.map { type -> type.spinnerTitle() },
                    icons = listOf(Lucide.Link, Lucide.FileText, Lucide.QrCode),
                    selectedIndex = importType.ordinal,
                    enabled = !profileLocked,
                    onSelectedIndexChange = { index ->
                        onTypeSelected(ProfileImportType.fromSpinnerIndex(index))
                    },
                )
                AppTextFieldRow(
                    label = YumeTxt.ProfilesPage.Input.ProfileName,
                    value = nameTextFieldValue,
                    onValueChange = onNameChange,
                    enabled = !profileLocked,
                    readOnly = profileLocked,
                    hint = YumeTxt.ProfilesPage.Input.Optional,
                )
            }
        }

        AppCard(applyHorizontalPadding = false) {
            Column {
                if (importType == ProfileImportType.Url) {
                    AppTextFieldRow(
                        label = YumeTxt.ProfilesPage.Input.SubscriptionUrl,
                        value = urlTextFieldValue,
                        onValueChange = onUrlChange,
                        enabled = !profileLocked,
                        readOnly = profileLocked,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        hint = YumeTxt.ProfilesPage.Input.Required,
                    )
                } else {
                    ArrowPreference(
                        title = YumeTxt.ProfilesPage.Input.SelectFile,
                        enabled = !profileLocked,
                        endActions = {
                            if (fileSize > 0L) {
                                Text(
                                    text = formatBytes(fileSize),
                                    style = MiuixTheme.textStyles.body2,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                    maxLines = 1,
                                )
                            }
                        },
                        onClick = onPickFile,
                    )
                }

                AppTextFieldRow(
                    label = YumeTxt.ProfilesPage.Input.AgeSecretKey,
                    value = ageSecretKeyTextFieldValue,
                    onValueChange = onAgeSecretKeyChange,
                    enabled = !profileLocked,
                    readOnly = profileLocked,
                    hint = YumeTxt.ProfilesPage.Input.Optional,
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType =
                                if (ageSecretVisible) {
                                    KeyboardType.Text
                                } else {
                                    KeyboardType.Password
                                },
                        ),
                    trailingIcon = {
                        AppTextFieldRowIcon(
                            icon = Yume.Eye,
                            contentDescription = YumeTxt.ProfilesPage.Input.AgeSecretKey,
                            tint =
                                if (ageSecretVisible) {
                                    MiuixTheme.colorScheme.primary
                                } else {
                                    Color.Unspecified
                                },
                            onClick = { ageSecretVisible = !ageSecretVisible },
                        )
                    },
                )
            }
        }

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
