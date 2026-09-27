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


import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.github.yumeyucca.yumebox.common.util.toast
import com.github.yumeyucca.yumebox.core.model.OverrideInternalConstants
import com.github.yumeyucca.yumebox.data.model.OverrideConfig
import com.github.yumeyucca.yumebox.data.model.ProfileBinding
import com.github.yumeyucca.yumebox.presentation.component.AgeSecretKeyField
import com.github.yumeyucca.yumebox.presentation.component.AppActionBottomSheet
import com.github.yumeyucca.yumebox.presentation.component.AppCard
import com.github.yumeyucca.yumebox.presentation.component.AppBottomSheetCloseAction
import com.github.yumeyucca.yumebox.presentation.component.AppBottomSheetConfirmAction
import com.github.yumeyucca.yumebox.presentation.component.OemTextField
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.runtime.api.Profile
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.preference.WindowSpinnerPreference

private enum class ProfileSettingsSection {
    Subscription,
    Override,
}

@Composable
internal fun ProfileSettingsDialog(
    show: Boolean,
    profile: Profile,
    builtInConfigs: List<OverrideConfig>,
    userConfigs: List<OverrideConfig>,
    binding: ProfileBinding?,
    onDismiss: () -> Unit,
    onDismissFinished: () -> Unit,
    onSaveProfileMeta: (ProfileMetaUpdate) -> Unit,
    onSaveOverrideSettings: suspend (List<String>) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val spacing = AppTheme.spacing
    val componentSizes = AppTheme.sizes
    val selectableConfigs = remember(builtInConfigs, userConfigs) { builtInConfigs + userConfigs }
    val sectionOptions = remember {
        listOf(
            ProfileSettingsSection.Subscription,
            ProfileSettingsSection.Override,
        )
    }

    var selectedSection by remember { mutableStateOf(ProfileSettingsSection.Subscription) }
    var editName by remember {
        mutableStateOf(TextFieldValue(profile.name, TextRange(profile.name.length)))
    }
    var editSource by remember { mutableStateOf(TextFieldValue()) }
    var editAgeSecretKey by remember { mutableStateOf(TextFieldValue()) }
    var ageSecretKeyEdited by remember { mutableStateOf(false) }
    var customRoutingSelected by remember { mutableStateOf(false) }
    var pendingSelectedOverrideIds by remember { mutableStateOf(emptyList<String>()) }
    // True once override selection has been seeded from the binding (or from the user editing it).
    // The binding loads ASYNCHRONOUSLY after the dialog is interactive, so a late binding arrival
    // must not clobber a toggle the user already changed.
    var overrideSelectionInitialized by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    val customRoutingId = OverrideInternalConstants.CUSTOM_ROUTING_OVERRIDE_ID
    val selectableById = remember(selectableConfigs) { selectableConfigs.associateBy { it.id } }
    val visibleSelectedOverrideIds =
        remember(pendingSelectedOverrideIds, selectableById) {
            pendingSelectedOverrideIds.filter(selectableById::containsKey)
        }
    val overrideRowIds =
        remember(selectableConfigs, pendingSelectedOverrideIds, visibleSelectedOverrideIds) {
            val selectedIds = pendingSelectedOverrideIds.toSet()
            visibleSelectedOverrideIds +
                    selectableConfigs
                        .asSequence()
                        .map(OverrideConfig::id)
                        .filterNot(selectedIds::contains)
                        .toList()
        }
    val currentVisibleSelectedOverrideIds by rememberUpdatedState(visibleSelectedOverrideIds)
    val selectedOverrideListState = rememberLazyListState()
    val selectedOverrideReorderState =
        rememberReorderableLazyListState(selectedOverrideListState) { from, to ->
            pendingSelectedOverrideIds =
                reorderVisibleOverrideIds(
                    allIds = pendingSelectedOverrideIds,
                    visibleIds = currentVisibleSelectedOverrideIds,
                    from = from.index,
                    to = to.index,
                )
        }

    // Reset per dialog-open identity only (NOT on every binding change), so re-firing when the
    // async binding loads cannot wipe edits already in progress.
    LaunchedEffect(show, profile.uuid) {
        if (show) {
            selectedSection = ProfileSettingsSection.Subscription
            editName = TextFieldValue(profile.name, TextRange(profile.name.length))
            editSource = TextFieldValue()
            editAgeSecretKey = TextFieldValue()
            ageSecretKeyEdited = false
            overrideSelectionInitialized = false
            customRoutingSelected = false
            pendingSelectedOverrideIds = emptyList()
        }
    }

    // Seed override selection from the binding exactly once it becomes available, and only while
    // the user has not yet edited it. Preserves correct initial state when the binding loads before
    // any user interaction; ignores the late load otherwise.
    LaunchedEffect(show, profile.uuid, binding) {
        if (show && !overrideSelectionInitialized && binding != null) {
            val overrideIds = binding.overrideIds
            customRoutingSelected =
                overrideIds.contains(OverrideInternalConstants.CUSTOM_ROUTING_OVERRIDE_ID)
            pendingSelectedOverrideIds = overrideIds
            overrideSelectionInitialized = true
        }
    }

    val toggleUserOverrideSelection: (String) -> Unit = { overrideId ->
        overrideSelectionInitialized = true
        pendingSelectedOverrideIds =
            if (overrideId in pendingSelectedOverrideIds) {
                pendingSelectedOverrideIds - overrideId
            } else {
                (pendingSelectedOverrideIds + overrideId).distinct()
            }
    }
    val saveSettings = {
        if (!isSaving) {
            scope.launch {
                isSaving = true
                runCatching {
                    val trimmedName = editName.text.trim()
                    val trimmedSource = editSource.text.trim()
                    val trimmedAgeSecretKey = editAgeSecretKey.text.trim()
                    val targetSource =
                        if (profile.type == Profile.Type.Url && trimmedSource.isNotEmpty()) {
                            trimmedSource
                        } else {
                            profile.source
                        }
                    val hasMetaChanges =
                        trimmedName != profile.name ||
                                targetSource != profile.source ||
                                ageSecretKeyEdited
                    if (trimmedName.isNotEmpty() && targetSource.isNotEmpty() && hasMetaChanges) {
                        onSaveProfileMeta(
                            ProfileMetaUpdate(
                                name = trimmedName,
                                source = targetSource,
                                updateAgeSecretKey = ageSecretKeyEdited,
                                ageSecretKey =
                                    if (ageSecretKeyEdited) trimmedAgeSecretKey else null,
                            )
                        )
                    }

                    val finalSelectedOverrideIds =
                        buildFinalOverrideIds(
                            selectedOverrideIds = pendingSelectedOverrideIds,
                            customRoutingSelected = customRoutingSelected,
                        )
                    onSaveOverrideSettings(finalSelectedOverrideIds)
                }
                    .onSuccess { onDismiss() }
                    .onFailure { error ->
                        context.toast(error.message ?: YumeTxt.Util.Error.UnknownError)
                    }
                isSaving = false
            }
        }
    }

    AppActionBottomSheet(
        show = show,
        modifier = Modifier,
        title = YumeTxt.ProfilesPage.SettingsDialog.Title,
        startAction = {
            AppBottomSheetCloseAction(
                onClick = onDismiss,
                contentDescription = YumeTxt.ProfilesPage.Button.Cancel,
            )
        },
        endAction = {
            AppBottomSheetConfirmAction(
                enabled = !isSaving,
                onClick = saveSettings,
                contentDescription = YumeTxt.ProfilesPage.Button.Confirm,
            )
        },
        onDismissRequest = onDismiss,
        onDismissFinished = onDismissFinished,
        enableNestedScroll = true,
    ) {
        // Match Add Profile sheet: content-sized + animate height on section switch.
        // Do NOT force a min/max fraction of the screen — that freezes height and kills animation.
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .animateContentSize(animationSpec = tween(300, easing = FastOutSlowInEasing))
                    .padding(bottom = UiDp.dp16),
            verticalArrangement = Arrangement.spacedBy(spacing.space16),
        ) {
            AppCard(applyHorizontalPadding = false) {
                WindowSpinnerPreference(
                    title = YumeTxt.ProfilesPage.SettingsDialog.SectionType,
                    items =
                        listOf(
                            DropdownItem(
                                title = YumeTxt.ProfilesPage.SettingsDialog.SectionSubscription
                            ),
                            DropdownItem(
                                title = YumeTxt.ProfilesPage.SettingsDialog.SectionOverride
                            ),
                        ),
                    selectedIndex = sectionOptions.indexOf(selectedSection).coerceAtLeast(0),
                    onSelectedIndexChange = { index ->
                        sectionOptions.getOrNull(index)?.let { selectedSection = it }
                    },
                )
            }

            Crossfade(
                targetState = selectedSection,
                animationSpec = tween(200),
                label = "ProfileSettingsSection",
            ) { section ->
                when (section) {
                    ProfileSettingsSection.Subscription -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(spacing.space16),
                        ) {
                            OemTextField(
                                value = editName,
                                onValueChange = { editName = it },
                                label = YumeTxt.ProfilesPage.Input.ProfileName,
                                useLabelAsPlaceholder = true,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            if (profile.type == Profile.Type.Url) {
                                OemTextField(
                                    value = editSource,
                                    onValueChange = { editSource = it },
                                    label = YumeTxt.ProfilesPage.SettingsDialog.ChangeLink,
                                    useLabelAsPlaceholder = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 2,
                                )
                            }

                            AgeSecretKeyField(
                                value = editAgeSecretKey,
                                onValueChange = {
                                    editAgeSecretKey = it
                                    ageSecretKeyEdited = true
                                },
                                label = YumeTxt.ProfilesPage.SettingsDialog.AgeSecretKey,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    ProfileSettingsSection.Override -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(spacing.space16),
                        ) {
                            AppCard(applyHorizontalPadding = false) {
                                Column {
                                    SwitchPreference(
                                        title = YumeTxt.ProfilesPage.SettingsDialog.CustomRouting,
                                        summary =
                                            YumeTxt.ProfilesPage.SettingsDialog
                                                .CustomRoutingSummary,
                                        checked = customRoutingSelected,
                                        onCheckedChange = {
                                            overrideSelectionInitialized = true
                                            customRoutingSelected = it
                                            pendingSelectedOverrideIds =
                                                if (it) {
                                                    (pendingSelectedOverrideIds + customRoutingId)
                                                        .distinct()
                                                } else {
                                                    pendingSelectedOverrideIds - customRoutingId
                                                }
                                        },
                                    )
                                }
                            }

                            if (overrideRowIds.isNotEmpty()) {
                                AppCard(applyHorizontalPadding = false) {
                                    LazyColumn(
                                        state = selectedOverrideListState,
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .heightIn(
                                                    max =
                                                        componentSizes
                                                            .profileSettingsListMaxHeight
                                                ),
                                    ) {
                                        items(
                                            overrideRowIds,
                                            key = { id -> "override-$id" },
                                        ) { id ->
                                            val config = selectableById[id] ?: return@items
                                            val isSelected = id in pendingSelectedOverrideIds
                                            Box(
                                                modifier =
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .animateItem(
                                                            fadeInSpec = tween(160),
                                                            fadeOutSpec = tween(120),
                                                            placementSpec =
                                                                tween(
                                                                    220,
                                                                    easing = FastOutSlowInEasing,
                                                                ),
                                                        )
                                            ) {
                                                if (isSelected) {
                                                    ReorderableItem(
                                                        selectedOverrideReorderState,
                                                        key = "override-$id",
                                                    ) { isDragging ->
                                                        BasicComponent(
                                                            title = config.name,
                                                            modifier =
                                                                Modifier
                                                                    .longPressDraggableHandle()
                                                                    .alpha(
                                                                        if (isDragging) 0.9f else 1f
                                                                    ),
                                                            endActions = {
                                                                Checkbox(
                                                                    state = ToggleableState.On,
                                                                    onClick = {
                                                                        overrideSelectionInitialized =
                                                                            true
                                                                        pendingSelectedOverrideIds =
                                                                            pendingSelectedOverrideIds -
                                                                                    id
                                                                    },
                                                                )
                                                            },
                                                            onClick = {},
                                                        )
                                                    }
                                                } else {
                                                    BasicComponent(
                                                        title = config.name,
                                                        endActions = {
                                                            Checkbox(
                                                                state = ToggleableState.Off,
                                                                onClick = {
                                                                    toggleUserOverrideSelection(id)
                                                                },
                                                            )
                                                        },
                                                        onClick = {
                                                            toggleUserOverrideSelection(id)
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
            }
        }
    }
}

private fun buildFinalOverrideIds(
    selectedOverrideIds: List<String>,
    customRoutingSelected: Boolean,
): List<String> {
    val customRoutingId = OverrideInternalConstants.CUSTOM_ROUTING_OVERRIDE_ID
    val normalizedIds = selectedOverrideIds.distinct()
    if (!customRoutingSelected) {
        return normalizedIds - customRoutingId
    }
    if (customRoutingId in normalizedIds) {
        return normalizedIds
    }
    return normalizedIds + customRoutingId
}

private fun reorderVisibleOverrideIds(
    allIds: List<String>,
    visibleIds: List<String>,
    from: Int,
    to: Int,
): List<String> {
    if (from !in visibleIds.indices || to !in visibleIds.indices || from == to) return allIds
    val reorderedVisible = visibleIds.toMutableList()
    reorderedVisible.add(to, reorderedVisible.removeAt(from))
    val visibleSet = visibleIds.toSet()
    val replacements = reorderedVisible.iterator()
    return allIds.map { id -> if (id in visibleSet) replacements.next() else id }
}

internal data class ProfileMetaUpdate(
    val name: String,
    val source: String,
    val updateAgeSecretKey: Boolean,
    val ageSecretKey: String?,
)
