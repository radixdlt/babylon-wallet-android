package com.babylon.wallet.android.presentation.settings.personas

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.usecases.securityproblems.EntityWithSecurityPrompt
import com.babylon.wallet.android.domain.usecases.securityproblems.GetEntitiesWithSecurityPromptUseCase
import com.babylon.wallet.android.domain.usecases.securityproblems.SecurityPromptType
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.extensions.string
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.activePersonasOnCurrentNetwork
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@HiltViewModel
class PersonasViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val preferencesManager: PreferencesManager,
    private val getEntitiesWithSecurityPromptUseCase: GetEntitiesWithSecurityPromptUseCase,
    @DefaultDispatcher defaultDispatcher: CoroutineDispatcher
) : StateViewModel<PersonasViewModel.PersonasUiState>(),
    OneOffEventHandler<PersonasViewModel.PersonasEvent> by OneOffEventHandlerImpl() {

    override fun initialState(): PersonasUiState = PersonasUiState()

    init {
        viewModelScope.launch {
            combine(
                getProfileUseCase.flow.map { it.activePersonasOnCurrentNetwork },
                getEntitiesWithSecurityPromptUseCase()
            ) { personas, entitiesWithSecurityPrompts ->
                _state.update {
                    it.copy(
                        personas = personas.toPersistentList(),
                        entitiesWithSecurityPrompts = entitiesWithSecurityPrompts,
                    )
                }
            }
                .flowOn(defaultDispatcher)
                .collect()
        }
    }

    fun onCreatePersona() {
        viewModelScope.launch {
            sendEvent(PersonasEvent.CreatePersona(preferencesManager.firstPersonaCreated.first()))
        }
    }

    data class PersonasUiState(
        val personas: ImmutableList<Persona> = persistentListOf(),
        val entitiesWithSecurityPrompts: List<EntityWithSecurityPrompt> = emptyList()
    ) : UiState {
        fun securityPrompt(forEntity: Persona): List<SecurityPromptType>? {
            return entitiesWithSecurityPrompts.find {
                it.entity.address.string == forEntity.address.string
            }?.prompts?.toList()
        }
    }

    sealed interface PersonasEvent : OneOffEvent {
        data class CreatePersona(val firstPersonaCreated: Boolean) : PersonasEvent
    }
}
