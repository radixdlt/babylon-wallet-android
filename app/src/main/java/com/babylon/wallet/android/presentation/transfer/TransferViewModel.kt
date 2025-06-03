package com.babylon.wallet.android.presentation.transfer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.model.AccountDepositResourceRules
import com.babylon.wallet.android.domain.usecases.GetAccountDepositResourceRulesUseCase
import com.babylon.wallet.android.presentation.common.NetworkContent
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.transfer.accounts.AccountsChooserDelegate
import com.babylon.wallet.android.presentation.transfer.assets.AssetsChooserDelegate
import com.babylon.wallet.android.presentation.transfer.assets.AssetsTab
import com.babylon.wallet.android.presentation.transfer.prepare.PrepareManifestDelegate
import com.babylon.wallet.android.presentation.ui.composables.assets.AssetsViewState
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.RnsDomainConfiguredReceiver
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.clamped
import com.radixdlt.sargon.extensions.compareTo
import com.radixdlt.sargon.extensions.formattedTextField
import com.radixdlt.sargon.extensions.isZero
import com.radixdlt.sargon.extensions.minus
import com.radixdlt.sargon.extensions.orZero
import com.radixdlt.sargon.extensions.parseFromTextField
import com.radixdlt.sargon.extensions.plus
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.extensions.sumOf
import com.radixdlt.sargon.extensions.toDecimal192
import com.radixdlt.sargon.extensions.unsecuredControllingFactorInstance
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
import rdx.works.core.domain.assets.Asset
import rdx.works.core.domain.assets.AssetPrice
import rdx.works.core.domain.assets.Assets
import rdx.works.core.domain.assets.NonFungibleCollection
import rdx.works.core.domain.assets.ValidatorWithStakes
import rdx.works.core.domain.resources.Resource
import rdx.works.core.mapWhen
import rdx.works.core.sargon.activeAccountOnCurrentNetwork
import rdx.works.core.sargon.isSignatureRequiredBasedOnDepositRules
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@Suppress("TooManyFunctions")
@HiltViewModel
class TransferViewModel @Inject constructor(
    getProfileUseCase: GetProfileUseCase,
    private val accountsChooserDelegate: AccountsChooserDelegate,
    private val assetsChooserDelegate: AssetsChooserDelegate,
    private val prepareManifestDelegate: PrepareManifestDelegate,
    private val getAccountDepositResourceRulesUseCase: GetAccountDepositResourceRulesUseCase,
    savedStateHandle: SavedStateHandle,
) : StateViewModel<TransferViewModel.State>(), OneOffEventHandler<TransferViewModel.Event> by OneOffEventHandlerImpl() {

    internal val args = TransferArgs(savedStateHandle)

    override fun initialState(): State = State()

    init {
        accountsChooserDelegate(viewModelScope, _state)
        assetsChooserDelegate(viewModelScope, _state)
        prepareManifestDelegate(viewModelScope, _state)

        viewModelScope.launch {
            val sourceAccount = getProfileUseCase().activeAccountOnCurrentNetwork(args.accountId)

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
        val remainingAmount = (maxAmount - spentAmount).clamped
        val remainingAmountString = remainingAmount.formattedTextField()

        if (fungibleAsset.resource.isXrd && remainingAmount > 0.toDecimal192()) {
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
                    amountString = remainingAmountString.formattedTextField()
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
                        amountString = maxXrdError.maxAccountAmount.string
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

    fun onReceiverChanged(receiver: String) = accountsChooserDelegate.onReceiverChanged(receiver = receiver)

    fun onErrorMessageShown() = accountsChooserDelegate.onErrorMessageShown()

    fun onOwnedAccountSelected(account: Account) = accountsChooserDelegate.onOwnedAccountSelected(account = account)

    fun onChooseAccountSubmitted() {
        viewModelScope.launch {
            accountsChooserDelegate.chooseAccountSubmitted()
            loadAccountDepositResourceRules()
        }
    }

    private suspend fun loadAccountDepositResourceRules() {
        _state.update { it.copy(accountDepositResourceRulesSet = NetworkContent.Loading) }
        val accountAddressesWithResources =
            _state.value.targetAccounts.filterIsInstance<TargetAccount.Other>().mapNotNull { targetAccount ->
                val address = targetAccount.address ?: return@mapNotNull null
                val resourceAddresses = targetAccount.spendingAssets.map {
                    it.resourceAddress
                }.toSet()
                address to resourceAddresses
            }.filter { it.second.isNotEmpty() }.toMap()
        val rules = getAccountDepositResourceRulesUseCase(accountAddressesWithResources)
        _state.update { state ->
            state.copy(accountDepositResourceRulesSet = NetworkContent.Loaded(rules.toPersistentSet())).withUpdatedDepositRules()
        }
    }

    fun onQRAddressDecoded(code: String) = accountsChooserDelegate.onQRDecoded(code = code)

    fun onQrCodeIconClick() = accountsChooserDelegate.onQRModeStarted()

    fun cancelQrScan() = accountsChooserDelegate.onQRModeCanceled()

    // Choose assets flow

    fun onChooseAssetTabSelected(tab: AssetsTab) = assetsChooserDelegate.onTabSelected(tab)

    fun onChooseAssetCollectionToggle(collectionId: String) = assetsChooserDelegate.onCollectionToggle(collectionId)

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

    fun onAssetClicked(asset: SpendingAsset) {
        val account = _state.value.fromAccount ?: return

        viewModelScope.launch {
            sendEvent(Event.ShowAssetDetails(asset, account))
        }
    }

    fun onUiMessageShown() {
        if (_state.value.sheet is State.Sheet.ChooseAssets) {
            assetsChooserDelegate.onUiMessageShown()
        } else {
            _state.update { it.copy(error = null) }
        }
    }

    fun onChooseAssetsSubmitted() {
        assetsChooserDelegate.onChooseAssetsSubmitted()
        viewModelScope.launch {
            loadAccountDepositResourceRules()
        }
    }

    fun onNextNFTsPageRequest(resource: Resource.NonFungibleResource) = assetsChooserDelegate.onNextNFTsPageRequest(resource)

    fun onStakesRequest() = assetsChooserDelegate.onStakesRequest()

    fun onSheetClose() {
        _state.update { it.copy(sheet = State.Sheet.None) }
    }

    data class State(
        val fromAccount: Account? = null,
        val targetAccounts: ImmutableList<TargetAccount> = persistentListOf(TargetAccount.Skeleton()),
        val messageState: Message = Message.None,
        val sheet: Sheet = Sheet.None,
        val error: UiMessage? = null,
        val maxXrdError: MaxAmountMessage? = null,
        val transferRequestId: String? = null,
        val accountDepositResourceRulesSet: NetworkContent<ImmutableSet<AccountDepositResourceRules>> = NetworkContent.None
    ) : UiState {

        private val canDepositToAllTargetAccounts: Boolean
            get() = accountDepositResourceRulesSet is NetworkContent.Loaded && targetAccounts.all { targetAccount ->
                accountDepositResourceRulesSet.data.find {
                    it.accountAddress == targetAccount.address
                }?.canDepositAll ?: true
            }

        val isSheetVisible: Boolean
            get() = sheet != Sheet.None

        val isLoadingAccountDepositResourceRules: Boolean
            get() = accountDepositResourceRulesSet is NetworkContent.Loading

        val isSubmitEnabled: Boolean = targetAccounts[0] !is TargetAccount.Skeleton && targetAccounts.all {
            it.isValidForSubmission
        } && canDepositToAllTargetAccounts

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

        fun withUpdatedDepositRules(): State {
            val targetAccounts = targetAccounts.map { targetAccount ->
                val accountDepositResourceRule = accountDepositResourceRulesSet.data?.find { it.accountAddress == targetAccount.address }
                targetAccount.updateAssets { assets ->
                    assets.map { asset ->
                        val canDeposit = accountDepositResourceRule?.canDeposit(asset.resourceAddress) ?: true
                        when (asset) {
                            is SpendingAsset.Fungible -> asset.copy(
                                canDeposit = canDeposit
                            )

                            is SpendingAsset.NFT -> asset.copy(
                                canDeposit = canDeposit
                            )
                        }
                    }.toPersistentSet()
                }
            }
            return copy(targetAccounts = targetAccounts.toPersistentList())
        }

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
                            predicate = { it.resourceAddress == asset.resourceAddress },
                            mutation = { asset.copy(amountString = amountString) }
                        ).toPersistentSet()
                    }
                }
            ).toPersistentList()
        ).withCheckedBalances()

        private fun withCheckedBalances(): State {
            val fungibleBalances = mutableMapOf<Resource.FungibleResource, Decimal192>()
            val nonFungibleBalances = mutableMapOf<Resource.NonFungibleResource.Item, Int>()

            targetAccounts
                .map { it.spendingAssets.filterIsInstance<SpendingAsset.Fungible>() }
                .flatten()
                .forEach { fungible ->
                    val spentAmount = fungibleBalances[fungible.resource].orZero()
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
                                        0.toDecimal192()
                                    ) > asset.resource.ownedAmount.orZero()
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
                val ownedAccounts: PersistentList<Account>,
                val mode: Mode = Mode.Chooser,
                val uiMessage: UiMessage.ErrorMessage? = null,
                val isResolving: Boolean
            ) : Sheet {

                val isOwnedAccountsEnabled: Boolean
                    get() = when (selectedAccount) {
                        is TargetAccount.Other -> selectedAccount.typed.isBlank()
                        is TargetAccount.Owned -> true
                        is TargetAccount.Skeleton -> true
                    }

                val isChooseButtonEnabled: Boolean
                    get() = when (selectedAccount) {
                        is TargetAccount.Other ->
                            selectedAccount.validity ==
                                TargetAccount.Other.InputValidity.VALID
                        is TargetAccount.Owned -> true
                        is TargetAccount.Skeleton -> false
                    }

                fun isOwnedAccountSelected(account: Account) =
                    (selectedAccount as? TargetAccount.Owned)?.account == account

                enum class Mode {
                    Chooser,
                    QRScanner
                }
            }

            data class ChooseAssets(
                val assets: Assets? = null,
                val isFiatBalancesEnabled: Boolean = true,
                val assetsWithAssetsPrices: Map<Asset, AssetPrice?>? = null,
                private val initialAssetAddress: ImmutableSet<String>, // Used to compute the difference between chosen assets
                val pendingStakeUnits: Boolean = false,
                val targetAccount: TargetAccount,
                val assetsViewState: AssetsViewState = AssetsViewState.init(),
                val epoch: Long? = null,
                val uiMessage: UiMessage? = null
            ) : Sheet {

                val isAccountBalanceLoading: Boolean
                    get() = assetsWithAssetsPrices == null

                val isSubmitEnabled: Boolean
                    get() {
                        val currentAssetAddresses = targetAccount.spendingAssets.map { it.resourceAddressOrGlobalId }.toSet()
                        val currentSub = currentAssetAddresses subtract initialAssetAddress
                        val initialSub = initialAssetAddress subtract currentAssetAddresses
                        val result = currentSub union initialSub
                        return result.isNotEmpty()
                    }

                val assetsSelectedCount: Int
                    get() = targetAccount.spendingAssets.size

                fun onNFTsLoading(forResource: Resource.NonFungibleResource): ChooseAssets {
                    return copy(assetsViewState = assetsViewState.nextPagePending(forResource.address))
                }

                fun onNFTsReceived(forResource: Resource.NonFungibleResource): ChooseAssets {
                    if (assets?.nonFungibles == null) return this
                    return copy(
                        assets = assets.copy(
                            nonFungibles = assets.nonFungibles.mapWhen(
                                predicate = {
                                    it.collection.address == forResource.address &&
                                        it.collection.items.size < forResource.items.size
                                },
                                mutation = { NonFungibleCollection(forResource) }
                            )
                        ),
                        assetsViewState = assetsViewState.nextPageReceived(forResource.address)
                    )
                }

                fun onNFTsError(forResource: Resource.NonFungibleResource, error: Throwable): ChooseAssets {
                    if (assets?.nonFungibles == null) return this
                    return copy(
                        assetsViewState = assetsViewState.nextPageReceived(forResource.address),
                        uiMessage = UiMessage.ErrorMessage(error = error)
                    )
                }

                fun onValidatorsReceived(validatorsWithStakes: List<ValidatorWithStakes>): ChooseAssets = copy(
                    assets = assets?.copy(
                        liquidStakeUnits = validatorsWithStakes.mapNotNull { it.liquidStakeUnit },
                        stakeClaims = validatorsWithStakes.mapNotNull { it.stakeClaimNft }
                    ),
                    pendingStakeUnits = false
                )

                companion object {
                    fun init(forTargetAccount: TargetAccount): ChooseAssets = ChooseAssets(
                        initialAssetAddress = forTargetAccount.spendingAssets.map { it.resourceAddressOrGlobalId }.toPersistentSet(),
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
            val maxAccountAmount: Decimal192,
            val account: TargetAccount,
            val asset: SpendingAsset
        ) {
            val amountWithoutFees: Decimal192
                get() = maxAccountAmount - 1.toDecimal192()

            val maxAccountAmountLessThanFee: Boolean
                get() = maxAccountAmount < 1.toDecimal192()
        }
    }

    sealed interface Event : OneOffEvent {
        data class ShowAssetDetails(val asset: SpendingAsset, val fromAccount: Account) : Event
    }
}

sealed class TargetAccount {
    abstract val address: AccountAddress?
    abstract val id: String
    abstract val spendingAssets: ImmutableSet<SpendingAsset>

    abstract fun isSignatureRequiredForTransfer(resourceAddress: ResourceAddress): Boolean

    val validatedAddress: AccountAddress?
        get() = when (this) {
            is Owned -> address
            is Other -> if (validity == Other.InputValidity.VALID) address else null
            is Skeleton -> null
        }

    val isValidForSubmission: Boolean
        get() = when (this) {
            is Other -> spendingAssets.isNotEmpty() && spendingAssets.all { it.isValidForSubmission }
            is Owned -> spendingAssets.isNotEmpty() && spendingAssets.all { it.isValidForSubmission }
            is Skeleton -> spendingAssets.isEmpty()
        }

    val factorSourceId: FactorSourceId.Hash?
        get() = (this as? Owned)?.account?.unsecuredControllingFactorInstance?.factorSourceId?.asGeneral()

    fun amountSpent(fungibleAsset: SpendingAsset.Fungible): Decimal192 = spendingAssets
        .filterIsInstance<SpendingAsset.Fungible>()
        .find { it.resourceAddress == fungibleAsset.resourceAddress }
        ?.amountDecimal.orZero()

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

    fun removeAsset(removingAsset: SpendingAsset): TargetAccount {
        val newSpendingAssets = spendingAssets.toMutableSet().apply {
            removeIf { asset ->
                when (asset) {
                    is SpendingAsset.Fungible ->
                        removingAsset is SpendingAsset.Fungible &&
                            removingAsset.resourceAddress == asset.resourceAddress
                    is SpendingAsset.NFT ->
                        removingAsset is SpendingAsset.NFT &&
                            removingAsset.item.globalId == asset.item.globalId
                }
            }
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
        override val address: AccountAddress? = null

        override fun isSignatureRequiredForTransfer(resourceAddress: ResourceAddress): Boolean = false
    }

    data class Other(
        val typed: String = "",
        val resolvedInput: ResolvedInput?,
        val validity: InputValidity,
        override val id: String,
        override val spendingAssets: ImmutableSet<SpendingAsset> = persistentSetOf()
    ) : TargetAccount() {
        override val address: AccountAddress? = resolvedInput?.let { input ->
            when (input) {
                is ResolvedInput.AccountInput -> input.accountAddress
                is ResolvedInput.DomainInput -> input.receiver.receiver
            }
        }

        override fun isSignatureRequiredForTransfer(resourceAddress: ResourceAddress): Boolean = false

        enum class InputValidity {
            VALID,
            INVALID,
            ADDRESS_USED
        }

        sealed interface ResolvedInput {
            data class AccountInput(val accountAddress: AccountAddress) : ResolvedInput

            data class DomainInput(val receiver: RnsDomainConfiguredReceiver) : ResolvedInput
        }
    }

    data class Owned(
        val account: Account,
        val accountAssetsAddresses: List<ResourceAddress> = emptyList(),
        override val id: String,
        override val spendingAssets: ImmutableSet<SpendingAsset> = persistentSetOf()
    ) : TargetAccount() {
        override val address: AccountAddress
            get() = account.address

        override fun isSignatureRequiredForTransfer(resourceAddress: ResourceAddress): Boolean {
            return account.isSignatureRequiredBasedOnDepositRules(
                forSpecificAssetAddress = resourceAddress,
                addressesOfAssetsOfTargetAccount = accountAssetsAddresses
            )
        }
    }
}

sealed class SpendingAsset {
    abstract val resourceAddress: ResourceAddress
    abstract val resourceAddressOrGlobalId: String
    abstract val isValidForSubmission: Boolean
    abstract val canDeposit: Boolean

    data class Fungible(
        val resource: Resource.FungibleResource,
        val amountString: String = "",
        val exceedingBalance: Boolean = false,
        override val canDeposit: Boolean = true
    ) : SpendingAsset() {
        override val resourceAddress: ResourceAddress
            get() = resource.address

        override val resourceAddressOrGlobalId: String
            get() = resourceAddress.string

        override val isValidForSubmission: Boolean
            get() = !exceedingBalance && amountString.isNotEmpty() && (resource.isXrd || !amountDecimal.isZero)

        val amountDecimal: Decimal192
            get() = Decimal192.Companion.parseFromTextField(amountString).decimal.orZero()
    }

    data class NFT(
        val resource: Resource.NonFungibleResource,
        val item: Resource.NonFungibleResource.Item,
        val exceedingBalance: Boolean = false,
        override val canDeposit: Boolean = true
    ) : SpendingAsset() {
        override val resourceAddress: ResourceAddress
            get() = resource.address

        override val resourceAddressOrGlobalId: String
            get() = item.globalId.string

        override val isValidForSubmission: Boolean
            get() = !exceedingBalance
    }
}
