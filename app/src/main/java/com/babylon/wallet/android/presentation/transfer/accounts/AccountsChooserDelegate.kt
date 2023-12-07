package com.babylon.wallet.android.presentation.transfer.accounts

import com.babylon.wallet.android.domain.usecases.assets.GetWalletAssetsUseCase
import com.babylon.wallet.android.presentation.common.ViewModelDelegate
import com.babylon.wallet.android.presentation.transfer.TargetAccount
import com.babylon.wallet.android.presentation.transfer.TransferViewModel
import com.babylon.wallet.android.presentation.transfer.TransferViewModel.State.Sheet.ChooseAccounts
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.AddressValidator
import rdx.works.profile.data.model.extensions.hasAcceptKnownDepositRule
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork
import javax.inject.Inject

class AccountsChooserDelegate @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val getWalletAssetsUseCase: GetWalletAssetsUseCase
) : ViewModelDelegate<TransferViewModel.State>() {

    suspend fun onChooseAccount(
        fromAccount: Network.Account,
        slotAccount: TargetAccount,
        selectedAccounts: List<TargetAccount>
    ) {
        _state.update {
            it.copy(
                sheet = ChooseAccounts(
                    selectedAccount = slotAccount,
                    ownedAccounts = persistentListOf(),
                    isLoadingAssetsForAccount = false
                )
            )
        }

        val accounts = getProfileUseCase.accountsOnCurrentNetwork().filterNot { account ->
            account.address == fromAccount.address || selectedAccounts.any { it.address == account.address }
        }

        updateSheetState { it.copy(ownedAccounts = accounts.toPersistentList()) }
    }

    fun addressTyped(address: String) {
        val currentNetworkId = _state.value.fromAccount?.networkID ?: return
        updateSheetState { sheetState ->
            val validity = if (!AddressValidator.isValid(address = address, networkId = currentNetworkId)) {
                TargetAccount.Other.AddressValidity.INVALID
            } else {
                val selectedAccountAddresses = _state.value.targetAccounts.map { it.address }
                if (address in selectedAccountAddresses || address == _state.value.fromAccount?.address) {
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
                    spendingAssets = sheetState.selectedAccount.spendingAssets
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
                    spendingAssets = it.selectedAccount.spendingAssets
                )
            )
        }
    }

    fun chooseAccountSubmitted() {
        val sheetState = _state.value.sheet as? ChooseAccounts ?: return

        if (!sheetState.isChooseButtonEnabled) return

        _state.update {
            it.copy(
                sheet = sheetState.copy(
                    isLoadingAssetsForAccount = true
                )
            )
        }

        viewModelScope.launch {
            _state.update { state ->
                val ownedAccount = sheetState.ownedAccounts.find { it.address == sheetState.selectedAccount.address }
                val selectedAccount = if (ownedAccount != null) {
                    // if the target owned account has accept known rule then we need to fetch its known resources
                    // in order to later check if a an extra signature is required
                    val areTargetAccountResourcesRequired = ownedAccount.hasAcceptKnownDepositRule()

                    TargetAccount.Owned(
                        account = ownedAccount,
                        accountAssetsAddresses = if (areTargetAccountResourcesRequired) {
                            fetchKnownResourcesOfOwnedAccount(
                                ownedAccount = ownedAccount
                            )
                        } else {
                            emptyList()
                        },
                        id = sheetState.selectedAccount.id,
                        spendingAssets = sheetState.selectedAccount.spendingAssets
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
                    sheet = TransferViewModel.State.Sheet.None,
                )
            }
        }
    }

    private suspend fun fetchKnownResourcesOfOwnedAccount(ownedAccount: Network.Account): List<String> {
        return getWalletAssetsUseCase(
            accounts = listOf(ownedAccount),
            isRefreshing = false
        ).first()
            .first()
            .assets
            ?.knownResources
            ?.map { resource ->
                resource.resourceAddress
            }.orEmpty()
    }

    private fun updateSheetState(
        onUpdate: (ChooseAccounts) -> ChooseAccounts
    ) {
        _state.update { state ->
            if (state.sheet is ChooseAccounts) {
                state.copy(sheet = onUpdate(state.sheet))
            } else {
                state
            }
        }
    }
}
