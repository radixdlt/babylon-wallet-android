package com.babylon.wallet.android.presentation.wallet

import android.annotation.SuppressLint
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.NPSSurveyState
import com.babylon.wallet.android.NPSSurveyStateObserver
import com.babylon.wallet.android.data.repository.tokenprice.FiatPriceRepository
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.babylon.wallet.android.domain.usecases.EntityWithSecurityPrompt
import com.babylon.wallet.android.domain.usecases.GetEntitiesWithSecurityPromptUseCase
import com.babylon.wallet.android.domain.usecases.SecurityPromptType
import com.babylon.wallet.android.domain.usecases.assets.GetFiatValueUseCase
import com.babylon.wallet.android.domain.usecases.assets.GetWalletAssetsUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddress
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEvent.RestoredMnemonic
import com.babylon.wallet.android.utils.AppEventBus
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.extensions.ProfileEntity
import com.radixdlt.sargon.extensions.orZero
import com.radixdlt.sargon.extensions.plus
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.extensions.toDecimal192
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.domain.assets.AssetPrice
import rdx.works.core.domain.assets.Assets
import rdx.works.core.domain.assets.FiatPrice
import rdx.works.core.domain.assets.SupportedCurrency
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.activeAccountOnCurrentNetwork
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.core.sargon.factorSourceId
import rdx.works.core.sargon.isLedgerAccount
import rdx.works.core.sargon.isOlympia
import rdx.works.profile.domain.EnsureBabylonFactorSourceExistUseCase
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.backup.GetBackupStateUseCase
import rdx.works.profile.domain.display.ChangeBalanceVisibilityUseCase
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class WalletViewModel @Inject constructor(
    private val getWalletAssetsUseCase: GetWalletAssetsUseCase,
    private val getFiatValueUseCase: GetFiatValueUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val getEntitiesWithSecurityPromptUseCase: GetEntitiesWithSecurityPromptUseCase,
    private val changeBalanceVisibilityUseCase: ChangeBalanceVisibilityUseCase,
    private val appEventBus: AppEventBus,
    private val ensureBabylonFactorSourceExistUseCase: EnsureBabylonFactorSourceExistUseCase,
    private val preferencesManager: PreferencesManager,
    private val npsSurveyStateObserver: NPSSurveyStateObserver,
    getBackupStateUseCase: GetBackupStateUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : StateViewModel<WalletUiState>(), OneOffEventHandler<WalletEvent> by OneOffEventHandlerImpl() {

    private var accountsWithAssets: List<AccountWithAssets>? = null
    private var accountsAddressesWithAssetsPrices: Map<AccountAddress, List<AssetPrice>?>? = null
    private var entitiesWithSecurityPrompt: List<EntityWithSecurityPrompt> = emptyList()
    private var isBackupWarningVisible: Boolean = false

    override fun initialState() = WalletUiState()

    private val refreshFlow = MutableSharedFlow<Unit>()
    private val accountsFlow = combine(
        getProfileUseCase.flow.map { it.activeAccountsOnCurrentNetwork }.distinctUntilChanged(),
        refreshFlow
    ) { accounts, _ ->
        accounts
    }

    val babylonFactorSourceDoesNotExistEvent =
        appEventBus.events.filterIsInstance<AppEvent.BabylonFactorSourceDoesNotExist>()

    init {
        viewModelScope.launch {
            if (ensureBabylonFactorSourceExistUseCase.babylonFactorSourceExist().not()) {
                appEventBus.sendEvent(AppEvent.BabylonFactorSourceDoesNotExist, delayMs = 500L)
                return@launch
            }
        }
        observeAccounts()
        observePrompts()
        observeProfileBackupState(getBackupStateUseCase)
        observeGlobalAppEvents()
        loadAssets(withRefresh = false)
        observeNpsSurveyState()
    }

    private fun observeNpsSurveyState() {
        viewModelScope.launch {
            npsSurveyStateObserver.npsSurveyState.filterIsInstance<NPSSurveyState.Active>().collectLatest {
                if (state.value.isNpsSurveyShown.not()) {
                    sendEvent(WalletEvent.ShowNpsSurvey)
                    _state.update { state -> state.copy(isNpsSurveyShown = true) }
                }
            }
        }
    }

    fun dismissSurvey() {
        viewModelScope.launch {
            _state.update { it.copy(isNpsSurveyShown = false) }
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
    private fun observeAccounts() {
        accountsFlow
            .flatMapLatest { accounts ->
                this.accountsWithAssets = accounts.map { account ->
                    val current = accountsWithAssets?.find { account == it.account }
                    AccountWithAssets(
                        account = account,
                        details = current?.details,
                        assets = current?.assets
                    )
                }

                _state.update { loadingAssets(isRefreshing = it.isRefreshing) }

                getWalletAssetsUseCase(accounts = accounts, isRefreshing = state.value.isRefreshing).catch { error ->
                    _state.update { onAssetsError(error) }
                    Timber.w(error)
                }
            }
            .mapLatest { accountsWithAssets ->
                this.accountsWithAssets = accountsWithAssets

                // keep the val here because the onAssetsReceived sets the refreshing to false
                val isRefreshing = state.value.isRefreshing

                _state.update { onAssetsReceived() }

                accountsAddressesWithAssetsPrices = accountsWithAssets.associate { accountWithAssets ->
                    accountWithAssets.account.address to getFiatValueUseCase.forAccount(
                        accountWithAssets = accountWithAssets,
                        isRefreshing = isRefreshing
                    ).onSuccess {
                        shouldEnableFiatPrices(isEnabled = true)
                    }.onFailure {
                        if (it is FiatPriceRepository.PricesNotSupportedInNetwork) {
                            shouldEnableFiatPrices(isEnabled = false)
                        }
                    }.getOrNull()
                }

                _state.update { onAssetsReceived() }
            }
            .flowOn(ioDispatcher)
            .launchIn(viewModelScope)
    }

    private fun observePrompts() {
        viewModelScope.launch {
            getEntitiesWithSecurityPromptUseCase().collect { entitiesWithSecurityPrompt ->
                this@WalletViewModel.entitiesWithSecurityPrompt = entitiesWithSecurityPrompt
                _state.update {
                    it.copy(
                        isSettingsWarningVisible = isBackupWarningVisible || anyBackupSecurityPrompt()
                    )
                }
            }
        }
        viewModelScope.launch {
            preferencesManager.isRadixBannerVisible.collect { isVisible ->
                _state.update { it.copy(isRadixBannerVisible = isVisible) }
            }
        }
    }

    private fun observeProfileBackupState(getBackupStateUseCase: GetBackupStateUseCase) {
        viewModelScope.launch {
            getBackupStateUseCase().collect { backupState ->
                this@WalletViewModel.isBackupWarningVisible = backupState.isWarningVisible

                _state.update {
                    it.copy(
                        isSettingsWarningVisible = isBackupWarningVisible || anyBackupSecurityPrompt()
                    )
                }
            }
        }
    }

    private fun observeGlobalAppEvents() {
        viewModelScope.launch {
            appEventBus.events.collect { event ->
                when (event) {
                    AppEvent.RefreshAssetsNeeded,
                    RestoredMnemonic -> {
                        loadAssets(withRefresh = event !is RestoredMnemonic)
                    }

                    AppEvent.NPSSurveySubmitted -> {
                        _state.update { it.copy(uiMessage = UiMessage.InfoMessage.NpsSurveySubmitted) }
                    }

                    else -> {}
                }
            }
        }
    }

    private fun loadAssets(withRefresh: Boolean) {
        _state.update { it.copy(isRefreshing = withRefresh) }
        viewModelScope.launch { refreshFlow.emit(Unit) }
    }

    fun onRefresh() {
        loadAssets(withRefresh = true)
    }

    fun onShowHideBalanceToggle(isVisible: Boolean) {
        viewModelScope.launch {
            changeBalanceVisibilityUseCase(isVisible = isVisible)
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    fun onApplySecuritySettings(account: Account, securityPromptType: SecurityPromptType) {
        viewModelScope.launch {
            val factorSourceId = getProfileUseCase().activeAccountOnCurrentNetwork(withAddress = account.address)
                ?.factorSourceId as? FactorSourceId.Hash ?: return@launch

            when (securityPromptType) {
                SecurityPromptType.NEEDS_BACKUP -> sendEvent(WalletEvent.NavigateToMnemonicBackup(factorSourceId))
                SecurityPromptType.NEEDS_RESTORE -> sendEvent(WalletEvent.NavigateToMnemonicRestore(factorSourceId))
            }
        }
    }

    fun onRadixBannerDismiss() = viewModelScope.launch {
        preferencesManager.setRadixBannerVisibility(isVisible = false)
    }

    private fun shouldEnableFiatPrices(isEnabled: Boolean) {
        _state.update { walletUiState ->
            walletUiState.copy(isFiatBalancesEnabled = isEnabled)
        }
    }

    private fun loadingAssets(isRefreshing: Boolean): WalletUiState = state.value.copy(
        accountUiItems = buildAccountUiItems(),
        isLoading = accountsWithAssets == null,
        isRefreshing = isRefreshing
    )

    private fun onAssetsReceived(): WalletUiState {
        val accountUiItems = buildAccountUiItems()

        return state.value.copy(
            isLoading = false,
            isRefreshing = false,
            accountUiItems = accountUiItems,
            totalFiatValueOfWallet = buildTotalFiatValue()
        )
    }

    private fun onAssetsError(error: Throwable?): WalletUiState = state.value.copy(
        uiMessage = UiMessage.ErrorMessage(error),
        accountUiItems = state.value.accountUiItems.map { account ->
            if (account.assets == null) {
                // If assets don't exist leave them empty
                account.copy(assets = Assets())
            } else {
                // Else continue with what the user used to see before the refresh
                account
            }
        },
        isLoading = false,
        isRefreshing = false
    )

    private fun buildAccountUiItems(): List<WalletUiState.AccountUiItem> {
        return accountsWithAssets.orEmpty()
            .map { accountWithAssets ->
                val isFiatBalanceVisible = accountWithAssets.assets == null ||
                    accountWithAssets.assets.ownsAnyAssetsThatContributeToBalance

                WalletUiState.AccountUiItem(
                    account = accountWithAssets.account,
                    address = ActionableAddress.Address(Address.Account(accountWithAssets.account.address)),
                    assets = accountWithAssets.assets,
                    securityPromptType = securityPrompt(accountWithAssets.account),
                    tag = getTag(accountWithAssets.account),
                    isFiatBalanceVisible = state.value.isFiatBalancesEnabled && isFiatBalanceVisible,
                    fiatTotalValue = totalFiatValueForAccount(accountWithAssets.account.address),
                    isLoadingAssets = accountWithAssets.assets == null,
                    isLoadingBalance = accountWithAssets.assets == null ||
                        isBalanceLoadingForAccount(accountWithAssets.account.address)
                )
            }
    }

    private fun isBalanceLoadingForAccount(accountAddress: AccountAddress): Boolean {
        return accountsAddressesWithAssetsPrices?.containsKey(accountAddress) != true
    }

    /**
     * if at least one account failed to fetch at least one price then return Zero
     */
    private fun buildTotalFiatValue(): FiatPrice? {
        val isAnyAccountTotalFailed = accountsAddressesWithAssetsPrices?.values?.any { assetsPrices ->
            assetsPrices == null
        } ?: false
        if (isAnyAccountTotalFailed) return null

        var total = 0.toDecimal192()
        var currency = SupportedCurrency.USD
        accountsAddressesWithAssetsPrices?.values?.forEach {
            it?.let { assetsPrices ->
                assetsPrices.forEach { assetPrice ->
                    total += assetPrice.price?.price.orZero()
                    currency = assetPrice.price?.currency ?: SupportedCurrency.USD
                }
            }
        } ?: return null

        return FiatPrice(price = total, currency = currency)
    }

    /**
     * if the account has zero assets then return Zero price
     * if the account has assets but without prices then return Zero price
     * if the account has assets but failed to fetch prices then return Null
     */
    private fun totalFiatValueForAccount(accountAddress: AccountAddress): FiatPrice? {
        val accountWithAssets = accountsWithAssets?.find {
            it.account.address == accountAddress
        }
        if (accountWithAssets?.assets?.ownsAnyAssetsThatContributeToBalance?.not() == true) {
            return FiatPrice(price = 0.toDecimal192(), currency = SupportedCurrency.USD)
        }

        val assetsPrices = accountsAddressesWithAssetsPrices?.get(accountAddress) ?: return null

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
            FiatPrice(price = 0.toDecimal192(), currency = SupportedCurrency.USD)
        }
    }

    private fun securityPrompt(forAccount: Account) = entitiesWithSecurityPrompt.find {
        it.entity.address.string == forAccount.address.string
    }?.prompt

    private fun anyBackupSecurityPrompt() = entitiesWithSecurityPrompt.any {
        it.entity is ProfileEntity.PersonaEntity && it.prompt == SecurityPromptType.NEEDS_BACKUP
    }

    private fun getTag(forAccount: Account): WalletUiState.AccountTag? {
        return when {
            !isDappDefinitionAccount(forAccount) && !isLegacyAccount(forAccount) && !isLedgerAccount(forAccount) -> null
            isDappDefinitionAccount(forAccount) -> WalletUiState.AccountTag.DAPP_DEFINITION
            isLegacyAccount(forAccount) && isLedgerAccount(forAccount) -> WalletUiState.AccountTag.LEDGER_LEGACY
            isLegacyAccount(forAccount) && !isLedgerAccount(forAccount) -> WalletUiState.AccountTag.LEGACY_SOFTWARE
            !isLegacyAccount(forAccount) && isLedgerAccount(forAccount) -> WalletUiState.AccountTag.LEDGER_BABYLON
            else -> null
        }
    }

    private fun isLegacyAccount(forAccount: Account): Boolean = forAccount.isOlympia

    private fun isLedgerAccount(forAccount: Account): Boolean {
        return forAccount.isLedgerAccount
    }

    private fun isDappDefinitionAccount(forAccount: Account): Boolean {
        return accountsWithAssets?.find { accountWithAssets ->
            accountWithAssets.account.address == forAccount.address
        }?.isDappDefinitionAccountType ?: false
    }
}

internal sealed interface WalletEvent : OneOffEvent {
    data class NavigateToMnemonicBackup(val factorSourceId: FactorSourceId.Hash) : WalletEvent
    data class NavigateToMnemonicRestore(val factorSourceId: FactorSourceId.Hash) : WalletEvent

    data object ShowNpsSurvey : WalletEvent
}

data class WalletUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val accountUiItems: List<AccountUiItem> = emptyList(),
    val isRadixBannerVisible: Boolean = false,
    val isFiatBalancesEnabled: Boolean = true,
    val uiMessage: UiMessage? = null,
    val isNpsSurveyShown: Boolean = false,
    val totalFiatValueOfWallet: FiatPrice? = null,
    val isSettingsWarningVisible: Boolean = false
) : UiState {

    enum class AccountTag {
        LEDGER_BABYLON, DAPP_DEFINITION, LEDGER_LEGACY, LEGACY_SOFTWARE
    }

    @SuppressLint("VisibleForTests")
    data class AccountUiItem(
        val account: Account,
        val address: ActionableAddress,
        val assets: Assets?,
        val fiatTotalValue: FiatPrice?,
        val tag: AccountTag?,
        val securityPromptType: SecurityPromptType?,
        val isFiatBalanceVisible: Boolean,
        val isLoadingAssets: Boolean,
        val isLoadingBalance: Boolean
    )
}
