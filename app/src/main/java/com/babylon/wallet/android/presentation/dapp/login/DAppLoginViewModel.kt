package com.babylon.wallet.android.presentation.dapp.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.pernetwork.OnNetwork
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

    // the incoming request from dapp
    private val authRequest =
        incomingRequestRepository.getAuthorizedRequest(
            args.requestId
        )

    var state by mutableStateOf(DAppLoginUiState())
        private set

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
            state = state.copy(
                firstTimeLogin = connectedDapp == null,
                personas = generatePersonasListForDisplay(allAuthorizedPersonas, state.personas).toPersistentList(),
                loginButtonEnabled = allAuthorizedPersonas?.any() != null
            )
            val result = dappMetadataRepository.getDappMetadata(
                authRequest.metadata.dAppDefinitionAddress
            )
            result.onValue {
                state = state.copy(dappMetadata = it, showProgress = false)
            }
            result.onError {
                state = state.copy(showProgress = false, uiMessage = UiMessage.ErrorMessage(it))
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
        state = state.copy(uiMessage = UiMessage.ErrorMessage(TransactionApprovalException(failure)))
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
                state = state.copy(
                    personas = generatePersonasListForDisplay(
                        allAuthorizedPersonas,
                        personas.map { it.toUiModel() }
                    ).toPersistentList()
                )
            }
        }
    }

    private suspend fun generatePersonasListForDisplay(
        allAuthorizedPersonas: List<OnNetwork.ConnectedDapp.AuthorizedPersonaSimple>?,
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
                sharedAccountNumber = defaultAuthorizedPersonaSimple.sharedAccounts.accountsReferencedByAddress.size
            )
        }
        var updatedPersonas = profilePersonas.map { personaUiModel ->
            val matchingAuthorizedPersona = allAuthorizedPersonas?.firstOrNull {
                personaUiModel.persona.address == it.identityAddress
            }
            if (matchingAuthorizedPersona != null) {
                personaUiModel.copy(
                    sharedAccountNumber = matchingAuthorizedPersona.sharedAccounts.accountsReferencedByAddress.size,
                    lastUsedOn = matchingAuthorizedPersona.lastUsedOn.fromISO8601String()
                        ?.format(DateTimeFormatter.ofPattern(LAST_USED_PERSONA_DATE_FORMAT)),
                )
            } else {
                personaUiModel
            }
        }
        val selectedPersona = state.personas.firstOrNull { it.selected }
        updatedPersonas = if (selectedPersona != null && selectedPersona != authorizedPersona) {
            updatedPersonas.map { p -> p.copy(selected = p.persona.address == selectedPersona.persona.address) }
        } else {
            updatedPersonas.filter { it.persona.address != authorizedPersona?.persona?.address }
        }
        return authorizedPersona?.let { listOf(authorizedPersona) }.orEmpty() + updatedPersonas
    }

    fun onMessageShown() {
        state = state.copy(uiMessage = null)
    }

    fun onLogin() {
        viewModelScope.launch {
            val selectedPersona = state.selectedPersona()?.persona
            requireNotNull(selectedPersona)
            updateOrCreateConnectedDappWithSelectedPersona(selectedPersona)
            val connectedDapp = connectedDapp
            requireNotNull(connectedDapp)
            if (authRequest.ongoingAccountsRequestItem != null) {
                state = state.copy(processedRequestItem = authRequest.ongoingAccountsRequestItem)
                handleOngoingAddressRequestItem(authRequest.ongoingAccountsRequestItem, connectedDapp, selectedPersona)
            } else if (authRequest.oneTimeAccountsRequestItem != null) {
                state = state.copy(processedRequestItem = authRequest.oneTimeAccountsRequestItem)
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
            state = state.copy(selectedAccountsOngoing = selectedAccounts)
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
                state = state.copy(processedRequestItem = authRequest.oneTimeAccountsRequestItem)
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
            val dAppName = state.dappMetadata?.getName()
            val dApp = OnNetwork.ConnectedDapp(
                authRequest.metadata.networkId,
                authRequest.metadata.dAppDefinitionAddress,
                dAppName.orEmpty(),
                listOf(
                    OnNetwork.ConnectedDapp.AuthorizedPersonaSimple(
                        identityAddress = selectedPersona.address,
                        fieldIDs = emptyList(),
                        lastUsedOn = date,
                        sharedAccounts = OnNetwork.ConnectedDapp.AuthorizedPersonaSimple.SharedAccounts(
                            emptyList(),
                            mode = OnNetwork.ConnectedDapp.AuthorizedPersonaSimple.SharedAccounts.Mode.Exactly
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
                        OnNetwork.ConnectedDapp.AuthorizedPersonaSimple(
                            identityAddress = selectedPersona.address,
                            fieldIDs = emptyList(),
                            lastUsedOn = date,
                            sharedAccounts = OnNetwork.ConnectedDapp.AuthorizedPersonaSimple.SharedAccounts(
                                emptyList(),
                                mode = OnNetwork.ConnectedDapp.AuthorizedPersonaSimple.SharedAccounts.Mode.Exactly
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
        val updatedPersonas = state.personas.map {
            it.copy(selected = it.persona.address == selectedPersona.persona.address)
        }.toPersistentList()
        state = state.copy(personas = updatedPersonas, loginButtonEnabled = true)
    }

    fun onPermissionAgree(
        numberOfAccounts: Int,
        quantifier: MessageFromDataChannel.IncomingRequest.AccountNumberQuantifier
    ) {
        viewModelScope.launch {
            sendEvent(DAppLoginEvent.ChooseAccounts(numberOfAccounts, quantifier))
        }
    }

    fun onAccountsSelected(selectedAccounts: List<AccountItemUiModel>) {
        val connectedDapp = connectedDapp
        val selectedPersona = state.selectedPersona()?.persona
        requireNotNull(connectedDapp)
        requireNotNull(selectedPersona)
        val request = state.processedRequestItem
        if (request?.isOngoing == true) {
            state = state.copy(selectedAccountsOngoing = selectedAccounts)
            viewModelScope.launch {
                dAppConnectionRepository.updateAuthorizedPersonaSharedAccounts(
                    connectedDapp.dAppDefinitionAddress,
                    selectedPersona.address,
                    OnNetwork.ConnectedDapp.AuthorizedPersonaSimple.SharedAccounts(
                        selectedAccounts.map { it.address },
                        request.quantifier.toProfileShareAccountsMode()
                    )
                )
                if (authRequest.oneTimeAccountsRequestItem != null) {
                    state = state.copy(processedRequestItem = authRequest.oneTimeAccountsRequestItem)
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
            state = state.copy(selectedAccountsOneTime = selectedAccounts)
            viewModelScope.launch {
                sendRequestResponse()
            }
        }
    }

    private suspend fun sendRequestResponse() {
        val selectedPersona = state.selectedPersona()?.persona
        requireNotNull(selectedPersona)
        state = state.copy(processedRequestItem = null)
        dAppMessenger.sendWalletInteractionSuccessResponse(
            args.requestId,
            selectedPersona,
            state.selectedAccountsOneTime,
            state.selectedAccountsOngoing
        )
        sendEvent(DAppLoginEvent.LoginFlowCompleted)
    }
}

sealed interface DAppLoginEvent : OneOffEvent {
    object RejectLogin : DAppLoginEvent
    data class HandleOngoingAccounts(
        val numberOfAccounts: Int,
        val quantifier: MessageFromDataChannel.IncomingRequest.AccountNumberQuantifier,
        val oneTime: Boolean = false
    ) : DAppLoginEvent

    object LoginFlowCompleted : DAppLoginEvent
    data class ChooseAccounts(
        val numberOfAccounts: Int,
        val quantifier: MessageFromDataChannel.IncomingRequest.AccountNumberQuantifier,
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
