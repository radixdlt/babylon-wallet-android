package com.babylon.wallet.android.presentation.dapp.authorized.login

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
import com.babylon.wallet.android.domain.model.MessageFromDataChannel.IncomingRequest.AccountsRequestItem
import com.babylon.wallet.android.domain.model.MessageFromDataChannel.IncomingRequest.AuthorizedRequest
import com.babylon.wallet.android.domain.model.RequiredPersonaFields
import com.babylon.wallet.android.domain.model.toProfileShareAccountsQuantifier
import com.babylon.wallet.android.domain.model.toRequiredFields
import com.babylon.wallet.android.domain.usecases.BuildAuthorizedDappResponseUseCase
import com.babylon.wallet.android.domain.usecases.transaction.SignatureCancelledException
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.dapp.InitialAuthorizedLoginRoute
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.account.toUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.PersonaUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.toUiModel
import com.babylon.wallet.android.presentation.model.getPersonaDataForFieldKinds
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.babylon.wallet.android.utils.toISO8601String
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.Network.AuthorizedDapp.AuthorizedPersonaSimple
import rdx.works.profile.data.model.pernetwork.PersonaData
import rdx.works.profile.data.model.pernetwork.RequestedNumber
import rdx.works.profile.data.model.pernetwork.Shared
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
    private val appEventBus: AppEventBus,
    private val dAppMessenger: DappMessenger,
    private val dAppConnectionRepository: DAppConnectionRepository,
    private val getProfileUseCase: GetProfileUseCase,
    private val getCurrentGatewayUseCase: GetCurrentGatewayUseCase,
    private val dAppRepository: DAppRepository,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val buildAuthorizedDappResponseUseCase: BuildAuthorizedDappResponseUseCase
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
            authorizedDapp = dAppConnectionRepository.getAuthorizedDapp(
                request.requestMetadata.dAppDefinitionAddress
            )
            editedDapp = authorizedDapp
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
            buildAuthorizedDappResponseUseCase.signingState.collect { signingState ->
                _state.update { state ->
                    state.copy(interactionState = signingState)
                }
            }
        }
    }

    fun onDismissSigningStatusDialog() {
        _state.update { it.copy(interactionState = null) }
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
                                ongoingAccountsRequestItem.numberOfValues.quantity,
                                isExactAccountsCount = ongoingAccountsRequestItem.numberOfValues.exactly()
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
                                ongoingPersonaDataRequestItem.toRequiredFields()
                            )
                        )
                    }
                }

                oneTimeAccountsRequestItem != null -> {
                    _state.update {
                        it.copy(
                            initialAuthorizedLoginRoute = InitialAuthorizedLoginRoute.ChooseAccount(
                                oneTimeAccountsRequestItem.numberOfValues.quantity,
                                isExactAccountsCount = oneTimeAccountsRequestItem.numberOfValues.exactly(),
                                oneTime = true
                            )
                        )
                    }
                }

                oneTimePersonaDataRequestItem != null && oneTimePersonaDataRequestItem.isValid() -> {
                    _state.update { state ->
                        state.copy(
                            initialAuthorizedLoginRoute = InitialAuthorizedLoginRoute.OneTimePersonaData(
                                oneTimePersonaDataRequestItem.toRequiredFields()
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
    private suspend fun handleRequestError(exception: DappRequestException) {
        if (exception.e is SignatureCancelledException) {
            return
        }
        val failure = exception.failure
        _state.update { it.copy(uiMessage = UiMessage.ErrorMessage.from(DappRequestException(failure))) }
        dAppMessenger.sendWalletInteractionResponseFailure(
            remoteConnectorId = request.remoteConnectorId,
            requestId = args.interactionId,
            error = failure.toWalletErrorType(),
            message = failure.getDappMessage()
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
        val numberOfAccounts = oneTimeAccountsRequestItem.numberOfValues.quantity
        val isExactAccountsCount = oneTimeAccountsRequestItem.numberOfValues.exactly()
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
                checkNotNull(request.ongoingPersonaDataRequestItem?.toRequiredFields())
            val selectedPersona = checkNotNull(state.value.selectedPersona)
            getProfileUseCase.personaOnCurrentNetwork(selectedPersona.persona.address)
                ?.let { updatedPersona ->
                    val grantedPersonaData = updatedPersona.getPersonaDataForFieldKinds(requiredFields.fields)
                    _state.update {
                        it.copy(
                            selectedPersona = updatedPersona.toUiModel(),
                            selectedOngoingPersonaData = grantedPersonaData
                        )
                    }
                    mutex.withLock {
                        editedDapp = editedDapp?.updateAuthorizedDappPersonaFields(
                            personaAddress = updatedPersona.address,
                            personaData = grantedPersonaData,
                            requiredFields = requiredFields.fields.associate { it.kind to it.numberOfValues.quantity }
                        )
                    }
                    handleNextOneTimeRequestItem()
                }
        }
    }

    fun onGrantedPersonaDataOnetime(persona: Network.Persona) {
        viewModelScope.launch {
            val requiredFields =
                checkNotNull(request.oneTimePersonaDataRequestItem?.toRequiredFields())
            val sharedPersonaData = persona.getPersonaDataForFieldKinds(requiredPersonaFields = requiredFields.fields)
            _state.update {
                it.copy(
                    selectedOnetimePersonaData = sharedPersonaData
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
                    accountsRequestItem.numberOfValues.quantity,
                    accountsRequestItem.numberOfValues.toProfileShareAccountsQuantifier()
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
                    requestItem.toRequiredFields()
                )
            )
        } else {
            val requestedFields = requestItem.toRequiredFields()
            val persona = requireNotNull(getProfileUseCase.personaOnCurrentNetwork(personaAddress))
            val sharedPersonaData = persona.getPersonaDataForFieldKinds(requestedFields.fields)
            _state.update { it.copy(selectedOngoingPersonaData = sharedPersonaData) }
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
        val requestedFieldKinds = requestItem.toRequiredFields()
        return dAppConnectionRepository.dAppAuthorizedPersonaHasAllDataFields(
            dapp.dAppDefinitionAddress,
            personaAddress,
            requestedFieldKinds.fields.associate { it.kind to it.numberOfValues.quantity }
        )
    }

    private fun handleOneTimePersonaDataRequestItem(oneTimePersonaRequestItem: MessageFromDataChannel.IncomingRequest.PersonaRequestItem) {
        viewModelScope.launch {
            sendEvent(
                Event.PersonaDataOnetime(
                    oneTimePersonaRequestItem.toRequiredFields()
                )
            )
        }
    }

    private suspend fun handleOngoingAddressRequestItem(
        ongoingAccountsRequestItem: AccountsRequestItem,
        personaAddress: String
    ) {
        val dapp = requireNotNull(editedDapp)
        val numberOfAccounts = ongoingAccountsRequestItem.numberOfValues.quantity
        val isExactAccountsCount = ongoingAccountsRequestItem.numberOfValues.exactly()
        val potentialOngoingAddresses =
            dAppConnectionRepository.dAppAuthorizedPersonaAccountAddresses(
                dapp.dAppDefinitionAddress,
                personaAddress,
                numberOfAccounts,
                ongoingAccountsRequestItem.numberOfValues.toProfileShareAccountsQuantifier()
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
            // TODO do we really need to save the dapp with "Unknown dApp"? Seems wrong!
            val dAppName = state.value.dappWithMetadata?.name.orEmpty().ifEmpty { "Unknown dApp" }
            mutex.withLock {
                editedDapp = Network.AuthorizedDapp(
                    request.metadata.networkId,
                    request.metadata.dAppDefinitionAddress,
                    dAppName,
                    listOf(
                        AuthorizedPersonaSimple(
                            identityAddress = selectedPersona.address,
                            lastLogin = date,
                            sharedAccounts = Shared(
                                emptyList(),
                                request = RequestedNumber(
                                    RequestedNumber.Quantifier.Exactly,
                                    0
                                )
                            ),
                            sharedPersonaData = Network.AuthorizedDapp.SharedPersonaData()
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
                    remoteConnectorId = request.remoteConnectorId,
                    requestId = args.interactionId,
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
                        Shared(
                            selectedAccounts.map { it.address },
                            RequestedNumber(
                                handledRequest.numberOfValues.toProfileShareAccountsQuantifier(),
                                handledRequest.numberOfValues.quantity
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
        sendEvent(Event.RequestCompletionBiometricPrompt(request.needSignatures()))
    }

    fun completeRequestHandling(deviceBiometricAuthenticationProvider: suspend () -> Boolean = { true }) {
        viewModelScope.launch {
            val selectedPersona = state.value.selectedPersona?.persona
            requireNotNull(selectedPersona)
            if (request.isInternalRequest()) {
                mutex.withLock {
                    editedDapp?.let { dAppConnectionRepository.updateOrCreateAuthorizedDApp(it) }
                }
                incomingRequestRepository.requestHandled(request.interactionId)
                sendEvent(Event.LoginFlowCompleted)
            } else {
                buildAuthorizedDappResponseUseCase(
                    request = request,
                    selectedPersona = selectedPersona,
                    oneTimeAccounts = state.value.selectedAccountsOneTime.mapNotNull {
                        getProfileUseCase.accountOnCurrentNetwork(it.address)
                    },
                    ongoingAccounts = state.value.selectedAccountsOngoing.mapNotNull {
                        getProfileUseCase.accountOnCurrentNetwork(it.address)
                    },
                    ongoingSharedPersonaData = state.value.selectedOngoingPersonaData,
                    onetimeSharedPersonaData = state.value.selectedOnetimePersonaData,
                    deviceBiometricAuthenticationProvider = deviceBiometricAuthenticationProvider
                ).onSuccess { response ->
                    dAppMessenger.sendWalletInteractionSuccessResponse(
                        remoteConnectorId = request.remoteConnectorId,
                        response = response
                    )
                    mutex.withLock {
                        editedDapp?.let { dAppConnectionRepository.updateOrCreateAuthorizedDApp(it) }
                    }
                    sendEvent(Event.LoginFlowCompleted)
                    if (!request.isInternalRequest()) {
                        appEventBus.sendEvent(
                            AppEvent.Status.DappInteraction(
                                requestId = request.interactionId,
                                dAppName = state.value.dappWithMetadata?.name
                            )
                        )
                    }
                }.onFailure { throwable ->
                    if (throwable is DappRequestException) {
                        handleRequestError(throwable)
                    }
                }
            }
        }
    }
}

sealed interface Event : OneOffEvent {

    object RejectLogin : Event
    data class RequestCompletionBiometricPrompt(val requestDuringSigning: Boolean) : Event

    object LoginFlowCompleted : Event

    data class DisplayPermission(
        val numberOfAccounts: Int,
        val isExactAccountsCount: Boolean,
        val oneTime: Boolean = false
    ) : Event

    data class PersonaDataOngoing(
        val personaAddress: String,
        val requiredPersonaFields: RequiredPersonaFields
    ) : Event

    data class PersonaDataOnetime(val requiredPersonaFields: RequiredPersonaFields) : Event

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
    val selectedOngoingPersonaData: PersonaData? = null,
    val selectedOnetimePersonaData: PersonaData? = null,
    val selectedAccountsOneTime: List<AccountItemUiModel> = emptyList(),
    val selectedPersona: PersonaUiModel? = null,
    val interactionState: InteractionState? = null
) : UiState
