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

package com.github.yumeyucca.yumebox.screen.home

import android.app.Application
import android.content.Intent
import androidx.lifecycle.viewModelScope
import com.github.yumeyucca.yumebox.common.util.stateInWhileSubscribed
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import com.github.yumeyucca.yumebox.core.model.Proxy
import com.github.yumeyucca.yumebox.core.model.RunMode
import com.github.yumeyucca.yumebox.presentation.base.AndroidContractStateViewModel
import com.github.yumeyucca.yumebox.core.util.AutoStartSessionGate
import com.github.yumeyucca.yumebox.core.util.BuiltinGeoAssetsRequiredException
import com.github.yumeyucca.yumebox.data.store.NetworkSettingsStore
import com.github.yumeyucca.yumebox.runtime.api.Profile
import com.github.yumeyucca.yumebox.runtime.api.RuntimeOwner
import com.github.yumeyucca.yumebox.runtime.api.RuntimePhase
import com.github.yumeyucca.yumebox.runtime.api.VpnPermissionRequired
import com.github.yumeyucca.yumebox.runtime.client.ProfilesRepository
import com.github.yumeyucca.yumebox.runtime.client.ProxyFacade
import com.github.yumeyucca.yumebox.runtime.client.ProxyGroupSyncPriority
import com.github.yumeyucca.yumebox.runtime.client.ProxyGroupSyncSource
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber

class HomeViewModel(
    application: Application,
    private val proxyFacade: ProxyFacade,
    private val profilesRepository: ProfilesRepository,
    private val networkSettingsStore: NetworkSettingsStore,
) : AndroidContractStateViewModel<HomeUiState, HomeUiEffect>(application, HomeUiState()) {
    /** Dedupes Failed-phase toasts against the same runtime generation / start attempt. */
    private var lastPresentedFailureGeneration: Long = -1L
    private var suppressNextRuntimeFailureToast: Boolean = false

    private val _homeProfiles = MutableStateFlow<HomeProfiles?>(null)

    val runtimeSnapshot = proxyFacade.runtimeSnapshot
    val isRunning =
        runtimeSnapshot
            .map { it.running }
            .stateInWhileSubscribed(viewModelScope, runtimeSnapshot.value.running)
    val isRemoteController: StateFlow<Boolean> =
        runtimeSnapshot
            .map { it.owner == RuntimeOwner.RemoteController }
            .stateInWhileSubscribed(
                viewModelScope,
                runtimeSnapshot.value.owner == RuntimeOwner.RemoteController,
            )
    val currentProfile = proxyFacade.currentProfile
    val trafficNow = proxyFacade.trafficNow
    val trafficTotal = proxyFacade.trafficTotal
    val nodeSession = proxyFacade.nodeSession

    private val _startAttempt = MutableStateFlow<StartAttempt>(StartAttempt.None)

    // A consent request outlives the screen: [MainScreen] is disposed as soon as another route is
    // pushed, and a dropped request left the connect flow waiting forever with no dialog and no
    // error. It stays pending until the screen reads it and calls [consumeVpnPrepareIntent].
    private val _vpnPrepareIntent = MutableStateFlow<Intent?>(null)
    val vpnPrepareIntent: StateFlow<Intent?> = _vpnPrepareIntent.asStateFlow()

    val controlState: StateFlow<HomeProxyControlState> =
        combine(runtimeSnapshot, _startAttempt) { snapshot, attempt ->
            resolveHomeControlState(snapshot.owner, snapshot.phase, attempt)
        }
            .stateInWhileSubscribed(
                viewModelScope,
                resolveHomeControlState(
                    runtimeSnapshot.value.owner,
                    runtimeSnapshot.value.phase,
                    _startAttempt.value,
                ),
            )

    private var reconcileJob: Job? = null

    private val mainProxyNode: StateFlow<Proxy?> = proxyFacade.resolvedPrimaryNode

    val selectedServerName: StateFlow<String?> =
        mainProxyNode.map { it?.name }.stateInWhileSubscribed(viewModelScope, null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedServerPing: StateFlow<Int?> =
        mainProxyNode
            .flatMapLatest { node ->
                if (node == null) {
                    flowOf(null)
                } else {
                    proxyFacade.proxyDelayFlow(node.name)
                        .map { liveDelay -> (liveDelay ?: node.delay).takeIf { it > 0 } }
                }
            }
            .stateInWhileSubscribed(viewModelScope, null)

    val screenState: StateFlow<HomeScreenState> =
        combine(
            combine(
                controlState,
                trafficNow,
                _homeProfiles,
            ) { control, traffic, homeProfiles ->
                HomeScreenState(
                    controlState = control,
                    trafficNow = traffic,
                    profiles = homeProfiles?.all ?: emptyList(),
                    profilesLoaded = homeProfiles != null,
                    hasEnabledProfile = homeProfiles?.hasEnabled ?: false,
                    recommendedProfile = homeProfiles?.recommended,
                )
            },
            combine(
                currentProfile,
                selectedServerName,
                selectedServerPing,
            ) { current, serverName, serverPing ->
                HomeProfileSummary(
                    currentProfile = current,
                    selectedServerName = serverName,
                    selectedServerPing = serverPing,
                )
            },
            combine(
                isRemoteController,
                uiState,
            ) { remote, ui ->
                HomeRuntimeSummary(
                    isRemoteController = remote,
                    uiState = ui,
                )
            },
        ) { base, profile, runtime ->
            base.copy(
                currentProfile = profile.currentProfile,
                selectedServerName = profile.selectedServerName,
                selectedServerPing = profile.selectedServerPing,
                isRemoteController = runtime.isRemoteController,
                uiMessage = runtime.uiState.message,
                uiError = runtime.uiState.error,
            )
        }
            .combine(runtimeSnapshot) { screen, snapshot ->
                screen.copy(runtimeStartedAt = snapshot.startedAt)
            }
            .stateInWhileSubscribed(viewModelScope, HomeScreenState())

    init {
        refreshProfiles()
        reconcileRuntimeState()
        observeRuntimeState()
        observeRuntimeFailures()
        observeProfileChanges()
    }

    // Fault barrier: repository failure only logs and marks profiles as loaded (CE rethrown).
    @Suppress("TooGenericExceptionCaught")
    private fun refreshProfiles() {
        viewModelScope.launch {
            _homeProfiles.value =
                try {
                    val allProfiles = profilesRepository.queryAllProfiles()
                    val active = profilesRepository.queryActiveProfile()
                    HomeProfiles(all = allProfiles, recommended = active)
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    Timber.e(error, "Failed to refresh profiles")
                    // A failed load must still end up loaded: keep the last good snapshot, or
                    // an empty one on the very first load, so the screen never sticks on the
                    // skeleton.
                    _homeProfiles.value ?: HomeProfiles(all = emptyList(), recommended = null)
                }
        }
    }

    private fun observeProfileChanges() {
        viewModelScope.launch {
            proxyFacade.currentProfile
                .map { it?.uuid }
                .distinctUntilChanged()
                .collect { refreshProfiles() }
        }
    }

    private fun observeRuntimeState() {
        viewModelScope.launch {
            runtimeSnapshot
                .map { it.phase }
                .distinctUntilChanged()
                .collect { phase ->
                    when (phase) {
                        // Keep the pending launch marker until Running. Some launch paths report
                        // Starting and then briefly fall back to Idle while the service/core is
                        // being brought up; clearing it here makes the UI flash back to Start.
                        RuntimePhase.Starting -> Unit

                        RuntimePhase.Running ->
                            if (_startAttempt.value.isStartup) {
                                _startAttempt.value = StartAttempt.None
                            }

                        RuntimePhase.Stopping ->
                            if (_startAttempt.value == StartAttempt.Stopping) {
                                _startAttempt.value = StartAttempt.None
                            }

                        // The launcher sets the pending transition before requesting the service.
                        // Keep that intermediate state while the runtime still reports Idle;
                        // otherwise the button briefly jumps back to Start until the kernel
                        // eventually reports Starting.
                        RuntimePhase.Idle ->
                            if (!_startAttempt.value.isStartup) resetPendingStart()

                        RuntimePhase.Failed -> resetPendingStart()
                    }
                }
        }
    }

    private fun observeRuntimeFailures() {
        viewModelScope.launch {
            runtimeSnapshot
                .drop(1)
                .map { snapshot -> Triple(snapshot.phase, snapshot.lastError, snapshot.generation) }
                .distinctUntilChanged()
                .collect { (phase, lastError, generation) ->
                    if (phase != RuntimePhase.Failed || lastError.isNullOrBlank()) {
                        // Leaving the Failed phase cancels a pending suppression: it only exists to
                        // dedupe the failure the start path already reported, so it must not
                        // outlive that failure and swallow a later, unrelated one.
                        suppressNextRuntimeFailureToast = false
                        return@collect
                    }
                    if (suppressNextRuntimeFailureToast) {
                        suppressNextRuntimeFailureToast = false
                        lastPresentedFailureGeneration = generation
                        return@collect
                    }
                    if (generation == lastPresentedFailureGeneration) {
                        return@collect
                    }
                    lastPresentedFailureGeneration = generation
                    showError(readableStartupError(lastError))
                }
        }
    }

    fun setHomeScreenActive(isActive: Boolean) {
        proxyFacade.setProxyGroupSyncPriority(
            priority = if (isActive) ProxyGroupSyncPriority.FAST else ProxyGroupSyncPriority.OFF,
            source = ProxyGroupSyncSource.Home,
        )
    }

    fun reconcileRuntimeState() {
        if (reconcileJob?.isActive == true) return
        // Off-main: reconcile touches the persisted runtime state (MMKV) and the active-profile
        // query; keeping it off the main thread avoids hitching navigation.
        reconcileJob =
            viewModelScope.launch(Dispatchers.IO) {
                runCatching {
                    proxyFacade.reconcileRuntimeState()
                    refreshProfiles()
                }
                    .onFailure { error ->
                        if (error is CancellationException) throw error
                        Timber.w(error, "Failed to reconcile runtime state for home")
                    }
            }
    }

    fun isCurrentProfile(profileId: UUID): Boolean = currentProfile.value?.uuid == profileId

    fun startProxy(profileId: String, mode: RunMode? = null) {
        if (controlState.value != HomeProxyControlState.Idle) return

        val request =
            PendingStartRequest(
                profileId = profileId,
                mode = mode ?: networkSettingsStore.runMode.value,
            )
        _startAttempt.value = StartAttempt.Starting(request)

        viewModelScope.launch { startProxyInternal(request) }
    }

    /** Clears the consent request once the screen has shown it. */
    fun consumeVpnPrepareIntent() {
        _vpnPrepareIntent.value = null
    }

    fun onVpnPermissionResult(granted: Boolean) {
        val request = (_startAttempt.value as? StartAttempt.AwaitingPermission)?.request ?: return

        if (!granted) {
            resetPendingStart()
            return
        }

        _startAttempt.value = StartAttempt.Starting(request)
        viewModelScope.launch { startProxyInternal(request) }
    }

    // Fault barrier: any stop failure is surfaced as UI error state (CE rethrown).
    @Suppress("TooGenericExceptionCaught")
    suspend fun stopProxy() {
        if (controlState.value != HomeProxyControlState.Running) return

        _startAttempt.value = StartAttempt.Stopping

        try {
            withContext(Dispatchers.IO) {
                AutoStartSessionGate.markManualPaused()
                proxyFacade.stopProxy()
            }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            _startAttempt.value = StartAttempt.None
            Timber.e(error, "Failed to stop proxy")
            showError(YumeTxt.Home.Message.StopFailed.format(error.message))
        }
    }

    @Suppress("UnusedSymbol")
    private fun showMessage(message: String) =
        postMessage(message, HomeUiEffect.ShowMessage(message))

    private fun showError(error: String) = postError(error, HomeUiEffect.ShowError(error))

    fun consumeMessage() = clearMessageState()

    fun consumeError() = clearErrorState()

    // Fault barrier: any start failure is surfaced as UI error state (CE rethrown).
    @Suppress("TooGenericExceptionCaught")
    private suspend fun startProxyInternal(request: PendingStartRequest) {
        val startedAt = System.currentTimeMillis()
        try {
            Timber.d("Home startProxy kickoff: mode=${request.mode} profileId=${request.profileId}")

            withContext(Dispatchers.IO) {
                if (request.profileId.isNotBlank()) {
                    profilesRepository.setActiveProfile(UUID.fromString(request.profileId))
                }

                AutoStartSessionGate.clearManualPaused()
                proxyFacade.startProxy(request.mode)
            }

            Timber.i(
                "Home startProxy completed in ${System.currentTimeMillis() - startedAt}ms, mode=${request.mode}"
            )

            // With a remote controller active ProxyFacade.startProxy() is a no-op, so no runtime
            // phase change will arrive to clear the pending transition; clear it here instead of
            // leaving the home button stuck on "Connecting".
            if (proxyFacade.isRemoteControllerActive()) {
                resetPendingStart()
            }
        } catch (error: BuiltinGeoAssetsRequiredException) {
            resetPendingStart()
            showError(YumeTxt.Home.Message.BuiltinGeoRequired)
        } catch (error: VpnPermissionRequired) {
            _startAttempt.value = StartAttempt.AwaitingPermission(request)
            _vpnPrepareIntent.value = error.intent
            Timber.i("VPN permission required")
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            resetPendingStart()
            Timber.e(error, "Failed to start proxy")
            // startProxyInternal already surfaces the failure; skip the duplicate Failed-phase toast.
            suppressNextRuntimeFailureToast = true
            lastPresentedFailureGeneration = runtimeSnapshot.value.generation
            showError(
                YumeTxt.Home.Message.StartFailed.format(
                    readableStartupError(error.message)
                )
            )
        }
    }

    private fun resetPendingStart() {
        _startAttempt.value = StartAttempt.None
    }

    /**
     * Intermediate bundle for the screen-state assembly: `combine` takes at most five flows, so the
     * inputs are folded in three groups before the final merge.
     */
    private data class HomeProfileSummary(
        val currentProfile: Profile?,
        val selectedServerName: String?,
        val selectedServerPing: Int?,
    )

    private data class HomeRuntimeSummary(
        val isRemoteController: Boolean,
        val uiState: HomeUiState,
    )
}
