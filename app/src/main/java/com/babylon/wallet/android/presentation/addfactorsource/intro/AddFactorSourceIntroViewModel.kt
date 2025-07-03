package com.babylon.wallet.android.presentation.addfactorsource.intro

import com.babylon.wallet.android.presentation.addfactorsource.AddFactorSourceIOHandler
import com.babylon.wallet.android.presentation.addfactorsource.AddFactorSourceInput
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.FactorSourceKind
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AddFactorSourceIntroViewModel @Inject constructor(
    addFactorSourceIOHandler: AddFactorSourceIOHandler
) : StateViewModel<AddFactorSourceIntroViewModel.State>() {

    private val input = addFactorSourceIOHandler.getInput() as AddFactorSourceInput.WithKind

    override fun initialState(): State = State(
        factorSourceKind = checkNotNull(input.kind)
    )

    data class State(
        val factorSourceKind: FactorSourceKind
    ) : UiState
}
