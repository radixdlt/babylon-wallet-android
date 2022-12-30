package com.babylon.wallet.android.presentation.dapp.account

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.DAppDetailsResponse
import com.babylon.wallet.android.data.dapp.DAppMessenger
import com.babylon.wallet.android.data.dapp.model.Account
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.dapp.GetAccountsUseCase
import com.babylon.wallet.android.presentation.navigation.Screen
import com.babylon.wallet.android.utils.OneOffEventHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChooseAccountsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val dAppMessenger: DAppMessenger
) : ViewModel() {

    private val requestId = savedStateHandle.get<String>(Screen.ARG_REQUEST_ID) ?: "no request id"
    private val requiredMinNumberOfAccounts = savedStateHandle.get<Int>(Screen.ARG_NUMBER_OF_ACCOUNTS) ?: 0

    var state by mutableStateOf(ChooseAccountUiState())
        private set

    private val _oneOffEvent = OneOffEventHandler<OneOffEvent>()
    val oneOffEvent by _oneOffEvent

    private var selectedAccounts = listOf<SelectedAccountUiState>()

    init {
        viewModelScope.launch {
            when (val accountsResult = getAccountsUseCase(this)) {
                is Result.Success -> {
                    state = state.copy(
                        accounts = accountsResult.data.accounts,
                        dAppDetails = accountsResult.data.dAppResult.dAppDetails,
                        accountAddresses = accountsResult.data.dAppResult.accountAddresses,
                        error = null,
                        showProgress = false
                    )
                }
                is Result.Error -> {
                    state = state.copy(
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
        if (state.accounts == null) return

        state.accounts?.let { accounts ->
            val currentlySelectedAccountsCount = accounts.count { it.selected }
            // If already selected max number of accounts (accountAddresses) and want to select more, skip
            if (currentlySelectedAccountsCount >= requiredMinNumberOfAccounts && !account.selected) {
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

            if (selectedAccountsCount >= requiredMinNumberOfAccounts) {
                selectedAccounts = updatedAccounts.filter { selectedAccountUiState ->
                    selectedAccountUiState.selected
                }
                state = state.copy(
                    accounts = updatedAccounts,
                    continueButtonEnabled = true
                )
            }
        }
    }

    fun sendAccountsResponse() {
        viewModelScope.launch {
            val accounts = selectedAccounts.map { account ->
                Account(
                    address = account.accountAddress,
                    label = account.accountName,
                    appearanceId = account.appearanceID
                )
            }
            val result = dAppMessenger.sendAccountsResponse(
                requestId = requestId,
                accounts = accounts
            )
            result.onValue {
                _oneOffEvent.sendEvent(OneOffEvent.NavigateToCompletionScreen)
            }
        }
    }
}

sealed interface OneOffEvent {
    object NavigateToCompletionScreen : OneOffEvent
    object FailedToSendResponse : OneOffEvent
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
    val appearanceID: Int,
    val selected: Boolean = false
)
