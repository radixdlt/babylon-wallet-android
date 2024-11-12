package com.babylon.wallet.android.presentation.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

@Suppress("VariableNaming")
open class ViewModelDelegate<T : UiState> {

    lateinit var viewModelScope: CoroutineScope
    lateinit var _state: MutableStateFlow<T>

    open operator fun invoke(
        scope: CoroutineScope,
        state: MutableStateFlow<T>
    ) {
        this.viewModelScope = scope
        _state = state
    }
}

@Suppress("VariableNaming")
open class ViewModelDelegateWithEvents<T : UiState, E: OneOffEvent> {

    lateinit var viewModelScope: CoroutineScope
    lateinit var _state: MutableStateFlow<T>
    lateinit var oneOffEventHandler: OneOffEventHandler<E>

    open operator fun invoke(
        scope: CoroutineScope,
        state: MutableStateFlow<T>,
        oneOffEventHandler: OneOffEventHandler<E>
    ) {
        this.viewModelScope = scope
        this._state = state
        this.oneOffEventHandler = oneOffEventHandler
    }

    suspend fun sendEvent(event: E) {
        oneOffEventHandler.sendEvent(event)
    }
}
