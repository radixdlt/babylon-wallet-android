package com.babylon.wallet.android.presentation.wallet

import android.annotation.SuppressLint
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.NPSSurveyState
import com.babylon.wallet.android.NPSSurveyStateObserver
import com.babylon.wallet.android.data.repository.p2plink.P2PLinksRepository
import com.babylon.wallet.android.data.repository.tokenprice.FiatPriceRepository
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.babylon.wallet.android.domain.usecases.GetEntitiesWithSecurityPromptUseCase
import com.babylon.wallet.android.domain.usecases.SecurityPromptType
import com.babylon.wallet.android.domain.usecases.accountPrompts
import com.babylon.wallet.android.domain.usecases.assets.GetFiatValueUseCase
import com.babylon.wallet.android.domain.usecases.assets.GetWalletAssetsUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.wallet.cards.HomeCardsDelegate
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEvent.RestoredMnemonic
import com.babylon.wallet.android.utils.AppEventBus
import com.babylon.wallet.android.utils.Constants.RAD_QUEST_URL
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.HomeCard
import com.radixdlt.sargon.extensions.orZero
import com.radixdlt.sargon.extensions.plus
import com.radixdlt.sargon.extensions.toDecimal192
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.domain.assets.AssetPrice
import rdx.works.core.domain.assets.Assets
import rdx.works.core.domain.assets.FiatPrice
import rdx.works.core.domain.assets.SupportedCurrency
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.core.sargon.isLedgerAccount
import rdx.works.core.sargon.isOlympia
import rdx.works.profile.cloudbackup.domain.CheckMigrationToNewBackupSystemUseCase
import rdx.works.profile.domain.EnsureBabylonFactorSourceExistUseCase
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.display.ChangeBalanceVisibilityUseCase
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

private const val DELAY_BETWEEN_POP_UP_SCREENS_MS = 1000L

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class WalletViewModel @Inject constructor(
    private val getWalletAssetsUseCase: GetWalletAssetsUseCase,
    private val getFiatValueUseCase: GetFiatValueUseCase,
    getProfileUseCase: GetProfileUseCase,
    private val getEntitiesWithSecurityPromptUseCase: GetEntitiesWithSecurityPromptUseCase,
    private val changeBalanceVisibilityUseCase: ChangeBalanceVisibilityUseCase,
    private val appEventBus: AppEventBus,
    private val ensureBabylonFactorSourceExistUseCase: EnsureBabylonFactorSourceExistUseCase,
    private val npsSurveyStateObserver: NPSSurveyStateObserver,
    private val p2PLinksRepository: P2PLinksRepository,
    private val checkMigrationToNewBackupSystemUseCase: CheckMigrationToNewBackupSystemUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    private val homeCards: HomeCardsDelegate
) : StateViewModel<WalletViewModel.State>(), OneOffEventHandler<WalletViewModel.Event> by OneOffEventHandlerImpl() {

    private var automaticRefreshJob: Job? = null
    private val refreshFlow = MutableSharedFlow<RefreshType>()
    private val accountsFlow = getProfileUseCase.flow.map {
        it.activeAccountsOnCurrentNetwork
    }.distinctUntilChanged()

    val babylonFactorSourceDoesNotExistEvent =
        appEventBus.events.filterIsInstance<AppEvent.BabylonFactorSourceDoesNotExist>()

    private var popUpScreensQueue = setOf<PopUpScreen>()
    private val popUpScreen = MutableStateFlow<PopUpScreen?>(null)

    init {
        viewModelScope.launch {
            if (ensureBabylonFactorSourceExistUseCase.babylonFactorSourceExist().not()) {
                appEventBus.sendEvent(AppEvent.BabylonFactorSourceDoesNotExist, delayMs = 500L)
                return@launch
            }
        }
        observePrompts()
        observeWalletAssets()
        observeGlobalAppEvents()
        observeNpsSurveyState()
        observeShowRelinkConnectors()
        checkForOldBackupSystemToMigrate()
        homeCards(scope = viewModelScope, state = _state)
    }

    fun processBufferedDeepLinkRequest() {
        viewModelScope.launch {
            appEventBus.sendEvent(AppEvent.ProcessBufferedDeepLinkRequest)
        }
    }

    override fun initialState() = State()

    fun onStart() {
        loadAssets(refreshType = RefreshType.Manual(overrideCache = false, showRefreshIndicator = false))
    }

    fun popUpScreen(): StateFlow<PopUpScreen?> = popUpScreen

    fun onPopUpScreenDismissed() {
        val dismissedScreen = popUpScreen.value ?: return

        viewModelScope.launch {
            popUpScreen.emit(null)
            popUpScreensQueue = popUpScreensQueue.minus(dismissedScreen)
            delay(DELAY_BETWEEN_POP_UP_SCREENS_MS)
            enqueuePopUpScreen()
        }
    }

    private fun onNewPopUpScreen(popUpScreen: PopUpScreen) {
        popUpScreensQueue = popUpScreensQueue + popUpScreen
        enqueuePopUpScreen()
    }

    private fun enqueuePopUpScreen() {
        viewModelScope.launch {
            popUpScreen.emit(popUpScreensQueue.minByOrNull { it.order })
        }
    }

    private fun checkForOldBackupSystemToMigrate() = viewModelScope.launch {
        if (checkMigrationToNewBackupSystemUseCase()) {
            onNewPopUpScreen(PopUpScreen.CONNECT_CLOUD_BACKUP)
        }
    }

    private fun observeShowRelinkConnectors() {
        viewModelScope.launch {
            p2PLinksRepository.showRelinkConnectors()
                .collect { showRelinkConnectors ->
                    if (showRelinkConnectors) {
                        onNewPopUpScreen(PopUpScreen.RELINK_CONNECTORS)
                    }
                }
        }
    }

    private fun observeNpsSurveyState() {
        viewModelScope.launch {
            npsSurveyStateObserver.npsSurveyState.filterIsInstance<NPSSurveyState.Active>()
                .collectLatest {
                    onNewPopUpScreen(PopUpScreen.NPS_SURVEY)
                }
        }
    }

    suspend fun createBabylonFactorSource(deviceBiometricAuthenticationProvider: suspend () -> Boolean) {
        viewModelScope.launch {
            if (deviceBiometricAuthenticationProvider()) {
                ensureBabylonFactorSourceExistUseCase()
            } else {
                // force user to authenticate until we can create Babylon Factor source
                appEventBus.sendEvent(AppEvent.BabylonFactorSourceDoesNotExist)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeWalletAssets() {
        combine(
            accountsFlow,
            refreshFlow
        ) { accounts, refreshType ->
            _state.update { it.loadingAssets(accounts = accounts, refreshType = refreshType) }

            accounts
        }.flatMapLatest { accounts ->
            getWalletAssetsUseCase(
                accounts = accounts,
                isRefreshing = _state.value.refreshType.overrideCache
            ).catch { error ->
                _state.update { it.assetsError(error) }
                Timber.w(error)
            }.distinctUntilChanged()
        }.onEach { accountsWithAssets ->
            _state.update { it.assetsReceived(accountsWithAssets) }

            // Only when all assets have concluded (either success or error) then we
            // can request for prices.
            if (accountsWithAssets.none { it.assets == null }) {
                val pricesPerAccount = mutableMapOf<AccountAddress, List<AssetPrice>?>()
                for (accountWithAssets in accountsWithAssets) {
                    val pricesError = getFiatValueUseCase.forAccount(
                        accountWithAssets = accountWithAssets,
                        isRefreshing = _state.value.refreshType.overrideCache
                    ).onSuccess { prices ->
                        pricesPerAccount[accountWithAssets.account.address] = prices
                    }.exceptionOrNull()

                    if (pricesError != null && pricesError is FiatPriceRepository.PricesNotSupportedInNetwork) {
                        _state.update { it.disableFiatPrices() }
                        return@onEach
                    }
                }

                _state.update { it.onPricesReceived(prices = pricesPerAccount) }
            }
        }.flowOn(defaultDispatcher).launchIn(viewModelScope)
    }

    private fun observePrompts() {
        viewModelScope.launch {
            getEntitiesWithSecurityPromptUseCase()
                .onEach { entitiesWithSecurityPrompts ->
                    _state.update { it.copy(accountsWithSecurityPrompts = entitiesWithSecurityPrompts.accountPrompts()) }
                }
                .flowOn(defaultDispatcher)
                .collect()
        }
    }

    private fun observeGlobalAppEvents() {
        viewModelScope.launch {
            appEventBus.events.collect { event ->
                when (event) {
                    AppEvent.RefreshAssetsNeeded -> loadAssets(
                        refreshType = RefreshType.Manual(
                            overrideCache = true,
                            showRefreshIndicator = true
                        )
                    )

                    RestoredMnemonic -> loadAssets(refreshType = RefreshType.Manual(overrideCache = false, showRefreshIndicator = false))
                    AppEvent.NPSSurveySubmitted -> {
                        _state.update { it.copy(uiMessage = UiMessage.InfoMessage.NpsSurveySubmitted) }
                    }

                    else -> {}
                }
            }
        }
    }

    private fun loadAssets(refreshType: RefreshType) {
        automaticRefreshJob?.cancel()
        viewModelScope.launch { refreshFlow.emit(refreshType) }
        automaticRefreshJob = viewModelScope.launch {
            delay(REFRESH_INTERVAL)
            loadAssets(refreshType = RefreshType.Automatic)
        }
    }

    fun onRefresh() {
        loadAssets(refreshType = RefreshType.Manual(overrideCache = true, showRefreshIndicator = true))
    }

    fun onShowHideBalanceToggle(isVisible: Boolean) {
        viewModelScope.launch {
            changeBalanceVisibilityUseCase(isVisible = isVisible)
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    fun onApplySecuritySettingsClick() {
        viewModelScope.launch {
            sendEvent(Event.NavigateToSecurityCenter)
        }
    }

    fun onCardClick(card: HomeCard) {
        viewModelScope.launch {
            when (card) {
                HomeCard.Connector -> {
                    sendEvent(Event.NavigateToLinkConnector)
                }
                HomeCard.StartRadQuest -> {
                    sendEvent(Event.OpenUrl(RAD_QUEST_URL))
                }
                else -> {}
            }
            // Currently all the cards should be dismissed on tap
            homeCards.dismissCard(card)
        }
    }

    fun onCardClose(card: HomeCard) {
        homeCards.dismissCard(card)
    }

    @Suppress("MagicNumber")
    enum class PopUpScreen(val order: Int) {

        RELINK_CONNECTORS(1),
        CONNECT_CLOUD_BACKUP(2),
        NPS_SURVEY(3)
    }

    sealed interface RefreshType {
        val overrideCache: Boolean
        val showRefreshIndicator: Boolean

        data object None : RefreshType {
            override val overrideCache: Boolean = false
            override val showRefreshIndicator: Boolean = false
        }

        data class Manual(
            override val overrideCache: Boolean,
            override val showRefreshIndicator: Boolean
        ) : RefreshType

        data object Automatic : RefreshType {
            override val overrideCache: Boolean = true
            override val showRefreshIndicator: Boolean = false
        }
    }

    data class State(
        val refreshType: RefreshType = RefreshType.None,
        private val accountsWithAssets: List<AccountWithAssets>? = null,
        private val accountsWithSecurityPrompts: Map<AccountAddress, Set<SecurityPromptType>> = emptyMap(),
        val prices: PricesState = PricesState.None,
        val uiMessage: UiMessage? = null,
        val cards: ImmutableList<HomeCard> = emptyList<HomeCard>().toPersistentList()
    ) : UiState {

        val isRefreshing: Boolean = refreshType.showRefreshIndicator

        val accountUiItems: List<AccountUiItem> = accountsWithAssets.orEmpty().map { accountWithAssets ->
            val isFiatBalanceVisible = prices !is PricesState.Disabled &&
                (accountWithAssets.assets == null || accountWithAssets.assets.ownsAnyAssetsThatContributeToBalance)

            val account = accountWithAssets.account

            AccountUiItem(
                account = account,
                assets = accountWithAssets.assets,
                securityPrompts = accountsWithSecurityPrompts[account.address]?.toList(),
                tag = when {
                    !accountWithAssets.isDappDefinitionAccountType && !account.isOlympia && !account.isLedgerAccount -> null
                    accountWithAssets.isDappDefinitionAccountType -> AccountTag.DAPP_DEFINITION
                    account.isOlympia && account.isLedgerAccount -> AccountTag.LEDGER_LEGACY
                    account.isOlympia && !account.isLedgerAccount -> AccountTag.LEGACY_SOFTWARE
                    !account.isOlympia && account.isLedgerAccount -> AccountTag.LEDGER_BABYLON
                    else -> null
                },
                isFiatBalanceVisible = isFiatBalanceVisible,
                fiatTotalValue = prices.totalBalance(forAccount = accountWithAssets),
                isLoadingAssets = accountWithAssets.assets == null,
                isLoadingBalance = prices.isLoadingBalance(forAccount = accountWithAssets)
            )
        }

        val isFiatPricesDisabled: Boolean = prices is PricesState.Disabled

        val totalBalance: FiatPrice? = (prices as? PricesState.Enabled)?.totalBalance

        val isLoadingTotalBalance = prices is PricesState.None ||
            (prices is PricesState.Enabled && accountsWithAssets.orEmpty().any { prices.isLoadingBalance(it) })

        fun loadingAssets(
            accounts: List<Account>,
            refreshType: RefreshType
        ): State = copy(
            accountsWithAssets = accounts.map { account ->
                val oldAssets = accountsWithAssets?.find { it.account.address == account.address }

                oldAssets?.copy(account = account) ?: AccountWithAssets(account = account)
            },
            refreshType = refreshType
        )

        fun assetsReceived(
            accountsWithAssets: List<AccountWithAssets>
        ): State = copy(
            accountsWithAssets = accountsWithAssets
        )

        fun assetsError(
            error: Throwable
        ): State = copy(
            refreshType = RefreshType.None,
            uiMessage = if (refreshType is RefreshType.Automatic) null else UiMessage.ErrorMessage(error),
            accountsWithAssets = accountsWithAssets?.map { accountWithAssets ->
                if (accountWithAssets.assets == null) {
                    // If assets don't exist leave them empty
                    accountWithAssets.copy(assets = Assets())
                } else {
                    accountWithAssets
                }
            },
            prices = if (prices is PricesState.None) {
                // In case that prices were never received, show an error
                // in the prices state
                PricesState.Enabled(
                    pricesPerAccount = accountsWithAssets?.associate {
                        it.account.address to null
                    }.orEmpty()
                )
            } else {
                prices
            }
        )

        fun onPricesReceived(prices: Map<AccountAddress, List<AssetPrice>?>): State = copy(
            refreshType = RefreshType.None,
            prices = PricesState.Enabled(
                pricesPerAccount = prices
            )
        )

        fun disableFiatPrices() = copy(
            prices = PricesState.Disabled,
            refreshType = RefreshType.None,
        )

        enum class AccountTag {
            LEDGER_BABYLON, DAPP_DEFINITION, LEDGER_LEGACY, LEGACY_SOFTWARE
        }

        @SuppressLint("VisibleForTests")
        data class AccountUiItem(
            val account: Account,
            val assets: Assets?,
            val fiatTotalValue: FiatPrice?,
            val tag: AccountTag?,
            val securityPrompts: List<SecurityPromptType>?,
            val isFiatBalanceVisible: Boolean,
            val isLoadingAssets: Boolean,
            val isLoadingBalance: Boolean
        )

        sealed interface PricesState {
            // Shimmering state
            data object None : PricesState

            // Price service available
            data class Enabled(
                val pricesPerAccount: Map<AccountAddress, List<AssetPrice>?>
            ) : PricesState {

                val totalBalance: FiatPrice? = run {
                    val prices = (this as? Enabled)?.pricesPerAccount ?: return@run null

                    val isAnyAccountTotalFailed = prices.values.any { assetsPrices -> assetsPrices == null }
                    if (isAnyAccountTotalFailed) return@run null

                    var total = 0.toDecimal192()
                    var currency = SupportedCurrency.USD
                    prices.values.mapNotNull { it }.forEach { assetPrices ->
                        assetPrices.forEach { assetPrice ->
                            total += assetPrice.price?.price.orZero()
                            currency = assetPrice.price?.currency ?: SupportedCurrency.USD
                        }
                    }

                    FiatPrice(price = total, currency = currency)
                }
            }

            // Price service not available, nothing is shown
            data object Disabled : PricesState

            fun isLoadingBalance(forAccount: AccountWithAssets): Boolean = this is None ||
                (this is Enabled && !pricesPerAccount.containsKey(forAccount.account.address))

            /**
             * if the account has zero assets then return Zero price
             * if the account has assets but without prices then return Zero price
             * if the account has assets but failed to fetch prices then return Null
             */
            fun totalBalance(forAccount: AccountWithAssets): FiatPrice? {
                if (forAccount.assets?.ownsAnyAssetsThatContributeToBalance?.not() == true) {
                    return FiatPrice(price = 0.toDecimal192(), currency = SupportedCurrency.USD)
                }

                val assetsPrices = (this as? Enabled)?.pricesPerAccount?.get(forAccount.account.address) ?: return null
                val hasAtLeastOnePrice = assetsPrices.any { assetPrice -> assetPrice.price != null }

                return if (hasAtLeastOnePrice) {
                    var total = 0.toDecimal192()
                    var currency = SupportedCurrency.USD
                    assetsPrices.forEach { assetPrice ->
                        total += assetPrice.price?.price.orZero()
                        currency = assetPrice.price?.currency ?: SupportedCurrency.USD
                    }
                    FiatPrice(price = total, currency = currency)
                } else {
                    null
                }
            }
        }
    }

    internal sealed interface Event : OneOffEvent {

        data object NavigateToSecurityCenter : Event

        data object NavigateToLinkConnector : Event

        data class OpenUrl(val url: String) : Event
    }

    companion object {
        private val REFRESH_INTERVAL = 5.minutes
    }
}
