@file:OptIn(ExperimentalCoroutinesApi::class)

package com.babylon.wallet.android.presentation.account

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.tokenprice.FiatPriceRepository
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.babylon.wallet.android.domain.usecases.GetEntitiesWithSecurityPromptUseCase
import com.babylon.wallet.android.domain.usecases.GetNetworkInfoUseCase
import com.babylon.wallet.android.domain.usecases.SecurityPromptType
import com.babylon.wallet.android.domain.usecases.accountPrompts
import com.babylon.wallet.android.domain.usecases.assets.GetFiatValueUseCase
import com.babylon.wallet.android.domain.usecases.assets.GetNextNFTsPageUseCase
import com.babylon.wallet.android.domain.usecases.assets.GetWalletAssetsUseCase
import com.babylon.wallet.android.domain.usecases.assets.UpdateLSUsInfo
import com.babylon.wallet.android.domain.usecases.transaction.SendClaimRequestUseCase
import com.babylon.wallet.android.presentation.account.AccountViewModel.State.RefreshType.*
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.transfer.assets.AssetsTab
import com.babylon.wallet.android.presentation.ui.composables.assets.AssetsViewState
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEvent.RestoredMnemonic
import com.babylon.wallet.android.utils.AppEventBus
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.extensions.orZero
import com.radixdlt.sargon.extensions.plus
import com.radixdlt.sargon.extensions.toDecimal192
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
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
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.display.ChangeBalanceVisibilityUseCase
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

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
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    savedStateHandle: SavedStateHandle
) : StateViewModel<AccountViewModel.State>(), OneOffEventHandler<AccountViewModel.Event> by OneOffEventHandlerImpl() {

    private val args = AccountArgs(savedStateHandle)
    override fun initialState(): State = State(accountWithAssets = null)

    private var automaticRefreshJob: Job? = null
    private val refreshFlow = MutableSharedFlow<State.RefreshType>()
    private val accountFlow = getProfileUseCase.flow
        .mapNotNull { profile ->
            profile.activeAccountsOnCurrentNetwork.find { it.address == args.accountAddress }
        }
        .distinctUntilChanged()

    init {
        observeAccountAssets()
        observeGlobalAppEvents()
        observeSecurityPrompt()
    }

    private fun observeAccountAssets() {
        combine(
            accountFlow,
            refreshFlow.onStart {
                loadAccountDetails(
                    refreshType = Manual(
                        overrideCache = false,
                        showRefreshIndicator = false,
                        firstRequest = true
                    )
                )
            }
        ) { account, refreshEvent ->
            _state.update { it.onAccount(account, refreshEvent) }
            account
        }.flatMapLatest { account ->
            Timber.tag("WALLET").d("ACCOUNT Override: ${_state.value.refreshType.overrideCache}")
            getWalletAssetsUseCase(
                accounts = listOf(account),
                isRefreshing = _state.value.refreshType.overrideCache
            ).catch { error ->
                _state.update {
                    it.copy(
                        refreshType = None,
                        uiMessage = UiMessage.ErrorMessage(error = error)
                    )
                }
            }.mapNotNull { it.firstOrNull() }
        }.onEach { accountWithAssets ->
            val refreshState = state.value.refreshType

            // Update assets of the account each time they are updated
            _state.update { state ->
                state.copy(
                    accountWithAssets = state.accountWithAssets?.copy(assets = accountWithAssets.assets),
                    refreshType = None
                )
            }

            getFiatValueUseCase.forAccount(
                accountWithAssets = accountWithAssets,
                isRefreshing = refreshState.overrideCache
            ).onSuccess { assetsPrices ->
                _state.update { state ->
                    state.copy(
                        pricesState = State.PricesState.Enabled(assetsPrices.associateBy { it.asset })
                    )
                }
            }.onFailure {
                if (it is FiatPriceRepository.PricesNotSupportedInNetwork) {
                    disableFiatPrices()
                } else {
                    Timber.e("Failed to fetch prices for account: ${it.message}")
                    // now try to fetch prices per asset of the account
                    getAssetsPricesForAccount(
                        accountWithAssets = accountWithAssets,
                        isRefreshing = refreshState.overrideCache
                    )
                }
            }

            if (refreshState.isFirstRefreshRequest()) {
                loadAccountDetails(
                    refreshType = Manual(overrideCache = true, showRefreshIndicator = false, firstRequest = false)
                )
            }
        }.flowOn(defaultDispatcher).launchIn(viewModelScope)
    }

    private fun observeGlobalAppEvents() {
        viewModelScope.launch {
            appEventBus.events.filter { event ->
                event is AppEvent.RefreshAssetsNeeded || event is AppEvent.RestoredMnemonic
            }.collect { event ->
                when (event) {
                    AppEvent.RefreshAssetsNeeded -> loadAccountDetails(
                        refreshType = Manual(
                            overrideCache = true,
                            showRefreshIndicator = true,
                            firstRequest = false
                        )
                    )

                    RestoredMnemonic -> loadAccountDetails(
                        refreshType = Manual(overrideCache = false, showRefreshIndicator = false, firstRequest = false)
                    )

                    else -> {}
                }
            }
        }
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
                    state.copy(pricesState = State.PricesState.Enabled(assetsPrices.mapNotNull { it }.associateBy { it.asset }))
                }
            }
        }
    }

    private fun observeSecurityPrompt() {
        viewModelScope.launch {
            getEntitiesWithSecurityPromptUseCase().collect { entities ->
                val securityPrompts = entities.accountPrompts()[args.accountAddress]?.toList()

                _state.update { state ->
                    state.copy(securityPrompts = securityPrompts)
                }
            }
        }
    }

    fun refresh() {
        loadAccountDetails(refreshType = Manual(overrideCache = true, showRefreshIndicator = true, firstRequest = false))
    }

    fun onShowHideBalanceToggle(isVisible: Boolean) {
        viewModelScope.launch {
            changeBalanceVisibilityUseCase(isVisible = isVisible)
        }
    }

    fun onFungibleResourceClicked(resource: Resource.FungibleResource) {
        val account = _state.value.accountWithAssets?.account ?: return

        viewModelScope.launch {
            sendEvent(Event.OnFungibleClick(resource, account))
        }
    }

    fun onNonFungibleResourceClicked(
        nonFungibleResource: Resource.NonFungibleResource,
        item: Resource.NonFungibleResource.Item
    ) {
        val account = _state.value.accountWithAssets?.account ?: return

        viewModelScope.launch {
            sendEvent(
                Event.OnNonFungibleClick(
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
            sendEvent(Event.OnFungibleClick(resource = liquidStakeUnit.fungibleResource, account = account))
        }
    }

    fun onPoolUnitClicked(poolUnit: PoolUnit) {
        val account = _state.value.accountWithAssets?.account ?: return

        viewModelScope.launch {
            sendEvent(Event.OnFungibleClick(resource = poolUnit.resource, account))
        }
    }

    fun onApplySecuritySettingsClick() {
        viewModelScope.launch {
            sendEvent(Event.NavigateToSecurityCenter)
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

    private fun loadAccountDetails(refreshType: State.RefreshType) {
        automaticRefreshJob?.cancel()
        viewModelScope.launch { refreshFlow.emit(refreshType) }
        onLatestEpochRequest()
        automaticRefreshJob = viewModelScope.launch {
            delay(REFRESH_INTERVAL)
            loadAccountDetails(refreshType = State.RefreshType.Automatic)
        }
    }

    private fun disableFiatPrices() {
        _state.update { accountUiState ->
            accountUiState.copy(pricesState = State.PricesState.Disabled)
        }
    }

    internal sealed interface Event : OneOffEvent {
        data object NavigateToSecurityCenter : Event
        data class OnFungibleClick(val resource: Resource.FungibleResource, val account: Account) : Event
        data class OnNonFungibleClick(
            val resource: Resource.NonFungibleResource,
            val item: Resource.NonFungibleResource.Item,
            val account: Account
        ) : Event
    }

    data class State(
        val accountWithAssets: AccountWithAssets? = null,
        private val pricesState: PricesState = PricesState.None,
        val refreshType: RefreshType = None,
        val nonFungiblesWithPendingNFTs: Set<ResourceAddress> = setOf(),
        val pendingStakeUnits: Boolean = false,
        val securityPrompts: List<SecurityPromptType>? = null,
        val assetsViewState: AssetsViewState = AssetsViewState.init(),
        val epoch: Long? = null,
        val uiMessage: UiMessage? = null
    ) : UiState {

        val isRefreshing: Boolean = refreshType.showRefreshIndicator

        sealed interface RefreshType {
            val overrideCache: Boolean
            val showRefreshIndicator: Boolean

            fun isFirstRefreshRequest(): Boolean = if (this is Manual) this.firstRequest else false

            data object None : RefreshType {
                override val overrideCache: Boolean = false
                override val showRefreshIndicator: Boolean = false
            }

            data class Manual(
                override val overrideCache: Boolean,
                override val showRefreshIndicator: Boolean,
                val firstRequest: Boolean
            ) : RefreshType

            data object Automatic : RefreshType {
                override val overrideCache: Boolean = true
                override val showRefreshIndicator: Boolean = false
            }
        }

        sealed interface PricesState {
            val totalPrice: FiatPrice?

            data object None : PricesState {
                override val totalPrice: FiatPrice? = null
            }

            data class Enabled(
                val prices: Map<Asset, AssetPrice?>
            ) : PricesState {
                override val totalPrice: FiatPrice = run {
                    var total = 0.toDecimal192()
                    var currency = SupportedCurrency.USD
                    prices.values.mapNotNull { it }
                        .forEach { assetPrice ->
                            total += assetPrice.price?.price.orZero()
                            currency = assetPrice.price?.currency ?: SupportedCurrency.USD
                        }

                    FiatPrice(price = total, currency = currency)
                }
            }

            data object Disabled : PricesState {
                override val totalPrice: FiatPrice? = null
            }
        }

        val isAccountBalanceLoading: Boolean
            get() = pricesState is PricesState.None

        val isPricesDisabled: Boolean
            get() = pricesState is PricesState.Disabled

        val assetsWithPrices: Map<Asset, AssetPrice?>?
            get() = (pricesState as? PricesState.Enabled)?.prices

        val totalFiatValue: FiatPrice?
            get() = pricesState.totalPrice

        val isTransferEnabled: Boolean
            get() = accountWithAssets?.assets != null

        fun onAccount(account: Account, refreshType: RefreshType): State = copy(
            accountWithAssets = accountWithAssets?.copy(account = account) ?: AccountWithAssets(account = account),
            refreshType = refreshType
        )

        fun onNFTsLoading(forResource: Resource.NonFungibleResource): State {
            return copy(nonFungiblesWithPendingNFTs = nonFungiblesWithPendingNFTs + forResource.address)
        }

        fun onNFTsReceived(forResource: Resource.NonFungibleResource): State {
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

        fun onNFTsError(forResource: Resource.NonFungibleResource, error: Throwable): State {
            if (accountWithAssets?.assets?.nonFungibles == null) return this
            return copy(
                nonFungiblesWithPendingNFTs = nonFungiblesWithPendingNFTs - forResource.address,
                uiMessage = UiMessage.ErrorMessage(error = error)
            )
        }

        fun onValidatorsReceived(validatorsWithStakes: List<ValidatorWithStakes>): State = copy(
            accountWithAssets = accountWithAssets?.copy(
                assets = accountWithAssets.assets?.copy(
                    liquidStakeUnits = validatorsWithStakes.mapNotNull { it.liquidStakeUnit },
                    stakeClaims = validatorsWithStakes.mapNotNull { it.stakeClaimNft }
                )
            ),
            pendingStakeUnits = false
        )
    }

    companion object {
        private val REFRESH_INTERVAL = 1.minutes
    }
}
