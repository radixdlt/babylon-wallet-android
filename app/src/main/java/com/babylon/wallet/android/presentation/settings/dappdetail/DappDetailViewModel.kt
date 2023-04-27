package com.babylon.wallet.android.presentation.settings.dappdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.repository.dappmetadata.DappMetadataRepository
import com.babylon.wallet.android.domain.common.onError
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.model.DappWithMetadata
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.account.toUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.PersonaUiModel
import com.babylon.wallet.android.presentation.model.encodeToString
import com.babylon.wallet.android.presentation.model.toQuantifierUsedInRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.UUIDGenerator
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.repository.DAppConnectionRepository
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork
import rdx.works.profile.domain.personaOnCurrentNetwork
import javax.inject.Inject

@HiltViewModel
class DappDetailViewModel @Inject constructor(
    private val dAppConnectionRepository: DAppConnectionRepository,
    private val dappMetadataRepository: DappMetadataRepository,
    private val getProfileUseCase: GetProfileUseCase,
    private val incomingRequestRepository: IncomingRequestRepository,
    savedStateHandle: SavedStateHandle
) : StateViewModel<DappDetailUiState>(), OneOffEventHandler<DappDetailEvent> by OneOffEventHandlerImpl() {

    private lateinit var authorizedDapp: Network.AuthorizedDapp
    private val args = DappDetailScreenArgs(savedStateHandle)

    override fun initialState(): DappDetailUiState = DappDetailUiState()

    init {
        viewModelScope.launch {
            val metadataResult = dappMetadataRepository.getDAppMetadata(
                definitionAddress = args.dappDefinitionAddress,
                needMostRecentData = false
            )
            metadataResult.onValue { metadata ->
                _state.update { state ->
                    state.copy(dappMetadata = metadata, loading = false)
                }
            }
            metadataResult.onError {
                _state.update { state ->
                    state.copy(loading = false)
                }
            }
        }
        observeDapp()
    }

    private fun observeDapp() {
        viewModelScope.launch {
            dAppConnectionRepository.getAuthorizedDappFlow(args.dappDefinitionAddress).collect {
                if (it == null) {
                    sendEvent(DappDetailEvent.LastPersonaDeleted)
                    return@collect
                } else {
                    authorizedDapp = checkNotNull(dAppConnectionRepository.getAuthorizedDapp(args.dappDefinitionAddress))
                }
                val personas = authorizedDapp.referencesToAuthorizedPersonas.mapNotNull { personaSimple ->
                    getProfileUseCase.personaOnCurrentNetwork(personaSimple.identityAddress)
                }
                _state.update { state ->
                    val selectedPersona = personas.firstOrNull {
                        it.address == state.selectedPersona?.persona?.address
                    } ?: state.selectedPersona?.persona
                    selectedPersona?.let { persona -> updateSelectedPersonaData(persona) }
                    state.copy(
                        dapp = authorizedDapp,
                        personas = personas.toPersistentList(),
                    )
                }
            }
        }
    }

    fun onPersonaClick(persona: Network.Persona) {
        viewModelScope.launch {
            updateSelectedPersonaData(persona)
        }
    }

    private suspend fun updateSelectedPersonaData(persona: Network.Persona) {
        val personaSimple =
            authorizedDapp.referencesToAuthorizedPersonas.firstOrNull { it.identityAddress == persona.address }
        val sharedAccounts = personaSimple?.sharedAccounts?.accountsReferencedByAddress?.mapNotNull {
            getProfileUseCase.accountOnCurrentNetwork(it)?.toUiModel()
        }.orEmpty()
        val requiredFieldIds = personaSimple?.fieldIDs.orEmpty()
        val requiredFieldKinds = persona.fields.filter { requiredFieldIds.contains(it.id) }.map {
            it.kind
        }
        _state.update {
            it.copy(
                selectedPersona = PersonaUiModel(persona, requiredFieldKinds = requiredFieldKinds),
                sharedPersonaAccounts = sharedAccounts.toPersistentList()
            )
        }
    }

    fun onPersonaDetailsClosed() {
        _state.update {
            it.copy(selectedPersona = null, sharedPersonaAccounts = persistentListOf())
        }
    }

    fun onDisconnectPersona(persona: Network.Persona) {
        viewModelScope.launch {
            dAppConnectionRepository.deletePersonaForDapp(args.dappDefinitionAddress, persona.address)
        }
    }

    fun onDeleteDapp() {
        viewModelScope.launch {
            dAppConnectionRepository.deleteAuthorizedDapp(args.dappDefinitionAddress)
            sendEvent(DappDetailEvent.DappDeleted)
        }
    }

    fun onEditPersona() {
        viewModelScope.launch {
            state.value.selectedPersona?.let { persona ->
                sendEvent(DappDetailEvent.EditPersona(persona.persona.address, persona.requiredFieldKinds.encodeToString()))
            }
        }
    }

    fun onEditAccountSharing() {
        viewModelScope.launch {
            val persona = checkNotNull(state.value.selectedPersona?.persona)
            val sharedAccounts = checkNotNull(
                authorizedDapp.referencesToAuthorizedPersonas.firstOrNull {
                    it.identityAddress == persona.address
                }?.sharedAccounts
            )
            val request = MessageFromDataChannel.IncomingRequest.AuthorizedRequest(
                dappId = "",
                requestId = UUIDGenerator.uuid().toString(),
                requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata(
                    authorizedDapp.networkID,
                    "",
                    authorizedDapp.dAppDefinitionAddress
                ),
                authRequest = MessageFromDataChannel.IncomingRequest.AuthorizedRequest.AuthRequest.UsePersonaRequest(persona.address),
                ongoingAccountsRequestItem = MessageFromDataChannel.IncomingRequest.AccountsRequestItem(
                    isOngoing = true,
                    requiresProofOfOwnership = false,
                    numberOfAccounts = sharedAccounts.request.quantity,
                    quantifier = sharedAccounts.request.quantifier.toQuantifierUsedInRequest()
                ),
                resetRequestItem = MessageFromDataChannel.IncomingRequest.ResetRequestItem(accounts = true, personaData = false)
            )
            incomingRequestRepository.add(request)
        }
    }
}

sealed interface DappDetailEvent : OneOffEvent {
    data class EditPersona(val personaAddress: String, val requiredFieldsStringEncoded: String) : DappDetailEvent
    object LastPersonaDeleted : DappDetailEvent
    object DappDeleted : DappDetailEvent
}

data class DappDetailUiState(
    val loading: Boolean = true,
    val dapp: Network.AuthorizedDapp? = null,
    val dappMetadata: DappWithMetadata? = null,
    val personas: ImmutableList<Network.Persona> = persistentListOf(),
    val selectedPersona: PersonaUiModel? = null,
    val sharedPersonaAccounts: ImmutableList<AccountItemUiModel> = persistentListOf(),
) : UiState
