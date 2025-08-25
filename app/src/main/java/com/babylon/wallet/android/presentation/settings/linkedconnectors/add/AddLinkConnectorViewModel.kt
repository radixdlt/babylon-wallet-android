package com.babylon.wallet.android.presentation.settings.linkedconnectors.add

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.p2plink.LinkConnectionPayload
import com.babylon.wallet.android.domain.usecases.p2plink.EstablishP2PLinkConnectionUseCase
import com.babylon.wallet.android.domain.usecases.p2plink.ParseLinkConnectionDetailsUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.logNonFatalException
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AddLinkConnectorViewModel @Inject constructor(
    private val parseLinkConnectionDetailsUseCase: ParseLinkConnectionDetailsUseCase,
    private val establishP2PLinkConnectionUseCase: EstablishP2PLinkConnectionUseCase,
    private val appEventBus: AppEventBus
) : StateViewModel<AddLinkConnectorUiState>(),
    OneOffEventHandler<AddLinkConnectorViewModel.Event> by OneOffEventHandlerImpl() {

    private var linkConnectionPayload: LinkConnectionPayload? = null

    override fun initialState() = AddLinkConnectorUiState.init

    fun onQrCodeScanned(content: String) {
        viewModelScope.launch {
            parseLinkConnectionDetailsUseCase(content)
                .onSuccess { payload ->
                    linkConnectionPayload = payload
                    _state.update { state ->
                        state.copy(
                            content = if (payload.existingP2PLink == null) {
                                AddLinkConnectorUiState.Content.ApproveNewLinkConnector
                            } else {
                                AddLinkConnectorUiState.Content.UpdateLinkConnector
                            }
                        )
                    }
                }
                .onFailure { exception ->
                    _state.update { state ->
                        val uiMessage = UiMessage.ErrorMessage(exception)

                        state.copy(
                            content = AddLinkConnectorUiState.Content.ScanQrCode(false),
                            error = when (exception) {
                                is RadixWalletException.LinkConnectionException.InvalidQR,
                                is RadixWalletException.LinkConnectionException.InvalidSignature -> {
                                    AddLinkConnectorUiState.Error.InvalidQR(uiMessage)
                                }
                                else -> {
                                    AddLinkConnectorUiState.Error.Other(uiMessage)
                                }
                            }
                        )
                    }
                }
        }
    }

    fun onQrCodeScanFailure(error: Throwable) {
        logNonFatalException(error)
        _state.update { state ->
            state.copy(
                error = AddLinkConnectorUiState.Error.Other(UiMessage.ErrorMessage(error))
            )
        }
    }

    fun onConnectorDisplayNameChanged(name: String) {
        val content = state.value.content as? AddLinkConnectorUiState.Content.NameLinkConnector ?: return

        _state.update {
            it.copy(
                content = content.copy(
                    connectorDisplayName = name,
                    isContinueButtonEnabled = name.trim().isNotEmpty()
                )
            )
        }
    }

    fun onContinueClick() {
        val payload = linkConnectionPayload ?: return

        when (val content = state.value.content) {
            is AddLinkConnectorUiState.Content.ApproveNewLinkConnector -> {
                _state.update { state ->
                    state.copy(
                        content = AddLinkConnectorUiState.Content.NameLinkConnector(
                            isContinueButtonEnabled = false,
                            connectorDisplayName = ""
                        )
                    )
                }
            }
            is AddLinkConnectorUiState.Content.NameLinkConnector -> {
                establishLinkConnection {
                    establishP2PLinkConnectionUseCase.add(payload, content.connectorDisplayName)
                }
            }
            is AddLinkConnectorUiState.Content.UpdateLinkConnector -> {
                establishLinkConnection {
                    establishP2PLinkConnectionUseCase.update(payload)
                }
            }
            else -> {
                Timber.e("This shouldn't happen. Invalid UI state: $state")
            }
        }
    }

    fun onCloseClick() {
        exitLinking(false)
    }

    fun onErrorDismiss() {
        exitLinking(false)
    }

    private fun establishLinkConnection(operation: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _state.update { it.copy(isAddingNewLinkConnectorInProgress = true) }

            operation().onFailure { throwable ->
                _state.update {
                    it.copy(
                        error = AddLinkConnectorUiState.Error.Other(
                            message = UiMessage.ErrorMessage(throwable)
                        )
                    )
                }
                exitLinking(false)
            }.onSuccess {
                appEventBus.sendEvent(AppEvent.ConnectorLinked)
                exitLinking(true)
            }
        }
    }

    private fun exitLinking(isLinkedConnectorAdded: Boolean) {
        linkConnectionPayload = null
        _state.value = AddLinkConnectorUiState.init

        viewModelScope.launch {
            sendEvent(
                Event.Close(
                    isLinkedConnectorAdded = isLinkedConnectorAdded
                )
            )
        }
    }

    sealed interface Event : OneOffEvent {

        data class Close(
            val isLinkedConnectorAdded: Boolean
        ) : Event
    }
}

data class AddLinkConnectorUiState(
    val isAddingNewLinkConnectorInProgress: Boolean,
    val content: Content,
    val error: Error?
) : UiState {

    sealed interface Content {

        data class ScanQrCode(
            val isCameraOn: Boolean
        ) : Content

        data object ApproveNewLinkConnector : Content

        data object UpdateLinkConnector : Content

        data class NameLinkConnector(
            val isContinueButtonEnabled: Boolean,
            val connectorDisplayName: String
        ) : Content
    }

    sealed class Error(
        open val message: UiMessage
    ) {

        data class InvalidQR(
            override val message: UiMessage
        ) : Error(message)

        data class Other(
            override val message: UiMessage.ErrorMessage
        ) : Error(message)
    }

    companion object {
        val init = AddLinkConnectorUiState(
            isAddingNewLinkConnectorInProgress = false,
            content = Content.ScanQrCode(true),
            error = null
        )
    }
}
