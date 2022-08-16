package com.babylon.wallet.android.presentation.account

import android.content.ClipData
import android.content.ClipboardManager
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.R
import com.babylon.wallet.android.domain.MainViewRepository
import com.babylon.wallet.android.presentation.model.AccountUi
import com.babylon.wallet.android.presentation.navigation.Screen.Companion.ARG_ACCOUNT_ID
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val mainViewRepository: MainViewRepository,
    private val clipboardManager: ClipboardManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val accountId: String = savedStateHandle.get<String>(ARG_ACCOUNT_ID).orEmpty()

    private val _accountUiState: MutableStateFlow<AccountUiState> = MutableStateFlow(AccountUiState.Loading)
    val accountUiState = _accountUiState.asStateFlow()

    // Holds our currently selected asset type tab
    private val _selectedAssetTypeTab = MutableStateFlow(AssetTypeTab.TOKEN_TAB)
    val selectedAssetTypeTab = _selectedAssetTypeTab.asStateFlow()

    init {
        viewModelScope.launch {
            if (accountId.isNotEmpty()) {
                // TODO how to handle the case when the gateway doesn't return the account?
                val account = mainViewRepository.getAccountBasedOnId(accountId)
                _accountUiState.value = AccountUiState.Loaded(account)
            } else {
                Log.d("AccountViewModel", "arg account id is empty")
            }
        }
    }

    fun onCopyAccountAddress(hash: String) {
        val clipData = ClipData.newPlainText("accountHash", hash)
        clipboardManager.setPrimaryClip(clipData)
    }

    fun onAssetTypeTabSelected(assetType: AssetTypeTab) {
        _selectedAssetTypeTab.value = assetType
    }
}

sealed class AccountUiState {
    object Loading : AccountUiState()
    data class Loaded(val account: AccountUi) : AccountUiState()
}

enum class AssetTypeTab(@StringRes val stringId: Int) {
    TOKEN_TAB(R.string.account_asset_row_tab_tokens),
    NTF_TAB(R.string.account_asset_row_tab_nfts),
}
