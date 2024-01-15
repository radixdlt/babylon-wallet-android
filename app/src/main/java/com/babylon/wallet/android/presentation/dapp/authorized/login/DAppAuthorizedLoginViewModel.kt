package com.babylon.wallet.android.presentation.dapp.authorized.login

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.DappMessenger
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.model.WalletErrorType
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.MessageFromDataChannel.IncomingRequest.AccountsRequestItem
import com.babylon.wallet.android.domain.model.MessageFromDataChannel.IncomingRequest.AuthorizedRequest
import com.babylon.wallet.android.domain.model.toProfileShareAccountsQuantifier
import com.babylon.wallet.android.domain.model.toRequiredFields
import com.babylon.wallet.android.domain.usecases.BuildAuthorizedDappResponseUseCase
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.dapp.DAppLoginDelegate
import com.babylon.wallet.android.presentation.dapp.DAppLoginEvent
import com.babylon.wallet.android.presentation.dapp.DAppLoginUiState
import com.babylon.wallet.android.presentation.dapp.InitialAuthorizedLoginRoute
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.account.toUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.toUiModel
import com.babylon.wallet.android.presentation.model.getPersonaDataForFieldKinds
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.babylon.wallet.android.utils.toISO8601String
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.Network.AuthorizedDapp.AuthorizedPersonaSimple
import rdx.works.profile.data.model.pernetwork.RequestedNumber
import rdx.works.profile.data.model.pernetwork.Shared
import rdx.works.profile.data.repository.DAppConnectionRepository
import rdx.works.profile.data.repository.addOrUpdateAuthorizedDappPersona
import rdx.works.profile.data.repository.updateAuthorizedDappPersonaFields
import rdx.works.profile.data.repository.updateAuthorizedDappPersonas
import rdx.works.profile.data.repository.updateDappAuthorizedPersonaSharedAccounts
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork
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
    private val incomingRequestRepository: IncomingRequestRepository,
    private val buildAuthorizedDappResponseUseCase: BuildAuthorizedDappResponseUseCase,
    private val dAppLoginDelegate: DAppLoginDelegate
) : StateViewModel<DAppLoginUiState>(),
    OneOffEventHandler<DAppLoginEvent> by OneOffEventHandlerImpl() {

    private val args = DAppAuthorizedLoginArgs(savedStateHandle)

    override fun initialState(): DAppLoginUiState = DAppLoginUiState()

    init {
        dAppLoginDelegate(scope = viewModelScope, state = _state)

        dAppLoginDelegate.observeSigningState(isAuthorizedRequest = true)

        viewModelScope.launch {
            dAppLoginDelegate.processLoginRequest(
                requestId = args.interactionId,
                sendEvent = {
                    viewModelScope.launch { sendEvent(DAppLoginEvent.CloseLoginFlow) }
                },
                isAuthorizedRequest = true
            )
            setInitialDappLoginRoute()
        }
    }

    fun onDismissSigningStatusDialog() = dAppLoginDelegate.onDismissSigningStatusDialog()

    private suspend fun setInitialDappLoginRoute() {
        val authorizedRequest = state.value.authorizeRequest!!
        when (val authRequest = authorizedRequest.authRequest) {
            is AuthorizedRequest.AuthRequest.UsePersonaRequest -> {
                val dapp = state.value.authorizedDApp
                if (dapp != null) {
                    setInitialDappLoginRouteForUsePersonaRequest(dapp, authorizedRequest, authRequest)
                } else {
                    onAbortDappLogin(WalletErrorType.InvalidPersona)
                }
            }

            else -> {
                _state.update {
                    it.copy(
                        initialAuthorizedLoginRoute = InitialAuthorizedLoginRoute.SelectPersona(
                            state.value.authorizeRequest!!.requestMetadata.dAppDefinitionAddress
                        )
                    )
                }
            }
        }
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod", "UnsafeCallOnNullableType")
    private suspend fun setInitialDappLoginRouteForUsePersonaRequest(
        dapp: Network.AuthorizedDapp,
        request: AuthorizedRequest,
        authRequest: AuthorizedRequest.AuthRequest.UsePersonaRequest
    ) {
        val hasAuthorizedPersona = dapp.hasAuthorizedPersona(authRequest.personaAddress)
        if (hasAuthorizedPersona.not()) {
            onAbortDappLogin(WalletErrorType.InvalidPersona)
            return
        }
        val resetAccounts = request.resetRequestItem?.accounts == true
        val resetPersonaData = request.resetRequestItem?.personaData == true
        val persona =
            checkNotNull(getProfileUseCase.personaOnCurrentNetwork(authRequest.personaAddress))
        onSelectPersona(persona)
        val ongoingAccountsRequestItem = request.ongoingAccountsRequestItem
        val oneTimeAccountsRequestItem = request.oneTimeAccountsRequestItem
        val ongoingPersonaDataRequestItem = request.ongoingPersonaDataRequestItem
        val oneTimePersonaDataRequestItem = request.oneTimePersonaDataRequestItem
        val ongoingAccountsAlreadyGranted = requestedAccountsPermissionAlreadyGranted(
            authRequest.personaAddress,
            ongoingAccountsRequestItem
        )
        val needSignature = request.needSignatures()
        val ongoingDataAlreadyGranted =
            personaDataAccessAlreadyGranted(ongoingPersonaDataRequestItem, persona.address)
        when {
            needSignature && ongoingAccountsAlreadyGranted && ongoingDataAlreadyGranted && request.hasOngoingRequestItemsOnly() -> {
                _state.update {
                    // automatically fill in persona, the persona data, accounts and complete request
                    it.copy(
                        selectedAccountsOngoing = dAppConnectionRepository.dAppAuthorizedPersonaAccountAddresses(
                            dapp.dAppDefinitionAddress,
                            persona.address,
                            ongoingAccountsRequestItem!!.numberOfValues.quantity,
                            ongoingAccountsRequestItem.numberOfValues.toProfileShareAccountsQuantifier()
                        ).mapNotNull { address ->
                            getProfileUseCase.accountOnCurrentNetwork(address)?.toUiModel()
                        },
                        selectedOngoingPersonaData = persona.getPersonaDataForFieldKinds(
                            ongoingPersonaDataRequestItem!!.toRequiredFields().fields
                        ),
                        initialAuthorizedLoginRoute = InitialAuthorizedLoginRoute.CompleteRequest
                    )
                }
            }

            ongoingAccountsRequestItem != null && (!ongoingAccountsAlreadyGranted || resetAccounts) -> {
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
                ongoingPersonaDataRequestItem.isValid() && (!ongoingDataAlreadyGranted || resetPersonaData) -> {
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
    }

    fun onAcknowledgeFailureDialog() = dAppLoginDelegate.onAcknowledgeFailureDialog(
        sendEvent = {
            viewModelScope.launch { sendEvent(DAppLoginEvent.CloseLoginFlow) }
        }
    )

    fun onMessageShown() = dAppLoginDelegate.onMessageShown()

    fun dismissNoMnemonicError() = dAppLoginDelegate.dismissNoMnemonicError()

    fun personaSelectionConfirmed() {
        viewModelScope.launch {
            val selectedPersona = state.value.selectedPersona?.persona
            requireNotNull(selectedPersona)
            updateOrCreateAuthorizedDappWithSelectedPersona(selectedPersona)
            handleNextRequestItem(selectedPersona)
        }
    }

    private suspend fun handleNextRequestItem(selectedPersona: Network.Persona) {
        val request = state.value.authorizeRequest!!
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
            DAppLoginEvent.ChooseAccounts(
                numberOfAccounts,
                isExactAccountsCount,
                oneTime = true
            )
        )
    }

    fun onGrantedPersonaDataOngoing() {
        viewModelScope.launch {
            val requiredFields =
                checkNotNull(state.value.authorizeRequest!!.ongoingPersonaDataRequestItem?.toRequiredFields())
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
                    val editedDApp = state.value.editedDApp?.updateAuthorizedDappPersonaFields(
                        personaAddress = updatedPersona.address,
                        personaData = grantedPersonaData,
                        requiredFields = requiredFields.fields.associate { it.kind to it.numberOfValues.quantity }
                    )
                    _state.update {
                        it.copy(
                            editedDApp = editedDApp
                        )
                    }
                    handleNextOneTimeRequestItem()
                }
        }
    }

    fun onGrantedPersonaDataOnetime(persona: Network.Persona) {
        viewModelScope.launch {
            val requiredFields =
                checkNotNull(state.value.authorizeRequest!!.oneTimePersonaDataRequestItem?.toRequiredFields())
            val sharedPersonaData = persona.getPersonaDataForFieldKinds(requiredPersonaFields = requiredFields.fields)
            _state.update {
                it.copy(
                    selectedOneTimePersonaData = sharedPersonaData
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
        return state.value.authorizedDApp?.let { dapp ->
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
        val dapp = requireNotNull(state.value.editedDApp)
        val dataAccessAlreadyGranted = personaDataAccessAlreadyGranted(requestItem, personaAddress)
        if (state.value.authorizeRequest!!.resetRequestItem?.personaData == true || !dataAccessAlreadyGranted) {
            sendEvent(
                DAppLoginEvent.PersonaDataOngoing(
                    personaAddress,
                    requestItem.toRequiredFields()
                )
            )
        } else {
            val requestedFields = requestItem.toRequiredFields()
            val persona = requireNotNull(getProfileUseCase.personaOnCurrentNetwork(personaAddress))
            val sharedPersonaData = persona.getPersonaDataForFieldKinds(requestedFields.fields)
            _state.update { it.copy(selectedOngoingPersonaData = sharedPersonaData) }
            val editedDApp = state.value.editedDApp?.updateAuthorizedDappPersonas(
                dapp.referencesToAuthorizedPersonas.map { ref ->
                    if (ref.identityAddress == personaAddress) {
                        ref.copy(lastLogin = LocalDateTime.now().toISO8601String())
                    } else {
                        ref
                    }
                }
            )
            _state.update {
                it.copy(
                    editedDApp = editedDApp
                )
            }
            handleNextOneTimeRequestItem()
        }
    }

    private suspend fun handleNextOneTimeRequestItem() {
        val request = state.value.authorizeRequest!!
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
        val dapp = requireNotNull(state.value.editedDApp)
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
                DAppLoginEvent.PersonaDataOnetime(
                    oneTimePersonaRequestItem.toRequiredFields()
                )
            )
        }
    }

    private suspend fun handleOngoingAddressRequestItem(
        ongoingAccountsRequestItem: AccountsRequestItem,
        personaAddress: String
    ) {
        val dapp = requireNotNull(state.value.editedDApp)
        val numberOfAccounts = ongoingAccountsRequestItem.numberOfValues.quantity
        val isExactAccountsCount = ongoingAccountsRequestItem.numberOfValues.exactly()
        val potentialOngoingAddresses =
            dAppConnectionRepository.dAppAuthorizedPersonaAccountAddresses(
                dapp.dAppDefinitionAddress,
                personaAddress,
                numberOfAccounts,
                ongoingAccountsRequestItem.numberOfValues.toProfileShareAccountsQuantifier()
            )
        if (state.value.authorizeRequest!!.resetRequestItem?.accounts == true || potentialOngoingAddresses.isEmpty()) {
            sendEvent(
                DAppLoginEvent.DisplayPermission(
                    numberOfAccounts,
                    isExactAccountsCount
                )
            )
        } else {
            val selectedAccounts = potentialOngoingAddresses.mapNotNull {
                getProfileUseCase.accountOnCurrentNetwork(it)?.toUiModel(true)
            }
            _state.update { it.copy(selectedAccountsOngoing = selectedAccounts) }
            val editedDApp = state.value.editedDApp?.updateAuthorizedDappPersonas(
                dapp.referencesToAuthorizedPersonas.map { ref ->
                    if (ref.identityAddress == personaAddress) {
                        ref.copy(lastLogin = LocalDateTime.now().toISO8601String())
                    } else {
                        ref
                    }
                }
            )
            _state.update {
                it.copy(
                    editedDApp = editedDApp
                )
            }
            val request = state.value.authorizeRequest!!
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
        val dApp = state.value.authorizedDApp
        val date = LocalDateTime.now().toISO8601String()
        if (dApp == null) {
            val request = state.value.authorizeRequest!!
            val dAppName = state.value.dapp?.name?.ifEmpty { null }
            _state.update {
                it.copy(
                    editedDApp = Network.AuthorizedDapp(
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
                )
            }
        } else {
            _state.update {
                it.copy(
                    editedDApp = state.value.authorizedDApp?.addOrUpdateAuthorizedDappPersona(selectedPersona, date)
                )
            }
        }
    }

    fun onAbortDappLogin(walletWalletErrorType: WalletErrorType = WalletErrorType.RejectedByUser) {
        val request = state.value.authorizeRequest!!
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
            sendEvent(DAppLoginEvent.CloseLoginFlow)
            incomingRequestRepository.requestHandled(requestId = args.interactionId)
        }
    }

    fun onSelectPersona(persona: Network.Persona) {
        dAppLoginDelegate.onSelectPersona(persona)
    }

    fun onPermissionGranted(
        numberOfAccounts: Int,
        isExactAccountsCount: Boolean,
        isOneTime: Boolean
    ) {
        viewModelScope.launch {
            sendEvent(
                DAppLoginEvent.ChooseAccounts(
                    numberOfAccounts = numberOfAccounts,
                    isExactAccountsCount = isExactAccountsCount,
                    oneTime = isOneTime
                )
            )
        }
    }

    fun onAccountsSelected(selectedAccounts: List<AccountItemUiModel>, oneTimeRequest: Boolean) {
        val request = state.value.authorizeRequest!!
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
                val editedDApp = state.value.editedDApp?.updateDappAuthorizedPersonaSharedAccounts(
                    selectedPersona.address,
                    Shared(
                        selectedAccounts.map { it.address },
                        RequestedNumber(
                            handledRequest.numberOfValues.toProfileShareAccountsQuantifier(),
                            handledRequest.numberOfValues.quantity
                        )
                    )
                )
                _state.update {
                    it.copy(
                        editedDApp = editedDApp
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
        sendEvent(DAppLoginEvent.RequestCompletionBiometricPrompt(state.value.authorizeRequest!!.needSignatures()))
    }

    fun completeRequestHandling(
        deviceBiometricAuthenticationProvider: suspend () -> Boolean = { true },
        abortOnFailure: Boolean = false
    ) {
        val request = state.value.authorizeRequest!!

        viewModelScope.launch {
            val selectedPersona = state.value.selectedPersona?.persona
            requireNotNull(selectedPersona)
            if (request.isInternalRequest()) {
                state.value.editedDApp?.let { dAppConnectionRepository.updateOrCreateAuthorizedDApp(it) }
                incomingRequestRepository.requestHandled(request.interactionId)
                sendEvent(DAppLoginEvent.LoginFlowCompleted)
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
                    onetimeSharedPersonaData = state.value.selectedOneTimePersonaData,
                    deviceBiometricAuthenticationProvider = deviceBiometricAuthenticationProvider
                ).onSuccess { response ->
                    dAppMessenger.sendWalletInteractionSuccessResponse(
                        remoteConnectorId = request.remoteConnectorId,
                        response = response
                    )
                    state.value.editedDApp?.let { dAppConnectionRepository.updateOrCreateAuthorizedDApp(it) }
                    sendEvent(DAppLoginEvent.LoginFlowCompleted)
                    if (!request.isInternalRequest()) {
                        appEventBus.sendEvent(
                            AppEvent.Status.DappInteraction(
                                requestId = request.interactionId,
                                dAppName = state.value.dapp?.name
                            )
                        )
                    }
                }.onFailure { throwable ->
                    dAppLoginDelegate.handleRequestError(
                        exception = throwable,
                        isAuthorizedRequest = true
                    )
                    if (abortOnFailure) {
                        onAbortDappLogin()
                    }
                }
            }
        }
    }
}
