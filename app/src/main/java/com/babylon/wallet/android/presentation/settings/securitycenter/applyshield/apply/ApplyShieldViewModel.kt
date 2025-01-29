package com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.apply

import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ApplyShieldViewModel @Inject constructor() : StateViewModel<ApplyShieldViewModel.State>() {

    override fun initialState(): State = State()

    data class State(
        val isLoading: Boolean = true
    ) : UiState
}
