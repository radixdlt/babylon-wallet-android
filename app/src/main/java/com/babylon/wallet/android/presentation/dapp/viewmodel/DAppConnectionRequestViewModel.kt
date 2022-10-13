package com.babylon.wallet.android.presentation.dapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.dapp.model.DAppConnectionData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DAppConnectionRequestViewModel @Inject constructor(
    private val dAppConnectionRepository: DAppConnectionRepository
) : ViewModel() {

    var uiState by mutableStateOf(DAppConnectionUiState())
        private set

    init {
        viewModelScope.launch {
            val dAppConnectionData = dAppConnectionRepository.getDAppConnectionData()
            uiState = uiState.copy(
                loading = false,
                dAppConnectionData = dAppConnectionData
            )
        }
    }
}

data class DAppConnectionUiState(
    val loading: Boolean = true,
    val dAppConnectionData: DAppConnectionData? = null
)
