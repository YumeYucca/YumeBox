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

package com.github.yumeyucca.yumebox.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.github.yumeyucca.yumebox.common.util.toast
import com.github.yumeyucca.yumebox.data.model.OverrideConfig
import com.github.yumeyucca.yumebox.presentation.component.AppActionBottomSheet
import com.github.yumeyucca.yumebox.presentation.component.AppCard
import com.github.yumeyucca.yumebox.presentation.component.AppBottomSheetCloseAction
import com.github.yumeyucca.yumebox.presentation.component.AppBottomSheetConfirmAction
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.presentation.viewmodel.OverrideConfigViewModel
import com.github.yumeyucca.yumebox.runtime.api.Profile
import kotlinx.coroutines.launch
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.CheckboxLocation
import top.yukonga.miuix.kmp.preference.CheckboxPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

private sealed interface ApplyUi {
    data object Loading : ApplyUi

    data class Ready(val profiles: List<Profile>, val selected: Set<String>) : ApplyUi
}

/**
 * Single signal: [target] non-null opens the sheet and binds that config; null dismisses.
 * Content is not cleared while [target] is null so the exit animation keeps its last frame.
 */
@Composable
internal fun OverrideApplyToProfilesSheet(
    target: OverrideConfig?,
    viewModel: OverrideConfigViewModel,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val componentSizes = AppTheme.sizes

    var ui by remember { mutableStateOf<ApplyUi>(ApplyUi.Loading) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(target?.id) {
        val config = target ?: return@LaunchedEffect
        ui = ApplyUi.Loading
        isSaving = false
        viewModel
            .loadApplySnapshot(config.id)
            .onSuccess { snapshot ->
                ui = ApplyUi.Ready(snapshot.profiles, snapshot.selectedProfileIds)
            }
            .onFailure { error ->
                context.toast(
                    YumeTxt.Override.ApplySheet.Failed.format(
                        error.message ?: YumeTxt.Util.Error.UnknownError
                    )
                )
                ui = ApplyUi.Ready(emptyList(), emptySet())
            }
    }

    val saveSelection = {
        val config = target
        val selection = (ui as? ApplyUi.Ready)?.selected
        if (!isSaving && config != null && selection != null) {
            scope.launch {
                isSaving = true
                viewModel
                    .applyOverrideToProfiles(config.id, selection)
                    .onSuccess {
                        context.toast(YumeTxt.Override.ApplySheet.Success)
                        onDismiss()
                    }
                    .onFailure { error ->
                        context.toast(
                            YumeTxt.Override.ApplySheet.Failed.format(
                                error.message ?: YumeTxt.Util.Error.UnknownError
                            )
                        )
                    }
                isSaving = false
            }
        }
    }

    AppActionBottomSheet(
        show = target != null,
        title = YumeTxt.Override.ApplySheet.Title,
        startAction = {
            AppBottomSheetCloseAction(
                onClick = onDismiss,
                contentDescription = YumeTxt.Override.ApplySheet.Button.Cancel,
            )
        },
        endAction = {
            // Read snapshot state inside this lambda (not a captured value): the title-bar row is
            // hosted by the overlay and can be re-rendered from a stale composable after the app
            // returns from background — only a state read here keeps it subscribed to updates.
            AppBottomSheetConfirmAction(
                enabled = !isSaving && ui is ApplyUi.Ready,
                onClick = saveSelection,
                contentDescription = YumeTxt.Override.ApplySheet.Button.Confirm,
            )
        },
        onDismissRequest = onDismiss,
        enableNestedScroll = true,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(bottom = UiDp.dp16),
            verticalArrangement = Arrangement.spacedBy(UiDp.dp16),
        ) {
            when (val state = ui) {
                ApplyUi.Loading -> {
                    Spacer(modifier = Modifier.height(UiDp.dp24))
                }

                is ApplyUi.Ready -> {
                    if (state.profiles.isEmpty()) {
                        Text(
                            text = YumeTxt.Override.ApplySheet.Empty,
                            color = colorScheme.onSurfaceVariantSummary,
                        )
                    } else {
                        AppCard(applyHorizontalPadding = false) {
                            LazyColumn(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = componentSizes.profileSettingsListMaxHeight)
                            ) {
                                items(
                                    items = state.profiles,
                                    key = { profile -> profile.uuid.toString() },
                                ) { profile ->
                                    val profileId = profile.uuid.toString()
                                    val isSelected = profileId in state.selected
                                    CheckboxPreference(
                                        title = profile.name,
                                        checked = isSelected,
                                        checkboxLocation = CheckboxLocation.End,
                                        onCheckedChange = { checked ->
                                            val current = ui as? ApplyUi.Ready ?: return@CheckboxPreference
                                            ui =
                                                current.copy(
                                                    selected =
                                                        if (checked) {
                                                            current.selected + profileId
                                                        } else {
                                                            current.selected - profileId
                                                        }
                                                )
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
