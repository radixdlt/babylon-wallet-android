package com.babylon.wallet.android.presentation.wallet

import android.content.ClipData
import android.content.ClipboardManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.MainViewRepository
import com.babylon.wallet.android.presentation.model.AccountUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val mainViewRepository: MainViewRepository,
    private val clipboardManager: ClipboardManager
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(true)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _walletUiState: MutableStateFlow<WalletUiState> = MutableStateFlow(WalletUiState.Loading)
    val walletUiState = _walletUiState.asStateFlow()

    init {
        refresh(fromUser = false)
    }

    fun refresh(fromUser: Boolean = true) {
        viewModelScope.launch {
            _isRefreshing.emit(fromUser)
            val wallet = mainViewRepository.getWallet()
            val accounts = mainViewRepository.getAccounts()
            _walletUiState.emit(
                WalletUiState.Loaded(
                    wallet = wallet,
                    accounts = accounts
                )
            )
            if (fromUser) _isRefreshing.emit(false)
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
        val accounts: List<AccountUi>
    ) : WalletUiState
}

data class WalletData(
    val currency: String,
    val amount: String
)
