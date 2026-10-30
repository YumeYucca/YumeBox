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


import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import com.github.yumeyucca.yumebox.data.model.OverrideConfig
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.BadgePlus
import com.github.yumeyucca.yumebox.presentation.icon.yume.Check
import com.github.yumeyucca.yumebox.presentation.icon.yume.List
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.navigation.Route
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import org.koin.androidx.compose.koinViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.foundation.lazy.LazyItemScope

/**
 * Standalone "覆写" page for one profile: custom routing and the profile's override selection.
 *
 * The edit page no longer carries this state; overrides are committed from this page's top bar and
 * are applied to the running session when this profile is the active one.
 */
@Composable
fun ProfileOverrideScreen(navigator: Navigator, profileId: String) {
    val profilesViewModel = koinViewModel<ProfilesViewModel>()
    val componentSizes = AppTheme.sizes
    val scrollBehavior = MiuixScrollBehavior()
    val pageListState = rememberLazyListState()

    val profiles by profilesViewModel.profiles.collectAsState()
    val profile = remember(profiles, profileId) {
        profiles.firstOrNull { it.uuid.toString() == profileId }
    }

    val draft = rememberProfileOverrideDraft(profileId = profileId, profile = profile)
    val overrideReorderState = rememberProfileOverrideReorderState(draft)

    ProfileSettingsPage(
        title = YumeTxt.ProfilesPage.SettingsDialog.SectionOverride,
        scrollBehavior = scrollBehavior,
        listState = pageListState,
    ) {
        item(key = "profile-override-section") {
            Column(modifier = Modifier.fillMaxWidth()) {
                ProfileOverrideCustomRoutingSection(
                    customRoutingSelected = draft.customRoutingSelected,
                    enabled = draft.isLoaded,
                    onCustomRoutingChange = { checked -> draft.setCustomRouting(checked) },
                    onOpenCustomRouting = { navigator.push(Route.CustomRouting) },
                )

                Title(YumeTxt.Settings.UiSettings.OverrideScript)

                ProfileOverrideRowList(
                    draft = draft,
                    reorderState = overrideReorderState,
                    maxHeight = componentSizes.profileSettingsListMaxHeight,
                )
            }
            SectionSummary(YumeTxt.ProfilesPage.SettingsDialog.SectionOverrideSummary)
        }
    }
}

/** The switch that turns custom routing on for this profile, and the arrow into its editor. */
@Composable
private fun ProfileOverrideCustomRoutingSection(
    customRoutingSelected: Boolean,
    enabled: Boolean,
    onCustomRoutingChange: (Boolean) -> Unit,
    onOpenCustomRouting: () -> Unit,
) {
    Title(YumeTxt.ProfilesPage.SettingsDialog.CustomRouting)
    AppCard {
        PreferenceSwitchItem(
            title = YumeTxt.ProfilesPage.SettingsDialog.EnableCustomRouting,
            checked = customRoutingSelected,
            enabled = enabled,
            onCheckedChange = { checked -> onCustomRoutingChange(checked) },
        )
        PreferenceArrowItem(
            title = YumeTxt.ProfilesPage.SettingsDialog.OpenCustomRouting,
            onClick = onOpenCustomRouting,
        )
    }
    SectionSummary(YumeTxt.ProfilesPage.SettingsDialog.CustomRoutingSummary)
}

/**
 * The add dropdown and the profile's override rows, in one card and capped at [maxHeight].
 *
 * [draft] is this page's own state holder: it owns the list state, the selection and the row order,
 * so the section reads them from it instead of taking each one as a parameter.
 */
@Composable
private fun ProfileOverrideRowList(
    draft: ProfileOverrideDraft,
    reorderState: ReorderableLazyListState,
    maxHeight: Dp,
) {
    AppCard {
        GlassDropdownPreference(
            options =
                draft.pickerConfigs.map { config ->
                    GlassDropdownOption(
                        value = config.id,
                        label = config.name,
                        icon = if (draft.isAttached(config.id)) Yume.Check else Yume.BadgePlus,
                    )
                },
            selected = null,
            title = YumeTxt.Override.Dialog.Create.Title,
            enabled = draft.isLoaded,
            maxHeight = maxHeight,
            modifier =
                Modifier
                    .fillMaxWidth(),
            onSelected = { overrideId ->
                if (draft.isAttached(overrideId)) {
                    draft.detach(overrideId)
                } else {
                    draft.attach(overrideId)
                }
            },
        )

        if (draft.rowIds.isNotEmpty()) {
            LazyColumn(
                state = draft.overrideListState,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxHeight),
            ) {
                items(draft.rowIds, key = { id -> "override-$id" }) { id ->
                    val config = draft.configFor(id) ?: return@items
                    ProfileOverrideConfigRow(
                        config = config,
                        isSelected = draft.isSelected(id),
                        enabled = draft.isLoaded,
                        reorderKey = "override-$id",
                        reorderState = reorderState,
                        onSelectedChange = { selected -> draft.setSelected(id, selected) },
                        modifier =
                            Modifier.animateItem(
                                fadeInSpec = tween(160),
                                fadeOutSpec = tween(120),
                                placementSpec = tween(220, easing = FastOutSlowInEasing),
                            ),
                    )
                }
            }
        }
    }
}

/**
 * One override row: a left drag handle, the name and the enable checkbox.
 *
 * [reorderKey] must match the key the list item was declared with, or dragging would address a
 * different item.
 */
@Composable
private fun LazyItemScope.ProfileOverrideConfigRow(
    config: OverrideConfig,
    isSelected: Boolean,
    enabled: Boolean,
    reorderKey: String,
    reorderState: ReorderableLazyListState,
    onSelectedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Only the handle starts a drag, so the row's tap area and checkbox stay usable. ReorderableItem
    // extends LazyItemScope, so it has to stay a direct child of the list item.
    ReorderableItem(
        state = reorderState,
        key = reorderKey,
        modifier = modifier.fillMaxWidth(),
    ) { isDragging ->
        ProfileOverrideRowContent(
            title = config.name,
            isSelected = isSelected,
            enabled = enabled,
            onSelectedChange = onSelectedChange,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .alpha(if (isDragging) 0.9f else 1f),
            dragHandleModifier = Modifier.draggableHandle(),
        )
    }
}

/** The row itself: drag handle, name, enable checkbox; the caller supplies the drag modifier. */
@Composable
private fun ProfileOverrideRowContent(
    title: String,
    isSelected: Boolean,
    enabled: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    dragHandleModifier: Modifier = Modifier,
) {
    BasicComponent(
        title = title,
        modifier = modifier,
        enabled = enabled,
        startAction = {
            Icon(
                imageVector = Yume.List,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier =
                    dragHandleModifier
                        .padding(end = UiDp.dp12)
                        .size(UiDp.dp24),
            )
        },
        endActions = {
            Checkbox(
                state =
                    if (isSelected) {
                        ToggleableState.On
                    } else {
                        ToggleableState.Off
                    },
                enabled = enabled,
                onClick = {
                    onSelectedChange(!isSelected)
                },
            )
        },
        onClick = {
            if (!isSelected) {
                onSelectedChange(true)
            }
        },
    )
}

