package com.babylon.wallet.android.presentation.dapp.unauthorized.accountonetime

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.account.toUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import rdx.works.profile.data.repository.AccountRepository
import javax.inject.Inject

@HiltViewModel
class OneTimeChooseAccountsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accountRepository: AccountRepository
) : ViewModel(), OneOffEventHandler<OneTimeChooseAccountsEvent> by OneOffEventHandlerImpl() {

    private val args = OneTimeChooseAccountsArgs(savedStateHandle)

    var state by mutableStateOf(
        OneTimeChooseAccountUiState(
            numberOfAccounts = args.numberOfAccounts,
            isExactAccountsCount = args.isExactAccountsCount,
            isSingleChoice = args.numberOfAccounts == 1 && args.isExactAccountsCount
        )
    )
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
                    availableAccountItems = accountItems.toPersistentList(),
                    error = null,
                    showProgress = false,
                    isContinueButtonEnabled = !state.isExactAccountsCount && state.numberOfAccounts == 0
                )
            }
        }
    }

    fun onAccountSelect(index: Int) {
        if (state.isExactAccountsCount && state.numberOfAccounts == 1) {
            // Radio buttons selection unselects the previous one
            state = state.copy(
                availableAccountItems = state.availableAccountItems.mapIndexed { i, accountItem ->
                    if (index == i) {
                        accountItem.copy(isSelected = true)
                    } else {
                        accountItem.copy(isSelected = false)
                    }
                }.toPersistentList()
            )
        } else {
            state = state.copy(
                availableAccountItems = state.availableAccountItems.mapIndexed { i, accountItem ->
                    if (index == i) {
                        accountItem.copy(isSelected = !accountItem.isSelected)
                    } else {
                        accountItem
                    }
                }.toPersistentList()
            )
        }
        val isContinueButtonEnabled = if (state.isExactAccountsCount) {
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

        state = state.copy(isContinueButtonEnabled = isContinueButtonEnabled)
    }
}

sealed interface OneTimeChooseAccountsEvent : OneOffEvent {
    object NavigateToCompletionScreen : OneTimeChooseAccountsEvent
    object FailedToSendResponse : OneTimeChooseAccountsEvent
}

data class OneTimeChooseAccountUiState(
    val availableAccountItems: ImmutableList<AccountItemUiModel> = persistentListOf(),
    val isContinueButtonEnabled: Boolean = false,
    val error: String? = null,
    val showProgress: Boolean = true,
    val numberOfAccounts: Int,
    val isExactAccountsCount: Boolean,
    val isSingleChoice: Boolean = false,
) {
    fun selectedAccounts(): List<AccountItemUiModel> {
        return availableAccountItems
            .filter { accountItem ->
                accountItem.isSelected
            }
    }
}
