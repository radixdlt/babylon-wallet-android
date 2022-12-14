package com.babylon.wallet.android.presentation.dapp.account

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.DAppDetailsResponse
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.dapp.GetAccountsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChooseDAppAccountViewModel @Inject constructor(
    private val getAccountsUseCase: GetAccountsUseCase,
) : ViewModel() {

    var accountsState by mutableStateOf(ChooseAccountUiState())
        private set

    init {
        viewModelScope.launch {
            when (val accountsResult = getAccountsUseCase(this)) {
                is Result.Success -> {
                    accountsState = accountsState.copy(
                        accounts = accountsResult.data.accounts,
                        dAppDetails = accountsResult.data.dAppResult.dAppDetails,
                        accountAddresses = accountsResult.data.dAppResult.accountAddresses,
                        error = null,
                        showProgress = false
                    )
                }
                is Result.Error -> {
                    accountsState = accountsState.copy(
                        accounts = null,
                        dAppDetails = null,
                        accountAddresses = null,
                        error = accountsResult.exception?.message,
                        showProgress = false
                    )
                }
            }
        }
    }

    fun onAccountSelect(account: SelectedAccountUiState) {
        if (accountsState.accounts == null) return

        // Required number of selected accounts
        val accountAddresses = accountsState.accountAddresses ?: 0

        accountsState.accounts?.let { accounts ->
            val currentlySelectedAccountsCount = accounts.count { it.selected }
            // If already selected max number of accounts (accountAddresses) and want to select more, skip
            if (currentlySelectedAccountsCount >= accountAddresses && !account.selected) {
                return
            }

            val updatedAccounts = accounts.map { accountUiState ->
                if (accountUiState == account) {
                    accountUiState.copy(
                        accountName = account.accountName,
                        accountAddress = account.accountAddress,
                        accountCurrency = account.accountCurrency,
                        accountValue = account.accountValue,
                        selected = !accountUiState.selected
                    )
                } else {
                    accountUiState
                }
            }

            val selectedAccountsCount: Int = updatedAccounts.count { updatedAccount ->
                updatedAccount.selected
            }

            accountsState = accountsState.copy(
                accounts = updatedAccounts,
                // We require user to select at least accountAddresses amount of accounts
                continueButtonEnabled = selectedAccountsCount >= accountAddresses
            )
        }
    }
}

data class ChooseAccountUiState(
    val accounts: List<SelectedAccountUiState>? = null,
    val dAppDetails: DAppDetailsResponse? = null,
    val accountAddresses: Int? = null,
    val continueButtonEnabled: Boolean = false,
    val error: String? = null,
    val showProgress: Boolean = true
)

data class SelectedAccountUiState(
    val accountName: String,
    val accountAddress: String,
    val accountCurrency: String,
    val accountValue: String,
    val selected: Boolean = false
)
