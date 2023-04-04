package com.babylon.wallet.android.presentation.dapp.authorized.account

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork
import java.util.Collections.emptyList
import javax.inject.Inject

@HiltViewModel
class ChooseAccountsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProfileUseCase: GetProfileUseCase
) : ViewModel(), OneOffEventHandler<ChooseAccountsEvent> by OneOffEventHandlerImpl() {

    private val args = ChooseAccountsArgs(savedStateHandle)

    private val _state = MutableStateFlow(
        ChooseAccountUiState(
            numberOfAccounts = args.numberOfAccounts,
            isExactAccountsCount = args.isExactAccountsCount,
            oneTimeRequest = args.oneTime,
            showBackButton = args.showBack
        )
    )
    var state: StateFlow<ChooseAccountUiState> = _state

    init {
        viewModelScope.launch {
            getProfileUseCase.accountsOnCurrentNetwork.collect { accounts ->
                // Check if single or multiple choice (radio or chechbox)
                val isSingleChoice = args.numberOfAccounts == 1 && args.isExactAccountsCount
                _state.update { it.copy(isSingleChoice = isSingleChoice) }

                // user can create a new account at the Choose Accounts screen,
                // therefore this part ensures that the selection state (if any account was selected)
                // remains once the user returns from the account creation flow
                val accountItems = accounts.map { account ->
                    val currentAccountItemState =
                        _state.value.availableAccountItems.find { accountItemUiModel ->
                            accountItemUiModel.address == account.address
                        }
                    account.toUiModel(currentAccountItemState?.isSelected ?: false)
                }

                _state.update {
                    it.copy(
                        availableAccountItems = accountItems.toPersistentList(),
                        error = null,
                        showProgress = false,
                        isContinueButtonEnabled = !it.isExactAccountsCount && it.numberOfAccounts == 0
                    )
                }
            }
        }
    }

    fun onAccountSelect(index: Int) {
        // update the isSelected property of the AccountItemUiModel based on index
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

        _state.update {
            it.copy(
                isContinueButtonEnabled = isContinueButtonEnabled,
                selectedAccounts = it.availableAccountItems.filter { accountItem -> accountItem.isSelected }
            )
        }
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
