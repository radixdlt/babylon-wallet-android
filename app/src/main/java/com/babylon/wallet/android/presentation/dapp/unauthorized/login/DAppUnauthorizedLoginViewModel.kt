package com.babylon.wallet.android.presentation.dapp.unauthorized.login

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.DappMessenger
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.model.WalletErrorType
import com.babylon.wallet.android.data.repository.dappmetadata.DAppRepository
import com.babylon.wallet.android.data.transaction.DappRequestException
import com.babylon.wallet.android.data.transaction.DappRequestFailure
import com.babylon.wallet.android.domain.common.onError
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.model.DAppWithMetadata
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
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
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.PersonaDataEntryID
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.gateway.GetCurrentGatewayUseCase
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
    private val incomingRequestRepository: IncomingRequestRepository
) : StateViewModel<DAppUnauthorizedLoginUiState>(),
    OneOffEventHandler<Event> by OneOffEventHandlerImpl() {

    private val args = DAppUnauthorizedLoginArgs(savedStateHandle)

    private val request = incomingRequestRepository.getUnauthorizedRequest(
        args.requestId
    )

    init {
        viewModelScope.launch {
            val currentNetworkId = getCurrentGatewayUseCase().network.networkId().value
            if (currentNetworkId != request.requestMetadata.networkId) {
                handleRequestError(
                    DappRequestFailure.WrongNetwork(
                        currentNetworkId,
                        request.requestMetadata.networkId
                    )
                )
                return@launch
            }
            if (!request.isValidRequest()) {
                handleRequestError(DappRequestFailure.InvalidRequest)
                return@launch
            }
            val result = dAppRepository.getDAppMetadata(
                definitionAddress = request.metadata.dAppDefinitionAddress,
                needMostRecentData = false
            )
            result.onValue { dappWithMetadata ->
                _state.update {
                    it.copy(dappWithMetadata = dappWithMetadata)
                }
            }
            result.onError { error ->
                _state.update { it.copy(uiMessage = UiMessage.ErrorMessage.from(error)) }
            }
            setInitialDappLoginRoute()
        }
    }

    private fun setInitialDappLoginRoute() {
        when {
            request.oneTimeAccountsRequestItem != null -> {
                _state.update {
                    it.copy(
                        initialUnauthorizedLoginRoute = InitialUnauthorizedLoginRoute.ChooseAccount(
                            request.oneTimeAccountsRequestItem.numberOfValues.quantity,
                            request.oneTimeAccountsRequestItem.numberOfValues.quantifier == MessageFromDataChannel.IncomingRequest.NumberOfValues.Quantifier.Exactly
                        )
                    )
                }
            }

            request.oneTimePersonaDataRequestItem != null -> {
                _state.update { state ->
                    state.copy(
                        // TODO persona data
//                        initialUnauthorizedLoginRoute = InitialUnauthorizedLoginRoute.OnetimePersonaData(
//                            request.oneTimePersonaDataRequestItem.fields.map { it.toKind() }.encodeToString()
//                        )
                    )
                }
            }

            else -> onRejectRequest()
        }
    }

    @Suppress("MagicNumber")
    private suspend fun handleRequestError(failure: DappRequestFailure) {
        _state.update { it.copy(uiMessage = UiMessage.ErrorMessage.from(DappRequestException(failure))) }
        dAppMessenger.sendWalletInteractionResponseFailure(
            request.dappId,
            args.requestId,
            failure.toWalletErrorType(),
            failure.getDappMessage()
        )
        delay(2000)
        sendEvent(Event.RejectLogin)
        incomingRequestRepository.requestHandled(requestId = args.requestId)
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    fun onGrantedPersonaDataOnetime() {
        val selectedPersona = checkNotNull(state.value.selectedPersona)
        viewModelScope.launch {
            // TODO persona data
//            val requiredFields = checkNotNull(request.oneTimePersonaDataRequestItem?.fields?.map { it.toKind() })
//            getProfileUseCase.personaOnCurrentNetwork(selectedPersona.persona.address)?.let { updatedPersona ->
//                val dataFields = updatedPersona.fields.filter { requiredFields.contains(it.id) }
//                _state.update {
//                    it.copy(
//                        selectedPersona = updatedPersona.toUiModel(),
//                        selectedOnetimeDataFields = dataFields.toPersistentList()
//                    )
//                }
//                sendEvent(Event.RequestCompletionBiometricPrompt)
//            }
        }
    }

    fun onRejectRequest() {
        viewModelScope.launch {
            dAppMessenger.sendWalletInteractionResponseFailure(
                request.dappId,
                args.requestId,
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
                // TODO persona data
//                sendEvent(
//                    Event.PersonaDataOnetime(
//                        request.oneTimePersonaDataRequestItem.fields.map { it.toKind() }.encodeToString()
//                    )
//                )
            } else {
                sendEvent(Event.RequestCompletionBiometricPrompt)
            }
        }
    }

    fun sendRequestResponse() {
        viewModelScope.launch {
            dAppMessenger.sendWalletInteractionUnauthorizedSuccessResponse(
                request.dappId,
                args.requestId,
                state.value.selectedAccountsOneTime,
                state.value.selectedOnetimeDataFields,
            )
            sendEvent(Event.LoginFlowCompleted)
            appEventBus.sendEvent(
                AppEvent.Status.DappInteraction(
                    requestId = request.id,
                    dAppName = state.value.dappWithMetadata?.name
                )
            )
        }
    }

    override fun initialState(): DAppUnauthorizedLoginUiState {
        return DAppUnauthorizedLoginUiState()
    }
}

sealed interface Event : OneOffEvent {

    object RequestCompletionBiometricPrompt : Event
    object RejectLogin : Event

    object LoginFlowCompleted : Event

    data class PersonaDataOnetime(val request: MessageFromDataChannel.IncomingRequest.PersonaRequestItem) : Event
}

data class DAppUnauthorizedLoginUiState(
    val dappWithMetadata: DAppWithMetadata? = null,
    val uiMessage: UiMessage? = null,
    val initialUnauthorizedLoginRoute: InitialUnauthorizedLoginRoute? = null,
    val selectedOnetimeDataFields: ImmutableList<PersonaDataEntryID> = persistentListOf(),
    val selectedAccountsOneTime: ImmutableList<AccountItemUiModel> = persistentListOf(),
    val selectedPersona: PersonaUiModel? = null
) : UiState
