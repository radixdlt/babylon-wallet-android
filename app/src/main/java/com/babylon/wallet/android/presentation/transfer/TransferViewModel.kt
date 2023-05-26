package com.babylon.wallet.android.presentation.transfer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.Resources
import com.babylon.wallet.android.domain.usecases.GetAccountsWithResourcesUseCase
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.transfer.accounts.AccountsChooserDelegate
import com.babylon.wallet.android.presentation.transfer.assets.AssetsChooserDelegate
import com.babylon.wallet.android.presentation.transfer.prepare.PrepareManifestDelegate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.UUIDGenerator
import rdx.works.core.mapWhen
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork
import java.math.BigDecimal
import javax.inject.Inject

@Suppress("TooManyFunctions")
@HiltViewModel
class TransferViewModel @Inject constructor(
    getProfileUseCase: GetProfileUseCase,
    getAccountsWithResourcesUseCase: GetAccountsWithResourcesUseCase,
    incomingRequestRepository: IncomingRequestRepository,
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

    private val prepareManifestDelegate = PrepareManifestDelegate(
        state = _state,
        incomingRequestRepository = incomingRequestRepository
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

    fun onMessageStateChanged(isOpen: Boolean) {
        _state.update {
            it.copy(messageState = if (isOpen) State.Message.Added() else State.Message.None)
        }
    }

    fun onMessageChanged(message: String) {
        val addedMessageState = _state.value.messageState as? State.Message.Added ?: return

        _state.update {
            it.copy(
                messageState = addedMessageState.copy(message = message)
            )
        }
    }

    fun addAccountClick() {
        _state.update { it.addSkeleton() }
    }

    fun deleteAccountClick(from: TargetAccount) {
        _state.update { it.remove(from) }
    }

    fun onRemoveAsset(account: TargetAccount, asset: SpendingAsset) {
        _state.update { it.removeAsset(account, asset) }
    }

    fun onAmountTyped(account: TargetAccount, asset: SpendingAsset, amount: String) {
        val fungibleAsset = asset as? SpendingAsset.Fungible ?: return

        _state.update { it.updateAssetAmount(account, fungibleAsset, amount) }
    }

    fun onMaxAmount(account: TargetAccount, asset: SpendingAsset) {
        val fungibleAsset = asset as? SpendingAsset.Fungible ?: return

        val maxAmount = fungibleAsset.resource.amount
        val spentAmount = _state.value.targetAccounts
            .filterNot { it.address == account.address }
            .sumOf { it.amountSpent(fungibleAsset) }
        val remainingAmountString = (maxAmount - spentAmount).coerceAtLeast(BigDecimal.ZERO).toPlainString()

        _state.update { it.updateAssetAmount(account, fungibleAsset, remainingAmountString) }
    }

    fun onTransferSubmit() {
        viewModelScope.launch { prepareManifestDelegate.onSubmit() }
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
        val currentState = state.value
        val fromAccount = currentState.fromAccount ?: return
        assetsChooserDelegate.onChooseAssets(
            fromAccount = fromAccount,
            targetAccount = targetAccount
        )
    }

    fun onAssetSelectionChanged(asset: SpendingAsset, isSelected: Boolean) = assetsChooserDelegate.onAssetSelectionChanged(
        asset = asset,
        isChecked = isSelected
    )

    fun onUiMessageShown() {
        if (_state.value.sheet is State.Sheet.ChooseAssets) {
            assetsChooserDelegate.onUiMessageShown()
        } else {
            _state.update { it.copy(error = null) }
        }
    }

    fun onChooseAssetsSubmitted() = assetsChooserDelegate.onChooseAssetsSubmitted()

    fun onSheetClose() {
        _state.update { it.copy(sheet = State.Sheet.None) }
    }

    data class State(
        val fromAccount: Network.Account? = null,
        val targetAccounts: ImmutableList<TargetAccount> = persistentListOf(TargetAccount.Skeleton()),
        val messageState: Message = Message.None,
        val sheet: Sheet = Sheet.None,
        val error: UiMessage? = null
    ) : UiState {

        val isSheetVisible: Boolean
            get() = sheet != Sheet.None

        val isSubmitEnabled: Boolean = targetAccounts[0] !is TargetAccount.Skeleton && targetAccounts.all { it.isValidForSubmission }

        val submittedMessage: String?
            get() = (messageState as? Message.Added)?.message

        fun addSkeleton(): State = copy(
            targetAccounts = targetAccounts.toMutableList().apply {
                add(TargetAccount.Skeleton())
            }.toPersistentList()
        ).withCheckedBalances()

        fun replace(account: TargetAccount): State = copy(
            targetAccounts = targetAccounts.mapWhen(
                predicate = {
                    it.id == account.id
                },
                mutation = { account }
            ).toPersistentList()
        ).withCheckedBalances()

        fun remove(at: TargetAccount): State {
            val targetAccounts = targetAccounts.toMutableList()
            val index = targetAccounts.indexOf(at)

            if (index != -1) {
                targetAccounts.removeAt(index)
            }

            if (targetAccounts.isEmpty()) {
                targetAccounts.add(TargetAccount.Skeleton())
            }

            return copy(targetAccounts = targetAccounts.toPersistentList()).withCheckedBalances()
        }

        fun removeAsset(account: TargetAccount, asset: SpendingAsset): State = copy(
            targetAccounts = targetAccounts.mapWhen(
                predicate = { it.id == account.id },
                mutation = { it.removeAsset(asset) }
            ).toPersistentList()
        ).withCheckedBalances()

        fun updateAssetAmount(account: TargetAccount, asset: SpendingAsset.Fungible, amountString: String): State = copy(
            targetAccounts = targetAccounts.mapWhen(
                predicate = { it.id == account.id },
                mutation = { target ->
                    target.updateAssets { assets ->
                        assets.mapWhen(
                            predicate = { it.address == asset.address },
                            mutation = { asset.copy(amountString = amountString) }
                        ).toPersistentSet()
                    }
                }
            ).toPersistentList()
        ).withCheckedBalances()

        private fun withCheckedBalances(): State {
            val fungibleBalances = mutableMapOf<Resource.FungibleResource, BigDecimal>()
            val nonFungibleBalances = mutableMapOf<Resource.NonFungibleResource.Item, Int>()

            targetAccounts
                .map { it.assets.filterIsInstance<SpendingAsset.Fungible>() }
                .flatten()
                .forEach { fungible ->
                    val spentAmount = fungibleBalances[fungible.resource] ?: BigDecimal.ZERO
                    fungibleBalances[fungible.resource] = spentAmount + fungible.amountDecimal
                }

            targetAccounts
                .map { it.assets.filterIsInstance<SpendingAsset.NFT>() }
                .flatten()
                .forEach { nft ->
                    val spent = nonFungibleBalances[nft.item] ?: 0
                    nonFungibleBalances[nft.item] = spent + 1
                }

            return copy(
                targetAccounts = targetAccounts.map { targetAccount ->
                    targetAccount.updateAssets { assets ->
                        assets.map { asset ->
                            when (asset) {
                                is SpendingAsset.Fungible -> asset.copy(
                                    exceedingBalance = fungibleBalances.getOrDefault(asset.resource, BigDecimal.ZERO) > asset.resource.amount
                                )
                                is SpendingAsset.NFT -> asset.copy(
                                    exceedingBalance = nonFungibleBalances.getOrDefault(asset.item, 0) > 1
                                )
                            }
                        }.toPersistentSet()
                    }
                }.toPersistentList()
            )
        }

        sealed interface Sheet {
            object None : Sheet

            data class ChooseAccounts(
                val selectedAccount: TargetAccount,
                val ownedAccounts: PersistentList<Network.Account>,
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

        sealed interface Message {
            object None : Message
            data class Added(val message: String = "") : Message
        }
    }
}

sealed class TargetAccount {
    abstract val address: String
    abstract val id: String
    abstract val assets: ImmutableSet<SpendingAsset>

    val isAddressValid: Boolean
        get() = when (this) {
            is Owned -> true
            is Other -> validity == Other.AddressValidity.VALID
            else -> false
        }

    val isValidForSubmission: Boolean
        get() = when (this) {
            is Other -> assets.isNotEmpty() && assets.all { it.isValidForSubmission }
            is Owned -> assets.isNotEmpty() && assets.all { it.isValidForSubmission }
            is Skeleton -> assets.isEmpty()
        }

    fun amountSpent(fungibleAsset: SpendingAsset.Fungible): BigDecimal = assets
        .filterIsInstance<SpendingAsset.Fungible>()
        .find { it.address == fungibleAsset.address }
        ?.amountDecimal ?: BigDecimal.ZERO

    fun updateAssets(onUpdate: (ImmutableSet<SpendingAsset>) -> ImmutableSet<SpendingAsset>): TargetAccount {
        return when (this) {
            is Owned -> copy(assets = onUpdate(assets))
            is Other -> copy(assets = onUpdate(assets))
            is Skeleton -> copy(assets = onUpdate(assets))
        }
    }

    fun addAsset(asset: SpendingAsset): TargetAccount {
        val newAssets = assets.toMutableSet().apply {
            add(asset)
        }.toPersistentSet()

        return when (this) {
            is Owned -> copy(assets = newAssets)
            is Other -> copy(assets = newAssets)
            is Skeleton -> copy(assets = newAssets)
        }
    }

    fun removeAsset(asset: SpendingAsset): TargetAccount {
        val newAssets = assets.toMutableSet().apply {
            remove(asset)
        }.toPersistentSet()

        return when (this) {
            is Owned -> copy(assets = newAssets)
            is Other -> copy(assets = newAssets)
            is Skeleton -> copy(assets = newAssets)
        }
    }

    data class Skeleton(
        override val id: String = UUIDGenerator.uuid().toString(),
        override val assets: ImmutableSet<SpendingAsset> = persistentSetOf()
    ) : TargetAccount() {
        override val address: String = ""
    }

    data class Other(
        override val address: String,
        val validity: AddressValidity,
        override val id: String,
        override val assets: ImmutableSet<SpendingAsset> = persistentSetOf()
    ) : TargetAccount() {

        enum class AddressValidity {
            VALID,
            INVALID,
            USED
        }
    }

    data class Owned(
        val account: Network.Account,
        override val id: String,
        override val assets: ImmutableSet<SpendingAsset> = persistentSetOf()
    ) : TargetAccount() {
        override val address: String
            get() = account.address
    }
}

sealed class SpendingAsset {
    abstract val address: String
    abstract val isValidForSubmission: Boolean

    data class Fungible(
        val resource: Resource.FungibleResource,
        val amountString: String = "",
        val exceedingBalance: Boolean = false
    ) : SpendingAsset() {
        override val address: String
            get() = resource.resourceAddress

        override val isValidForSubmission: Boolean
            get() = !exceedingBalance && amountDecimal != BigDecimal.ZERO

        val amountDecimal: BigDecimal
            get() = amountString.toBigDecimalOrNull() ?: BigDecimal.ZERO
    }

    data class NFT(
        val item: Resource.NonFungibleResource.Item,
        val exceedingBalance: Boolean = false
    ) : SpendingAsset() {
        override val address: String
            get() = item.globalAddress

        override val isValidForSubmission: Boolean
            get() = !exceedingBalance
    }
}
