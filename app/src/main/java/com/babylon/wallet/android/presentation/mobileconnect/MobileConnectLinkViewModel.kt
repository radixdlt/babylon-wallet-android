package com.babylon.wallet.android.presentation.mobileconnect

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.di.coroutines.ApplicationScope
import com.babylon.wallet.android.domain.model.IncomingMessage
import com.babylon.wallet.android.domain.usecases.GetDAppsUseCase
import com.babylon.wallet.android.domain.usecases.RespondToIncomingRequestUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.DappWalletInteractionErrorType
import com.radixdlt.sargon.extensions.init
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.domain.DApp
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class MobileConnectLinkViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getDAppsUseCase: GetDAppsUseCase,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val respondToIncomingRequestUseCase: RespondToIncomingRequestUseCase,
    private val appEventBus: AppEventBus,
    @ApplicationScope private val appScope: CoroutineScope
) : StateViewModel<MobileConnectLinkViewModel.State>(), OneOffEventHandler<MobileConnectLinkViewModel.Event> by OneOffEventHandlerImpl() {

    private val args = MobileConnectArgs(savedStateHandle)

    private lateinit var request: IncomingMessage.IncomingRequest

    override fun initialState(): State {
        return State()
    }

    init {
        observeDeferEvent()
        viewModelScope.launch {
            val requestToHandle = incomingRequestRepository.getRequest(args.interactionId)
            if (requestToHandle == null) {
                sendEvent(Event.Close)
                return@launch
            } else {
                request = requestToHandle
            }
            runCatching { AccountAddress.init(request.metadata.dAppDefinitionAddress) }.mapCatching { address ->
                getDAppsUseCase(definitionAddress = address, needMostRecentData = false).getOrThrow()
            }.onSuccess { dApp ->
                _state.update { it.copy(dApp = dApp, isLoading = false) }
            }.onFailure {
                _state.update { state -> state.copy(isLoading = false) }
            }
        }
    }

    private fun observeDeferEvent() {
        viewModelScope.launch {
            appEventBus.events.filterIsInstance<AppEvent.DeferRequestHandling>().collect {
                if (it.interactionId == args.interactionId) {
                    sendEvent(Event.Close)
                    incomingRequestRepository.requestDeferred(args.interactionId)
                }
            }
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    fun onVerifyOrigin() = viewModelScope.launch {
        _state.update {
            it.copy(isVerifying = true)
        }
        viewModelScope.launch {
            sendEvent(Event.HandleRequest(request))
        }
    }

    fun onDenyOrigin() = viewModelScope.launch {
        _state.update {
            it.copy(isVerifying = true)
        }
        appScope.launch {
            respondToIncomingRequestUseCase.respondWithFailure(request, DappWalletInteractionErrorType.REJECTED_BY_USER)
        }
        viewModelScope.launch {
            incomingRequestRepository.requestHandled(args.interactionId)
            sendEvent(Event.Close)
        }
    }

    sealed class Event : OneOffEvent {
        data object Close : Event()
        data class HandleRequest(val request: IncomingMessage.IncomingRequest) : Event()
    }

    data class State(
        val dApp: DApp? = null,
        val uiMessage: UiMessage? = null,
        val isLoading: Boolean = true,
        val isVerifying: Boolean = false
    ) : UiState
}
