package com.babylon.wallet.android.presentation.dapp.unauthorized.login

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.DappMessenger
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.model.WalletErrorType
import com.babylon.wallet.android.data.repository.dappmetadata.DAppRepository
import com.babylon.wallet.android.data.transaction.DappRequestException
import com.babylon.wallet.android.data.transaction.DappRequestFailure
import com.babylon.wallet.android.data.transaction.InteractionState
import com.babylon.wallet.android.domain.model.DAppWithMetadata
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.RequiredPersonaFields
import com.babylon.wallet.android.domain.model.toRequiredFields
import com.babylon.wallet.android.domain.usecases.BuildUnauthorizedDappResponseUseCase
import com.babylon.wallet.android.domain.usecases.transaction.SignatureCancelledException
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.dapp.InitialUnauthorizedLoginRoute
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.PersonaUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.toUiModel
import com.babylon.wallet.android.presentation.model.toPersonaData
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.PersonaData
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork
import rdx.works.profile.domain.gateway.GetCurrentGatewayUseCase
import rdx.works.profile.domain.personaOnCurrentNetwork
import javax.inject.Inject

@HiltViewModel
@Suppress("LongParameterList", "TooManyFunctions")
class DAppUnauthorizedLoginViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dAppMessenger: DappMessenger,
    private val appEventBus: AppEventBus,
    private val getProfileUseCase: GetProfileUseCase,
    private val getCurrentGatewayUseCase: GetCurrentGatewayUseCase,
    private val dAppRepository: DAppRepository,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val buildUnauthorizedDappResponseUseCase: BuildUnauthorizedDappResponseUseCase
) : StateViewModel<DAppUnauthorizedLoginUiState>(),
    OneOffEventHandler<Event> by OneOffEventHandlerImpl() {

    private val args = DAppUnauthorizedLoginArgs(savedStateHandle)

    private val request = incomingRequestRepository.getUnauthorizedRequest(
        args.requestId
    )

    init {
        observeSigningState()
        viewModelScope.launch {
            val currentNetworkId = getCurrentGatewayUseCase().network.networkId().value
            if (currentNetworkId != request.requestMetadata.networkId) {
                handleRequestError(
                    DappRequestException(
                        DappRequestFailure.WrongNetwork(
                            currentNetworkId,
                            request.requestMetadata.networkId
                        )
                    )
                )
                return@launch
            }
            if (!request.isValidRequest()) {
                handleRequestError(DappRequestException(DappRequestFailure.InvalidRequest))
                return@launch
            }
            dAppRepository.getDAppMetadata(
                definitionAddress = request.metadata.dAppDefinitionAddress,
                needMostRecentData = false
            ).onSuccess { dappWithMetadata ->
                _state.update {
                    it.copy(dappWithMetadata = dappWithMetadata)
                }
            }.onFailure { error ->
                _state.update { it.copy(uiMessage = UiMessage.ErrorMessage.from(error)) }
            }
            setInitialDappLoginRoute()
        }
    }

    private fun observeSigningState() {
        viewModelScope.launch {
            buildUnauthorizedDappResponseUseCase.signingState.collect { signingState ->
                _state.update { state ->
                    state.copy(interactionState = signingState)
                }
            }
        }
    }

    fun onDismissSigningStatusDialog() {
        _state.update { it.copy(interactionState = null) }
    }

    private fun setInitialDappLoginRoute() {
        when {
            request.oneTimeAccountsRequestItem != null -> {
                _state.update {
                    it.copy(
                        initialUnauthorizedLoginRoute = InitialUnauthorizedLoginRoute.ChooseAccount(
                            request.oneTimeAccountsRequestItem.numberOfValues.quantity,
                            request.oneTimeAccountsRequestItem.numberOfValues.quantifier
                                == MessageFromDataChannel.IncomingRequest.NumberOfValues.Quantifier.Exactly
                        )
                    )
                }
            }

            request.oneTimePersonaDataRequestItem != null -> {
                _state.update { state ->
                    state.copy(
                        initialUnauthorizedLoginRoute = InitialUnauthorizedLoginRoute.OnetimePersonaData(
                            request.oneTimePersonaDataRequestItem.toRequiredFields()
                        )
                    )
                }
            }

            else -> onRejectRequest()
        }
    }

    private suspend fun handleRequestError(exception: DappRequestException) {
        if (exception.e is SignatureCancelledException) {
            return
        }
        dAppMessenger.sendWalletInteractionResponseFailure(
            remoteConnectorId = request.remoteConnectorId,
            requestId = args.requestId,
            error = exception.failure.toWalletErrorType(),
            message = exception.failure.getDappMessage()
        )
        _state.update { it.copy(failureDialog = DAppUnauthorizedLoginUiState.FailureDialog.Open(exception)) }
    }

    fun onAcknowledgeFailureDialog() = viewModelScope.launch {
        _state.update { it.copy(failureDialog = DAppUnauthorizedLoginUiState.FailureDialog.Closed) }
        sendEvent(Event.RejectLogin)
        incomingRequestRepository.requestHandled(requestId = args.requestId)
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    fun onGrantedPersonaDataOnetime() {
        val selectedPersona = checkNotNull(state.value.selectedPersona)
        viewModelScope.launch {
            val requiredFields = checkNotNull(
                request.oneTimePersonaDataRequestItem
                    ?.toRequiredFields()
                    ?.fields
                    ?.map { it.kind }
            )
            getProfileUseCase.personaOnCurrentNetwork(selectedPersona.persona.address)?.let { updatedPersona ->
                val dataFields = updatedPersona.personaData.allFields.filter { requiredFields.contains(it.value.kind) }
                _state.update { state ->
                    state.copy(
                        selectedPersona = updatedPersona.toUiModel(),
                        selectedPersonaData = dataFields.map { it.value }.toPersonaData()
                    )
                }
                sendEvent(Event.RequestCompletionBiometricPrompt(request.needSignatures()))
            }
        }
    }

    fun onRejectRequest() {
        viewModelScope.launch {
            dAppMessenger.sendWalletInteractionResponseFailure(
                remoteConnectorId = request.remoteConnectorId,
                requestId = args.requestId,
                error = WalletErrorType.RejectedByUser
            )
            sendEvent(Event.RejectLogin)
            incomingRequestRepository.requestHandled(requestId = args.requestId)
        }
    }

    fun onSelectPersona(persona: Network.Persona) {
        _state.update { it.copy(selectedPersona = persona.toUiModel()) }
    }

    fun onAccountsSelected(onetimeAccounts: List<AccountItemUiModel>) {
        viewModelScope.launch {
            _state.update { it.copy(selectedAccountsOneTime = onetimeAccounts.toPersistentList()) }
            if (request.oneTimePersonaDataRequestItem != null) {
                sendEvent(
                    Event.PersonaDataOnetime(
                        request.oneTimePersonaDataRequestItem.toRequiredFields()
                    )
                )
            } else {
                sendEvent(Event.RequestCompletionBiometricPrompt(request.needSignatures()))
            }
        }
    }

    fun sendRequestResponse(deviceBiometricAuthenticationProvider: suspend () -> Boolean = { true }) {
        viewModelScope.launch {
            buildUnauthorizedDappResponseUseCase(
                request = request,
                oneTimeAccounts = state.value.selectedAccountsOneTime.mapNotNull {
                    getProfileUseCase.accountOnCurrentNetwork(it.address)
                },
                onetimeSharedPersonaData = state.value.selectedPersonaData,
                deviceBiometricAuthenticationProvider = deviceBiometricAuthenticationProvider
            ).onSuccess {
                dAppMessenger.sendWalletInteractionSuccessResponse(
                    remoteConnectorId = request.remoteConnectorId,
                    response = it
                )
                sendEvent(Event.LoginFlowCompleted)
                appEventBus.sendEvent(
                    AppEvent.Status.DappInteraction(
                        requestId = request.id,
                        dAppName = state.value.dappWithMetadata?.name
                    )
                )
            }.onFailure { exception ->
                if (exception is DappRequestException) {
                    handleRequestError(exception)
                }
            }
        }
    }

    override fun initialState(): DAppUnauthorizedLoginUiState {
        return DAppUnauthorizedLoginUiState()
    }
}

sealed interface Event : OneOffEvent {

    data class RequestCompletionBiometricPrompt(val requestDuringSigning: Boolean) : Event
    object RejectLogin : Event

    object LoginFlowCompleted : Event

    data class PersonaDataOnetime(val requiredPersonaFields: RequiredPersonaFields) : Event
}

data class DAppUnauthorizedLoginUiState(
    val dappWithMetadata: DAppWithMetadata? = null,
    val uiMessage: UiMessage? = null,
    val failureDialog: FailureDialog = FailureDialog.Closed,
    val initialUnauthorizedLoginRoute: InitialUnauthorizedLoginRoute? = null,
    val selectedPersonaData: PersonaData? = null,
    val selectedAccountsOneTime: ImmutableList<AccountItemUiModel> = persistentListOf(),
    val selectedPersona: PersonaUiModel? = null,
    val interactionState: InteractionState? = null
) : UiState {

    sealed interface FailureDialog {
        data object Closed : FailureDialog
        data class Open(val dappRequestException: DappRequestException) : FailureDialog
    }
}
