package com.babylon.wallet.android.presentation.transfer.accounts

import com.babylon.wallet.android.presentation.transfer.TargetAccount
import com.babylon.wallet.android.presentation.transfer.TransferViewModel
import com.babylon.wallet.android.presentation.transfer.TransferViewModel.State.Sheet.ChooseAccounts
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.AddressValidator
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork

class AccountsChooserDelegate(
    private val state: MutableStateFlow<TransferViewModel.State>,
    private val viewModelScope: CoroutineScope,
    private val getProfileUseCase: GetProfileUseCase
) {

    fun onChooseAccount(
        fromAccount: Network.Account,
        slotAccount: TargetAccount,
        selectedAccounts: List<TargetAccount>
    ) {
        state.update {
            it.copy(
                sheet = ChooseAccounts(
                    selectedAccount = slotAccount,
                    ownedAccounts = persistentListOf(),
                )
            )
        }

        viewModelScope.launch {
            val accounts = getProfileUseCase.accountsOnCurrentNetwork().filterNot { account ->
                account.address == fromAccount.address || selectedAccounts.any { it.address == account.address }
            }

            updateSheetState { it.copy(ownedAccounts = accounts.toPersistentList()) }
        }
    }

    fun addressTyped(address: String) {
        updateSheetState { sheetState ->
            val validity = if (!AddressValidator.isValid(address)) {
                TargetAccount.Other.AddressValidity.INVALID
            } else {
                val selectedAccountAddresses = state.value.targetAccounts.map { it.address }
                if (address in selectedAccountAddresses || address == state.value.fromAccount?.address) {
                    TargetAccount.Other.AddressValidity.USED
                } else {
                    TargetAccount.Other.AddressValidity.VALID
                }
            }
            sheetState.copy(
                selectedAccount = TargetAccount.Other(
                    address = address,
                    validity = validity,
                    id = sheetState.selectedAccount.id,
                    assets = sheetState.selectedAccount.assets
                ),
                mode = ChooseAccounts.Mode.Chooser
            )
        }
    }

    fun onQRModeStarted() {
        updateSheetState { it.copy(mode = ChooseAccounts.Mode.QRScanner) }
    }

    fun onQRModeCanceled() {
        updateSheetState { it.copy(mode = ChooseAccounts.Mode.Chooser) }
    }

    fun onQRAddressDecoded(address: String) = addressTyped(address = address)

    fun onOwnedAccountSelected(account: Network.Account) {
        updateSheetState {
            it.copy(
                selectedAccount = TargetAccount.Owned(
                    account = account,
                    id = it.selectedAccount.id,
                    assets = it.selectedAccount.assets
                )
            )
        }
    }

    fun chooseAccountSubmitted() {
        val sheetState = state.value.sheet as? ChooseAccounts ?: return

        if (!sheetState.isChooseButtonEnabled) return

        state.update { state ->
            val ownedAccount = sheetState.ownedAccounts.find { it.address == sheetState.selectedAccount.address }
            val selectedAccount = if (ownedAccount != null) {
                TargetAccount.Owned(
                    account = ownedAccount,
                    id = sheetState.selectedAccount.id,
                    assets = sheetState.selectedAccount.assets
                )
            } else {
                sheetState.selectedAccount
            }

            val targetAccounts = state.targetAccounts.map { targetAccount ->
                if (targetAccount.id == selectedAccount.id) {
                    selectedAccount
                } else {
                    targetAccount
                }
            }

            state.copy(
                targetAccounts = targetAccounts.toPersistentList(),
                sheet = TransferViewModel.State.Sheet.None
            )
        }
    }

    private fun updateSheetState(
        onUpdate: (ChooseAccounts) -> ChooseAccounts
    ) {
        state.update { state ->
            if (state.sheet is ChooseAccounts) {
                state.copy(sheet = onUpdate(state.sheet))
            } else {
                state
            }
        }
    }
}
