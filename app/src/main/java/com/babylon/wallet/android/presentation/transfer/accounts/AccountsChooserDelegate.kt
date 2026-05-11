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
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.AddressBookEntry
import com.radixdlt.sargon.DisplayName
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.string
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.update
import rdx.works.core.domain.validatedOnNetworkOrNull
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.core.sargon.accountAddressOrNull
import rdx.works.core.sargon.hasAcceptKnownDepositRule
import rdx.works.core.sargon.sortedForDisplay
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.addressbook.AddAddressBookEntryUseCase
import rdx.works.profile.domain.addressbook.GetAccountAddressBookEntriesOnCurrentNetworkUseCase
import javax.inject.Inject

class AccountsChooserDelegate @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val getWalletAssetsUseCase: GetWalletAssetsUseCase,
    private val resolveRadixDomainUseCase: ResolveRadixDomainUseCase,
    private val isValidRadixDomainUseCase: IsValidRadixDomainUseCase,
    private val getAccountAddressBookEntriesOnCurrentNetworkUseCase: GetAccountAddressBookEntriesOnCurrentNetworkUseCase,
    private val addAddressBookEntryUseCase: AddAddressBookEntryUseCase
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
        val filteredAccountAddresses = (selectedAccounts.mapNotNull { it.address } + fromAccount.address).toPersistentList()
        val addressBookEntries = runCatching {
            getAccountAddressBookEntriesOnCurrentNetworkUseCase()
                .sortedForDisplay()
                .toPersistentList()
        }.getOrElse {
            persistentListOf()
        }

        updateSheetState {
            it.copy(
                ownedAccounts = accounts.toPersistentList(),
                addressBookEntries = addressBookEntries,
                filteredAccountAddresses = filteredAccountAddresses
            )
        }
    }

    fun onReceiverChanged(receiver: String) {
        val currentNetworkId = _state.value.fromAccount?.networkId ?: return
        val receiverSanitized = receiver.trim().lowercase()
        updateSheetState { sheetState ->
            val maybeAccountAddress =
                AccountAddress.validatedOnNetworkOrNull(receiverSanitized, currentNetworkId)

            val inputValidity = if (maybeAccountAddress != null) {
                val fromAccountAddressString = _state.value.fromAccount?.address?.string.orEmpty()
                val selectedAccountAddressesString =
                    _state.value.targetAccounts.map { it.address?.string.orEmpty() }
                if (receiverSanitized in selectedAccountAddressesString || receiverSanitized == fromAccountAddressString) {
                    TargetAccount.Other.InputValidity.ADDRESS_USED
                } else {
                    TargetAccount.Other.InputValidity.VALID
                }
            } else if (isValidRadixDomainUseCase(receiverSanitized)) {
                TargetAccount.Other.InputValidity.VALID
            } else {
                TargetAccount.Other.InputValidity.INVALID
            }

            val updatedSheet = sheetState.copy(
                selectedAccount = TargetAccount.Other(
                    typed = receiver,
                    resolvedInput = maybeAccountAddress?.let {
                        TargetAccount.Other.ResolvedInput.AccountInput(it)
                    },
                    validity = inputValidity,
                    id = sheetState.selectedAccount.id,
                    spendingAssets = sheetState.selectedAccount.spendingAssets
                ),
                mode = ChooseAccounts.Mode.Chooser
            )

            if (!updatedSheet.canStoreValidatedManualRecipientInAddressBook) {
                updatedSheet.copy(storeManualRecipientInAddressBook = false)
            } else {
                updatedSheet
            }
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
                ),
                storeManualRecipientInAddressBook = false
            )
        }
    }

    fun onRecipientTabSelected(tab: ChooseAccounts.RecipientTab) {
        updateSheetState {
            it.copy(selectedTab = tab)
        }
    }

    fun onAddressBookEntrySelected(entry: AddressBookEntry) {
        val sheetState = _state.value.sheet as? ChooseAccounts ?: return
        val address = entry.accountAddressOrNull ?: return
        _state.update { state ->
            val selectedAccount = TargetAccount.Other(
                typed = address.string,
                resolvedInput = TargetAccount.Other.ResolvedInput.AccountInput(address),
                validity = TargetAccount.Other.InputValidity.VALID,
                id = sheetState.selectedAccount.id,
                addressBookName = entry.name.value,
                spendingAssets = sheetState.selectedAccount.spendingAssets
            )
            state.copy(
                targetAccounts = state.targetAccounts.map { targetAccount ->
                    if (targetAccount.id == selectedAccount.id) {
                        selectedAccount
                    } else {
                        targetAccount
                    }
                }.toPersistentList(),
                sheet = TransferViewModel.State.Sheet.None,
                accountDepositResourceRulesSet = NetworkContent.None
            )
        }
    }

    fun onStoreManualRecipientInAddressBookToggled() {
        updateSheetState { sheetState ->
            if (sheetState.canStoreValidatedManualRecipientInAddressBook) {
                sheetState.copy(
                    storeManualRecipientInAddressBook = !sheetState.storeManualRecipientInAddressBook
                )
            } else {
                sheetState.copy(storeManualRecipientInAddressBook = false)
            }
        }
    }

    fun onAddAddressBookInputDismissed() {
        updateSheetState {
            it.copy(
                addAddressBookInput = null,
                pendingExternalAccountAddressToSelect = null
            )
        }
    }

    fun onAddAddressBookNameChanged(name: String) {
        updateSheetState { sheetState ->
            sheetState.copy(
                addAddressBookInput = sheetState.addAddressBookInput?.copy(name = name)
            )
        }
    }

    fun onAddAddressBookNoteChanged(note: String) {
        updateSheetState { sheetState ->
            sheetState.copy(
                addAddressBookInput = sheetState.addAddressBookInput?.copy(note = note)
            )
        }
    }

    suspend fun onAddAddressBookSaveClick() {
        val sheetState = _state.value.sheet as? ChooseAccounts ?: return
        val input = sheetState.addAddressBookInput ?: return
        val pendingAddress = sheetState.pendingExternalAccountAddressToSelect ?: return
        if (!input.isValid) return

        updateSheetState {
            it.copy(
                addAddressBookInput = input.copy(isSaving = true)
            )
        }

        runCatching {
            addAddressBookEntryUseCase(
                address = input.address.asGeneral(),
                name = DisplayName(input.trimmedName),
                note = input.trimmedNote
            )
        }.fold(
            onSuccess = { saved ->
                if (saved) {
                    _state.update { state ->
                        val selectedAccount = TargetAccount.Other(
                            typed = pendingAddress.string,
                            resolvedInput = TargetAccount.Other.ResolvedInput.AccountInput(pendingAddress),
                            validity = TargetAccount.Other.InputValidity.VALID,
                            id = sheetState.selectedAccount.id,
                            addressBookName = input.trimmedName,
                            spendingAssets = sheetState.selectedAccount.spendingAssets
                        )
                        state.copy(
                            targetAccounts = state.targetAccounts.map { targetAccount ->
                                if (targetAccount.id == selectedAccount.id) {
                                    selectedAccount
                                } else {
                                    targetAccount
                                }
                            }.toPersistentList(),
                            sheet = TransferViewModel.State.Sheet.None,
                            accountDepositResourceRulesSet = NetworkContent.None
                        )
                    }
                } else {
                    updateSheetState {
                        it.copy(
                            addAddressBookInput = input.copy(isSaving = false),
                            uiMessage = UiMessage.ErrorMessage(
                                Throwable("Failed to save address book entry")
                            )
                        )
                    }
                }
            },
            onFailure = { error ->
                updateSheetState {
                    it.copy(
                        addAddressBookInput = input.copy(isSaving = false),
                        uiMessage = UiMessage.ErrorMessage(error)
                    )
                }
            }
        )
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

        if (selectedAccountWithResolvedInput is TargetAccount.Other &&
            selectedAccountWithResolvedInput.resolvedInput is TargetAccount.Other.ResolvedInput.AccountInput &&
            sheetState.storeManualRecipientInAddressBook &&
            sheetState.canStoreValidatedManualRecipientInAddressBook
        ) {
            val address = (selectedAccountWithResolvedInput.resolvedInput as TargetAccount.Other.ResolvedInput.AccountInput)
                .accountAddress
            updateSheetState {
                it.copy(
                    isResolving = false,
                    selectedAccount = selectedAccountWithResolvedInput,
                    pendingExternalAccountAddressToSelect = address,
                    addAddressBookInput = ChooseAccounts.AddAddressBookInput(address = address)
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
                when (selectedAccountWithResolvedInput) {
                    is TargetAccount.Other -> {
                        val maybeAddress = (selectedAccountWithResolvedInput.resolvedInput as? TargetAccount.Other.ResolvedInput.AccountInput)
                            ?.accountAddress
                        val addressBookName = maybeAddress?.let { address ->
                            sheetState.addressBookEntries.firstOrNull { it.accountAddressOrNull == address }?.name?.value
                        }
                        selectedAccountWithResolvedInput.copy(addressBookName = addressBookName)
                    }

                    else -> selectedAccountWithResolvedInput
                }
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
                    validity = accountAddressValidity(accountAddress),
                    addressBookName = null
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
                                validity = accountAddressValidity(receiver.receiver),
                                addressBookName = null
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
