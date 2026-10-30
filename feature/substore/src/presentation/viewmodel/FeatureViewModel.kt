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

@file:Suppress("UnusedSymbol", "RedundantIf")

package com.github.yumeyucca.yumebox.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumeyucca.yumebox.common.util.DeviceUtil
import com.github.yumeyucca.yumebox.common.util.toast
import com.github.yumeyucca.yumebox.core.util.PollingTimerSpecs
import com.github.yumeyucca.yumebox.core.util.PollingTimers
import com.github.yumeyucca.yumebox.data.model.RemoteProtocol
import com.github.yumeyucca.yumebox.data.model.SubStorePanel
import com.github.yumeyucca.yumebox.data.store.FeatureStore
import com.github.yumeyucca.yumebox.data.store.Preference
import com.github.yumeyucca.yumebox.substore.SubStorePaths
import com.github.yumeyucca.yumebox.substore.SubStoreServiceController
import com.github.yumeyucca.yumebox.substore.SubStoreServiceRequest
import com.github.yumeyucca.yumebox.substore.engine.NativeLibraryManager
import com.github.yumeyucca.yumebox.substore.model.AutoCloseMode
import com.github.yumeyucca.yumebox.substore.util.SubStoreDownloadClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.github.yumeyucca.yumebox.core.locale.YumeTxt

/** Independently downloadable payloads of the Sub-Store runtime. */
enum class SubStoreResource {
    Frontend,
    Backend,
    Javet,
}

class FeatureViewModel(
    store: FeatureStore,
    private val application: Application,
    private val downloadClient: SubStoreDownloadClient,
) : ViewModel() {
    val allowLanAccess: Preference<Boolean> = store.allowLanAccess
    val backendPort: Preference<Int> = store.backendPort
    val frontendPort: Preference<Int> = store.frontendPort
    val panelProtocol: Preference<RemoteProtocol> = store.panelProtocol

    /** Persisted as an ordinal; [FeatureScreenState.panelType] is the typed view of it. */
    private val selectedPanelType: Preference<Int> = store.selectedPanelType
    private val autoCloseModePreference: Preference<Int> = store.subStoreAutoCloseMode

    /** Derived from the stored ordinal; the preference stays the single source of truth. */
    val autoCloseMode: StateFlow<AutoCloseMode> =
        autoCloseModePreference.state
            .map { it.toAutoCloseMode() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = autoCloseModePreference.value.toAutoCloseMode(),
            )

    private val serviceRunningState: StateFlow<Boolean> =
        SubStoreServiceController.lifecycle
            .map { it.isActive }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = SubStoreServiceController.lifecycle.value.isActive,
            )

    private var autoCloseJob: Job? = null

    /** Resources whose download is in flight; a set instead of one flag per payload. */
    private val _downloading = MutableStateFlow<Set<SubStoreResource>>(emptySet())

    private val _isSubStoreInitialized = MutableStateFlow(false)

    private val _isJavetLoaded = MutableStateFlow(false)

    data class FeatureScreenState(
        val isServiceRunning: Boolean = false,
        val allowLanAccess: Boolean = false,
        val frontendPort: Int = 0,
        val backendPort: Int = 0,
        val autoCloseMode: AutoCloseMode = AutoCloseMode.ALWAYS_ON,
        val downloading: Set<SubStoreResource> = emptySet(),
        val isJavetLoaded: Boolean = false,
        /** Null when the stored ordinal matches no [SubStorePanel], so no panel URL is offered. */
        val panelType: SubStorePanel? = null,
    )

    /** Everything the service itself reports; the rest of the screen state is layered on below. */
    private val serviceState: Flow<FeatureScreenState> =
        combine(
            serviceRunningState,
            allowLanAccess.state,
            frontendPort.state,
            backendPort.state,
            autoCloseMode,
        ) { isRunning, allowLan, frontendPort, backendPort, autoCloseMode ->
            FeatureScreenState(
                isServiceRunning = isRunning,
                allowLanAccess = allowLan,
                frontendPort = frontendPort,
                backendPort = backendPort,
                autoCloseMode = autoCloseMode,
            )
        }

    val screenState: StateFlow<FeatureScreenState> =
        combine(
            serviceState,
            _downloading,
            _isJavetLoaded,
            selectedPanelType.state.map { SubStorePanel.fromIndex(it) },
        ) { service, downloading, javetLoaded, panelType ->
            service.copy(
                downloading = downloading,
                isJavetLoaded = javetLoaded,
                panelType = panelType,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue =
                FeatureScreenState(panelType = SubStorePanel.fromIndex(selectedPanelType.value)),
        )

    fun startService() {
        if (DeviceUtil.is32BitDevice()) {
            showToast(YumeTxt.Feature.SubStore.Not32Bit)
            return
        }
        if (!checkSubStoreReadiness()) return
        viewModelScope.launch {
            try {
                SubStoreServiceController.startService(
                    context = application,
                    request = currentServiceRequest(),
                )
                setupAutoCloseTimer()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                showToast(error.message ?: YumeTxt.Util.Error.UnknownError)
            }
        }
    }

    private fun checkSubStoreReadiness(): Boolean =
        when {
            !_isSubStoreInitialized.value -> {
                showToast(YumeTxt.Feature.SubStore.DownloadSubStoreFirst)
                false
            }

            !_isJavetLoaded.value -> {
                showToast(YumeTxt.Feature.SubStore.JavetNotReady)
                false
            }

            else -> {
                true
            }
        }

    fun stopService() {
        cancelAutoCloseTimer()
        SubStoreServiceController.stopService(application)
    }

    fun setAllowLanAccess(allow: Boolean) {
        allowLanAccess.set(allow)
        if (serviceRunningState.value) {
            SubStoreServiceController.startService(application, currentServiceRequest())
        }
    }

    private fun currentServiceRequest() =
        SubStoreServiceRequest(
            backendPort = backendPort.value,
            frontendPort = frontendPort.value,
            allowLan = allowLanAccess.value,
        )

    fun setAutoCloseMode(mode: AutoCloseMode) {
        autoCloseModePreference.set(mode.ordinal)
        if (serviceRunningState.value) setupAutoCloseTimer(mode)
    }

    fun initializeSubStoreStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            _isSubStoreInitialized.value = SubStorePaths.isResourcesReady()
            initializeJavetStatus()
        }
    }

    private fun initializeJavetStatus() {
        NativeLibraryManager.initialize(application)
        _isJavetLoaded.value =
            NativeLibraryManager.isLibraryAvailable() && NativeLibraryManager.loadJniLibrary()
    }

    fun setSelectedPanelType(panelType: SubStorePanel) {
        selectedPanelType.set(panelType.ordinal)
    }

    fun setPanelProtocol(protocol: RemoteProtocol) = panelProtocol.set(protocol)

    /**
     * Installs the runtime first when missing, then fetches the frontend and backend payloads.
     * The two payloads are independent downloads, so they run concurrently.
     */
    fun installResources() {
        viewModelScope.launch {
            if (!_isJavetLoaded.value && !downloadJavetLibrary()) return@launch
            coroutineScope {
                launch { downloadSubStoreFrontend() }
                launch { downloadSubStoreBackend() }
            }
        }
    }

    private suspend fun downloadJavetLibrary(): Boolean {
        val wasJavetLoaded = _isJavetLoaded.value
        val downloaded =
            runResourceDownload(
                resource = SubStoreResource.Javet,
                successMessage = YumeTxt.Feature.SubStore.JavetDownloadSuccess,
                failureMessage = YumeTxt.Feature.SubStore.JavetDownloadFailed,
            ) {
                NativeLibraryManager.initialize(application)
                val temporaryFile = requireNotNull(NativeLibraryManager.getDownloadTempFile())
                temporaryFile.delete()
                // Versioned asset first, legacy fixed tag as fallback.
                NativeLibraryManager.JAVET_ARCHIVE_URLS.any { url ->
                    downloadClient.download(url, temporaryFile) &&
                        NativeLibraryManager.installDownloadedArchive(temporaryFile)
                } && NativeLibraryManager.loadJniLibrary()
            }
        _isJavetLoaded.value = downloaded || wasJavetLoaded
        return _isJavetLoaded.value
    }

    private suspend fun downloadSubStoreFrontend() {
        runResourceDownload(
            resource = SubStoreResource.Frontend,
            successMessage = YumeTxt.Feature.SubStore.FrontendDownloadSuccess,
            failureMessage = YumeTxt.Feature.SubStore.FrontendDownloadFailed,
        ) {
            SubStorePaths.ensureStructure()
            SubStorePaths.frontendDir.apply { if (!exists()) mkdirs() }
            downloadClient.downloadAndExtract(
                url =
                    "https://github.com/sub-store-org/Sub-Store-Front-End/releases/latest/download/dist.zip",
                targetDir = SubStorePaths.frontendDir,
            )
        }
    }

    private suspend fun downloadSubStoreBackend() {
        runResourceDownload(
            resource = SubStoreResource.Backend,
            successMessage = YumeTxt.Feature.SubStore.BackendDownloadSuccess,
            failureMessage = YumeTxt.Feature.SubStore.BackendDownloadFailed,
        ) {
            SubStorePaths.ensureStructure()
            SubStorePaths.backendDir.apply { if (!exists()) mkdirs() }
            downloadClient.download(
                url =
                    "https://github.com/sub-store-org/Sub-Store/releases/latest/download/sub-store.bundle.js",
                targetFile = SubStorePaths.backendBundle,
            )
        }
    }

    /**
     * Runs one download with the shared toasts and bookkeeping, and skips it when the same
     * resource is already downloading. Suspends until the download has finished, so callers can
     * chain payloads without polling.
     */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun runResourceDownload(
        resource: SubStoreResource,
        successMessage: String,
        failureMessage: String,
        action: suspend () -> Boolean,
    ): Boolean {
        if (resource in _downloading.value) return false
        _downloading.update { it + resource }
        return try {
            val success = action()
            showToast(if (success) successMessage else failureMessage)
            if (success) {
                _isSubStoreInitialized.value = SubStorePaths.isResourcesReady()
            }
            success
        } catch (error: CancellationException) {
            throw error
        } catch (
            error: Exception
        ) { // fault barrier: one failed payload toasts instead of aborting the batch
            showToast(
                YumeTxt.Feature.SubStore.DownloadError.format(
                    error.message ?: YumeTxt.Util.Error.UnknownError,
                ),
            )
            false
        } finally {
            _downloading.update { it - resource }
        }
    }

    private fun showToast(msg: String) = application.toast(msg)

    private fun Int.toAutoCloseMode(): AutoCloseMode =
        AutoCloseMode.entries.getOrNull(this) ?: AutoCloseMode.ALWAYS_ON

    private fun setupAutoCloseTimer(
        mode: AutoCloseMode = autoCloseModePreference.value.toAutoCloseMode(),
    ) {
        cancelAutoCloseTimer()
        mode.minutes?.let { minutes ->
            autoCloseJob =
                viewModelScope.launch {
                    val timeoutMillis = minutes * 60 * 1000L
                    PollingTimers.awaitTick(
                        PollingTimerSpecs.dynamic(
                            name = "substore_auto_close",
                            intervalMillis = timeoutMillis,
                            initialDelayMillis = timeoutMillis,
                        ),
                    )
                    showToast(YumeTxt.Feature.ServiceStatus.AutoClosed)
                    stopService()
                }
        }
    }

    private fun cancelAutoCloseTimer() {
        autoCloseJob?.cancel()
        autoCloseJob = null
    }
}
