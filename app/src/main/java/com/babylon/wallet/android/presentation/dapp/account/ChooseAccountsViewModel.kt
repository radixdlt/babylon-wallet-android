package com.babylon.wallet.android.presentation.dapp.account

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.DAppDetailsResponse
import com.babylon.wallet.android.data.dapp.DAppMessenger
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.model.Account
import com.babylon.wallet.android.domain.common.OneOffEvent
import com.babylon.wallet.android.domain.common.OneOffEventHandler
import com.babylon.wallet.android.domain.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.dapp.GetAccountsUseCase
import com.babylon.wallet.android.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChooseAccountsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val dAppMessenger: DAppMessenger,
    incomingRequestRepository: IncomingRequestRepository
) : ViewModel(), OneOffEventHandler<ChooseAccountsEvent> by OneOffEventHandlerImpl() {

    private val accountsRequest = incomingRequestRepository.getAccountsRequest(
        savedStateHandle.get<String>(Screen.ARG_INCOMING_REQUEST_ID).orEmpty()
    )

    var state by mutableStateOf(ChooseAccountUiState())
        private set

    private var selectedAccounts = listOf<SelectedAccountUiState>()

    init {
        viewModelScope.launch {
            getAccountsUseCase(this).collect { result ->
                when (result) {
                    is Result.Success -> {
                        state = state.copy(
                            accounts = mergeSelectedState(result.data.accounts),
                            dAppDetails = result.data.dAppResult.dAppDetails,
                            accountAddresses = result.data.dAppResult.accountAddresses,
                            error = null,
                            showProgress = false
                        )
                    }
                    is Result.Error -> {
                        state = state.copy(
                            accounts = null,
                            dAppDetails = null,
                            accountAddresses = null,
                            error = result.exception?.message,
                            showProgress = false
                        )
                    }
                }
            }
        }
    }

    private fun mergeSelectedState(accounts: List<SelectedAccountUiState>): List<SelectedAccountUiState> {
        val selectedAddresses = state.accounts?.filter { it.selected }?.map { it.accountAddress }.orEmpty()
        return accounts.map {
            if (selectedAddresses.contains(it.accountAddress)) {
                it.copy(selected = true)
            } else {
                it
            }
        }
    }

    fun onAccountSelect(account: SelectedAccountUiState) {
        if (state.accounts == null) return

        state.accounts?.let { accounts ->
            val currentlySelectedAccountsCount = accounts.count { it.selected }
            // If already selected max number of accounts (accountAddresses) and want to select more, skip
            if (accountsRequest.numberOfAccounts != 0 &&
                currentlySelectedAccountsCount >= accountsRequest.numberOfAccounts && !account.selected
            ) {
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

            if (selectedAccountsCount >= accountsRequest.numberOfAccounts || accountsRequest.numberOfAccounts == 0) {
                selectedAccounts = updatedAccounts.filter { selectedAccountUiState ->
                    selectedAccountUiState.selected
                }
                state = state.copy(accounts = updatedAccounts, continueButtonEnabled = true)
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
            val result = dAppMessenger.sendAccountsResponse(requestId = accountsRequest.requestId, accounts = accounts)
            result.onValue {
                sendEvent(ChooseAccountsEvent.NavigateToCompletionScreen)
            }
        }
    }
}

sealed interface ChooseAccountsEvent : OneOffEvent {
    object NavigateToCompletionScreen : ChooseAccountsEvent
    object FailedToSendResponse : ChooseAccountsEvent
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
