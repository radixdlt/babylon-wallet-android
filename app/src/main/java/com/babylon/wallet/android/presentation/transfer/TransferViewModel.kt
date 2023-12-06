package com.babylon.wallet.android.presentation.transfer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.model.assets.Assets
import com.babylon.wallet.android.domain.model.assets.ValidatorWithStakes
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.isXrd
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
import rdx.works.profile.data.model.extensions.factorSourceId
import rdx.works.profile.data.model.extensions.isSignatureRequiredBasedOnDepositRules
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork
import java.math.BigDecimal
import javax.inject.Inject

@Suppress("TooManyFunctions")
@HiltViewModel
class TransferViewModel @Inject constructor(
    getProfileUseCase: GetProfileUseCase,
    private val accountsChooserDelegate: AccountsChooserDelegate,
    private val assetsChooserDelegate: AssetsChooserDelegate,
    private val prepareManifestDelegate: PrepareManifestDelegate,
    savedStateHandle: SavedStateHandle,
) : StateViewModel<TransferViewModel.State>() {

    internal val args = TransferArgs(savedStateHandle)

    override fun initialState(): State = State()

    init {
        accountsChooserDelegate(viewModelScope, _state)
        assetsChooserDelegate(viewModelScope, _state)
        prepareManifestDelegate(viewModelScope, _state)

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
        val maxAmount = fungibleAsset.resource.ownedAmount ?: return
        val spentAmount = _state.value.targetAccounts
            .filterNot { it.address == account.address }
            .sumOf { it.amountSpent(fungibleAsset) }
        val remainingAmount = (maxAmount - spentAmount).coerceAtLeast(BigDecimal.ZERO)
        val remainingAmountString = remainingAmount.toPlainString()

        if (fungibleAsset.resource.isXrd && remainingAmount > BigDecimal.ZERO) {
            _state.update {
                it.copy(
                    maxXrdError = State.MaxAmountMessage(
                        maxAccountAmount = remainingAmount,
                        account = account,
                        asset = asset
                    )
                )
            }
        } else {
            _state.update { state ->
                state.updateAssetAmount(
                    account = account,
                    asset = fungibleAsset,
                    amountString = remainingAmountString
                )
            }
        }
    }

    fun onMaxAmountApplied(emptyAccount: Boolean) {
        _state.value.maxXrdError?.let { maxXrdError ->
            val fungibleAsset = maxXrdError.asset as? SpendingAsset.Fungible ?: return
            val remainingAmountString = if (emptyAccount) {
                maxXrdError.maxAccountAmount
            } else {
                maxXrdError.amountWithoutFees
            }
            _state.update { state ->
                state.updateAssetAmount(
                    account = maxXrdError.account,
                    asset = fungibleAsset,
                    amountString = remainingAmountString.toPlainString()
                )
                    .copy(
                        maxXrdError = null
                    )
            }
        }
    }

    fun onLessThanFeeApplied(confirm: Boolean) {
        if (confirm) {
            _state.value.maxXrdError?.let { maxXrdError ->
                val fungibleAsset = maxXrdError.asset as? SpendingAsset.Fungible ?: return
                _state.update { state ->
                    state.updateAssetAmount(
                        account = maxXrdError.account,
                        asset = fungibleAsset,
                        amountString = maxXrdError.maxAccountAmount.toPlainString()
                    )
                        .copy(
                            maxXrdError = null
                        )
                }
            }
        } else {
            _state.update { it.copy(maxXrdError = null) }
        }
    }

    fun onTransferSubmit() {
        viewModelScope.launch { prepareManifestDelegate.onSubmit() }
    }

    // Choose accounts flow

    fun onChooseAccountForSkeleton(from: TargetAccount) {
        val fromAccount = _state.value.fromAccount ?: return

        viewModelScope.launch {
            accountsChooserDelegate.onChooseAccount(
                fromAccount = fromAccount,
                slotAccount = from,
                selectedAccounts = _state.value.targetAccounts
            )
        }
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

    fun onNextNFTsPageRequest(resource: Resource.NonFungibleResource) = assetsChooserDelegate.onNextNFTsPageRequest(resource)

    fun onStakesRequest() = assetsChooserDelegate.onStakesRequest()

    fun onSheetClose() {
        _state.update { it.copy(sheet = State.Sheet.None) }
    }

    data class State(
        val fromAccount: Network.Account? = null,
        val targetAccounts: ImmutableList<TargetAccount> = persistentListOf(TargetAccount.Skeleton()),
        val messageState: Message = Message.None,
        val sheet: Sheet = Sheet.None,
        val error: UiMessage? = null,
        val maxXrdError: MaxAmountMessage? = null,
        val transferRequestId: String? = null
    ) : UiState {

        val isSheetVisible: Boolean
            get() = sheet != Sheet.None

        val isSubmitEnabled: Boolean = targetAccounts[0] !is TargetAccount.Skeleton && targetAccounts.all {
            it.isValidForSubmission
        }

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
                .map { it.spendingAssets.filterIsInstance<SpendingAsset.Fungible>() }
                .flatten()
                .forEach { fungible ->
                    val spentAmount = fungibleBalances[fungible.resource] ?: BigDecimal.ZERO
                    fungibleBalances[fungible.resource] = spentAmount + fungible.amountDecimal
                }

            targetAccounts
                .map { it.spendingAssets.filterIsInstance<SpendingAsset.NFT>() }
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
                                    exceedingBalance = fungibleBalances.getOrDefault(
                                        asset.resource,
                                        BigDecimal.ZERO
                                    ) > asset.resource.ownedAmount
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
            data object None : Sheet

            data class ChooseAccounts(
                val selectedAccount: TargetAccount,
                val ownedAccounts: PersistentList<Network.Account>,
                val mode: Mode = Mode.Chooser,
                val isLoadingAssetsForAccount: Boolean
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
                val assets: Assets? = null,
                private val initialAssetAddress: ImmutableSet<String>, // Used to compute the difference between chosen assets
                val nonFungiblesWithPendingNFTs: Set<String> = setOf(),
                val pendingStakeUnits: Boolean = false,
                val targetAccount: TargetAccount,
                val selectedTab: Tab = Tab.Tokens,
                val epoch: Long? = null,
                val uiMessage: UiMessage? = null
            ) : Sheet {

                val isSubmitEnabled: Boolean
                    get() {
                        val currentAssetAddresses = targetAccount.spendingAssets.map { it.address }.toSet()
                        val currentSub = currentAssetAddresses subtract initialAssetAddress
                        val initialSub = initialAssetAddress subtract currentAssetAddresses
                        val result = currentSub union initialSub
                        return result.isNotEmpty()
                    }

                val assetsSelectedCount: Int
                    get() = targetAccount.spendingAssets.size

                fun onNFTsLoading(forResource: Resource.NonFungibleResource): ChooseAssets {
                    return copy(nonFungiblesWithPendingNFTs = nonFungiblesWithPendingNFTs + forResource.resourceAddress)
                }

                fun onNFTsReceived(forResource: Resource.NonFungibleResource): ChooseAssets {
                    if (assets?.nonFungibles == null) return this
                    return copy(
                        assets = assets.copy(
                            nonFungibles = assets.nonFungibles.mapWhen(
                                predicate = {
                                    it.resourceAddress == forResource.resourceAddress && it.items.size < forResource.items.size
                                },
                                mutation = { forResource }
                            )
                        ),
                        nonFungiblesWithPendingNFTs = nonFungiblesWithPendingNFTs - forResource.resourceAddress
                    )
                }

                fun onNFTsError(forResource: Resource.NonFungibleResource, error: Throwable): ChooseAssets {
                    if (assets?.nonFungibles == null) return this
                    return copy(
                        nonFungiblesWithPendingNFTs = nonFungiblesWithPendingNFTs - forResource.resourceAddress,
                        uiMessage = UiMessage.ErrorMessage(error = error)
                    )
                }

                fun onValidatorsReceived(validatorsWithStakes: List<ValidatorWithStakes>): ChooseAssets = copy(
                    assets = assets?.copy(validatorsWithStakes = validatorsWithStakes),
                    pendingStakeUnits = false
                )

                enum class Tab {
                    Tokens,
                    NFTs,
                    PoolUnits
                }

                companion object {
                    fun init(forTargetAccount: TargetAccount): ChooseAssets = ChooseAssets(
                        initialAssetAddress = forTargetAccount.spendingAssets.map { it.address }.toPersistentSet(),
                        targetAccount = forTargetAccount
                    )
                }
            }
        }

        sealed interface Message {
            data object None : Message
            data class Added(val message: String = "") : Message
        }

        data class MaxAmountMessage(
            val maxAccountAmount: BigDecimal,
            val account: TargetAccount,
            val asset: SpendingAsset
        ) {
            val amountWithoutFees: BigDecimal
                get() = maxAccountAmount - BigDecimal.ONE

            val maxAccountAmountLessThanFee: Boolean
                get() = maxAccountAmount < BigDecimal.ONE
        }
    }
}

sealed class TargetAccount {
    abstract val address: String
    abstract val id: String
    abstract val spendingAssets: ImmutableSet<SpendingAsset>

    abstract fun isSignatureRequiredForTransfer(forSpendingAsset: SpendingAsset): Boolean

    val isAddressValid: Boolean
        get() = when (this) {
            is Owned -> true
            is Other -> validity == Other.AddressValidity.VALID
            else -> false
        }

    val isValidForSubmission: Boolean
        get() = when (this) {
            is Other -> spendingAssets.isNotEmpty() && spendingAssets.all { it.isValidForSubmission }
            is Owned -> spendingAssets.isNotEmpty() && spendingAssets.all { it.isValidForSubmission }
            is Skeleton -> spendingAssets.isEmpty()
        }

    val factorSourceId: FactorSource.FactorSourceID.FromHash?
        get() = (this as? Owned)?.account?.factorSourceId() as? FactorSource.FactorSourceID.FromHash

    fun amountSpent(fungibleAsset: SpendingAsset.Fungible): BigDecimal = spendingAssets
        .filterIsInstance<SpendingAsset.Fungible>()
        .find { it.address == fungibleAsset.address }
        ?.amountDecimal ?: BigDecimal.ZERO

    fun updateAssets(onUpdate: (ImmutableSet<SpendingAsset>) -> ImmutableSet<SpendingAsset>): TargetAccount {
        return when (this) {
            is Owned -> copy(spendingAssets = onUpdate(spendingAssets))
            is Other -> copy(spendingAssets = onUpdate(spendingAssets))
            is Skeleton -> copy(spendingAssets = onUpdate(spendingAssets))
        }
    }

    fun addAsset(asset: SpendingAsset): TargetAccount {
        val newSpendingAssets = spendingAssets.toMutableSet().apply {
            add(asset)
        }.toPersistentSet()

        return when (this) {
            is Owned -> copy(spendingAssets = newSpendingAssets)
            is Other -> copy(spendingAssets = newSpendingAssets)
            is Skeleton -> copy(spendingAssets = newSpendingAssets)
        }
    }

    fun removeAsset(asset: SpendingAsset): TargetAccount {
        val newSpendingAssets = spendingAssets.toMutableSet().apply {
            removeIf { it.address == asset.address }
        }.toPersistentSet()

        return when (this) {
            is Owned -> copy(spendingAssets = newSpendingAssets)
            is Other -> copy(spendingAssets = newSpendingAssets)
            is Skeleton -> copy(spendingAssets = newSpendingAssets)
        }
    }

    data class Skeleton(
        override val id: String = UUIDGenerator.uuid().toString(),
        override val spendingAssets: ImmutableSet<SpendingAsset> = persistentSetOf()
    ) : TargetAccount() {
        override val address: String = ""

        override fun isSignatureRequiredForTransfer(forSpendingAsset: SpendingAsset): Boolean = false
    }

    data class Other(
        override val address: String,
        val validity: AddressValidity,
        override val id: String,
        override val spendingAssets: ImmutableSet<SpendingAsset> = persistentSetOf()
    ) : TargetAccount() {

        override fun isSignatureRequiredForTransfer(forSpendingAsset: SpendingAsset): Boolean = false

        enum class AddressValidity {
            VALID,
            INVALID,
            USED
        }
    }

    data class Owned(
        val account: Network.Account,
        val accountAssetsAddresses: List<String> = emptyList(),
        override val id: String,
        override val spendingAssets: ImmutableSet<SpendingAsset> = persistentSetOf()
    ) : TargetAccount() {
        override val address: String
            get() = account.address

        override fun isSignatureRequiredForTransfer(forSpendingAsset: SpendingAsset): Boolean {
            return account.isSignatureRequiredBasedOnDepositRules(
                forSpecificAssetAddress = forSpendingAsset.address,
                addressesOfAssetsOfTargetAccount = accountAssetsAddresses
            )
        }
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
            get() = !exceedingBalance && amountString.isNotEmpty() && (resource.isXrd || amountDecimal != BigDecimal.ZERO)

        val amountDecimal: BigDecimal
            get() = amountString.toBigDecimalOrNull() ?: BigDecimal.ZERO
    }

    data class NFT(
        val resource: Resource.NonFungibleResource,
        val item: Resource.NonFungibleResource.Item,
        val exceedingBalance: Boolean = false
    ) : SpendingAsset() {
        override val address: String
            get() = item.globalAddress

        override val isValidForSubmission: Boolean
            get() = !exceedingBalance
    }
}
