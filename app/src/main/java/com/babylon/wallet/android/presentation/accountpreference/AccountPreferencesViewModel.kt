package com.babylon.wallet.android.presentation.accountpreference

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.di.coroutines.ApplicationScope
import com.babylon.wallet.android.domain.common.onError
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.usecases.GetFreeXrdUseCase
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.babylon.wallet.android.utils.DeviceSecurityHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountPreferenceViewModel @Inject constructor(
    private val getFreeXrdUseCase: GetFreeXrdUseCase,
    private val deviceSecurityHelper: DeviceSecurityHelper,
    @ApplicationScope private val appScope: CoroutineScope,
    savedStateHandle: SavedStateHandle,
    private val appEventBus: AppEventBus
) : ViewModel() {

    private val args = AccountPreferencesArgs(savedStateHandle)

    internal var state by mutableStateOf(AccountPreferenceUiState())
        private set

    init {
        viewModelScope.launch {
            state = state.copy(
                isDeviceSecure = deviceSecurityHelper.isDeviceSecure()
            )
            getFreeXrdUseCase.isAllowedToUseFaucet(args.address).collect {
                state = state.copy(
                    canUseFaucet = it,
                    isDeviceSecure = deviceSecurityHelper.isDeviceSecure()
                )
            }
        }
    }

    fun onGetFreeXrdClick() {
        appScope.launch {
            state = state.copy(isLoading = true)
            val result = getFreeXrdUseCase(true, args.address)
            result.onValue {
                state = state.copy(isLoading = false, gotFreeXrd = true)
                appEventBus.sendEvent(AppEvent.GotFreeXrd)
            }
            result.onError {
                state = state.copy(isLoading = false, error = UiMessage.ErrorMessage(error = it))
            }
        }
    }

    fun onMessageShown() {
        state = state.copy(error = null)
    }
}

internal data class AccountPreferenceUiState(
    val canUseFaucet: Boolean = false,
    val isLoading: Boolean = false,
    val isDeviceSecure: Boolean = false,
    val gotFreeXrd: Boolean = false,
    val error: UiMessage? = null,
)
