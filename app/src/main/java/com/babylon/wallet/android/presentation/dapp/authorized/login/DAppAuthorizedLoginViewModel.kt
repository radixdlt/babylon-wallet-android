package com.babylon.wallet.android.presentation.dapp.authorized.login

import InitialAuthorizedLoginRoute
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.DappMessenger
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.model.WalletErrorType
import com.babylon.wallet.android.data.dapp.model.toKind
import com.babylon.wallet.android.data.repository.dappmetadata.DAppRepository
import com.babylon.wallet.android.data.transaction.DappRequestException
import com.babylon.wallet.android.data.transaction.DappRequestFailure
import com.babylon.wallet.android.data.transaction.SigningState
import com.babylon.wallet.android.domain.common.onError
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.model.DAppWithMetadata
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.MessageFromDataChannel.IncomingRequest.AccountsRequestItem
import com.babylon.wallet.android.domain.model.MessageFromDataChannel.IncomingRequest.AuthorizedRequest
import com.babylon.wallet.android.domain.model.toProfileShareAccountsQuantifier
import com.babylon.wallet.android.domain.usecases.BuildAuthorizedDappResponseUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.account.toUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.PersonaUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.toUiModel
import com.babylon.wallet.android.presentation.model.encodeToString
import com.babylon.wallet.android.utils.toISO8601String
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.Network.AuthorizedDapp.AuthorizedPersonaSimple
import rdx.works.profile.data.model.pernetwork.filterFields
import rdx.works.profile.data.repository.DAppConnectionRepository
import rdx.works.profile.data.repository.addOrUpdateAuthorizedDappPersona
import rdx.works.profile.data.repository.updateAuthorizedDappPersonaFields
import rdx.works.profile.data.repository.updateAuthorizedDappPersonas
import rdx.works.profile.data.repository.updateDappAuthorizedPersonaSharedAccounts
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork
import rdx.works.profile.domain.gateway.GetCurrentGatewayUseCase
import rdx.works.profile.domain.personaOnCurrentNetwork
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
@Suppress("LongParameterList", "TooManyFunctions", "LongMethod", "LargeClass")
class DAppAuthorizedLoginViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dAppMessenger: DappMessenger,
    private val dAppConnectionRepository: DAppConnectionRepository,
    private val getProfileUseCase: GetProfileUseCase,
    private val getCurrentGatewayUseCase: GetCurrentGatewayUseCase,
    private val dAppRepository: DAppRepository,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val buildAuthorizedDappResponseUseCase: BuildAuthorizedDappResponseUseCase,
) : StateViewModel<DAppLoginUiState>(),
    OneOffEventHandler<Event> by OneOffEventHandlerImpl() {

    private val args = DAppAuthorizedLoginArgs(savedStateHandle)

    private val mutex = Mutex()

    private val request = incomingRequestRepository.getAuthorizedRequest(
        args.interactionId
    )

    private var authorizedDapp: Network.AuthorizedDapp? = null
    private var editedDapp: Network.AuthorizedDapp? = null

    override fun initialState(): DAppLoginUiState = DAppLoginUiState()

    init {
        observeSigningState()
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
            authorizedDapp = dAppConnectionRepository.getAuthorizedDapp(
                request.requestMetadata.dAppDefinitionAddress
            )
            editedDapp = authorizedDapp
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
                _state.update { it.copy(uiMessage = UiMessage.ErrorMessage(error)) }
            }
            setInitialDappLoginRoute()
        }
    }

    private fun observeSigningState() {
        viewModelScope.launch {
            buildAuthorizedDappResponseUseCase.signingState.filterNotNull().collect { signingState ->
                // TODO verify how we should show signing state in persona login flow
//                _state.update { state ->
//                    state.copy(signingState = signingState)
//                }
            }
        }
    }

    private suspend fun setInitialDappLoginRoute() {
        when (val request = request.authRequest) {
            is AuthorizedRequest.AuthRequest.UsePersonaRequest -> {
                val dapp = authorizedDapp
                if (dapp != null) {
                    setInitialDappLoginRouteForUsePersonaRequest(dapp, request)
                } else {
                    onAbortDappLogin(WalletErrorType.InvalidPersona)
                }
            }

            else -> {
                _state.update {
                    it.copy(
                        initialAuthorizedLoginRoute = InitialAuthorizedLoginRoute.SelectPersona(
                            args.interactionId
                        )
                    )
                }
            }
        }
    }

    @Suppress("LongMethod")
    private suspend fun setInitialDappLoginRouteForUsePersonaRequest(
        dapp: Network.AuthorizedDapp,
        authRequest: AuthorizedRequest.AuthRequest.UsePersonaRequest
    ) {
        val hasAuthorizedPersona = dapp.hasAuthorizedPersona(
            authRequest.personaAddress
        )
        if (hasAuthorizedPersona) {
            val resetAccounts = request.resetRequestItem?.accounts == true
            val resetPersonaData = request.resetRequestItem?.personaData == true
            val persona =
                checkNotNull(getProfileUseCase.personaOnCurrentNetwork(authRequest.personaAddress))
            onSelectPersona(persona)
            val ongoingAccountsRequestItem = request.ongoingAccountsRequestItem
            val oneTimeAccountsRequestItem = request.oneTimeAccountsRequestItem
            val ongoingPersonaDataRequestItem = request.ongoingPersonaDataRequestItem
            val oneTimePersonaDataRequestItem = request.oneTimePersonaDataRequestItem
            val requestedAccountsAlreadyGranted = requestedAccountsPermissionAlreadyGranted(
                authRequest.personaAddress,
                ongoingAccountsRequestItem
            )
            val requestedDataAlreadyGranted =
                personaDataAccessAlreadyGranted(ongoingPersonaDataRequestItem, persona.address)
            when {
                ongoingAccountsRequestItem != null && (!requestedAccountsAlreadyGranted || resetAccounts) -> {
                    _state.update {
                        it.copy(
                            initialAuthorizedLoginRoute = InitialAuthorizedLoginRoute.Permission(
                                ongoingAccountsRequestItem.numberOfAccounts,
                                isExactAccountsCount = ongoingAccountsRequestItem.quantifier.exactly()
                            )
                        )
                    }
                }

                ongoingPersonaDataRequestItem != null &&
                    ongoingPersonaDataRequestItem.isValid() && (!requestedDataAlreadyGranted || resetPersonaData) -> {
                    _state.update { state ->
                        state.copy(
                            initialAuthorizedLoginRoute = InitialAuthorizedLoginRoute.OngoingPersonaData(
                                authRequest.personaAddress,
                                ongoingPersonaDataRequestItem.fields.map { it.toKind() }.encodeToString()
                            )
                        )
                    }
                }

                oneTimeAccountsRequestItem != null -> {
                    _state.update {
                        it.copy(
                            initialAuthorizedLoginRoute = InitialAuthorizedLoginRoute.ChooseAccount(
                                oneTimeAccountsRequestItem.numberOfAccounts,
                                isExactAccountsCount = oneTimeAccountsRequestItem.quantifier.exactly(),
                                oneTime = true
                            )
                        )
                    }
                }

                oneTimePersonaDataRequestItem != null && oneTimePersonaDataRequestItem.isValid() -> {
                    _state.update { state ->
                        state.copy(
                            initialAuthorizedLoginRoute = InitialAuthorizedLoginRoute.OneTimePersonaData(
                                oneTimePersonaDataRequestItem.fields.map { it.toKind() }.encodeToString()
                            )
                        )
                    }
                }
            }
        } else {
            onAbortDappLogin(WalletErrorType.InvalidPersona)
        }
    }

    @Suppress("MagicNumber")
    private suspend fun handleRequestError(failure: DappRequestFailure) {
        _state.update { it.copy(uiMessage = UiMessage.ErrorMessage(DappRequestException(failure))) }
        dAppMessenger.sendWalletInteractionResponseFailure(
            request.dappId,
            args.interactionId,
            failure.toWalletErrorType(),
            failure.getDappMessage()
        )
        delay(2000)
        sendEvent(Event.RejectLogin)
        incomingRequestRepository.requestHandled(requestId = args.interactionId)
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    fun personaSelectionConfirmed() {
        viewModelScope.launch {
            val selectedPersona = state.value.selectedPersona?.persona
            requireNotNull(selectedPersona)
            updateOrCreateAuthorizedDappWithSelectedPersona(selectedPersona)
            handleNextRequestItem(selectedPersona)
        }
    }

    private suspend fun handleNextRequestItem(selectedPersona: Network.Persona) {
        when {
            request.hasOnlyAuthItem() -> {
                promptForBiometrics()
            }

            request.ongoingAccountsRequestItem != null -> {
                handleOngoingAddressRequestItem(
                    request.ongoingAccountsRequestItem,
                    selectedPersona.address
                )
            }

            request.ongoingPersonaDataRequestItem != null && request.ongoingPersonaDataRequestItem.isValid() -> {
                handleOngoingPersonaDataRequestItem(
                    selectedPersona.address,
                    request.ongoingPersonaDataRequestItem
                )
            }

            request.oneTimeAccountsRequestItem != null -> {
                handleOneTimeAccountRequestItem(request.oneTimeAccountsRequestItem)
            }

            request.oneTimePersonaDataRequestItem != null && request.oneTimePersonaDataRequestItem.isValid() -> {
                handleOneTimePersonaDataRequestItem(request.oneTimePersonaDataRequestItem)
            }

            else -> {
                promptForBiometrics()
            }
        }
    }

    private suspend fun handleOneTimeAccountRequestItem(
        oneTimeAccountsRequestItem: AccountsRequestItem
    ) {
        val numberOfAccounts = oneTimeAccountsRequestItem.numberOfAccounts
        val isExactAccountsCount = oneTimeAccountsRequestItem.quantifier.exactly()
        sendEvent(
            Event.ChooseAccounts(
                numberOfAccounts,
                isExactAccountsCount,
                oneTime = true
            )
        )
    }

    fun onGrantedPersonaDataOngoing() {
        viewModelScope.launch {
            val requiredFields =
                checkNotNull(request.ongoingPersonaDataRequestItem?.fields?.map { it.toKind() })
            val selectedPersona = checkNotNull(state.value.selectedPersona)
            getProfileUseCase.personaOnCurrentNetwork(selectedPersona.persona.address)
                ?.let { updatedPersona ->
                    val requiredDataFields =
                        updatedPersona.fields.filter { requiredFields.contains(it.id) }
                    _state.update {
                        it.copy(
                            selectedPersona = updatedPersona.toUiModel(),
                            selectedOngoingDataFields = requiredDataFields
                        )
                    }
                    mutex.withLock {
                        editedDapp = editedDapp?.updateAuthorizedDappPersonaFields(
                            personaAddress = updatedPersona.address,
                            allExistingFieldIds = updatedPersona.fields.map { it.id },
                            requestedFieldIds = requiredDataFields.map { it.id }
                        )
                    }
                    handleNextOneTimeRequestItem()
                }
        }
    }

    fun onGrantedPersonaDataOnetime(persona: Network.Persona) {
        viewModelScope.launch {
            val requiredFields =
                checkNotNull(request.oneTimePersonaDataRequestItem?.fields?.map { it.toKind() })
            val requiredDataFields = persona.fields.filter { requiredFields.contains(it.id) }
            _state.update {
                it.copy(
                    selectedOnetimeDataFields = requiredDataFields
                )
            }
            promptForBiometrics()
        }
    }

    private suspend fun requestedAccountsPermissionAlreadyGranted(
        personaAddress: String,
        accountsRequestItem: AccountsRequestItem?
    ): Boolean {
        if (accountsRequestItem == null) return false
        return authorizedDapp?.let { dapp ->
            val potentialOngoingAddresses =
                dAppConnectionRepository.dAppAuthorizedPersonaAccountAddresses(
                    dapp.dAppDefinitionAddress,
                    personaAddress,
                    accountsRequestItem.numberOfAccounts,
                    accountsRequestItem.quantifier.toProfileShareAccountsQuantifier()
                )
            potentialOngoingAddresses.isNotEmpty()
        } ?: false
    }

    private suspend fun handleOngoingPersonaDataRequestItem(
        personaAddress: String,
        requestItem: MessageFromDataChannel.IncomingRequest.PersonaRequestItem
    ) {
        val dapp = requireNotNull(editedDapp)
        val dataAccessAlreadyGranted = personaDataAccessAlreadyGranted(requestItem, personaAddress)
        if (request.resetRequestItem?.personaData == true || !dataAccessAlreadyGranted) {
            sendEvent(
                Event.PersonaDataOngoing(
                    personaAddress,
                    requestItem.fields.map { it.toKind() }.encodeToString()
                )
            )
        } else {
            val dataFields = getProfileUseCase.personaOnCurrentNetwork(personaAddress)
                ?.filterFields(requestItem.fields.map { it.toKind() }).orEmpty()
            _state.update { it.copy(selectedOngoingDataFields = dataFields) }
            mutex.withLock {
                editedDapp =
                    editedDapp?.updateAuthorizedDappPersonas(
                        dapp.referencesToAuthorizedPersonas.map { ref ->
                            if (ref.identityAddress == personaAddress) {
                                ref.copy(lastLogin = LocalDateTime.now().toISO8601String())
                            } else {
                                ref
                            }
                        }
                    )
            }
            handleNextOneTimeRequestItem()
        }
    }

    private suspend fun handleNextOneTimeRequestItem() {
        when {
            request.oneTimeAccountsRequestItem != null -> handleOneTimeAccountRequestItem(
                request.oneTimeAccountsRequestItem
            )

            request.oneTimePersonaDataRequestItem != null && request.oneTimePersonaDataRequestItem.isValid() -> {
                handleOneTimePersonaDataRequestItem(request.oneTimePersonaDataRequestItem)
            }

            else -> promptForBiometrics()
        }
    }

    private suspend fun personaDataAccessAlreadyGranted(
        requestItem: MessageFromDataChannel.IncomingRequest.PersonaRequestItem?,
        personaAddress: String
    ): Boolean {
        if (requestItem == null) return false
        val dapp = requireNotNull(editedDapp)
        val requestedFieldsCount = requestItem.fields.size
        val requestedFieldKinds = requestItem.fields.map { it.toKind() }
        val personaFields = getProfileUseCase.personaOnCurrentNetwork(personaAddress)?.fields.orEmpty()
        val requestedFieldsIds =
            personaFields.filter { requestedFieldKinds.contains(it.id) }.map { it.id }
        return requestedFieldsCount == requestedFieldsIds.size && dAppConnectionRepository.dAppAuthorizedPersonaHasAllDataFields(
            dapp.dAppDefinitionAddress,
            personaAddress,
            requestedFieldsIds
        )
    }

    private fun handleOneTimePersonaDataRequestItem(oneTimePersonaRequestItem: MessageFromDataChannel.IncomingRequest.PersonaRequestItem) {
        viewModelScope.launch {
            sendEvent(
                Event.PersonaDataOnetime(
                    oneTimePersonaRequestItem.fields.map { it.toKind() }.encodeToString()
                )
            )
        }
    }

    private suspend fun handleOngoingAddressRequestItem(
        ongoingAccountsRequestItem: AccountsRequestItem,
        personaAddress: String
    ) {
        val dapp = requireNotNull(editedDapp)
        val numberOfAccounts = ongoingAccountsRequestItem.numberOfAccounts
        val isExactAccountsCount = ongoingAccountsRequestItem.quantifier.exactly()
        val potentialOngoingAddresses =
            dAppConnectionRepository.dAppAuthorizedPersonaAccountAddresses(
                dapp.dAppDefinitionAddress,
                personaAddress,
                numberOfAccounts,
                ongoingAccountsRequestItem.quantifier.toProfileShareAccountsQuantifier()
            )
        if (request.resetRequestItem?.accounts == true || potentialOngoingAddresses.isEmpty()) {
            sendEvent(
                Event.DisplayPermission(
                    numberOfAccounts,
                    isExactAccountsCount
                )
            )
        } else {
            val selectedAccounts = potentialOngoingAddresses.mapNotNull {
                getProfileUseCase.accountOnCurrentNetwork(it)?.toUiModel(true)
            }
            _state.update { it.copy(selectedAccountsOngoing = selectedAccounts) }
            mutex.withLock {
                editedDapp =
                    editedDapp?.updateAuthorizedDappPersonas(
                        dapp.referencesToAuthorizedPersonas.map { ref ->
                            if (ref.identityAddress == personaAddress) {
                                ref.copy(lastLogin = LocalDateTime.now().toISO8601String())
                            } else {
                                ref
                            }
                        }
                    )
            }
            when {
                request.ongoingPersonaDataRequestItem != null && request.ongoingPersonaDataRequestItem.isValid() -> {
                    handleOngoingPersonaDataRequestItem(
                        personaAddress,
                        request.ongoingPersonaDataRequestItem
                    )
                }

                request.oneTimeAccountsRequestItem != null -> handleOneTimeAccountRequestItem(
                    request.oneTimeAccountsRequestItem
                )

                request.oneTimePersonaDataRequestItem != null && request.oneTimePersonaDataRequestItem.isValid() -> {
                    handleOneTimePersonaDataRequestItem(request.oneTimePersonaDataRequestItem)
                }

                else -> promptForBiometrics()
            }
        }
    }

    private suspend fun updateOrCreateAuthorizedDappWithSelectedPersona(selectedPersona: Network.Persona) {
        val dApp = authorizedDapp
        val date = LocalDateTime.now().toISO8601String()
        if (dApp == null) {
            val dAppName = state.value.dappWithMetadata?.name.orEmpty().ifEmpty { "Unknown dApp" }
            mutex.withLock {
                editedDapp = Network.AuthorizedDapp(
                    request.metadata.networkId,
                    request.metadata.dAppDefinitionAddress,
                    dAppName,
                    listOf(
                        AuthorizedPersonaSimple(
                            identityAddress = selectedPersona.address,
                            fieldIDs = emptyList(),
                            lastLogin = date,
                            sharedAccounts = AuthorizedPersonaSimple.SharedAccounts(
                                emptyList(),
                                request = AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts(
                                    AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts.Quantifier.Exactly,
                                    0
                                )
                            )
                        )
                    )
                )
            }
        } else {
            mutex.withLock {
                editedDapp = authorizedDapp?.addOrUpdateAuthorizedDappPersona(selectedPersona, date)
            }
        }
    }

    fun onAbortDappLogin(walletWalletErrorType: WalletErrorType = WalletErrorType.RejectedByUser) {
        viewModelScope.launch {
            if (request.isInternalRequest()) {
                incomingRequestRepository.requestHandled(request.id)
            } else {
                dAppMessenger.sendWalletInteractionResponseFailure(
                    request.dappId,
                    args.interactionId,
                    error = walletWalletErrorType
                )
            }
            sendEvent(Event.RejectLogin)
            incomingRequestRepository.requestHandled(requestId = args.interactionId)
        }
    }

    fun onSelectPersona(persona: Network.Persona) {
        _state.update { it.copy(selectedPersona = persona.toUiModel()) }
    }

    fun onPermissionGranted(
        numberOfAccounts: Int,
        isExactAccountsCount: Boolean,
        isOneTime: Boolean
    ) {
        viewModelScope.launch {
            sendEvent(
                Event.ChooseAccounts(
                    numberOfAccounts = numberOfAccounts,
                    isExactAccountsCount = isExactAccountsCount,
                    oneTime = isOneTime
                )
            )
        }
    }

    fun onAccountsSelected(selectedAccounts: List<AccountItemUiModel>, oneTimeRequest: Boolean) {
        val selectedPersona = state.value.selectedPersona?.persona
        requireNotNull(selectedPersona)
        val handledRequest = if (oneTimeRequest) {
            request.oneTimeAccountsRequestItem
        } else {
            request.ongoingAccountsRequestItem
        }
        if (handledRequest?.isOngoing == true) {
            _state.update { it.copy(selectedAccountsOngoing = selectedAccounts) }
            viewModelScope.launch {
                mutex.withLock {
                    editedDapp = editedDapp?.updateDappAuthorizedPersonaSharedAccounts(
                        selectedPersona.address,
                        AuthorizedPersonaSimple.SharedAccounts(
                            selectedAccounts.map { it.address },
                            AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts(
                                handledRequest.quantifier.toProfileShareAccountsQuantifier(),
                                handledRequest.numberOfAccounts
                            )
                        )
                    )
                }
                when {
                    request.ongoingPersonaDataRequestItem != null && request.ongoingPersonaDataRequestItem.isValid() -> {
                        handleOngoingPersonaDataRequestItem(
                            selectedPersona.address,
                            request.ongoingPersonaDataRequestItem
                        )
                    }

                    request.oneTimeAccountsRequestItem != null -> {
                        handleOneTimeAccountRequestItem(request.oneTimeAccountsRequestItem)
                    }

                    request.oneTimePersonaDataRequestItem != null && request.oneTimePersonaDataRequestItem.isValid() -> {
                        handleOneTimePersonaDataRequestItem(request.oneTimePersonaDataRequestItem)
                    }

                    else -> {
                        promptForBiometrics()
                    }
                }
            }
        } else if (handledRequest?.isOngoing == false) {
            _state.update { it.copy(selectedAccountsOneTime = selectedAccounts) }
            when {
                request.oneTimePersonaDataRequestItem != null && request.oneTimePersonaDataRequestItem.isValid() -> {
                    handleOneTimePersonaDataRequestItem(request.oneTimePersonaDataRequestItem)
                }

                else -> {
                    viewModelScope.launch {
                        promptForBiometrics()
                    }
                }
            }
        }
    }

    private suspend fun promptForBiometrics() {
        sendEvent(Event.RequestCompletionBiometricPrompt)
    }

    fun sendRequestResponse() {
        viewModelScope.launch {
            val selectedPersona = state.value.selectedPersona?.persona
            requireNotNull(selectedPersona)
            if (request.isInternalRequest()) {
                incomingRequestRepository.requestHandled(request.interactionId)
            } else {
                buildAuthorizedDappResponseUseCase(
                    request,
                    selectedPersona,
                    state.value.selectedAccountsOneTime.mapNotNull { getProfileUseCase.accountOnCurrentNetwork(it.address) },
                    state.value.selectedAccountsOngoing.mapNotNull { getProfileUseCase.accountOnCurrentNetwork(it.address) },
                    state.value.selectedOngoingDataFields,
                    state.value.selectedOnetimeDataFields
                ).onSuccess { response ->
                    dAppMessenger.sendWalletInteractionAuthorizedSuccessResponse(dappId = request.dappId, response = response)
                    mutex.withLock {
                        editedDapp?.let { dAppConnectionRepository.updateOrCreateAuthorizedDApp(it) }
                    }
                    sendEvent(
                        Event.LoginFlowCompleted(
                            requestId = request.interactionId,
                            dAppName = state.value.dappWithMetadata?.name.orEmpty(),
                            showSuccessDialog = !request.isInternalRequest()
                        )
                    )
                }.onFailure { throwable ->
                    if (throwable is DappRequestFailure) {
                        handleRequestError(throwable)
                    }
                }
            }
        }
    }
}

sealed interface Event : OneOffEvent {

    object RejectLogin : Event
    object RequestCompletionBiometricPrompt : Event

    data class LoginFlowCompleted(
        val requestId: String,
        val dAppName: String,
        val showSuccessDialog: Boolean = true
    ) : Event

    data class DisplayPermission(
        val numberOfAccounts: Int,
        val isExactAccountsCount: Boolean,
        val oneTime: Boolean = false
    ) : Event

    data class PersonaDataOngoing(
        val personaAddress: String,
        val requiredFieldsEncoded: String
    ) : Event

    data class PersonaDataOnetime(val requiredFieldsEncoded: String) : Event

    data class ChooseAccounts(
        val numberOfAccounts: Int,
        val isExactAccountsCount: Boolean,
        val oneTime: Boolean = false,
        val showBack: Boolean = true
    ) : Event
}

data class DAppLoginUiState(
    val dappWithMetadata: DAppWithMetadata? = null,
    val uiMessage: UiMessage? = null,
    val initialAuthorizedLoginRoute: InitialAuthorizedLoginRoute? = null,
    val selectedAccountsOngoing: List<AccountItemUiModel> = emptyList(),
    val selectedOngoingDataFields: List<Network.Persona.Field> = emptyList(),
    val selectedOnetimeDataFields: List<Network.Persona.Field> = emptyList(),
    val selectedAccountsOneTime: List<AccountItemUiModel> = emptyList(),
    val selectedPersona: PersonaUiModel? = null,
    val signingState: SigningState? = null
) : UiState
