package com.babylon.wallet.android.presentation.wallet

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.common.onError
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.model.AccountResources
import com.babylon.wallet.android.domain.model.toDomainModel
import com.babylon.wallet.android.domain.usecases.GetAccountResourcesUseCase
import com.babylon.wallet.android.presentation.common.BaseViewModel
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.encodeUtf8
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.domain.GetProfileStateUseCase
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork
import rdx.works.profile.domain.exists
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val getAccountResourcesUseCase: GetAccountResourcesUseCase,
    private val getProfileStateUseCase: GetProfileStateUseCase,
    private val getProfileUseCase: GetProfileUseCase
) : BaseViewModel<WalletUiState>(), OneOffEventHandler<WalletEvent> by OneOffEventHandlerImpl() {

    override fun initialState(): WalletUiState = WalletUiState()

    init {
        viewModelScope.launch { // TODO probably here we can observe the accounts from network repository
            getProfileUseCase().collect {
                loadResourceData(isRefreshing = false)
            }
        }
    }

    private suspend fun loadResourceData(isRefreshing: Boolean) {
        viewModelScope.launch {
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

    fun onAccountClick(address: String, name: String) {
        viewModelScope.launch {
            sendEvent(WalletEvent.AccountClick(address, name.encodeUtf8()))
        }
    }
}

internal sealed interface WalletEvent : OneOffEvent {
    data class AccountClick(val address: String, val nameEncoded: String) : WalletEvent
}

data class WalletUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val resources: ImmutableList<AccountResources> = persistentListOf(),
    val error: UiMessage? = null,
) : UiState
