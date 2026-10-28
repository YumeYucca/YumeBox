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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.state.ToggleableState
import com.github.yumeyucca.yumebox.common.util.toast
import com.github.yumeyucca.yumebox.core.model.OverrideInternalConstants
import com.github.yumeyucca.yumebox.data.controller.OverrideService
import com.github.yumeyucca.yumebox.data.model.OverrideConfig
import com.github.yumeyucca.yumebox.data.model.ProfileBinding
import com.github.yumeyucca.yumebox.data.store.ProfileBindingProvider
import com.github.yumeyucca.yumebox.feature.meta.presentation.util.CustomRoutingBootstrapper
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.Check
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.theme.LocalTopBarBackdrop
import com.github.yumeyucca.yumebox.presentation.viewmodel.OverrideConfigViewModel
import com.github.yumeyucca.yumebox.runtime.api.Profile
import com.github.yumeyucca.yumebox.screen.home.HomeViewModel
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import timber.log.Timber
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.preference.SwitchPreference

/**
 * Standalone "覆写" page for one profile: custom routing and the profile's override selection.
 *
 * The edit page no longer carries this state; overrides are committed from this page's top bar and
 * are applied to the running session when this profile is the active one.
 */
@Composable
fun ProfileOverrideScreen(navigator: Navigator, profileId: String) {
    val profilesViewModel = koinViewModel<ProfilesViewModel>()
    val homeViewModel = koinViewModel<HomeViewModel>()
    val overrideConfigViewModel = koinViewModel<OverrideConfigViewModel>()
    val bindingProvider: ProfileBindingProvider = koinInject()
    val overrideService: OverrideService = koinInject()
    val routingBootstrapper: CustomRoutingBootstrapper = koinInject()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val spacing = AppTheme.spacing
    val componentSizes = AppTheme.sizes
    val scrollBehavior = MiuixScrollBehavior()
    val pageListState = rememberLazyListState()
    val overrideListState = rememberLazyListState()

    val profiles by profilesViewModel.profiles.collectAsState()
    val isRunning by homeViewModel.isRunning.collectAsState()
    val builtInConfigs by overrideConfigViewModel.builtInConfigs.collectAsState()
    val userConfigs by overrideConfigViewModel.userConfigs.collectAsState()

    val profile = remember(profiles, profileId) {
        profiles.firstOrNull { it.uuid.toString() == profileId }
    }
    val selectableConfigs = remember(builtInConfigs, userConfigs) { builtInConfigs + userConfigs }

    var customRoutingSelected by remember(profileId) { mutableStateOf(false) }
    var pendingSelectedOverrideIds by remember(profileId) { mutableStateOf(emptyList<String>()) }
    // The binding loads asynchronously after the page is interactive, so a late arrival must not
    // clobber a toggle the user already changed.
    var overrideSelectionInitialized by remember(profileId) { mutableStateOf(false) }
    var binding by remember(profileId) { mutableStateOf<ProfileBinding?>(null) }
    var isSaving by remember(profileId) { mutableStateOf(false) }

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
    val overrideReorderState =
        rememberReorderableLazyListState(overrideListState) { from, to ->
            pendingSelectedOverrideIds =
                reorderVisibleOverrideIds(
                    allIds = pendingSelectedOverrideIds,
                    visibleIds = currentVisibleSelectedOverrideIds,
                    from = from.index,
                    to = to.index,
                )
        }

    LaunchedEffect(profileId) {
        overrideConfigViewModel.refreshAndAwait()
        val loaded = bindingProvider.getBinding(profileId)
        binding = loaded
        if (!overrideSelectionInitialized && loaded != null) {
            customRoutingSelected = customRoutingId in loaded.overrideIds
            pendingSelectedOverrideIds = loaded.overrideIds
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

    val saveOverrides = {
        if (!isSaving) {
            scope.launch {
                val target = profile
                if (target == null) {
                    navigator.pop()
                    return@launch
                }
                isSaving = true
                runCatching {
                    binding =
                        saveProfileOverrides(
                            profile = target,
                            selectedIds =
                                buildFinalOverrideIds(
                                    selectedOverrideIds = pendingSelectedOverrideIds,
                                    customRoutingSelected = customRoutingSelected,
                                ),
                            binding = binding,
                            bindingProvider = bindingProvider,
                            routingBootstrapper = routingBootstrapper,
                            overrideService = overrideService,
                            homeViewModel = homeViewModel,
                            isRunning = isRunning,
                            context = context,
                        )
                }
                    .onSuccess { navigator.pop() }
                    .onFailure { error ->
                        context.toast(error.message ?: YumeTxt.Util.Error.UnknownError)
                    }
                isSaving = false
            }
        }
    }

    GlassScaffold(
        topBar = {
            TopBar(
                title = YumeTxt.ProfilesPage.SettingsDialog.SectionOverride,
                scrollBehavior = scrollBehavior,
                actions = {
                    GlassTopActionPill(
                        actions =
                            listOf(
                                GlassTopAction(
                                    icon = Yume.Check,
                                    contentDescription = YumeTxt.ProfilesPage.Button.Confirm,
                                    onClick = saveOverrides,
                                ),
                            ),
                        backdrop = LocalTopBarBackdrop.current,
                    )
                },
            )
        }
    ) { paddingValues ->
        val mainLikePadding = rememberStandalonePageMainPadding()
        ScreenLazyColumn(
            scrollBehavior = scrollBehavior,
            innerPadding = combinePaddingValues(paddingValues, mainLikePadding),
            lazyListState = pageListState,
        ) {
            item(key = "profile-override-section") {
                Spacer(modifier = Modifier.height(spacing.space16))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(spacing.space12),
                ) {
                    AppCard {
                        SwitchPreference(
                            title = YumeTxt.ProfilesPage.SettingsDialog.CustomRouting,
                            summary = YumeTxt.ProfilesPage.SettingsDialog.CustomRoutingSummary,
                            checked = customRoutingSelected,
                            onCheckedChange = {
                                overrideSelectionInitialized = true
                                customRoutingSelected = it
                                pendingSelectedOverrideIds =
                                    if (it) {
                                        (pendingSelectedOverrideIds + customRoutingId).distinct()
                                    } else {
                                        pendingSelectedOverrideIds - customRoutingId
                                    }
                            },
                        )
                    }

                    if (overrideRowIds.isNotEmpty()) {
                        AppCard {
                            LazyColumn(
                                state = overrideListState,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .heightIn(
                                            max = componentSizes.profileSettingsListMaxHeight
                                        ),
                            ) {
                                items(overrideRowIds, key = { id -> "override-$id" }) { id ->
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
                                                overrideReorderState,
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
                                                                    pendingSelectedOverrideIds - id
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
                                                onClick = { toggleUserOverrideSelection(id) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                SectionSummary(YumeTxt.ProfilesPage.SettingsDialog.SectionOverrideSummary)
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

/**
 * Persists the override selection and applies it to the running session when this profile is the
 * one currently in use. Returns the refreshed binding for the page's local state.
 */
private suspend fun saveProfileOverrides(
    profile: Profile,
    selectedIds: List<String>,
    binding: ProfileBinding?,
    bindingProvider: ProfileBindingProvider,
    routingBootstrapper: CustomRoutingBootstrapper,
    overrideService: OverrideService,
    homeViewModel: HomeViewModel,
    isRunning: Boolean,
    context: android.content.Context,
): ProfileBinding? {
    val profileId = profile.uuid.toString()
    val overrideIds = selectedIds.distinct()
    val updatedBinding =
        binding?.copy(overrideIds = overrideIds)
            ?: ProfileBinding(profileId = profileId, overrideIds = overrideIds)

    bindingProvider.setBinding(updatedBinding)
    val refreshedBinding = bindingProvider.getBinding(profileId)

    if (OverrideInternalConstants.CUSTOM_ROUTING_OVERRIDE_ID in overrideIds) {
        runCatching { routingBootstrapper.ensureDefaultContent() }
            .onFailure { error ->
                Timber.e(
                    error,
                    "Failed to generate custom routing content for profile %s",
                    profileId,
                )
                context.toast(
                    error.message ?: YumeTxt.ProfilesPage.SettingsDialog.CustomRouting
                )
            }
    }

    val shouldApplyRuntime =
        (isRunning || profile.active) &&
                (profile.active || homeViewModel.isCurrentProfile(profile.uuid))
    if (shouldApplyRuntime) {
        val applied = overrideService.applyOverride(profileId)
        if (!applied) {
            Timber.w("Override apply reported failure for profile %s", profileId)
            context.toast(YumeTxt.Util.Error.UnknownError)
        }
    }
    return refreshedBinding
}
