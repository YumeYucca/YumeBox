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

@file:Suppress("UnusedSymbol", "PropertyName")

package com.github.yumeyucca.yumebox.presentation.base


import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*

/**
 * State and one-shot effect machinery for the contract ViewModels.
 *
 * [ContractStateViewModel] and [AndroidContractStateViewModel] cannot share a superclass — an
 * [AndroidViewModel] subclass needs the Application at construction — so the behaviour lives here
 * once and both bases expose it as protected members.
 */
private class ContractStateHolder<State : LoadableState<State>, Effect : Any>(initialState: State) {
    val state = MutableStateFlow(initialState)

    private val effects =
        MutableSharedFlow<Effect>(
            replay = 0,
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    val effect: SharedFlow<Effect> = effects.asSharedFlow()

    fun update(transform: (State) -> State) {
        state.update(transform)
    }

    fun emit(value: Effect) {
        effects.tryEmit(value)
    }

    fun setLoading(loading: Boolean) {
        update { it.withLoading(loading) }
    }

    fun postError(error: String, effect: Effect?) {
        update { it.withError(error).withLoading(false) }
        effect?.let(::emit)
    }

    fun postMessage(message: String, effect: Effect?) {
        update { it.withMessage(message) }
        effect?.let(::emit)
    }

    fun clearError() {
        update { it.withError(null) }
    }

    fun clearMessage() {
        update { it.withMessage(null) }
    }
}

/** Contract base with strongly-typed state and one-shot effect channel. */
abstract class ContractStateViewModel<State : LoadableState<State>, Effect : Any>(
    initialState: State
) : ViewModel() {
    private val contract = ContractStateHolder<State, Effect>(initialState)

    protected val _uiState: MutableStateFlow<State> = contract.state
    val uiState: StateFlow<State> = contract.state.asStateFlow()
    val effect: SharedFlow<Effect> = contract.effect

    protected fun updateState(transform: (State) -> State) = contract.update(transform)

    protected fun setLoading(loading: Boolean) = contract.setLoading(loading)

    protected fun postError(error: String, effect: Effect? = null) = contract.postError(error, effect)

    protected fun postMessage(message: String, effect: Effect? = null) =
        contract.postMessage(message, effect)

    protected fun clearErrorState() = contract.clearError()

    protected fun clearMessageState() = contract.clearMessage()
}

/** AndroidViewModel variant for state/effect contract ViewModels requiring Application context. */
abstract class AndroidContractStateViewModel<State : LoadableState<State>, Effect : Any>(
    application: Application,
    initialState: State,
) : AndroidViewModel(application) {
    private val contract = ContractStateHolder<State, Effect>(initialState)

    protected val _uiState: MutableStateFlow<State> = contract.state
    val uiState: StateFlow<State> = contract.state.asStateFlow()
    val effect: SharedFlow<Effect> = contract.effect

    protected fun updateState(transform: (State) -> State) = contract.update(transform)

    protected fun setLoading(loading: Boolean) = contract.setLoading(loading)

    protected fun postError(error: String, effect: Effect? = null) = contract.postError(error, effect)

    protected fun postMessage(message: String, effect: Effect? = null) =
        contract.postMessage(message, effect)

    protected fun clearErrorState() = contract.clearError()

    protected fun clearMessageState() = contract.clearMessage()
}

/**
 * Interface for state classes that support loading, error, and message states.
 *
 * Implement this interface in a state data class to use [ContractStateViewModel] or
 * [AndroidContractStateViewModel].
 */
interface LoadableState<T : LoadableState<T>> {
    val isLoading: Boolean
    val error: String?
    val message: String?

    /** Creates a copy of this state with updated loading status. */
    fun withLoading(loading: Boolean): T

    /** Creates a copy of this state with updated error message. */
    fun withError(error: String?): T

    /** Creates a copy of this state with updated transient message. */
    fun withMessage(message: String?): T
}
