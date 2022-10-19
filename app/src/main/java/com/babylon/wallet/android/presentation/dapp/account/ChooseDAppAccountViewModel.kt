package com.babylon.wallet.android.presentation.dapp.account

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.DAppAccountsData
import com.babylon.wallet.android.domain.dapp.DAppConnectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChooseDAppAccountViewModel @Inject constructor(
    private val dAppConnectionRepository: DAppConnectionRepository
) : ViewModel() {

    var state by mutableStateOf(ChooseDAppAccountUiState())
        private set

    var selectedIndexes = mutableStateMapOf<Int, Boolean>()
        private set

    init {
        viewModelScope.launch {
            val dAppAccountsData = dAppConnectionRepository.getChooseDAppAccountsData()
            selectedIndexes.apply {
                List(dAppAccountsData.dAppAccounts.size) { index ->
                    index to false
                }.toMap().also {
                    putAll(it)
                }
            }
            state = state.copy(
                loading = false,
                dAppAccountsData = DAppAccountsData(
                    dAppAccountsData.imageUrl,
                    dAppAccountsData.dAppAccounts
                )
            )
        }
    }

    fun onAccountSelect(selected: Boolean, index: Int) {
        selectedIndexes[index] = selected
    }
}

data class ChooseDAppAccountUiState(
    val loading: Boolean = true,
    val dAppAccountsData: DAppAccountsData? = null
)