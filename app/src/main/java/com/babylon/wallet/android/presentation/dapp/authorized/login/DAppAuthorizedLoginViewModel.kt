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
import com.babylon.wallet.android.domain.usecases.RespondToIncomingRequestUseCase
import com.babylon.wallet.android.domain.usecases.login.BuildAuthorizedDappResponseUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.dapp.FailureDialogState
import com.babylon.wallet.android.presentation.dapp.authorized.InitialAuthorizedLoginRoute
import com.babylon.wallet.android.presentation.dapp.authorized.verifyentities.EntitiesForProofWithSignatures
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
import com.radixdlt.sargon.SignatureWithPublicKey
import com.radixdlt.sargon.extensions.ProfileEntity
import com.radixdlt.sargon.extensions.ReferencesToAuthorizedPersonas
import com.radixdlt.sargon.extensions.asProfileEntity
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
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.core.sargon.activePersonaOnCurrentNetwork
import rdx.works.core.sargon.activePersonasOnCurrentNetwork
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
) : StateViewModel<DAppLoginUiState>(), OneOffEventHandler<Event> by OneOffEventHandlerImpl() {

    override fun initialState(): DAppLoginUiState = DAppLoginUiState()

    private val args = DAppAuthorizedLoginArgs(savedStateHandle)

    private lateinit var request: WalletAuthorizedRequest

    private var authorizedDapp: AuthorizedDapp? = null
    private var editedDapp: AuthorizedDapp? = null
    private val mutex = Mutex()

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
                if (request.isValidRequest().not()) {
                    handleRequestError(RadixWalletException.DappRequestException.InvalidRequest)
                    return@launch
                }
            }

            val dAppDefinitionAddress = runCatching {
                AccountAddress.init(request.requestMetadata.dAppDefinitionAddress)
            }.getOrNull()
            if (dAppDefinitionAddress == null) {
                handleRequestError(RadixWalletException.DappRequestException.InvalidRequest)
                return@launch
            }

            authorizedDapp = dAppConnectionRepository.getAuthorizedDApp(dAppDefinitionAddress = dAppDefinitionAddress)
            editedDapp = authorizedDapp

            getDappDetails(dAppDefinitionAddress = dAppDefinitionAddress)

            setInitialDappLoginRoute(dAppDefinitionAddress)
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

    private suspend fun setInitialDappLoginRoute(dAppDefinitionAddress: AccountAddress) {
        when (val authRequest = request.authRequestItem) {
            is WalletAuthorizedRequest.AuthRequestItem.UsePersonaRequest -> {
                if (authorizedDapp != null && authorizedDapp?.hasAuthorizedPersona(authRequest.identityAddress) == true) {
                    checkForProofOfOwnershipRequest().onSuccess {
                        setInitialDappLoginRouteForUsePersonaRequest(
                            usePersonaRequest = authRequest,
                            dappDefinitionAddress = dAppDefinitionAddress
                        )
                    }.onFailure {
                        onAbortDappLogin(DappWalletInteractionErrorType.INVALID_PERSONA_OR_ACCOUNTS)
                    }
                } else {
                    onAbortDappLogin(DappWalletInteractionErrorType.INVALID_PERSONA)
                }
            }

            is WalletAuthorizedRequest.AuthRequestItem.LoginRequest -> {
                _state.update {
                    it.copy(
                        initialAuthorizedLoginRoute = InitialAuthorizedLoginRoute.SelectPersona(
                            authorizedRequestInteractionId = args.interactionId,
                            dappDefinitionAddress = dAppDefinitionAddress
                        )
                    )
                }
            }
        }
    }

    private suspend fun checkForProofOfOwnershipRequest(): Result<Unit> {
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
                return Result.success(Unit)
            } else {
                return Result.failure(RadixWalletException.DappRequestException.InvalidPersonaOrAccounts)
            }
        }
        return Result.success(Unit)
    }

    private suspend fun validateRequestedAddressesForProofOfOwnership(
        requestedPersonaAddress: IdentityAddress?,
        requestedAccountAddresses: List<AccountAddress>?
    ): Boolean {
        // if requestedPersonaAddress is present, then check if it exists in the allPersonasOnCurrentNetwork
        val isIdentityValid = requestedPersonaAddress?.let { address ->
            getProfileUseCase().activePersonasOnCurrentNetwork.any { it.address == address }
        } ?: true // if requestedPersonaAddress is null, assume it's valid (no need to check)

        // if requestedAccountAddresses are present, then check if all of them exist in the existingAccounts
        val areAccountsValid = requestedAccountAddresses?.let { addresses ->
            addresses.all { address ->
                getProfileUseCase().activeAccountsOnCurrentNetwork.any { it.address == address }
            }
        } ?: true // if requestedAccountAddresses is null, assume it's valid (no need to check)

        return isIdentityValid && areAccountsValid
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private suspend fun setInitialDappLoginRouteForUsePersonaRequest(
        usePersonaRequest: WalletAuthorizedRequest.AuthRequestItem.UsePersonaRequest,
        dappDefinitionAddress: AccountAddress
    ) {
        val persona = checkNotNull(getProfileUseCase().activePersonaOnCurrentNetwork(usePersonaRequest.identityAddress))
        _state.update { it.copy(personaWithSignature = persona.asProfileEntity() to null) }

        val resetAccounts = request.resetRequestItem?.accounts == true
        val resetPersonaData = request.resetRequestItem?.personaData == true
        val ongoingAccountsRequestItem = request.ongoingAccountsRequestItem
        val oneTimeAccountsRequestItem = request.oneTimeAccountsRequestItem
        val ongoingPersonaDataRequestItem = request.ongoingPersonaDataRequestItem
        val oneTimePersonaDataRequestItem = request.oneTimePersonaDataRequestItem
        val ongoingAccountsAlreadyGranted = requestedAccountsPermissionAlreadyGranted(
            personaAddress = usePersonaRequest.identityAddress,
            accountsRequestItem = ongoingAccountsRequestItem
        )
        val ongoingDataAlreadyGranted = personaDataAccessAlreadyGranted(
            requestItem = ongoingPersonaDataRequestItem,
            personaAddress = persona.address
        )
        val proofOfOwnershipRequestItem = request.proofOfOwnershipRequestItem

        when {
            ongoingAccountsRequestItem != null && (!ongoingAccountsAlreadyGranted || resetAccounts) -> {
                _state.update {
                    it.copy(
                        initialAuthorizedLoginRoute = InitialAuthorizedLoginRoute.OngoingAccounts(
                            authorizedRequestInteractionId = args.interactionId,
                            numberOfAccounts = ongoingAccountsRequestItem.numberOfValues.quantity,
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
                            usePersonaRequest.identityAddress,
                            ongoingPersonaDataRequestItem.toRequiredFields()
                        )
                    )
                }
            }

            oneTimeAccountsRequestItem != null -> {
                _state.update {
                    it.copy(
                        initialAuthorizedLoginRoute = InitialAuthorizedLoginRoute.OneTimeAccounts(
                            authorizedRequestInteractionId = args.interactionId,
                            numberOfAccounts = oneTimeAccountsRequestItem.numberOfValues.quantity,
                            isExactAccountsCount = oneTimeAccountsRequestItem.numberOfValues.exactly(),
                            isOneTimeRequest = true
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

            proofOfOwnershipRequestItem != null -> {
                if (proofOfOwnershipRequestItem.personaAddress != null) {
                    _state.update { state ->
                        state.copy(
                            initialAuthorizedLoginRoute = InitialAuthorizedLoginRoute.VerifyPersona(
                                walletAuthorizedRequestInteractionId = args.interactionId,
                                entitiesForProofWithSignatures = EntitiesForProofWithSignatures(
                                    personaAddress = state.personaRequiredProofOfOwnership,
                                    accountAddresses = state.accountsRequiredProofOfOwnership.orEmpty()
                                )
                            )
                        )
                    }
                } else if (request.proofOfOwnershipRequestItem?.accountAddresses != null) {
                    _state.update { state ->
                        state.copy(
                            initialAuthorizedLoginRoute = InitialAuthorizedLoginRoute.VerifyAccounts(
                                walletAuthorizedRequestInteractionId = args.interactionId,
                                entitiesForProofWithSignatures = EntitiesForProofWithSignatures(
                                    accountAddresses = state.accountsRequiredProofOfOwnership.orEmpty()
                                )
                            )
                        )
                    }
                }
            }

            else -> {
                _state.update {
                    val selectedOngoingAccounts = ongoingAccountsRequestItem?.let { ongoingAccountsRequest ->
                        dAppConnectionRepository.dAppAuthorizedPersonaAccountAddresses(
                            dAppDefinitionAddress = dappDefinitionAddress,
                            personaAddress = persona.address,
                            numberOfAccounts = ongoingAccountsRequest.numberOfValues.quantity,
                            quantifier = ongoingAccountsRequest.numberOfValues.toRequestedNumberQuantifier()
                        ).mapNotNull { address ->
                            getProfileUseCase().activeAccountOnCurrentNetwork(address)?.asProfileEntity()
                        }
                    }.orEmpty()

                    val selectedOngoingPersonaData = ongoingPersonaDataRequestItem?.let { personaDataRequest ->
                        persona.getPersonaDataForFieldKinds(
                            personaDataRequest.toRequiredFields().fields
                        )
                    }

                    it.copy(
                        ongoingAccountsWithSignatures = selectedOngoingAccounts.associateWith { null },
                        selectedOngoingPersonaData = selectedOngoingPersonaData,
                        initialAuthorizedLoginRoute = InitialAuthorizedLoginRoute.CompleteRequest
                    )
                }
            }
        }
    }

    fun onAcknowledgeFailureDialog() = viewModelScope.launch {
        val exception = (_state.value.failureDialog as? FailureDialogState.Open)?.dappRequestException ?: return@launch
        respondToIncomingRequestUseCase.respondWithFailure(
            request,
            exception.dappWalletInteractionErrorType,
            exception.getDappMessage()
        )
        _state.update { it.copy(failureDialog = FailureDialogState.Closed) }
        sendEvent(Event.CloseLoginFlow)
        incomingRequestRepository.requestHandled(requestId = args.interactionId)
    }

    fun onPersonaAuthorized(
        authorizedPersona: ProfileEntity.PersonaEntity,
        signature: SignatureWithPublicKey?
    ) { // when user selects a persona for the AuthRequestItem.LoginRequest
        viewModelScope.launch {
            _state.update { it.copy(personaWithSignature = authorizedPersona to signature) }
            updateOrCreateAuthorizedDappWithSelectedPersona(authorizedPersona.persona)
            handleNextRequestItemOfLoginRequest(authorizedPersona.persona)
        }
    }

    private suspend fun handleNextRequestItemOfLoginRequest(selectedPersona: Persona) {
        val request = request
        when {
            request.isOnlyLoginRequest() -> {
                completeRequestHandling()
            }

            request.ongoingAccountsRequestItem != null -> {
                handleOngoingAccountsRequestItem(
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

    private suspend fun handleOneTimeAccountRequestItem(oneTimeAccountsRequestItem: DappToWalletInteraction.AccountsRequestItem) {
        val numberOfAccounts = oneTimeAccountsRequestItem.numberOfValues.quantity
        val isExactAccountsCount = oneTimeAccountsRequestItem.numberOfValues.exactly()
        sendEvent(
            Event.NavigateToChooseAccounts(
                authorizedRequestInteractionId = args.interactionId,
                isOneTimeRequest = true,
                isExactAccountsCount = isExactAccountsCount,
                numberOfAccounts = numberOfAccounts
            )
        )
    }

    fun onGrantedOngoingPersonaData() {
        viewModelScope.launch {
            val requiredFields = checkNotNull(request.ongoingPersonaDataRequestItem?.toRequiredFields())
            val selectedPersona = state.value.personaWithSignature?.first
            checkNotNull(selectedPersona)
            getProfileUseCase().activePersonaOnCurrentNetwork(selectedPersona.persona.address)
                ?.let { updatedPersona ->
                    val grantedPersonaData = updatedPersona.getPersonaDataForFieldKinds(requiredFields.fields)
                    _state.update {
                        it.copy(selectedOngoingPersonaData = grantedPersonaData)
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

    fun onGrantedOnetimePersonaData(persona: Persona) {
        viewModelScope.launch {
            val requiredFields = checkNotNull(request.oneTimePersonaDataRequestItem?.toRequiredFields())
            val sharedPersonaData = persona.getPersonaDataForFieldKinds(requiredPersonaFields = requiredFields.fields)
            _state.update {
                it.copy(selectedOnetimePersonaData = sharedPersonaData)
            }

            val proofOfOwnershipRequestItem = request.proofOfOwnershipRequestItem
            if (proofOfOwnershipRequestItem != null) {
                handleProofOfOwnershipRequestItem(proofOfOwnershipRequestItem)
            } else {
                completeRequestHandling()
            }
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
                Event.NavigateToOngoingPersonaData(
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
                Event.NavigateToOneTimePersonaData(
                    oneTimePersonaDataRequestItem.toRequiredFields()
                )
            )
        }
    }

    private suspend fun handleOngoingAccountsRequestItem(
        ongoingAccountsRequestItem: DappToWalletInteraction.AccountsRequestItem,
        personaAddress: IdentityAddress
    ) {
        val dapp = requireNotNull(editedDapp)
        val numberOfAccounts = ongoingAccountsRequestItem.numberOfValues.quantity
        val isExactAccountsCount = ongoingAccountsRequestItem.numberOfValues.exactly()
        val potentialOngoingAddresses = dAppConnectionRepository.dAppAuthorizedPersonaAccountAddresses(
            dAppDefinitionAddress = dapp.dappDefinitionAddress,
            personaAddress = personaAddress,
            numberOfAccounts = numberOfAccounts,
            quantifier = ongoingAccountsRequestItem.numberOfValues.toRequestedNumberQuantifier()
        )
        if (request.resetRequestItem?.accounts == true || potentialOngoingAddresses.isEmpty()) {
            sendEvent(
                event = Event.NavigateToOngoingAccounts(
                    isExactAccountsCount = isExactAccountsCount,
                    numberOfAccounts = numberOfAccounts
                )
            )
        } else {
            val selectedAccounts = potentialOngoingAddresses.mapNotNull {
                getProfileUseCase().activeAccountOnCurrentNetwork(it) // ?.toUiModel(true)
            }.map { it.asProfileEntity() }
            _state.update { it.copy(ongoingAccountsWithSignatures = selectedAccounts.associateWith { null }) }
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

    fun onAccountPermissionGranted(
        isOneTimeRequest: Boolean,
        isExactAccountsCount: Boolean,
        numberOfAccounts: Int
    ) {
        viewModelScope.launch {
            sendEvent(
                Event.NavigateToChooseAccounts(
                    authorizedRequestInteractionId = args.interactionId,
                    isOneTimeRequest = isOneTimeRequest,
                    isExactAccountsCount = isExactAccountsCount,
                    numberOfAccounts = numberOfAccounts
                )
            )
        }
    }

    @Suppress("CyclomaticComplexMethod")
    fun onAccountsCollected(
        accountsWithSignatures: Map<ProfileEntity.AccountEntity, SignatureWithPublicKey?>,
        isOneTimeRequest: Boolean
    ) {
        val selectedPersona = state.value.personaWithSignature?.first
        requireNotNull(selectedPersona)

        val handledRequest = if (isOneTimeRequest) {
            request.oneTimeAccountsRequestItem
        } else {
            request.ongoingAccountsRequestItem
        }
        val request = request

        if (handledRequest?.isOngoing == true) {
            _state.update { it.copy(ongoingAccountsWithSignatures = accountsWithSignatures) }
            viewModelScope.launch {
                mutex.withLock {
                    editedDapp = editedDapp?.updateDAppAuthorizedPersonaSharedAccounts(
                        personaAddress = selectedPersona.identityAddress,
                        sharedAccounts = SharedToDappWithPersonaAccountAddresses(
                            request = RequestedQuantity(
                                quantifier = handledRequest.numberOfValues.toRequestedNumberQuantifier(),
                                quantity = handledRequest.numberOfValues.quantity.toUShort()
                            ),
                            ids = accountsWithSignatures.keys.map { it.accountAddress }
                        )
                    )
                }
                when {
                    request.ongoingPersonaDataRequestItem != null && request.ongoingPersonaDataRequestItem.isValid() -> {
                        handleOngoingPersonaDataRequestItem(
                            selectedPersona.identityAddress,
                            request.ongoingPersonaDataRequestItem
                        )
                    }

                    request.oneTimeAccountsRequestItem != null -> {
                        handleOneTimeAccountRequestItem(request.oneTimeAccountsRequestItem)
                    }

                    request.oneTimePersonaDataRequestItem != null && request.oneTimePersonaDataRequestItem.isValid() -> {
                        handleOneTimePersonaDataRequestItem(request.oneTimePersonaDataRequestItem)
                    }

                    request.proofOfOwnershipRequestItem != null -> {
                        handleProofOfOwnershipRequestItem(request.proofOfOwnershipRequestItem)
                    }

                    else -> {
                        completeRequestHandling()
                    }
                }
            }
        } else if (handledRequest?.isOngoing == false) {
            _state.update { it.copy(oneTimeAccountsWithSignatures = accountsWithSignatures) }
            when {
                request.oneTimePersonaDataRequestItem != null && request.oneTimePersonaDataRequestItem.isValid() -> {
                    handleOneTimePersonaDataRequestItem(request.oneTimePersonaDataRequestItem)
                }

                request.proofOfOwnershipRequestItem != null -> {
                    handleProofOfOwnershipRequestItem(request.proofOfOwnershipRequestItem)
                }

                else -> {
                    viewModelScope.launch {
                        completeRequestHandling()
                    }
                }
            }
        }
    }

    private fun handleProofOfOwnershipRequestItem(proofOfOwnershipRequestItem: WalletAuthorizedRequest.ProofOfOwnershipRequestItem) {
        viewModelScope.launch {
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
    }

    fun onRequestedEntitiesVerified(entitiesWithSignatures: Map<ProfileEntity, SignatureWithPublicKey>) {
        _state.update { state ->
            state.copy(verifiedEntities = entitiesWithSignatures)
        }
        completeRequestHandling()
    }

    fun onMessageShown() = _state.update { it.copy(uiMessage = null) }

    fun dismissNoMnemonicError() = _state.update { it.copy(isNoMnemonicErrorVisible = false) }

    fun completeRequestHandling() {
        viewModelScope.launch {
            val selectedPersona = state.value.personaWithSignature?.first // state.value.selectedPersona?.persona
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
                    authorizedPersona = selectedPersona to state.value.personaWithSignature?.second,
                    oneTimeAccountsWithSignatures = state.value.oneTimeAccountsWithSignatures,
                    ongoingAccountsWithSignatures = state.value.ongoingAccountsWithSignatures,
                    ongoingSharedPersonaData = state.value.selectedOngoingPersonaData,
                    onetimeSharedPersonaData = state.value.selectedOnetimePersonaData,
                    verifiedEntities = state.value.verifiedEntities
                ).mapCatching { walletToDappInteractionResponse ->
                    respondToIncomingRequestUseCase.respondWithSuccess(
                        request = request,
                        response = walletToDappInteractionResponse
                    ).getOrThrow()
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
                        request,
                        exception.dappWalletInteractionErrorType,
                        exception.getDappMessage()
                    )
                    _state.update { it.copy(failureDialog = FailureDialogState.Open(exception)) }
                }
            }
        }
    }
}

sealed interface Event : OneOffEvent {

    data object CloseLoginFlow : Event

    data object LoginFlowCompleted : Event

    data class NavigateToOngoingAccounts(
        val isOneTimeRequest: Boolean = false,
        val isExactAccountsCount: Boolean,
        val numberOfAccounts: Int
    ) : Event

    data class NavigateToOngoingPersonaData(
        val personaAddress: IdentityAddress,
        val requiredPersonaFields: RequiredPersonaFields
    ) : Event

    data class NavigateToOneTimePersonaData(val requiredPersonaFields: RequiredPersonaFields) : Event

    data class NavigateToChooseAccounts(
        val authorizedRequestInteractionId: String,
        val isOneTimeRequest: Boolean = false,
        val isExactAccountsCount: Boolean,
        val numberOfAccounts: Int,
        val showBack: Boolean = true
    ) : Event

    data class NavigateToVerifyPersona(
        val walletUnauthorizedRequestInteractionId: String,
        val entitiesForProofWithSignatures: EntitiesForProofWithSignatures
    ) : Event

    data class NavigateToVerifyAccounts(
        val walletUnauthorizedRequestInteractionId: String,
        val entitiesForProofWithSignatures: EntitiesForProofWithSignatures
    ) : Event
}

data class DAppLoginUiState(
    val dapp: DApp? = null,
    val uiMessage: UiMessage? = null,
    val personaWithSignature: Pair<ProfileEntity.PersonaEntity, SignatureWithPublicKey?>? = null,
    val failureDialog: FailureDialogState = FailureDialogState.Closed,
    val initialAuthorizedLoginRoute: InitialAuthorizedLoginRoute? = null,
    val ongoingAccountsWithSignatures: Map<ProfileEntity.AccountEntity, SignatureWithPublicKey?> = emptyMap(),
    val oneTimeAccountsWithSignatures: Map<ProfileEntity.AccountEntity, SignatureWithPublicKey?> = emptyMap(),
    val selectedOngoingPersonaData: PersonaData? = null,
    val selectedOnetimePersonaData: PersonaData? = null,
    val personaRequiredProofOfOwnership: IdentityAddress? = null,
    val accountsRequiredProofOfOwnership: List<AccountAddress>? = null,
    val verifiedEntities: Map<ProfileEntity, SignatureWithPublicKey> = emptyMap(),
    val isNoMnemonicErrorVisible: Boolean = false
) : UiState
