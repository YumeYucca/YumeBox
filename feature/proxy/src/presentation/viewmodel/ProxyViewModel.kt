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
import com.github.yumeyucca.yumebox.core.presentation.ContractStateViewModel
import com.github.yumeyucca.yumebox.core.presentation.LoadableState
import com.github.yumeyucca.yumebox.core.util.PollingTimerSpecs
import com.github.yumeyucca.yumebox.core.util.PollingTimers
import com.github.yumeyucca.yumebox.data.controller.RuntimeOverrideController
import com.github.yumeyucca.yumebox.data.model.ProxySortMode
import com.github.yumeyucca.yumebox.data.store.AppSettingsStore
import com.github.yumeyucca.yumebox.data.store.ProxyDisplaySettingsStore
import com.github.yumeyucca.yumebox.domain.model.ProxyGroupInfo
import com.github.yumeyucca.yumebox.runtime.client.ProxyFacade
import com.github.yumeyucca.yumebox.runtime.client.ProxyGroupSyncPriority
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import tf.gal.yumebox.locale.YumeTxt

class ProxyViewModel(
    private val runtimeOverrideController: RuntimeOverrideController,
    private val proxyFacade: ProxyFacade,
    private val proxyDisplaySettingsStore: ProxyDisplaySettingsStore,
    appSettings: AppSettingsStore,
) :
    ContractStateViewModel<ProxyViewModel.ProxyUiState, ProxyViewModel.ProxyUiEffect>(
        ProxyUiState()
    ) {
    private val _testingGroupNames = MutableStateFlow<Set<String>>(emptySet())
    val testingGroupNames: StateFlow<Set<String>> = _testingGroupNames.asStateFlow()

    private val _testingProxyNames = MutableStateFlow<Set<String>>(emptySet())
    val testingProxyNames: StateFlow<Set<String>> = _testingProxyNames.asStateFlow()

    private var groupDelayTestInProgress = false
    private val pendingProxyDelayTests = mutableSetOf<String>()

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

    val singleNodeTest: StateFlow<Boolean> =
        appSettings.singleNodeTest.state.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            true,
        )

    val proxyGroups: StateFlow<List<ProxyGroupInfo>> =
        proxyFacade.proxyGroups
            .map { groups -> groups.filterNot(ProxyGroupInfo::hidden) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val activeSyncSources = mutableSetOf<String>()

    init {
        proxyFacade.warmUpProxyGroups()
        viewModelScope.launch {
            proxyGroups
                .distinctUntilChangedBy { groups -> groups.map(ProxyGroupInfo::name) }
                .collect { groups -> groupSorter.track(groups) }
        }
    }

    val sortedProxyGroups: StateFlow<List<ProxyGroupInfo>> =
        groupSorter.bind(scope = viewModelScope, proxyGroups = proxyGroups, sortMode = sortMode)

    fun ensureCoreLoaded(isActive: Boolean, source: String = "proxy_page") {
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
            viewModelScope.launch {
                runCatching {
                    if (proxyGroups.value.isEmpty()) {
                        proxyFacade.refreshProxyGroups()
                    }
                }
                    .onFailure { error -> if (error is CancellationException) throw error }
            }
        }
    }

    fun refreshGroup(groupName: String) {
        viewModelScope.launch {
            runCatching { proxyFacade.refreshProxyGroup(groupName) }
                .onFailure { error -> if (error is CancellationException) throw error }
        }
    }

    fun testDelay(groupName: String? = null) {
        if (groupDelayTestInProgress) return
        groupDelayTestInProgress = true
        viewModelScope.launch {
            try {
                setLoading(true)
                clearError()
                val currentGroups = proxyGroups.value
                val testingTargets: Set<String> =
                    if (groupName != null) {
                        setOf(groupName)
                    } else {
                        currentGroups.mapTo(linkedSetOf()) { it.name }
                    }
                if (testingTargets.isNotEmpty()) {
                    _testingGroupNames.update { it + testingTargets }
                }

                val result = runCatching {
                    if (groupName != null) {
                        showMessage(YumeTxt.Proxy.Testing.Group.format(groupName))
                        proxyFacade.healthCheck(groupName, singleNodeTest.value)
                        showMessage(YumeTxt.Proxy.Testing.RequestSent)
                    } else {
                        showMessage(YumeTxt.Proxy.Testing.All)
                        proxyFacade.healthCheckAll(singleNodeTest.value)
                    }
                }

                setLoading(false)

                if (testingTargets.isNotEmpty()) {
                    PollingTimers.awaitTick(PollingTimerSpecs.ProxyTestingSortHold)
                    _testingGroupNames.update { it - testingTargets }
                }

                result.exceptionOrNull()?.let { error ->
                    if (error is CancellationException) throw error
                    showError(YumeTxt.Proxy.Testing.Failed.format(error.message))
                }
            } finally {
                setLoading(false)
                groupDelayTestInProgress = false
            }
        }
    }

    fun setSortMode(mode: ProxySortMode) {
        proxyDisplaySettingsStore.sortMode.set(mode)
    }

    fun selectProxy(groupName: String, proxyName: String) {
        viewModelScope.launch {
            runCatching {
                val success = proxyFacade.selectProxy(groupName, proxyName)
                if (success) {
                    showMessage(YumeTxt.Proxy.Selection.Switched.format(proxyName))
                } else {
                    showError(YumeTxt.Proxy.Selection.Failed)
                }
            }
                .onFailure { error ->
                    showError(YumeTxt.Proxy.Selection.Error.format(error.message))
                }
        }
    }

    fun testProxyDelay(groupName: String, proxyName: String) {
        if (!pendingProxyDelayTests.add(proxyName)) return
        viewModelScope.launch {
            _testingProxyNames.update { it + proxyName }
            try {
                runCatching { proxyFacade.healthCheckProxy(groupName, proxyName) }
                    .onFailure { error ->
                        if (error is CancellationException) throw error
                        showError(YumeTxt.Proxy.Testing.Failed.format(error.message))
                    }
            } finally {
                _testingProxyNames.update { it - proxyName }
                pendingProxyDelayTests.remove(proxyName)
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
