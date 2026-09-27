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

@file:Suppress("FunctionName", "SortModifiers")

package com.github.yumeyucca.yumebox.screen.profiles

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import com.github.yumeyucca.yumebox.presentation.component.AppActionBottomSheet
import com.github.yumeyucca.yumebox.presentation.component.AppBottomSheetCloseAction
import com.github.yumeyucca.yumebox.presentation.component.AppBottomSheetConfirmAction
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.presentation.util.ProfileImportType
import com.github.yumeyucca.yumebox.core.locale.YumeTxt

internal class ProfileAddSheetActions(
    val dismiss: () -> Unit,
    val submit: () -> Unit,
    val selectType: (ProfileImportType) -> Unit,
    val changeName: (TextFieldValue) -> Unit,
    val changeUrl: (TextFieldValue) -> Unit,
    val changeAgeSecretKey: (TextFieldValue) -> Unit,
    val updateHeight: (Int) -> Unit,
    val pickFile: () -> Unit,
    val selectQrImage: () -> Unit,
    val qrScanned: (String) -> Unit,
)

context(actions: ProfileAddSheetActions)
@Composable
internal fun ProfileAddSheetContent(
    show: Boolean,
    isEditing: Boolean,
    isDownloading: Boolean,
    selectedImportType: ProfileImportType,
    nameValue: TextFieldValue,
    urlValue: TextFieldValue,
    fileNameValue: TextFieldValue,
    ageSecretKeyValue: TextFieldValue,
    error: String,
    hasCameraPermission: Boolean,
    showCameraPreview: Boolean,
    downloadProgress: DownloadProgress?,
    stableHeightPx: Int,
    downloadHeight: Dp,
    downloadCompleteHeight: Dp,
) {
    val density = LocalDensity.current
    val stableHeight =
        remember(stableHeightPx, density) {
            if (stableHeightPx <= 0) UiDp.dp0 else with(density) { stableHeightPx.toDp() }
        }

    // Read inside the title-bar action lambdas via state (not captured parameters): the
    // overlay-hosted action row can be re-rendered from a stale composable after the app returns
    // from background, and only a state read there keeps it subscribed to updates.
    val downloading by rememberUpdatedState(isDownloading)
    val importType by rememberUpdatedState(selectedImportType)

    AppActionBottomSheet(
        show = show,
        title =
            if (isEditing) YumeTxt.ProfilesPage.Sheet.EditTitle
            else YumeTxt.ProfilesPage.Sheet.AddTitle,
        startAction = {
            if (!downloading) {
                AppBottomSheetCloseAction(
                    contentDescription = YumeTxt.Component.Button.Cancel,
                    onClick = actions.dismiss,
                )
            }
        },
        endAction = {
            if (!downloading && importType != ProfileImportType.Qr) {
                AppBottomSheetConfirmAction(
                    contentDescription = YumeTxt.Component.Button.Confirm,
                    onClick = actions.submit,
                )
            }
        },
        onDismissRequest = actions.dismiss,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .animateContentSize(animationSpec = tween(300, easing = FastOutSlowInEasing))
                    .padding(bottom = UiDp.dp16)
        ) {
            AnimatedContent(
                targetState = isDownloading,
                transitionSpec = {
                    if (targetState) {
                        (slideInHorizontally(animationSpec = tween(260), initialOffsetX = { it }) +
                                fadeIn()) togetherWith
                                (slideOutHorizontally(
                                    animationSpec = tween(220),
                                    targetOffsetX = { -it / 3 },
                                ) + fadeOut())
                    } else {
                        (slideInHorizontally(
                            animationSpec = tween(220),
                            initialOffsetX = { -it / 3 },
                        ) + fadeIn()) togetherWith
                                (slideOutHorizontally(
                                    animationSpec = tween(260),
                                    targetOffsetX = { it },
                                ) + fadeOut())
                    }
                },
                label = "ProfileImportContentSwitch",
            ) { downloading ->
                if (downloading) {
                    DownloadProgressContent(
                        downloadProgress = downloadProgress,
                        stableSheetHeightPx = stableHeightPx,
                        stableSheetHeight = stableHeight,
                        downloadSheetContentHeight = downloadHeight,
                        downloadCompleteSheetContentHeight = downloadCompleteHeight,
                    )
                } else {
                    ProfileFormContent(
                        selectedImportType = selectedImportType,
                        profileLocked = isEditing,
                        nameTextFieldValue = nameValue,
                        urlTextFieldValue = urlValue,
                        fileNameTextFieldValue = fileNameValue,
                        ageSecretKeyTextFieldValue = ageSecretKeyValue,
                        error = error,
                        hasCameraPermission = hasCameraPermission,
                        showCameraPreview = showCameraPreview,
                        onContainerMeasured = { actions.updateHeight(it.height) },
                        onTypeSelected = actions.selectType,
                        onNameChange = actions.changeName,
                        onUrlChange = actions.changeUrl,
                        onAgeSecretKeyChange = actions.changeAgeSecretKey,
                        onPickFile = actions.pickFile,
                        onSelectQrImage = actions.selectQrImage,
                        onQrScanned = actions.qrScanned,
                    )
                }
            }
        }
    }
}
