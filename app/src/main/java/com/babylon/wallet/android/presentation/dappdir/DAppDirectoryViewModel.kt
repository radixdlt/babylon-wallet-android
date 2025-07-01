package com.babylon.wallet.android.presentation.dappdir

import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class DAppDirectoryViewModel @Inject constructor() : StateViewModel<DAppDirectoryViewModel.State>() {

    override fun initialState(): State = State()

    fun onTabSelected(tab: State.Tab) {
        _state.update { state -> state.copy(selectedTab = tab) }
    }

    data class State(
        val selectedTab: Tab = Tab.All
    ) : UiState {

        enum class Tab {

            All,
            Connected
        }
    }
}
