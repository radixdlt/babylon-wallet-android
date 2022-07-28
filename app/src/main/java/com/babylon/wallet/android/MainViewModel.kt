package com.babylon.wallet.android

import android.content.ClipData
import android.content.ClipboardManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val mainViewRepository: MainViewRepository,
    private val clipboardManager: ClipboardManager
) : ViewModel() {

    private val _walletUiState = MutableStateFlow<WalletUiState>(WalletUiState.Loading)
    val walletUiState: StateFlow<WalletUiState>
        get() = _walletUiState.asStateFlow()

    private val _accountUiState = MutableStateFlow<AccountUiState>(AccountUiState.Loading)
    val accountUiState: StateFlow<AccountUiState>
        get() = _accountUiState.asStateFlow()

    init {
        viewModelScope.launch {
            mainViewRepository.getWalletData().collect {
                _walletUiState.value = WalletUiState.Loaded(it)
            }
        }
        viewModelScope.launch {
            mainViewRepository.getAccountData().collect {
                _accountUiState.value = AccountUiState.Loaded(it)
            }
        }
    }

    fun onCopy(hashValue: String) {
        val clipData = ClipData.newPlainText("accountHash", hashValue)
        clipboardManager.setPrimaryClip(clipData)
    }
}

data class WalletData(
    val currency: String,
    val amount: String
)

data class AccountData(
    val accountName: String,
    val accountHash: String,
    val accountValue: String,
    val accountCurrency: String
)

sealed class WalletUiState {
    object Loading : WalletUiState()
    class Loaded(val walletData: WalletData) : WalletUiState()
}

sealed class AccountUiState {
    object Loading : AccountUiState()
    class Loaded(val accountData: AccountData) : AccountUiState()
}
