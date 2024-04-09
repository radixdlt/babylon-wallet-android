package com.babylon.wallet.android.presentation.settings.approveddapps.dappdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.domain.model.DAppWithResources
import com.babylon.wallet.android.domain.model.IncomingMessage
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
import com.radixdlt.sargon.AuthorizedDapp
import com.radixdlt.sargon.IdentityAddress
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.extensions.string
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.UUIDGenerator
import rdx.works.core.domain.resources.Resource
import rdx.works.core.sargon.activeAccountOnCurrentNetwork
import rdx.works.core.sargon.activePersonaOnCurrentNetwork
import rdx.works.core.sargon.alreadyGrantedIds
import rdx.works.core.sargon.asIdentifiable
import rdx.works.core.sargon.getDataFieldKind
import rdx.works.core.then
import rdx.works.profile.data.repository.DAppConnectionRepository
import rdx.works.profile.domain.GetProfileUseCase
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

    private lateinit var authorizedDapp: AuthorizedDapp
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
            dAppConnectionRepository.getAuthorizedDAppFlow(args.dappDefinitionAddress).collect {
                if (it == null) {
                    sendEvent(DappDetailEvent.LastPersonaDeleted)
                    return@collect
                } else {
                    authorizedDapp = checkNotNull(dAppConnectionRepository.getAuthorizedDApp(args.dappDefinitionAddress))
                }
                val personas = authorizedDapp.referencesToAuthorizedPersonas.mapNotNull { personaSimple ->
                    getProfileUseCase().activePersonaOnCurrentNetwork(personaSimple.identityAddress)
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

    fun onPersonaClick(persona: Persona) {
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

    private suspend fun updateSelectedPersonaData(persona: Persona) {
        val personaSimple =
            authorizedDapp.referencesToAuthorizedPersonas.firstOrNull { it.identityAddress == persona.address }
        val sharedAccounts = personaSimple?.sharedAccounts?.ids?.mapNotNull {
            getProfileUseCase().activeAccountOnCurrentNetwork(it)?.toUiModel()
        }.orEmpty()
        val requiredKinds = personaSimple?.sharedPersonaData?.alreadyGrantedIds.orEmpty().mapNotNull {
            persona.personaData.getDataFieldKind(it)
        }
        // TODO properly compute required fields and number of values when we will support multiple entry values
        val selectedPersona = PersonaUiModel(
            persona = persona,
            requiredPersonaFields = RequiredPersonaFields(
                fields = requiredKinds.map {
                    RequiredPersonaField(
                        kind = it,
                        numberOfValues = IncomingMessage.IncomingRequest.NumberOfValues(
                            1,
                            IncomingMessage.IncomingRequest.NumberOfValues.Quantifier.Exactly
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

    fun onDisconnectPersona(persona: Persona) {
        viewModelScope.launch {
            dAppConnectionRepository.deletePersonaForDApp(args.dappDefinitionAddress, persona.address)
        }
    }

    fun onDeleteDapp() {
        viewModelScope.launch {
            dAppConnectionRepository.deleteAuthorizedDApp(args.dappDefinitionAddress)
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
                        authorizedDapp.referencesToAuthorizedPersonas.asIdentifiable().getBy(persona.address)?.sharedAccounts
                    )
                    val request = IncomingMessage.IncomingRequest.AuthorizedRequest(
                        remoteEntityId = IncomingMessage.RemoteEntityID.ConnectorId(""),
                        interactionId = UUIDGenerator.uuid().toString(),
                        requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata(
                            authorizedDapp.networkId,
                            "",
                            authorizedDapp.dappDefinitionAddress.string,
                            isInternal = true
                        ),
                        authRequest = MessageFromDataChannel.IncomingRequest.AuthorizedRequest.AuthRequest.UsePersonaRequest(
                            persona.address.string
                        ),
                        ongoingAccountsRequestItem = IncomingMessage.IncomingRequest.AccountsRequestItem(
                            isOngoing = true,
                            numberOfValues = MessageFromDataChannel.IncomingRequest.NumberOfValues(
                                quantity = sharedAccounts.request.quantity.toInt(),
                                quantifier = sharedAccounts.request.quantifier.toQuantifierUsedInRequest()
                            ),
                            challenge = null
                        ),
                        resetRequestItem = IncomingMessage.IncomingRequest.AuthorizedRequest.ResetRequestItem(
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
    data class EditPersona(val personaAddress: IdentityAddress, val requiredPersonaFields: RequiredPersonaFields? = null) :
        DappDetailEvent

    data object LastPersonaDeleted : DappDetailEvent
    data object DappDeleted : DappDetailEvent
    data class OnFungibleClick(val resource: Resource.FungibleResource) : DappDetailEvent
    data class OnNonFungibleClick(val resource: Resource.NonFungibleResource) : DappDetailEvent
}

data class DappDetailUiState(
    val loading: Boolean = true,
    val dapp: AuthorizedDapp? = null,
    val dAppWithResources: DAppWithResources? = null,
    val isValidatingWebsite: Boolean = false,
    val validatedWebsite: String? = null,
    val personas: ImmutableList<Persona> = persistentListOf(),
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
