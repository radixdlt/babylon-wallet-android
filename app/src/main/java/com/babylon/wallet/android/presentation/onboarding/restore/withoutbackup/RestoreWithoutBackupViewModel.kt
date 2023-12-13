package com.babylon.wallet.android.presentation.onboarding.restore.withoutbackup

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RestoreWithoutBackupViewModel @Inject constructor() :
    StateViewModel<RestoreWithoutBackupViewModel.State>(),
    OneOffEventHandler<RestoreWithoutBackupViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State = State()

    fun onBackClick() {
        viewModelScope.launch { sendEvent(Event.OnDismiss) }
    }

    fun onShowLedgerPrompt() {
        _state.update { it.copy(dialogPrompt = State.PromptState.Ledger) }
    }

    fun onShowOlympiaPrompt() {
        _state.update { it.copy(dialogPrompt = State.PromptState.Olympia) }
    }

    fun onDismissPrompt() {
        _state.update { it.copy(dialogPrompt = State.PromptState.None) }
    }

    data class State(
        val dialogPrompt: PromptState = PromptState.None
    ) : UiState {
        enum class PromptState {
            None, Olympia, Ledger
        }
    }

    sealed interface Event : OneOffEvent {
        data object OnDismiss : Event
    }
}
