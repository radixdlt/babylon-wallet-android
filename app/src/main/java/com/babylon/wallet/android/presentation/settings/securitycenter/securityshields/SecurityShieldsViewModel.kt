package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields

import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SecurityShieldsViewModel @Inject constructor() : StateViewModel<SecurityShieldsViewModel.State>() {

    override fun initialState(): State = State()

    data class State(
        val isLoading: Boolean = false
    ) : UiState
}