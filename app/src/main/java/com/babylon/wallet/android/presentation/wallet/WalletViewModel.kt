package com.babylon.wallet.android.presentation.wallet

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.common.onError
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.model.AccountResources
import com.babylon.wallet.android.domain.model.toDomainModel
import com.babylon.wallet.android.domain.usecases.GetAccountResourcesUseCase
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
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.domain.GetProfileStateUseCase
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork
import rdx.works.profile.domain.accountsOnCurrentNetwork
import rdx.works.profile.domain.backup.GetBackupStateUseCase
import rdx.works.profile.domain.exists
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val getAccountResourcesUseCase: GetAccountResourcesUseCase,
    private val getProfileStateUseCase: GetProfileStateUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val preferencesManager: PreferencesManager,
    private val appEventBus: AppEventBus,
    getBackupStateUseCase: GetBackupStateUseCase
) : StateViewModel<WalletUiState>(), OneOffEventHandler<WalletEvent> by OneOffEventHandlerImpl() {

    override fun initialState() = WalletUiState()

    init {
        viewModelScope.launch { // TODO probably here we can observe the accounts from network repository
            getProfileUseCase().collect {
                loadResourceData(isRefreshing = false)
            }
        }

        viewModelScope.launch {
            getBackupStateUseCase().collect { backupState ->
                _state.update { it.copy(isBackupWarningVisible = backupState.isWarningVisible) }
            }
        }
        observeBackedUpMnemonics()
        observeGlobalAppEvents()
    }

    private fun observeGlobalAppEvents() {
        viewModelScope.launch {
            appEventBus.events.filter { event ->
                event is AppEvent.GotFreeXrd || event is AppEvent.ApprovedTransaction
            }.collect {
                loadResourceData(true)
            }
        }
    }

    private fun observeBackedUpMnemonics() {
        viewModelScope.launch {
            preferencesManager.getBackedUpFactorSourceIds().distinctUntilChanged().collect {
                loadResourceData(isRefreshing = false)
            }
        }
    }

    private suspend fun loadResourceData(isRefreshing: Boolean) {
        _state.update { state ->
            state.copy(
                resources = getProfileUseCase.accountsOnCurrentNetwork()
                    .map { it.toDomainModel() }
                    .toPersistentList(),
                isLoading = false
            )
        }
        val result = getAccountResourcesUseCase.getAccountsFromProfile(isRefreshing = isRefreshing)
        result.onError { error ->
            Timber.w(error)
            _state.update { it.copy(error = UiMessage.ErrorMessage(error = error), isLoading = false) }
        }
        result.onValue { resourceList ->
            _state.update { state ->
                state.copy(resources = resourceList.toPersistentList(), isLoading = false)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            if (getProfileStateUseCase.exists()) {
                loadResourceData(isRefreshing = true)
            }
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(error = null) }
    }

    fun onApplySecuritySettings(address: String) {
        viewModelScope.launch {
            getProfileUseCase.accountOnCurrentNetwork(address)?.accountFactorSourceId()?.let {
                sendEvent(WalletEvent.ApplySecuritySettingsClick(it.value))
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
    data class ApplySecuritySettingsClick(val factorSourceIdString: String) : WalletEvent
}

data class WalletUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val resources: ImmutableList<AccountResources> = persistentListOf(),
    val error: UiMessage? = null,
    val isBackupWarningVisible: Boolean = false
) : UiState
