package com.babylon.wallet.android.presentation.boot

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.DeviceCapabilityHelper
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.errorCodeFromError
import com.radixdlt.sargon.errorMessageFromError
import com.radixdlt.sargon.os.SargonOsManager
import com.radixdlt.sargon.os.SargonOsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SargonBootErrorViewModel @Inject constructor(
    sargonOsManager: SargonOsManager,
    private val deviceCapabilityHelper: DeviceCapabilityHelper
) : StateViewModel<SargonBootErrorViewModel.State>(),
    OneOffEventHandler<SargonBootErrorViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State = State()

    init {
        viewModelScope.launch {
            val bootError = sargonOsManager
                .sargonState
                .filterIsInstance<SargonOsState.BootError>()
                .firstOrNull()
                ?.error

            _state.update { it.copy(bootErrorMessage = UiMessage.ErrorMessage(bootError)) }
        }
    }

    fun onFinishClick() {
        viewModelScope.launch {
            sendEvent(Event.Finish)
        }
    }

    fun onSendLogsClick() {
        viewModelScope.launch {
            val body = StringBuilder()
            body.append(deviceCapabilityHelper.supportEmailTemplate)
            body.appendLine("=========================")
            body.appendLine("Sargon Boot Error")

            val cause = _state.value.bootErrorMessage?.error
            if (cause != null) {
                if (cause is CommonException) {
                    body.appendLine("Error Code: ${errorCodeFromError(cause)}")
                    body.appendLine("Error Message: ${errorMessageFromError(cause)}")
                    body.appendLine()
                }

                body.appendLine(cause.message)
                body.appendLine()
                body.appendLine(cause.stackTraceToString())
            }

            sendEvent(Event.OnSendLogs(body = body.toString()))
        }
    }

    data class State(
        val bootErrorMessage: UiMessage.ErrorMessage? = null
    ) : UiState

    sealed interface Event : OneOffEvent {
        data object Finish : Event
        data class OnSendLogs(val body: String): Event
    }

}