package com.babylon.wallet.android.presentation.settings.personas

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.model.PersonaUiModel
import com.babylon.wallet.android.domain.usecases.GetPersonasUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.utils.factorSourceId
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.personaOnCurrentNetwork
import javax.inject.Inject

@HiltViewModel
class PersonasViewModel @Inject constructor(
    private val getPersonasUseCase: GetPersonasUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val preferencesManager: PreferencesManager
) : StateViewModel<PersonasViewModel.PersonasUiState>(), OneOffEventHandler<PersonasViewModel.PersonasEvent> by OneOffEventHandlerImpl() {

    private var personaAddressThatNeedBackup: String? = null

    override fun initialState(): PersonasUiState = PersonasUiState()

    init {
        viewModelScope.launch {
            getPersonasUseCase().collect { personas ->
                updateUiState(personas)
            }
        }
        observeBackedUpMnemonics()
    }

    private fun updateUiState(personas: List<PersonaUiModel>) {
        personaAddressThatNeedBackup = personas.firstOrNull { !it.mnemonicBackedUp }?.address
        _state.update {
            it.copy(
                personas = personas.toPersistentList(),
                displaySecurityPrompt = personaAddressThatNeedBackup != null
            )
        }
    }

    private fun observeBackedUpMnemonics() {
        viewModelScope.launch {
            preferencesManager.getBackedUpFactorSourceIds().distinctUntilChanged().collect {
                getPersonasUseCase().firstOrNull()?.let { personas ->
                    updateUiState(personas)
                }
            }
        }
    }

    fun onCreatePersona() {
        viewModelScope.launch {
            sendEvent(PersonasEvent.CreatePersona(preferencesManager.firstPersonaCreated.first()))
        }
    }

    fun onApplySecuritySettings() {
        viewModelScope.launch {
            personaAddressThatNeedBackup?.let { address ->
                getProfileUseCase.personaOnCurrentNetwork(address)?.factorSourceId()?.let {
                    sendEvent(PersonasEvent.NavigateToMnemonicBackup(it))
                }
            }
        }
    }

    data class PersonasUiState(
        val personas: ImmutableList<PersonaUiModel> = persistentListOf(),
        val displaySecurityPrompt: Boolean = false
    ) : UiState

    sealed interface PersonasEvent : OneOffEvent {
        data class CreatePersona(val firstPersonaCreated: Boolean) : PersonasEvent
        data class NavigateToMnemonicBackup(val factorSourceId: FactorSource.ID) : PersonasEvent
    }
}
