package com.babylon.wallet.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(private val mainViewRepository: MainViewRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState>
        get() = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            mainViewRepository.getWalletData().collect {
                _uiState.value = UiState.Loaded(it)
            }
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
