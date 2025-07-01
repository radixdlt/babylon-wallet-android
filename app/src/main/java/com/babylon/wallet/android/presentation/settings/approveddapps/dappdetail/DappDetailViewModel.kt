package com.babylon.wallet.android.presentation.settings.approveddapps.dappdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.domain.model.DAppWithResources
import com.babylon.wallet.android.domain.model.messages.DappToWalletInteraction
import com.babylon.wallet.android.domain.model.messages.RemoteEntityID
import com.babylon.wallet.android.domain.model.messages.RequiredPersonaField
import com.babylon.wallet.android.domain.model.messages.RequiredPersonaFields
import com.babylon.wallet.android.domain.model.messages.WalletAuthorizedRequest
import com.babylon.wallet.android.domain.usecases.ChangeLockerDepositsVisibilityUseCase
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
import com.radixdlt.sargon.AuthorizedDappPreferenceDeposits
import com.radixdlt.sargon.DappToWalletInteractionResetRequestItem
import com.radixdlt.sargon.IdentityAddress
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.extensions.string
import com.radixdlt.sargon.newWalletInteractionVersionCurrent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.domain.resources.Resource
import rdx.works.core.sargon.activeAccountOnCurrentNetwork
import rdx.works.core.sargon.activePersonaOnCurrentNetwork
import rdx.works.core.sargon.alreadyGrantedIds
import rdx.works.core.sargon.asIdentifiable
import rdx.works.core.sargon.getDataFieldKind
import rdx.works.core.then
import rdx.works.profile.data.repository.DAppConnectionRepository
import rdx.works.profile.domain.GetProfileUseCase
import java.util.UUID
import javax.inject.Inject

@Suppress("TooManyFunctions", "LongParameterList")
@HiltViewModel
class DappDetailViewModel @Inject constructor(
    private val dAppConnectionRepository: DAppConnectionRepository,
    private val dAppWithAssociatedResourcesUseCase: GetDAppWithResourcesUseCase,
    private val getValidatedDAppWebsiteUseCase: GetValidatedDAppWebsiteUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val changeLockerDepositsVisibilityUseCase: ChangeLockerDepositsVisibilityUseCase,
    savedStateHandle: SavedStateHandle
) : StateViewModel<DappDetailUiState>(), OneOffEventHandler<DappDetailEvent> by OneOffEventHandlerImpl() {

    private lateinit var authorizedDapp: AuthorizedDapp
    private val args = DappDetailScreenArgs(savedStateHandle)

    override fun initialState(): DappDetailUiState = DappDetailUiState(
        isReadOnly = args.isReadOnly
    )

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
            dAppConnectionRepository.getAuthorizedDAppFlow(args.dappDefinitionAddress).onEach { authorizedDApp ->
                _state.update { state ->
                    state.copy(
                        isReadOnly = state.isReadOnly || authorizedDApp == null
                    )
                }
            }.filterNotNull().collect { dapp ->
                authorizedDapp = dapp
                val personas = authorizedDapp.referencesToAuthorizedPersonas.mapNotNull { personaSimple ->
                    getProfileUseCase().activePersonaOnCurrentNetwork(personaSimple.identityAddress)
                }
                (state.value.selectedSheetState as? SelectedSheetState.SelectedPersona)?.persona?.persona?.let { persona ->
                    updateSelectedPersonaData(persona)
                }
                _state.update { state ->
                    state.copy(
                        isShowLockerDepositsChecked = dapp.preferences.deposits == AuthorizedDappPreferenceDeposits.VISIBLE,
                        authorizedPersonas = personas.toPersistentList(),
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
                        numberOfValues = DappToWalletInteraction.NumberOfValues(
                            1,
                            DappToWalletInteraction.NumberOfValues.Quantifier.Exactly
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
        val lastPersona =
            _state.value.authorizedPersonas.size == 1 && _state.value.authorizedPersonas.first().address == persona.address
        viewModelScope.launch {
            dAppConnectionRepository.deletePersonaForDApp(args.dappDefinitionAddress, persona.address)
            if (lastPersona) {
                sendEvent(DappDetailEvent.DappDeleted)
            }
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
                        authorizedDapp.referencesToAuthorizedPersonas.asIdentifiable()
                            .getBy(persona.address)?.sharedAccounts
                    )
                    val request = WalletAuthorizedRequest(
                        remoteEntityId = RemoteEntityID.ConnectorId(""),
                        interactionId = UUID.randomUUID().toString(),
                        requestMetadata = DappToWalletInteraction.RequestMetadata(
                            version = newWalletInteractionVersionCurrent(),
                            networkId = authorizedDapp.networkId,
                            origin = "",
                            dAppDefinitionAddress = authorizedDapp.dappDefinitionAddress.string,
                            isInternal = true
                        ),
                        authRequestItem = WalletAuthorizedRequest.AuthRequestItem.UsePersonaRequest(
                            persona.address
                        ),
                        ongoingAccountsRequestItem = DappToWalletInteraction.AccountsRequestItem(
                            isOngoing = true,
                            numberOfValues = DappToWalletInteraction.NumberOfValues(
                                quantity = sharedAccounts.request.quantity.toInt(),
                                quantifier = sharedAccounts.request.quantifier.toQuantifierUsedInRequest()
                            ),
                            challenge = null
                        ),
                        resetRequestItem = DappToWalletInteractionResetRequestItem(
                            accounts = true,
                            personaData = false
                        )
                    )
                    incomingRequestRepository.add(dappToWalletInteraction = request)
                }
            }
        }
    }

    fun hidePersonaBottomSheet() {
        _state.update { it.copy(selectedSheetState = null) }
    }

    fun onShowLockerDepositsCheckedChange(isChecked: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(isShowLockerDepositsChecked = isChecked) }
            changeLockerDepositsVisibilityUseCase(authorizedDapp, isChecked)
        }
    }
}

sealed interface DappDetailEvent : OneOffEvent {
    data class EditPersona(
        val personaAddress: IdentityAddress,
        val requiredPersonaFields: RequiredPersonaFields? = null
    ) :
        DappDetailEvent

    data object DappDeleted : DappDetailEvent
    data class OnFungibleClick(val resource: Resource.FungibleResource) : DappDetailEvent
    data class OnNonFungibleClick(val resource: Resource.NonFungibleResource) : DappDetailEvent
}

data class DappDetailUiState(
    val loading: Boolean = true,
    val dAppWithResources: DAppWithResources? = null,
    val isValidatingWebsite: Boolean = false,
    val validatedWebsite: String? = null,
    val isReadOnly: Boolean = false,
    val authorizedPersonas: ImmutableList<Persona> = persistentListOf(),
    val sharedPersonaAccounts: ImmutableList<AccountItemUiModel> = persistentListOf(),
    val selectedSheetState: SelectedSheetState? = null,
    val isShowLockerDepositsChecked: Boolean = false
) : UiState {

    val isBottomSheetVisible: Boolean
        get() = selectedSheetState != null
}

sealed interface SelectedSheetState {
    data class SelectedPersona(
        val persona: PersonaUiModel?
    ) : SelectedSheetState
}
