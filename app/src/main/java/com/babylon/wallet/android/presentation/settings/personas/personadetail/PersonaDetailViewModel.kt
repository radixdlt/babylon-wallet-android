package com.babylon.wallet.android.presentation.settings.personas.personadetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.usecases.GetDAppsUseCase
import com.babylon.wallet.android.domain.usecases.factorsources.GetFactorSourceIntegrityStatusMessagesUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.babylon.wallet.android.presentation.ui.model.factors.toFactorSourceCard
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.unsecuredControllingFactorInstance
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.domain.DApp
import rdx.works.core.sargon.activePersonaOnCurrentNetwork
import rdx.works.core.sargon.factorSourceById
import rdx.works.profile.data.repository.DAppConnectionRepository
import rdx.works.profile.domain.ChangeEntityVisibilityUseCase
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class PersonaDetailViewModel @Inject constructor(
    dAppConnectionRepository: DAppConnectionRepository,
    getProfileUseCase: GetProfileUseCase,
    private val getDAppsUseCase: GetDAppsUseCase,
    savedStateHandle: SavedStateHandle,
    private val changeEntityVisibilityUseCase: ChangeEntityVisibilityUseCase,
    private val getFactorSourceIntegrityStatusMessagesUseCase: GetFactorSourceIntegrityStatusMessagesUseCase
) : StateViewModel<PersonaDetailUiState>(), OneOffEventHandler<Event> by OneOffEventHandlerImpl() {

    private val args = PersonaDetailScreenArgs(savedStateHandle)

    init {
        viewModelScope.launch {
            combine(
                getProfileUseCase.flow.mapNotNull { profile ->
                    profile.activePersonaOnCurrentNetwork(args.personaAddress)?.let { persona ->
                        val factorSource = persona.unsecuredControllingFactorInstance?.factorSourceId?.asGeneral()
                            ?.let { profile.factorSourceById(it) }
                        persona to factorSource
                    }
                },
                dAppConnectionRepository.getAuthorizedDAppsByPersona(args.personaAddress)
            ) { personaAndFactorSource, dApps ->
                personaAndFactorSource to dApps
            }.collect { personaAndFactorSourceToDApps ->
                val personaAndFactorSource = personaAndFactorSourceToDApps.first
                val metadataResults = personaAndFactorSourceToDApps.second.map { authorizedDApp ->
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
                        persona = personaAndFactorSource.first,
                        securedWith = personaAndFactorSource.second?.toFactorSourceCard(
                            includeLastUsedOn = true,
                            messages = personaAndFactorSource.second?.let { factorSource ->
                                getFactorSourceIntegrityStatusMessagesUseCase.forFactorSource(
                                    factorSource = factorSource,
                                    includeNoIssuesStatus = false,
                                    checkIntegrityOnlyIfAnyEntitiesLinked = false
                                )
                            }.orEmpty().toPersistentList()
                        ),
                        loading = false
                    )
                }
            }
        }
    }

    fun onHidePersona() {
        viewModelScope.launch {
            state.value.persona?.address?.let {
                changeEntityVisibilityUseCase.changePersonaVisibility(
                    entityAddress = it,
                    hide = true
                )
            }
            sendEvent(Event.Close)
        }
    }

    override fun initialState(): PersonaDetailUiState {
        return PersonaDetailUiState()
    }
}

sealed interface Event : OneOffEvent {
    data object Close : Event
}

data class PersonaDetailUiState(
    val loading: Boolean = true,
    val authorizedDapps: ImmutableList<DApp> = persistentListOf(),
    val persona: Persona? = null,
    val securedWith: FactorSourceCard? = null
) : UiState
