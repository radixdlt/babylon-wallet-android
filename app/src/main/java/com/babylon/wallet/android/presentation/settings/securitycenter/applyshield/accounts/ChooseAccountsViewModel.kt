package com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.accounts

import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ChooseAccountsViewModel @Inject constructor() : StateViewModel<ChooseAccountsViewModel.State>() {

    override fun initialState(): State = State()

    data class State(
        val isLoading: Boolean = true
    ) : UiState
}
