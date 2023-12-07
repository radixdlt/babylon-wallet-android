package com.babylon.wallet.android.presentation.onboarding.restore.withoutbackup

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
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

    fun onShowLedgerOrOlympiaPrompt(visible: Boolean) {
        _state.update { it.copy(showLedgerOrOlympiaPrompt = visible) }
    }

    data class State(
        val showLedgerOrOlympiaPrompt: Boolean = false,
        val uiMessage: UiMessage? = null
    ) : UiState

    sealed interface Event : OneOffEvent {
        data object OnDismiss : Event
    }
}
