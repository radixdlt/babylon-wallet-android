@file:OptIn(ExperimentalCoroutinesApi::class)

package com.babylon.wallet.android.presentation.wallet

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.babylon.wallet.android.domain.model.assets.Assets
import com.babylon.wallet.android.domain.usecases.EntityWithSecurityPrompt
import com.babylon.wallet.android.domain.usecases.GetEntitiesWithSecurityPromptUseCase
import com.babylon.wallet.android.domain.usecases.NPSSurveyState
import com.babylon.wallet.android.domain.usecases.NPSSurveyUseCase
import com.babylon.wallet.android.domain.usecases.SecurityPromptType
import com.babylon.wallet.android.domain.usecases.assets.GetWalletAssetsUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEvent.RestoredMnemonic
import com.babylon.wallet.android.utils.AppEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.data.model.extensions.factorSourceId
import rdx.works.profile.data.model.extensions.isOlympiaAccount
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.FactorSource.FactorSourceID
import rdx.works.profile.data.model.factorsources.FactorSourceKind
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.EnsureBabylonFactorSourceExistUseCase
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork
import rdx.works.profile.domain.activeAccountsOnCurrentNetwork
import rdx.works.profile.domain.backup.GetBackupStateUseCase
import rdx.works.profile.domain.factorSources
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class WalletViewModel @Inject constructor(
    private val getWalletAssetsUseCase: GetWalletAssetsUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val getEntitiesWithSecurityPromptUseCase: GetEntitiesWithSecurityPromptUseCase,
    private val appEventBus: AppEventBus,
    private val ensureBabylonFactorSourceExistUseCase: EnsureBabylonFactorSourceExistUseCase,
    private val preferencesManager: PreferencesManager,
    private val npsSurveyUseCase: NPSSurveyUseCase,
    getBackupStateUseCase: GetBackupStateUseCase
) : StateViewModel<WalletUiState>(), OneOffEventHandler<WalletEvent> by OneOffEventHandlerImpl() {

    override fun initialState() = WalletUiState()

    private val refreshFlow = MutableSharedFlow<Unit>()
    private val accountsFlow = combine(
        getProfileUseCase.activeAccountsOnCurrentNetwork.distinctUntilChanged(),
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
        observeDeviceFactorSources()
        observePrompts()
        observeProfileBackupState(getBackupStateUseCase)
        observeGlobalAppEvents()
        loadResources(withRefresh = false)
        viewModelScope.launch {
            npsSurveyUseCase.npsSurveyState().filter { it is NPSSurveyState.Active }.collectLatest { state ->
                _state.update { it.copy(isNpsSurveyShown = state == NPSSurveyState.Active) }
            }
        }
    }

    fun dismissNpsSurvey() {
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

    private fun observeAccounts() {
        accountsFlow
            .flatMapLatest { accounts ->
                _state.update { it.loadingResources(accounts = accounts, isRefreshing = it.isRefreshing) }
                getWalletAssetsUseCase(accounts = accounts, isRefreshing = state.value.isRefreshing).catch { error ->
                    _state.update { it.onResourcesError(error) }
                    Timber.w(error)
                }
            }
            .onEach { resources ->
                _state.update { it.onResourcesReceived(resources) }
            }
            .launchIn(viewModelScope)
    }

    private fun observeDeviceFactorSources() {
        viewModelScope.launch {
            getProfileUseCase.factorSources.collect { factorSourcesList ->
                _state.update { state ->
                    state.copy(factorSources = factorSourcesList.toPersistentList())
                }
            }
        }
    }

    private fun observePrompts() {
        viewModelScope.launch {
            getEntitiesWithSecurityPromptUseCase().collect { accounts ->
                _state.update { it.copy(entitiesWithSecurityPrompt = accounts) }
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
                _state.update { it.copy(isBackupWarningVisible = backupState.isWarningVisible) }
            }
        }
    }

    private fun observeGlobalAppEvents() {
        viewModelScope.launch {
            appEventBus.events.collect { event ->
                when (event) {
                    AppEvent.RefreshResourcesNeeded,
                    RestoredMnemonic -> {
                        loadResources(withRefresh = event !is RestoredMnemonic)
                    }

                    AppEvent.NPSSurveySubmitted -> {
                        _state.update { it.copy(uiMessage = UiMessage.InfoMessage.NpsSurveySubmitted) }
                    }

                    else -> {}
                }
            }
        }
    }

    private fun loadResources(withRefresh: Boolean) {
        _state.update { it.copy(refreshing = withRefresh) }
        viewModelScope.launch { refreshFlow.emit(Unit) }
    }

    fun onRefresh() {
        loadResources(withRefresh = true)
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    fun onApplySecuritySettings(account: Network.Account, securityPromptType: SecurityPromptType) {
        viewModelScope.launch {
            val factorSourceId =
                getProfileUseCase.accountOnCurrentNetwork(account.address)?.factorSourceId as? FactorSourceID.FromHash ?: return@launch

            when (securityPromptType) {
                SecurityPromptType.NEEDS_BACKUP -> sendEvent(WalletEvent.NavigateToMnemonicBackup(factorSourceId))
                SecurityPromptType.NEEDS_RESTORE -> sendEvent(WalletEvent.NavigateToMnemonicRestore(factorSourceId))
            }
        }
    }

    fun onRadixBannerDismiss() = viewModelScope.launch {
        preferencesManager.setRadixBannerVisibility(isVisible = false)
    }
}

internal sealed interface WalletEvent : OneOffEvent {
    data class NavigateToMnemonicBackup(val factorSourceId: FactorSourceID.FromHash) : WalletEvent
    data class NavigateToMnemonicRestore(val factorSourceId: FactorSourceID.FromHash) : WalletEvent
}

data class WalletUiState(
    private val accountsWithResources: List<AccountWithAssets>? = null,
    private val loading: Boolean = true,
    private val refreshing: Boolean = false,
    private val entitiesWithSecurityPrompt: List<EntityWithSecurityPrompt> = emptyList(),
    private val factorSources: List<FactorSource> = emptyList(),
    val isRadixBannerVisible: Boolean = false,
    val isBackupWarningVisible: Boolean = false,
    val uiMessage: UiMessage? = null,
    val isNpsSurveyShown: Boolean = false
) : UiState {

    val accountResources: List<AccountWithAssets>
        get() = accountsWithResources.orEmpty()

    /**
     * Initial loading of the screen.
     */
    val isLoading: Boolean
        get() = accountsWithResources == null && loading

    /**
     * Used in pull to refresh mode.
     */
    val isRefreshing: Boolean
        get() = refreshing

    fun securityPrompt(forAccount: Network.Account) = entitiesWithSecurityPrompt.find {
        it.entity.address == forAccount.address
    }?.prompt

    val isSettingsWarningVisible: Boolean
        get() = isBackupWarningVisible || entitiesWithSecurityPrompt.any {
            it.entity is Network.Persona && it.prompt == SecurityPromptType.NEEDS_BACKUP
        }

    fun getTag(forAccount: Network.Account): AccountTag? {
        return when {
            !isDappDefinitionAccount(forAccount) && !isLegacyAccount(forAccount) && !isLedgerAccount(forAccount) -> null
            isDappDefinitionAccount(forAccount) -> AccountTag.DAPP_DEFINITION
            isLegacyAccount(forAccount) && isLedgerAccount(forAccount) -> AccountTag.LEDGER_LEGACY
            isLegacyAccount(forAccount) && !isLedgerAccount(forAccount) -> AccountTag.LEGACY_SOFTWARE
            !isLegacyAccount(forAccount) && isLedgerAccount(forAccount) -> AccountTag.LEDGER_BABYLON
            else -> null
        }
    }

    private fun isLegacyAccount(forAccount: Network.Account): Boolean = forAccount.isOlympiaAccount()

    private fun isLedgerAccount(forAccount: Network.Account): Boolean {
        val factorSource = factorSources.find {
            it.id == forAccount.factorSourceId
        }?.id?.kind

        return factorSource == FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET
    }

    private fun isDappDefinitionAccount(forAccount: Network.Account): Boolean {
        return accountResources.find { accountWithResources ->
            accountWithResources.account.address == forAccount.address
        }?.isDappDefinitionAccountType ?: false
    }

    fun loadingResources(accounts: List<Network.Account>, isRefreshing: Boolean): WalletUiState = copy(
        accountsWithResources = accounts.map { account ->
            val current = accountsWithResources?.find { account == it.account }
            AccountWithAssets(
                account = account,
                details = current?.details,
                assets = current?.assets
            )
        },
        loading = true,
        refreshing = isRefreshing
    )

    fun onResourcesReceived(accountsWithResources: List<AccountWithAssets>): WalletUiState = copy(
        accountsWithResources = accountsWithResources,
        loading = false,
        refreshing = false
    )

    fun onResourcesError(error: Throwable?): WalletUiState = copy(
        uiMessage = UiMessage.ErrorMessage(error),
        accountsWithResources = accountsWithResources?.map { account ->
            if (account.assets == null) {
                // If assets don't exist leave them empty
                account.copy(assets = Assets())
            } else {
                // Else continue with what the user used to see before the refresh
                account
            }
        },
        loading = false,
        refreshing = false
    )

    enum class AccountTag {
        LEDGER_BABYLON, DAPP_DEFINITION, LEDGER_LEGACY, LEGACY_SOFTWARE
    }
}
