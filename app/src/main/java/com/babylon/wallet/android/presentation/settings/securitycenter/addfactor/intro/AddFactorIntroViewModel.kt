package com.babylon.wallet.android.presentation.settings.securitycenter.addfactor.intro

import androidx.lifecycle.SavedStateHandle
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.settings.securitycenter.addfactor.AddFactorArgs
import com.radixdlt.sargon.FactorSourceKind
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AddFactorIntroViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : StateViewModel<AddFactorIntroViewModel.State>() {

    private val args = AddFactorArgs(savedStateHandle)

    override fun initialState(): State = State(
        factorSourceKind = args.kind
    )

    data class State(
        val factorSourceKind: FactorSourceKind
    ) : UiState
}
