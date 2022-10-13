package com.babylon.wallet.android.presentation.dapp.login.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.dapp.data.DAppConnectionData
import com.babylon.wallet.android.presentation.dapp.domain.DAppConnectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChooseDAppLoginViewModel @Inject constructor(
    private val dAppConnectionRepository: DAppConnectionRepository
) : ViewModel() {

    var selected by mutableStateOf(false)
        private set

    var uiState by mutableStateOf(ChooseDAppLoginUiState())
        private set

    init {
        viewModelScope.launch {
            val dAppConnectionData = dAppConnectionRepository.getChooseDAppLoginData()
            uiState = uiState.copy(
                loading = false,
                dAppData = dAppConnectionData
            )
        }
    }

    fun onSelectChange(selected: Boolean) {
        this.selected = selected
    }
}

data class ChooseDAppLoginUiState(
    val loading: Boolean = true,
    val dAppData: DAppConnectionData? = null
)
