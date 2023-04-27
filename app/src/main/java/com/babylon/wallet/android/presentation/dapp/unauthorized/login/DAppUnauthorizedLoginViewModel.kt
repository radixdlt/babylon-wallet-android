package com.babylon.wallet.android.presentation.dapp.unauthorized.login

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.DappMessenger
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.model.WalletErrorType
import com.babylon.wallet.android.data.dapp.model.toKind
import com.babylon.wallet.android.data.repository.dappmetadata.DappMetadataRepository
import com.babylon.wallet.android.data.transaction.DappRequestException
import com.babylon.wallet.android.data.transaction.DappRequestFailure
import com.babylon.wallet.android.domain.common.onError
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.model.DappWithMetadata
import com.babylon.wallet.android.domain.model.MessageFromDataChannel.IncomingRequest.AccountsRequestItem
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.PersonaUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.toUiModel
import com.babylon.wallet.android.presentation.dapp.unauthorized.InitialUnauthorizedLoginRoute
import com.babylon.wallet.android.presentation.model.encodeToString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.gateway.GetCurrentGatewayUseCase
import rdx.works.profile.domain.personaOnCurrentNetwork
import javax.inject.Inject

@HiltViewModel
@Suppress("LongParameterList", "TooManyFunctions")
class DAppUnauthorizedLoginViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dAppMessenger: DappMessenger,
    private val getProfileUseCase: GetProfileUseCase,
    private val getCurrentGatewayUseCase: GetCurrentGatewayUseCase,
    private val dappMetadataRepository: DappMetadataRepository,
    private val incomingRequestRepository: IncomingRequestRepository
) : StateViewModel<DAppUnauthorizedLoginUiState>(),
    OneOffEventHandler<DAppUnauthorizedLoginEvent> by OneOffEventHandlerImpl() {

    private val args = DAppUnauthorizedLoginArgs(savedStateHandle)

    private val request = incomingRequestRepository.getUnauthorizedRequest(
        args.requestId
    )

    private val topLevelOneOffEventHandler = OneOffEventHandlerImpl<DAppUnauthorizedLoginEvent>()
    val topLevelOneOffEvent by topLevelOneOffEventHandler

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
            val result = dappMetadataRepository.getDappMetadata(
                defitnionAddress = request.metadata.dAppDefinitionAddress,
                needMostRecentData = false
            )
            result.onValue { dappMetadata ->
                _state.update {
                    it.copy(dappMetadata = dappMetadata)
                }
            }
            result.onError { error ->
                _state.update { it.copy(uiMessage = UiMessage.ErrorMessage(error)) }
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
                            request.oneTimeAccountsRequestItem.numberOfAccounts,
                            request.oneTimeAccountsRequestItem.quantifier == AccountsRequestItem.AccountNumberQuantifier.Exactly
                        )
                    )
                }
            }
            request.oneTimePersonaDataRequestItem != null -> {
                _state.update { state ->
                    state.copy(
                        initialUnauthorizedLoginRoute = InitialUnauthorizedLoginRoute.OnetimePersonaData(
                            request.oneTimePersonaDataRequestItem.fields.map { it.toKind() }.encodeToString()
                        )
                    )
                }
            }
            else -> onRejectRequest()
        }
    }

    @Suppress("MagicNumber")
    private suspend fun handleRequestError(failure: DappRequestFailure) {
        _state.update { it.copy(uiMessage = UiMessage.ErrorMessage(DappRequestException(failure))) }
        dAppMessenger.sendWalletInteractionResponseFailure(
            request.dappId,
            args.requestId,
            failure.toWalletErrorType(),
            failure.getDappMessage()
        )
        delay(2000)
        topLevelOneOffEventHandler.sendEvent(DAppUnauthorizedLoginEvent.RejectLogin)
        incomingRequestRepository.requestHandled(requestId = args.requestId)
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    fun onGrantedPersonaDataOnetime() {
        val selectedPersona = checkNotNull(state.value.selectedPersona)
        viewModelScope.launch {
            val requiredFields = checkNotNull(request.oneTimePersonaDataRequestItem?.fields?.map { it.toKind() })
            getProfileUseCase.personaOnCurrentNetwork(selectedPersona.persona.address)?.let { updatedPersona ->
                val dataFields = updatedPersona.fields.filter { requiredFields.contains(it.kind) }
                _state.update {
                    it.copy(
                        selectedPersona = updatedPersona.toUiModel(),
                        selectedOnetimeDataFields = dataFields.toPersistentList()
                    )
                }
                sendRequestResponse()
            }
        }
    }

    fun onRejectRequest() {
        viewModelScope.launch {
            dAppMessenger.sendWalletInteractionResponseFailure(
                request.dappId,
                args.requestId,
                error = WalletErrorType.RejectedByUser
            )
            topLevelOneOffEventHandler.sendEvent(DAppUnauthorizedLoginEvent.RejectLogin)
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
                    DAppUnauthorizedLoginEvent.PersonaDataOnetime(
                        request.oneTimePersonaDataRequestItem.fields.map { it.toKind() }.encodeToString()
                    )
                )
            } else {
                sendRequestResponse()
            }
        }
    }

    private suspend fun sendRequestResponse() {
        dAppMessenger.sendWalletInteractionUnauthorizedSuccessResponse(
            request.dappId,
            args.requestId,
            state.value.selectedAccountsOneTime,
            state.value.selectedOnetimeDataFields,
        )
        sendEvent(
            DAppUnauthorizedLoginEvent.LoginFlowCompleted(
                requestId = request.id,
                dAppName = state.value.dappMetadata?.name ?: "Unknown dApp"
            )
        )
    }

    override fun initialState(): DAppUnauthorizedLoginUiState {
        return DAppUnauthorizedLoginUiState()
    }
}

sealed interface DAppUnauthorizedLoginEvent : OneOffEvent {

    object RejectLogin : DAppUnauthorizedLoginEvent

    data class LoginFlowCompleted(
        val requestId: String,
        val dAppName: String
    ) : DAppUnauthorizedLoginEvent

    data class PersonaDataOnetime(val requiredFieldsEncoded: String) : DAppUnauthorizedLoginEvent
}

data class DAppUnauthorizedLoginUiState(
    val dappMetadata: DappWithMetadata? = null,
    val uiMessage: UiMessage? = null,
    val initialUnauthorizedLoginRoute: InitialUnauthorizedLoginRoute? = null,
    val selectedOnetimeDataFields: ImmutableList<Network.Persona.Field> = persistentListOf(),
    val selectedAccountsOneTime: ImmutableList<AccountItemUiModel> = persistentListOf(),
    val selectedPersona: PersonaUiModel? = null
) : UiState
