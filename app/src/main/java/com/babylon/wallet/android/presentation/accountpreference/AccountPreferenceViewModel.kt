package com.babylon.wallet.android.presentation.accountpreference

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.common.onError
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.transaction.TransactionClient
import com.babylon.wallet.android.utils.DeviceSecurityHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountPreferenceViewModel @Inject constructor(
    private val transactionClient: TransactionClient,
    private val deviceSecurityHelper: DeviceSecurityHelper,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val args = AccountPreferenceArgs(savedStateHandle)

    internal var state by mutableStateOf(AccountPreferenceUiState())
        private set

    init {
        viewModelScope.launch {
            state = state.copy(
                canUseFaucet = transactionClient.isAllowedToUseFaucet(args.address),
                isDeviceSecure = deviceSecurityHelper.isDeviceSecure()
            )
        }
    }

    fun onGetFreeXrdClick() {
        viewModelScope.launch {
            state = state.copy(isLoading = true)
            val result = transactionClient.getFreeXrd(true, args.address)
            result.onValue {
                state = state.copy(isLoading = false, gotFreeXrd = true)
            }
            result.onError {
                state = state.copy(isLoading = false)
            }
        }
    }
}

internal data class AccountPreferenceUiState(
    val canUseFaucet: Boolean = false,
    val isLoading: Boolean = false,
    val isDeviceSecure: Boolean = false,
    val gotFreeXrd: Boolean = false
)
