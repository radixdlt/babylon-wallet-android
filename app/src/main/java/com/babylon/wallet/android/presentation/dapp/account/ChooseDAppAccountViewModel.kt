package com.babylon.wallet.android.presentation.dapp.account

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.DAppAccountUiState
import com.babylon.wallet.android.domain.dapp.GetDAppAccountsUseCase
import com.babylon.wallet.android.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChooseDAppAccountViewModel @Inject constructor(
    private val getDAppAccountsUseCase: GetDAppAccountsUseCase
) : ViewModel() {

    var accountsState by mutableStateOf(ChooseAccountUiState())
        private set

    init {
        viewModelScope.launch {
            val accounts = getDAppAccountsUseCase.getDAppAccounts()
            delay(1000)
            accountsState = accountsState.copy(
                accounts = accounts,
                initialPage = false,
                destination = Screen.DAppCompleteDestination
            )
        }
    }

    fun onAccountSelect(account: DAppAccountUiState) {
        val updatedAccount = accountsState.accounts?.map { accountUiState ->
            if (accountUiState == account) {
                accountUiState.copy(
                    account = accountUiState.account,
                    selected = !accountUiState.selected
                )
            }
            else accountUiState
        }
        accountsState = accountsState.copy(
            accounts = updatedAccount
        )
    }
}

data class ChooseAccountUiState(
    val accounts: List<DAppAccountUiState>? = null,
    val initialPage: Boolean = true,
    val destination: Screen? = null
)