package com.babylon.wallet.android.presentation.settings.personas.personadetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.usecases.GetDAppsUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.Persona
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.domain.DApp
import rdx.works.core.sargon.activePersonaOnCurrentNetwork
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
) : StateViewModel<PersonaDetailUiState>(), OneOffEventHandler<Event> by OneOffEventHandlerImpl() {

    private val args = PersonaDetailScreenArgs(savedStateHandle)

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
                        loading = false
                    )
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
}

sealed interface Event : OneOffEvent {
    data object Close : Event
}

data class PersonaDetailUiState(
    val loading: Boolean = true,
    val authorizedDapps: ImmutableList<DApp> = persistentListOf(),
    val persona: Persona? = null
) : UiState
