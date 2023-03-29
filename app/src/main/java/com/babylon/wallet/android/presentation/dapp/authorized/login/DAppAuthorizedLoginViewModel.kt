package com.babylon.wallet.android.presentation.dapp.authorized.login

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
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
import com.babylon.wallet.android.domain.model.DappMetadata
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.MessageFromDataChannel.IncomingRequest.AccountsRequestItem
import com.babylon.wallet.android.domain.model.MessageFromDataChannel.IncomingRequest.AuthorizedRequest
import com.babylon.wallet.android.domain.model.toProfileShareAccountsQuantifier
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.dapp.authorized.InitialAuthorizedLoginRoute
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.account.toUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.PersonaUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.toUiModel
import com.babylon.wallet.android.presentation.dapp.unauthorized.login.DAppUnauthorizedLoginEvent
import com.babylon.wallet.android.presentation.model.encodeToString
import com.babylon.wallet.android.utils.toISO8601String
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.Network.AuthorizedDapp.AuthorizedPersonaSimple
import rdx.works.profile.data.repository.AccountRepository
import rdx.works.profile.data.repository.DAppConnectionRepository
import rdx.works.profile.data.repository.PersonaRepository
import rdx.works.profile.data.repository.ProfileDataSource
import rdx.works.profile.data.repository.addOrUpdateAuthorizedDappPersona
import rdx.works.profile.data.repository.updateAuthorizedDappPersonaFields
import rdx.works.profile.data.repository.updateAuthorizedDappPersonas
import rdx.works.profile.data.repository.updateDappAuthorizedPersonaSharedAccounts
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
@Suppress("LongParameterList", "TooManyFunctions")
class DAppAuthorizedLoginViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dAppMessenger: DappMessenger,
    private val dAppConnectionRepository: DAppConnectionRepository,
    private val personaRepository: PersonaRepository,
    private val accountRepository: AccountRepository,
    private val profileDataSource: ProfileDataSource,
    private val dappMetadataRepository: DappMetadataRepository,
    private val incomingRequestRepository: IncomingRequestRepository
) : ViewModel(), OneOffEventHandler<DAppAuthorizedLoginEvent> by OneOffEventHandlerImpl() {

    private val args = DAppAuthorizedLoginArgs(savedStateHandle)

    private val mutex = Mutex()

    private val request = incomingRequestRepository.getAuthorizedRequest(
        args.requestId
    )

    private val _state = MutableStateFlow(DAppLoginUiState())
    val state = _state.asStateFlow()

    private var authorizedDapp: Network.AuthorizedDapp? = null
    private var editedDapp: Network.AuthorizedDapp? = null

    private val topLevelOneOffEventHandler = OneOffEventHandlerImpl<DAppUnauthorizedLoginEvent>()
    val topLevelOneOffEvent by topLevelOneOffEventHandler

    init {
        viewModelScope.launch {
            val currentNetworkId = profileDataSource.getCurrentNetwork().networkId().value
            if (currentNetworkId != request.requestMetadata.networkId) {
                handleWrongNetwork(currentNetworkId)
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
        val isLoginRequest = request.authRequest is AuthorizedRequest.AuthRequest.LoginRequest
        val usePersonaRequest = request.isUsePersonaAuth()
        val resetAccounts = request.resetRequestItem?.accounts == true
        if (isLoginRequest) {
            _state.update { it.copy(initialAuthorizedLoginRoute = InitialAuthorizedLoginRoute.SelectPersona(args.requestId)) }
        } else if (usePersonaRequest) {
            val dapp = authorizedDapp
            if (dapp != null) {
                setInitialDappLoginRouteForUsePersonaRequest(
                    dapp,
                    request.authRequest as AuthorizedRequest.AuthRequest.UsePersonaRequest,
                    resetAccounts
                )
            } else {
                onAbortDappLogin(WalletErrorType.InvalidPersona)
            }
        } else {
            onAbortDappLogin()
        }
    }

    @Suppress("LongMethod")
    private suspend fun setInitialDappLoginRouteForUsePersonaRequest(
        dapp: Network.AuthorizedDapp,
        authRequest: AuthorizedRequest.AuthRequest.UsePersonaRequest,
        resetAccounts: Boolean
    ) {
        val hasAuthorizedPersona = dapp.hasAuthorizedPersona(
            authRequest.personaAddress
        )
        if (hasAuthorizedPersona) {
            val persona = checkNotNull(personaRepository.getPersonaByAddress(authRequest.personaAddress))
            onSelectPersona(persona)
            val ongoingAccountsRequestItem = request.ongoingAccountsRequestItem
            val oneTimeAccountsRequestItem = request.oneTimeAccountsRequestItem
            val ongoingPersonaDataRequestItem = request.ongoingPersonaDataRequestItem
            val oneTimePersonaDataRequestItem = request.oneTimePersonaDataRequestItem
            if (ongoingAccountsRequestItem != null && (
                !requestedAccountsPermissionAlreadyGranted(
                        authRequest.personaAddress,
                        ongoingAccountsRequestItem
                    ) || resetAccounts
                )
            ) {
                _state.update {
                    it.copy(
                        initialAuthorizedLoginRoute = InitialAuthorizedLoginRoute.Permission(
                            ongoingAccountsRequestItem.numberOfAccounts,
                            isExactAccountsCount = ongoingAccountsRequestItem.quantifier.exactly()
                        )
                    )
                }
            } else {
                when {
                    ongoingPersonaDataRequestItem != null -> {
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
                    oneTimePersonaDataRequestItem != null -> {
                        _state.update { state ->
                            state.copy(
                                initialAuthorizedLoginRoute = InitialAuthorizedLoginRoute.OneTimePersonaData(
                                    oneTimePersonaDataRequestItem.fields.map { it.toKind() }.encodeToString()
                                )
                            )
                        }
                    }
                }
            }
        } else {
            onAbortDappLogin(WalletErrorType.InvalidPersona)
        }
    }

    @Suppress("MagicNumber")
    private suspend fun handleWrongNetwork(currentNetworkId: Int) {
        val failure = DappRequestFailure.WrongNetwork(
            currentNetworkId,
            request.requestMetadata.networkId
        )
        _state.update { it.copy(uiMessage = UiMessage.ErrorMessage(DappRequestException(failure))) }
        dAppMessenger.sendWalletInteractionResponseFailure(
            request.dappId,
            args.requestId,
            failure.toWalletErrorType(),
            failure.getDappMessage()
        )
        delay(4000)
        topLevelOneOffEventHandler.sendEvent(DAppUnauthorizedLoginEvent.RejectLogin)
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
            request.ongoingPersonaDataRequestItem != null -> {
                handleOngoingPersonaDataRequestItem(selectedPersona.address, request.ongoingPersonaDataRequestItem)
            }
            request.oneTimeAccountsRequestItem != null -> {
                handleOneTimeAccountRequestItem(request.oneTimeAccountsRequestItem)
            }
        }
    }

    private suspend fun handleOneTimeAccountRequestItem(
        oneTimeAccountsRequestItem: AccountsRequestItem
    ) {
        val numberOfAccounts = oneTimeAccountsRequestItem.numberOfAccounts
        val isExactAccountsCount = oneTimeAccountsRequestItem.quantifier.exactly()
        sendEvent(DAppAuthorizedLoginEvent.ChooseAccounts(numberOfAccounts, isExactAccountsCount, oneTime = true))
    }

    fun onGrantedPersonaDataOngoing() {
        viewModelScope.launch {
            val requiredFields = checkNotNull(request.ongoingPersonaDataRequestItem?.fields?.map { it.toKind() })
            val selectedPersona = checkNotNull(state.value.selectedPersona)
            personaRepository.getPersonaByAddress(selectedPersona.persona.address)?.let { updatedPersona ->
                val requiredDataFields = updatedPersona.fields.filter { requiredFields.contains(it.kind) }
                _state.update { it.copy(selectedPersona = updatedPersona.toUiModel(), selectedOngoingDataFields = requiredDataFields) }
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

    private suspend fun requestedAccountsPermissionAlreadyGranted(
        personaAddress: String,
        accountsRequestItem: AccountsRequestItem
    ): Boolean {
        return authorizedDapp?.let { dapp ->
            val potentialOngoingAddresses = dAppConnectionRepository.dAppAuthorizedPersonaAccountAddresses(
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
            val dataFields = personaRepository.getPersonaDataFields(address = personaAddress, requestItem.fields.map { it.toKind() })
            _state.update { it.copy(selectedOngoingDataFields = dataFields) }
            mutex.withLock {
                editedDapp =
                    editedDapp?.updateAuthorizedDappPersonas(
                        dapp.referencesToAuthorizedPersonas.map { ref ->
                            if (ref.identityAddress == personaAddress) {
                                ref.copy(lastUsedOn = LocalDateTime.now().toISO8601String())
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
            request.oneTimePersonaDataRequestItem != null -> {
                handleOneTimePersonaDataRequestItem(request.oneTimePersonaDataRequestItem)
            }
            else -> sendRequestResponse()
        }
    }

    private suspend fun personaDataAccessAlreadyGranted(
        requestItem: MessageFromDataChannel.IncomingRequest.PersonaRequestItem,
        personaAddress: String
    ): Boolean {
        val dapp = requireNotNull(editedDapp)
        val requestedFieldsCount = requestItem.fields.size
        val requestedFieldKinds = requestItem.fields.map { it.toKind() }
        val personaFields = personaRepository.getPersonaByAddress(personaAddress)?.fields.orEmpty()
        val requestedFieldsIds = personaFields.filter { requestedFieldKinds.contains(it.kind) }.map { it.id }
        return requestedFieldsCount == requestedFieldsIds.size && dAppConnectionRepository.dAppAuthorizedPersonaHasAllDataFields(
            dapp.dAppDefinitionAddress,
            personaAddress,
            requestedFieldsIds
        )
    }

    private fun handleOneTimePersonaDataRequestItem(oneTimePersonaRequestItem: MessageFromDataChannel.IncomingRequest.PersonaRequestItem) {
        viewModelScope.launch {
            sendEvent(DAppAuthorizedLoginEvent.PersonaDataOnetime(oneTimePersonaRequestItem.fields.map { it.toKind() }.encodeToString()))
        }
    }

    private suspend fun handleOngoingAddressRequestItem(
        ongoingAccountsRequestItem: AccountsRequestItem,
        personaAddress: String
    ) {
        val dapp = requireNotNull(editedDapp)
        val numberOfAccounts = ongoingAccountsRequestItem.numberOfAccounts
        val isExactAccountsCount = ongoingAccountsRequestItem.quantifier.exactly()
        val potentialOngoingAddresses = dAppConnectionRepository.dAppAuthorizedPersonaAccountAddresses(
            dapp.dAppDefinitionAddress,
            personaAddress,
            numberOfAccounts,
            ongoingAccountsRequestItem.quantifier.toProfileShareAccountsQuantifier()
        )
        if (request.resetRequestItem?.accounts == true || potentialOngoingAddresses.isEmpty()) {
            sendEvent(DAppAuthorizedLoginEvent.DisplayPermission(numberOfAccounts, isExactAccountsCount))
        } else {
            val selectedAccounts = potentialOngoingAddresses.mapNotNull {
                accountRepository.getAccountByAddress(it)?.toUiModel(true)
            }
            _state.update { it.copy(selectedAccountsOngoing = selectedAccounts) }
            mutex.withLock {
                editedDapp =
                    editedDapp?.updateAuthorizedDappPersonas(
                        dapp.referencesToAuthorizedPersonas.map { ref ->
                            if (ref.identityAddress == personaAddress) {
                                ref.copy(lastUsedOn = LocalDateTime.now().toISO8601String())
                            } else {
                                ref
                            }
                        }
                    )
            }
            when {
                request.ongoingPersonaDataRequestItem != null -> handleOngoingPersonaDataRequestItem(
                    personaAddress,
                    request.ongoingPersonaDataRequestItem
                )
                request.oneTimeAccountsRequestItem != null -> handleOneTimeAccountRequestItem(
                    request.oneTimeAccountsRequestItem
                )
                request.oneTimePersonaDataRequestItem != null -> {
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
            val dAppName = state.value.dappMetadata?.getName() ?: "Unknown dApp"
            mutex.withLock {
                editedDapp = Network.AuthorizedDapp(
                    request.metadata.networkId,
                    request.metadata.dAppDefinitionAddress,
                    dAppName,
                    listOf(
                        AuthorizedPersonaSimple(
                            identityAddress = selectedPersona.address,
                            fieldIDs = emptyList(),
                            lastUsedOn = date,
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
            topLevelOneOffEventHandler.sendEvent(DAppUnauthorizedLoginEvent.RejectLogin)
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
                    request.ongoingPersonaDataRequestItem != null -> {
                        handleOngoingPersonaDataRequestItem(selectedPersona.address, request.ongoingPersonaDataRequestItem)
                    }
                    request.oneTimeAccountsRequestItem != null -> {
                        handleOneTimeAccountRequestItem(request.oneTimeAccountsRequestItem)
                    }
                    request.oneTimePersonaDataRequestItem != null -> {
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
                request.oneTimePersonaDataRequestItem != null -> {
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
                state.value.dappMetadata?.getName().orEmpty(),
                showSuccessDialog = !request.isInternalRequest()
            )
        )
    }
}

sealed interface DAppAuthorizedLoginEvent : OneOffEvent {
    object RejectLogin : DAppAuthorizedLoginEvent
    data class LoginFlowCompleted(val dappName: String, val showSuccessDialog: Boolean = true) : DAppAuthorizedLoginEvent
    data class DisplayPermission(
        val numberOfAccounts: Int,
        val isExactAccountsCount: Boolean,
        val oneTime: Boolean = false
    ) : DAppAuthorizedLoginEvent

    data class PersonaDataOngoing(val personaAddress: String, val requiredFieldsEncoded: String) : DAppAuthorizedLoginEvent
    data class PersonaDataOnetime(val requiredFieldsEncoded: String) : DAppAuthorizedLoginEvent
    data class ChooseAccounts(
        val numberOfAccounts: Int,
        val isExactAccountsCount: Boolean,
        val oneTime: Boolean = false,
        val showBack: Boolean = true
    ) : DAppAuthorizedLoginEvent
}

data class DAppLoginUiState(
    val dappMetadata: DappMetadata? = null,
    val uiMessage: UiMessage? = null,
    val initialAuthorizedLoginRoute: InitialAuthorizedLoginRoute? = null,
    val selectedAccountsOngoing: List<AccountItemUiModel> = emptyList(),
    val selectedOngoingDataFields: List<Network.Persona.Field> = emptyList(),
    val selectedOnetimeDataFields: List<Network.Persona.Field> = emptyList(),
    val selectedAccountsOneTime: List<AccountItemUiModel> = emptyList(),
    val selectedPersona: PersonaUiModel? = null
)
