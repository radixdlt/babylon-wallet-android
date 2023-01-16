package com.babylon.wallet.android.presentation.wallet

import android.content.ClipData
import android.content.ClipboardManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.common.OneOffEvent
import com.babylon.wallet.android.domain.common.OneOffEventHandler
import com.babylon.wallet.android.domain.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.domain.common.onError
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.model.AccountResources
import com.babylon.wallet.android.domain.usecases.GetAccountResourcesUseCase
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.utils.encodeUtf8
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.repository.ProfileDataSource
import javax.inject.Inject

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val clipboardManager: ClipboardManager,
    private val getAccountResourcesUseCase: GetAccountResourcesUseCase,
    private val profileDataSource: ProfileDataSource,
) : ViewModel(), OneOffEventHandler<WalletEvent> by OneOffEventHandlerImpl() {

    private val _walletUiState: MutableStateFlow<WalletUiState> = MutableStateFlow(WalletUiState())
    val walletUiState = _walletUiState.asStateFlow()

    init {
        viewModelScope.launch { // TODO probably here we can observe the accounts from network repository
            profileDataSource.profile.filterNotNull().collect {
                loadResourceData()
            }
        }
    }

    private suspend fun loadResourceData() {
        viewModelScope.launch {
            val result = getAccountResourcesUseCase()
            result.onError { error ->
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
            profileDataSource.readProfile()?.let {
                loadResourceData()
            }
            _walletUiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun onCopyAccountAddress(hashValue: String) {
        val clipData = ClipData.newPlainText("accountHash", hashValue)
        clipboardManager.setPrimaryClip(clipData)
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
