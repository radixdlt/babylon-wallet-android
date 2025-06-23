package com.babylon.wallet.android.presentation.discover.learn

import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class LearnViewModel @Inject constructor(
    private val glossaryItemsProvider: GlossaryItemsProvider
) : StateViewModel<LearnViewModel.State>() {

    override fun initialState(): State = State(
        items = GlossaryItemsProvider.searchableGlossaryItems
    )

    fun onSearchQueryChange(value: String) {
        _state.update { state ->
            state.copy(
                searchQuery = value,
                items = glossaryItemsProvider.search(value)
            )
        }
    }

    data class State(
        val items: List<GlossaryItem>,
        val searchQuery: String = ""
    ) : UiState {

        val isSearchQueryEmpty = searchQuery.isEmpty()
    }
}
