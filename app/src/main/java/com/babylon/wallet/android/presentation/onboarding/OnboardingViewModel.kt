package com.babylon.wallet.android.presentation.onboarding

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.domain.backup.DiscardRestoredProfileFromBackupUseCase
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
//    private val deviceSecurityHelper: DeviceSecurityHelper,
    private val discardRestoredProfileFromBackupUseCase: DiscardRestoredProfileFromBackupUseCase
) : StateViewModel<OnboardingViewModel.OnBoardingUiState>(),
    OneOffEventHandler<OnboardingViewModel.OnBoardingEvent> by OneOffEventHandlerImpl() {

    override fun initialState(): OnBoardingUiState = OnBoardingUiState()

    fun onProceedClick() {
        viewModelScope.launch {
//            if (deviceSecurityHelper.isDeviceSecure()) {
//                _state.update {
//                    it.copy(authenticateWithBiometric = true)
//                }
//            } else {
//                _state.update {
//                    it.copy(showWarning = true)
//                }
//            }
            discardRestoredProfileFromBackupUseCase()
            sendEvent(OnBoardingEvent.EndOnBoarding)
        }
    }

    fun onAlertClicked(accepted: Boolean) {
        if (!accepted) {
            _state.update {
                it.copy(showWarning = false)
            }
        }
    }

    fun onUserAuthenticated(authenticated: Boolean) {
        if (!authenticated) {
            _state.update {
                it.copy(authenticateWithBiometric = false)
            }
        }
    }

    data class OnBoardingUiState(
        val currentPagerPage: Int = 0,
        val showButtons: Boolean = false,
        val authenticateWithBiometric: Boolean = false,
        val showWarning: Boolean = false
    ) : UiState

    sealed interface OnBoardingEvent : OneOffEvent {
        object EndOnBoarding : OnBoardingEvent
    }
}
