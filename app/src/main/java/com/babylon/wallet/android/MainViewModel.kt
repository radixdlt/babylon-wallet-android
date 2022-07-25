package com.babylon.wallet.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState>
        get() = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Simulate network
            delay(500)
            // Mock data from backend for now
            _uiState.value = UiState.Loaded(
                WalletData(
                    "$",
                    "1000"
                )
            )
        }
    }
}

data class WalletData(
    val currency: String,
    val amount: String
)

sealed class UiState {
    object Loading : UiState()
    class Loaded(val walletData: WalletData) : UiState()
}