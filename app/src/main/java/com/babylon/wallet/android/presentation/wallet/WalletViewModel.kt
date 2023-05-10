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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
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
) : StateViewModel<WalletState>(), OneOffEventHandler<WalletEvent> by OneOffEventHandlerImpl() {

    override fun initialState() = WalletState()

    private val refreshFlow = MutableSharedFlow<Unit>()
    private val accountsFlow = combine(
        getProfileUseCase().map { it.factorSources },
        getProfileUseCase.accountsOnCurrentNetwork.distinctUntilChanged(),
        refreshFlow
    ) { factorSources, accounts, _ ->
        Pair(factorSources, accounts)
    }

    init {
        observeAccounts()
        observePrompts()
        observeProfileBackupState(getBackupStateUseCase)
        observeGlobalAppEvents()
        refresh()
    }

    private fun observeAccounts() {
        viewModelScope.launch {
            accountsFlow.collect { pair ->
                _state.update { state ->
                    state.copy(
                        factorSources = pair.first,
                        resources = pair.second.map {
                            AccountWithResources(account = it, resources = null)
                        },
                        loading = true
                    )
                }

                getAccountsWithResourcesUseCase(pair.second, isRefreshing = true)
                    .onValue { resources ->
                        _state.update {
                            it.copy(resources = resources, loading = false)
                        }
                    }
                    .onError { error ->
                        _state.update { it.copy(error = UiMessage.ErrorMessage(error), loading = false) }
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
                refresh()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch { refreshFlow.emit(Unit) }
    }

    fun onMessageShown() {
        _state.update { it.copy(error = null) }
    }

    fun onApplyMnemonicBackup(address: String) {
        viewModelScope.launch {
            getProfileUseCase.accountOnCurrentNetwork(address)?.unsecuredFactorSourceId()?.let {
                sendEvent(WalletEvent.NavigateToMnemonicBackup(it))
            }
        }
    }

    fun onAccountClick(address: String) {
        viewModelScope.launch {
            sendEvent(WalletEvent.AccountClick(address))
        }
    }
}

internal sealed interface WalletEvent : OneOffEvent {
    data class AccountClick(val address: String) : WalletEvent
    data class NavigateToMnemonicBackup(val factorSourceId: FactorSource.ID) : WalletEvent
}

data class WalletState(
    private val factorSources: List<FactorSource> = emptyList(),
    private val resources: List<AccountWithResources>? = null,
    private val loading: Boolean = true,
    private val backedUpFactorSourceIds: Set<String> = emptySet(),
    val isSettingsWarningVisible: Boolean = false,
    val error: UiMessage? = null,
) : UiState {

    val accountResources: List<AccountWithResources>
        get() = resources ?: emptyList()

    val isLoading: Boolean
        get() = resources == null && loading

    val isRefreshing: Boolean
        get() = resources != null && loading

    fun isMnemonicBackupNeeded(forAccount: Network.Account): Boolean {
        val unsecuredFactorSourceId = forAccount.unsecuredFactorSourceId() ?: return false

        return backedUpFactorSourceIds.any { it == unsecuredFactorSourceId.value }
    }

}

data class WalletUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val accountsWithResources: ImmutableList<AccountWithResources> = persistentListOf(),
    val error: UiMessage? = null,
    val isBackupWarningVisible: Boolean = false
) : UiState
