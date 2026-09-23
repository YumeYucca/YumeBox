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
import com.github.yumeyucca.yumebox.data.store.FeatureStore
import com.github.yumeyucca.yumebox.data.store.Preference
import com.github.yumeyucca.yumebox.substore.SubStorePaths
import com.github.yumeyucca.yumebox.substore.SubStoreServiceController
import com.github.yumeyucca.yumebox.substore.SubStoreServiceRequest
import com.github.yumeyucca.yumebox.substore.engine.NativeLibraryManager
import com.github.yumeyucca.yumebox.substore.model.AutoCloseMode
import com.github.yumeyucca.yumebox.substore.util.SubStoreDownloadClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tf.gal.yumebox.locale.YumeTxt

class FeatureViewModel(
    store: FeatureStore,
    private val application: Application,
    private val downloadClient: SubStoreDownloadClient,
) : ViewModel() {
    val allowLanAccess: Preference<Boolean> = store.allowLanAccess
    val backendPort: Preference<Int> = store.backendPort
    val frontendPort: Preference<Int> = store.frontendPort
    val selectedPanelType: Preference<Int> = store.selectedPanelType
    val panelProtocol: Preference<RemoteProtocol> = store.panelProtocol
    val exitUiWhenBackground: Preference<Boolean> = store.exitUiWhenBackground
    private val autoCloseModePreference: Preference<Int> = store.subStoreAutoCloseMode

    private val _autoCloseMode =
        MutableStateFlow(
            AutoCloseMode.entries.getOrNull(autoCloseModePreference.value)
                ?: AutoCloseMode.ALWAYS_ON,
        )
    val autoCloseMode: StateFlow<AutoCloseMode> = _autoCloseMode.asStateFlow()

    val serviceRunningState: StateFlow<Boolean> =
        SubStoreServiceController.snapshot
            .map { it.isActive }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = SubStoreServiceController.snapshot.value.isActive,
            )

    private var autoCloseJob: Job? = null
    private val statusInitializationMutex = Mutex()

    private val _isDownloadingSubStoreFrontend = MutableStateFlow(false)
    val isDownloadingSubStoreFrontend: StateFlow<Boolean> =
        _isDownloadingSubStoreFrontend.asStateFlow()

    private val _isDownloadingSubStoreBackend = MutableStateFlow(false)
    val isDownloadingSubStoreBackend: StateFlow<Boolean> =
        _isDownloadingSubStoreBackend.asStateFlow()

    private val _isSubStoreInitialized = MutableStateFlow(false)
    val isSubStoreInitialized: StateFlow<Boolean> = _isSubStoreInitialized.asStateFlow()

    private val _isDownloadingJavet = MutableStateFlow(false)
    val isDownloadingJavet: StateFlow<Boolean> = _isDownloadingJavet.asStateFlow()

    private val _isJavetLoaded = MutableStateFlow(false)
    val isJavetLoaded: StateFlow<Boolean> = _isJavetLoaded.asStateFlow()

    data class FeatureScreenState(
        val isServiceRunning: Boolean = false,
        val allowLanAccess: Boolean = false,
        val frontendPort: Int = 0,
        val backendPort: Int = 0,
        val autoCloseMode: AutoCloseMode = AutoCloseMode.ALWAYS_ON,
        val isDownloadingSubStoreFrontend: Boolean = false,
        val isDownloadingSubStoreBackend: Boolean = false,
        val isDownloadingJavet: Boolean = false,
        val isJavetLoaded: Boolean = false,
        val selectedPanelType: Int = 0,
    )

    val screenState: StateFlow<FeatureScreenState> =
        combine(
            combine(
                serviceRunningState,
                allowLanAccess.state,
                frontendPort.state,
                backendPort.state,
                autoCloseMode,
            ) { running, lan, front, back, autoClose ->
                FeatureScreenState(
                    isServiceRunning = running,
                    allowLanAccess = lan,
                    frontendPort = front,
                    backendPort = back,
                    autoCloseMode = autoClose,
                )
            },
            combine(
                isDownloadingSubStoreFrontend,
                isDownloadingSubStoreBackend,
                isDownloadingJavet,
                isJavetLoaded,
                selectedPanelType.state,
            ) { dlFront, dlBack, dlJavet, javet, panel ->
                FeatureScreenState(
                    isDownloadingSubStoreFrontend = dlFront,
                    isDownloadingSubStoreBackend = dlBack,
                    isDownloadingJavet = dlJavet,
                    isJavetLoaded = javet,
                    selectedPanelType = panel,
                )
            },
        ) { base, extra ->
            base.copy(
                isDownloadingSubStoreFrontend = extra.isDownloadingSubStoreFrontend,
                isDownloadingSubStoreBackend = extra.isDownloadingSubStoreBackend,
                isDownloadingJavet = extra.isDownloadingJavet,
                isJavetLoaded = extra.isJavetLoaded,
                selectedPanelType = extra.selectedPanelType,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FeatureScreenState(),
        )

    fun startService() {
        if (DeviceUtil.is32BitDevice()) {
            showToast(YumeTxt.Feature.SubStore.Not32Bit)
            return
        }
        if (!checkSubStoreReadiness()) return
        viewModelScope.launch {
            runCatching {
                SubStoreServiceController.startService(
                    context = application,
                    request = currentServiceRequest(),
                )
            }.onSuccess { setupAutoCloseTimer() }
                .onFailure { error -> showToast(error.message ?: YumeTxt.Util.Error.UnknownError) }
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
        viewModelScope.launch {
            cancelAutoCloseTimer()
            SubStoreServiceController.stopService(application)
        }
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
        _autoCloseMode.value = mode
        autoCloseModePreference.set(mode.ordinal)
        if (serviceRunningState.value) {
            cancelAutoCloseTimer()
            setupAutoCloseTimer()
        }
    }

    fun initializeSubStoreStatus() {
        viewModelScope.launch(Dispatchers.IO) { refreshSubStoreStatus() }
    }

    private fun initializeJavetStatus() {
        NativeLibraryManager.initialize(application)
        _isJavetLoaded.value =
            NativeLibraryManager.isLibraryAvailable(NativeLibraryManager.JAVET_LIBRARY_NAME) &&
            NativeLibraryManager.loadJniLibrary(NativeLibraryManager.JAVET_LIBRARY_NAME)
    }

    fun refreshJavetStatus() {
        viewModelScope.launch(Dispatchers.IO) { refreshSubStoreStatus() }
    }

    private suspend fun refreshSubStoreStatus() {
        statusInitializationMutex.withLock {
            _isSubStoreInitialized.value = SubStorePaths.isResourcesReady()
            initializeJavetStatus()
        }
    }

    fun downloadJavetLibrary() {
        if (_isDownloadingJavet.value) return
        viewModelScope.launch {
            _isDownloadingJavet.value = true
            val wasJavetLoaded = _isJavetLoaded.value
            val downloaded =
                runCatching {
                    NativeLibraryManager.initialize(application)
                    val temporaryFile =
                        requireNotNull(
                            NativeLibraryManager.getDownloadTempFile(
                                NativeLibraryManager.JAVET_LIBRARY_NAME,
                            ),
                        )
                    temporaryFile.delete()
                    // Versioned asset first, legacy fixed tag as a fallback; the installer rejects
                    // anything that does not hash to the Javet version this APK needs.
                    NativeLibraryManager.JAVET_ARCHIVE_URLS.any { url ->
                        downloadClient.download(url, temporaryFile) &&
                            NativeLibraryManager.installDownloadedArchive(
                                NativeLibraryManager.JAVET_LIBRARY_NAME,
                                temporaryFile,
                            )
                    } &&
                        NativeLibraryManager.loadJniLibrary(NativeLibraryManager.JAVET_LIBRARY_NAME)
                }.getOrElse { error ->
                    showToast(
                        YumeTxt.Feature.SubStore.DownloadError.format(
                            error.message ?: YumeTxt.Util.Error.UnknownError,
                        ),
                    )
                    false
                }
            _isJavetLoaded.value = downloaded || wasJavetLoaded
            showToast(
                if (downloaded) {
                    YumeTxt.Feature.SubStore.JavetDownloadSuccess
                } else {
                    YumeTxt.Feature.SubStore.JavetDownloadFailed
                },
            )
            _isDownloadingJavet.value = false
        }
    }

    fun setSelectedPanelType(panelType: Int) {
        selectedPanelType.set(panelType)
    }

    fun setPanelProtocol(protocol: RemoteProtocol) = panelProtocol.set(protocol)

    fun setExitUiWhenBackground(enabled: Boolean) = exitUiWhenBackground.set(enabled)

    fun downloadSubStoreFrontend() {
        launchResourceDownload(
            loadingState = _isDownloadingSubStoreFrontend,
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

    fun downloadSubStoreBackend() {
        launchResourceDownload(
            loadingState = _isDownloadingSubStoreBackend,
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

    fun downloadSubStoreAll() {
        viewModelScope.launch {
            if (_isDownloadingSubStoreFrontend.value || _isDownloadingSubStoreBackend.value) {
                return@launch
            }
            downloadSubStoreFrontend()
            while (_isDownloadingSubStoreFrontend.value) {
                PollingTimers.awaitTick(
                    PollingTimerSpecs.dynamic(
                        name = "substore_frontend_download_wait",
                        intervalMillis = 200L,
                        initialDelayMillis = 200L,
                    ),
                )
            }
            downloadSubStoreBackend()
        }
    }

    private fun showToast(msg: String) = application.toast(msg)

    private fun launchResourceDownload(
        loadingState: MutableStateFlow<Boolean>,
        successMessage: String,
        failureMessage: String,
        action: suspend () -> Boolean,
    ) {
        if (loadingState.value) return
        viewModelScope.launch {
            loadingState.value = true
            runCatching {
                val success = action()
                showToast(if (success) successMessage else failureMessage)
                if (success) {
                    _isSubStoreInitialized.value = SubStorePaths.isResourcesReady()
                }
            }.onFailure { error ->
                showToast(
                    YumeTxt.Feature.SubStore.DownloadError.format(
                        error.message ?: YumeTxt.Util.Error.UnknownError,
                    ),
                )
            }
            loadingState.value = false
        }
    }

    private fun setupAutoCloseTimer() {
        cancelAutoCloseTimer()
        val mode = _autoCloseMode.value
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
