package com.babylon.wallet.android.presentation.settings.personas.personadetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.manifest.prepareInternalTransactionRequest
import com.babylon.wallet.android.data.transaction.ROLAClient
import com.babylon.wallet.android.domain.common.value
import com.babylon.wallet.android.domain.model.DAppWithMetadataAndAssociatedResources
import com.babylon.wallet.android.domain.usecases.GetDAppWithMetadataAndAssociatedResourcesUseCase
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.UUIDGenerator
import rdx.works.profile.data.model.pernetwork.FactorInstance
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.repository.DAppConnectionRepository
import rdx.works.profile.data.utils.hasAuthSigning
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.account.AddAuthSigningFactorInstanceUseCase
import rdx.works.profile.domain.personaOnCurrentNetworkFlow
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
    private val dAppWithAssociatedResourcesUseCase: GetDAppWithMetadataAndAssociatedResourcesUseCase,
    savedStateHandle: SavedStateHandle
) : StateViewModel<PersonaDetailUiState>() {

    private val args = PersonaDetailScreenArgs(savedStateHandle)
    private var authSigningFactorInstance: FactorInstance? = null
    private lateinit var uploadAuthKeyRequestId: String

    init {
        viewModelScope.launch {
            combine(
                getProfileUseCase.personaOnCurrentNetworkFlow(args.personaAddress),
                dAppConnectionRepository.getAuthorizedDappsByPersona(args.personaAddress)
            ) { persona, dApps ->
                persona to dApps
            }.collect { personaToDApps ->
                val metadataResults = personaToDApps.second.map { authorizedDApp ->
                    dAppWithAssociatedResourcesUseCase.invoke(
                        definitionAddress = authorizedDApp.dAppDefinitionAddress,
                        needMostRecentData = false
                    ).value()
                }
                val dApps = metadataResults.mapNotNull { dAppWithAssociatedResources ->
                    dAppWithAssociatedResources
                }
                _state.update { state ->
                    state.copy(
                        authorizedDapps = dApps.toImmutableList(),
                        persona = personaToDApps.first,
                        loading = false,
                        hasAuthKey = personaToDApps.first.hasAuthSigning()
                    )
                }
            }
        }

        viewModelScope.launch {
            appEventBus.events.filterIsInstance<AppEvent.Status.Transaction>().filter { it.requestId == uploadAuthKeyRequestId }
                .collect { event ->
                    when (event) {
                        is AppEvent.Status.Transaction.Fail -> {
                            _state.update { it.copy(loading = false) }
                        }
                        is AppEvent.Status.Transaction.Success -> {
                            val persona = requireNotNull(state.value.persona)
                            val authSigningFactorInstance = requireNotNull(authSigningFactorInstance)
                            addAuthSigningFactorInstanceUseCase(persona, authSigningFactorInstance)
                            _state.update { it.copy(loading = false) }
                        }
                        else -> {}
                    }
                }
        }
    }

    fun onDAppClick(dApp: DAppWithMetadataAndAssociatedResources) {
        _state.update { state ->
            state.copy(selectedDApp = dApp)
        }
    }

    override fun initialState(): PersonaDetailUiState {
        return PersonaDetailUiState()
    }

    fun onCreateAndUploadAuthKey() {
        viewModelScope.launch {
            state.value.persona?.let { persona ->
                _state.update { it.copy(loading = true) }
                rolaClient.generateAuthSigningFactorInstance(persona).onSuccess { authSigningFactorInstance ->
                    this@PersonaDetailViewModel.authSigningFactorInstance = authSigningFactorInstance
                    val manifest = rolaClient
                        .createAuthKeyManifestWithStringInstructions(persona, authSigningFactorInstance)
                        .getOrElse {
                            _state.update { state -> state.copy(loading = false) }
                            return@launch
                        }
                    uploadAuthKeyRequestId = UUIDGenerator.uuid().toString()
                    incomingRequestRepository.add(
                        manifest.prepareInternalTransactionRequest(
                            networkId = persona.networkID,
                            requestId = uploadAuthKeyRequestId
                        )
                    )
                }
            }
        }
    }
}

data class PersonaDetailUiState(
    val loading: Boolean = true,
    val authorizedDapps: ImmutableList<DAppWithMetadataAndAssociatedResources> = persistentListOf(),
    val persona: Network.Persona? = null,
    val hasAuthKey: Boolean = false,
    val selectedDApp: DAppWithMetadataAndAssociatedResources? = null
) : UiState
