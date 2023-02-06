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
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.toProfileShareAccountsMode
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
import rdx.works.profile.data.model.pernetwork.OnNetwork
import rdx.works.profile.data.model.pernetwork.OnNetwork.ConnectedDapp.AuthorizedPersonaSimple
import rdx.works.profile.data.repository.AccountRepository
import rdx.works.profile.data.repository.DAppConnectionRepository
import rdx.works.profile.data.repository.PersonaRepository
import rdx.works.profile.data.repository.ProfileDataSource
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
    private val incomingRequestRepository: IncomingRequestRepository
) : ViewModel(), OneOffEventHandler<DAppLoginEvent> by OneOffEventHandlerImpl() {

    private val args = DAppLoginArgs(savedStateHandle)

    private val authRequest =
        incomingRequestRepository.getAuthorizedRequest(
            args.requestId
        )

    private val _state = MutableStateFlow(DAppLoginUiState())
    val state = _state.asStateFlow()

    private var connectedDapp: OnNetwork.ConnectedDapp? = null

    init {
        viewModelScope.launch {
            val currentNetworkId = profileDataSource.getCurrentNetwork().networkId().value
            if (currentNetworkId != authRequest.requestMetadata.networkId) {
                handleWrongNetwork(currentNetworkId)
                return@launch
            }
            connectedDapp = dAppConnectionRepository.getConnectedDapp(
                authRequest.requestMetadata.dAppDefinitionAddress
            )
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
            val result = dappMetadataRepository.getDappMetadata(
                authRequest.metadata.dAppDefinitionAddress
            )
            result.onValue { dappMetadata ->
                _state.update {
                    it.copy(dappMetadata = dappMetadata, showProgress = false)
                }
            }
            result.onError { error ->
                _state.update { it.copy(showProgress = false, uiMessage = UiMessage.ErrorMessage(error)) }
            }
        }
        observePersonas()
    }

    @Suppress("MagicNumber")
    private suspend fun handleWrongNetwork(currentNetworkId: Int) {
        val failure = TransactionApprovalFailure.WrongNetwork(
            currentNetworkId,
            authRequest.requestMetadata.networkId
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
                    authRequest.requestMetadata.dAppDefinitionAddress
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
            if (authRequest.ongoingAccountsRequestItem != null) {
                _state.update { it.copy(processedRequestItem = authRequest.ongoingAccountsRequestItem) }
                handleOngoingAddressRequestItem(authRequest.ongoingAccountsRequestItem, connectedDapp, selectedPersona)
            } else if (authRequest.oneTimeAccountsRequestItem != null) {
                _state.update { it.copy(processedRequestItem = authRequest.oneTimeAccountsRequestItem) }
                handleOneTimeAccountRequestItem(authRequest.oneTimeAccountsRequestItem)
            }
        }
    }

    private suspend fun handleOneTimeAccountRequestItem(
        oneTimeAccountsRequestItem: MessageFromDataChannel.IncomingRequest.AccountsRequestItem
    ) {
        val numberOfAccounts = oneTimeAccountsRequestItem.numberOfAccounts
        val quantifier = oneTimeAccountsRequestItem.quantifier
        sendEvent(DAppLoginEvent.ChooseAccounts(numberOfAccounts, quantifier, oneTime = true))
    }

    private suspend fun handleOngoingAddressRequestItem(
        ongoingAccountsRequestItem: MessageFromDataChannel.IncomingRequest.AccountsRequestItem,
        connectedDapp: OnNetwork.ConnectedDapp,
        selectedPersona: OnNetwork.Persona
    ) {
        val numberOfAccounts = ongoingAccountsRequestItem.numberOfAccounts
        val quantifier = ongoingAccountsRequestItem.quantifier
        val potentialOngoingAddresses = dAppConnectionRepository.dAppConnectedPersonaAccountAddresses(
            connectedDapp.dAppDefinitionAddress,
            selectedPersona.address,
            numberOfAccounts,
            quantifier.toProfileShareAccountsMode()
        )
        if (potentialOngoingAddresses.isNotEmpty()) {
            val selectedAccounts = potentialOngoingAddresses.mapNotNull {
                accountRepository.getAccountByAddress(it)?.toUiModel(true)
            }
            _state.update { it.copy(selectedAccountsOngoing = selectedAccounts) }
            dAppConnectionRepository.updateConnectedDappPersonas(
                connectedDapp.dAppDefinitionAddress,
                connectedDapp.referencesToAuthorizedPersonas.map { ref ->
                    if (ref.identityAddress == selectedPersona.address) {
                        ref.copy(lastUsedOn = LocalDateTime.now().toISO8601String())
                    } else {
                        ref
                    }
                }
            )
            if (authRequest.oneTimeAccountsRequestItem != null) {
                _state.update { it.copy(processedRequestItem = authRequest.oneTimeAccountsRequestItem) }
                handleOneTimeAccountRequestItem(authRequest.oneTimeAccountsRequestItem)
            } else {
                sendRequestResponse()
            }
        } else {
            sendEvent(DAppLoginEvent.HandleOngoingAccounts(numberOfAccounts, quantifier))
        }
    }

    private suspend fun updateOrCreateConnectedDappWithSelectedPersona(selectedPersona: OnNetwork.Persona) {
        val connectedDapp = connectedDapp
        val date = LocalDateTime.now().toISO8601String()
        if (connectedDapp == null) {
            val dAppName = state.value.dappMetadata?.getName() ?: "Unknown dApp"
            val dApp = OnNetwork.ConnectedDapp(
                authRequest.metadata.networkId,
                authRequest.metadata.dAppDefinitionAddress,
                dAppName,
                listOf(
                    AuthorizedPersonaSimple(
                        identityAddress = selectedPersona.address,
                        fieldIDs = emptyList(),
                        lastUsedOn = date,
                        sharedAccounts = AuthorizedPersonaSimple.SharedAccounts(
                            emptyList(),
                            request = AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts(
                                AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts.Quantifier.Exactly, 0
                            )
                        )
                    )
                )
            )
            dAppConnectionRepository.addConnectedDApp(dApp)
        } else {
            val personaExists = connectedDapp.hasAuthorizedPersona(selectedPersona.address)
            if (!personaExists) {
                dAppConnectionRepository.updateConnectedDappPersonas(
                    connectedDapp.dAppDefinitionAddress,
                    listOf(
                        AuthorizedPersonaSimple(
                            identityAddress = selectedPersona.address,
                            fieldIDs = emptyList(),
                            lastUsedOn = date,
                            sharedAccounts = AuthorizedPersonaSimple.SharedAccounts(
                                emptyList(),
                                request = AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts(
                                    AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts.Quantifier.Exactly, 0
                                )
                            )
                        )
                    )
                )
            }
        }
        this.connectedDapp = dAppConnectionRepository.getConnectedDapp(authRequest.metadata.dAppDefinitionAddress)
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

    fun onSelectPersona(selectedPersona: PersonaUiModel) {
        val updatedPersonas = state.value.personas.map {
            it.copy(selected = it.persona.address == selectedPersona.persona.address)
        }.toPersistentList()
        _state.update { it.copy(personas = updatedPersonas, loginButtonEnabled = true) }
    }

    fun onPermissionAgree(
        numberOfAccounts: Int,
        quantifier: MessageFromDataChannel.IncomingRequest.AccountsRequestItem.AccountNumberQuantifier
    ) {
        viewModelScope.launch {
            sendEvent(DAppLoginEvent.ChooseAccounts(numberOfAccounts, quantifier))
        }
    }

    fun onAccountsSelected(selectedAccounts: List<AccountItemUiModel>) {
        val connectedDapp = connectedDapp
        val selectedPersona = state.value.selectedPersona()?.persona
        requireNotNull(connectedDapp)
        requireNotNull(selectedPersona)
        val request = state.value.processedRequestItem
        if (request?.isOngoing == true) {
            _state.update { it.copy(selectedAccountsOngoing = selectedAccounts) }
            viewModelScope.launch {
                dAppConnectionRepository.updateAuthorizedPersonaSharedAccounts(
                    connectedDapp.dAppDefinitionAddress,
                    selectedPersona.address,
                    AuthorizedPersonaSimple.SharedAccounts(
                        selectedAccounts.map { it.address },
                        AuthorizedPersonaSimple.SharedAccounts.NumberOfAccounts(
                            request.quantifier.toProfileShareAccountsMode(),
                            request.numberOfAccounts
                        )
                    )
                )
                if (authRequest.oneTimeAccountsRequestItem != null) {
                    _state.update { it.copy(processedRequestItem = authRequest.oneTimeAccountsRequestItem) }
                    sendEvent(
                        DAppLoginEvent.ChooseAccounts(
                            numberOfAccounts = authRequest.oneTimeAccountsRequestItem.numberOfAccounts,
                            quantifier = authRequest.oneTimeAccountsRequestItem.quantifier,
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
            state.value.selectedAccountsOneTime,
            state.value.selectedAccountsOngoing
        )
        sendEvent(DAppLoginEvent.LoginFlowCompleted(state.value.dappMetadata?.getName() ?: "Unknown dApp"))
    }
}

sealed interface DAppLoginEvent : OneOffEvent {
    object RejectLogin : DAppLoginEvent
    data class HandleOngoingAccounts(
        val numberOfAccounts: Int,
        val quantifier: MessageFromDataChannel.IncomingRequest.AccountsRequestItem.AccountNumberQuantifier,
        val oneTime: Boolean = false
    ) : DAppLoginEvent

    data class LoginFlowCompleted(val dappName: String) : DAppLoginEvent
    data class ChooseAccounts(
        val numberOfAccounts: Int,
        val quantifier: MessageFromDataChannel.IncomingRequest.AccountsRequestItem.AccountNumberQuantifier,
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
    val selectedAccountsOngoing: List<AccountItemUiModel>? = null,
    val selectedAccountsOneTime: List<AccountItemUiModel>? = null,
    val processedRequestItem: MessageFromDataChannel.IncomingRequest.AccountsRequestItem? = null,
    val personas: ImmutableList<PersonaUiModel> = persistentListOf()
) {
    fun selectedPersona(): PersonaUiModel? {
        return personas.firstOrNull { it.selected }
    }
}
