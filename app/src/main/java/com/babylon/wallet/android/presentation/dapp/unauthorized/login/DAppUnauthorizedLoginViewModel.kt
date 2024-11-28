package com.babylon.wallet.android.presentation.dapp.unauthorized.login

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.getDappMessage
import com.babylon.wallet.android.domain.model.messages.DappToWalletInteraction
import com.babylon.wallet.android.domain.model.messages.IncomingRequestResponse
import com.babylon.wallet.android.domain.model.messages.RequiredPersonaFields
import com.babylon.wallet.android.domain.model.messages.WalletUnauthorizedRequest
import com.babylon.wallet.android.domain.usecases.RespondToIncomingRequestUseCase
import com.babylon.wallet.android.domain.usecases.login.BuildUnauthorizedDappResponseUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.dapp.FailureDialogState
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.PersonaUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.toUiModel
import com.babylon.wallet.android.presentation.dapp.unauthorized.InitialUnauthorizedLoginRoute
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.DappWalletInteractionErrorType
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.PersonaData
import com.radixdlt.sargon.SignatureWithPublicKey
import com.radixdlt.sargon.extensions.ProfileEntity
import com.radixdlt.sargon.extensions.init
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.domain.DApp
import rdx.works.core.logNonFatalException
import rdx.works.core.sargon.activePersonaOnCurrentNetwork
import rdx.works.core.sargon.fields
import rdx.works.core.sargon.toPersonaData
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.ProfileException
import javax.inject.Inject

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class DAppUnauthorizedLoginViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val respondToIncomingRequestUseCase: RespondToIncomingRequestUseCase,
    private val appEventBus: AppEventBus,
    private val getProfileUseCase: GetProfileUseCase,
    private val stateRepository: StateRepository,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val buildUnauthorizedDappResponseUseCase: BuildUnauthorizedDappResponseUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : StateViewModel<DAppUnauthorizedLoginUiState>(), OneOffEventHandler<Event> by OneOffEventHandlerImpl() {

    override fun initialState(): DAppUnauthorizedLoginUiState {
        return DAppUnauthorizedLoginUiState()
    }

    private val args = DAppUnauthorizedLoginArgs(savedStateHandle)

    private lateinit var request: WalletUnauthorizedRequest

    init {
        viewModelScope.launch {
            appEventBus.events.filterIsInstance<AppEvent.DeferRequestHandling>().collect {
                if (it.interactionId == args.interactionId) {
                    sendEvent(Event.CloseLoginFlow)
                    incomingRequestRepository.requestDeferred(args.interactionId)
                }
            }
        }

        viewModelScope.launch(ioDispatcher) {
            val requestToHandle = incomingRequestRepository.getRequest(args.interactionId) as? WalletUnauthorizedRequest

            if (requestToHandle == null) {
                sendEvent(Event.CloseLoginFlow)
                return@launch
            } else {
                request = requestToHandle
                if (request.isValidRequest().not()) {
                    handleRequestError(RadixWalletException.DappRequestException.InvalidRequest)
                    return@launch
                }
            }

            val dAppDefinitionAddress = runCatching {
                AccountAddress.init(request.metadata.dAppDefinitionAddress)
            }.getOrNull()
            if (dAppDefinitionAddress == null) { // validate dApp
                handleRequestError(RadixWalletException.DappRequestException.InvalidRequest)
                return@launch
            } else {
                getDappDetails(dAppDefinitionAddress = dAppDefinitionAddress)
            }

            setInitialDappLoginRoute()
        }
    }

    private suspend fun getDappDetails(dAppDefinitionAddress: AccountAddress) {
        stateRepository.getDAppsDetails(
            definitionAddresses = listOf(dAppDefinitionAddress),
            isRefreshing = false
        ).onSuccess { dApps ->
            dApps.firstOrNull()?.let { dApp ->
                _state.update { it.copy(dapp = dApp) }
            }
        }.onFailure { error ->
            _state.update { it.copy(uiMessage = UiMessage.ErrorMessage(error)) }
        }
    }

    private fun setInitialDappLoginRoute() {
        val request = request
        when {
            request.oneTimeAccountsRequestItem != null -> {
                _state.update {
                    it.copy(
                        initialUnauthorizedLoginRoute = InitialUnauthorizedLoginRoute.ChooseAccount(
                            walletUnauthorizedRequestInteractionId = args.interactionId,
                            numberOfAccounts = request.oneTimeAccountsRequestItem.numberOfValues.quantity,
                            isExactAccountsCount = request.oneTimeAccountsRequestItem.numberOfValues.quantifier
                                == DappToWalletInteraction.NumberOfValues.Quantifier.Exactly
                        )
                    )
                }
            }

            request.oneTimePersonaDataRequestItem != null -> {
                _state.update { state ->
                    state.copy(
                        initialUnauthorizedLoginRoute = InitialUnauthorizedLoginRoute.OnetimePersonaData(
                            requiredPersonaFields = request.oneTimePersonaDataRequestItem.toRequiredFields()
                        )
                    )
                }
            }

            else -> onUserRejectedRequest()
        }
    }

    fun onOneTimeAccountsCollected(accountsWithSignatures: Map<ProfileEntity.AccountEntity, SignatureWithPublicKey?>) {
        viewModelScope.launch {
            _state.update { it.copy(oneTimeAccountsWithSignatures = accountsWithSignatures) }
            val request = request
            if (request.oneTimePersonaDataRequestItem != null) {
                sendEvent(Event.NavigateToOneTimeChoosePersona(request.oneTimePersonaDataRequestItem.toRequiredFields()))
            } else {
                sendResponseToDapp()
            }
        }
    }

    fun onPersonaSelected(persona: Persona) = _state.update { it.copy(selectedPersona = persona.toUiModel()) }

    fun onPersonaGranted() {
        val selectedPersona = checkNotNull(state.value.selectedPersona)
        val requiredFields = checkNotNull(
            request.oneTimePersonaDataRequestItem
                ?.toRequiredFields()
                ?.fields
                ?.map { it.kind }
        )

        viewModelScope.launch {
            getProfileUseCase().activePersonaOnCurrentNetwork(selectedPersona.persona.address)?.let { updatedPersona ->
                val dataFields = updatedPersona.personaData.fields.filter { requiredFields.contains(it.value.kind) }
                _state.update { state ->
                    state.copy(
                        selectedPersona = updatedPersona.toUiModel(),
                        selectedPersonaData = dataFields.map { it.value }.toPersonaData()
                    )
                }
            }
            sendResponseToDapp()
        }
    }

    fun onUserRejectedRequest() {
        viewModelScope.launch {
            incomingRequestRepository.requestHandled(requestId = args.interactionId)
            respondToIncomingRequestUseCase.respondWithFailure(request, DappWalletInteractionErrorType.REJECTED_BY_USER)
            sendEvent(Event.CloseLoginFlow)
        }
    }

    fun onAcknowledgeFailureDialog() = viewModelScope.launch {
        val exception = (_state.value.failureDialogState as? FailureDialogState.Open)
            ?.dappRequestException
            ?: return@launch
        respondToIncomingRequestUseCase.respondWithFailure(
            request,
            exception.dappWalletInteractionErrorType,
            exception.getDappMessage()
        )
        _state.update { it.copy(failureDialogState = FailureDialogState.Closed) }
        sendEvent(Event.CloseLoginFlow)
        incomingRequestRepository.requestHandled(requestId = args.interactionId)
    }

    fun dismissNoMnemonicError() = _state.update { it.copy(isNoMnemonicErrorVisible = false) }

    fun onMessageShown() = _state.update { it.copy(uiMessage = null) }

    private fun sendResponseToDapp() {
        viewModelScope.launch {
            val walletToDappInteractionResponse = buildUnauthorizedDappResponseUseCase(
                request = request,
                oneTimeAccountsWithSignatures = state.value.oneTimeAccountsWithSignatures,
                oneTimePersonaData = state.value.selectedPersonaData,
            )

            respondToIncomingRequestUseCase.respondWithSuccess(
                request = request,
                response = walletToDappInteractionResponse
            ).onSuccess { result ->
                sendEvent(Event.LoginFlowCompleted)
                if (!request.isInternal) {
                    appEventBus.sendEvent(
                        AppEvent.Status.DappInteraction(
                            requestId = request.interactionId,
                            dAppName = state.value.dapp?.name,
                            isMobileConnect = result is IncomingRequestResponse.SuccessRadixMobileConnect
                        )
                    )
                }
            }.onFailure { exception ->
                handleRequestError(exception)
            }
        }
    }

    private suspend fun handleRequestError(exception: Throwable) {
        if (exception is RadixWalletException.DappRequestException) {
            logNonFatalException(exception)
            when (exception.cause) {
                is ProfileException.SecureStorageAccess -> {
                    appEventBus.sendEvent(AppEvent.SecureFolderWarning)
                }

                is ProfileException.NoMnemonic -> {
                    _state.update { it.copy(isNoMnemonicErrorVisible = true) }
                }

                is RadixWalletException.LedgerCommunicationException,
                is RadixWalletException.DappRequestException.RejectedByUser -> {
                }

                else -> {
                    respondToIncomingRequestUseCase.respondWithFailure(
                        request = request,
                        dappWalletInteractionErrorType = exception.dappWalletInteractionErrorType,
                        message = exception.getDappMessage()
                    )
                    _state.update { it.copy(failureDialogState = FailureDialogState.Open(exception)) }
                }
            }
        }
    }
}

sealed interface Event : OneOffEvent {

    data object CloseLoginFlow : Event

    data object LoginFlowCompleted : Event

    data class NavigateToOneTimeChoosePersona(val requiredPersonaFields: RequiredPersonaFields) : Event
}

data class DAppUnauthorizedLoginUiState(
    val dapp: DApp? = null,
    val uiMessage: UiMessage? = null,
    val failureDialogState: FailureDialogState = FailureDialogState.Closed,
    val initialUnauthorizedLoginRoute: InitialUnauthorizedLoginRoute? = null,
    val selectedPersonaData: PersonaData? = null,
    val selectedPersona: PersonaUiModel? = null,
    val oneTimeAccountsWithSignatures: Map<ProfileEntity.AccountEntity, SignatureWithPublicKey?> = emptyMap(),
    val isNoMnemonicErrorVisible: Boolean = false
) : UiState
