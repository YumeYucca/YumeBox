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

@file:Suppress("UnusedSymbol", "FunctionName")

package com.github.yumeyucca.yumebox.feature.meta.presentation.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumeyucca.yumebox.core.model.OverrideInternalConstants
import com.github.yumeyucca.yumebox.core.util.YamlCodec
import com.github.yumeyucca.yumebox.data.controller.ActiveProfileOverrideReloader
import com.github.yumeyucca.yumebox.data.store.OverrideConfigStore
import com.github.yumeyucca.yumebox.feature.meta.presentation.util.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class CustomRoutingViewModel(
    private val overrideConfigRepository: OverrideConfigStore,
    private val activeProfileOverrideReloader: ActiveProfileOverrideReloader,
    private val customRoutingBootstrapper: CustomRoutingBootstrapper,
) : ViewModel() {
    private val presetSelectionState = MutableStateFlow(defaultOverridePresetTemplateSelection())
    val presetSelection: StateFlow<OverridePresetTemplateSelection> =
        presetSelectionState.asStateFlow()

    private val customRoutingContentState = MutableStateFlow("")
    val customRoutingContent: StateFlow<String> = customRoutingContentState.asStateFlow()

    private val templateRoundTripSafeState = MutableStateFlow(false)
    val templateRoundTripSafe: StateFlow<Boolean> = templateRoundTripSafeState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                applyContentState(customRoutingBootstrapper.ensureDefaultContent())
            } catch (error: CancellationException) {
                // The scope is gone; an empty custom routing page must not be published as content.
                throw error
            } catch (error: Exception) {
                Timber.e(error, "Failed to reload custom routing state from stored content")
            }
        }
    }

    suspend fun savePresetSelection(
        updatedPresetSelection: OverridePresetTemplateSelection
    ): Result<Unit> = saveOutcome {
        check(templateRoundTripSafeState.value) {
            "Preset changes cannot overwrite manually edited custom routing YAML"
        }
        persist(dumpPreset(updatedPresetSelection))
    }

    suspend fun saveCustomRoutingYaml(content: String): Result<Unit> = saveOutcome {
        val contentToSave =
            if (content.isBlank()) {
                dumpPreset(defaultOverridePresetTemplateSelection())
            } else {
                withContext(Dispatchers.Default) { YamlCodec.validate(content) }
                content
            }
        persist(contentToSave)
    }

    /**
     * Renders one preset. The dump is dozens of providers, groups and rules plus a re-load
     * self-check, so it stays off the main thread: saves are launched from the composition scope.
     */
    private suspend fun dumpPreset(selection: OverridePresetTemplateSelection): String =
        withContext(Dispatchers.Default) { buildPresetTemplateYaml(selection) }

    /**
     * A failed save is a [Result] the screen toasts, but a cancelled one is not: leaving the screen
     * mid-save has to cancel this work instead of reporting a failure that never happened.
     */
    private suspend fun saveOutcome(block: suspend () -> Unit): Result<Unit> =
        try {
            block()
            Result.success(Unit)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error)
        }

    /** Stores one custom routing payload and re-applies it to the active profile. */
    private suspend fun persist(content: String) {
        overrideConfigRepository.saveCustomRoutingContent(content)
        applyContentState(content)
        activeProfileOverrideReloader.reapplyActiveProfileIfUsingOverride(
            OverrideInternalConstants.CUSTOM_ROUTING_OVERRIDE_ID
        )
    }

    private suspend fun applyContentState(content: String?) {
        // Parsing a whole override YAML is the same weight as rendering one, so it happens off the
        // main thread too; only the three published values are written on the calling dispatcher.
        val analysis = withContext(Dispatchers.Default) { analyzePresetTemplateContent(content) }
        customRoutingContentState.value = content.orEmpty()
        presetSelectionState.value = analysis.selection
        templateRoundTripSafeState.value = analysis.matchesTemplateExactly
    }
}
