package com.babylon.wallet.android.presentation.dapp.authorized.account

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import rdx.works.profile.data.repository.AccountRepository
import java.util.Collections.emptyList
import javax.inject.Inject

@HiltViewModel
class ChooseAccountsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accountRepository: AccountRepository
) : ViewModel(), OneOffEventHandler<ChooseAccountsEvent> by OneOffEventHandlerImpl() {

    private val args = ChooseAccountsArgs(savedStateHandle)

    var state by mutableStateOf(
        ChooseAccountUiState(
            numberOfAccounts = args.numberOfAccounts,
            isExactAccountsCount = args.isExactAccountsCount,
            oneTimeRequest = args.oneTime,
            showBackButton = args.showBack
        )
    )

    init {
        viewModelScope.launch {
            accountRepository.accounts.collect { accounts ->
                // Check if single or multiple choice (radio or chechbox)
                val isSingleChoice = args.numberOfAccounts == 1 && args.isExactAccountsCount
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
                    showProgress = false,
                    isContinueButtonEnabled = !state.isExactAccountsCount && state.numberOfAccounts == 0
                )
            }
        }
    }

    fun onAccountSelect(index: Int) {
        // update the isSelected property of the AccountItemUiModel based on index
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

        state = state.copy(
            isContinueButtonEnabled = isContinueButtonEnabled,
            selectedAccounts = state.availableAccountItems.filter { accountItem -> accountItem.isSelected }
        )
    }
}

sealed interface ChooseAccountsEvent : OneOffEvent

data class ChooseAccountUiState(
    val numberOfAccounts: Int,
    val isExactAccountsCount: Boolean,
    val availableAccountItems: ImmutableList<AccountItemUiModel> = persistentListOf(),
    val isContinueButtonEnabled: Boolean = false,
    val oneTimeRequest: Boolean = false,
    val isSingleChoice: Boolean = false,
    val error: String? = null,
    val showProgress: Boolean = true,
    val showBackButton: Boolean = false,
    val selectedAccounts: List<AccountItemUiModel> = emptyList()
)
