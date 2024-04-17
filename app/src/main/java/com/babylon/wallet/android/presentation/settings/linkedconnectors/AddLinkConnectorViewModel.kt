package com.babylon.wallet.android.presentation.settings.linkedconnectors

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.PeerdroidClient
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.connection.LinkConnectionPayload
import com.babylon.wallet.android.domain.usecases.connection.AddLinkConnectionUseCase
import com.babylon.wallet.android.domain.usecases.connection.ParseLinkConnectionDetailsUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.apppreferences.P2PLink
import rdx.works.profile.domain.p2plink.AddP2PLinkUseCase
import rdx.works.profile.domain.p2plink.DeleteP2PLinkUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AddLinkConnectorViewModel @Inject constructor(
    private val parseLinkConnectionDetailsUseCase: ParseLinkConnectionDetailsUseCase,
    private val addLinkConnectionUseCase: AddLinkConnectionUseCase,
    private val peerdroidClient: PeerdroidClient,
    private val deleteP2PLinkUseCase: DeleteP2PLinkUseCase,
    private val addP2PLinkUseCase: AddP2PLinkUseCase
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
                addNewLinkConnection(
                    payload = payload,
                    name = content.connectorDisplayName
                )
            }
            is AddLinkConnectorUiState.Content.UpdateLinkConnector -> {
                updateLinkConnection(payload)
            }
            else -> {
                Timber.e("This shouldn't happen. Invalid UI state: $state")
            }
        }
    }

    fun onCloseClick() {
        exitLinkingFlow()
    }

    fun onErrorDismiss() {
        exitLinkingFlow()
    }

    private fun addNewLinkConnection(payload: LinkConnectionPayload, name: String) {
        viewModelScope.launch {
            _state.update { it.copy(isAddingNewLinkConnectorInProgress = true) }

            val newLink = P2PLink.init(
                connectionPassword = payload.password,
                displayName = name,
                publicKey = payload.publicKey,
                purpose = payload.purpose
            )
            addLinkConnectionUseCase(newLink)
                .onSuccess {
                    addP2PLinkUseCase(newLink)
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            error = AddLinkConnectorUiState.Error.Other(
                                message = UiMessage.ErrorMessage(throwable)
                            )
                        )
                    }
                }

            exitLinkingFlow()
        }
    }

    private fun updateLinkConnection(payload: LinkConnectionPayload) {
        viewModelScope.launch {
            val existingLink = payload.existingP2PLink ?: run {
                Timber.e("The existing p2p link can't be null at this point")
                return@launch
            }

            _state.update { it.copy(isAddingNewLinkConnectorInProgress = true) }

            val updatedLink = existingLink.copy(
                connectionPassword = payload.password
            )

            addLinkConnectionUseCase(updatedLink)
                .onSuccess {
                    deleteP2PLinkUseCase(existingLink.connectionPassword)
                    peerdroidClient.deleteLink(existingLink.connectionPassword)
                    addP2PLinkUseCase(updatedLink)
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            error = AddLinkConnectorUiState.Error.Other(
                                message = UiMessage.ErrorMessage(throwable)
                            )
                        )
                    }
                }

            exitLinkingFlow()
        }
    }

    private fun exitLinkingFlow() {
        linkConnectionPayload = null
        _state.value = AddLinkConnectorUiState.init

        viewModelScope.launch {
            sendEvent(Event.Close)
        }
    }

    internal sealed interface Event : OneOffEvent {
        data object Close : Event
    }
}

data class AddLinkConnectorUiState(
    val isAddingNewLinkConnectorInProgress: Boolean,
    val content: Content,
    val error: Error?
) : UiState {

    sealed interface Content {

        data object ScanQrCode : Content

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
            override val message: UiMessage
        ) : Error(message)
    }

    companion object {
        val init = AddLinkConnectorUiState(
            isAddingNewLinkConnectorInProgress = false,
            content = Content.ScanQrCode,
            error = null
        )
    }
}
