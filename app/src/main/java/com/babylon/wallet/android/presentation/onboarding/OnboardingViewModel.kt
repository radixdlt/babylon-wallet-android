package com.babylon.wallet.android.presentation.onboarding

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.PreferencesManager
import com.babylon.wallet.android.presentation.common.BaseViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.DeviceSecurityHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val deviceSecurityHelper: DeviceSecurityHelper,
) : BaseViewModel<OnboardingViewModel.OnboardingUiState>() {

    override fun initialState(): OnboardingUiState = OnboardingUiState()

    fun onProceedClick() {
        viewModelScope.launch {
            if (deviceSecurityHelper.isDeviceSecure()) {
                _state.update {
                    it.copy(authenticateWithBiometric = true)
                }
            } else {
                _state.update {
                    it.copy(showWarning = true)
                }
            }
        }
    }

    fun onAlertClicked(accepted: Boolean) {
        if (accepted) {
            goNext()
        } else {
            _state.update {
                it.copy(showWarning = false)
            }
        }
    }

    fun onUserAuthenticated(authenticated: Boolean) {
        if (authenticated) {
            goNext()
        } else {
            _state.update {
                it.copy(authenticateWithBiometric = false)
            }
        }
    }

    private fun goNext() {
        viewModelScope.launch {
            preferencesManager.setShowOnboarding(false)
        }
    }

    data class OnboardingUiState(
        val currentPagerPage: Int = 0,
        val showButtons: Boolean = false,
        val authenticateWithBiometric: Boolean = false,
        val showWarning: Boolean = false
    ) : UiState
}
