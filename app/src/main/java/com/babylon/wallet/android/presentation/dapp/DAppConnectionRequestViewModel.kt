package com.babylon.wallet.android.presentation.dapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DAppConnectionRequestViewModel @Inject constructor(
    private val dAppConnectionRepository: DAppConnectionRepository
) : ViewModel() {

    private val _dAppConnectionUiState: MutableStateFlow<DAppConnectionUiState> =
        MutableStateFlow(DAppConnectionUiState.Loading)
    val dAppConnectionUiState = _dAppConnectionUiState.asStateFlow()

    init {
        viewModelScope.launch {
            val dAppConnectionData = dAppConnectionRepository.getDAppConnectionData()
            _dAppConnectionUiState.emit(DAppConnectionUiState.Loaded(dAppConnectionData))
        }
    }
}

sealed interface DAppConnectionUiState {
    object Loading : DAppConnectionUiState

    data class Loaded(
        val wallet: DAppConnectionData
    ) : DAppConnectionUiState
}
