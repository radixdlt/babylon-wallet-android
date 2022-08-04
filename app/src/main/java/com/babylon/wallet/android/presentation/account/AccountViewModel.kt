package com.babylon.wallet.android.presentation.account

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.MainViewRepository
import com.babylon.wallet.android.presentation.model.AccountUi
import com.babylon.wallet.android.presentation.navigation.Screen.Companion.ARG_ACCOUNT_ID
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val mainViewRepository: MainViewRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val accountId: String = Uri.decode(savedStateHandle.get<String>(ARG_ACCOUNT_ID))

    private val _accountUiState: MutableStateFlow<AccountUiState> = MutableStateFlow(AccountUiState.Loading)
    val accountUiState: StateFlow<AccountUiState> = _accountUiState

    init {
        viewModelScope.launch {
            val account = mainViewRepository.getAccountBasedOnId(accountId)
            _accountUiState.value = AccountUiState.Loaded(account)
        }
    }
}

sealed class AccountUiState {
    object Loading : AccountUiState()
    data class Loaded(val account: AccountUi) : AccountUiState()
}
