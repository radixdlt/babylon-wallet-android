package com.babylon.wallet.android.presentation.onboarding

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor() :
    StateViewModel<OnboardingViewModel.OnBoardingUiState>(),
    OneOffEventHandler<OnboardingViewModel.OnBoardingEvent> by OneOffEventHandlerImpl() {

    override fun initialState(): OnBoardingUiState = OnBoardingUiState()

    fun onCreateNewWalletClick() {
        viewModelScope.launch {
            sendEvent(OnBoardingEvent.CreateNewWallet)
        }
    }

    data class OnBoardingUiState(
        val showWarning: Boolean = false
    ) : UiState

    sealed interface OnBoardingEvent : OneOffEvent {
        object CreateNewWallet : OnBoardingEvent
    }
}
