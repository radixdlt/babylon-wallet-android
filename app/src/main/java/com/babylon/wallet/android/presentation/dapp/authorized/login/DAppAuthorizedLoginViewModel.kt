package com.babylon.wallet.android.presentation.dapp.authorized.login

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.getDappMessage
import com.babylon.wallet.android.domain.model.messages.DappToWalletInteraction
import com.babylon.wallet.android.domain.model.messages.IncomingRequestResponse
import com.babylon.wallet.android.domain.model.messages.RequiredPersonaFields
import com.babylon.wallet.android.domain.model.messages.WalletAuthorizedRequest
import com.babylon.wallet.android.domain.usecases.BuildAuthorizedDappResponseUseCase
import com.babylon.wallet.android.domain.usecases.RespondToIncomingRequestUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.dapp.FailureDialogState
import com.babylon.wallet.android.presentation.dapp.InitialAuthorizedLoginRoute
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.account.toUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.PersonaUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.toUiModel
import com.babylon.wallet.android.presentation.model.getPersonaDataForFieldKinds
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.AuthorizedDapp
import com.radixdlt.sargon.AuthorizedDappPreferenceDeposits
import com.radixdlt.sargon.AuthorizedDappPreferences
import com.radixdlt.sargon.AuthorizedPersonaSimple
import com.radixdlt.sargon.DappWalletInteractionErrorType
import com.radixdlt.sargon.IdentityAddress
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.PersonaData
import com.radixdlt.sargon.RequestedNumberQuantifier
import com.radixdlt.sargon.RequestedQuantity
import com.radixdlt.sargon.SharedPersonaData
import com.radixdlt.sargon.SharedToDappWithPersonaAccountAddresses
import com.radixdlt.sargon.extensions.ReferencesToAuthorizedPersonas
import com.radixdlt.sargon.extensions.init
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import rdx.works.core.TimestampGenerator
import rdx.works.core.domain.DApp
import rdx.works.core.logNonFatalException
import rdx.works.core.sargon.activeAccountOnCurrentNetwork
import rdx.works.core.sargon.activePersonaOnCurrentNetwork
import rdx.works.core.sargon.addOrUpdateAuthorizedDAppPersona
import rdx.works.core.sargon.hasAuthorizedPersona
import rdx.works.core.sargon.updateAuthorizedDAppPersonaFields
import rdx.works.core.sargon.updateAuthorizedDAppPersonas
import rdx.works.core.sargon.updateDAppAuthorizedPersonaSharedAccounts
import rdx.works.profile.data.repository.DAppConnectionRepository
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.ProfileException
import javax.inject.Inject

@HiltViewModel
@Suppress("LongParameterList", "TooManyFunctions", "LongMethod", "LargeClass")
class DAppAuthorizedLoginViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val appEventBus: AppEventBus,
    private val respondToIncomingRequestUseCase: RespondToIncomingRequestUseCase,
    private val dAppConnectionRepository: DAppConnectionRepository,
    private val getProfileUseCase: GetProfileUseCase,
    private val stateRepository: StateRepository,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val buildAuthorizedDappResponseUseCase: BuildAuthorizedDappResponseUseCase
) : StateViewModel<DAppLoginUiState>(),
    OneOffEventHandler<Event> by OneOffEventHandlerImpl() {

    private val args = DAppAuthorizedLoginArgs(savedStateHandle)

    private val mutex = Mutex()

    private lateinit var request: WalletAuthorizedRequest

    private var authorizedDapp: AuthorizedDapp? = null
    private var editedDapp: AuthorizedDapp? = null

    override fun initialState(): DAppLoginUiState = DAppLoginUiState()

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
            val requestToHandle = incomingRequestRepository.getRequest(args.interactionId) as? WalletAuthorizedRequest
            if (requestToHandle == null) {
                sendEvent(Event.CloseLoginFlow)
                return@launch
            } else {
                request = requestToHandle
            }
            val dAppDefinitionAddress = runCatching { AccountAddress.init(request.requestMetadata.dAppDefinitionAddress) }.getOrNull()
            if (!request.isValidRequest() || dAppDefinitionAddress == null) {
                handleRequestError(RadixWalletException.DappRequestException.InvalidRequest)
                return@launch
            }
            authorizedDapp = dAppConnectionRepository.getAuthorizedDApp(dAppDefinitionAddress = dAppDefinitionAddress)
            editedDapp = authorizedDapp
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
            setInitialDappLoginRoute(dAppDefinitionAddress)
        }
    }

    private suspend fun setInitialDappLoginRoute(dAppDefinitionAddress: AccountAddress) {
        when (val authRequest = request.authRequestItem) {
            is WalletAuthorizedRequest.AuthRequestItem.UsePersonaRequest -> {
                val dapp = authorizedDapp
                if (dapp != null) {
                    setInitialDappLoginRouteForUsePersonaRequest(dapp, authRequest)
                } else {
                    onAbortDappLogin(DappWalletInteractionErrorType.INVALID_PERSONA)
                }
            }

            else -> {
                _state.update {
                    it.copy(
                        initialAuthorizedLoginRoute = InitialAuthorizedLoginRoute.SelectPersona(
                            dappDefinitionAddress = dAppDefinitionAddress
                        )
                    )
                }
            }
        }
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod", "UnsafeCallOnNullableType")
    private suspend fun setInitialDappLoginRouteForUsePersonaRequest(
        dapp: AuthorizedDapp,
        authRequestItem: WalletAuthorizedRequest.AuthRequestItem.UsePersonaRequest
    ) {
        val hasAuthorizedPersona = dapp.hasAuthorizedPersona(authRequestItem.identityAddress)
        if (hasAuthorizedPersona.not()) {
            onAbortDappLogin(DappWalletInteractionErrorType.INVALID_PERSONA)
            return
        }
        val resetAccounts = request.resetRequestItem?.accounts == true
        val resetPersonaData = request.resetRequestItem?.personaData == true
        val persona = checkNotNull(getProfileUseCase().activePersonaOnCurrentNetwork(authRequestItem.identityAddress))
        onSelectPersona(persona)
        val ongoingAccountsRequestItem = request.ongoingAccountsRequestItem
        val oneTimeAccountsRequestItem = request.oneTimeAccountsRequestItem
        val ongoingPersonaDataRequestItem = request.ongoingPersonaDataRequestItem
        val oneTimePersonaDataRequestItem = request.oneTimePersonaDataRequestItem
        val ongoingAccountsAlreadyGranted = requestedAccountsPermissionAlreadyGranted(
            personaAddress = authRequestItem.identityAddress,
            accountsRequestItem = ongoingAccountsRequestItem
        )
        val ongoingDataAlreadyGranted = personaDataAccessAlreadyGranted(
            requestItem = ongoingPersonaDataRequestItem,
            personaAddress = persona.address
        )
        when {
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
                            authRequestItem.identityAddress,
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

            else -> {
                _state.update {
                    val selectedOngoingAccounts = ongoingAccountsRequestItem?.let { ongoingAccountsRequest ->
                        dAppConnectionRepository.dAppAuthorizedPersonaAccountAddresses(
                            dapp.dappDefinitionAddress,
                            persona.address,
                            ongoingAccountsRequest.numberOfValues.quantity,
                            ongoingAccountsRequest.numberOfValues.toRequestedNumberQuantifier()
                        ).mapNotNull { address ->
                            getProfileUseCase().activeAccountOnCurrentNetwork(address)?.toUiModel()
                        }
                    }.orEmpty()
                    val selectedOngoingPersonaData = ongoingPersonaDataRequestItem?.let { personaDataRequest ->
                        persona.getPersonaDataForFieldKinds(
                            personaDataRequest.toRequiredFields().fields
                        )
                    }
                    // automatically fill in persona, the persona data, accounts and complete request
                    it.copy(
                        selectedAccountsOngoing = selectedOngoingAccounts,
                        selectedOngoingPersonaData = selectedOngoingPersonaData,
                        initialAuthorizedLoginRoute = InitialAuthorizedLoginRoute.CompleteRequest
                    )
                }
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
                        request,
                        exception.dappWalletInteractionErrorType,
                        exception.getDappMessage()
                    )
                    _state.update { it.copy(failureDialog = FailureDialogState.Open(exception)) }
                }
            }
        }
    }

    fun onAcknowledgeFailureDialog() = viewModelScope.launch {
        val exception = (_state.value.failureDialog as? FailureDialogState.Open)?.dappRequestException ?: return@launch
        respondToIncomingRequestUseCase.respondWithFailure(request, exception.dappWalletInteractionErrorType, exception.getDappMessage())
        _state.update { it.copy(failureDialog = FailureDialogState.Closed) }
        sendEvent(Event.CloseLoginFlow)
        incomingRequestRepository.requestHandled(requestId = args.interactionId)
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    fun dismissNoMnemonicError() {
        _state.update { it.copy(isNoMnemonicErrorVisible = false) }
    }

    fun personaSelectionConfirmed() {
        viewModelScope.launch {
            val selectedPersona = state.value.selectedPersona?.persona
            requireNotNull(selectedPersona)
            updateOrCreateAuthorizedDappWithSelectedPersona(selectedPersona)
            handleNextRequestItem(selectedPersona)
        }
    }

    private suspend fun handleNextRequestItem(selectedPersona: Persona) {
        val request = request
        when {
            request.hasOnlyAuthItem() -> {
                completeRequestHandling()
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
                completeRequestHandling()
            }
        }
    }

    private suspend fun handleOneTimeAccountRequestItem(
        oneTimeAccountsRequestItem: DappToWalletInteraction.AccountsRequestItem
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
            getProfileUseCase().activePersonaOnCurrentNetwork(selectedPersona.persona.address)
                ?.let { updatedPersona ->
                    val grantedPersonaData = updatedPersona.getPersonaDataForFieldKinds(requiredFields.fields)
                    _state.update {
                        it.copy(
                            selectedPersona = updatedPersona.toUiModel(),
                            selectedOngoingPersonaData = grantedPersonaData
                        )
                    }
                    mutex.withLock {
                        editedDapp = editedDapp?.updateAuthorizedDAppPersonaFields(
                            personaAddress = updatedPersona.address,
                            personaData = grantedPersonaData,
                            requiredFields = requiredFields.fields.associate { it.kind to it.numberOfValues.quantity }
                        )
                    }
                    handleNextOneTimeRequestItem()
                }
        }
    }

    fun onGrantedPersonaDataOnetime(persona: Persona) {
        viewModelScope.launch {
            val requiredFields =
                checkNotNull(request.oneTimePersonaDataRequestItem?.toRequiredFields())
            val sharedPersonaData = persona.getPersonaDataForFieldKinds(requiredPersonaFields = requiredFields.fields)
            _state.update {
                it.copy(
                    selectedOnetimePersonaData = sharedPersonaData
                )
            }
            completeRequestHandling()
        }
    }

    private suspend fun requestedAccountsPermissionAlreadyGranted(
        personaAddress: IdentityAddress,
        accountsRequestItem: DappToWalletInteraction.AccountsRequestItem?
    ): Boolean {
        if (accountsRequestItem == null) return false
        return authorizedDapp?.let { dapp ->
            val potentialOngoingAddresses =
                dAppConnectionRepository.dAppAuthorizedPersonaAccountAddresses(
                    dapp.dappDefinitionAddress,
                    personaAddress,
                    accountsRequestItem.numberOfValues.quantity,
                    accountsRequestItem.numberOfValues.toRequestedNumberQuantifier()
                )
            potentialOngoingAddresses.isNotEmpty()
        } ?: false
    }

    private suspend fun handleOngoingPersonaDataRequestItem(
        personaAddress: IdentityAddress,
        requestItem: DappToWalletInteraction.PersonaDataRequestItem
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
            val persona = requireNotNull(getProfileUseCase().activePersonaOnCurrentNetwork(personaAddress))
            val sharedPersonaData = persona.getPersonaDataForFieldKinds(requestedFields.fields)
            _state.update { it.copy(selectedOngoingPersonaData = sharedPersonaData) }
            mutex.withLock {
                editedDapp =
                    editedDapp?.updateAuthorizedDAppPersonas(
                        dapp.referencesToAuthorizedPersonas.map { ref ->
                            if (ref.identityAddress == personaAddress) {
                                ref.copy(lastLogin = TimestampGenerator())
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
        val request = request
        when {
            request.oneTimeAccountsRequestItem != null -> handleOneTimeAccountRequestItem(
                request.oneTimeAccountsRequestItem
            )

            request.oneTimePersonaDataRequestItem != null && request.oneTimePersonaDataRequestItem.isValid() -> {
                handleOneTimePersonaDataRequestItem(request.oneTimePersonaDataRequestItem)
            }

            else -> completeRequestHandling()
        }
    }

    private suspend fun personaDataAccessAlreadyGranted(
        requestItem: DappToWalletInteraction.PersonaDataRequestItem?,
        personaAddress: IdentityAddress
    ): Boolean {
        if (requestItem == null) return false
        val dapp = requireNotNull(editedDapp)
        val requestedFieldKinds = requestItem.toRequiredFields()
        return dAppConnectionRepository.dAppAuthorizedPersonaHasAllDataFields(
            dapp.dappDefinitionAddress,
            personaAddress,
            requestedFieldKinds.fields.associate { it.kind to it.numberOfValues.quantity }
        )
    }

    private fun handleOneTimePersonaDataRequestItem(oneTimePersonaDataRequestItem: DappToWalletInteraction.PersonaDataRequestItem) {
        viewModelScope.launch {
            sendEvent(
                Event.PersonaDataOnetime(
                    oneTimePersonaDataRequestItem.toRequiredFields()
                )
            )
        }
    }

    private suspend fun handleOngoingAddressRequestItem(
        ongoingAccountsRequestItem: DappToWalletInteraction.AccountsRequestItem,
        personaAddress: IdentityAddress
    ) {
        val dapp = requireNotNull(editedDapp)
        val numberOfAccounts = ongoingAccountsRequestItem.numberOfValues.quantity
        val isExactAccountsCount = ongoingAccountsRequestItem.numberOfValues.exactly()
        val potentialOngoingAddresses =
            dAppConnectionRepository.dAppAuthorizedPersonaAccountAddresses(
                dapp.dappDefinitionAddress,
                personaAddress,
                numberOfAccounts,
                ongoingAccountsRequestItem.numberOfValues.toRequestedNumberQuantifier()
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
                getProfileUseCase().activeAccountOnCurrentNetwork(it)?.toUiModel(true)
            }
            _state.update { it.copy(selectedAccountsOngoing = selectedAccounts) }
            mutex.withLock {
                editedDapp =
                    editedDapp?.updateAuthorizedDAppPersonas(
                        dapp.referencesToAuthorizedPersonas.map { ref ->
                            if (ref.identityAddress == personaAddress) {
                                ref.copy(lastLogin = TimestampGenerator())
                            } else {
                                ref
                            }
                        }
                    )
            }
            val request = request
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

                else -> completeRequestHandling()
            }
        }
    }

    private suspend fun updateOrCreateAuthorizedDappWithSelectedPersona(selectedPersona: Persona) {
        val dApp = authorizedDapp
        val date = TimestampGenerator()
        if (dApp == null) {
            val dAppName = state.value.dapp?.name?.ifEmpty { null }
            mutex.withLock {
                editedDapp = AuthorizedDapp(
                    networkId = request.metadata.networkId,
                    dappDefinitionAddress = AccountAddress.init(request.metadata.dAppDefinitionAddress),
                    displayName = dAppName,
                    referencesToAuthorizedPersonas = ReferencesToAuthorizedPersonas(
                        AuthorizedPersonaSimple(
                            identityAddress = selectedPersona.address,
                            lastLogin = date,
                            sharedAccounts = SharedToDappWithPersonaAccountAddresses(
                                ids = emptyList(),
                                request = RequestedQuantity(
                                    quantifier = RequestedNumberQuantifier.EXACTLY,
                                    quantity = 0u
                                )
                            ),
                            sharedPersonaData = SharedPersonaData(
                                name = null,
                                emailAddresses = null,
                                phoneNumbers = null
                            )
                        )
                    ).asList(),
                    preferences = AuthorizedDappPreferences(
                        deposits = AuthorizedDappPreferenceDeposits.VISIBLE
                    )
                )
            }
        } else {
            mutex.withLock {
                editedDapp = authorizedDapp?.addOrUpdateAuthorizedDAppPersona(selectedPersona, date)
            }
        }
    }

    fun onAbortDappLogin(walletWalletErrorType: DappWalletInteractionErrorType = DappWalletInteractionErrorType.REJECTED_BY_USER) {
        viewModelScope.launch {
            sendEvent(Event.CloseLoginFlow)
            incomingRequestRepository.requestHandled(request.interactionId)
            if (!request.isInternal) {
                respondToIncomingRequestUseCase.respondWithFailure(request, walletWalletErrorType)
            }
        }
    }

    fun onSelectPersona(persona: Persona) {
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
                    editedDapp = editedDapp?.updateDAppAuthorizedPersonaSharedAccounts(
                        personaAddress = selectedPersona.address,
                        sharedAccounts = SharedToDappWithPersonaAccountAddresses(
                            request = RequestedQuantity(
                                quantifier = handledRequest.numberOfValues.toRequestedNumberQuantifier(),
                                quantity = handledRequest.numberOfValues.quantity.toUShort()
                            ),
                            ids = selectedAccounts.map { it.address }
                        )
                    )
                }
                val request = request
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
                        completeRequestHandling()
                    }
                }
            }
        } else if (handledRequest?.isOngoing == false) {
            _state.update { it.copy(selectedAccountsOneTime = selectedAccounts) }
            val request = request
            when {
                request.oneTimePersonaDataRequestItem != null && request.oneTimePersonaDataRequestItem.isValid() -> {
                    handleOneTimePersonaDataRequestItem(request.oneTimePersonaDataRequestItem)
                }

                else -> {
                    viewModelScope.launch {
                        completeRequestHandling()
                    }
                }
            }
        }
    }

    fun completeRequestHandling() {
        viewModelScope.launch {
            val selectedPersona = state.value.selectedPersona?.persona
            requireNotNull(selectedPersona)
            if (request.isInternal) {
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
                        getProfileUseCase().activeAccountOnCurrentNetwork(it.address)
                    },
                    ongoingAccounts = state.value.selectedAccountsOngoing.mapNotNull {
                        getProfileUseCase().activeAccountOnCurrentNetwork(it.address)
                    },
                    ongoingSharedPersonaData = state.value.selectedOngoingPersonaData,
                    onetimeSharedPersonaData = state.value.selectedOnetimePersonaData
                ).mapCatching { response ->
                    respondToIncomingRequestUseCase.respondWithSuccess(request, response).getOrThrow()
                }.onSuccess { result ->
                    mutex.withLock {
                        editedDapp?.let { dAppConnectionRepository.updateOrCreateAuthorizedDApp(it) }
                    }
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
                }.onFailure { throwable ->
                    handleRequestError(throwable)
                }
            }
        }
    }
}

sealed interface Event : OneOffEvent {

    data object CloseLoginFlow : Event

    data object LoginFlowCompleted : Event
    data class DisplayPermission(
        val numberOfAccounts: Int,
        val isExactAccountsCount: Boolean,
        val oneTime: Boolean = false
    ) : Event

    data class PersonaDataOngoing(
        val personaAddress: IdentityAddress,
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
    val dapp: DApp? = null,
    val uiMessage: UiMessage? = null,
    val failureDialog: FailureDialogState = FailureDialogState.Closed,
    val initialAuthorizedLoginRoute: InitialAuthorizedLoginRoute? = null,
    val selectedAccountsOngoing: List<AccountItemUiModel> = emptyList(),
    val selectedOngoingPersonaData: PersonaData? = null,
    val selectedOnetimePersonaData: PersonaData? = null,
    val selectedAccountsOneTime: List<AccountItemUiModel> = emptyList(),
    val selectedPersona: PersonaUiModel? = null,
    val isNoMnemonicErrorVisible: Boolean = false
) : UiState
