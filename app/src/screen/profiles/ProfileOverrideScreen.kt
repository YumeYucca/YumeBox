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
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.navigation.Route
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import org.koin.androidx.compose.koinViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState
import top.yukonga.miuix.kmp.basic.*
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

                if (draft.rowIds.isNotEmpty()) {
                    ProfileOverrideRowList(
                        draft = draft,
                        reorderState = overrideReorderState,
                        maxHeight = componentSizes.profileSettingsListMaxHeight,
                    )
                }
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
 * The profile's override rows, capped at [maxHeight].
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

/**
 * One override row: a checkbox plus the row tap, wrapped so a selected row can be dragged.
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
    // Selected rows drag to reorder; both states share one row config so the checkbox can never
    // diverge from the tap area. ReorderableItem extends LazyItemScope, so it has to stay a direct
    // child of the list item instead of sitting inside an extra Box.
    val row: @Composable (Modifier) -> Unit = { rowModifier ->
        ProfileOverrideRowContent(
            title = config.name,
            isSelected = isSelected,
            enabled = enabled,
            onSelectedChange = onSelectedChange,
            modifier = rowModifier,
        )
    }
    if (isSelected) {
        ReorderableItem(
            state = reorderState,
            key = reorderKey,
            modifier = modifier.fillMaxWidth(),
        ) { isDragging ->
            row(
                Modifier
                    .fillMaxWidth()
                    .longPressDraggableHandle()
                    .alpha(if (isDragging) 0.9f else 1f)
            )
        }
    } else {
        Box(modifier = modifier.fillMaxWidth()) { row(Modifier) }
    }
}

/** The row itself: its name, its checkbox and its tap area, on the modifier the caller chooses. */
@Composable
private fun ProfileOverrideRowContent(
    title: String,
    isSelected: Boolean,
    enabled: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicComponent(
        title = title,
        modifier = modifier,
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
