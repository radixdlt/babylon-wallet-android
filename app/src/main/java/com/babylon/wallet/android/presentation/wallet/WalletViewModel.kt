package com.babylon.wallet.android.presentation.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.common.onError
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.model.AccountResources
import com.babylon.wallet.android.domain.model.toDomainModel
import com.babylon.wallet.android.domain.usecases.GetAccountResourcesUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.utils.encodeUtf8
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.ProfileState
import rdx.works.profile.data.repository.AccountRepository
import rdx.works.profile.data.repository.ProfileDataSource
import rdx.works.profile.data.repository.profile
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val getAccountResourcesUseCase: GetAccountResourcesUseCase,
    private val profileDataSource: ProfileDataSource,
    private val accountRepository: AccountRepository
) : ViewModel(), OneOffEventHandler<WalletEvent> by OneOffEventHandlerImpl() {

    private val _walletUiState: MutableStateFlow<WalletUiState> = MutableStateFlow(WalletUiState())
    val walletUiState = _walletUiState.asStateFlow()

    init {
        viewModelScope.launch { // TODO probably here we can observe the accounts from network repository
            profileDataSource.profile.collect {
                loadResourceData(isRefreshing = false)
            }
        }
    }

    private suspend fun loadResourceData(isRefreshing: Boolean) {
        viewModelScope.launch {
            _walletUiState.update { state ->
                state.copy(
                    resources = accountRepository.getAccounts().map { it.toDomainModel() }.toPersistentList(),
                    isLoading = false
                )
            }
            val result = getAccountResourcesUseCase.getAccountsFromProfile(isRefreshing = isRefreshing)
            result.onError { error ->
                Timber.w(error)
                _walletUiState.update { it.copy(error = UiMessage.ErrorMessage(error = error), isLoading = false) }
            }
            result.onValue { resourceList ->
                _walletUiState.update { state ->
                    state.copy(resources = resourceList.toPersistentList(), isLoading = false)
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _walletUiState.update { it.copy(isRefreshing = true) }
            if (profileDataSource.profileState.first() is ProfileState.Restored) {
                loadResourceData(isRefreshing = true)
            }
            _walletUiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun onMessageShown() {
        _walletUiState.update { it.copy(error = null) }
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
)
