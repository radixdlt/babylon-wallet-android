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
import rdx.works.profile.domain.backup.IsProfileFromBackupExistsUseCase
import rdx.works.profile.domain.backup.RestoreProfileFromBackupUseCase
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
//    private val deviceSecurityHelper: DeviceSecurityHelper,
    private val isProfileFromBackupExistsUseCase: IsProfileFromBackupExistsUseCase,
    private val restoreProfileFromBackupUseCase: RestoreProfileFromBackupUseCase,
    private val discardRestoredProfileFromBackupUseCase: DiscardRestoredProfileFromBackupUseCase
) : StateViewModel<OnboardingViewModel.OnBoardingUiState>(),
    OneOffEventHandler<OnboardingViewModel.OnBoardingEvent> by OneOffEventHandlerImpl() {

    override fun initialState(): OnBoardingUiState = OnBoardingUiState()

    init {
        viewModelScope.launch {
            val profileFromBackupExists = isProfileFromBackupExistsUseCase()
            _state.update { it.copy(profileFromBackupExists = profileFromBackupExists) }
        }
    }

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

    fun onRestoreProfileFromBackupClicked() = viewModelScope.launch {
        restoreProfileFromBackupUseCase()
        sendEvent(OnBoardingEvent.EndOnBoarding)
    }

    data class OnBoardingUiState(
        val currentPagerPage: Int = 0,
        val showButtons: Boolean = false,
        val authenticateWithBiometric: Boolean = false,
        val showWarning: Boolean = false,
        val profileFromBackupExists: Boolean = false
    ) : UiState

    sealed interface OnBoardingEvent : OneOffEvent {
        object EndOnBoarding : OnBoardingEvent
    }
}
