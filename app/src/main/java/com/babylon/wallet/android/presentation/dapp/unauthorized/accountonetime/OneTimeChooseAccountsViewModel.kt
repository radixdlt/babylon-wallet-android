package com.babylon.wallet.android.presentation.dapp.unauthorized.accountonetime

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.account.toUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@HiltViewModel
class OneTimeChooseAccountsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProfileUseCase: GetProfileUseCase
) : StateViewModel<OneTimeChooseAccountUiState>(),
    OneOffEventHandler<OneTimeChooseAccountsEvent> by OneOffEventHandlerImpl() {

    private val args = OneTimeChooseAccountsArgs(savedStateHandle)

    override fun initialState(): OneTimeChooseAccountUiState = OneTimeChooseAccountUiState(
        numberOfAccounts = args.numberOfAccounts,
        isExactAccountsCount = args.isExactAccountsCount,
        isSingleChoice = isSingleChoice()
    )

    init {
        viewModelScope.launch {
            getProfileUseCase.flow.map { it.activeAccountsOnCurrentNetwork }.collect { accounts ->
                // user can create a new account at the Choose Accounts screen,
                // therefore this part ensures that the selection state (if any account was selected)
                // remains once the user returns from the account creation flow
                val accountItems = accounts.map { account ->
                    val currentAccountItemState = _state.value.availableAccountItems.find { accountItemUiModel ->
                        accountItemUiModel.address == account.address
                    }
                    val defaultSelected = isSingleChoice() && accounts.size == 1
                    account.toUiModel(currentAccountItemState?.isSelected ?: defaultSelected)
                }

                _state.update {
                    it.copy(
                        availableAccountItems = accountItems.toPersistentList(),
                        showProgress = false
                    )
                }
            }
        }
    }

    fun onAccountSelect(index: Int) {
        if (_state.value.isExactAccountsCount && _state.value.numberOfAccounts == 1) {
            // Radio buttons selection unselects the previous one
            _state.update {
                it.copy(
                    availableAccountItems = it.availableAccountItems.mapIndexed { i, accountItem ->
                        if (index == i) {
                            accountItem.copy(isSelected = true)
                        } else {
                            accountItem.copy(isSelected = false)
                        }
                    }.toPersistentList()
                )
            }
        } else {
            _state.update {
                it.copy(
                    availableAccountItems = it.availableAccountItems.mapIndexed { i, accountItem ->
                        if (index == i) {
                            accountItem.copy(isSelected = !accountItem.isSelected)
                        } else {
                            accountItem
                        }
                    }.toPersistentList()
                )
            }
        }
    }

    private fun isSingleChoice(): Boolean {
        return args.numberOfAccounts == 1 && args.isExactAccountsCount
    }
}

sealed interface OneTimeChooseAccountsEvent : OneOffEvent {
    data object NavigateToCompletionScreen : OneTimeChooseAccountsEvent
    data object FailedToSendResponse : OneTimeChooseAccountsEvent
}

data class OneTimeChooseAccountUiState(
    val availableAccountItems: ImmutableList<AccountItemUiModel> = persistentListOf(),
    val showProgress: Boolean = true,
    val numberOfAccounts: Int,
    val isExactAccountsCount: Boolean,
    val isSingleChoice: Boolean = false,
) : UiState {

    val isContinueButtonEnabled: Boolean
        get() = if (isExactAccountsCount) {
            availableAccountItems
                .count { accountItem ->
                    accountItem.isSelected
                } == numberOfAccounts
        } else {
            availableAccountItems.isEmpty() || availableAccountItems
                .count { accountItem ->
                    accountItem.isSelected
                } >= numberOfAccounts
        }

    fun selectedAccounts(): List<AccountItemUiModel> {
        return availableAccountItems
            .filter { accountItem ->
                accountItem.isSelected
            }
    }
}
