package com.babylon.wallet.android.presentation.wallet

import android.content.ClipData
import android.content.ClipboardManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.MainViewRepository
import com.babylon.wallet.android.domain.common.onError
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.model.AccountResources
import com.babylon.wallet.android.domain.usecase.wallet.GetAccountResourcesUseCase
import com.babylon.wallet.android.presentation.common.UiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.repository.ProfileRepository
import javax.inject.Inject

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val mainViewRepository: MainViewRepository,
    private val clipboardManager: ClipboardManager,
    private val getAccountsUseCase: GetAccountResourcesUseCase,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _walletUiState: MutableStateFlow<WalletUiState> = MutableStateFlow(WalletUiState())
    val walletUiState = _walletUiState.asStateFlow()

    init {
        viewModelScope.launch {
            profileRepository.profileSnapshot.filterNotNull().collect { profileSnapshot ->
                loadResourceData()
            }
        }
    }

    private suspend fun loadResourceData() {
        val wallet = mainViewRepository.getWallet()
        viewModelScope.launch {
            val result = getAccountsUseCase()
            result.onError { error ->
                _walletUiState.update { it.copy(error = UiMessage.ErrorMessage(error = error), isLoading = false) }
            }
            result.onValue { resourceList ->
                _walletUiState.update { state ->
                    state.copy(wallet = wallet, resources = resourceList.toPersistentList(), isLoading = false)
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _walletUiState.update { it.copy(isRefreshing = true) }
            profileRepository.readProfileSnapshot()?.let { profileSnapshot ->
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
}

data class WalletUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val wallet: WalletData? = null,
    val resources: ImmutableList<AccountResources> = persistentListOf(),
    val error: UiMessage? = null
)

data class WalletData(
    val currency: String,
    val amount: String
)
