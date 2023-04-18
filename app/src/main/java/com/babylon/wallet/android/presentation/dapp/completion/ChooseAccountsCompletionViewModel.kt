package com.babylon.wallet.android.presentation.dapp.completion

import androidx.lifecycle.SavedStateHandle
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ChooseAccountsCompletionViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle
) : StateViewModel<ChooseAccountsCompletionViewModel.ChooseAccountsCompletionState>() {

    override fun initialState(): ChooseAccountsCompletionState = ChooseAccountsCompletionState(
        dAppName = savedStateHandle.get<String>(Screen.ARG_DAPP_NAME).orEmpty()
    )

    data class ChooseAccountsCompletionState(
        val dAppName: String
    ) : UiState
}
