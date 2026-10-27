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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.github.yumeyucca.yumebox.common.util.toast
import com.github.yumeyucca.yumebox.core.model.OverrideInternalConstants
import com.github.yumeyucca.yumebox.data.controller.OverrideService
import com.github.yumeyucca.yumebox.data.model.OverrideConfig
import com.github.yumeyucca.yumebox.data.model.ProfileBinding
import com.github.yumeyucca.yumebox.data.store.ProfileBindingProvider
import com.github.yumeyucca.yumebox.feature.editor.language.LanguageScope
import com.github.yumeyucca.yumebox.feature.meta.presentation.util.CustomRoutingBootstrapper
import com.github.yumeyucca.yumebox.presentation.component.*
import com.github.yumeyucca.yumebox.presentation.icon.Yume
import com.github.yumeyucca.yumebox.presentation.icon.yume.Check
import com.github.yumeyucca.yumebox.presentation.icon.yume.Edit
import com.github.yumeyucca.yumebox.presentation.navigation.Route
import com.github.yumeyucca.yumebox.presentation.theme.AppTheme
import com.github.yumeyucca.yumebox.presentation.theme.LocalTopBarBackdrop
import com.github.yumeyucca.yumebox.presentation.theme.UiDp
import com.github.yumeyucca.yumebox.presentation.theme.horizontalPadding
import com.github.yumeyucca.yumebox.presentation.util.OverrideEditorStore
import com.github.yumeyucca.yumebox.presentation.viewmodel.OverrideConfigViewModel
import com.github.yumeyucca.yumebox.runtime.api.Profile
import com.github.yumeyucca.yumebox.runtime.client.ProfilePatch
import com.github.yumeyucca.yumebox.screen.home.HomeViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import timber.log.Timber
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.github.yumeyucca.yumebox.core.locale.YumeTxt

/**
 * Standalone "编辑配置" page, replacing the old edit-options dialog and settings sheet.
 *
 * Two tabs split the page: 订阅 (name / link / age key) first, then 覆写 (custom routing switch plus
 * the profile's override selection). The tab row sticks to the top of the content area; a one-line
 * description under it explains the active group. Edits are committed from the top bar; leaving
 * without confirming discards them, like the sheet's cancel did.
 */
@Composable
fun ProfileEditScreen(navigator: Navigator, profileId: String) {
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
    val backdrop = LocalTopBarBackdrop.current

    val profiles by profilesViewModel.profiles.collectAsState()
    val isRunning by homeViewModel.isRunning.collectAsState()
    val builtInConfigs by overrideConfigViewModel.builtInConfigs.collectAsState()
    val userConfigs by overrideConfigViewModel.userConfigs.collectAsState()

    val profile = remember(profiles, profileId) {
        profiles.firstOrNull { it.uuid.toString() == profileId }
    }
    val selectableConfigs = remember(builtInConfigs, userConfigs) { builtInConfigs + userConfigs }

    var editName by remember(profileId) { mutableStateOf(TextFieldValue()) }
    var editSource by remember(profileId) { mutableStateOf(TextFieldValue()) }
    var editAgeSecretKey by remember(profileId) { mutableStateOf(TextFieldValue()) }
    var ageSecretKeyEdited by remember(profileId) { mutableStateOf(false) }
    var customRoutingSelected by remember(profileId) { mutableStateOf(false) }
    var pendingSelectedOverrideIds by remember(profileId) { mutableStateOf(emptyList<String>()) }
    // True once the override selection has been seeded from the binding (or edited by the user).
    // The binding loads ASYNCHRONOUSLY after the page is interactive, so a late arrival must not
    // clobber a toggle the user already changed.
    var overrideSelectionInitialized by remember(profileId) { mutableStateOf(false) }
    var binding by remember(profileId) { mutableStateOf<ProfileBinding?>(null) }
    var isSaving by remember(profileId) { mutableStateOf(false) }
    // 订阅 first: index 0 opens the page on the subscription fields.
    var selectedTab by rememberSaveable(profileId) { mutableStateOf(0) }

    val tabTitles =
        remember {
            listOf(
                YumeTxt.ProfilesPage.SettingsDialog.SectionSubscription,
                YumeTxt.ProfilesPage.SettingsDialog.SectionOverride,
            )
        }
    val tabSummaries =
        remember {
            listOf(
                YumeTxt.ProfilesPage.SettingsDialog.SectionSubscriptionSummary,
                YumeTxt.ProfilesPage.SettingsDialog.SectionOverrideSummary,
            )
        }

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
    val pageListState = rememberLazyListState()
    val overrideListState = rememberLazyListState()
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

    // Seed the editable fields once the profile arrives; keyed on the id only so a list refresh
    // emitting an equal profile cannot wipe edits in progress.
    LaunchedEffect(profile?.uuid) {
        val target = profile ?: return@LaunchedEffect
        editName = TextFieldValue(target.name, TextRange(target.name.length))
    }

    // Same load order as the sheet it replaces: refresh overrides first, then read the binding.
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

    val openConfigInEditor = {
        val target = profile
        if (target != null) {
            val configFile = importedConfigFile(target)
            openProfileConfigPreview(
                targetFile = configFile,
                missingMessage =
                    YumeTxt.ProfilesPage.SettingsDialog.ConfigMissing.format(
                        configFile.absolutePath
                    ),
                editable = true,
                onReadFailed = context::toast,
            ) { content, callback ->
                OverrideEditorStore.setupConfigPreview(
                    title = target.name,
                    content = content,
                    language = LanguageScope.Yaml,
                    callback = callback,
                )
                navigator.push(Route.OverrideConfigPreview)
            }
        }
    }

    val saveSettings = {
        if (!isSaving) {
            scope.launch {
                val target = profile
                if (target == null) {
                    navigator.pop()
                    return@launch
                }
                isSaving = true
                runCatching {
                    val trimmedName = editName.text.trim()
                    val trimmedSource = editSource.text.trim()
                    val trimmedAgeSecretKey = editAgeSecretKey.text.trim()
                    val targetSource =
                        if (target.type == Profile.Type.Url && trimmedSource.isNotEmpty()) {
                            trimmedSource
                        } else {
                            target.source
                        }
                    val hasMetaChanges =
                        trimmedName != target.name ||
                                targetSource != target.source ||
                                ageSecretKeyEdited
                    if (trimmedName.isNotEmpty() && targetSource.isNotEmpty() && hasMetaChanges) {
                        profilesViewModel.patchProfile(
                            uuid = target.uuid,
                            patch =
                                ProfilePatch(
                                    name = trimmedName,
                                    source = targetSource,
                                    interval = target.interval,
                                    updateAgeSecretKey = ageSecretKeyEdited,
                                    ageSecretKey =
                                        if (ageSecretKeyEdited) trimmedAgeSecretKey else null,
                                ),
                        )
                    }

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
                title = YumeTxt.ProfilesPage.SettingsDialog.EditProfile,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    // 与节点页一致的玻璃圆钮 chrome。
                    GlassTopIconButton(
                        action =
                            GlassTopAction(
                                icon = MiuixIcons.Back,
                                contentDescription = YumeTxt.Component.Navigation.Back,
                                onClick = { navigator.pop() },
                            ),
                        backdrop = backdrop,
                    )
                },
                actions = {
                    GlassTopActionPill(
                        actions =
                            listOf(
                                GlassTopAction(
                                    icon = Yume.Edit,
                                    contentDescription =
                                        YumeTxt.ProfilesPage.SettingsDialog.OpenConfig,
                                    onClick = openConfigInEditor,
                                ),
                                GlassTopAction(
                                    icon = Yume.Check,
                                    contentDescription = YumeTxt.ProfilesPage.Button.Confirm,
                                    onClick = saveSettings,
                                ),
                            ),
                        backdrop = backdrop,
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
            // 订阅在前,覆写在后;tab 吸附在内容区顶部。
            stickyHeader(key = "profile-edit-tabs") {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(MiuixTheme.colorScheme.surface)
                            .padding(vertical = UiDp.dp4),
                ) {
                    SegmentedTabRow(
                        tabs = tabTitles,
                        selectedTabIndex = selectedTab,
                        onTabSelected = { selectedTab = it },
                        modifier = Modifier.padding(horizontal = UiDp.dp12),
                        tabHeight = UiDp.dp42,
                    )
                }
            }

            item(key = "profile-edit-section-summary") {
                Text(
                    text = tabSummaries.getOrElse(selectedTab) { "" },
                    modifier =
                        Modifier
                            .horizontalPadding()
                            .padding(top = spacing.space8, bottom = spacing.space12),
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }

            if (selectedTab == 0) {
                item(key = "profile-edit-subscription") {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .horizontalPadding(),
                        verticalArrangement = Arrangement.spacedBy(spacing.space12),
                    ) {
                        OemTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = YumeTxt.ProfilesPage.Input.ProfileName,
                            useLabelAsPlaceholder = true,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        if (profile?.type == Profile.Type.Url) {
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
            } else {
                item(key = "profile-edit-override") {
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
