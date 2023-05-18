package com.babylon.wallet.android.presentation.wallet

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.common.onError
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.model.AccountWithResources
import com.babylon.wallet.android.domain.usecases.GetAccountsWithResourcesUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.utils.unsecuredFactorSourceId
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork
import rdx.works.profile.domain.accountsOnCurrentNetwork
import rdx.works.profile.domain.backup.GetBackupStateUseCase
import javax.inject.Inject

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val getAccountsWithResourcesUseCase: GetAccountsWithResourcesUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val preferencesManager: PreferencesManager,
    private val appEventBus: AppEventBus,
    getBackupStateUseCase: GetBackupStateUseCase
) : StateViewModel<WalletUiState>(), OneOffEventHandler<WalletEvent> by OneOffEventHandlerImpl() {

    override fun initialState() = WalletUiState()

    private val refreshFlow = MutableSharedFlow<Unit>()
    private val accountsFlow = combine(
        getProfileUseCase.accountsOnCurrentNetwork.distinctUntilChanged(),
        refreshFlow
    ) { accounts, _ -> accounts }

    init {
        observeAccounts()
        observePrompts()
        observeProfileBackupState(getBackupStateUseCase)
        observeGlobalAppEvents()
        loadResources(withRefresh = false)
    }

    private fun observeAccounts() {
        viewModelScope.launch {
            accountsFlow.collect { accounts ->
                _state.update { state ->
                    state.loadingResources(accounts = accounts, isRefreshing = state.isRefreshing)
                }

                getAccountsWithResourcesUseCase(accounts, isRefreshing = _state.value.isRefreshing)
                    .onValue { resources ->
                        _state.update { it.onResourcesReceived(resources) }
                    }
                    .onError { error ->
                        _state.update { it.onResourcesError(error) }
                    }
            }
        }
    }

    private fun observePrompts() {
        viewModelScope.launch {
            preferencesManager
                .getBackedUpFactorSourceIds()
                .distinctUntilChanged()
                .collect { backedUpFactorSourceIds ->
                    _state.update { it.copy(backedUpFactorSourceIds = backedUpFactorSourceIds) }
                }
        }
    }

    private fun observeProfileBackupState(getBackupStateUseCase: GetBackupStateUseCase) {
        viewModelScope.launch {
            getBackupStateUseCase()
                .collect { backupState ->
                    _state.update { it.copy(isSettingsWarningVisible = backupState.isWarningVisible) }
                }
        }
    }

    private fun observeGlobalAppEvents() {
        viewModelScope.launch {
            appEventBus.events.filter { event ->
                event is AppEvent.GotFreeXrd || event is AppEvent.ApprovedTransaction
            }.collect {
                loadResources(withRefresh = true)
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
        _state.update { it.copy(error = null) }
    }

    fun onApplyMnemonicBackup(account: Network.Account) {
        viewModelScope.launch {
            getProfileUseCase.accountOnCurrentNetwork(account.address)?.unsecuredFactorSourceId()?.let {
                sendEvent(WalletEvent.NavigateToMnemonicBackup(it))
            }
        }
    }
}

internal sealed interface WalletEvent : OneOffEvent {
    data class NavigateToMnemonicBackup(val factorSourceId: FactorSource.ID) : WalletEvent
}

data class WalletUiState(
    private val accountsWithResources: List<AccountWithResources>? = null,
    private val loading: Boolean = true,
    private val refreshing: Boolean = false,
    private val backedUpFactorSourceIds: Set<String> = emptySet(),
    val isSettingsWarningVisible: Boolean = false,
    val error: UiMessage? = null,
) : UiState {

    val accountResources: List<AccountWithResources>
        get() = accountsWithResources.orEmpty()

    /**
     * Initial loading of the screen.
     */
    val isLoading: Boolean
        get() = accountsWithResources == null && loading

    val isLoadingResources: Boolean
        get() = accountsWithResources != null && accountsWithResources.none { it.resources != null } && loading

    /**
     * Used in pull to refresh mode.
     */
    val isRefreshing: Boolean
        get() = refreshing

    fun isSecurityPromptVisible(forAccount: Network.Account): Boolean {
        val unsecuredFactorSourceId = forAccount.unsecuredFactorSourceId() ?: return false

        return accountsWithResources?.find { it.account == forAccount }?.hasXrd() == true &&
            backedUpFactorSourceIds.none { it == unsecuredFactorSourceId.value }
    }

    fun loadingResources(accounts: List<Network.Account>, isRefreshing: Boolean): WalletUiState = copy(
        accountsWithResources = accounts.map { account ->
            AccountWithResources(
                account = account,
                resources = accountsWithResources?.find { account == it.account }?.resources
            )
        },
        loading = true,
        refreshing = isRefreshing
    )

    fun onResourcesReceived(accountsWithResources: List<AccountWithResources>): WalletUiState = copy(
        accountsWithResources = accountsWithResources,
        loading = false,
        refreshing = false
    )

    fun onResourcesError(error: Throwable?): WalletUiState = copy(
        error = UiMessage.ErrorMessage(error),
        loading = false,
        refreshing = false
    )
}
