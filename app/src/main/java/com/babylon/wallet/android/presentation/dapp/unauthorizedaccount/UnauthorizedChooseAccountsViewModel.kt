package com.babylon.wallet.android.presentation.dapp.unauthorizedaccount

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.DAppMessenger
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.domain.common.OneOffEvent
import com.babylon.wallet.android.domain.common.OneOffEventHandler
import com.babylon.wallet.android.domain.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.domain.common.onError
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.presentation.dapp.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.dapp.account.toUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import rdx.works.profile.data.repository.AccountRepository
import javax.inject.Inject

@HiltViewModel
class UnauthorizedChooseAccountsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accountRepository: AccountRepository,
    private val dAppMessenger: DAppMessenger,
    private val incomingRequestRepository: IncomingRequestRepository
) : ViewModel(), OneOffEventHandler<UnauthorizedChooseAccountsEvent> by OneOffEventHandlerImpl() {

    // the incoming request from dapp

    private val args = ChooseAccountsArgs(savedStateHandle)

    private val accountsRequest = incomingRequestRepository.getUnauthorizedRequest(
        args.requestId
    )

    // TODO this is temporary until we have proper handling of CAP-21 requests models!
    @Suppress("TooGenericExceptionThrown")
    private val oneTimeAccountRequestItem =
        accountsRequest.oneTimeAccountsRequestItem
            ?: throw RuntimeException("Only oneTimeAccountsRequestItem supported")

    var state by mutableStateOf(UnauthorizedChooseAccountUiState())
        private set

    init {
        viewModelScope.launch {
            accountRepository.accounts.collect { accounts ->
                // user can create a new account at the Choose Accounts screen,
                // therefore this part ensures that the selection state (if any account was selected)
                // remains once the user returns from the account creation flow
                val accountItems = accounts.map { account ->
                    val currentAccountItemState = state.availableAccountItems.find { accountItemUiModel ->
                        accountItemUiModel.address == account.address
                    }
                    account.toUiModel(currentAccountItemState?.isSelected ?: false)
                }

                state = state.copy(
                    availableAccountItems = accountItems,
                    error = null,
                    showProgress = false
                )
            }
        }
    }

    fun onAccountSelect(index: Int) {
        // update the isSelected property of the AccountItemUiModel based on index
        state = state.copy(
            availableAccountItems = state.availableAccountItems.mapIndexed { i, accountItem ->
                if (index == i) {
                    accountItem.copy(isSelected = !accountItem.isSelected)
                } else {
                    accountItem
                }
            }
        )

        val isMinRequiredCountOfAccountsSelected = state
            .availableAccountItems
            .count { accountItem ->
                accountItem.isSelected
            } >= oneTimeAccountRequestItem.numberOfAccounts

        state = state.copy(isContinueButtonEnabled = isMinRequiredCountOfAccountsSelected)
    }

    fun sendAccountsResponse() {
        // get the accounts that are selected
        val selectedAccounts = state.availableAccountItems
            .filter { accountItem ->
                accountItem.isSelected
            }
        viewModelScope.launch {
            val result = dAppMessenger.sendAccountsResponse(
                requestId = accountsRequest.requestId,
                accounts = selectedAccounts
            )
            result.onValue {
                sendEvent(UnauthorizedChooseAccountsEvent.NavigateToCompletionScreen)
            }
            result.onError {
                sendEvent(UnauthorizedChooseAccountsEvent.NavigateToCompletionScreen)
            }
        }
    }
}

sealed interface UnauthorizedChooseAccountsEvent : OneOffEvent {
    object NavigateToCompletionScreen : UnauthorizedChooseAccountsEvent
    object FailedToSendResponse : UnauthorizedChooseAccountsEvent
}

data class UnauthorizedChooseAccountUiState(
    val availableAccountItems: List<AccountItemUiModel> = emptyList(),
    val isContinueButtonEnabled: Boolean = false,
    val error: String? = null,
    val showProgress: Boolean = true
)
