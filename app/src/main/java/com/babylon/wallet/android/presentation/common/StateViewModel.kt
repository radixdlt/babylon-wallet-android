package com.babylon.wallet.android.presentation.common

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface UiState

@Suppress("VariableNaming")
abstract class StateViewModel<T : UiState>() : ViewModel() {
    protected val _state: MutableStateFlow<T> by lazy { MutableStateFlow(initialState()) }
    val state: StateFlow<T>
        get() = _state.asStateFlow()

    abstract fun initialState(): T
}
