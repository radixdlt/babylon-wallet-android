package com.babylon.wallet.android.presentation.account

import android.content.ClipData
import android.content.ClipboardManager
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.R
import com.babylon.wallet.android.domain.model.AccountResources
import com.babylon.wallet.android.domain.onValue
import com.babylon.wallet.android.domain.usecase.wallet.RequestAccountResourcesUseCase
import com.babylon.wallet.android.presentation.navigation.Screen.Companion.ARG_ACCOUNT_ID
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val requestAccountResourcesUseCase: RequestAccountResourcesUseCase,
    private val clipboardManager: ClipboardManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val accountId: String = savedStateHandle.get<String>(ARG_ACCOUNT_ID).orEmpty()

    private val _accountUiState: MutableStateFlow<AccountUiState> = MutableStateFlow(AccountUiState.Loading)
    val accountUiState = _accountUiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(true)
    val isRefreshing = _isRefreshing.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            if (accountId.isNotEmpty()) {
                _isRefreshing.value = true
                // TODO how to handle the case when the gateway doesn't return the account?
                // TODO this should probably change to flow later
                val account = requestAccountResourcesUseCase.getAccountResources(accountId)
                account.onValue { accountResource ->
                    _accountUiState.update {
                        AccountUiState.Loaded(accountResource)
                    }
                }
                _isRefreshing.value = false
            } else {
                Timber.d("arg account id is empty")
            }
        }
    }

    fun onCopyAccountAddress(hash: String) {
        val clipData = ClipData.newPlainText("accountHash", hash)
        clipboardManager.setPrimaryClip(clipData)
    }
}

sealed interface AccountUiState {
    object Loading : AccountUiState

    data class Loaded(
        val account: AccountResources
    ) : AccountUiState
}

enum class AssetTypeTab(@StringRes val stringId: Int) {
    TOKEN_TAB(R.string.account_asset_row_tab_tokens),
    NTF_TAB(R.string.account_asset_row_tab_nfts),
}
