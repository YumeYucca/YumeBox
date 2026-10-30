/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 */

package com.github.yumeyucca.yumebox.screen.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumeyucca.yumebox.common.util.stateInWhileSubscribed
import com.github.yumeyucca.yumebox.data.model.RunMode
import com.github.yumeyucca.yumebox.data.model.WifiAutomationAction
import com.github.yumeyucca.yumebox.data.model.WifiAutomationFallbackAction
import com.github.yumeyucca.yumebox.data.model.WifiAutomationRule
import com.github.yumeyucca.yumebox.data.store.NetworkSettingsStore
import com.github.yumeyucca.yumebox.runtime.api.Profile
import com.github.yumeyucca.yumebox.runtime.client.ProfilesRepository
import com.github.yumeyucca.yumebox.runtime.service.WifiAutomationService
import com.github.yumeyucca.yumebox.runtime.service.session.WifiSsidObservation
import com.github.yumeyucca.yumebox.runtime.service.session.WifiSsidObserver
import com.github.yumeyucca.yumebox.runtime.service.session.WifiSsidScanResult
import com.github.yumeyucca.yumebox.runtime.service.session.WifiSsidScanner
import com.github.yumeyucca.yumebox.runtime.service.session.WifiSsidNetwork
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Scan lifecycle of the manual-add sheet.
 *
 * [Results] carries the networks it renders so the list can never drift from the phase it belongs
 * to; [Idle] is the state before the first scan finishes, where the sheet shows nothing but its
 * chrome.
 */
sealed interface WifiScanPhase {
    data object Idle : WifiScanPhase

    data object Scanning : WifiScanPhase

    data object Unavailable : WifiScanPhase

    data object Empty : WifiScanPhase

    data class Results(val networks: List<WifiSsidNetwork>) : WifiScanPhase
}

class WifiAutomationViewModel(
    application: Application,
    private val settings: NetworkSettingsStore,
    private val profilesRepository: ProfilesRepository,
) : AndroidViewModel(application) {
    data class UiState(
        val enabled: Boolean = false,
        val rules: List<WifiAutomationRule> = emptyList(),
        val profiles: List<Profile> = emptyList(),
        val runMode: RunMode = RunMode.VpnService,
        val locationRequested: Boolean = false,
        val otherWifiAction: WifiAutomationFallbackAction = WifiAutomationFallbackAction.Keep,
        val noWifiAction: WifiAutomationFallbackAction = WifiAutomationFallbackAction.Keep,
        val otherWifiProfileUuid: String? = null,
        val noWifiProfileUuid: String? = null,
        val scanPhase: WifiScanPhase = WifiScanPhase.Idle,
    )

    sealed interface Effect {
        data object AddedCurrentSsid : Effect

        data object SsidAlreadyExists : Effect

        data object NoWifi : Effect

        data object SsidUnavailable : Effect

        data object ScanUnavailable : Effect
    }

    private val scanPhase = MutableStateFlow<WifiScanPhase>(WifiScanPhase.Idle)
    private var scanJob: Job? = null

    private val profiles = MutableStateFlow<List<Profile>>(emptyList())

    init {
        refreshProfiles()
    }

    private val settingsState =
        combine(
            settings.wifiAutomationEnabled.state,
            settings.wifiAutomationRules.state,
            settings.runMode.state,
            settings.wifiAutomationLocationRequested.state,
        ) { enabled, rules, runMode, locationRequested ->
            UiState(
                enabled = enabled,
                rules = rules,
                runMode = runMode,
                locationRequested = locationRequested,
            )
        }

    val uiState: StateFlow<UiState> =
        combine(
            settingsState,
            settings.wifiAutomationOtherWifiAction.state,
            settings.wifiAutomationNoWifiAction.state,
            profiles,
        ) { state, otherWifiAction, noWifiAction, profiles ->
            state.copy(
                otherWifiAction = otherWifiAction,
                noWifiAction = noWifiAction,
                profiles = profiles,
            )
        }
            .combine(settings.wifiAutomationOtherWifiProfileUuid.state) { state, profileUuid ->
                state.copy(otherWifiProfileUuid = profileUuid.ifBlank { null })
            }
            .combine(settings.wifiAutomationNoWifiProfileUuid.state) { state, profileUuid ->
                state.copy(noWifiProfileUuid = profileUuid.ifBlank { null })
            }
            .combine(scanPhase) { state, phase -> state.copy(scanPhase = phase) }
            .stateInWhileSubscribed(
                viewModelScope,
                UiState(
                    enabled = settings.wifiAutomationEnabled.value,
                    rules = settings.wifiAutomationRules.value,
                    runMode = settings.runMode.value,
                    locationRequested = settings.wifiAutomationLocationRequested.value,
                    otherWifiAction = settings.wifiAutomationOtherWifiAction.value,
                    noWifiAction = settings.wifiAutomationNoWifiAction.value,
                    otherWifiProfileUuid = settings.wifiAutomationOtherWifiProfileUuid.value.ifBlank { null },
                    noWifiProfileUuid = settings.wifiAutomationNoWifiProfileUuid.value.ifBlank { null },
                ),
            )

    // Buffered, so an effect raised while the screen is not collecting parks here instead of
    // suspending the coroutine that raised it.
    private val _effects =
        MutableSharedFlow<Effect>(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val effects = _effects.asSharedFlow()

    fun enable() {
        if (settings.runMode.value != RunMode.VpnService) return
        settings.wifiAutomationEnabled.set(true)
        WifiAutomationService.start(getApplication())
    }

    fun disable() {
        settings.wifiAutomationEnabled.set(false)
        WifiAutomationService.stop(getApplication())
    }

    fun markLocationPermissionRequested() {
        settings.wifiAutomationLocationRequested.set(true)
    }

    @Suppress("TooGenericExceptionCaught")
    fun refreshProfiles() {
        viewModelScope.launch {
            try {
                profiles.value = profilesRepository.queryAllProfiles()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                Timber.w(error, "Failed to load profiles for Wi-Fi automation")
            }
        }
    }

    fun addManualSsid(
        rawSsid: String,
        action: WifiAutomationAction,
        profileUuid: String?,
    ) {
        val ssid = WifiSsidObserver.normalizeSsid(rawSsid) ?: return
        addRule(ssid, action, profileUuid)
    }

    fun scanWifi() {
        if (scanPhase.value == WifiScanPhase.Scanning) return
        scanPhase.value = WifiScanPhase.Scanning
        scanJob = viewModelScope.launch {
            when (val result = WifiSsidScanner.scanOnce(getApplication())) {
                is WifiSsidScanResult.Success -> {
                    scanPhase.value =
                        if (result.networks.isEmpty()) {
                            WifiScanPhase.Empty
                        } else {
                            WifiScanPhase.Results(result.networks)
                        }
                }

                WifiSsidScanResult.Unavailable -> {
                    scanPhase.value = WifiScanPhase.Unavailable
                    _effects.emit(Effect.ScanUnavailable)
                }
            }
        }
    }

    fun resetScan() {
        scanJob?.cancel()
        scanJob = null
        scanPhase.value = WifiScanPhase.Idle
    }

    fun addCurrentSsid() {
        viewModelScope.launch {
            when (val result = WifiSsidObserver.readOnce(getApplication())) {
                is WifiSsidObservation.Connected -> addRule(result.ssid)
                WifiSsidObservation.NoWifi -> _effects.emit(Effect.NoWifi)
                WifiSsidObservation.Unavailable -> _effects.emit(Effect.SsidUnavailable)
            }
        }
    }

    fun changeRuleAction(ssid: String, action: WifiAutomationAction, profileUuid: String?) {
        settings.wifiAutomationRules.set(
            settings.wifiAutomationRules.value.map { rule ->
                if (rule.ssid == ssid) rule.copy(action = action, profileUuid = profileUuid) else rule
            }
        )
        refreshAutomationIfEnabled()
    }

    fun removeRule(ssid: String) {
        settings.wifiAutomationRules.set(settings.wifiAutomationRules.value.filterNot { it.ssid == ssid })
        refreshAutomationIfEnabled()
    }

    fun changeOtherWifiAction(action: WifiAutomationFallbackAction) {
        settings.wifiAutomationOtherWifiAction.set(action)
        refreshAutomationIfEnabled()
    }

    fun changeNoWifiAction(action: WifiAutomationFallbackAction) {
        settings.wifiAutomationNoWifiAction.set(action)
        refreshAutomationIfEnabled()
    }

    fun changeOtherWifiProfileUuid(profileUuid: String?) {
        settings.wifiAutomationOtherWifiProfileUuid.set(profileUuid.orEmpty())
        refreshAutomationIfEnabled()
    }

    fun changeNoWifiProfileUuid(profileUuid: String?) {
        settings.wifiAutomationNoWifiProfileUuid.set(profileUuid.orEmpty())
        refreshAutomationIfEnabled()
    }

    private fun addRule(
        ssid: String,
        action: WifiAutomationAction = WifiAutomationAction.Start,
        profileUuid: String? = null,
    ) {
        if (settings.wifiAutomationRules.value.any { it.ssid == ssid }) {
            viewModelScope.launch { _effects.emit(Effect.SsidAlreadyExists) }
            return
        }
        settings.wifiAutomationRules.set(
            settings.wifiAutomationRules.value + WifiAutomationRule(ssid, action, profileUuid)
        )
        refreshAutomationIfEnabled()
        viewModelScope.launch { _effects.emit(Effect.AddedCurrentSsid) }
    }

    private fun refreshAutomationIfEnabled() {
        if (settings.wifiAutomationEnabled.value) WifiAutomationService.start(getApplication())
    }
}
