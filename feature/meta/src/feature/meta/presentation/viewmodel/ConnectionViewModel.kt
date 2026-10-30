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

@file:Suppress("UnusedSymbol", "FunctionName")

package com.github.yumeyucca.yumebox.feature.meta.presentation.viewmodel


import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumeyucca.yumebox.core.domain.ConnectionHistoryManager
import com.github.yumeyucca.yumebox.core.model.ConnectionInfo
import com.github.yumeyucca.yumebox.core.model.ConnectionSnapshot
import com.github.yumeyucca.yumebox.core.util.PollingTimerSpecs
import com.github.yumeyucca.yumebox.core.util.PollingTimers
import com.github.yumeyucca.yumebox.feature.meta.presentation.util.displayTarget
import com.github.yumeyucca.yumebox.feature.meta.presentation.util.stringOrEmpty
import com.github.yumeyucca.yumebox.runtime.client.access.RuntimeAccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

enum class ConnectionSort {
    Time,
    Upload,
    Download,
    Host,
}

enum class ConnectionTab {
    ACTIVE,
    CLOSED,
}

data class ConnectionState(
    val snapshot: ConnectionSnapshot? = null,
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val sortBy: ConnectionSort = ConnectionSort.Time,
    val selectedTab: ConnectionTab = ConnectionTab.ACTIVE,
    val error: String? = null,
)

class ConnectionViewModel(private val appContext: Context) : ViewModel() {
    private val _state = MutableStateFlow(ConnectionState())
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    data class ConnectionScreenState(
        val state: ConnectionState = ConnectionState(),
        val filteredConnections: List<ConnectionInfo> = emptyList(),
    )

    /** Derived from [state] alone, so the snapshot and its projection can never disagree. */
    val screenState: StateFlow<ConnectionScreenState> =
        state
            .map { current -> ConnectionScreenState(current, buildFilteredConnections(current)) }
            .flowOn(Dispatchers.Default)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ConnectionScreenState(),
            )

    private var pollingJob: Job? = null

    fun startPolling() {
        if (pollingJob?.isActive == true) return

        pollingJob = viewModelScope.launch {
            pollOnce()
            PollingTimers.ticks(PollingTimerSpecs.ConnectionsPolling).collect { pollOnce() }
        }
    }

    /** One poll tick; a failure is reported through the state instead of killing the loop. */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun pollOnce() {
        try {
            refreshConnections()
        } catch (
            error: Exception
        ) { // fault barrier: keep the polling loop alive on any failure
            if (error is CancellationException) throw error
            Timber.w(error, "Failed to poll connections")
            _state.update { it.copy(error = error.message) }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun setSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun setSortBy(sort: ConnectionSort) {
        _state.update { it.copy(sortBy = sort) }
    }

    fun setTab(tab: ConnectionTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    suspend fun closeConnection(id: String): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                RuntimeAccess.connect(appContext)
                RuntimeAccess.core().closeConnection(id)
            }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    Timber.w(error, "Failed to close connection: %s", id)
                    _state.update { it.copy(error = error.message) }
                }
                .getOrDefault(false)
        }
            .also { refreshConnections() }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun refreshConnections() {
        withContext(Dispatchers.IO) {
            try {
                RuntimeAccess.connect(appContext)
                val snapshot = RuntimeAccess.core().queryConnections()
                ConnectionHistoryManager.updateConnections(snapshot.connections)
                _state.update {
                    it.copy(
                        snapshot = snapshot,
                        error = null,
                        isLoading = false,
                    )
                }
            } catch (
                error: Exception
            ) { // fault barrier: service IPC failure becomes UI error state
                if (error is CancellationException) throw error
                Timber.w(error, "Failed to query connections")
                _state.update {
                    it.copy(error = error.message, isLoading = false)
                }
            }
        }
    }

    private fun buildFilteredConnections(currentState: ConnectionState): List<ConnectionInfo> {
        val connections =
            when (currentState.selectedTab) {
                ConnectionTab.ACTIVE -> currentState.snapshot?.connections ?: emptyList()
                ConnectionTab.CLOSED -> ConnectionHistoryManager.getClosedConnections()
            }

        val filtered =
            if (currentState.searchQuery.isEmpty()) {
                connections
            } else {
                val query = currentState.searchQuery.lowercase()
                connections.filter { conn ->
                    val host = conn.displayTarget().lowercase()
                    val process = conn.metadata.stringOrEmpty("process").lowercase()
                    val chains = conn.chains.joinToString(" ").lowercase()
                    val rule = conn.rule.lowercase()

                    host.contains(query) ||
                            process.contains(query) ||
                            chains.contains(query) ||
                            rule.contains(query)
                }
            }

        val sorted =
            if (currentState.selectedTab == ConnectionTab.ACTIVE) {
                when (currentState.sortBy) {
                    ConnectionSort.Time -> filtered.sortedByDescending { it.start }
                    ConnectionSort.Upload -> filtered.sortedByDescending { it.upload }
                    ConnectionSort.Download -> filtered.sortedByDescending { it.download }
                    ConnectionSort.Host -> filtered.sortedBy { it.displayTarget() }
                }
            } else {
                filtered
            }
        return sorted
    }

    override fun onCleared() {
        stopPolling()
    }
}
