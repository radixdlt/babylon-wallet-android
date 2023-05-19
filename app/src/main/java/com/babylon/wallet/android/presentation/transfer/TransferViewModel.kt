package com.babylon.wallet.android.presentation.transfer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.Resources
import com.babylon.wallet.android.domain.usecases.GetAccountsWithResourcesUseCase
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.transfer.accounts.AccountsChooserDelegate
import com.babylon.wallet.android.presentation.transfer.assets.AssetsChooserDelegate
import dagger.hilt.android.lifecycle.HiltViewModel
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

    // Transfer flow

    fun onMessageChanged(message: String) {
        _state.update {
            it.copy(
                message = message
            )
        }
    }

    fun addAccountClick() {
        _state.update {
            it.copy(
                targetAccounts = it.targetAccounts.toMutableList().apply {
                    add(TargetAccount.Skeleton(it.targetAccounts.size, assets = emptyList()))
                }.toPersistentList()
            )
        }
    }

    fun deleteAccountClick(from: TargetAccount) {
        _state.update {
            val targetAccounts = it.targetAccounts.toMutableList()

            if (from.index == 0 && targetAccounts.size == 1) {
                targetAccounts.removeAt(from.index)
                targetAccounts.add(TargetAccount.Skeleton(from.index, assets = emptyList()))
                it.copy(targetAccounts = targetAccounts.toPersistentList())
            } else if (from.index > 0 && from.index < targetAccounts.size) {
                targetAccounts.removeAt(from.index)
                it.copy(targetAccounts = targetAccounts.toPersistentList())
            } else {
                it
            }
        }
    }

    // Choose accounts flow

    fun onChooseAccountForSkeleton(from: TargetAccount) {
        val fromAccount = _state.value.fromAccount ?: return
        accountsChooserDelegate.onChooseAccount(
            fromAccount = fromAccount,
            slotAccount = from,
            selectedAccounts = _state.value.targetAccounts
        )
    }

    fun onAddressTyped(address: String) = accountsChooserDelegate.addressTyped(address = address)

    fun onOwnedAccountSelected(account: Network.Account) = accountsChooserDelegate.onOwnedAccountSelected(account = account)

    fun onChooseAccountSubmitted() = accountsChooserDelegate.chooseAccountSubmitted()

    fun onQRAddressDecoded(address: String) = accountsChooserDelegate.onQRAddressDecoded(address = address)

    fun onQrCodeIconClick() = accountsChooserDelegate.onQRModeStarted()

    fun cancelQrScan() = accountsChooserDelegate.onQRModeCanceled()

    // Choose assets flow

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

    fun onUiMessageShown() = assetsChooserDelegate.onUiMessageShown()

    fun onChooseAssetsSubmitted() = assetsChooserDelegate.onChooseAssetsSubmitted()

    fun onSheetClose() {
        _state.update { it.copy(sheet = State.Sheet.None) }
    }

    data class State(
        val fromAccount: Network.Account? = null,
        val targetAccounts: List<TargetAccount> = listOf(TargetAccount.Skeleton(index = 0, assets = emptyList())),
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
                val selectedTab: Tab = Tab.Tokens,
                val uiMessage: UiMessage? = null
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
    abstract val assets: List<Resource>

    val isAddressValid: Boolean
        get() = when (this) {
            is Owned -> true
            is Other -> isValidatedSuccessfully
            else -> false
        }

    data class Skeleton(
        override val index: Int,
        override val assets: List<Resource>
    ): TargetAccount() {
        override val address: String = ""
    }

    data class Other(
        override val address: String,
        val isValidatedSuccessfully: Boolean,
        override val index: Int,
        override val assets: List<Resource>
    ): TargetAccount()

    data class Owned(
        val account: Network.Account,
        override val index: Int,
        override val assets: List<Resource>
    ): TargetAccount() {
        override val address: String
            get() = account.address
    }
}

enum class ChooseAccountSheetMode {
    Chooser, QRScanner
}
