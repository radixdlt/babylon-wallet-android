package com.babylon.wallet.android.presentation.settings.personas.personadetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.manifest.prepareInternalTransactionRequest
import com.babylon.wallet.android.domain.usecases.GetDAppsUseCase
import com.babylon.wallet.android.domain.usecases.signing.ROLAClient
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.radixdlt.sargon.HierarchicalDeterministicFactorInstance
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.WalletInteractionId
import com.radixdlt.sargon.extensions.asProfileEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.domain.DApp
import rdx.works.core.sargon.activePersonaOnCurrentNetwork
import rdx.works.core.sargon.hasAuthSigning
import rdx.works.profile.data.repository.DAppConnectionRepository
import rdx.works.profile.domain.ChangeEntityVisibilityUseCase
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.account.AddAuthSigningFactorInstanceUseCase
import java.util.UUID
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class PersonaDetailViewModel @Inject constructor(
    dAppConnectionRepository: DAppConnectionRepository,
    getProfileUseCase: GetProfileUseCase,
    private val appEventBus: AppEventBus,
    private val addAuthSigningFactorInstanceUseCase: AddAuthSigningFactorInstanceUseCase,
    private val rolaClient: ROLAClient,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val getDAppsUseCase: GetDAppsUseCase,
    savedStateHandle: SavedStateHandle,
    private val changeEntityVisibilityUseCase: ChangeEntityVisibilityUseCase
) : StateViewModel<PersonaDetailUiState>(), OneOffEventHandler<Event> by OneOffEventHandlerImpl() {

    private val args = PersonaDetailScreenArgs(savedStateHandle)
    private var authSigningFactorInstance: HierarchicalDeterministicFactorInstance? = null
    private var uploadAuthKeyInteractionId: WalletInteractionId? = null

    init {
        viewModelScope.launch {
            combine(
                getProfileUseCase.flow.mapNotNull { it.activePersonaOnCurrentNetwork(args.personaAddress) },
                dAppConnectionRepository.getAuthorizedDAppsByPersona(args.personaAddress)
            ) { persona, dApps ->
                persona to dApps
            }.collect { personaToDApps ->
                val metadataResults = personaToDApps.second.map { authorizedDApp ->
                    getDAppsUseCase.invoke(
                        definitionAddress = authorizedDApp.dappDefinitionAddress,
                        needMostRecentData = false
                    ).getOrNull()
                }
                val dApps = metadataResults.mapNotNull { dAppWithAssociatedResources ->
                    dAppWithAssociatedResources
                }
                _state.update { state ->
                    state.copy(
                        authorizedDapps = dApps.toImmutableList(),
                        persona = personaToDApps.first,
                        loading = false,
                        hasAuthKey = personaToDApps.first.hasAuthSigning
                    )
                }
            }
        }

        viewModelScope.launch {
            appEventBus.events.filterIsInstance<AppEvent.Status.Transaction>().filter { it.requestId == uploadAuthKeyInteractionId }
                .collect { event ->
                    when (event) {
                        is AppEvent.Status.Transaction.Fail -> {
                            _state.update { it.copy(loading = false) }
                        }

                        is AppEvent.Status.Transaction.Success -> {
                            val persona = requireNotNull(state.value.persona)
                            val authSigningFactorInstance = requireNotNull(authSigningFactorInstance)
                            addAuthSigningFactorInstanceUseCase(persona.asProfileEntity(), authSigningFactorInstance)
                            _state.update { it.copy(loading = false) }
                        }

                        else -> {}
                    }
                }
        }
    }

    fun onHidePersona() {
        viewModelScope.launch {
            state.value.persona?.address?.let { changeEntityVisibilityUseCase.changePersonaVisibility(entityAddress = it, hide = true) }
            sendEvent(Event.Close)
        }
    }

    override fun initialState(): PersonaDetailUiState {
        return PersonaDetailUiState()
    }

    fun onCreateAndUploadAuthKey() {
        viewModelScope.launch {
            state.value.persona?.let { persona ->
                _state.update { it.copy(loading = true) }
                val entity = persona.asProfileEntity()
                rolaClient.generateAuthSigningFactorInstance(entity)
                    .onSuccess { authSigningFactorInstance ->
                        this@PersonaDetailViewModel.authSigningFactorInstance = authSigningFactorInstance
                        val manifest = rolaClient
                            .createAuthKeyManifest(entity, authSigningFactorInstance)
                            .getOrElse {
                                _state.update { state -> state.copy(loading = false) }
                                return@launch
                            }
                        val interactionId = UUID.randomUUID().toString()
                        uploadAuthKeyInteractionId = interactionId
                        incomingRequestRepository.add(
                            manifest.prepareInternalTransactionRequest(
                                requestId = interactionId
                            )
                        )
                    }
            }
        }
    }
}

sealed interface Event : OneOffEvent {
    data object Close : Event
}

data class PersonaDetailUiState(
    val loading: Boolean = true,
    val authorizedDapps: ImmutableList<DApp> = persistentListOf(),
    val persona: Persona? = null,
    val hasAuthKey: Boolean = false
) : UiState
