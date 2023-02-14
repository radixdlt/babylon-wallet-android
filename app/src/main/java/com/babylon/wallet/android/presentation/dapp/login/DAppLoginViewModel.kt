package com.babylon.wallet.android.presentation.dapp.login

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.DAppMessenger
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.model.WalletErrorType
import com.babylon.wallet.android.data.repository.dappmetadata.DappMetadataRepository
import com.babylon.wallet.android.data.transaction.TransactionApprovalException
import com.babylon.wallet.android.data.transaction.TransactionApprovalFailure
import com.babylon.wallet.android.domain.common.OneOffEvent
import com.babylon.wallet.android.domain.common.OneOffEventHandler
import com.babylon.wallet.android.domain.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.domain.common.onError
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.model.DappMetadata
import com.babylon.wallet.android.domain.model.MessageFromDataChannel.IncomingRequest.AccountsRequestItem
import com.babylon.wallet.android.domain.model.MessageFromDataChannel.IncomingRequest.AuthorizedRequest
import com.babylon.wallet.android.domain.model.toProfileShareAccountsQuantifier
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.dapp.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.dapp.account.toUiModel
import com.babylon.wallet.android.utils.LAST_USED_PERSONA_DATE_FORMAT
import com.babylon.wallet.android.utils.fromISO8601String
import com.babylon.wallet.android.utils.toISO8601String
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import rdx.works.profile.data.model.pernetwork.OnNetwork
import rdx.works.profile.data.model.pernetwork.OnNetwork.ConnectedDapp.AuthorizedPersonaSimple
import rdx.works.profile.data.repository.AccountRepository
import rdx.works.profile.data.repository.DAppConnectionRepository
import rdx.works.profile.data.repository.PersonaRepository
import rdx.works.profile.data.repository.ProfileDataSource
import rdx.works.profile.data.repository.updateConnectedDappPersonas
import rdx.works.profile.data.repository.updateDappAuthorizedPersonaSharedAccounts
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
@Suppress("LongParameterList", "TooManyFunctions")
class DAppLoginViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dAppMessenger: DAppMessenger,
    private val dAppConnectionRepository: DAppConnectionRepository,
    private val personaRepository: PersonaRepository,
    private val accountRepository: AccountRepository,
    private val profileDataSource: ProfileDataSource,
    private val dappMetadataRepository: DappMetadataRepository,
    incomingRequestRepository: IncomingRequestRepository
) : ViewModel(), OneOffEventHandler<DAppLoginEvent> by OneOffEventHandlerImpl() {

    private val args = DAppLoginArgs(savedStateHandle)

    private val mutex = Mutex()

    private val authorizedRequest =
        incomingRequestRepository.getAuthorizedRequest(
            args.requestId
        )

    private val _state = MutableStateFlow(DAppLoginUiState())
    val state = _state.asStateFlow()
    private val isUsePersonaRequest = authorizedRequest.isUsePersonaAuth()

    private var connectedDapp: OnNetwork.ConnectedDapp? = null

    init {
        viewModelScope.launch {
            val currentNetworkId = profileDataSource.getCurrentNetwork().networkId().value
            if (currentNetworkId != authorizedRequest.requestMetadata.networkId) {
                handleWrongNetwork(currentNetworkId)
                return@launch
            }
            connectedDapp = dAppConnectionRepository.getConnectedDapp(
                authorizedRequest.requestMetadata.dAppDefinitionAddress
            )
            val result = dappMetadataRepository.getDappMetadata(
                authorizedRequest.metadata.dAppDefinitionAddress
            )
            result.onValue { dappMetadata ->
                _state.update {
                    it.copy(dappMetadata = dappMetadata)
                }
            }
            result.onError { error ->
                _state.update { it.copy(uiMessage = UiMessage.ErrorMessage(error)) }
            }
            val allAuthorizedPersonas =
                connectedDapp?.referencesToAuthorizedPersonas
            _state.update { state ->
                val personas = generatePersonasListForDisplay(
                    allAuthorizedPersonas,
                    state.personas
                ).toPersistentList()
                val selected = personas.any { it.selected }
                state.copy(
                    firstTimeLogin = connectedDapp == null,
                    personas = personas,
                    loginButtonEnabled = selected
                )
            }
            if (isUsePersonaRequest) {
                if (connectedDapp != null && authorizedRequest.isUsePersonaAuth()) {
                    authorizedRequest.authRequest as AuthorizedRequest.AuthRequest.UsePersonaRequest
                    val connectedDapp = checkNotNull(connectedDapp)
                    val hasAuthorizedPersona = connectedDapp.hasAuthorizedPersona(
                        authorizedRequest.authRequest.personaAddress
                    )
                    if (hasAuthorizedPersona) {
                        onSelectPersona(authorizedRequest.authRequest.personaAddress)
                        handleUsePersonaAuthRequest()
                    } else {
                        onRejectLogin()
                    }
                    _state.update { it.copy(showProgress = false) }
                }
            } else {
                _state.update { it.copy(showProgress = false) }
            }
        }
        observePersonas()
    }

    private suspend fun handleUsePersonaAuthRequest() {
        authorizedRequest.authRequest as AuthorizedRequest.AuthRequest.UsePersonaRequest
        val connectedDapp = checkNotNull(connectedDapp)
        val hasAuthorizedPersona = connectedDapp.hasAuthorizedPersona(
            authorizedRequest.authRequest.personaAddress
        )
        if (hasAuthorizedPersona) {
            onSelectPersona(authorizedRequest.authRequest.personaAddress)
            val personaAddress = authorizedRequest.authRequest.personaAddress
            personaRepository.getPersonaByAddress(personaAddress)
            if (authorizedRequest.ongoingAccountsRequestItem != null) {
                val ongoingRequestItem = checkNotNull(authorizedRequest.ongoingAccountsRequestItem)
                handleOngoingAddressRequestItem(ongoingRequestItem, personaAddress, false)
            } else if (authorizedRequest.oneTimeAccountsRequestItem != null) {
                _state.update { it.copy(processedRequestItem = authorizedRequest.oneTimeAccountsRequestItem) }
                handleOneTimeAccountRequestItem(authorizedRequest.oneTimeAccountsRequestItem)
            }
        } else {
            onRejectLogin()
        }
    }

    @Suppress("MagicNumber")
    private suspend fun handleWrongNetwork(currentNetworkId: Int) {
        val failure = TransactionApprovalFailure.WrongNetwork(
            currentNetworkId,
            authorizedRequest.requestMetadata.networkId
        )
        _state.update { it.copy(uiMessage = UiMessage.ErrorMessage(TransactionApprovalException(failure))) }
        dAppMessenger.sendWalletInteractionResponseFailure(
            args.requestId,
            failure.toWalletErrorType(),
            failure.getDappMessage()
        )
        delay(4000)
        sendEvent(DAppLoginEvent.RejectLogin)
    }

    private fun observePersonas() {
        viewModelScope.launch {
            personaRepository.personas.collect { personas ->
                connectedDapp = dAppConnectionRepository.getConnectedDapp(
                    authorizedRequest.requestMetadata.dAppDefinitionAddress
                )
                val allAuthorizedPersonas =
                    connectedDapp?.referencesToAuthorizedPersonas
                _state.update { state ->
                    state.copy(
                        personas = generatePersonasListForDisplay(
                            allAuthorizedPersonas,
                            personas.map { it.toUiModel() }
                        ).toPersistentList()
                    )
                }
            }
        }
    }

    private suspend fun generatePersonasListForDisplay(
        allAuthorizedPersonas: List<AuthorizedPersonaSimple>?,
        profilePersonas: List<PersonaUiModel>
    ): List<PersonaUiModel> {
        val defaultAuthorizedPersonaSimple = allAuthorizedPersonas?.firstOrNull()
        val authorizedPersona = defaultAuthorizedPersonaSimple?.let {
            personaRepository.getPersonaByAddress(it.identityAddress)
        }?.let {
            PersonaUiModel(
                it,
                selected = true,
                pinned = true,
                lastUsedOn = defaultAuthorizedPersonaSimple.lastUsedOn.fromISO8601String()
                    ?.format(DateTimeFormatter.ofPattern(LAST_USED_PERSONA_DATE_FORMAT)),
            )
        }
        var updatedPersonas = profilePersonas.map { personaUiModel ->
            val matchingAuthorizedPersona = allAuthorizedPersonas?.firstOrNull {
                personaUiModel.persona.address == it.identityAddress
            }
            if (matchingAuthorizedPersona != null) {
                personaUiModel.copy(
                    lastUsedOn = matchingAuthorizedPersona.lastUsedOn.fromISO8601String()
                        ?.format(DateTimeFormatter.ofPattern(LAST_USED_PERSONA_DATE_FORMAT)),
                )
            } else {
                personaUiModel
            }
        }
        val selectedPersona = state.value.personas.firstOrNull { it.selected }
        updatedPersonas = if (selectedPersona != null && selectedPersona != authorizedPersona) {
            updatedPersonas.map { p -> p.copy(selected = p.persona.address == selectedPersona.persona.address) }
        } else {
            updatedPersonas.filter { it.persona.address != authorizedPersona?.persona?.address }
        }
        return authorizedPersona?.let { listOf(authorizedPersona) }.orEmpty() + updatedPersonas
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    fun onLogin() {
        viewModelScope.launch {
            val selectedPersona = state.value.selectedPersona()?.persona
            requireNotNull(selectedPersona)
            updateOrCreateConnectedDappWithSelectedPersona(selectedPersona)
            val connectedDapp = connectedDapp
            requireNotNull(connectedDapp)
            if (authorizedRequest.ongoingAccountsRequestItem != null) {
                _state.update { it.copy(processedRequestItem = authorizedRequest.ongoingAccountsRequestItem) }
                handleOngoingAddressRequestItem(
                    authorizedRequest.ongoingAccountsRequestItem,
                    selectedPersona.address
                )
            } else if (authorizedRequest.oneTimeAccountsRequestItem != null) {
                _state.update { it.copy(processedRequestItem = authorizedRequest.oneTimeAccountsRequestItem) }
                handleOneTimeAccountRequestItem(authorizedRequest.oneTimeAccountsRequestItem)
            }
        }
    }

    private suspend fun handleOneTimeAccountRequestItem(
        oneTimeAccountsRequestItem: AccountsRequestItem
    ) {
        val numberOfAccounts = oneTimeAccountsRequestItem.numberOfAccounts
        val isExactAccountsCount =
            oneTimeAccountsRequestItem.quantifier == AccountsRequestItem.AccountNumberQuantifier.Exactly
        sendEvent(DAppLoginEvent.ChooseAccounts(numberOfAccounts, isExactAccountsCount, oneTime = true))
    }

    private suspend fun handleOngoingAddressRequestItem(
        ongoingAccountsRequestItem: AccountsRequestItem,
        personaAddress: String,
        transitionToAccountSelection: Boolean = true
    ) {
        val dapp = requireNotNull(connectedDapp)
        val numberOfAccounts = ongoingAccountsRequestItem.numberOfAccounts
        val isExactAccountsCount =
            ongoingAccountsRequestItem.quantifier == AccountsRequestItem.AccountNumberQuantifier.Exactly
        val potentialOngoingAddresses = dAppConnectionRepository.dAppConnectedPersonaAccountAddresses(
            dapp.dAppDefinitionAddress,
            personaAddress,
            numberOfAccounts,
            ongoingAccountsRequestItem.quantifier.toProfileShareAccountsQuantifier()
        )
        if (potentialOngoingAddresses.isNotEmpty()) {
            val selectedAccounts = potentialOngoingAddresses.mapNotNull {
                accountRepository.getAccountByAddress(it)?.toUiModel(true)
            }
            _state.update { it.copy(selectedAccountsOngoing = selectedAccounts) }
            mutex.withLock {
                connectedDapp =
                    connectedDapp?.updateConnectedDappPersonas(
                        dapp.referencesToAuthorizedPersonas.map { ref ->
                            if (ref.identityAddress == personaAddress) {
                                ref.copy(lastUsedOn = LocalDateTime.now().toISO8601String())
                            } else {
                                ref
                            }
                        }
                    )
            }
            if (authorizedRequest.oneTimeAccountsRequestItem != null) {
                _state.update { it.copy(processedRequestItem = authorizedRequest.oneTimeAccountsRequestItem) }
                handleOneTimeAccountRequestItem(authorizedRequest.oneTimeAccountsRequestItem)
            } else {
                sendRequestResponse()
            }
        } else if (transitionToAccountSelection) {
            sendEvent(DAppLoginEvent.HandleOngoingAccounts(numberOfAccounts, isExactAccountsCount))
        }
    }

    private suspend fun updateOrCreateConnectedDappWithSelectedPersona(selectedPersona: OnNetwork.Persona) {
        val dApp = connectedDapp
        val date = LocalDateTime.now().toISO8601String()
        if (dApp == null) {
            val dAppName = state.value.dappMetadata?.getName() ?: "Unknown dApp"
            mutex.withLock {
                connectedDapp = OnNetwork.ConnectedDapp(
                    authorizedRequest.metadata.networkId,
                    authorizedRequest.metadata.dAppDefinitionAddress,
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
            val personaExists = dApp.hasAuthorizedPersona(selectedPersona.address)
            if (!personaExists) {
                mutex.withLock {
                    connectedDapp = connectedDapp?.updateConnectedDappPersonas(
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
            }
        }
    }

    fun onRejectLogin() {
        viewModelScope.launch {
            dAppMessenger.sendWalletInteractionResponseFailure(
                args.requestId,
                error = WalletErrorType.RejectedByUser
            )
            sendEvent(DAppLoginEvent.RejectLogin)
        }
    }

    fun onSelectPersona(personaAddress: String) {
        val updatedPersonas = state.value.personas.map {
            it.copy(selected = it.persona.address == personaAddress)
        }.toPersistentList()
        _state.update { it.copy(personas = updatedPersonas, loginButtonEnabled = true) }
    }

    fun onPermissionAgree(
        numberOfAccounts: Int,
        isExactAccountsCount: Boolean
    ) {
        viewModelScope.launch {
            sendEvent(DAppLoginEvent.ChooseAccounts(numberOfAccounts, isExactAccountsCount))
        }
    }

    fun onAccountsSelected(selectedAccounts: List<AccountItemUiModel>) {
        val dApp = connectedDapp
        val selectedPersona = state.value.selectedPersona()?.persona
        requireNotNull(dApp)
        requireNotNull(selectedPersona)
        val request = state.value.processedRequestItem
        if (request?.isOngoing == true) {
            _state.update { it.copy(selectedAccountsOngoing = selectedAccounts) }
            viewModelScope.launch {
                mutex.withLock {
                    connectedDapp = connectedDapp?.updateDappAuthorizedPersonaSharedAccounts(
                        selectedPersona.address,
                        AuthorizedPersonaSimple.SharedAccounts(
                            selectedAccounts.map { it.address },
                            AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts(
                                request.quantifier.toProfileShareAccountsQuantifier(),
                                request.numberOfAccounts
                            )
                        )
                    )
                }
                if (authorizedRequest.oneTimeAccountsRequestItem != null) {
                    _state.update { it.copy(processedRequestItem = authorizedRequest.oneTimeAccountsRequestItem) }
                    sendEvent(
                        DAppLoginEvent.ChooseAccounts(
                            numberOfAccounts = authorizedRequest.oneTimeAccountsRequestItem.numberOfAccounts,
                            isExactAccountsCount = authorizedRequest.oneTimeAccountsRequestItem.quantifier
                                == AccountsRequestItem.AccountNumberQuantifier.Exactly,
                            oneTime = true
                        )
                    )
                } else {
                    sendRequestResponse()
                }
            }
        } else if (request?.isOngoing == false) {
            _state.update { it.copy(selectedAccountsOneTime = selectedAccounts) }
            viewModelScope.launch {
                sendRequestResponse()
            }
        }
    }

    private suspend fun sendRequestResponse() {
        val selectedPersona = state.value.selectedPersona()?.persona
        requireNotNull(selectedPersona)
        _state.update { it.copy(processedRequestItem = null) }
        dAppMessenger.sendWalletInteractionSuccessResponse(
            args.requestId,
            selectedPersona,
            authorizedRequest.isUsePersonaAuth(),
            state.value.selectedAccountsOneTime,
            state.value.selectedAccountsOngoing
        )
        mutex.withLock {
            connectedDapp?.let { dAppConnectionRepository.updateOrCreateConnectedDApp(it) }
        }
        sendEvent(DAppLoginEvent.LoginFlowCompleted(state.value.dappMetadata?.getName() ?: "Unknown dApp"))
    }
}

sealed interface DAppLoginEvent : OneOffEvent {
    object RejectLogin : DAppLoginEvent
    object SkipSelectPersona : DAppLoginEvent
    data class HandleOngoingAccounts(
        val numberOfAccounts: Int,
        val isExactAccountsCount: Boolean,
        val oneTime: Boolean = false
    ) : DAppLoginEvent

    data class LoginFlowCompleted(val dappName: String) : DAppLoginEvent
    data class ChooseAccounts(
        val numberOfAccounts: Int,
        val isExactAccountsCount: Boolean,
        val oneTime: Boolean = false
    ) : DAppLoginEvent
}

data class DAppLoginUiState(
    val dappMetadata: DappMetadata? = null,
    val loginButtonEnabled: Boolean = false,
    val uiMessage: UiMessage? = null,
    val firstTimeLogin: Boolean = true,
    val showProgress: Boolean = true,
    val isOngoing: Boolean = false,
    val selectedAccountsOngoing: List<AccountItemUiModel> = emptyList(),
    val selectedAccountsOneTime: List<AccountItemUiModel> = emptyList(),
    val processedRequestItem: AccountsRequestItem? = null,
    val personas: ImmutableList<PersonaUiModel> = persistentListOf()
) {
    fun selectedPersona(): PersonaUiModel? {
        return personas.firstOrNull { it.selected }
    }
}
