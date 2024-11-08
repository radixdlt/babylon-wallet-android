package com.babylon.wallet.android.presentation.dapp.unauthorized.login

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.getDappMessage
import com.babylon.wallet.android.domain.model.messages.DappToWalletInteraction
import com.babylon.wallet.android.domain.model.messages.IncomingRequestResponse
import com.babylon.wallet.android.domain.model.messages.RequiredPersonaFields
import com.babylon.wallet.android.domain.model.messages.WalletUnauthorizedRequest
import com.babylon.wallet.android.domain.usecases.BuildUnauthorizedDappResponseUseCase
import com.babylon.wallet.android.domain.usecases.RespondToIncomingRequestUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.dapp.FailureDialogState
import com.babylon.wallet.android.presentation.dapp.InitialUnauthorizedLoginRoute
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.PersonaUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.toUiModel
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.DappWalletInteractionErrorType
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.PersonaData
import com.radixdlt.sargon.extensions.init
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.domain.DApp
import rdx.works.core.logNonFatalException
import rdx.works.core.sargon.activeAccountOnCurrentNetwork
import rdx.works.core.sargon.activePersonaOnCurrentNetwork
import rdx.works.core.sargon.fields
import rdx.works.core.sargon.toPersonaData
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.ProfileException
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class DAppUnauthorizedLoginViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val respondToIncomingRequestUseCase: RespondToIncomingRequestUseCase,
    private val appEventBus: AppEventBus,
    private val getProfileUseCase: GetProfileUseCase,
    private val stateRepository: StateRepository,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val buildUnauthorizedDappResponseUseCase: BuildUnauthorizedDappResponseUseCase
) : StateViewModel<DAppUnauthorizedLoginUiState>(),
    OneOffEventHandler<Event> by OneOffEventHandlerImpl() {

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
        viewModelScope.launch {
            val requestToHandle = incomingRequestRepository.getRequest(
                args.interactionId
            ) as? WalletUnauthorizedRequest
            if (requestToHandle == null) {
                sendEvent(Event.CloseLoginFlow)
                return@launch
            } else {
                request = requestToHandle
            }
            val dAppDefinitionAddress = runCatching { AccountAddress.init(request.metadata.dAppDefinitionAddress) }.getOrNull()
            if (!request.isValidRequest() || dAppDefinitionAddress == null) {
                handleRequestError(RadixWalletException.DappRequestException.InvalidRequest)
                return@launch
            }
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
            setInitialDappLoginRoute()
        }
    }

    fun dismissNoMnemonicError() {
        _state.update { it.copy(isNoMnemonicErrorVisible = false) }
    }

    private fun setInitialDappLoginRoute() {
        val request = request
        when {
            request.oneTimeAccountsRequestItem != null -> {
                _state.update {
                    it.copy(
                        initialUnauthorizedLoginRoute = InitialUnauthorizedLoginRoute.ChooseAccount(
                            request.oneTimeAccountsRequestItem.numberOfValues.quantity,
                            request.oneTimeAccountsRequestItem.numberOfValues.quantifier
                                == DappToWalletInteraction.NumberOfValues.Quantifier.Exactly
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
                is RadixWalletException.DappRequestException.RejectedByUser -> {}

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

    fun onAcknowledgeFailureDialog() = viewModelScope.launch {
        val exception = (_state.value.failureDialogState as? FailureDialogState.Open)?.dappRequestException ?: return@launch
        respondToIncomingRequestUseCase.respondWithFailure(request, exception.dappWalletInteractionErrorType, exception.getDappMessage())
        _state.update { it.copy(failureDialogState = FailureDialogState.Closed) }
        sendEvent(Event.CloseLoginFlow)
        incomingRequestRepository.requestHandled(requestId = args.interactionId)
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
            getProfileUseCase().activePersonaOnCurrentNetwork(selectedPersona.persona.address)?.let { updatedPersona ->
                val dataFields = updatedPersona.personaData.fields.filter { requiredFields.contains(it.value.kind) }
                _state.update { state ->
                    state.copy(
                        selectedPersona = updatedPersona.toUiModel(),
                        selectedPersonaData = dataFields.map { it.value }.toPersonaData()
                    )
                }
                sendRequestResponse()
            }
        }
    }

    fun onRejectRequest() {
        viewModelScope.launch {
            incomingRequestRepository.requestHandled(requestId = args.interactionId)
            respondToIncomingRequestUseCase.respondWithFailure(request, DappWalletInteractionErrorType.REJECTED_BY_USER)
            sendEvent(Event.CloseLoginFlow)
        }
    }

    fun onSelectPersona(persona: Persona) {
        _state.update { it.copy(selectedPersona = persona.toUiModel()) }
    }

    fun onAccountsSelected(onetimeAccounts: List<AccountItemUiModel>) {
        viewModelScope.launch {
            _state.update { it.copy(selectedAccountsOneTime = onetimeAccounts.toPersistentList()) }
            val request = request
            if (request.oneTimePersonaDataRequestItem != null) {
                sendEvent(
                    Event.PersonaDataOnetime(
                        request.oneTimePersonaDataRequestItem.toRequiredFields()
                    )
                )
            } else {
                sendRequestResponse()
            }
        }
    }

    private fun sendRequestResponse() {
        viewModelScope.launch {
            buildUnauthorizedDappResponseUseCase(
                request = request,
                oneTimeAccounts = state.value.selectedAccountsOneTime.mapNotNull {
                    getProfileUseCase().activeAccountOnCurrentNetwork(it.address)
                },
                oneTimePersonaData = state.value.selectedPersonaData
            ).mapCatching {
                respondToIncomingRequestUseCase.respondWithSuccess(request, it).getOrThrow()
            }.onSuccess { result ->
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

    override fun initialState(): DAppUnauthorizedLoginUiState {
        return DAppUnauthorizedLoginUiState()
    }
}

sealed interface Event : OneOffEvent {

    data object CloseLoginFlow : Event

    data object LoginFlowCompleted : Event

    data class PersonaDataOnetime(val requiredPersonaFields: RequiredPersonaFields) : Event
}

data class DAppUnauthorizedLoginUiState(
    val dapp: DApp? = null,
    val uiMessage: UiMessage? = null,
    val failureDialogState: FailureDialogState = FailureDialogState.Closed,
    val initialUnauthorizedLoginRoute: InitialUnauthorizedLoginRoute? = null,
    val selectedPersonaData: PersonaData? = null,
    val selectedAccountsOneTime: ImmutableList<AccountItemUiModel> = persistentListOf(),
    val selectedPersona: PersonaUiModel? = null,
    val isNoMnemonicErrorVisible: Boolean = false
) : UiState
