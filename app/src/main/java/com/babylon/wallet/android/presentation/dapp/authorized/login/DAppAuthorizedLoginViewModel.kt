package com.babylon.wallet.android.presentation.dapp.authorized.login

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
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.MessageFromDataChannel.IncomingRequest.AccountsRequestItem
import com.babylon.wallet.android.domain.model.MessageFromDataChannel.IncomingRequest.AuthorizedRequest
import com.babylon.wallet.android.domain.model.toProfileShareAccountsQuantifier
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.dapp.authorized.InitialAuthorizedLoginRoute
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.account.toUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.PersonaUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.toUiModel
import com.babylon.wallet.android.presentation.model.encodeToString
import com.babylon.wallet.android.utils.toISO8601String
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
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
@Suppress("LongParameterList", "TooManyFunctions")
class DAppAuthorizedLoginViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dAppMessenger: DappMessenger,
    private val dAppConnectionRepository: DAppConnectionRepository,
    private val getProfileUseCase: GetProfileUseCase,
    private val getCurrentGatewayUseCase: GetCurrentGatewayUseCase,
    private val dappMetadataRepository: DappMetadataRepository,
    private val incomingRequestRepository: IncomingRequestRepository
) : StateViewModel<DAppLoginUiState>(), OneOffEventHandler<DAppAuthorizedLoginEvent> by OneOffEventHandlerImpl() {

    private val args = DAppAuthorizedLoginArgs(savedStateHandle)

    private val mutex = Mutex()

    private val request = incomingRequestRepository.getAuthorizedRequest(
        args.requestId
    )

    private var authorizedDapp: Network.AuthorizedDapp? = null
    private var editedDapp: Network.AuthorizedDapp? = null

    private val topLevelOneOffEventHandler = OneOffEventHandlerImpl<DAppAuthorizedLoginEvent>()
    val topLevelOneOffEvent by topLevelOneOffEventHandler

    override fun initialState(): DAppLoginUiState = DAppLoginUiState()

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
            authorizedDapp = dAppConnectionRepository.getAuthorizedDapp(
                request.requestMetadata.dAppDefinitionAddress
            )
            editedDapp = authorizedDapp
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

    private suspend fun setInitialDappLoginRoute() {
        when (val request = request.authRequest) {
            is AuthorizedRequest.AuthRequest.LoginRequest.WithChallenge -> {
                // TODO temporary until flow with challenge is implemented
                onAbortDappLogin()
            }
            is AuthorizedRequest.AuthRequest.LoginRequest.WithoutChallenge -> {
                _state.update {
                    it.copy(
                        initialAuthorizedLoginRoute = InitialAuthorizedLoginRoute.SelectPersona(
                            args.requestId
                        )
                    )
                }
            }
            is AuthorizedRequest.AuthRequest.UsePersonaRequest -> {
                val dapp = authorizedDapp
                if (dapp != null) {
                    setInitialDappLoginRouteForUsePersonaRequest(dapp, request)
                } else {
                    onAbortDappLogin(WalletErrorType.InvalidPersona)
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
            args.requestId,
            failure.toWalletErrorType(),
            failure.getDappMessage()
        )
        delay(2000)
        topLevelOneOffEventHandler.sendEvent(DAppAuthorizedLoginEvent.RejectLogin)
        incomingRequestRepository.requestHandled(requestId = args.requestId)
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
                sendRequestResponse()
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
                sendRequestResponse()
            }
        }
    }

    private suspend fun handleOneTimeAccountRequestItem(
        oneTimeAccountsRequestItem: AccountsRequestItem
    ) {
        val numberOfAccounts = oneTimeAccountsRequestItem.numberOfAccounts
        val isExactAccountsCount = oneTimeAccountsRequestItem.quantifier.exactly()
        sendEvent(
            DAppAuthorizedLoginEvent.ChooseAccounts(
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
            sendRequestResponse()
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
                DAppAuthorizedLoginEvent.PersonaDataOngoing(
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
            else -> sendRequestResponse()
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
                DAppAuthorizedLoginEvent.PersonaDataOnetime(
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
                DAppAuthorizedLoginEvent.DisplayPermission(
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
                else -> sendRequestResponse()
            }
        }
    }

    private suspend fun updateOrCreateAuthorizedDappWithSelectedPersona(selectedPersona: Network.Persona) {
        val dApp = authorizedDapp
        val date = LocalDateTime.now().toISO8601String()
        if (dApp == null) {
            val dAppName = state.value.dappMetadata?.getName().orEmpty().ifEmpty { "Unknown dApp" }
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
                    args.requestId,
                    error = walletWalletErrorType
                )
            }
            topLevelOneOffEventHandler.sendEvent(DAppAuthorizedLoginEvent.RejectLogin)
            incomingRequestRepository.requestHandled(requestId = args.requestId)
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
                DAppAuthorizedLoginEvent.ChooseAccounts(
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
                        sendRequestResponse()
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
                        sendRequestResponse()
                    }
                }
            }
        }
    }

    private suspend fun sendRequestResponse() {
        val selectedPersona = state.value.selectedPersona?.persona
        requireNotNull(selectedPersona)
        if (request.isInternalRequest()) {
            incomingRequestRepository.requestHandled(request.requestId)
        } else {
            dAppMessenger.sendWalletInteractionAuthorizedSuccessResponse(
                request.dappId,
                args.requestId,
                selectedPersona,
                request.isUsePersonaAuth(),
                state.value.selectedAccountsOneTime,
                state.value.selectedAccountsOngoing,
                state.value.selectedOngoingDataFields,
                state.value.selectedOnetimeDataFields,
            )
        }
        mutex.withLock {
            editedDapp?.let { dAppConnectionRepository.updateOrCreateAuthorizedDApp(it) }
        }
        sendEvent(
            DAppAuthorizedLoginEvent.LoginFlowCompleted(
                requestId = request.requestId,
                dAppName = state.value.dappMetadata?.getName().orEmpty(),
                showSuccessDialog = !request.isInternalRequest()
            )
        )
    }
}

sealed interface DAppAuthorizedLoginEvent : OneOffEvent {

    object RejectLogin : DAppAuthorizedLoginEvent

    data class LoginFlowCompleted(
        val requestId: String,
        val dAppName: String,
        val showSuccessDialog: Boolean = true
    ) : DAppAuthorizedLoginEvent

    data class DisplayPermission(
        val numberOfAccounts: Int,
        val isExactAccountsCount: Boolean,
        val oneTime: Boolean = false
    ) : DAppAuthorizedLoginEvent

    data class PersonaDataOngoing(
        val personaAddress: String,
        val requiredFieldsEncoded: String
    ) : DAppAuthorizedLoginEvent

    data class PersonaDataOnetime(val requiredFieldsEncoded: String) : DAppAuthorizedLoginEvent

    data class ChooseAccounts(
        val numberOfAccounts: Int,
        val isExactAccountsCount: Boolean,
        val oneTime: Boolean = false,
        val showBack: Boolean = true
    ) : DAppAuthorizedLoginEvent
}

data class DAppLoginUiState(
    val dappMetadata: DappWithMetadata? = null,
    val uiMessage: UiMessage? = null,
    val initialAuthorizedLoginRoute: InitialAuthorizedLoginRoute? = null,
    val selectedAccountsOngoing: List<AccountItemUiModel> = emptyList(),
    val selectedOngoingDataFields: List<Network.Persona.Field> = emptyList(),
    val selectedOnetimeDataFields: List<Network.Persona.Field> = emptyList(),
    val selectedAccountsOneTime: List<AccountItemUiModel> = emptyList(),
    val selectedPersona: PersonaUiModel? = null
) : UiState
