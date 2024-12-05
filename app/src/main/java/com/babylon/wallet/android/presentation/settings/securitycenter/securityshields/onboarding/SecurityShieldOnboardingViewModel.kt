package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.onboarding

import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SecurityShieldOnboardingViewModel @Inject constructor() : StateViewModel<SecurityShieldOnboardingViewModel.State>() {

    override fun initialState(): State = State()

    data class State(
        val isLoading: Boolean = false
    ) : UiState
}