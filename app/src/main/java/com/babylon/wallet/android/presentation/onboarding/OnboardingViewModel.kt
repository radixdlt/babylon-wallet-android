package com.babylon.wallet.android.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.DataStoreManager
import com.babylon.wallet.android.utils.DeviceSecurityHelper
import com.google.accompanist.pager.ExperimentalPagerApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalPagerApi::class)
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesManager: DataStoreManager,
    private val deviceSecurityHelper: DeviceSecurityHelper,
) : ViewModel() {

    private val _onboardingUiState: MutableStateFlow<OnboardingUiState> = MutableStateFlow(OnboardingUiState())
    val onboardingUiState = _onboardingUiState.asStateFlow()

    private var showButtons: Boolean = false

    fun onPageSelected(currentPageIndex: Int, pageCount: Int) {
        _onboardingUiState.update {
            if (!showButtons) {
                showButtons = currentPageIndex == pageCount - 1
            }
            it.copy(showButtons = showButtons)
        }
    }

    fun onProceedClick() {
        viewModelScope.launch {
            if (deviceSecurityHelper.isDeviceSecure()) {
                _onboardingUiState.update {
                    it.copy(authenticateWithBiometric = true)
                }
            } else {
                _onboardingUiState.update {
                    it.copy(showWarning = true)
                }
            }
        }
    }

    fun onAlertClicked(accepted: Boolean) {
        if (accepted) {
            goNext()
        } else {
            _onboardingUiState.update {
                it.copy(showWarning = false)
            }
        }
    }

    fun onUserAuthenticated(authenticated: Boolean) {
        if (authenticated) {
            goNext()
        } else {
            _onboardingUiState.update {
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
    )
}
