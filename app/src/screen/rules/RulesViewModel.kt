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

package com.github.yumeyucca.yumebox.screen.rules

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yumeyucca.yumebox.core.model.RuntimeRule
import com.github.yumeyucca.yumebox.runtime.client.access.RuntimeAccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

data class RulesUiState(
    val rules: List<RuntimeRule> = emptyList(),
    val isLoading: Boolean = true,
    val isRunning: Boolean = false,
    val toggleError: String? = null,
    val togglingIndexes: Set<Int> = emptySet(),
)

class RulesViewModel(private val appContext: Context) : ViewModel() {
    private val _uiState = MutableStateFlow(RulesUiState())
    val uiState: StateFlow<RulesUiState> = _uiState.asStateFlow()
    private val toggleMutex = Mutex()

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                RuntimeAccess.connect(appContext)
                RuntimeAccess.core().queryRules()
            }
                .onSuccess { rules ->
                    _uiState.update {
                        it.copy(rules = rules, isLoading = false, isRunning = true)
                    }
                }
                .onFailure { error ->
                    // Cancellation must not be published as "runtime not running" with no rules.
                    if (error is CancellationException) throw error
                    Timber.w(error, "queryRules failed")
                    _uiState.update {
                        it.copy(
                            rules = emptyList(),
                            isLoading = false,
                            isRunning = false,
                        )
                    }
                }
        }
    }

    /**
     * Toggle a single rule. Switch ON means the rule is **enabled** (disabled=false). PATCH
     * /rules/disable uses { index: disabled }.
     */
    fun setRuleEnabled(index: Int, enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            toggleMutex.withLock {
                val disabled = !enabled
                val originalRule =
                    _uiState.value.rules.firstOrNull { it.index == index } ?: return@withLock
                _uiState.update { state ->
                    state.copy(
                        rules =
                            state.rules.map { rule ->
                                rule.takeUnless { it.index == index }
                                    ?: rule.copy(disabled = disabled)
                            },
                        togglingIndexes = state.togglingIndexes + index,
                        toggleError = null,
                    )
                }
                runCatching {
                    RuntimeAccess.connect(appContext)
                    RuntimeAccess.core().setRuleDisabled(originalRule, disabled)
                }
                    .onSuccess { confirmedRules ->
                        _uiState.update {
                            it.copy(
                                rules = confirmedRules,
                                togglingIndexes = it.togglingIndexes - index,
                                toggleError = null,
                            )
                        }
                    }
                    .onFailure { error ->
                        // A cancelled toggle must not be rolled back with a bogus error banner.
                        if (error is CancellationException) throw error
                        Timber.w(error, "setRuleDisabled failed")
                        _uiState.update { state ->
                            state.copy(
                                rules =
                                    state.rules.map { rule ->
                                        rule.takeIf { it.index != index } ?: originalRule
                                    },
                                togglingIndexes = state.togglingIndexes - index,
                                toggleError = error.message ?: error.javaClass.simpleName,
                            )
                        }
                    }
            }
        }
    }

    fun consumeToggleError() {
        _uiState.update { it.copy(toggleError = null) }
    }
}
