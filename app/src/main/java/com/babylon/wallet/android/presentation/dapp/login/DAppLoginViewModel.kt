package com.babylon.wallet.android.presentation.dapp.login

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.DappMessenger
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.model.WalletErrorType
import com.babylon.wallet.android.data.repository.dappmetadata.DappMetadataRepository
import com.babylon.wallet.android.data.transaction.DappRequestFailure
import com.babylon.wallet.android.data.transaction.TransactionApprovalException
import com.babylon.wallet.android.domain.common.onError
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.model.DappMetadata
import com.babylon.wallet.android.domain.model.MessageFromDataChannel.IncomingRequest.AccountsRequestItem
import com.babylon.wallet.android.domain.model.MessageFromDataChannel.IncomingRequest.AuthorizedRequest
import com.babylon.wallet.android.domain.model.toProfileShareAccountsQuantifier
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.dapp.InitialDappLoginRoute
import com.babylon.wallet.android.presentation.dapp.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.dapp.account.toUiModel
import com.babylon.wallet.android.presentation.dapp.selectpersona.PersonaUiModel
import com.babylon.wallet.android.presentation.dapp.selectpersona.toUiModel
import com.babylon.wallet.android.utils.toISO8601String
import dagger.hilt.android.lifecycle.HiltViewModel
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
import javax.inject.Inject

@HiltViewModel
@Suppress("LongParameterList", "TooManyFunctions")
class DAppLoginViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dAppMessenger: DappMessenger,
    private val dAppConnectionRepository: DAppConnectionRepository,
    private val personaRepository: PersonaRepository,
    private val accountRepository: AccountRepository,
    private val profileDataSource: ProfileDataSource,
    private val dappMetadataRepository: DappMetadataRepository,
    incomingRequestRepository: IncomingRequestRepository
) : ViewModel(), OneOffEventHandler<DAppLoginEvent> by OneOffEventHandlerImpl() {

    private val args = DAppLoginArgs(savedStateHandle)

    private val mutex = Mutex()

    private val authorizedRequest = incomingRequestRepository.getAuthorizedRequest(
        args.requestId
    )

    private val _state = MutableStateFlow(DAppLoginUiState())
    val state = _state.asStateFlow()

    private var connectedDapp: OnNetwork.ConnectedDapp? = null
    private var editedDapp: OnNetwork.ConnectedDapp? = null

    private val topLevelOneOffEventHandler = OneOffEventHandlerImpl<DAppLoginEvent>()
    val topLevelOneOffEvent by topLevelOneOffEventHandler

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
            editedDapp = connectedDapp
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
            setInitialDappLoginRoute()
        }
    }

    private suspend fun setInitialDappLoginRoute() {
        when (authorizedRequest.authRequest) {
            is AuthorizedRequest.AuthRequest.LoginRequest -> {
                _state.update { it.copy(initialDappLoginRoute = InitialDappLoginRoute.SelectPersona(args.requestId)) }
            }
            is AuthorizedRequest.AuthRequest.UsePersonaRequest -> {
                val dapp = connectedDapp
                if (dapp != null) {
                    setInitialDappLoginRouteForUsePersonaRequest(dapp, authorizedRequest.authRequest)
                } else {
                    onRejectLogin()
                }
            }
        }
    }

    private suspend fun setInitialDappLoginRouteForUsePersonaRequest(
        dapp: OnNetwork.ConnectedDapp,
        authRequest: AuthorizedRequest.AuthRequest.UsePersonaRequest
    ) {
        val hasAuthorizedPersona = dapp.hasAuthorizedPersona(
            authRequest.personaAddress
        )
        if (hasAuthorizedPersona) {
            val persona = checkNotNull(personaRepository.getPersonaByAddress(authRequest.personaAddress))
            onSelectPersona(persona)
            val ongoingAccountsRequestItem = authorizedRequest.ongoingAccountsRequestItem
            val oneTimeAccountsRequestItem = authorizedRequest.oneTimeAccountsRequestItem
            if (ongoingAccountsRequestItem != null && !requestedPermissionAlreadyGranted(
                    authRequest.personaAddress,
                    ongoingAccountsRequestItem
                )
            ) {
                _state.update {
                    it.copy(
                        initialDappLoginRoute = InitialDappLoginRoute.Permission(
                            ongoingAccountsRequestItem.numberOfAccounts,
                            isExactAccountsCount = ongoingAccountsRequestItem.quantifier.exactly()
                        )
                    )
                }
            } else if (oneTimeAccountsRequestItem != null) {
                _state.update {
                    it.copy(
                        initialDappLoginRoute = InitialDappLoginRoute.ChooseAccount(
                            oneTimeAccountsRequestItem.numberOfAccounts,
                            isExactAccountsCount = oneTimeAccountsRequestItem.quantifier.exactly(),
                            oneTime = true
                        )
                    )
                }
            }
        } else {
            onRejectLogin()
        }
    }

    @Suppress("MagicNumber")
    private suspend fun handleWrongNetwork(currentNetworkId: Int) {
        val failure = DappRequestFailure.WrongNetwork(
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
        topLevelOneOffEventHandler.sendEvent(DAppLoginEvent.RejectLogin)
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    fun onLogin() {
        viewModelScope.launch {
            val selectedPersona = state.value.selectedPersona?.persona
            requireNotNull(selectedPersona)
            updateOrCreateConnectedDappWithSelectedPersona(selectedPersona)
            if (authorizedRequest.ongoingAccountsRequestItem != null) {
                handleOngoingAddressRequestItem(
                    authorizedRequest.ongoingAccountsRequestItem,
                    selectedPersona.address
                )
            } else if (authorizedRequest.oneTimeAccountsRequestItem != null) {
                handleOneTimeAccountRequestItem(authorizedRequest.oneTimeAccountsRequestItem)
            }
        }
    }

    private suspend fun handleOneTimeAccountRequestItem(
        oneTimeAccountsRequestItem: AccountsRequestItem
    ) {
        val numberOfAccounts = oneTimeAccountsRequestItem.numberOfAccounts
        val isExactAccountsCount = oneTimeAccountsRequestItem.quantifier.exactly()
        sendEvent(DAppLoginEvent.ChooseAccounts(numberOfAccounts, isExactAccountsCount, oneTime = true))
    }

    private suspend fun requestedPermissionAlreadyGranted(
        personaAddress: String,
        accountsRequestItem: AccountsRequestItem
    ): Boolean {
        return connectedDapp?.let { dapp ->
            val potentialOngoingAddresses = dAppConnectionRepository.dAppConnectedPersonaAccountAddresses(
                dapp.dAppDefinitionAddress,
                personaAddress,
                accountsRequestItem.numberOfAccounts,
                accountsRequestItem.quantifier.toProfileShareAccountsQuantifier()
            )
            potentialOngoingAddresses.isNotEmpty()
        } ?: false
    }

    private suspend fun handleOngoingAddressRequestItem(
        ongoingAccountsRequestItem: AccountsRequestItem,
        personaAddress: String
    ) {
        val dapp = requireNotNull(editedDapp)
        val numberOfAccounts = ongoingAccountsRequestItem.numberOfAccounts
        val isExactAccountsCount = ongoingAccountsRequestItem.quantifier.exactly()
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
                editedDapp =
                    editedDapp?.updateConnectedDappPersonas(
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
                handleOneTimeAccountRequestItem(authorizedRequest.oneTimeAccountsRequestItem)
            } else {
                sendRequestResponse()
            }
        } else {
            sendEvent(DAppLoginEvent.DisplayPermission(numberOfAccounts, isExactAccountsCount))
        }
    }

    private suspend fun updateOrCreateConnectedDappWithSelectedPersona(selectedPersona: OnNetwork.Persona) {
        val dApp = connectedDapp
        val date = LocalDateTime.now().toISO8601String()
        if (dApp == null) {
            val dAppName = state.value.dappMetadata?.getName() ?: "Unknown dApp"
            mutex.withLock {
                editedDapp = OnNetwork.ConnectedDapp(
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
            mutex.withLock {
                editedDapp = connectedDapp?.updateConnectedDappPersonas(
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

    fun onRejectLogin() {
        viewModelScope.launch {
            dAppMessenger.sendWalletInteractionResponseFailure(
                args.requestId,
                error = WalletErrorType.RejectedByUser
            )
            sendEvent(DAppLoginEvent.RejectLogin)
        }
    }

    fun onSelectPersona(persona: OnNetwork.Persona) {
        _state.update { it.copy(selectedPersona = persona.toUiModel()) }
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
        val selectedPersona = state.value.selectedPersona?.persona
        requireNotNull(selectedPersona)
        val request = if (oneTimeRequest) {
            authorizedRequest.oneTimeAccountsRequestItem
        } else {
            authorizedRequest.ongoingAccountsRequestItem
        }
        if (request?.isOngoing == true) {
            _state.update { it.copy(selectedAccountsOngoing = selectedAccounts) }
            viewModelScope.launch {
                mutex.withLock {
                    editedDapp = editedDapp?.updateDappAuthorizedPersonaSharedAccounts(
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
                    sendEvent(
                        DAppLoginEvent.ChooseAccounts(
                            numberOfAccounts = authorizedRequest.oneTimeAccountsRequestItem.numberOfAccounts,
                            isExactAccountsCount = authorizedRequest.oneTimeAccountsRequestItem.quantifier.exactly(),
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
        val selectedPersona = state.value.selectedPersona?.persona
        requireNotNull(selectedPersona)
        dAppMessenger.sendWalletInteractionSuccessResponse(
            args.requestId,
            selectedPersona,
            authorizedRequest.isUsePersonaAuth(),
            state.value.selectedAccountsOneTime,
            state.value.selectedAccountsOngoing
        )
        mutex.withLock {
            editedDapp?.let { dAppConnectionRepository.updateOrCreateConnectedDApp(it) }
        }
        sendEvent(DAppLoginEvent.LoginFlowCompleted(state.value.dappMetadata?.getName() ?: "Unknown dApp"))
    }
}

sealed interface DAppLoginEvent : OneOffEvent {
    object RejectLogin : DAppLoginEvent
    data class LoginFlowCompleted(val dappName: String) : DAppLoginEvent
    data class DisplayPermission(
        val numberOfAccounts: Int,
        val isExactAccountsCount: Boolean,
        val oneTime: Boolean = false
    ) : DAppLoginEvent

    data class ChooseAccounts(
        val numberOfAccounts: Int,
        val isExactAccountsCount: Boolean,
        val oneTime: Boolean = false,
        val showBack: Boolean = true
    ) : DAppLoginEvent
}

data class DAppLoginUiState(
    val dappMetadata: DappMetadata? = null,
    val uiMessage: UiMessage? = null,
    val initialDappLoginRoute: InitialDappLoginRoute? = null,
    val selectedAccountsOngoing: List<AccountItemUiModel> = emptyList(),
    val selectedAccountsOneTime: List<AccountItemUiModel> = emptyList(),
    val selectedPersona: PersonaUiModel? = null
)
