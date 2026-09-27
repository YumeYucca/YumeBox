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
import kotlinx.coroutines.Dispatchers
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

    private val _events = MutableSharedFlow<BackupRestoreEvent>()
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

    fun restoreBackup(uri: Uri) {
        launchBusy {
            application.contentResolver.openInputStream(uri)?.use { input ->
                repository.restoreBackup(input)
            } ?: error(YumeTxt.Feature.BackupRestore.Error.OpenInputFailed)
            emitMessage(YumeTxt.Feature.BackupRestore.Message.RestoreSuccess)
        }
    }

    private fun launchBusy(block: suspend () -> Unit) {
        if (_uiState.value.isBusy) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            runCatching { withContext(Dispatchers.IO) { block() } }
                .onFailure { error ->
                    emitMessage(
                        YumeTxt.Feature.BackupRestore.Error.OperationFailed.format(
                            error.message ?: YumeTxt.Util.Error.UnknownError
                        )
                    )
                }
            _uiState.update { it.copy(isBusy = false) }
        }
    }

    private suspend fun emitMessage(message: String) {
        _events.emit(BackupRestoreEvent.Message(message))
    }
}
