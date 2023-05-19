package com.babylon.wallet.android.presentation.transfer.accounts

import com.babylon.wallet.android.presentation.transfer.TargetAccount
import com.babylon.wallet.android.presentation.transfer.TransferViewModel
import com.babylon.wallet.android.presentation.transfer.TransferViewModel.State.Sheet.ChooseAccounts
import com.radixdlt.toolkit.RadixEngineToolkit
import com.radixdlt.toolkit.models.request.DecodeAddressRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
        index: Int,
        selectedAccounts: List<TargetAccount>
    ) {
        state.update {
            it.copy(
                sheet = ChooseAccounts(
                    selectedAccount = TargetAccount.Skeleton(index = index),
                    ownedAccounts = emptyList(),
                )
            )
        }

        viewModelScope.launch {
            val accounts = getProfileUseCase.accountsOnCurrentNetwork().filterNot { account ->
                account.address == fromAccount.address || selectedAccounts.any { it.address == account.address }
            }

            updateSheetState { it.copy(ownedAccounts = accounts) }
        }
    }

    fun addressTyped(address: String) {
        updateSheetState {
            val isValid = isAddressValid(address = address)
            it.copy(
                selectedAccount = TargetAccount.Other(
                    address = address,
                    isValidatedSuccessfully = isValid,
                    index = it.selectedAccount.index
                )
            )
        }
    }

    fun onQRModeStarted() {
        updateSheetState { it.copy(mode = ChooseAccounts.Mode.QRScanner) }
    }

    fun onQRModeCanceled() {
        updateSheetState { it.copy(mode = ChooseAccounts.Mode.Chooser) }
    }

    fun onQRAddressDecoded(address: String) {
        updateSheetState {
            val isValid = isAddressValid(address = address)
            it.copy(
                selectedAccount = TargetAccount.Other(
                    address = address,
                    isValidatedSuccessfully = isValid,
                    index = it.selectedAccount.index
                ),
                mode = ChooseAccounts.Mode.Chooser
            )
        }
    }

    fun onOwnedAccountSelected(account: Network.Account) {
        updateSheetState {
            it.copy(
                selectedAccount = TargetAccount.Owned(
                    account = account,
                    index = it.selectedAccount.index
                )
            )
        }
    }

    fun chooseAccountSubmitted() {
        val sheetState = state.value.sheet as? ChooseAccounts ?: return

        if (!sheetState.isChooseButtonEnabled) return

        state.update {
            val targetAccounts = it.targetAccounts.map { targetAccount ->
                if (targetAccount.index == sheetState.selectedAccount.index) {
                    // TODO copy assets if existed
                    sheetState.selectedAccount
                } else {
                    targetAccount
                }
            }

            it.copy(
                targetAccounts = targetAccounts,
                sheet = TransferViewModel.State.Sheet.None
            )
        }
    }

    /**
     * As per [REP 39 Bech32m and Addressing](https://radixdlt.atlassian.net/wiki/spaces/S/pages/2781839425/REP+39+Bech32m+and+Addressing)
     * The address need to be at least 26 chars long, and should be validated against KET.
     *
     * TODO: this should be a use case or a helper function
     */
    private fun isAddressValid(address: String): Boolean {
        return address.length >= 26 && RadixEngineToolkit.decodeAddress(DecodeAddressRequest(address)).isSuccess
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
