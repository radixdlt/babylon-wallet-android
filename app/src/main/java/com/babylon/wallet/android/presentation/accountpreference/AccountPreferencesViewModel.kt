package com.babylon.wallet.android.presentation.accountpreference

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
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

    private val _state = MutableStateFlow(AccountPreferenceUiState())
    internal val state: StateFlow<AccountPreferenceUiState> = _state

    init {
        viewModelScope.launch {
            _state.update {
                it.copy(isDeviceSecure = deviceSecurityHelper.isDeviceSecure())
            }
            getFreeXrdUseCase.isAllowedToUseFaucet(args.address).collect { isAllowed ->
                _state.update {
                    it.copy(
                        canUseFaucet = isAllowed,
                        isDeviceSecure = deviceSecurityHelper.isDeviceSecure()
                    )
                }
            }
        }
    }

    fun onGetFreeXrdClick() {
        appScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = getFreeXrdUseCase(true, args.address)
            result.onValue { _ ->
                _state.update { it.copy(isLoading = false, gotFreeXrd = true) }
                appEventBus.sendEvent(AppEvent.GotFreeXrd)
            }
            result.onError { error ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = UiMessage.ErrorMessage(error = error)
                    )
                }
            }
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(error = null) }
    }
}

internal data class AccountPreferenceUiState(
    val canUseFaucet: Boolean = false,
    val isLoading: Boolean = false,
    val isDeviceSecure: Boolean = false,
    val gotFreeXrd: Boolean = false,
    val error: UiMessage? = null,
)
