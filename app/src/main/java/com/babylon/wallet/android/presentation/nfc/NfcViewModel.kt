package com.babylon.wallet.android.presentation.nfc

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class NfcViewModel @Inject constructor(
    private val appEventBus: AppEventBus
) : StateViewModel<NfcViewModel.State>(),
    OneOffEventHandler<NfcViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State = State()

    init {
        appEventBus.events
            .filterIsInstance<AppEvent.Nfc>()
            .onEach { event ->
                when (event) {
                    is AppEvent.Nfc.StartSession -> _state.update { it.copy(title = "NFC Session") }
                    is AppEvent.Nfc.SetMessage -> _state.update { it.copy(message = event.message) }
                    is AppEvent.Nfc.EndSession -> sendEvent(Event.Completed)
                }
            }
            .launchIn(viewModelScope)
    }

    fun onDismiss() {
        viewModelScope.launch {
            sendEvent(Event.Completed)
        }
    }

    data class State(
        val title: String = "",
        val message: String = ""
    ) : UiState

    sealed interface Event : OneOffEvent {
        data object Completed : Event
    }
}


