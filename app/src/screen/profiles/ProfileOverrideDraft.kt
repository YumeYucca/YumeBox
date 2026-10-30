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

package com.github.yumeyucca.yumebox.screen.profiles

import android.content.Context
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.github.yumeyucca.yumebox.common.util.toast
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import com.github.yumeyucca.yumebox.core.model.OverrideInternalConstants
import com.github.yumeyucca.yumebox.data.controller.OverrideService
import com.github.yumeyucca.yumebox.data.model.OverrideConfig
import com.github.yumeyucca.yumebox.data.model.ProfileBinding
import com.github.yumeyucca.yumebox.data.store.ProfileBindingProvider
import com.github.yumeyucca.yumebox.feature.meta.presentation.util.CustomRoutingBootstrapper
import com.github.yumeyucca.yumebox.feature.override.presentation.viewmodel.OverrideConfigViewModel
import com.github.yumeyucca.yumebox.runtime.api.Profile
import com.github.yumeyucca.yumebox.screen.home.HomeViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import sh.calvin.reorderable.ReorderableLazyListState
import sh.calvin.reorderable.rememberReorderableLazyListState
import timber.log.Timber

/**
 * The 覆写 page's loaded state and its only write path: the profile's binding, the override rows
 * derived from the selection, and the serialised save every toggle and drag ends in.
 *
 * The page never assigns a field — [attach], [detach], [setSelected], [setCustomRouting] and
 * [reorder] are the only writers, and each of them persists the selection it just changed.
 * [ProfileOverrideSelection] still owns the loaded gate, so a toggle racing [load] cannot write over
 * a binding that has not been read yet.
 */
@Stable
internal class ProfileOverrideDraft internal constructor(
    val overrideListState: LazyListState,
    private val selection: ProfileOverrideSelection,
    private val profileId: String,
    private val currentProfile: State<Profile?>,
    private val currentIsRunning: State<Boolean>,
    private val builtInConfigs: State<List<OverrideConfig>>,
    private val userConfigs: State<List<OverrideConfig>>,
    private val context: Context,
    private val scope: CoroutineScope,
    private val configViewModel: OverrideConfigViewModel,
    private val bindingProvider: ProfileBindingProvider,
    private val routingBootstrapper: CustomRoutingBootstrapper,
    private val overrideService: OverrideService,
    private val homeViewModel: HomeViewModel,
) {
    private val customRoutingId = OverrideInternalConstants.CUSTOM_ROUTING_OVERRIDE_ID

    /** The binding the last save read back; nothing renders it, the next save builds on it. */
    private var binding: ProfileBinding? = null

    private val saveMutex = Mutex()

    /** False until [load] has read the binding; every switch row stays disabled until then. */
    val isLoaded: Boolean
        get() = selection.isLoaded

    /** 自定义路由 is one more override id, so the switch is just a membership test. */
    val customRoutingSelected: Boolean
        get() = selection.isEnabled(customRoutingId)

    private val selectableConfigs by derivedStateOf { builtInConfigs.value + userConfigs.value }

    private val selectableById by derivedStateOf { selectableConfigs.associateBy(OverrideConfig::id) }

    /** Attached rows in drag order; ids whose config disappeared are skipped. */
    val rowIds: List<String> by derivedStateOf {
        selection.ids.filter(selectableById::containsKey)
    }

    /** Configs offered by the picker, including already attached ones so they can be removed. */
    val pickerConfigs: List<OverrideConfig> by derivedStateOf {
        selectableConfigs
    }

    /** The row's config; null for an id that is no longer offered, which the row skips. */
    fun configFor(overrideId: String): OverrideConfig? = selectableById[overrideId]

    fun isSelected(overrideId: String): Boolean = selection.isEnabled(overrideId)

    fun isAttached(overrideId: String): Boolean = overrideId in selection.ids

    /** Adds a row without applying it, so the checkbox starts off. */
    fun attach(overrideId: String) {
        if (selection.attach(overrideId)) persistOverrides()
    }

    fun detach(overrideId: String) {
        if (selection.detach(overrideId)) persistOverrides()
    }

    /** Turns one row on or off; a toggle that changes nothing is never persisted. */
    fun setSelected(overrideId: String, selected: Boolean) {
        if (selection.setEnabled(overrideId, selected)) persistOverrides()
    }

    /** The 自定义路由 switch, behind the same commit-then-persist rule as a row. */
    fun setCustomRouting(selected: Boolean) {
        if (selection.setCustomRouting(customRoutingId, selected)) persistOverrides()
    }

    /**
     * Commits a drag inside the visible order; [ProfileOverrideSelection.reorder] answers false when
     * the move changes nothing, and a false result is never persisted.
     */
    fun reorder(from: Int, to: Int) {
        if (selection.reorder(visibleIds = rowIds, from = from, to = to)) persistOverrides()
    }

    /** Reads the persisted binding and seeds the list; the switches unlock afterwards. */
    suspend fun load() {
        configViewModel.refreshAndAwait()
        val loaded = bindingProvider.getBinding(profileId)
        binding = loaded
        selection.bind(loaded)
    }

    /**
     * Runs saves one at a time, in the order they were requested: a toggle during an in-flight save
     * waits for it instead of overwriting it. A failed save is toasted and leaves [binding] alone,
     * and the profile is read after the lock so a save queued behind a switch saves for the newest
     * one.
     */
    private fun persistOverrides() {
        val overrideIds = selection.ids.distinct()
        val disabledOverrideIds = selection.disabledIds.filter { it in overrideIds }
        scope.launch {
            saveMutex.withLock {
                val target = currentProfile.value ?: return@withLock
                runCatching {
                    binding =
                        saveOverrides(
                            target = target,
                            overrideIds = overrideIds,
                            disabledOverrideIds = disabledOverrideIds,
                        )
                }
                    .onFailure { error ->
                        // Leaving the screen cancels the save; that is not a save failure.
                        if (error is CancellationException) throw error
                        context.toast(error.message ?: YumeTxt.Util.Error.UnknownError)
                    }
            }
        }
    }

    /**
     * Persists the override selection and applies it to the running session when this profile is the
     * one currently in use. Returns the refreshed binding for [binding].
     */
    private suspend fun saveOverrides(
        target: Profile,
        overrideIds: List<String>,
        disabledOverrideIds: List<String>,
    ): ProfileBinding? {
        val targetId = target.uuid.toString()
        val updatedBinding =
            binding?.copy(
                overrideIds = overrideIds,
                disabledOverrideIds = disabledOverrideIds,
            )
                ?: ProfileBinding(
                    profileId = targetId,
                    overrideIds = overrideIds,
                    disabledOverrideIds = disabledOverrideIds,
                )

        bindingProvider.setBinding(updatedBinding)
        val refreshedBinding = bindingProvider.getBinding(targetId)

        if (OverrideInternalConstants.CUSTOM_ROUTING_OVERRIDE_ID in updatedBinding.enabledOverrideIds) {
            runCatching { routingBootstrapper.ensureDefaultContent() }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    Timber.e(
                        error,
                        "Failed to generate custom routing content for profile %s",
                        targetId,
                    )
                    context.toast(
                        error.message ?: YumeTxt.ProfilesPage.SettingsDialog.CustomRouting
                    )
                }
        }

        val shouldApplyRuntime =
            (currentIsRunning.value || target.active) &&
                    (target.active || homeViewModel.isCurrentProfile(target.uuid))
        if (shouldApplyRuntime) {
            val applied = overrideService.applyOverride(targetId)
            if (!applied) {
                Timber.w("Override apply reported failure for profile %s", targetId)
                context.toast(YumeTxt.Util.Error.UnknownError)
            }
        }
        return refreshedBinding
    }
}

/**
 * Remembers the [ProfileOverrideDraft] of [profileId] and keeps it loaded.
 *
 * [profile] is handed over as the latest-value state the page used to hold inline, so a save that
 * lands after a list refresh still persists for the profile on screen, and [ProfileOverrideDraft.load]
 * runs once per profile id — a re-emitted equal profile cannot re-seed the list under a drag.
 */
@Composable
internal fun rememberProfileOverrideDraft(
    profileId: String,
    profile: Profile?,
): ProfileOverrideDraft {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val configViewModel = koinViewModel<OverrideConfigViewModel>()
    val homeViewModel = koinViewModel<HomeViewModel>()
    val bindingProvider: ProfileBindingProvider = koinInject()
    val overrideService: OverrideService = koinInject()
    val routingBootstrapper: CustomRoutingBootstrapper = koinInject()

    val selection = rememberProfileOverrideSelection(profileId)
    val builtInConfigs = configViewModel.builtInConfigs.collectAsState()
    val userConfigs = configViewModel.userConfigs.collectAsState()
    val isRunning by homeViewModel.isRunning.collectAsState()
    val currentProfile = rememberUpdatedState(profile)
    val currentIsRunning = rememberUpdatedState(isRunning)
    val overrideListState = rememberLazyListState()

    val draft =
        remember(profileId) {
            ProfileOverrideDraft(
                overrideListState = overrideListState,
                selection = selection,
                profileId = profileId,
                currentProfile = currentProfile,
                currentIsRunning = currentIsRunning,
                builtInConfigs = builtInConfigs,
                userConfigs = userConfigs,
                context = context,
                scope = scope,
                configViewModel = configViewModel,
                bindingProvider = bindingProvider,
                routingBootstrapper = routingBootstrapper,
                overrideService = overrideService,
                homeViewModel = homeViewModel,
            )
        }
    LaunchedEffect(profileId) { draft.load() }
    return draft
}

/**
 * The drag handle state of [draft]'s row list.
 *
 * `rememberReorderableLazyListState` is a composable factory, so the one object the draft cannot
 * build itself is built here; every move still goes through [ProfileOverrideDraft.reorder], which
 * is what decides whether the drop is committed.
 */
@Composable
internal fun rememberProfileOverrideReorderState(
    draft: ProfileOverrideDraft,
): ReorderableLazyListState =
    rememberReorderableLazyListState(draft.overrideListState) { from, to ->
        draft.reorder(from = from.index, to = to.index)
    }
