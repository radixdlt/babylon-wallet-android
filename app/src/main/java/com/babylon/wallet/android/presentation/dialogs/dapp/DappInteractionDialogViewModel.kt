package com.babylon.wallet.android.presentation.dialogs.dapp

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

    init {
        viewModelScope.launch {
            incomingRequestRepository.requestHandled(state.value.requestId)
        }
    }

    override fun initialState(): State = with(DappInteractionSuccessDialogArgs(savedStateHandle)) {
        State(
            requestId = requestId,
            dAppName = dAppName,
            isMobileConnect = mobileConnect
        )
    }

    fun onDismiss() {
        viewModelScope.launch {
            sendEvent(Event.DismissDialog)
        }
    }

    data class State(
        val requestId: String,
        val dAppName: String,
        val isMobileConnect: Boolean
    ) : UiState

    sealed interface Event : OneOffEvent {
        data object DismissDialog : Event
    }
}
