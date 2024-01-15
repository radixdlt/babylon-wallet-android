package com.babylon.wallet.android.presentation.dapp

import com.babylon.wallet.android.data.dapp.DappMessenger
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.getDappMessage
import com.babylon.wallet.android.domain.usecases.BuildAuthorizedDappResponseUseCase
import com.babylon.wallet.android.domain.usecases.BuildUnauthorizedDappResponseUseCase
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.ViewModelDelegate
import com.babylon.wallet.android.presentation.dapp.authorized.login.DAppLoginUiState
import com.babylon.wallet.android.presentation.dapp.authorized.login.Event
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.toUiModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.repository.DAppConnectionRepository
import rdx.works.profile.domain.ProfileException
import rdx.works.profile.domain.gateway.GetCurrentGatewayUseCase
import javax.inject.Inject

class DAppLoginDelegate @Inject constructor(
    private val dAppMessenger: DappMessenger,
    private val dAppConnectionRepository: DAppConnectionRepository,
    private val getCurrentGatewayUseCase: GetCurrentGatewayUseCase,
    private val stateRepository: StateRepository,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val buildAuthorizedDappResponseUseCase: BuildAuthorizedDappResponseUseCase,
    private val buildUnauthorizedDappResponseUseCase: BuildUnauthorizedDappResponseUseCase
) : ViewModelDelegate<DAppLoginUiState>() {

    suspend fun processLoginRequest(
        requestId: String,
        sendEvent: (Event) -> Unit,
        isAuthorizedRequest: Boolean
    ) {
        _state.update { it.copy(requestId = requestId) }

        if (isAuthorizedRequest) {
            val requestToHandle = incomingRequestRepository.getAuthorizedRequest(requestId)
            if (requestToHandle == null) {
                sendEvent(Event.CloseLoginFlow)
                return
            } else {
                _state.update { it.copy(authorizeRequest = requestToHandle) }
            }

            val currentNetworkId = getCurrentGatewayUseCase().network.networkId().value
            if (currentNetworkId != requestToHandle.requestMetadata.networkId) {
                handleRequestError(
                    exception = RadixWalletException.DappRequestException.WrongNetwork(
                        currentNetworkId,
                        requestToHandle.requestMetadata.networkId
                    ),
                    isAuthorizedRequest = true
                )
                return
            }
            if (!requestToHandle.isValidRequest()) {
                handleRequestError(
                    exception = RadixWalletException.DappRequestException.InvalidRequest,
                    isAuthorizedRequest = true
                )
                return
            }

            val authorizedDApp = dAppConnectionRepository.getAuthorizedDapp(
                requestToHandle.requestMetadata.dAppDefinitionAddress
            )

            _state.update { it.copy(authorizedDApp = authorizedDApp) }
            _state.update { it.copy(editedDApp = authorizedDApp) }

            stateRepository.getDAppsDetails(
                definitionAddresses = listOf(requestToHandle.metadata.dAppDefinitionAddress),
                skipCache = false
            ).onSuccess { dApps ->
                dApps.firstOrNull()?.let { dApp ->
                    _state.update { it.copy(dapp = dApp) }
                }
            }.onFailure { error ->
                _state.update { it.copy(uiMessage = UiMessage.ErrorMessage(error)) }
            }
        } else {
            val requestToHandle = incomingRequestRepository.getUnauthorizedRequest(requestId)
            if (requestToHandle == null) {
                sendEvent(Event.CloseLoginFlow)
                return
            } else {
                _state.update { it.copy(unAuthorizeRequest = requestToHandle) }
            }

            val currentNetworkId = getCurrentGatewayUseCase().network.networkId().value
            if (currentNetworkId != requestToHandle.requestMetadata.networkId) {
                handleRequestError(
                    exception = RadixWalletException.DappRequestException.WrongNetwork(
                        currentNetworkId,
                        requestToHandle.requestMetadata.networkId
                    ),
                    isAuthorizedRequest = false
                )
                return
            }
            if (!requestToHandle.isValidRequest()) {
                handleRequestError(
                    exception = RadixWalletException.DappRequestException.InvalidRequest,
                    isAuthorizedRequest = false
                )
                return
            }

            stateRepository.getDAppsDetails(
                definitionAddresses = listOf(requestToHandle.metadata.dAppDefinitionAddress),
                skipCache = false
            ).onSuccess { dApps ->
                dApps.firstOrNull()?.let { dApp ->
                    _state.update { it.copy(dapp = dApp) }
                }
            }.onFailure { error ->
                _state.update { it.copy(uiMessage = UiMessage.ErrorMessage(error)) }
            }
        }
    }

    suspend fun handleRequestError(exception: Throwable, isAuthorizedRequest: Boolean) {
        if (exception is RadixWalletException.DappRequestException) {
            if (isAuthorizedRequest) {
                if (exception.cause is RadixWalletException.LedgerCommunicationException) {
                    return
                }
            } else {
                if (exception is RadixWalletException.LedgerCommunicationException.FailedToSignAuthChallenge) {
                    return
                }
            }
            if (exception.cause is RadixWalletException.SignatureCancelled) {
                return
            }
            if (exception.cause is ProfileException.NoMnemonic) {
                _state.update { it.copy(isNoMnemonicErrorVisible = true) }
                return
            }
            dAppMessenger.sendWalletInteractionResponseFailure(
                remoteConnectorId = _state.value.remoteConnectionId,
                requestId = _state.value.requestId.orEmpty(),
                error = exception.ceError,
                message = exception.getDappMessage()
            )
            _state.update { it.copy(failureDialog = FailureDialogState.Open(exception)) }
        } else {
            if (isAuthorizedRequest.not() && exception is ProfileException.NoMnemonic) {
                _state.update { it.copy(isNoMnemonicErrorVisible = true) }
            }
        }
    }

    fun observeSigningState(isAuthorizedRequest: Boolean) {
        viewModelScope.launch {
            if (isAuthorizedRequest) {
                buildAuthorizedDappResponseUseCase.signingState
            } else {
                buildUnauthorizedDappResponseUseCase.signingState
            }.collect { signingState ->
                _state.update { state ->
                    state.copy(interactionState = signingState)
                }
            }
        }
    }

    fun onAcknowledgeFailureDialog(
        sendEvent: (Event) -> Unit
    ) = viewModelScope.launch {
        _state.update { it.copy(failureDialog = FailureDialogState.Closed) }
        sendEvent(Event.CloseLoginFlow)
        incomingRequestRepository.requestHandled(requestId = _state.value.requestId.orEmpty())
    }

    fun onDismissSigningStatusDialog() {
        _state.update { it.copy(interactionState = null) }
    }

    fun onSelectPersona(persona: Network.Persona) {
        _state.update { it.copy(selectedPersona = persona.toUiModel()) }
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    fun dismissNoMnemonicError() {
        _state.update { it.copy(isNoMnemonicErrorVisible = false) }
    }
}