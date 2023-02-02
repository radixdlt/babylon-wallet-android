package com.babylon.wallet.android.presentation.dapp.account

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.common.OneOffEvent
import com.babylon.wallet.android.domain.common.OneOffEventHandler
import com.babylon.wallet.android.domain.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import rdx.works.profile.data.repository.AccountRepository
import javax.inject.Inject

@HiltViewModel
class ChooseAccountsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accountRepository: AccountRepository
) : ViewModel(), OneOffEventHandler<ChooseAccountsEvent> by OneOffEventHandlerImpl() {

    // the incoming request from dapp
    private val args = ChooseAccountsArgs(savedStateHandle)

    var state by mutableStateOf(
        ChooseAccountUiState(
            numberOfAccounts = args.numberOfAccounts,
            quantifier = args.accountQuantifier,
            oneTimeRequest = args.oneTime
        )
    )
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
                    availableAccountItems = accountItems.toPersistentList(),
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
                } == args.numberOfAccounts
        } else {
            state
                .availableAccountItems
                .count { accountItem ->
                    accountItem.isSelected
                } >= args.numberOfAccounts
        }

        state = state.copy(
            isContinueButtonEnabled = isMinRequiredCountOfAccountsSelected,
            selectedAccounts = state.availableAccountItems.filter { accountItem -> accountItem.isSelected }
        )
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
}

data class ChooseAccountUiState(
    val numberOfAccounts: Int,
    val quantifier: MessageFromDataChannel.IncomingRequest.AccountNumberQuantifier,
    val availableAccountItems: ImmutableList<AccountItemUiModel> = persistentListOf(),
    val isContinueButtonEnabled: Boolean = false,
    val oneTimeRequest: Boolean = false,
    val isSingleChoice: Boolean = false,
    val error: String? = null,
    val showProgress: Boolean = true,
    val selectedAccounts: List<AccountItemUiModel> = emptyList()
)
