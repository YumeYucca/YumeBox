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

@file:Suppress("UnusedSymbol", "CanBeParameter")

package com.github.yumeyucca.yumebox.screen.home

import android.app.Application
import android.content.Intent
import androidx.lifecycle.viewModelScope
import com.github.yumeyucca.yumebox.common.util.stateInWhileSubscribed
import com.github.yumeyucca.yumebox.core.model.RunMode
import com.github.yumeyucca.yumebox.core.presentation.AndroidContractStateViewModel
import com.github.yumeyucca.yumebox.core.util.AutoStartSessionGate
import com.github.yumeyucca.yumebox.core.util.PollingTimerSpecs
import com.github.yumeyucca.yumebox.core.util.PollingTimers
import com.github.yumeyucca.yumebox.data.network.IpMonitoringState
import com.github.yumeyucca.yumebox.data.network.NetworkInfoService
import com.github.yumeyucca.yumebox.data.store.NetworkSettingsStore
import com.github.yumeyucca.yumebox.domain.model.TrafficData
import com.github.yumeyucca.yumebox.runtime.api.Profile
import com.github.yumeyucca.yumebox.runtime.api.RuntimeOwner
import com.github.yumeyucca.yumebox.runtime.api.RuntimePhase
import com.github.yumeyucca.yumebox.runtime.client.ProfilesRepository
import com.github.yumeyucca.yumebox.runtime.client.ProxyFacade
import com.github.yumeyucca.yumebox.runtime.client.ProxyGroupSyncPriority
import com.github.yumeyucca.yumebox.runtime.client.RuntimeStateMapper
import com.github.yumeyucca.yumebox.runtime.service.WifiAutomationService
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import com.github.yumeyucca.yumebox.core.locale.YumeTxt
import com.github.yumeyucca.yumebox.core.util.BuiltinGeoAssetsRequiredException
import timber.log.Timber

class HomeViewModel(
    application: Application,
    private val proxyFacade: ProxyFacade,
    private val profilesRepository: ProfilesRepository,
    private val networkInfoService: NetworkInfoService,
    private val networkSettingsStore: NetworkSettingsStore,
    private val remoteControllerStore: com.github.yumeyucca.yumebox.data.store.RemoteControllerStore,
) :
    AndroidContractStateViewModel<HomeUiState, HomeUiEffect>(
        application,
        HomeUiState(),
    ) {
    /** Dedupes Failed-phase toasts against the same runtime generation / start attempt. */
    private var lastPresentedFailureGeneration: Long = -1L
    private var suppressNextRuntimeFailureToast: Boolean = false

    private val _profiles = MutableStateFlow<List<Profile>>(emptyList())
    val profiles: StateFlow<List<Profile>> = _profiles.asStateFlow()

    private val _recommendedProfile = MutableStateFlow<Profile?>(null)
    val recommendedProfile: StateFlow<Profile?> = _recommendedProfile.asStateFlow()

    private val _profilesLoaded = MutableStateFlow(false)
    val profilesLoaded: StateFlow<Boolean> = _profilesLoaded.asStateFlow()

    val hasEnabledProfile: StateFlow<Boolean> =
        profiles
            .map { list -> list.any { it.active } }
            .stateInWhileSubscribed(viewModelScope, false)

    val runtimeSnapshot = proxyFacade.runtimeSnapshot
    val isRunning =
        runtimeSnapshot
            .map(RuntimeStateMapper::isActuallyRunning)
            .stateInWhileSubscribed(
                viewModelScope,
                RuntimeStateMapper.isActuallyRunning(runtimeSnapshot.value),
            )
    val isRemoteController: StateFlow<Boolean> =
        runtimeSnapshot
            .map { it.owner == RuntimeOwner.RemoteController }
            .stateInWhileSubscribed(
                viewModelScope,
                runtimeSnapshot.value.owner == RuntimeOwner.RemoteController,
            )
    val isRemoteControllerMode: StateFlow<Boolean> =
        combine(
            remoteControllerStore.controllerEnabled.state,
            remoteControllerStore.activeBackendId.state,
            remoteControllerStore.backends.state,
        ) { enabled, activeBackendId, backends ->
            enabled && backends.any { it.id == activeBackendId }
        }
            .stateInWhileSubscribed(
                viewModelScope,
                remoteControllerStore.controllerEnabled.value &&
                        remoteControllerStore.activeBackend() != null,
            )
    val isConfigReloading: StateFlow<Boolean> = proxyFacade.isConfigReloading
    val controllerBackendName: StateFlow<String?> =
        combine(
            remoteControllerStore.activeBackendId.state,
            remoteControllerStore.backends.state,
        ) { id, list ->
            list
                .firstOrNull { it.id == id }
                ?.let { it.name.ifBlank { "${it.host}:${it.port}" } }
        }
            .stateInWhileSubscribed(viewModelScope, null)
    val currentProfile = proxyFacade.currentProfile
    val trafficNow = proxyFacade.trafficNow
    val proxyGroups = proxyFacade.proxyGroups
    val nodeSession = proxyFacade.nodeSession

    private val _proxyMode = MutableStateFlow(RunMode.VpnService)
    val proxyMode: StateFlow<RunMode> = _proxyMode.asStateFlow()

    private val _pendingTransition = MutableStateFlow(PendingTransition.None)
    private var pendingStartRequest: PendingStartRequest? = null

    private val _vpnPrepareIntent =
        MutableSharedFlow<Intent>(
            replay = 0,
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val vpnPrepareIntent = _vpnPrepareIntent.asSharedFlow()

    val controlState: StateFlow<HomeProxyControlState> =
        combine(runtimeSnapshot, _pendingTransition) { snapshot, pendingTransition ->
            resolveHomeControlState(snapshot.owner, snapshot.phase, pendingTransition)
        }
            .stateInWhileSubscribed(
                viewModelScope,
                resolveHomeControlState(
                    runtimeSnapshot.value.owner,
                    runtimeSnapshot.value.phase,
                    _pendingTransition.value,
                ),
            )

    private val _speedHistory = MutableStateFlow<List<Long>>(emptyList())
    val speedHistory: StateFlow<List<Long>> = _speedHistory.asStateFlow()
    private var reconcileJob: Job? = null

    private val mainProxyNode: StateFlow<com.github.yumeyucca.yumebox.core.model.Proxy?> =
        proxyFacade.resolvedPrimaryNode

    val selectedServerName: StateFlow<String?> =
        mainProxyNode.map { it?.name }.stateInWhileSubscribed(viewModelScope, null)

    val selectedServerPing: StateFlow<Int?> =
        mainProxyNode
            .map { node -> node?.delay?.takeIf { delay -> delay > 0 } }
            .stateInWhileSubscribed(viewModelScope, null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val ipMonitoringState: StateFlow<IpMonitoringState> =
        isRunning
            .flatMapLatest { running ->
                if (running) {
                    networkInfoService.startIpMonitoring(
                        isProxyActiveFlow = isRunning,
                        externalRefreshFlow =
                            PollingTimers.ticks(PollingTimerSpecs.HomeIpRefresh).map { Unit },
                    )
                } else {
                    flowOf(IpMonitoringState.Loading)
                }
            }
            .stateInWhileSubscribed(viewModelScope, IpMonitoringState.Loading)

    val screenState: StateFlow<HomeScreenState> =
        combine(
            combine(controlState, trafficNow, profiles, profilesLoaded, hasEnabledProfile) { control,
                                                                                             traffic,
                                                                                             profileList,
                                                                                             loaded,
                                                                                             hasEnabled ->
                HomeScreenState(
                    controlState = control,
                    trafficNow = traffic,
                    profiles = profileList,
                    profilesLoaded = loaded,
                    hasEnabledProfile = hasEnabled,
                )
            },
            combine(
                recommendedProfile,
                currentProfile,
                selectedServerName,
                selectedServerPing,
                speedHistory,
            ) { recommended, current, serverName, serverPing, history ->
                HomeProfileSummary(
                    recommendedProfile = recommended,
                    currentProfile = current,
                    selectedServerName = serverName,
                    selectedServerPing = serverPing,
                    speedHistory = history,
                )
            },
            combine(
                proxyMode,
                isRemoteController,
                controllerBackendName,
                ipMonitoringState,
                uiState,
            ) { mode, remote, backendName, ipState, ui ->
                HomeRuntimeSummary(
                    proxyMode = mode,
                    isRemoteController = remote,
                    controllerBackendName = backendName,
                    ipMonitoringState = ipState,
                    uiState = ui,
                )
            },
        ) { base, profile, runtime ->
            base.copy(
                recommendedProfile = profile.recommendedProfile,
                currentProfile = profile.currentProfile,
                selectedServerName = profile.selectedServerName,
                selectedServerPing = profile.selectedServerPing,
                speedHistory = profile.speedHistory,
                proxyMode = runtime.proxyMode,
                isRemoteController = runtime.isRemoteController,
                controllerBackendName = runtime.controllerBackendName,
                ipMonitoringState = runtime.ipMonitoringState,
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
        observeControlState()
        observeRuntimeState()
        observeRuntimeFailures()
        syncProxyModeState()
        startSpeedSampling()
        observeProfileChanges()
    }

    // Fault barrier: repository failure only logs and marks profiles as loaded (CE rethrown).
    @Suppress("TooGenericExceptionCaught")
    private fun refreshProfiles() {
        viewModelScope.launch {
            try {
                val allProfiles = profilesRepository.queryAllProfiles()
                val active = profilesRepository.queryActiveProfile()
                _profiles.value = allProfiles
                _recommendedProfile.value = active
                _profilesLoaded.value = true
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                Timber.e(error, "Failed to refresh profiles")
                _profilesLoaded.value = true
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

    private fun observeControlState() {
        viewModelScope.launch {
            controlState.collect { state ->
                if (state != HomeProxyControlState.Running) {
                    _speedHistory.value = List(24) { 0L }
                }
                _uiState.update {
                    it.copy(
                        isStartingProxy = state == HomeProxyControlState.Connecting,
                        loadingProgress =
                            if (state == HomeProxyControlState.Connecting) {
                                YumeTxt.Home.Message.Preparing
                            } else {
                                null
                            },
                    )
                }
            }
        }
    }

    private fun observeRuntimeState() {
        viewModelScope.launch {
            runtimeSnapshot
                .map { it.phase }
                .distinctUntilChanged()
                .collect { phase ->
                    when (phase) {
                        RuntimePhase.Starting -> {
                            clearPendingStart()
                            // Keep the pending launch marker until Running. Some launch paths
                            // report Starting and then briefly fall back to Idle while the
                            // service/core is being brought up; clearing it here makes the UI
                            // flash back to Start before the next runtime update.
                        }

                        RuntimePhase.Running -> {
                            clearPendingStart()
                            if (
                                _pendingTransition.value == PendingTransition.Starting ||
                                _pendingTransition.value == PendingTransition.AwaitingPermission
                            ) {
                                _pendingTransition.value = PendingTransition.None
                            }
                        }

                        RuntimePhase.Stopping -> {
                            clearPendingStart()
                            if (_pendingTransition.value == PendingTransition.Stopping) {
                                _pendingTransition.value = PendingTransition.None
                            }
                        }

                        RuntimePhase.Idle -> {
                            // The launcher sets the pending transition before requesting the
                            // service. Keep that intermediate state while the runtime still
                            // reports Idle; otherwise the button briefly jumps back to Start
                            // until the kernel eventually reports Starting.
                            if (
                                _pendingTransition.value != PendingTransition.Starting &&
                                    _pendingTransition.value != PendingTransition.AwaitingPermission
                            ) {
                                clearPendingStart()
                                _pendingTransition.value = PendingTransition.None
                            }
                        }

                        RuntimePhase.Failed -> {
                            clearPendingStart()
                            _pendingTransition.value = PendingTransition.None
                        }
                    }
                }
        }
    }

    private fun syncProxyModeState() {
        viewModelScope.launch {
            runtimeSnapshot
                .map {
                    RuntimeStateMapper.resolveDisplayMode(it, networkSettingsStore.runMode.value)
                }
                .distinctUntilChanged()
                .collect { refreshProxyMode() }
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

    fun refreshProxyMode() {
        val configuredMode = networkSettingsStore.runMode.value
        _proxyMode.value =
            RuntimeStateMapper.resolveDisplayMode(runtimeSnapshot.value, configuredMode)
    }

    fun setHomeScreenActive(isActive: Boolean) {
        proxyFacade.setProxyGroupSyncPriority(
            priority = if (isActive) ProxyGroupSyncPriority.FAST else ProxyGroupSyncPriority.OFF,
            source = "home",
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
                    refreshProxyMode()
                }
                    .onFailure { error ->
                        if (error is CancellationException) throw error
                        Timber.w(error, "Failed to reconcile runtime state for home")
                    }
            }
    }

    // Fault barrier: any reload failure is surfaced as UI error state (CE rethrown).
    @Suppress("TooGenericExceptionCaught")
    suspend fun reloadProfile() {
        try {
            applyLoading(true)

            val activeProfile = profilesRepository.queryActiveProfile()
            if (activeProfile == null) {
                showError(
                    YumeTxt.Home.Message.ConfigSwitchFailed.format(
                        YumeTxt.ProfilesVM.Error.ProfileNotExist
                    )
                )
                return
            }

            profilesRepository.updateProfile(activeProfile.uuid)

            profilesRepository.setActiveProfile(activeProfile.uuid)
            showMessage(YumeTxt.Home.Message.ConfigSwitched)
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            Timber.e(error, "Failed to reload profile")
            showError(YumeTxt.Home.Message.ConfigSwitchFailed.format(error.message))
        } finally {
            applyLoading(false)
        }
    }

    fun isCurrentProfile(profileId: java.util.UUID): Boolean =
        currentProfile.value?.uuid == profileId

    fun startProxy(profileId: String, mode: RunMode? = null) {
        if (!controlState.value.canInteract || controlState.value != HomeProxyControlState.Idle) {
            return
        }

        val request =
            PendingStartRequest(
                profileId = profileId,
                mode = mode ?: networkSettingsStore.runMode.value,
            )
        pendingStartRequest = request
        _pendingTransition.value = PendingTransition.Starting

        viewModelScope.launch { startProxyInternal(request) }
    }

    fun onVpnPermissionResult(granted: Boolean) {
        val request = pendingStartRequest ?: return
        if (_pendingTransition.value != PendingTransition.AwaitingPermission) return

        if (!granted) {
            clearPendingStart()
            _pendingTransition.value = PendingTransition.None
            refreshProxyMode()
            return
        }

        _pendingTransition.value = PendingTransition.Starting
        viewModelScope.launch { startProxyInternal(request) }
    }

    // Fault barrier: any stop failure is surfaced as UI error state (CE rethrown).
    @Suppress("TooGenericExceptionCaught")
    suspend fun stopProxy() {
        if (
            !controlState.value.canInteract || controlState.value != HomeProxyControlState.Running
        ) {
            return
        }

        _pendingTransition.value = PendingTransition.Stopping

        try {
            withContext(Dispatchers.IO) {
                AutoStartSessionGate.markManualPaused()
                proxyFacade.stopProxy()
            }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            _pendingTransition.value = PendingTransition.None
            Timber.e(error, "Failed to stop proxy")
            showError(YumeTxt.Home.Message.StopFailed.format(error.message))
        }
    }

    private fun startSpeedSampling(sampleLimit: Int = 24) {
        viewModelScope.launch {
            PollingTimers.ticks(PollingTimerSpecs.HomeSpeedSampling).collect {
                val snapshot = runtimeSnapshot.value
                val sample =
                    when {
                        snapshot.phase == RuntimePhase.Idle ||
                                snapshot.phase == RuntimePhase.Failed -> 0L

                        snapshot.phase.running -> {
                            val t = proxyFacade.trafficNow.value
                            val d = TrafficData.from(t)
                            (d.upload + d.download).coerceAtLeast(0L)
                        }

                        else -> 0L
                    }
                _speedHistory.update { history ->
                    appendSpeedSample(history, sample, sampleLimit)
                }
            }
        }
    }

    private fun applyLoading(loading: Boolean) = super.setLoading(loading)

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
            _proxyMode.value = request.mode
            Timber.d("Home startProxy kickoff: mode=${request.mode} profileId=${request.profileId}")

            withContext(Dispatchers.IO) {
                if (request.profileId.isNotBlank()) {
                    profilesRepository.setActiveProfile(
                        java.util.UUID.fromString(request.profileId)
                    )
                }

                AutoStartSessionGate.clearManualPaused()
                proxyFacade.startProxy(request.mode)
            }

            Timber.i(
                "Home startProxy completed in ${System.currentTimeMillis() - startedAt}ms, mode=${request.mode}"
            )

            // If the remote controller is active, ProxyFacade.startProxy() is a no-op and no
            // runtime
            // phase change will arrive to reset the pending transition — clear it so the home
            // button
            // doesn't stick on "Connecting" forever.
            if (proxyFacade.isRemoteControllerActive()) {
                clearPendingStart()
                _pendingTransition.value = PendingTransition.None
            }
        } catch (error: BuiltinGeoAssetsRequiredException) {
            clearPendingStart()
            _pendingTransition.value = PendingTransition.None
            showError(YumeTxt.Home.Message.BuiltinGeoRequired)
        } catch (error: com.github.yumeyucca.yumebox.runtime.api.VpnPermissionRequired) {
            _pendingTransition.value = PendingTransition.AwaitingPermission
            _vpnPrepareIntent.emit(error.intent)
            Timber.i("VPN permission required")
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            clearPendingStart()
            _pendingTransition.value = PendingTransition.None
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

    private fun readableStartupError(raw: String?): String {
        val message = raw?.trim().orEmpty()
        if (message.isEmpty()) {
            return YumeTxt.Util.Error.UnknownError
        }
        if (message.equals("runtime backend unavailable", ignoreCase = true)) {
            return YumeTxt.Home.Message.RuntimeUnavailable
        }
        // Prefer the first actionable line; strip huge core.log tails and stack noise.
        val firstLine =
            message
                .lineSequence()
                .map { it.trim() }
                .firstOrNull { it.isNotEmpty() }
                ?: message
        val cleaned =
            firstLine
                .removePrefix("java.lang.IllegalStateException:")
                .removePrefix("java.lang.IllegalArgumentException:")
                .removePrefix("java.lang.RuntimeException:")
                .trim()
        return cleaned.take(280).ifBlank { YumeTxt.Util.Error.UnknownError }
    }

    private fun clearPendingStart() {
        pendingStartRequest = null
    }

    private data class PendingStartRequest(
        val profileId: String,
        val mode: RunMode,
    )
}
