package com.babylon.wallet.android.presentation.wallet

import android.content.ClipData
import android.content.ClipboardManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.MainViewRepository
import com.babylon.wallet.android.presentation.model.AccountUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private const val UPSTREAM_FLOW_ACTIVE_PERIOD = 5_000L

@HiltViewModel
class WalletViewModel @Inject constructor(
    mainViewRepository: MainViewRepository,
    private val clipboardManager: ClipboardManager
) : ViewModel() {

    val walletUiState: StateFlow<WalletUiState> = mainViewRepository
        .getWallet()
        .map {
            WalletUiState.Loaded(it)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(UPSTREAM_FLOW_ACTIVE_PERIOD),
            initialValue = WalletUiState.Loading
        )

    val accountUiState: StateFlow<AccountsUiState> = mainViewRepository
        .getAccounts()
        .map {
            AccountsUiState.Loaded(it)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(UPSTREAM_FLOW_ACTIVE_PERIOD),
            initialValue = AccountsUiState.Loading
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

sealed class WalletUiState {
    object Loading : WalletUiState()
    data class Loaded(val walletData: WalletData) : WalletUiState()
}

sealed class AccountsUiState {
    object Loading : AccountsUiState()
    data class Loaded(val accounts: List<AccountUi>) : AccountsUiState()
}
