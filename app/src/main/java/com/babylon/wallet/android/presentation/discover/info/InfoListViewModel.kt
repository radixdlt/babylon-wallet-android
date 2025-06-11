package com.babylon.wallet.android.presentation.discover.info

import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class InfoListViewModel @Inject constructor() : StateViewModel<InfoListViewModel.State>() {

    override fun initialState(): State = State()

    data class State(
        val items: List<GlossaryItem> = GlossaryItem.entries
            .filterNot { it in GlossaryItem.mfaRelated }
    ) : UiState
}