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

package com.github.yumeyucca.yumebox.screen.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumeyucca.yumebox.common.util.stateInWhileSubscribed
import com.github.yumeyucca.yumebox.data.model.RemoteBackend
import com.github.yumeyucca.yumebox.data.model.RemoteProtocol
import com.github.yumeyucca.yumebox.data.store.RemoteControllerStore
import com.github.yumeyucca.yumebox.runtime.client.ProxyFacade
import kotlinx.coroutines.flow.*

class RemoteControllerViewModel(
    application: Application,
    private val store: RemoteControllerStore,
    private val proxyFacade: ProxyFacade,
) : AndroidViewModel(application) {
    val controllerEnabled: StateFlow<Boolean> =
        store.controllerEnabled.state.stateInWhileSubscribed(
            viewModelScope,
            store.controllerEnabled.value,
        )

    val backends: StateFlow<List<RemoteBackend>> =
        store.backends.state.stateInWhileSubscribed(viewModelScope, store.backends.value)

    val activeBackendId: StateFlow<String> =
        store.activeBackendId.state.stateInWhileSubscribed(
            viewModelScope,
            store.activeBackendId.value,
        )

    data class SectionState(
        val controllerEnabled: Boolean = false,
        val backends: List<RemoteBackend> = emptyList(),
        val activeBackendId: String = "",
    )

    val sectionState: StateFlow<SectionState> =
        combine(controllerEnabled, backends, activeBackendId) { enabled, list, activeId ->
            SectionState(
                controllerEnabled = enabled,
                backends = list,
                activeBackendId = activeId,
            )
        }.stateInWhileSubscribed(
            viewModelScope,
            SectionState(
                controllerEnabled = store.controllerEnabled.value,
                backends = store.backends.value,
                activeBackendId = store.activeBackendId.value,
            ),
        )

    fun setEnabled(enabled: Boolean) {
        store.controllerEnabled.set(enabled)
        proxyFacade.applyRemoteControllerState()
    }

    fun addBackend(
        name: String,
        host: String,
        port: Int,
        secret: String,
    ) {
        val backend =
            RemoteBackend(
                id = RemoteBackend.newId(),
                name = name.trim(),
                host = host.trim(),
                port = port,
                secret = secret.trim(),
            )
        store.backends.set(store.backends.value + backend)
        if (store.activeBackendId.value.isBlank()) {
            store.activeBackendId.set(backend.id)
            proxyFacade.applyRemoteControllerState()
        }
    }

    fun updateBackend(updated: RemoteBackend) {
        store.backends.set(store.backends.value.map { if (it.id == updated.id) updated else it })
        if (updated.id == store.activeBackendId.value) {
            proxyFacade.applyRemoteControllerState()
        }
    }

    fun setProtocol(
        id: String,
        protocol: RemoteProtocol,
    ) {
        store.backends.set(
            store.backends.value.map { if (it.id == id) it.copy(protocol = protocol) else it },
        )
        if (id == store.activeBackendId.value) {
            proxyFacade.applyRemoteControllerState()
        }
    }

    fun deleteBackend(id: String) {
        val wasActive = store.activeBackendId.value == id
        store.backends.set(store.backends.value.filterNot { it.id == id })
        if (wasActive) {
            store.activeBackendId.set("")
            store.controllerEnabled.set(false)
            proxyFacade.applyRemoteControllerState()
        }
    }

    fun setActive(id: String) {
        store.activeBackendId.set(id)
        proxyFacade.applyRemoteControllerState()
    }
}
