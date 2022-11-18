package com.babylon.wallet.android.presentation.wallet

import android.content.ClipData
import android.content.ClipboardManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.gateway.HammunetGatewayTestConstants
import com.babylon.wallet.android.domain.MainViewRepository
import com.babylon.wallet.android.domain.model.AccountResources
import com.babylon.wallet.android.domain.onValue
import com.babylon.wallet.android.domain.usecase.wallet.RequestAccountResourcesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val mainViewRepository: MainViewRepository,
    private val clipboardManager: ClipboardManager,
    private val requestAccountsUseCase: RequestAccountResourcesUseCase
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _walletUiState: MutableStateFlow<WalletUiState> = MutableStateFlow(WalletUiState.Loading)
    val walletUiState = _walletUiState.asStateFlow()

    init {
        viewModelScope.launch {
            loadResourceData()
        }
    }

    private suspend fun loadResourceData() {
        val callResult = requestAccountsUseCase.getAccountResources(HammunetGatewayTestConstants.SAMPLE_ACCOUNT)
        val wallet = mainViewRepository.getWallet()
        callResult.onValue { accountResources ->
            _walletUiState.update { state ->
                WalletUiState.Loaded(
                    wallet = wallet,
                    resources = listOf(accountResources)
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.emit(true)
            loadResourceData()
            _isRefreshing.emit(false)
        }
    }

    fun onCopyAccountAddress(hashValue: String) {
        val clipData = ClipData.newPlainText("accountHash", hashValue)
        clipboardManager.setPrimaryClip(clipData)
    }
}

sealed interface WalletUiState {
    object Loading : WalletUiState

    data class Loaded(
        val wallet: WalletData,
        val resources: List<AccountResources> = emptyList()
    ) : WalletUiState
}

data class WalletData(
    val currency: String,
    val amount: String
)
