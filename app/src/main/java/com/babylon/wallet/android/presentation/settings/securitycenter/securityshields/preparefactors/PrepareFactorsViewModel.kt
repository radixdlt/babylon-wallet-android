package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.preparefactors

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.callSafely
import com.radixdlt.sargon.SecurityShieldPrerequisitesStatus
import com.radixdlt.sargon.os.SargonOsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrepareFactorsViewModel @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher
) : StateViewModel<PrepareFactorsViewModel.State>(),
    OneOffEventHandler<PrepareFactorsViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State = State()

    fun onButtonClick() {
        viewModelScope.launch {
            sargonOsManager.callSafely(dispatcher) {
                securityShieldPrerequisitesStatus()
            }.onSuccess { status ->
                when (status) {
                    SecurityShieldPrerequisitesStatus.SUFFICIENT -> error("Should not be here")
                    SecurityShieldPrerequisitesStatus.HARDWARE_REQUIRED -> sendEvent(Event.AddHardwareDevice)
                    SecurityShieldPrerequisitesStatus.ANY_REQUIRED -> sendEvent(Event.AddAnotherFactor)
                }
            }.onFailure {
                _state.update { state -> state.copy(message = UiMessage.ErrorMessage(it)) }
            }
        }
    }

    fun onMessageShown() {
        _state.update { state -> state.copy(message = null) }
    }

    data class State(
        val message: UiMessage? = null
    ) : UiState

    sealed interface Event : OneOffEvent {

        data object AddHardwareDevice : Event

        data object AddAnotherFactor : Event
    }
}
