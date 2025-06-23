package com.babylon.wallet.android.presentation.transfer.accounts

import com.babylon.wallet.android.domain.usecases.IsValidRadixDomainUseCase
import com.babylon.wallet.android.domain.usecases.ResolveRadixDomainUseCase
import com.babylon.wallet.android.domain.usecases.assets.GetWalletAssetsUseCase
import com.babylon.wallet.android.presentation.common.NetworkContent
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.ViewModelDelegate
import com.babylon.wallet.android.presentation.transfer.TargetAccount
import com.babylon.wallet.android.presentation.transfer.TransferViewModel
import com.babylon.wallet.android.presentation.transfer.TransferViewModel.State.Sheet.ChooseAccounts
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.extensions.string
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.update
import rdx.works.core.domain.validatedOnNetworkOrNull
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.core.sargon.hasAcceptKnownDepositRule
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

class AccountsChooserDelegate @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val getWalletAssetsUseCase: GetWalletAssetsUseCase,
    private val resolveRadixDomainUseCase: ResolveRadixDomainUseCase,
    private val isValidRadixDomainUseCase: IsValidRadixDomainUseCase
) : ViewModelDelegate<TransferViewModel.State>() {

    suspend fun onChooseAccount(
        fromAccount: Account,
        slotAccount: TargetAccount,
        selectedAccounts: List<TargetAccount>
    ) {
        _state.update {
            it.copy(
                sheet = ChooseAccounts(
                    selectedAccount = slotAccount,
                    ownedAccounts = persistentListOf(),
                    isResolving = false
                )
            )
        }

        val accounts = getProfileUseCase().activeAccountsOnCurrentNetwork.filterNot { account ->
            account.address == fromAccount.address || selectedAccounts.any { it.address == account.address }
        }

        updateSheetState { it.copy(ownedAccounts = accounts.toPersistentList()) }
    }

    fun onReceiverChanged(receiver: String) {
        val currentNetworkId = _state.value.fromAccount?.networkId ?: return
        val receiverLowercase = receiver.lowercase()
        updateSheetState { sheetState ->
            val maybeAccountAddress =
                AccountAddress.validatedOnNetworkOrNull(receiverLowercase, currentNetworkId)

            val inputValidity = if (maybeAccountAddress != null) {
                val fromAccountAddressString = _state.value.fromAccount?.address?.string.orEmpty()
                val selectedAccountAddressesString =
                    _state.value.targetAccounts.map { it.address?.string.orEmpty() }
                if (receiverLowercase in selectedAccountAddressesString || receiverLowercase == fromAccountAddressString) {
                    TargetAccount.Other.InputValidity.ADDRESS_USED
                } else {
                    TargetAccount.Other.InputValidity.VALID
                }
            } else if (isValidRadixDomainUseCase(receiverLowercase)) {
                TargetAccount.Other.InputValidity.VALID
            } else {
                TargetAccount.Other.InputValidity.INVALID
            }

            sheetState.copy(
                selectedAccount = TargetAccount.Other(
                    typed = receiver,
                    resolvedInput = null,
                    validity = inputValidity,
                    id = sheetState.selectedAccount.id,
                    spendingAssets = sheetState.selectedAccount.spendingAssets
                ),
                mode = ChooseAccounts.Mode.Chooser
            )
        }
    }

    fun onErrorMessageShown() {
        updateSheetState { it.copy(uiMessage = null) }
    }

    fun onQRModeStarted() {
        updateSheetState { it.copy(mode = ChooseAccounts.Mode.QRScanner) }
    }

    fun onQRModeCanceled() {
        updateSheetState { it.copy(mode = ChooseAccounts.Mode.Chooser) }
    }

    fun onQRDecoded(code: String) {
        val domain = code.replaceFirst(RNS_HRP, "")
        onReceiverChanged(receiver = domain)
    }

    fun onOwnedAccountSelected(account: Account) {
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

    @Suppress("ReturnCount")
    suspend fun chooseAccountSubmitted() {
        val sheetState = _state.value.sheet as? ChooseAccounts ?: return
        val networkId = _state.value.fromAccount?.networkId ?: return

        if (!sheetState.isChooseButtonEnabled) return

        updateSheetState { it.copy(isResolving = true) }

        // Resolve selected account
        val selectedAccountWithResolvedInput = resolveTargetAccount(
            sheetState = sheetState,
            networkId = networkId
        ) ?: return

        if (selectedAccountWithResolvedInput is TargetAccount.Other &&
            selectedAccountWithResolvedInput.validity != TargetAccount.Other.InputValidity.VALID
        ) {
            updateSheetState {
                it.copy(
                    isResolving = false,
                    selectedAccount = selectedAccountWithResolvedInput
                )
            }

            return
        }

        _state.update { state ->
            val ownedAccount =
                sheetState.ownedAccounts.find { it.address == selectedAccountWithResolvedInput.address }
            val resolvedAccount = if (ownedAccount != null) {
                // if the target owned account has accept known rule then we need to fetch its known resources
                // in order to later check if a an extra signature is required
                val areTargetAccountResourcesRequired = ownedAccount.hasAcceptKnownDepositRule

                TargetAccount.Owned(
                    account = ownedAccount,
                    accountAssetsAddresses = if (areTargetAccountResourcesRequired) {
                        fetchKnownResourcesOfOwnedAccount(
                            ownedAccount = ownedAccount
                        )
                    } else {
                        emptyList()
                    },
                    id = selectedAccountWithResolvedInput.id,
                    spendingAssets = selectedAccountWithResolvedInput.spendingAssets
                )
            } else {
                selectedAccountWithResolvedInput
            }

            state.copy(
                targetAccounts = state.targetAccounts.map { targetAccount ->
                    if (targetAccount.id == resolvedAccount.id) {
                        resolvedAccount
                    } else {
                        targetAccount
                    }
                }.toPersistentList(),
                sheet = TransferViewModel.State.Sheet.None,
                accountDepositResourceRulesSet = NetworkContent.None
            )
        }
    }

    private suspend fun resolveTargetAccount(
        sheetState: ChooseAccounts,
        networkId: NetworkId
    ): TargetAccount? = when (val selectedAccount = sheetState.selectedAccount) {
        is TargetAccount.Other -> {
            val accountAddress = AccountAddress.validatedOnNetworkOrNull(
                validating = selectedAccount.typedLowercase,
                networkId = networkId
            )

            // Checks if input AccountAddress is not used
            val accountAddressValidity: (AccountAddress) -> TargetAccount.Other.InputValidity =
                { address ->
                    val usedAccountAddresses = with(_state.value) {
                        targetAccounts.mapNotNull { it.address } + listOfNotNull(fromAccount?.address)
                    }

                    if (address in usedAccountAddresses) {
                        TargetAccount.Other.InputValidity.ADDRESS_USED
                    } else {
                        TargetAccount.Other.InputValidity.VALID
                    }
                }

            if (accountAddress != null) {
                selectedAccount.copy(
                    resolvedInput = TargetAccount.Other.ResolvedInput.AccountInput(
                        accountAddress
                    ),
                    // Check if typed account address validity
                    validity = accountAddressValidity(accountAddress)
                )
            } else {
                resolveRadixDomainUseCase(selectedAccount.typedLowercase)
                    .fold(
                        onSuccess = { receiver ->
                            selectedAccount.copy(
                                resolvedInput = TargetAccount.Other.ResolvedInput.DomainInput(
                                    receiver = receiver
                                ),
                                // Check resolved account address validity
                                validity = accountAddressValidity(receiver.receiver)
                            )
                        },
                        onFailure = { error ->
                            _state.update {
                                it.copy(
                                    sheet = sheetState.copy(
                                        isResolving = false,
                                        uiMessage = UiMessage.ErrorMessage(error)
                                    )
                                )
                            }
                            null
                        }
                    )
            }
        }

        is TargetAccount.Owned -> sheetState.selectedAccount
        is TargetAccount.Skeleton -> null
    }

    private suspend fun fetchKnownResourcesOfOwnedAccount(ownedAccount: Account): List<ResourceAddress> {
        return getWalletAssetsUseCase.collect(
            account = ownedAccount,
            isRefreshing = false
        ).fold(
            onSuccess = { accountWithAssets ->
                accountWithAssets.assets?.knownResources?.map { it.address }.orEmpty()
            },
            onFailure = {
                emptyList()
            }
        )
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

    companion object {
        private const val RNS_HRP = "rns:"
    }
}
