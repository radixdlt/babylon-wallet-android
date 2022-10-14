package com.babylon.wallet.android.presentation.dapp.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.DAppConnectionData
import com.babylon.wallet.android.domain.dapp.DAppConnectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChooseDAppLoginViewModel @Inject constructor(
    private val dAppConnectionRepository: DAppConnectionRepository
) : ViewModel() {

    var selected by mutableStateOf(false)
        private set

    var state by mutableStateOf(ChooseDAppLoginUiState())
        private set

    init {
        viewModelScope.launch {
            val dAppConnectionData = dAppConnectionRepository.getChooseDAppLoginData()
            state = state.copy(
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
