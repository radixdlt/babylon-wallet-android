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
import com.babylon.wallet.android.domain.common.OneOffEvent
import com.babylon.wallet.android.domain.common.OneOffEventHandler
import com.babylon.wallet.android.domain.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import rdx.works.profile.data.repository.AccountRepository
import javax.inject.Inject

@HiltViewModel
class ChooseAccountsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accountRepository: AccountRepository,
    private val dAppMessenger: DAppMessenger,
    incomingRequestRepository: IncomingRequestRepository
) : ViewModel(), OneOffEventHandler<ChooseAccountsEvent> by OneOffEventHandlerImpl() {

    private val args = ChooseAccountsScreenArgs(savedStateHandle)
    private val accountsRequest = incomingRequestRepository.getAuthorizedRequest(args.requestId)

    // TODO this is temporary until we have proper handling of CAP-21 requests models!
    @Suppress("TooGenericExceptionThrown")
    private val oneTimeAccountRequestItem =
        accountsRequest.oneTimeAccountsRequestItem
            ?: throw RuntimeException("Only oneTimeAccountsRequestItem supported")

    private val numberOfAccounts = oneTimeAccountRequestItem.numberOfAccounts
    private val isExactCountRequired = oneTimeAccountRequestItem
        .quantifier == MessageFromDataChannel.IncomingRequest.AccountNumberQuantifier.Exactly

    var state by mutableStateOf(ChooseAccountUiState())
        private set

    init {
        viewModelScope.launch {
            accountRepository.accounts.collect { accounts ->
                // Check if single or multiple choice (radio or chechbox)
                val isSingleChoice = numberOfAccounts == 1 && isExactCountRequired
                state = state.copy(
                    isSingleChoice = isSingleChoice
                )

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
                    dAppDetails = DAppDetailsResponse( // TODO when we have the actual dapp validation
                        imageUrl = "https://cdn-icons-png.flaticon.com/512/738/738680.png",
                        dAppName = "RadixSwap"
                    ),
                    error = null,
                    showProgress = false
                )
            }
        }
    }

    fun onAccountSelect(index: Int) {
        // update the isSelected property of the AccountItemUiModel based on index
        if (isExactCountRequired && numberOfAccounts == 1) {
            // Radio buttons selection unselects the previous one
            state = state.copy(
                availableAccountItems = state.availableAccountItems.mapIndexed { i, accountItem ->
                    if (index == i) {
                        accountItem.copy(isSelected = true)
                    } else {
                        accountItem.copy(isSelected = false)
                    }
                }
            )
        } else {
            state = state.copy(
                availableAccountItems = state.availableAccountItems.mapIndexed { i, accountItem ->
                    if (index == i) {
                        accountItem.copy(isSelected = !accountItem.isSelected)
                    } else {
                        accountItem
                    }
                }
            )
        }

        val isContinueButtonEnabled = if (isExactCountRequired) {
            state
                .availableAccountItems
                .count { accountItem ->
                    accountItem.isSelected
                } == numberOfAccounts
        } else {
            state
                .availableAccountItems
                .count { accountItem ->
                    accountItem.isSelected
                } >= numberOfAccounts
        }

        state = state.copy(isContinueButtonEnabled = isContinueButtonEnabled)
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
    val availableAccountItems: List<AccountItemUiModel> = emptyList(),
    val dAppDetails: DAppDetailsResponse? = null,
    val isContinueButtonEnabled: Boolean = false,
    val isSingleChoice: Boolean = false,
    val error: String? = null,
    val showProgress: Boolean = true
)
