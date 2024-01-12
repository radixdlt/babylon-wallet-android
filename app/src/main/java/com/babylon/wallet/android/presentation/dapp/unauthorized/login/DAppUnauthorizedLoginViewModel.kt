package com.babylon.wallet.android.presentation.dapp.unauthorized.login

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.DappMessenger
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.model.WalletErrorType
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.toRequiredFields
import com.babylon.wallet.android.domain.usecases.BuildUnauthorizedDappResponseUseCase
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.dapp.DAppLoginDelegate
import com.babylon.wallet.android.presentation.dapp.DAppLoginEvent
import com.babylon.wallet.android.presentation.dapp.DAppLoginUiState
import com.babylon.wallet.android.presentation.dapp.InitialUnauthorizedLoginRoute
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.toUiModel
import com.babylon.wallet.android.presentation.model.toPersonaData
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork
import rdx.works.profile.domain.personaOnCurrentNetwork
import javax.inject.Inject

@HiltViewModel
@Suppress("LongParameterList", "TooManyFunctions")
class DAppUnauthorizedLoginViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dAppMessenger: DappMessenger,
    private val appEventBus: AppEventBus,
    private val getProfileUseCase: GetProfileUseCase,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val buildUnauthorizedDappResponseUseCase: BuildUnauthorizedDappResponseUseCase,
    private val dAppLoginDelegate: DAppLoginDelegate
) : StateViewModel<DAppLoginUiState>(),
    OneOffEventHandler<DAppLoginEvent> by OneOffEventHandlerImpl() {

    private val args = DAppUnauthorizedLoginArgs(savedStateHandle)

    private lateinit var request: MessageFromDataChannel.IncomingRequest.UnauthorizedRequest

    init {
        dAppLoginDelegate(scope = viewModelScope, state = _state)

        dAppLoginDelegate.observeSigningState(isAuthorizedRequest = false)

        viewModelScope.launch {
            dAppLoginDelegate.processLoginRequest(
                requestId = args.requestId,
                sendEvent = {
                    viewModelScope.launch { sendEvent(DAppLoginEvent.CloseLoginFlow) }
                },
                isAuthorizedRequest = true
            )
            setInitialDappLoginRoute()
        }
    }

    fun onDismissSigningStatusDialog() = dAppLoginDelegate.onDismissSigningStatusDialog()

    fun dismissNoMnemonicError() = dAppLoginDelegate.dismissNoMnemonicError()

    private fun setInitialDappLoginRoute() {
        val request = request
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

    fun onAcknowledgeFailureDialog() {
        dAppLoginDelegate.onAcknowledgeFailureDialog(
            sendEvent = {
                viewModelScope.launch { sendEvent(DAppLoginEvent.CloseLoginFlow) }
            }
        )
    }

    fun onMessageShown() = dAppLoginDelegate.onMessageShown()

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
                        selectedOneTimePersonaData = dataFields.map { it.value }.toPersonaData()
                    )
                }
                sendEvent(DAppLoginEvent.RequestCompletionBiometricPrompt(request.needSignatures()))
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
            sendEvent(DAppLoginEvent.CloseLoginFlow)
            incomingRequestRepository.requestHandled(requestId = args.requestId)
        }
    }

    fun onSelectPersona(persona: Network.Persona) {
        dAppLoginDelegate.onSelectPersona(persona)
    }

    fun onAccountsSelected(onetimeAccounts: List<AccountItemUiModel>) {
        viewModelScope.launch {
            _state.update { it.copy(selectedAccountsOneTime = onetimeAccounts.toPersistentList()) }
            val request = request
            if (request.oneTimePersonaDataRequestItem != null) {
                sendEvent(
                    DAppLoginEvent.PersonaDataOnetime(
                        request.oneTimePersonaDataRequestItem.toRequiredFields()
                    )
                )
            } else {
                sendEvent(DAppLoginEvent.RequestCompletionBiometricPrompt(request.needSignatures()))
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
                onetimeSharedPersonaData = state.value.selectedOneTimePersonaData,
                deviceBiometricAuthenticationProvider = deviceBiometricAuthenticationProvider
            ).onSuccess {
                dAppMessenger.sendWalletInteractionSuccessResponse(
                    remoteConnectorId = request.remoteConnectorId,
                    response = it
                )
                sendEvent(DAppLoginEvent.LoginFlowCompleted)
                appEventBus.sendEvent(
                    AppEvent.Status.DappInteraction(
                        requestId = request.id,
                        dAppName = state.value.dapp?.name
                    )
                )
            }.onFailure { exception ->
                dAppLoginDelegate.handleRequestError(
                    exception = exception,
                    isAuthorizedRequest = false
                )
            }
        }
    }

    override fun initialState(): DAppLoginUiState {
        return DAppLoginUiState()
    }
}
