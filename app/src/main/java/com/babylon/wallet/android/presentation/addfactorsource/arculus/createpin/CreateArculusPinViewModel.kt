package com.babylon.wallet.android.presentation.addfactorsource.arculus.createpin

import com.babylon.wallet.android.presentation.addfactorsource.AddFactorSourceIOHandler
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CreateArculusPinViewModel @Inject constructor(
    private val addFactorSourceIOHandler: AddFactorSourceIOHandler
) : StateViewModel<CreateArculusPinViewModel.State>() {

    override fun initialState(): State = State(isLoading = true)

    data class State(
        val isLoading: Boolean
    ) : UiState
}