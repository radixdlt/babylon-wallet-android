package com.babylon.wallet.android.presentation.dapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChooseDAppLoginViewModel @Inject constructor(
    private val dAppConnectionRepository: DAppConnectionRepository
) : ViewModel() {

    private val _chooseDAppLoginUiState: MutableStateFlow<ChooseDAppLoginUiState> =
        MutableStateFlow(ChooseDAppLoginUiState.Loading)
    val chooseDAppLoginUiState = _chooseDAppLoginUiState.asStateFlow()

    init {
        viewModelScope.launch {
            val dAppConnectionData = dAppConnectionRepository.getChooseDAppLoginData()
            _chooseDAppLoginUiState.emit(ChooseDAppLoginUiState.Loaded(dAppConnectionData))
        }
    }
}

sealed interface ChooseDAppLoginUiState {
    object Loading : ChooseDAppLoginUiState

    data class Loaded(
        val dAppData: DAppConnectionData
    ) : ChooseDAppLoginUiState
}
