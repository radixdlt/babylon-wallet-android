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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.activeAccountsOnCurrentNetwork
import javax.inject.Inject

@HiltViewModel
class OneTimeChooseAccountsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProfileUseCase: GetProfileUseCase
) : StateViewModel<OneTimeChooseAccountUiState>(), OneOffEventHandler<OneTimeChooseAccountsEvent> by OneOffEventHandlerImpl() {

    private val args = OneTimeChooseAccountsArgs(savedStateHandle)

    override fun initialState(): OneTimeChooseAccountUiState = OneTimeChooseAccountUiState(
        numberOfAccounts = args.numberOfAccounts,
        isExactAccountsCount = args.isExactAccountsCount,
        isSingleChoice = args.numberOfAccounts == 1 && args.isExactAccountsCount
    )

    init {
        viewModelScope.launch {
            getProfileUseCase.activeAccountsOnCurrentNetwork.collect { accounts ->
                // user can create a new account at the Choose Accounts screen,
                // therefore this part ensures that the selection state (if any account was selected)
                // remains once the user returns from the account creation flow
                val accountItems = accounts.map { account ->
                    val currentAccountItemState = _state.value.availableAccountItems.find { accountItemUiModel ->
                        accountItemUiModel.address == account.address
                    }
                    account.toUiModel(currentAccountItemState?.isSelected ?: false)
                }

                _state.update {
                    it.copy(
                        availableAccountItems = accountItems.toPersistentList(),
                        showProgress = false,
                        isContinueButtonEnabled = !it.isExactAccountsCount && it.numberOfAccounts == 0
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
        val isContinueButtonEnabled = if (_state.value.isExactAccountsCount) {
            _state
                .value
                .availableAccountItems
                .count { accountItem ->
                    accountItem.isSelected
                } == args.numberOfAccounts
        } else {
            _state
                .value
                .availableAccountItems
                .count { accountItem ->
                    accountItem.isSelected
                } >= args.numberOfAccounts
        }

        _state.update { it.copy(isContinueButtonEnabled = isContinueButtonEnabled) }
    }
}

sealed interface OneTimeChooseAccountsEvent : OneOffEvent {
    data object NavigateToCompletionScreen : OneTimeChooseAccountsEvent
    data object FailedToSendResponse : OneTimeChooseAccountsEvent
}

data class OneTimeChooseAccountUiState(
    val availableAccountItems: ImmutableList<AccountItemUiModel> = persistentListOf(),
    val isContinueButtonEnabled: Boolean = false,
    val showProgress: Boolean = true,
    val numberOfAccounts: Int,
    val isExactAccountsCount: Boolean,
    val isSingleChoice: Boolean = false,
) : UiState {
    fun selectedAccounts(): List<AccountItemUiModel> {
        return availableAccountItems
            .filter { accountItem ->
                accountItem.isSelected
            }
    }
}
