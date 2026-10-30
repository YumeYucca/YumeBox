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

package com.github.yumeyucca.yumebox.screen.settings.backup

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.github.yumeyucca.yumebox.core.locale.YumeTxt

data class BackupRestoreUiState(val isBusy: Boolean = false)

sealed interface BackupRestoreEvent {
    data class Message(val text: String) : BackupRestoreEvent
}

class BackupRestoreViewModel(
    private val application: Application,
    private val repository: BackupRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(BackupRestoreUiState())
    val uiState: StateFlow<BackupRestoreUiState> = _uiState.asStateFlow()

    // Buffered, so an event raised while the screen is not collecting (the user left the page
    // mid-restore) parks here instead of suspending the operation that raised it.
    private val _events =
        MutableSharedFlow<BackupRestoreEvent>(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val events: SharedFlow<BackupRestoreEvent> = _events.asSharedFlow()

    fun defaultBackupFileName(): String = repository.defaultBackupFileName()

    fun exportBackup(uri: Uri) {
        launchBusy {
            application.contentResolver.openOutputStream(uri)?.use { output ->
                repository.exportBackup(output)
            } ?: error(YumeTxt.Feature.BackupRestore.Error.OpenOutputFailed)
            emitMessage(YumeTxt.Feature.BackupRestore.Message.ExportSuccess)
        }
    }

    internal fun restoreBackup(uri: Uri, strategy: BackupRestoreStrategy) {
        launchBusy {
            application.contentResolver.openInputStream(uri)?.use { input ->
                repository.restoreBackup(input, strategy)
            } ?: error(YumeTxt.Feature.BackupRestore.Error.OpenInputFailed)
            emitMessage(YumeTxt.Feature.BackupRestore.Message.RestoreSuccess)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun launchBusy(block: suspend () -> Unit) {
        if (_uiState.value.isBusy) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            try {
                withContext(Dispatchers.IO) { block() }
            } catch (error: CancellationException) {
                throw error
            } catch (
                error: Exception
            ) { // fault barrier: a failed operation becomes a message, never a crash
                emitMessage(
                    YumeTxt.Feature.BackupRestore.Error.OperationFailed.format(
                        error.message ?: YumeTxt.Util.Error.UnknownError
                    )
                )
            } finally {
                // Also on cancellation, so a cancelled operation cannot leave the screen busy.
                _uiState.update { it.copy(isBusy = false) }
            }
        }
    }

    private suspend fun emitMessage(message: String) {
        _events.emit(BackupRestoreEvent.Message(message))
    }
}
