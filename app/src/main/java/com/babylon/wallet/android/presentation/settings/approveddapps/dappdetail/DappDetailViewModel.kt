package com.babylon.wallet.android.presentation.settings.approveddapps.dappdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.domain.model.DAppWithResources
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.RequiredPersonaField
import com.babylon.wallet.android.domain.model.RequiredPersonaFields
import com.babylon.wallet.android.domain.usecases.GetDAppWithResourcesUseCase
import com.babylon.wallet.android.domain.usecases.GetValidatedDAppWebsiteUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.dapp.authorized.account.AccountItemUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.account.toUiModel
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.PersonaUiModel
import com.babylon.wallet.android.presentation.model.toQuantifierUsedInRequest
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.extensions.init
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.UUIDGenerator
import rdx.works.core.domain.resources.Resource
import rdx.works.core.then
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.repository.DAppConnectionRepository
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork
import rdx.works.profile.domain.personaOnCurrentNetwork
import javax.inject.Inject

@Suppress("TooManyFunctions")
@HiltViewModel
class DappDetailViewModel @Inject constructor(
    private val dAppConnectionRepository: DAppConnectionRepository,
    private val dAppWithAssociatedResourcesUseCase: GetDAppWithResourcesUseCase,
    private val getValidatedDAppWebsiteUseCase: GetValidatedDAppWebsiteUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val incomingRequestRepository: IncomingRequestRepository,
    savedStateHandle: SavedStateHandle
) : StateViewModel<DappDetailUiState>(), OneOffEventHandler<DappDetailEvent> by OneOffEventHandlerImpl() {

    private lateinit var authorizedDapp: Network.AuthorizedDapp
    private val args = DappDetailScreenArgs(savedStateHandle)

    override fun initialState(): DappDetailUiState = DappDetailUiState()

    init {
        viewModelScope.launch {
            dAppWithAssociatedResourcesUseCase(
                definitionAddress = args.dappDefinitionAddress,
                needMostRecentData = false
            ).onSuccess { dAppWithAssociatedResources ->
                _state.update { state ->
                    state.copy(
                        dAppWithResources = dAppWithAssociatedResources,
                        loading = false,
                    )
                }
            }.then { dAppWithResources ->
                if (dAppWithResources.dApp.claimedWebsites.isNotEmpty()) {
                    _state.update { it.copy(isValidatingWebsite = true) }
                    getValidatedDAppWebsiteUseCase(dAppWithResources.dApp).onSuccess { website ->
                        _state.update { it.copy(isValidatingWebsite = false, validatedWebsite = website) }
                    }
                } else {
                    Result.success(dAppWithResources)
                }
            }.onFailure {
                _state.update { state ->
                    state.copy(loading = false, isValidatingWebsite = false)
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
                (state.value.selectedSheetState as? SelectedSheetState.SelectedPersona)?.persona?.persona?.let { persona ->
                    updateSelectedPersonaData(persona)
                }
                _state.update { state ->
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

    fun onFungibleTokenClick(fungibleResource: Resource.FungibleResource) {
        viewModelScope.launch {
            sendEvent(DappDetailEvent.OnFungibleClick(fungibleResource))
        }
    }

    fun onNftClick(nftItem: Resource.NonFungibleResource) {
        viewModelScope.launch {
            sendEvent(DappDetailEvent.OnNonFungibleClick(nftItem))
        }
    }

    private suspend fun updateSelectedPersonaData(persona: Network.Persona) {
        val personaSimple =
            authorizedDapp.referencesToAuthorizedPersonas.firstOrNull { it.identityAddress == persona.address }
        val sharedAccounts = personaSimple?.sharedAccounts?.ids?.mapNotNull {
            getProfileUseCase.accountOnCurrentNetwork(AccountAddress.init(it))?.toUiModel()
        }.orEmpty()
        val requiredKinds = personaSimple?.sharedPersonaData?.alreadyGrantedIds().orEmpty().mapNotNull {
            persona.personaData.getDataFieldKind(it)
        }
        // TODO properly compute required fields and number of values when we will support multiple entry values
        val selectedPersona = PersonaUiModel(
            persona = persona,
            requiredPersonaFields = RequiredPersonaFields(
                fields = requiredKinds.map {
                    RequiredPersonaField(
                        kind = it,
                        numberOfValues = MessageFromDataChannel.IncomingRequest.NumberOfValues(
                            1,
                            MessageFromDataChannel.IncomingRequest.NumberOfValues.Quantifier.Exactly
                        )
                    )
                }
            )
        )
        _state.update {
            it.copy(
                sharedPersonaAccounts = sharedAccounts.toPersistentList(),
                selectedSheetState = SelectedSheetState.SelectedPersona(selectedPersona)
            )
        }
    }

    fun onPersonaDetailsClosed() {
        if (_state.value.selectedSheetState is SelectedSheetState.SelectedPersona) {
            _state.update {
                it.copy(
                    selectedSheetState = null,
                    sharedPersonaAccounts = persistentListOf()
                )
            }
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
            if (_state.value.selectedSheetState is SelectedSheetState.SelectedPersona) {
                (_state.value.selectedSheetState as SelectedSheetState.SelectedPersona).persona?.let { persona ->
                    sendEvent(
                        DappDetailEvent.EditPersona(persona.persona.address, persona.requiredPersonaFields)
                    )
                }
            }
        }
    }

    fun onEditAccountSharing() {
        viewModelScope.launch {
            if (_state.value.selectedSheetState is SelectedSheetState.SelectedPersona) {
                (_state.value.selectedSheetState as SelectedSheetState.SelectedPersona).persona?.persona?.let { persona ->
                    val sharedAccounts = checkNotNull(
                        authorizedDapp.referencesToAuthorizedPersonas.firstOrNull {
                            it.identityAddress == persona.address
                        }?.sharedAccounts
                    )
                    val request = MessageFromDataChannel.IncomingRequest.AuthorizedRequest(
                        remoteConnectorId = "",
                        interactionId = UUIDGenerator.uuid().toString(),
                        requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata(
                            authorizedDapp.networkID,
                            "",
                            authorizedDapp.dAppDefinitionAddress,
                            isInternal = true
                        ),
                        authRequest = MessageFromDataChannel.IncomingRequest.AuthorizedRequest.AuthRequest.UsePersonaRequest(
                            persona.address
                        ),
                        ongoingAccountsRequestItem = MessageFromDataChannel.IncomingRequest.AccountsRequestItem(
                            isOngoing = true,
                            numberOfValues = MessageFromDataChannel.IncomingRequest.NumberOfValues(
                                quantity = sharedAccounts.request.quantity,
                                quantifier = sharedAccounts.request.quantifier.toQuantifierUsedInRequest()
                            ),
                            challenge = null
                        ),
                        resetRequestItem = MessageFromDataChannel.IncomingRequest.ResetRequestItem(
                            accounts = true,
                            personaData = false
                        )
                    )
                    incomingRequestRepository.add(incomingRequest = request)
                }
            }
        }
    }

    fun hidePersonaBottomSheet() {
        _state.update { it.copy(selectedSheetState = null) }
    }
}

sealed interface DappDetailEvent : OneOffEvent {
    data class EditPersona(val personaAddress: String, val requiredPersonaFields: RequiredPersonaFields? = null) :
        DappDetailEvent

    data object LastPersonaDeleted : DappDetailEvent
    data object DappDeleted : DappDetailEvent
    data class OnFungibleClick(val resource: Resource.FungibleResource) : DappDetailEvent
    data class OnNonFungibleClick(val resource: Resource.NonFungibleResource) : DappDetailEvent
}

data class DappDetailUiState(
    val loading: Boolean = true,
    val dapp: Network.AuthorizedDapp? = null,
    val dAppWithResources: DAppWithResources? = null,
    val isValidatingWebsite: Boolean = false,
    val validatedWebsite: String? = null,
    val personas: ImmutableList<Network.Persona> = persistentListOf(),
    val sharedPersonaAccounts: ImmutableList<AccountItemUiModel> = persistentListOf(),
    val selectedSheetState: SelectedSheetState? = null
) : UiState {

    val isBottomSheetVisible: Boolean
        get() = selectedSheetState != null
}

sealed interface SelectedSheetState {
    data class SelectedPersona(
        val persona: PersonaUiModel?
    ) : SelectedSheetState
}