package com.babylon.wallet.android

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UiState.Loaded)
    val uiState: StateFlow<UiState>
        get() = _uiState

}

sealed class UiState {
    object Loaded : UiState()
}