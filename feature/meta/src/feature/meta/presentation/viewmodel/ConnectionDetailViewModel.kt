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
import com.github.yumeyucca.yumebox.core.model.ConnectionInfo
import com.github.yumeyucca.yumebox.core.util.PollingTimerSpecs
import com.github.yumeyucca.yumebox.core.util.PollingTimers
import com.github.yumeyucca.yumebox.runtime.client.access.RuntimeAccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * One connection's detail page: polls that connection while it is live and issues the interrupt.
 *
 * The list screen hands the tapped row over through a staged snapshot, so the first frame already
 * shows real numbers; everything after that comes from polling here.
 */
class ConnectionDetailViewModel(private val appContext: Context) : ViewModel() {
    data class ConnectionDetailState(
        val info: ConnectionInfo? = null,
        /** False once the connection has closed: the page freezes its last snapshot. */
        val canInterrupt: Boolean = false,
        val isInterrupting: Boolean = false,
        /** False until the first poll answers, so the page can tell "gone" from "not yet asked". */
        val hasLoaded: Boolean = false,
    )

    private val _state = MutableStateFlow(ConnectionDetailState())
    val state: StateFlow<ConnectionDetailState> = _state.asStateFlow()

    /** Emitted once per successful interrupt; replay is intentionally off, so a page re-opened
     *  later does not immediately leave itself. */
    private val _closed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val closed: SharedFlow<Unit> = _closed.asSharedFlow()

    private var pollingJob: Job? = null
    private var watchedId: String? = null

    /**
     * Starts watching [connectionId]. The view model is scoped to the activity, so a second detail
     * page must take over from the first rather than sharing its snapshot.
     */
    fun start(connectionId: String, canInterrupt: Boolean, staged: ConnectionInfo?) {
        if (watchedId == connectionId && pollingJob?.isActive == true) return
        pollingJob?.cancel()
        watchedId = connectionId
        val initial = staged?.takeIf { it.id == connectionId }
        // The route carries whether the core still had this connection; a missing staged snapshot
        // (process death, or a link opened the page directly) must not disable the button.
        _state.value =
            ConnectionDetailState(
                info = initial,
                canInterrupt = canInterrupt,
            )
        pollingJob =
            viewModelScope.launch {
                val gone = pollOnce(connectionId)
                _state.update { it.copy(hasLoaded = true) }
                if (gone) return@launch
                PollingTimers.ticks(PollingTimerSpecs.ConnectionsPolling)
                    .takeWhile { !pollOnce(connectionId) }
                    .collect { }
            }
    }

    /** Stops watching; called when the page leaves, so a background poll does not outlive it. */
    fun stop() {
        pollingJob?.cancel()
        pollingJob = null
        watchedId = null
    }

    /** Asks the core to drop this connection; the page leaves when the core confirms. */
    fun interrupt() {
        val connection = _state.value.info ?: return
        if (_state.value.isInterrupting) return
        _state.update { it.copy(isInterrupting = true) }
        viewModelScope.launch {
            try {
                if (closeConnection(connection.id)) _closed.tryEmit(Unit)
            } finally {
                // Runs on cancellation too: a page that comes back must not show a stuck spinner.
                _state.update { it.copy(isInterrupting = false) }
            }
        }
    }

    /**
     * One poll tick.
     *
     * @return true when the connection is gone and polling should stop.
     */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun pollOnce(connectionId: String): Boolean =
        withContext(Dispatchers.IO) {
            val connections =
                try {
                    RuntimeAccess.connect(appContext)
                    RuntimeAccess.core().queryConnections().connections
                } catch (
                    error: Exception
                ) { // fault barrier: a failed query is not a closed connection, keep polling
                    if (error is CancellationException) throw error
                    Timber.w(error, "Failed to query connections")
                    null
                }
            val match = connections?.firstOrNull { it.id == connectionId }
            when {
                match != null -> {
                    _state.update { it.copy(info = match) }
                    false
                }
                // A snapshot without this id means the core dropped it; a failed query means we
                // know nothing, so the loop must survive.
                connections != null && _state.value.info != null -> {
                    _state.update { it.copy(canInterrupt = false) }
                    true
                }
                else -> false
            }
        }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun closeConnection(id: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                RuntimeAccess.connect(appContext)
                RuntimeAccess.core().closeConnection(id)
            } catch (
                error: Exception
            ) { // fault barrier: the button reports failure through the state it already shows
                if (error is CancellationException) throw error
                Timber.w(error, "Failed to close connection: %s", id)
                false
            }
        }
}
