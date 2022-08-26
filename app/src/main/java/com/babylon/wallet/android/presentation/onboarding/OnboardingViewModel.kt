package com.babylon.wallet.android.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.DataStoreManager
import com.babylon.wallet.android.utils.SecurityHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesManager: DataStoreManager,
    private val securityHelper: SecurityHelper,
) : ViewModel() {

    private val _onboardingUiAction: MutableSharedFlow<OnboardingUiAction> = MutableSharedFlow()
    val onboardingUiAction = _onboardingUiAction.asSharedFlow()

    fun onProceedClick() {
        viewModelScope.launch {
            if (securityHelper.isDeviceSecure()) {
                _onboardingUiAction.emit(OnboardingUiAction.AuthenticateWithBiometric)
            } else {
                _onboardingUiAction.emit(OnboardingUiAction.ShowSecurityWarning)
            }
        }
    }

    fun onUserAuthenticated() {
        viewModelScope.launch {
            preferencesManager.setShowOnboarding(false)
        }
    }

    sealed interface OnboardingUiAction {
        object AuthenticateWithBiometric : OnboardingUiAction
        object ShowSecurityWarning : OnboardingUiAction
    }
}
