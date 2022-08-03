package com.babylon.wallet.android.presentation.wallet

import android.content.ClipData
import android.content.ClipboardManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.MainViewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class WalletViewModel @Inject constructor(
    mainViewRepository: MainViewRepository,
    private val clipboardManager: ClipboardManager
) : ViewModel() {

    val walletUiState: StateFlow<WalletUiState> = mainViewRepository
        .getWalletData()
        .map {
            WalletUiState.Loaded(it)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = WalletUiState.Loading
        )

    val accountUiState: StateFlow<AccountUiState> = mainViewRepository
        .getAccountData()
        .map {
            AccountUiState.Loaded(it)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AccountUiState.Loading
        )

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
    data class Loaded(val walletData: WalletData) : WalletUiState()
}

sealed class AccountUiState {
    object Loading : AccountUiState()
    data class Loaded(val accountData: AccountData) : AccountUiState()
}
