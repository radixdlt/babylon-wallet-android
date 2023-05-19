package com.babylon.wallet.android.presentation.transfer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.Resources
import com.babylon.wallet.android.domain.usecases.GetAccountsWithResourcesUseCase
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.transfer.accounts.AccountsChooserDelegate
import com.babylon.wallet.android.presentation.transfer.assets.AssetsChooserDelegate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork
import javax.inject.Inject

@HiltViewModel
class TransferViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    getAccountsWithResourcesUseCase: GetAccountsWithResourcesUseCase,
    savedStateHandle: SavedStateHandle
) : StateViewModel<TransferViewModel.State>() {

    internal val args = TransferArgs(savedStateHandle)

    override fun initialState(): State = State()

    private val accountsChooserDelegate = AccountsChooserDelegate(
        state = _state,
        viewModelScope = viewModelScope,
        getProfileUseCase = getProfileUseCase
    )

    private val assetsChooserDelegate = AssetsChooserDelegate(
        state = _state,
        viewModelScope = viewModelScope,
        getAccountsWithResourcesUseCase = getAccountsWithResourcesUseCase
    )

    init {
        viewModelScope.launch {
            val sourceAccount = getProfileUseCase.accountOnCurrentNetwork(args.accountId)

            _state.update {
                it.copy(fromAccount = sourceAccount)
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

    fun onAddressTyped(address: String) = accountsChooserDelegate.addressTyped(address = address)

    fun onOwnedAccountSelected(account: Network.Account) = accountsChooserDelegate.onOwnedAccountSelected(account = account)

    fun addAccountClick() {
        _state.update {
            it.copy(
                targetAccounts = it.targetAccounts.toMutableList().apply {
                    add(TargetAccount.Skeleton(it.targetAccounts.size))
                }.toPersistentList()
            )
        }
    }

    fun deleteAccountClick(index: Int) {
        _state.update {
            val targetAccounts = it.targetAccounts.toMutableList()

            if (index == 0 && targetAccounts.size == 1) {
                targetAccounts.removeAt(index)
                targetAccounts.add(TargetAccount.Skeleton(index))
                it.copy(targetAccounts = targetAccounts.toPersistentList())
            } else if (index > 0 && index < targetAccounts.size) {
                targetAccounts.removeAt(index)
                it.copy(targetAccounts = targetAccounts.toPersistentList())
            } else {
                it
            }
        }
    }

    fun onChooseAccountSubmitted() = accountsChooserDelegate.chooseAccountSubmitted()

    fun onChooseAccountForSkeleton(index: Int) {
        val fromAccount = _state.value.fromAccount ?: return
        accountsChooserDelegate.onChooseAccount(
            fromAccount = fromAccount,
            index = index,
            selectedAccounts = _state.value.targetAccounts
        )
    }

    fun onQRAddressDecoded(address: String) = accountsChooserDelegate.onQRAddressDecoded(address = address)

    fun onQrCodeIconClick() = accountsChooserDelegate.onQRModeStarted()

    fun cancelQrScan() = accountsChooserDelegate.onQRModeCanceled()

    fun onChooseAssetTabSelected(tab: State.Sheet.ChooseAssets.Tab) = assetsChooserDelegate.onTabSelected(tab)

    fun onAddAssetsClick(/* TODO add selected account */) {
        val fromAccount = state.value.fromAccount ?: return
        assetsChooserDelegate.onChooseAssets(
            fromAccount = fromAccount,
            selectedAssets = setOf()
        )
    }

    fun onAssetSelectionChanged(resource: Resource, isSelected: Boolean) = assetsChooserDelegate.onAssetSelectionChanged(
        resource = resource,
        isChecked = isSelected
    )

    fun onSheetClose() {
        _state.update { it.copy(sheet = State.Sheet.None) }
    }

    data class State(
        val fromAccount: Network.Account? = null,
        val targetAccounts: List<TargetAccount> = listOf(TargetAccount.Skeleton(index = 0)),
        val address: String = "",
        val message: String = "",
        val sheet: Sheet = Sheet.None
    ) : UiState {

        val isSheetVisible: Boolean
            get() = sheet != Sheet.None

        sealed interface Sheet {
            object None: Sheet

            data class ChooseAccounts(
                val selectedAccount: TargetAccount,
                val ownedAccounts: List<Network.Account>,
                val mode: Mode = Mode.Chooser
            ): Sheet {

                val isOwnedAccountsEnabled: Boolean
                    get() = when (selectedAccount) {
                        is TargetAccount.Other -> selectedAccount.address.isBlank()
                        is TargetAccount.Owned -> true
                        is TargetAccount.Skeleton -> true
                    }

                val isChooseButtonEnabled: Boolean
                    get() = selectedAccount.isAddressValid

                fun isOwnedAccountSelected(account: Network.Account) = (selectedAccount as? TargetAccount.Owned)?.account == account

                enum class Mode {
                    Chooser,
                    QRScanner
                }
            }

            data class ChooseAssets(
                val resources: Resources? = null,
                val selectedResources: Set<Resource>,
                val selectedTab: Tab = Tab.Tokens
            ): Sheet {

                enum class Tab {
                    Tokens,
                    NFTs
                }
            }
        }
    }
}

sealed class TargetAccount {
    abstract val address: String
    abstract val index: Int

    val isAddressValid: Boolean
        get() = when (this) {
            is Owned -> true
            is Other -> isValidatedSuccessfully
            else -> false
        }

    data class Skeleton(
        override val index: Int
    ): TargetAccount() {
        override val address: String = ""
    }

    data class Other(
        override val address: String,
        val isValidatedSuccessfully: Boolean,
        override val index: Int
    ): TargetAccount()

    data class Owned(
        val account: Network.Account,
        override val index: Int
    ): TargetAccount() {
        override val address: String
            get() = account.address
    }
}

enum class ChooseAccountSheetMode {
    Chooser, QRScanner
}
