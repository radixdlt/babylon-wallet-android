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
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.radixdlt.sargon.DisplayName
import com.radixdlt.sargon.IdentityAddress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.domain.ProfileException
import rdx.works.profile.domain.persona.CreatePersonaWithDeviceFactorSourceUseCase
import javax.inject.Inject

@HiltViewModel
class CreatePersonaViewModel @Inject constructor(
    private val createPersonaWithDeviceFactorSourceUseCase: CreatePersonaWithDeviceFactorSourceUseCase,
    private val preferencesManager: PreferencesManager,
    private val appEventBus: AppEventBus
) : StateViewModel<CreatePersonaViewModel.CreatePersonaUiState>(),
    OneOffEventHandler<CreatePersonaEvent> by OneOffEventHandlerImpl(),
    PersonaEditable by PersonaEditableImpl() {

    override fun initialState(): CreatePersonaUiState = CreatePersonaUiState()

    init {
        viewModelScope.launch {
            personaEditState.collect { s ->
                _state.update {
                    it.copy(
                        anyFieldSelected = s.areThereFieldsSelected,
                        personaDisplayName = s.personaDisplayName,
                        continueButtonEnabled = s.inputValid,
                        currentFields = s.currentFields,
                        fieldsToAdd = s.fieldsToAdd
                    )
                }
            }
        }
        setPersona(null)
    }

    fun onPersonaCreateClick() {
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            val personaData = _state.value.currentFields.toPersonaData()
            val persona = createPersonaWithDeviceFactorSourceUseCase(
                displayName = DisplayName(_state.value.personaDisplayName.value),
                personaData = personaData
            ).onSuccess { persona ->
                val personaId = persona.address
                _state.update { it.copy(loading = true) }
                preferencesManager.markFirstPersonaCreated()

                sendEvent(
                    CreatePersonaEvent.Complete(
                        personaId = personaId
                    )
                )
            }.onFailure { error ->
                when {
                    error is ProfileException.SecureStorageAccess -> {
                        appEventBus.sendEvent(AppEvent.SecureFolderWarning)
                    }

                    else -> {
                        _state.update {
                            if (error is ProfileException.NoMnemonic) {
                                it.copy(loading = false, isNoMnemonicErrorVisible = true)
                            } else {
                                it.copy(loading = false, uiMessage = UiMessage.ErrorMessage(error))
                            }
                        }
                    }
                }
            }
        }
    }

    fun setAddFieldSheetVisible(isVisible: Boolean) {
        _state.update { it.copy(isAddFieldBottomSheetVisible = isVisible) }
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    fun dismissNoMnemonicError() {
        _state.update { it.copy(isNoMnemonicErrorVisible = false) }
    }

    data class CreatePersonaUiState(
        val loading: Boolean = false,
        val currentFields: ImmutableList<PersonaFieldWrapper> = persistentListOf(),
        val fieldsToAdd: ImmutableList<PersonaFieldWrapper> = persistentListOf(),
        val personaDisplayName: PersonaDisplayNameFieldWrapper = PersonaDisplayNameFieldWrapper(),
        val continueButtonEnabled: Boolean = false,
        val anyFieldSelected: Boolean = false,
        val isAddFieldBottomSheetVisible: Boolean = false,
        val uiMessage: UiMessage? = null,
        val isNoMnemonicErrorVisible: Boolean = false
    ) : UiState
}

internal sealed interface CreatePersonaEvent : OneOffEvent {
    data class Complete(
        val personaId: IdentityAddress
    ) : CreatePersonaEvent
}
