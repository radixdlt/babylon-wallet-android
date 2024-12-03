package com.babylon.wallet.android.presentation.settings.debug.factors

import com.babylon.wallet.android.domain.model.factors.FactorSourceCard
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SecurityFactorSamplesViewModel @Inject constructor() : StateViewModel<SecurityFactorSamplesViewModel.State>() {

    override fun initialState(): State = State()

    data class State(
        val items: List<FactorSourceCard> = emptyList()
    ) : UiState
}