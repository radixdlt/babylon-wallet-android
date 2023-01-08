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
import com.babylon.wallet.android.domain.dapp.GetAccountsUseCase
import com.babylon.wallet.android.domain.model.AccountResources
import com.babylon.wallet.android.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChooseAccountsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val dAppMessenger: DAppMessenger,
    incomingRequestRepository: IncomingRequestRepository
) : ViewModel(), OneOffEventHandler<ChooseAccountsEvent> by OneOffEventHandlerImpl() {

    // the incoming request from dapp
    private val accountsRequest = incomingRequestRepository.getAccountsRequest(
        savedStateHandle.get<String>(Screen.ARG_INCOMING_REQUEST_ID).orEmpty()
    )

    var state by mutableStateOf(ChooseAccountUiState())
        private set

    // keep all the available accounts in the profile
    private var currentAvailableAccounts = listOf<AccountResources>()

    private var observeAccountsJob: Job? = null

    init {
        observeAccountsJob = viewModelScope.launch {
            getAccountsUseCase().collect { accountsForSelection ->
                currentAvailableAccounts = accountsForSelection
                // user can create a new account at the Choose Accounts screen,
                // therefore this part ensures that the selection state (if any account was selected)
                // remains once the user returns from the account creation flow
                val accountItems = accountsForSelection.map { accountResources ->
                    val currentAccountItemState = state.availableAccountItems.find { accountItemUiModel ->
                        accountItemUiModel.address == accountResources.address
                    }
                    accountResources.toUiModel(currentAccountItemState?.isSelected ?: false)
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
        state = state.copy(
            availableAccountItems = state.availableAccountItems.mapIndexed { i, accountItem ->
                if (index == i) {
                    accountItem.copy(isSelected = !accountItem.isSelected)
                } else {
                    accountItem
                }
            }
        )

        val isRequiredCountOfAccountsSelected = state
            .availableAccountItems
            .count { accountItem ->
                accountItem.isSelected
            } >= accountsRequest.numberOfAccounts

        state = if (isRequiredCountOfAccountsSelected) {
            state.copy(isContinueButtonEnabled = true)
        } else {
            state.copy(isContinueButtonEnabled = false)
        }
    }

    fun sendAccountsResponse() {
        // get the accounts that are selected
        val selectedAccounts = currentAvailableAccounts
            .filter { accountResources ->
                accountResources.address in state.availableAccountItems
                    .filter { accountItem ->
                        accountItem.isSelected
                    }
                    .map { selectedAccount ->
                        selectedAccount.address
                    }
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

    override fun onCleared() {
        super.onCleared()
        observeAccountsJob?.cancel()
        currentAvailableAccounts = emptyList()
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
    val error: String? = null,
    val showProgress: Boolean = true
)
