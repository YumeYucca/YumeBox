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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

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
        val isScanning: Boolean = false,
        val scanCompleted: Boolean = false,
        val scannedNetworks: List<WifiSsidNetwork> = emptyList(),
        val scanUnavailable: Boolean = false,
    )

    sealed interface Effect {
        data object AddedCurrentSsid : Effect

        data object SsidAlreadyExists : Effect

        data object NoWifi : Effect

        data object SsidUnavailable : Effect

        data object ScanUnavailable : Effect
    }

    private data class ScanState(
        val isScanning: Boolean = false,
        val scanCompleted: Boolean = false,
        val networks: List<WifiSsidNetwork> = emptyList(),
        val unavailable: Boolean = false,
    )

    private val scanState = MutableStateFlow(ScanState())
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
            .combine(scanState) { state, scan ->
                state.copy(
                    isScanning = scan.isScanning,
                    scanCompleted = scan.scanCompleted,
                    scannedNetworks = scan.networks,
                    scanUnavailable = scan.unavailable,
                )
            }
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

    private val _effects = MutableSharedFlow<Effect>()
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
        action: WifiAutomationAction = WifiAutomationAction.Start,
        profileUuid: String? = null,
    ) {
        val ssid = WifiSsidObserver.normalizeSsid(rawSsid) ?: return
        addRule(ssid, action, profileUuid)
    }

    fun scanWifi() {
        if (scanState.value.isScanning) return
        scanState.value = ScanState(isScanning = true)
        scanJob = viewModelScope.launch {
            when (val result = WifiSsidScanner.scanOnce(getApplication())) {
                is WifiSsidScanResult.Success -> {
                    scanState.value = ScanState(scanCompleted = true, networks = result.networks)
                }

                WifiSsidScanResult.Unavailable -> {
                    scanState.value = ScanState(scanCompleted = true, unavailable = true)
                    _effects.emit(Effect.ScanUnavailable)
                }
            }
        }
    }

    fun resetScan() {
        scanJob?.cancel()
        scanJob = null
        scanState.value = ScanState()
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
