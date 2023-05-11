package com.babylon.wallet.android.presentation.transfer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork
import rdx.works.profile.domain.accountsOnCurrentNetwork
import javax.inject.Inject

@HiltViewModel
class TransferViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    savedStateHandle: SavedStateHandle
) : StateViewModel<TransferViewModel.State>() {

    internal val args = TransferArgs(savedStateHandle)

    override fun initialState(): State = State()

    init {
        viewModelScope.launch {
            val sourceAccount = getProfileUseCase.accountOnCurrentNetwork(args.accountId)

            val destinationAccounts = getProfileUseCase.accountsOnCurrentNetwork().filter { it != sourceAccount }
            sourceAccount?.let { account ->
                _state.update {
                    it.copy(
                        fromAccount = AccountItemUiModel(
                            address = account.address,
                            displayName = account.displayName,
                            appearanceID = account.appearanceID
                        ),
                        receivingAccounts = destinationAccounts.map { destinationAccount ->
                            AccountItemUiModel(
                                address = destinationAccount.address,
                                displayName = destinationAccount.displayName,
                                appearanceID = destinationAccount.appearanceID
                            )
                        }.toPersistentList()
                    )
                }
            }
        }
    }

    fun onMessageChanged(message: String) {
        _state.update {
            it.copy(
                message = message
            )
        }
    }

    fun onAddressChanged(address: String) {
        val updatedAccounts = _state.value.receivingAccounts.map { account ->
            account.copy(
                address = account.address,
                displayName = account.displayName,
                appearanceID = account.appearanceID,
                isSelected = false
            )
        }
        _state.update {
            it.copy(
                address = address,
                buttonEnabled = address.isNotEmpty(),
                accountsDisabled = address.isNotEmpty(),
                receivingAccounts = updatedAccounts.toPersistentList()
            )
        }
    }

    fun onAccountSelect(accountIndex: Int) {
        val updatedAccounts = _state.value.receivingAccounts.mapIndexed { index, account ->
            if (index == accountIndex) {
                account.copy(
                    address = account.address,
                    displayName = account.displayName,
                    appearanceID = account.appearanceID,
                    isSelected = !account.isSelected
                )
            } else {
                account.copy(
                    address = account.address,
                    displayName = account.displayName,
                    appearanceID = account.appearanceID,
                    isSelected = false
                )
            }
        }
        _state.update {
            it.copy(
                receivingAccounts = updatedAccounts.toPersistentList(),
                buttonEnabled = updatedAccounts.any { account -> account.isSelected },
                accountsDisabled = false
            )
        }
    }

    fun addAccountClick() {
        val updatedSelectedAccounts = _state.value.selectedAccounts.toMutableList()
        updatedSelectedAccounts.add(
            State.SelectedAccountForTransfer()
        )

        _state.update {
            it.copy(
                selectedAccounts = updatedSelectedAccounts.toPersistentList()
            )
        }
    }

    fun deleteAccountClick(index: Int) {
        val updatedSelectedAccounts = _state.value.selectedAccounts.toMutableList()
        updatedSelectedAccounts.removeAt(index)

        _state.update {
            it.copy(
                selectedAccounts = updatedSelectedAccounts.toPersistentList()
            )
        }
    }

    fun onChooseClick(index: Int) {
        _state.update {
            it.copy(
                chosenAccountIndex = index
            )
        }
    }

    fun onChooseDestinationAccountClick() {
        val selectedDestinationAccount = _state.value.receivingAccounts.find { it.isSelected }
        val chosenIndex = _state.value.chosenAccountIndex
        val updatedSelectedAccounts = _state.value.selectedAccounts.mapIndexed { index, account ->
            if (chosenIndex == index) {
                if (selectedDestinationAccount != null) {
                    // We selected account we hold
                    State.SelectedAccountForTransfer(
                        account = selectedDestinationAccount,
                        type = State.SelectedAccountForTransfer.Type.ExistingAccount
                    )
                } else {
                    // We provide external account address
                    State.SelectedAccountForTransfer(
                        account = AccountItemUiModel(
                            address = _state.value.address,
                            displayName = "Account",
                            appearanceID = 0
                        ),
                        type = State.SelectedAccountForTransfer.Type.ThirdPartyAccount
                    )
                }
            } else {
                account
            }
        }
        _state.update {
            it.copy(
                selectedAccounts = updatedSelectedAccounts.toPersistentList()
            )
        }
    }

    fun onAddressDecoded(address: String) {
        _state.update {
            it.copy(
                address = address,
                chooseAccountSheetMode = ChooseAccountSheetMode.Default,
                buttonEnabled = address.isNotEmpty()
            )
        }
    }

    fun onQrCodeIconClick() {
        _state.update {
            it.copy(
                chooseAccountSheetMode = ChooseAccountSheetMode.ScanQr
            )
        }
    }

    fun cancelQrScan() {
        _state.update {
            it.copy(
                chooseAccountSheetMode = ChooseAccountSheetMode.Default
            )
        }
    }

    data class State(
        val fromAccount: AccountItemUiModel? = null,
        val receivingAccounts: ImmutableList<AccountItemUiModel> = persistentListOf(),
        val selectedAccounts: ImmutableList<SelectedAccountForTransfer> = persistentListOf(
            SelectedAccountForTransfer()
        ),
        val chosenAccountIndex: Int? = null,
        val address: String = "",
        val message: String = "",
        val buttonEnabled: Boolean = false,
        val accountsDisabled: Boolean = false,
        val chooseAccountSheetMode: ChooseAccountSheetMode = ChooseAccountSheetMode.Default,
    ) : UiState {

        data class SelectedAccountForTransfer(
            val account: AccountItemUiModel? = null,
            val type: Type = Type.NoAccount
        ) {
            enum class Type {
                NoAccount, ThirdPartyAccount, ExistingAccount
            }
        }
    }
}

enum class ChooseAccountSheetMode {
    Default, ScanQr
}
