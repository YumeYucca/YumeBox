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

package com.github.yumeyucca.yumebox.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumeyucca.yumebox.core.model.Provider
import com.github.yumeyucca.yumebox.data.controller.ProvidersController
import com.github.yumeyucca.yumebox.runtime.client.ProxyFacade
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.github.yumeyucca.yumebox.core.locale.YumeTxt

/**
 * The one identity of a provider: type plus name. The list key and the "being updated" set must
 * agree, so both are derived from this instead of repeating the concatenation.
 */
internal fun providerKey(provider: Provider): String = "${provider.type}_${provider.name}"

class ProvidersViewModel(
    private val proxyFacade: ProxyFacade,
    private val providersRepository: ProvidersController,
) : ViewModel() {
    private val _providers = MutableStateFlow<List<Provider>>(emptyList())
    val providers: StateFlow<List<Provider>> = _providers.asStateFlow()

    private val _uiState = MutableStateFlow(ProvidersUiState())
    val uiState: StateFlow<ProvidersUiState> = _uiState.asStateFlow()

    val isRunning: StateFlow<Boolean> = proxyFacade.isRunning

    fun refreshProviders() {
        viewModelScope.launch {
            if (!proxyFacade.isRunning.value) {
                _providers.value = emptyList()
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }
            val result = providersRepository.queryProviders()
            result
                .onSuccess { providerList -> _providers.value = providerList.sorted() }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            error =
                                YumeTxt.Providers.Message.FetchFailed.format(
                                    error.message ?: YumeTxt.Util.Error.UnknownError
                                )
                        )
                    }
                }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    /**
     * One provider, one request: mark it busy, run it, refresh on success with a message, report the
     * failure message on error, clear the mark either way. Only the request and the two texts differ
     * between "update" and "upload", so they are arguments instead of a second copy of the flow.
     */
    private fun runProviderUpdate(
        provider: Provider,
        successMessage: () -> String,
        failureMessage: (String) -> String,
        request: suspend () -> Result<Unit>,
    ) {
        val key = providerKey(provider)
        viewModelScope.launch {
            _uiState.update { it.copy(updatingProviders = it.updatingProviders + key) }
            request()
                .onSuccess {
                    refreshProviders()
                    _uiState.update { it.copy(message = successMessage()) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(error = failureMessage(error.message ?: YumeTxt.Util.Error.UnknownError))
                    }
                }
            _uiState.update { it.copy(updatingProviders = it.updatingProviders - key) }
        }
    }

    fun updateProvider(provider: Provider) {
        runProviderUpdate(
            provider = provider,
            successMessage = { YumeTxt.Providers.Message.UpdateSuccess.format(provider.name) },
            failureMessage = { YumeTxt.Providers.Message.UpdateFailed.format(it) },
            request = { providersRepository.updateProvider(provider) },
        )
    }

    fun updateAllProviders() {
        viewModelScope.launch {
            val httpProviders =
                _providers.value.filter { it.vehicleType == Provider.VehicleType.HTTP }
            if (httpProviders.isEmpty()) return@launch

            _uiState.update { it.copy(isUpdatingAll = true) }
            val providerKeys = httpProviders.map(::providerKey).toSet()
            _uiState.update { it.copy(updatingProviders = providerKeys) }

            val result = providersRepository.updateAllProviders(httpProviders)
            result
                .onSuccess { updateResult ->
                    refreshProviders()
                    if (updateResult.failedProviders.isEmpty()) {
                        _uiState.update { it.copy(message = YumeTxt.Providers.Message.AllUpdated) }
                    } else {
                        _uiState.update {
                            it.copy(
                                error =
                                    YumeTxt.Providers.Message.UpdateFailed.format(
                                        updateResult.failedProviders.joinToString(", ")
                                    )
                            )
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            error =
                                YumeTxt.Providers.Message.UpdateFailed.format(
                                    error.message ?: YumeTxt.Util.Error.UnknownError
                                )
                        )
                    }
                }

            _uiState.update { it.copy(isUpdatingAll = false, updatingProviders = emptySet()) }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun uploadProviderFile(provider: Provider, uri: Uri) {
        runProviderUpdate(
            provider = provider,
            successMessage = { YumeTxt.Providers.Message.UploadSuccess.format(provider.name) },
            failureMessage = { YumeTxt.Providers.Message.UploadFailed.format(it) },
            request = { providersRepository.uploadProviderFile(provider, uri) },
        )
    }

    data class ProvidersUiState(
        val isLoading: Boolean = false,
        val isUpdatingAll: Boolean = false,
        val updatingProviders: Set<String> = emptySet(),
        val message: String? = null,
        val error: String? = null,
    )
}
