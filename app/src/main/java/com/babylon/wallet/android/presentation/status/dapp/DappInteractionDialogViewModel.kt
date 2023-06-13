package com.babylon.wallet.android.presentation.status.dapp

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DappInteractionDialogViewModel @Inject constructor(
    private val incomingRequestRepository: IncomingRequestRepository,
    private val savedStateHandle: SavedStateHandle
) : StateViewModel<DappInteractionDialogViewModel.State>(),
    OneOffEventHandler<DappInteractionDialogViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State = with(DappInteractionSuccessDialogArgs(savedStateHandle)) {
        State(
            requestId = requestId,
            dAppName = dAppName
        )
    }

    fun onDismiss() {
        viewModelScope.launch {
            incomingRequestRepository.requestHandled(state.value.requestId)
            sendEvent(Event.DismissDialog)
        }
    }

    data class State(
        val requestId: String,
        val dAppName: String
    ) : UiState

    sealed interface Event : OneOffEvent {
        object DismissDialog : Event
    }
}
