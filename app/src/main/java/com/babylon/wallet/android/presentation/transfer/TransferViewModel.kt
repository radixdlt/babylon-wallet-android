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
import rdx.works.core.UUIDGenerator
import rdx.works.core.mapWhen
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork
import java.math.BigDecimal
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
                    add(TargetAccount.Skeleton())
                }.toPersistentList()
            )
        }
    }

    fun deleteAccountClick(from: TargetAccount) {
        _state.update {
            val targetAccounts = it.targetAccounts.toMutableList()
            val index = it.targetAccounts.indexOf(from)

            if (index != -1) {
                targetAccounts.removeAt(index)
            }

            if (targetAccounts.isEmpty()) {
                targetAccounts.add(TargetAccount.Skeleton())
            }

            it.copy(targetAccounts = targetAccounts.toPersistentList())
        }
    }

    fun onRemoveAsset(account: TargetAccount, asset: SpendingAsset) {
        _state.update { state ->
            state.copy(
                targetAccounts = state.targetAccounts.mapWhen(
                    predicate = { it == account },
                    mutation = { it.removeAsset(asset) }
                )
            )
        }
    }

    fun onAmountTyped(account: TargetAccount, asset: SpendingAsset, amount: String) {
        val fungibleAsset = asset as? SpendingAsset.Fungible ?: return

        val amountDecimal = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val maxAmount = fungibleAsset.resource.amount
        val spentAmount = _state.value.targetAccounts
            .filterNot { it.address == account.address }
            .sumOf { it.amountSpent(fungibleAsset) } + amountDecimal

        _state.update { state ->
            state.copy(
                targetAccounts = state.targetAccounts.mapWhen(
                    predicate = { it.address == account.address },
                    mutation = {
                        it.updateAsset(
                            asset.copy(
                                amountString = amount,
                                exceedingBalance = spentAmount > maxAmount
                            )
                        )
                    }
                )
            )
        }
    }

    fun onMaxAmount(account: TargetAccount, asset: SpendingAsset) {
        val fungibleAsset = asset as? SpendingAsset.Fungible ?: return

        val maxAmount = fungibleAsset.resource.amount
        val spendAmount = _state.value.targetAccounts
            .filterNot { it.address == account.address }
            .sumOf { it.amountSpent(fungibleAsset) }

        _state.update { state ->
            state.copy(
                targetAccounts = state.targetAccounts.mapWhen(
                    predicate = { it.address == account.address },
                    mutation = {
                        it.updateAsset(
                            asset.copy(
                                amountString = (maxAmount - spendAmount).coerceAtLeast(BigDecimal.ZERO).toPlainString(),
                                exceedingBalance = false
                            )
                        )
                    }
                )
            )
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

    fun onAddAssetsClick(targetAccount: TargetAccount) {
        val fromAccount = state.value.fromAccount ?: return
        assetsChooserDelegate.onChooseAssets(
            fromAccount = fromAccount,
            targetAccount = targetAccount
        )
    }

    fun onAssetSelectionChanged(asset: SpendingAsset, isSelected: Boolean) = assetsChooserDelegate.onAssetSelectionChanged(
        asset = asset,
        isChecked = isSelected
    )

    fun onUiMessageShown() = assetsChooserDelegate.onUiMessageShown()

    fun onChooseAssetsSubmitted() = assetsChooserDelegate.onChooseAssetsSubmitted()

    fun onSheetClose() {
        _state.update { it.copy(sheet = State.Sheet.None) }
    }

    data class State(
        val fromAccount: Network.Account? = null,
        val targetAccounts: List<TargetAccount> = listOf(TargetAccount.Skeleton()),
        val address: String = "",
        val message: String = "",
        val sheet: Sheet = Sheet.None
    ) : UiState {

        val isSheetVisible: Boolean
            get() = sheet != Sheet.None

        sealed interface Sheet {
            object None : Sheet

            data class ChooseAccounts(
                val selectedAccount: TargetAccount,
                val ownedAccounts: List<Network.Account>,
                val mode: Mode = Mode.Chooser
            ) : Sheet {

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
                val targetAccount: TargetAccount,
                val selectedTab: Tab = Tab.Tokens,
                val uiMessage: UiMessage? = null
            ) : Sheet {

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
    abstract val id: String
    abstract val assets: Set<SpendingAsset>

    val isAddressValid: Boolean
        get() = when (this) {
            is Owned -> true
            is Other -> isValidatedSuccessfully
            else -> false
        }

    fun amountSpent(fungibleAsset: SpendingAsset.Fungible): BigDecimal = assets
        .filterIsInstance<SpendingAsset.Fungible>()
        .find { it.address == fungibleAsset.address }
        ?.amountDecimal ?: BigDecimal.ZERO

    fun updateAsset(asset: SpendingAsset.Fungible): TargetAccount {
        val newAssets = assets.mapWhen(
            predicate = { it.address == asset.address },
            mutation = { asset }
        ).toSet()
        return when (this) {
            is Owned -> copy(assets = newAssets)
            is Other -> copy(assets = newAssets)
            is Skeleton -> copy(assets = newAssets)
        }
    }

    fun addAsset(asset: SpendingAsset): TargetAccount {
        val newAssets = assets.toMutableSet().apply {
            add(asset)
        }

        return when (this) {
            is Owned -> copy(assets = newAssets)
            is Other -> copy(assets = newAssets)
            is Skeleton -> copy(assets = newAssets)
        }
    }

    fun removeAsset(asset: SpendingAsset): TargetAccount {
        val newAssets = assets.toMutableSet().apply {
            remove(asset)
        }

        return when (this) {
            is Owned -> copy(assets = newAssets)
            is Other -> copy(assets = newAssets)
            is Skeleton -> copy(assets = newAssets)
        }
    }

    data class Skeleton(
        override val id: String = UUIDGenerator.uuid().toString(),
        override val assets: Set<SpendingAsset> = emptySet()
    ) : TargetAccount() {
        override val address: String = ""
    }

    data class Other(
        override val address: String,
        val isValidatedSuccessfully: Boolean,
        override val id: String,
        override val assets: Set<SpendingAsset> = emptySet()
    ) : TargetAccount()

    data class Owned(
        val account: Network.Account,
        override val id: String,
        override val assets: Set<SpendingAsset> = emptySet()
    ) : TargetAccount() {
        override val address: String
            get() = account.address
    }
}

sealed class SpendingAsset {
    abstract val address: String

    data class Fungible(
        val resource: Resource.FungibleResource,
        val amountString: String = "",
        val exceedingBalance: Boolean = false
    ) : SpendingAsset() {
        override val address: String
            get() = resource.resourceAddress

        val amountDecimal: BigDecimal
            get() = amountString.toBigDecimalOrNull() ?: BigDecimal.ZERO
    }

    data class NFT(val item: Resource.NonFungibleResource.Item) : SpendingAsset() {
        override val address: String
            get() = item.globalAddress
    }
}

enum class ChooseAccountSheetMode {
    Chooser, QRScanner
}
