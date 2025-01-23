package com.babylon.wallet.android.presentation.settings.personas.createpersona

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.PersonaEditable
import com.babylon.wallet.android.presentation.common.PersonaEditableImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.model.PersonaDisplayNameFieldWrapper
import com.babylon.wallet.android.presentation.model.PersonaFieldWrapper
import com.babylon.wallet.android.presentation.model.toPersonaData
import com.radixdlt.sargon.DisplayName
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.os.SargonOsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.activePersonasOnCurrentNetwork
import rdx.works.core.sargon.currentGateway
import rdx.works.profile.domain.GetProfileUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class CreatePersonaViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val sargonOsManager: SargonOsManager,
    private val preferencesManager: PreferencesManager,
) : StateViewModel<CreatePersonaViewModel.State>(),
    OneOffEventHandler<CreatePersonaViewModel.Event> by OneOffEventHandlerImpl(),
    PersonaEditable by PersonaEditableImpl() {

    override fun initialState(): State = State()

    private var accessFactorSourcesJob: Job? = null

    init {
        viewModelScope.launch {
            personaEditState.collect { s ->
                _state.update {
                    it.copy(
                        anyFieldSelected = s.areThereFieldsSelected,
                        personaDisplayName = s.personaDisplayName,
                        currentFields = s.currentFields,
                        fieldsToAdd = s.fieldsToAdd
                    )
                }
            }
        }
        setPersona(null)
    }

    fun onPersonaCreateClick() {
        accessFactorSourcesJob?.cancel()
        accessFactorSourcesJob = viewModelScope.launch {
            _state.update { it.copy(isPersonaCreating = true) }
            val personaData = _state.value.currentFields.toPersonaData()
            val profile = getProfileUseCase()
            val networkId = profile.currentGateway.network.id
            val isFirstPersonaAboutToBeCreated = profile.activePersonasOnCurrentNetwork.isEmpty()

            runCatching {
                sargonOsManager.sargonOs.createAndSaveNewPersonaWithBdfs(
                    networkId = networkId,
                    name = DisplayName.init(_state.value.personaDisplayName.value.trim()),
                    personaData = personaData
                )
            }.onSuccess {
                if (isFirstPersonaAboutToBeCreated) {
                    preferencesManager.markFirstPersonaCreated()
                }

                _state.update { state -> state.copy(isPersonaCreating = false) }
                sendEvent(Event.NavigateToCompletion)
            }.onFailure { error ->
                Timber.w(error)
                _state.update { state -> state.copy(isPersonaCreating = false) }
            }
        }
    }

    fun setAddFieldSheetVisible(isVisible: Boolean) {
        _state.update { it.copy(isAddFieldBottomSheetVisible = isVisible) }
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    data class State(
        val currentFields: ImmutableList<PersonaFieldWrapper> = persistentListOf(),
        val fieldsToAdd: ImmutableList<PersonaFieldWrapper> = persistentListOf(),
        val personaDisplayName: PersonaDisplayNameFieldWrapper = PersonaDisplayNameFieldWrapper(),
        val anyFieldSelected: Boolean = false,
        val isAddFieldBottomSheetVisible: Boolean = false,
        val uiMessage: UiMessage? = null,
        val isPersonaCreating: Boolean = false,
    ) : UiState {
        val isContinueButtonEnabled: Boolean
            get() {
                val valid = currentFields.all { it.isValid == true } && personaDisplayName.isValid
                return valid
            }
    }

    sealed interface Event : OneOffEvent {
        data object NavigateToCompletion : Event
    }
}
