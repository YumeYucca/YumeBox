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

@file:Suppress("ConvertLongToDuration")

package com.github.yumeyucca.yumebox.screen.log

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumeyucca.yumebox.core.model.LogMessage
import com.github.yumeyucca.yumebox.runtime.api.LogObserver
import com.github.yumeyucca.yumebox.runtime.api.LogSubscription
import com.github.yumeyucca.yumebox.runtime.client.access.RuntimeAccess
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import kotlin.collections.ArrayDeque

/**
 * Minimum log level shown in the UI. [All] keeps every entry; other values keep that level and
 * anything more severe (Debug < Info < Warning < Error).
 */
enum class LogLevelFilter {
    All,
    Debug,
    Info,
    Warning,
    Error,
}

data class LiveLogEntry(
    val id: Long,
    val time: String,
    val level: LogMessage.Level,
    val message: String,
)

enum class LogConnectionState {
    Connecting,
    Live,
    Retrying,
}

class LogViewModel(private val appContext: Context) : ViewModel() {
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private val nextId = AtomicLong(0L)
    private val pendingLock = Any()
    private val pendingEntries = ArrayDeque<LiveLogEntry>()

    private val _entries = MutableStateFlow<List<LiveLogEntry>>(emptyList())
    private val _levelFilter = MutableStateFlow(LogLevelFilter.All)
    private val _searchQuery = MutableStateFlow("")
    private val _connectionState = MutableStateFlow(LogConnectionState.Connecting)
    @Volatile
    private var logSubscription: LogSubscription? = null
    private var connectJob: Job? = null

    val levelFilter: StateFlow<LogLevelFilter> = _levelFilter.asStateFlow()
    val connectionState: StateFlow<LogConnectionState> = _connectionState.asStateFlow()
    val filteredLogEntries: StateFlow<List<LiveLogEntry>> =
        combine(_entries, _levelFilter, _searchQuery) { entries, filter, query ->
            entries.filter { entry ->
                entry.level.passes(filter) &&
                        (query.isBlank() ||
                                entry.message.contains(query, ignoreCase = true) ||
                                entry.level.name.contains(query, ignoreCase = true))
            }
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    private val observer =
        object : LogObserver {
            override fun onConnected() {
                _connectionState.value = LogConnectionState.Live
            }

            override fun onError(error: Throwable) {
                _connectionState.value = LogConnectionState.Retrying
            }

            override fun newItem(log: LogMessage) {
                val entry =
                    LiveLogEntry(
                        id = nextId.incrementAndGet(),
                        time = synchronized(timeFormat) { timeFormat.format(log.time) },
                        level = log.level,
                        message = log.message,
                    )
                synchronized(pendingLock) {
                    if (pendingEntries.size == MAX_ENTRIES) pendingEntries.removeFirst()
                    pendingEntries.addLast(entry)
                }
            }
        }

    init {
        viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(LOG_BATCH_WINDOW_MS)
                val batch =
                    synchronized(pendingLock) {
                        if (pendingEntries.isEmpty()) {
                            emptyList()
                        } else {
                            pendingEntries.toList().also { pendingEntries.clear() }
                        }
                    }
                if (batch.isNotEmpty()) {
                    _entries.update { entries ->
                        (batch.asReversed() + entries).take(MAX_ENTRIES)
                    }
                }
            }
        }
    }

    data class LogScreenState(
        val filteredEntries: List<LiveLogEntry> = emptyList(),
        val levelFilter: LogLevelFilter = LogLevelFilter.All,
        val connectionState: LogConnectionState = LogConnectionState.Connecting,
    )

    val screenState: StateFlow<LogScreenState> =
        combine(filteredLogEntries, levelFilter, connectionState) { entries, filter, connection ->
            LogScreenState(
                filteredEntries = entries,
                levelFilter = filter,
                connectionState = connection,
            )
        }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                LogScreenState(),
            )

    fun connect() {
        if (logSubscription != null || connectJob?.isActive == true) return
        connectJob =
            viewModelScope.launch(Dispatchers.IO) {
                var retryDelay = INITIAL_CONNECT_RETRY_MS
                var firstAttempt = true
                while (isActive && logSubscription == null) {
                    _connectionState.value =
                        if (firstAttempt) {
                            LogConnectionState.Connecting
                        } else {
                            LogConnectionState.Retrying
                        }
                    try {
                        RuntimeAccess.connect(appContext)
                        logSubscription = RuntimeAccess.core().subscribeLogs(observer)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Throwable) {
                        Timber.w(error, "Failed to subscribe to live logs; retrying")
                        firstAttempt = false
                        delay(retryDelay)
                        retryDelay = (retryDelay * 2).coerceAtMost(MAX_CONNECT_RETRY_MS)
                    }
                }
            }
    }

    fun setLevelFilter(filter: LogLevelFilter) {
        _levelFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    override fun onCleared() {
        connectJob?.cancel()
        connectJob = null
        logSubscription?.close()
        logSubscription = null
    }

    private fun LogMessage.Level.passes(filter: LogLevelFilter): Boolean {
        val rank =
            when (this) {
                LogMessage.Level.Debug -> 0
                LogMessage.Level.Info -> 1
                LogMessage.Level.Warning -> 2
                LogMessage.Level.Error -> 3
                LogMessage.Level.Silent,
                LogMessage.Level.Unknown -> return filter == LogLevelFilter.All
            }
        val min =
            when (filter) {
                LogLevelFilter.All -> return true
                LogLevelFilter.Debug -> 0
                LogLevelFilter.Info -> 1
                LogLevelFilter.Warning -> 2
                LogLevelFilter.Error -> 3
            }
        return rank >= min
    }

    private companion object {
        const val MAX_ENTRIES = 2_000
        const val LOG_BATCH_WINDOW_MS = 280L
        const val INITIAL_CONNECT_RETRY_MS = 500L
        const val MAX_CONNECT_RETRY_MS = 5_000L
    }
}
