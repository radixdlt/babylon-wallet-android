@file:OptIn(ExperimentalCoroutinesApi::class)

package com.babylon.wallet.android.presentation.account

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.tokenprice.FiatPriceRepository
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.babylon.wallet.android.domain.usecases.GetEntitiesWithSecurityPromptUseCase
import com.babylon.wallet.android.domain.usecases.GetNetworkInfoUseCase
import com.babylon.wallet.android.domain.usecases.SecurityPromptType
import com.babylon.wallet.android.domain.usecases.assets.GetFiatValueUseCase
import com.babylon.wallet.android.domain.usecases.assets.GetNextNFTsPageUseCase
import com.babylon.wallet.android.domain.usecases.assets.GetWalletAssetsUseCase
import com.babylon.wallet.android.domain.usecases.assets.UpdateLSUsInfo
import com.babylon.wallet.android.domain.usecases.transaction.SendClaimRequestUseCase
import com.babylon.wallet.android.presentation.account.AccountEvent.NavigateToMnemonicBackup
import com.babylon.wallet.android.presentation.account.AccountEvent.NavigateToMnemonicRestore
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.transfer.assets.AssetsTab
import com.babylon.wallet.android.presentation.ui.composables.assets.AssetsViewState
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.extensions.ProfileEntity
import com.radixdlt.sargon.extensions.orZero
import com.radixdlt.sargon.extensions.plus
import com.radixdlt.sargon.extensions.toDecimal192
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.domain.assets.Asset
import rdx.works.core.domain.assets.AssetPrice
import rdx.works.core.domain.assets.FiatPrice
import rdx.works.core.domain.assets.LiquidStakeUnit
import rdx.works.core.domain.assets.NonFungibleCollection
import rdx.works.core.domain.assets.PoolUnit
import rdx.works.core.domain.assets.StakeClaim
import rdx.works.core.domain.assets.SupportedCurrency
import rdx.works.core.domain.assets.ValidatorWithStakes
import rdx.works.core.domain.resources.Resource
import rdx.works.core.mapWhen
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.core.sargon.factorSourceId
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.display.ChangeBalanceVisibilityUseCase
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class AccountViewModel @Inject constructor(
    private val getWalletAssetsUseCase: GetWalletAssetsUseCase,
    private val getFiatValueUseCase: GetFiatValueUseCase,
    getProfileUseCase: GetProfileUseCase,
    private val getNetworkInfoUseCase: GetNetworkInfoUseCase,
    private val getEntitiesWithSecurityPromptUseCase: GetEntitiesWithSecurityPromptUseCase,
    private val getNextNFTsPageUseCase: GetNextNFTsPageUseCase,
    private val updateLSUsInfo: UpdateLSUsInfo,
    private val changeBalanceVisibilityUseCase: ChangeBalanceVisibilityUseCase,
    private val appEventBus: AppEventBus,
    private val sendClaimRequestUseCase: SendClaimRequestUseCase,
    savedStateHandle: SavedStateHandle
) : StateViewModel<AccountUiState>(), OneOffEventHandler<AccountEvent> by OneOffEventHandlerImpl() {

    private val args = AccountArgs(savedStateHandle)
    override fun initialState(): AccountUiState = AccountUiState(accountWithAssets = null)

    private val refreshFlow = MutableSharedFlow<Unit>()
    private val accountFlow = combine(
        getProfileUseCase.flow.mapNotNull { profile ->
            profile.activeAccountsOnCurrentNetwork.find { it.address == args.accountAddress }
        },
        refreshFlow
    ) { account, _ -> account }

    init {
        viewModelScope.launch {
            accountFlow
                .onEach { account ->
                    // Update details of profile account each time it is updated
                    _state.update { state ->
                        val accountWithAssets = state.accountWithAssets?.copy(account = account) ?: AccountWithAssets(account = account)
                        state.copy(accountWithAssets = accountWithAssets)
                    }
                }
                .flatMapLatest { account ->
                    getWalletAssetsUseCase(listOf(account), state.value.isRefreshing)
                        .catch { error ->
                            _state.update {
                                it.copy(isRefreshing = false, uiMessage = UiMessage.ErrorMessage(error = error))
                            }
                        }
                        .mapNotNull { it.firstOrNull() }
                }
                .collectLatest { accountWithAssets ->
                    // keep the val here because the assets have been updated and we need to stop refreshing
                    // in the next update of the state (below)
                    val isRefreshing = state.value.isRefreshing

                    // Update assets of the account each time they are updated
                    _state.update { state ->
                        state.copy(
                            accountWithAssets = state.accountWithAssets?.copy(assets = accountWithAssets.assets),
                            isRefreshing = false
                        )
                    }

                    getFiatValueUseCase.forAccount(
                        accountWithAssets = accountWithAssets,
                        isRefreshing = isRefreshing
                    )
                        .onSuccess { assetsPrices ->
                            _state.update { state ->
                                state.copy(
                                    assetsWithAssetsPrices = assetsPrices.associateBy { it.asset },
                                    hasFailedToFetchPricesForAccount = false
                                )
                            }
                        }
                        .onFailure {
                            if (it is FiatPriceRepository.PricesNotSupportedInNetwork) {
                                disableFiatPrices()
                            } else {
                                _state.update { state ->
                                    state.copy(hasFailedToFetchPricesForAccount = true)
                                }
                                Timber.e("Failed to fetch prices for account: ${it.message}")
                                // now try to fetch prices per asset of the account
                                getAssetsPricesForAccount(accountWithAssets = accountWithAssets, isRefreshing = isRefreshing)
                            }
                        }
                }
        }

        viewModelScope.launch {
            appEventBus.events.filter { event ->
                event is AppEvent.RefreshResourcesNeeded || event is AppEvent.RestoredMnemonic
            }.collect {
                loadAccountDetails(withRefresh = it !is AppEvent.RestoredMnemonic)
            }
        }

        observeSecurityPrompt()
        loadAccountDetails(withRefresh = false)
    }

    private suspend fun getAssetsPricesForAccount(
        accountWithAssets: AccountWithAssets,
        isRefreshing: Boolean
    ) {
        val assets = accountWithAssets.assets
        if (assets?.ownsAnyAssetsThatContributeToBalance == true && assets.ownedAssets.isNotEmpty()) {
            viewModelScope.launch {
                val assetsPrices = getFiatValueUseCase.forAssets(
                    assets = assets.ownedAssets,
                    account = accountWithAssets.account,
                    isRefreshing = isRefreshing
                )
                _state.update { state ->
                    state.copy(assetsWithAssetsPrices = assetsPrices.mapNotNull { it }.associateBy { it.asset })
                }
            }
        }
    }

    private fun observeSecurityPrompt() {
        viewModelScope.launch {
            getEntitiesWithSecurityPromptUseCase().collect { entities ->
                val securityPrompt = entities.find {
                    (it.entity as? ProfileEntity.AccountEntity)?.account?.address == args.accountAddress
                }?.prompt

                _state.update { state ->
                    state.copy(securityPromptType = securityPrompt)
                }
            }
        }
    }

    fun refresh() {
        loadAccountDetails(withRefresh = true)
    }

    fun onShowHideBalanceToggle(isVisible: Boolean) {
        viewModelScope.launch {
            changeBalanceVisibilityUseCase(isVisible = isVisible)
        }
    }

    fun onFungibleResourceClicked(resource: Resource.FungibleResource) {
        val account = _state.value.accountWithAssets?.account ?: return

        viewModelScope.launch {
            sendEvent(AccountEvent.OnFungibleClick(resource, account))
        }
    }

    fun onNonFungibleResourceClicked(
        nonFungibleResource: Resource.NonFungibleResource,
        item: Resource.NonFungibleResource.Item
    ) {
        val account = _state.value.accountWithAssets?.account ?: return

        viewModelScope.launch {
            sendEvent(
                AccountEvent.OnNonFungibleClick(
                    resource = nonFungibleResource,
                    item = item,
                    account = account
                )
            )
        }
    }

    fun onLSUUnitClicked(liquidStakeUnit: LiquidStakeUnit) {
        val account = _state.value.accountWithAssets?.account ?: return

        viewModelScope.launch {
            sendEvent(AccountEvent.OnFungibleClick(resource = liquidStakeUnit.fungibleResource, account = account))
        }
    }

    fun onPoolUnitClicked(poolUnit: PoolUnit) {
        val account = _state.value.accountWithAssets?.account ?: return

        viewModelScope.launch {
            sendEvent(AccountEvent.OnFungibleClick(resource = poolUnit.resource, account))
        }
    }

    fun onApplySecuritySettings(securityPromptType: SecurityPromptType) {
        viewModelScope.launch {
            val factorSourceId = _state.value.accountWithAssets?.account?.factorSourceId as? FactorSourceId.Hash ?: return@launch

            when (securityPromptType) {
                SecurityPromptType.NEEDS_BACKUP -> sendEvent(NavigateToMnemonicBackup(factorSourceId))
                SecurityPromptType.NEEDS_RESTORE -> sendEvent(NavigateToMnemonicRestore(factorSourceId))
            }
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    fun onNextNftPageRequest(resource: Resource.NonFungibleResource) {
        val account = state.value.accountWithAssets?.account ?: return
        if (!state.value.isRefreshing && resource.address !in state.value.nonFungiblesWithPendingNFTs) {
            _state.update { state -> state.onNFTsLoading(resource) }
            viewModelScope.launch {
                getNextNFTsPageUseCase(account, resource)
                    .onSuccess { resourceWithUpdatedNFTs ->
                        _state.update { state ->
                            state.onNFTsReceived(resourceWithUpdatedNFTs)
                        }
                    }.onFailure { error ->
                        _state.update { state ->
                            state.onNFTsError(resource, error)
                        }
                    }
            }
        }
    }

    fun onStakesRequest() {
        val account = state.value.accountWithAssets?.account ?: return
        val lsus = state.value.accountWithAssets?.assets?.ownedLiquidStakeUnits ?: return
        val stakeClaims = state.value.accountWithAssets?.assets?.ownedStakeClaims ?: return
        val validatorsWithStakes = ValidatorWithStakes.from(lsus, stakeClaims)
        val unknownLSUs = validatorsWithStakes.any { !it.isDetailsAvailable }
        onLatestEpochRequest()
        if (!state.value.isRefreshing && !state.value.pendingStakeUnits && unknownLSUs) {
            _state.update { state -> state.copy(pendingStakeUnits = true) }
            viewModelScope.launch {
                updateLSUsInfo(account, validatorsWithStakes).onSuccess {
                    _state.update { state -> state.onValidatorsReceived(it) }
                }.onFailure { error ->
                    _state.update { state -> state.copy(pendingStakeUnits = false, uiMessage = UiMessage.ErrorMessage(error)) }
                }
            }
        }
    }

    fun onClaimClick(stakeClaims: List<StakeClaim>) {
        val account = state.value.accountWithAssets?.account ?: return
        val epoch = state.value.epoch ?: return
        viewModelScope.launch {
            sendClaimRequestUseCase(
                account = account,
                claims = stakeClaims,
                epoch = epoch
            )
        }
    }

    fun onTabSelected(tab: AssetsTab) {
        _state.update { it.copy(assetsViewState = it.assetsViewState.copy(selectedTab = tab)) }
    }

    fun onCollectionToggle(collectionId: String) {
        _state.update { it.copy(assetsViewState = it.assetsViewState.onCollectionToggle(collectionId)) }
    }

    private fun onLatestEpochRequest() = viewModelScope.launch {
        getNetworkInfoUseCase().onSuccess { info ->
            _state.update { it.copy(epoch = info.epoch) }
        }
    }

    private fun loadAccountDetails(withRefresh: Boolean) {
        _state.update { it.copy(isRefreshing = withRefresh) }
        viewModelScope.launch { refreshFlow.emit(Unit) }
        onLatestEpochRequest()
    }

    private fun disableFiatPrices() {
        _state.update { accountUiState ->
            accountUiState.copy(isFiatBalancesEnabled = false)
        }
    }
}

internal sealed interface AccountEvent : OneOffEvent {
    data class NavigateToMnemonicBackup(val factorSourceId: FactorSourceId.Hash) : AccountEvent
    data class NavigateToMnemonicRestore(val factorSourceId: FactorSourceId.Hash) : AccountEvent
    data class OnFungibleClick(val resource: Resource.FungibleResource, val account: Account) : AccountEvent
    data class OnNonFungibleClick(
        val resource: Resource.NonFungibleResource,
        val item: Resource.NonFungibleResource.Item,
        val account: Account
    ) : AccountEvent
}

data class AccountUiState(
    val accountWithAssets: AccountWithAssets? = null,
    val isFiatBalancesEnabled: Boolean = true,
    val assetsWithAssetsPrices: Map<Asset, AssetPrice?>? = null,
    private val hasFailedToFetchPricesForAccount: Boolean = false,
    val nonFungiblesWithPendingNFTs: Set<ResourceAddress> = setOf(),
    val pendingStakeUnits: Boolean = false,
    val securityPromptType: SecurityPromptType? = null,
    val assetsViewState: AssetsViewState = AssetsViewState.init(),
    val epoch: Long? = null,
    val isRefreshing: Boolean = false,
    val uiMessage: UiMessage? = null
) : UiState {

    val isAccountBalanceLoading: Boolean
        get() = assetsWithAssetsPrices == null

    val totalFiatValue: FiatPrice?
        get() {
            if (hasFailedToFetchPricesForAccount) return null

            var total = 0.toDecimal192()
            var currency = SupportedCurrency.USD
            assetsWithAssetsPrices?.let { assetsWithAssetsPrices ->
                assetsWithAssetsPrices.values
                    .mapNotNull { it }
                    .forEach { assetPrice ->
                        total += assetPrice.price?.price.orZero()
                        currency = assetPrice.price?.currency ?: SupportedCurrency.USD
                    }
            } ?: return null

            return FiatPrice(price = total, currency = currency)
        }

    val isTransferEnabled: Boolean
        get() = accountWithAssets?.assets != null

    fun onNFTsLoading(forResource: Resource.NonFungibleResource): AccountUiState {
        return copy(nonFungiblesWithPendingNFTs = nonFungiblesWithPendingNFTs + forResource.address)
    }

    fun onNFTsReceived(forResource: Resource.NonFungibleResource): AccountUiState {
        if (accountWithAssets?.assets?.nonFungibles == null) return this
        return copy(
            accountWithAssets = accountWithAssets.copy(
                assets = accountWithAssets.assets.copy(
                    nonFungibles = accountWithAssets.assets.nonFungibles.mapWhen(
                        predicate = {
                            it.collection.address == forResource.address &&
                                it.collection.items.size < forResource.items.size
                        },
                        mutation = { NonFungibleCollection(forResource) }
                    )
                )
            ),
            nonFungiblesWithPendingNFTs = nonFungiblesWithPendingNFTs - forResource.address
        )
    }

    fun onNFTsError(forResource: Resource.NonFungibleResource, error: Throwable): AccountUiState {
        if (accountWithAssets?.assets?.nonFungibles == null) return this
        return copy(
            nonFungiblesWithPendingNFTs = nonFungiblesWithPendingNFTs - forResource.address,
            uiMessage = UiMessage.ErrorMessage(error = error)
        )
    }

    fun onValidatorsReceived(validatorsWithStakes: List<ValidatorWithStakes>): AccountUiState = copy(
        accountWithAssets = accountWithAssets?.copy(
            assets = accountWithAssets.assets?.copy(
                liquidStakeUnits = validatorsWithStakes.mapNotNull { it.liquidStakeUnit },
                stakeClaims = validatorsWithStakes.mapNotNull { it.stakeClaimNft }
            )
        ),
        pendingStakeUnits = false
    )
}
