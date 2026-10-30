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

@file:Suppress("UnusedSymbol")

package com.github.yumeyucca.yumebox.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.github.yumeyucca.yumebox.presentation.base.ContractStateViewModel
import com.github.yumeyucca.yumebox.presentation.base.LoadableState
import com.github.yumeyucca.yumebox.data.model.ProxySortMode
import com.github.yumeyucca.yumebox.data.store.ProxyDisplaySettingsStore
import com.github.yumeyucca.yumebox.domain.model.ProxyGroupInfo
import com.github.yumeyucca.yumebox.runtime.client.ProxyFacade
import com.github.yumeyucca.yumebox.runtime.client.ProxyGroupSyncPriority
import com.github.yumeyucca.yumebox.runtime.client.ProxyGroupSyncSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.github.yumeyucca.yumebox.core.locale.YumeTxt

sealed interface ProxyDelayTestProgress {
    data class Running(val completed: Int, val total: Int) : ProxyDelayTestProgress
}

/**
 * The delay tests currently in flight.
 *
 * Group tests and proxy tests are two facts, not one: a group health check and per-node tests can
 * run at the same time ([testDelay] only latches against another group test, [testProxyDelay] is
 * independent), and the UI reports them on different surfaces — the sheet/header refresh indicator
 * keys off [groupNames], the per-node spinners key off [proxyNames]. They are kept as two sets in
 * one observed state so the boundary collects a single flow instead of one per name set.
 */
data class DelayTestState(
    val groupNames: Set<String> = emptySet(),
    val proxyNames: Set<String> = emptySet(),
)

class ProxyViewModel(
    private val proxyFacade: ProxyFacade,
    private val proxyDisplaySettingsStore: ProxyDisplaySettingsStore,
) :
    ContractStateViewModel<ProxyViewModel.ProxyUiState, ProxyViewModel.ProxyUiEffect>(
        ProxyUiState()
    ) {
    private val _delayTests = MutableStateFlow(DelayTestState())
    val delayTests: StateFlow<DelayTestState> = _delayTests.asStateFlow()

    private val _delayTestProgress = MutableStateFlow<ProxyDelayTestProgress?>(null)
    val delayTestProgress: StateFlow<ProxyDelayTestProgress?> = _delayTestProgress.asStateFlow()

    // Re-entrancy latch for [testDelay] only. It is not derivable from [DelayTestState]: the name
    // set is published inside the launched coroutine, so a second call in the same frame would slip
    // past a set-based guard, and "test all" holds the bulk path even when no group names exist.
    private var groupDelayTestInProgress = false

    /** UI selection shared between the left group list and shell right-pane node list. */
    private val _uiSelectedGroupName = MutableStateFlow<String?>(null)
    val uiSelectedGroupName: StateFlow<String?> = _uiSelectedGroupName.asStateFlow()

    fun selectUiGroup(name: String?) {
        _uiSelectedGroupName.value = name
    }

    private val groupSorter = ProxyGroupSorter()

    val sortMode: StateFlow<ProxySortMode> =
        proxyDisplaySettingsStore.sortMode.state.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            ProxySortMode.DEFAULT,
        )

    val nodeSearchVisible: StateFlow<Boolean> =
        proxyDisplaySettingsStore.nodeSearchVisible.state.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            false,
        )

    val proxyGroups: StateFlow<List<ProxyGroupInfo>> =
        proxyFacade.proxyGroups
            .map { groups -> groups.filterNot(ProxyGroupInfo::hidden) }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun proxyDelayFlow(name: String) = proxyFacade.proxyDelayFlow(name)

    private val activeSyncSources = mutableSetOf<ProxyGroupSyncSource>()

    init {
        proxyFacade.warmUpProxyGroups()
        viewModelScope.launch {
            proxyGroups
                .distinctUntilChangedBy { groups -> groups.map(ProxyGroupInfo::name) }
                .flowOn(Dispatchers.Default)
                .collect { groups -> groupSorter.track(groups) }
        }
    }

    val sortedProxyGroups: StateFlow<List<ProxyGroupInfo>> =
        groupSorter.bind(scope = viewModelScope, proxyGroups = proxyGroups, sortMode = sortMode)

    fun ensureCoreLoaded(
        isActive: Boolean,
        source: ProxyGroupSyncSource = ProxyGroupSyncSource.Page,
    ) {
        val changed =
            if (isActive) {
                activeSyncSources.add(source)
            } else {
                activeSyncSources.remove(source)
            }
        if (!changed) return
        proxyFacade.setProxyGroupSyncPriority(
            priority = if (isActive) ProxyGroupSyncPriority.FAST else ProxyGroupSyncPriority.OFF,
            source = source,
        )
        if (isActive) {
            launchGuarded {
                if (proxyGroups.value.isEmpty()) {
                    proxyFacade.refreshProxyGroups()
                }
            }
        }
    }

    fun refreshGroup(groupName: String) {
        launchGuarded(onFailure = { error ->
            showError(error.message ?: YumeTxt.Util.Error.UnknownError)
        }) {
            proxyFacade.refreshProxyGroup(groupName)
        }
    }

    fun testDelay(groupName: String? = null) {
        if (groupDelayTestInProgress) return
        groupDelayTestInProgress = true
        viewModelScope.launch {
            try {
                clearError()
                val currentGroups = proxyGroups.value
                val testingTargets: Set<String> =
                    if (groupName != null) {
                        setOf(groupName)
                    } else {
                        currentGroups.mapTo(linkedSetOf()) { it.name }
                    }
                if (testingTargets.isNotEmpty()) {
                    _delayTests.update { it.copy(groupNames = it.groupNames + testingTargets) }
                }

                val onProgress: (Int, Int) -> Unit = { completed, total ->
                    val next = ProxyDelayTestProgress.Running(completed, total)
                    if (_delayTestProgress.value != next) {
                        _delayTestProgress.value = next
                    }
                }
                val result =
                    runCatching {
                        if (groupName != null) {
                            proxyFacade.healthCheck(groupName, onProgress)
                        } else {
                            proxyFacade.healthCheckAll(onProgress)
                        }
                    }

                if (testingTargets.isNotEmpty()) {
                    _delayTests.update { it.copy(groupNames = it.groupNames - testingTargets) }
                }

                _delayTestProgress.value = null

                result.exceptionOrNull()?.let { error ->
                    if (error is CancellationException) throw error
                    showError(YumeTxt.Proxy.Testing.Failed.format(error.message))
                }
            } finally {
                groupDelayTestInProgress = false
            }
        }
    }

    fun setSortMode(mode: ProxySortMode) {
        proxyDisplaySettingsStore.sortMode.set(mode)
    }

    fun setNodeSearchVisible(visible: Boolean) {
        proxyDisplaySettingsStore.nodeSearchVisible.set(visible)
    }

    fun selectProxy(groupName: String, proxyName: String) {
        launchGuarded(onFailure = { error ->
            showError(YumeTxt.Proxy.Selection.Error.format(error.message))
        }) {
            val success = proxyFacade.selectProxy(groupName, proxyName)
            if (success) {
                showMessage(YumeTxt.Proxy.Selection.Switched.format(proxyName))
            } else {
                showError(YumeTxt.Proxy.Selection.Failed)
            }
        }
    }

    fun testProxyDelay(proxyName: String) {
        // Claim the name atomically: the observed set is both the spinner source and the
        // "already in flight" guard, so a second tap on the same node while its test runs is still
        // dropped instead of queueing a duplicate health check.
        val previous = _delayTests.getAndUpdate { state ->
            if (proxyName in state.proxyNames) {
                state
            } else {
                state.copy(proxyNames = state.proxyNames + proxyName)
            }
        }
        if (proxyName in previous.proxyNames) return
        launchGuarded(onFailure = { error ->
            showError(YumeTxt.Proxy.Testing.Failed.format(error.message))
        }) {
            try {
                proxyFacade.healthCheckProxy(proxyName)
            } finally {
                _delayTests.update { it.copy(proxyNames = it.proxyNames - proxyName) }
            }
        }
    }

    /**
     * Runs [block] in [viewModelScope] so that the "cancellation is not a failure" rule lives in one
     * place: every proxy call here is a fire-and-forget action the user triggered, and a job that was
     * cancelled mid-flight (page left, another selection made) must neither toast nor be swallowed.
     */
    private fun launchGuarded(
        onFailure: (Throwable) -> Unit = {},
        block: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            runCatching { block() }.onFailure { error ->
                if (error is CancellationException) throw error
                onFailure(error)
            }
        }
    }

    private fun showMessage(message: String) {
        postMessage(message, ProxyUiEffect.ShowMessage(message))
    }

    private fun showError(error: String) {
        postError(error, ProxyUiEffect.ShowError(error))
    }

    fun clearError() {
        clearErrorState()
    }

    data class ProxyUiState(
        override val isLoading: Boolean = false,
        override val message: String? = null,
        override val error: String? = null,
    ) : LoadableState<ProxyUiState> {
        override fun withLoading(loading: Boolean): ProxyUiState = copy(isLoading = loading)

        override fun withError(error: String?): ProxyUiState = copy(error = error)

        override fun withMessage(message: String?): ProxyUiState = copy(message = message)
    }

    sealed interface ProxyUiEffect {
        data class ShowMessage(val message: String) : ProxyUiEffect

        data class ShowError(val message: String) : ProxyUiEffect
    }
}
