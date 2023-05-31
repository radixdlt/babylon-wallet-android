package com.babylon.wallet.android.presentation.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Suppress("VariableNaming")
abstract class Stateful<T : UiState> {
    protected val _state: MutableStateFlow<T> by lazy { MutableStateFlow(initialState()) }
    val state: StateFlow<T>
        get() = _state.asStateFlow()

    abstract fun initialState(): T
}
