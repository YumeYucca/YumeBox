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

@Composable
internal fun ProfileAddSheetContent(
    actions: ProfileAddSheetActions,
    show: Boolean,
    phase: ProfileSheetPhase,
    selectedImportType: ProfileImportType,
    form: ProfileAddFormState,
    hasCameraPermission: Boolean,
    showCameraPreview: Boolean,
    downloadProgress: DownloadProgress?,
    stableHeightPx: Int,
    downloadHeight: Dp,
    downloadCompleteHeight: Dp,
) {
    val density = LocalDensity.current
    val stableHeight: Dp? =
        remember(stableHeightPx, density) {
            if (stableHeightPx <= 0) null else with(density) { stableHeightPx.toDp() }
        }

    // Read inside the title-bar action lambdas via state (not captured parameters): the
    // overlay-hosted action row can be re-rendered from a stale composable after the app returns
    // from background, and only a state read there keeps it subscribed to updates.
    val currentPhase by rememberUpdatedState(phase)
    val importType by rememberUpdatedState(selectedImportType)

    AppActionBottomSheet(
        show = show,
        title = YumeTxt.ProfilesPage.Sheet.AddTitle,
        startAction = {
            if (currentPhase == ProfileSheetPhase.Editing) {
                AppBottomSheetCloseAction(onClick = actions.dismiss)
            }
        },
        endAction = {
            // Visibility only: submitProfile owns the in-flight guard.
            if (currentPhase == ProfileSheetPhase.Editing && importType != ProfileImportType.Qr) {
                AppBottomSheetConfirmAction(onClick = actions.submit)
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
                targetState = phase.showsProgress,
                transitionSpec = {
                    if (targetState) {
                        (
                            slideInHorizontally(
                                animationSpec = tween(260),
                                initialOffsetX = { it },
                            ) + fadeIn()
                        ) togetherWith
                            (
                                slideOutHorizontally(
                                    animationSpec = tween(220),
                                    targetOffsetX = { -it / 3 },
                                ) + fadeOut()
                            )
                    } else {
                        (
                            slideInHorizontally(
                                animationSpec = tween(220),
                                initialOffsetX = { -it / 3 },
                            ) + fadeIn()
                        ) togetherWith
                            (
                                slideOutHorizontally(
                                    animationSpec = tween(260),
                                    targetOffsetX = { it },
                                ) + fadeOut()
                            )
                    }
                },
                label = "ProfileImportContentSwitch",
            ) { showsProgress ->
                if (showsProgress) {
                    DownloadProgressContent(
                        downloadProgress = downloadProgress,
                        stableSheetHeight = stableHeight,
                        downloadSheetContentHeight = downloadHeight,
                        downloadCompleteSheetContentHeight = downloadCompleteHeight,
                    )
                } else {
                    ProfileFormContent(
                        selectedImportType = selectedImportType,
                        form = form,
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
