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
import com.babylon.wallet.android.domain.usecases.BuildUnauthorizedDappResponseUseCase
import com.babylon.wallet.android.domain.usecases.RespondToIncomingRequestUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.dapp.FailureDialogState
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.PersonaUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.toUiModel
import com.babylon.wallet.android.presentation.dapp.unauthorized.InitialUnauthorizedLoginRoute
import com.babylon.wallet.android.presentation.dapp.unauthorized.verifyentities.EntitiesForProofWithSignatures
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.DappWalletInteractionErrorType
import com.radixdlt.sargon.IdentityAddress
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.PersonaData
import com.radixdlt.sargon.SignatureWithPublicKey
import com.radixdlt.sargon.extensions.ProfileEntity
import com.radixdlt.sargon.extensions.init
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.domain.DApp
import rdx.works.core.logNonFatalException
import rdx.works.core.sargon.activeAccountOnCurrentNetwork
import rdx.works.core.sargon.activePersonaOnCurrentNetwork
import rdx.works.core.sargon.allAccountsOnCurrentNetwork
import rdx.works.core.sargon.allPersonasOnCurrentNetwork
import rdx.works.core.sargon.fields
import rdx.works.core.sargon.toPersonaData
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.ProfileException
import javax.inject.Inject

/** The order of presenting the screens for a [WalletUnauthorizedRequest],
 * regardless of whether all the *RequestItems are present, is as follows:
 *
 * 1. oneTimeAccountsRequestItem: AccountsRequestItem -> OneTimeChooseAccountsScreen
 * 2. oneTimePersonaDataRequestItem: PersonaDataRequestItem -> OneTimeChoosePersonaScreen
 * 3. proofOfOwnershipRequestItem: ProofOfOwnershipRequestItem
 *      a. personaAddress: IdentityAddress -> VerifyPersonaScreen
 *      b. accountAddresses: List<AccountAddress> -> VerifyAccountsScreen
 *
 * For each screen wallet asks for signature(s) IF a challenge exists in the *RequestItem.
 *
 */
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

            // check if request contains proofOfOwnershipRequestItem
            // and if yes then validate the requested entities from this request item
            val proofOfOwnershipRequestItem = request.proofOfOwnershipRequestItem
            if (proofOfOwnershipRequestItem != null) {
                val areRequestedAddressesValid = validateRequestedAddressesForProofOfOwnership(
                    requestedPersonaAddress = proofOfOwnershipRequestItem.personaAddress,
                    requestedAccountAddresses = proofOfOwnershipRequestItem.accountAddresses
                )
                if (areRequestedAddressesValid) {
                    _state.update { state ->
                        state.copy(
                            personaRequiredProofOfOwnership = proofOfOwnershipRequestItem.personaAddress,
                            accountsRequiredProofOfOwnership = proofOfOwnershipRequestItem.accountAddresses
                        )
                    }
                } else {
                    handleRequestError(RadixWalletException.DappRequestException.InvalidPersonaOrAccounts)
                    return@launch
                }
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

    private suspend fun validateRequestedAddressesForProofOfOwnership(
        requestedPersonaAddress: IdentityAddress?,
        requestedAccountAddresses: List<AccountAddress>?
    ): Boolean {
        // if requestedPersonaAddress is present, then check if it exists in the allPersonasOnCurrentNetwork
        val isIdentityValid = requestedPersonaAddress?.let { address ->
            getProfileUseCase().allPersonasOnCurrentNetwork.any { it.address == address }
        } ?: true // if requestedPersonaAddress is null, assume it's valid (no need to check)

        // if requestedAccountAddresses are present, then check if all of them exist in the existingAccounts
        val areAccountsValid = requestedAccountAddresses?.let { addresses ->
            addresses.all { address ->
                getProfileUseCase().allAccountsOnCurrentNetwork.any { it.address == address }
            }
        } ?: true // if requestedAccountAddresses is null, assume it's valid (no need to check)

        return isIdentityValid && areAccountsValid
    }

    private fun setInitialDappLoginRoute() {
        val request = request
        when {
            request.oneTimeAccountsRequestItem != null -> {
                _state.update {
                    it.copy(
                        initialUnauthorizedLoginRoute = InitialUnauthorizedLoginRoute.ChooseAccount(
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

            request.proofOfOwnershipRequestItem != null -> {
                if (request.proofOfOwnershipRequestItem.personaAddress != null) {
                    _state.update { state ->
                        state.copy(
                            initialUnauthorizedLoginRoute = InitialUnauthorizedLoginRoute.VerifyPersona(
                                walletUnauthorizedRequestInteractionId = args.interactionId,
                                EntitiesForProofWithSignatures(
                                    personaAddress = state.personaRequiredProofOfOwnership,
                                    accountAddresses = state.accountsRequiredProofOfOwnership.orEmpty()
                                )
                            )
                        )
                    }
                } else if (request.proofOfOwnershipRequestItem.accountAddresses != null) {
                    _state.update { state ->
                        state.copy(
                            initialUnauthorizedLoginRoute = InitialUnauthorizedLoginRoute.VerifyAccounts(
                                walletUnauthorizedRequestInteractionId = args.interactionId,
                                EntitiesForProofWithSignatures(
                                    accountAddresses = state.accountsRequiredProofOfOwnership.orEmpty()
                                )
                            )
                        )
                    }
                }
            }

            else -> onUserRejectedRequest()
        }
    }

    fun onAccountsSelected(onetimeAccounts: List<AccountItemUiModel>) {
        viewModelScope.launch {
            _state.update { it.copy(selectedAccountsOneTime = onetimeAccounts.toPersistentList()) }
            val request = request
            if (request.oneTimePersonaDataRequestItem != null) {
                sendEvent(Event.NavigateToOneTimeChoosePersona(request.oneTimePersonaDataRequestItem.toRequiredFields()))
            } else if (request.proofOfOwnershipRequestItem != null) {
                navigateToVerifyEntities(proofOfOwnershipRequestItem = request.proofOfOwnershipRequestItem)
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

            val request = request
            if (request.proofOfOwnershipRequestItem != null) {
                navigateToVerifyEntities(proofOfOwnershipRequestItem = request.proofOfOwnershipRequestItem)
            } else {
                sendResponseToDapp()
            }
        }
    }

    fun onRequestedEntitiesVerified(entitiesWithSignatures: Map<ProfileEntity, SignatureWithPublicKey>) {
        _state.update { state ->
            state.copy(verifiedEntities = entitiesWithSignatures)
        }
        sendResponseToDapp()
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

    private suspend fun navigateToVerifyEntities(proofOfOwnershipRequestItem: WalletUnauthorizedRequest.ProofOfOwnershipRequestItem) {
        if (proofOfOwnershipRequestItem.personaAddress != null) {
            sendEvent(
                Event.NavigateToVerifyPersona(
                    walletUnauthorizedRequestInteractionId = request.interactionId,
                    entitiesForProofWithSignatures = EntitiesForProofWithSignatures(
                        personaAddress = state.value.personaRequiredProofOfOwnership,
                        accountAddresses = state.value.accountsRequiredProofOfOwnership.orEmpty()
                    )
                )
            )
        } else if (proofOfOwnershipRequestItem.accountAddresses != null) {
            sendEvent(
                Event.NavigateToVerifyAccounts(
                    walletUnauthorizedRequestInteractionId = request.interactionId,
                    entitiesForProofWithSignatures = EntitiesForProofWithSignatures(
                        accountAddresses = state.value.accountsRequiredProofOfOwnership.orEmpty()
                    )
                )
            )
        }
    }

    private fun sendResponseToDapp() {
        viewModelScope.launch {
            buildUnauthorizedDappResponseUseCase(
                request = request,
                oneTimeAccounts = state.value.selectedAccountsOneTime.mapNotNull {
                    getProfileUseCase().activeAccountOnCurrentNetwork(it.address)
                },
                oneTimePersonaData = state.value.selectedPersonaData,
                verifiedEntities = state.value.verifiedEntities
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
}

sealed interface Event : OneOffEvent {

    data object CloseLoginFlow : Event

    data object LoginFlowCompleted : Event

    data class NavigateToOneTimeChoosePersona(val requiredPersonaFields: RequiredPersonaFields) : Event

    data class NavigateToVerifyPersona(
        val walletUnauthorizedRequestInteractionId: String,
        val entitiesForProofWithSignatures: EntitiesForProofWithSignatures
    ) : Event

    data class NavigateToVerifyAccounts(
        val walletUnauthorizedRequestInteractionId: String,
        val entitiesForProofWithSignatures: EntitiesForProofWithSignatures
    ) : Event
}

data class DAppUnauthorizedLoginUiState(
    val dapp: DApp? = null,
    val uiMessage: UiMessage? = null,
    val failureDialogState: FailureDialogState = FailureDialogState.Closed,
    val initialUnauthorizedLoginRoute: InitialUnauthorizedLoginRoute? = null,
    val selectedPersonaData: PersonaData? = null,
    val selectedAccountsOneTime: ImmutableList<AccountItemUiModel> = persistentListOf(),
    val selectedPersona: PersonaUiModel? = null,
    val personaRequiredProofOfOwnership: IdentityAddress? = null,
    val accountsRequiredProofOfOwnership: List<AccountAddress>? = null,
    val verifiedEntities: Map<ProfileEntity, SignatureWithPublicKey> = emptyMap(),
    val isNoMnemonicErrorVisible: Boolean = false
) : UiState
