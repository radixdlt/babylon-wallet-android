package com.babylon.wallet.android.presentation.settings.personas.createpersona

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesProxy
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
import com.radixdlt.sargon.EntityKind
import com.radixdlt.sargon.extensions.asGeneral
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.sargon.currentGateway
import rdx.works.core.sargon.mainBabylonFactorSource
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.ProfileException
import rdx.works.profile.domain.persona.CreatePersonaUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class CreatePersonaViewModel @Inject constructor(
    private val createPersonaUseCase: CreatePersonaUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val accessFactorSourcesProxy: AccessFactorSourcesProxy,
    private val appEventBus: AppEventBus
) : StateViewModel<CreatePersonaViewModel.CreatePersonaUiState>(),
    PersonaEditable by PersonaEditableImpl() {

    override fun initialState(): CreatePersonaUiState = CreatePersonaUiState()

    private var accessFactorSourcesJob: Job? = null

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
        accessFactorSourcesJob?.cancel()
        accessFactorSourcesJob = viewModelScope.launch {
            val displayName = DisplayName(_state.value.personaDisplayName.value)
            val personaData = _state.value.currentFields.toPersonaData()
            val factorSource = getProfileUseCase().mainBabylonFactorSource ?: return@launch
            accessFactorSourcesProxy.getPublicKeyAndDerivationPathForFactorSource(
                accessFactorSourcesInput = AccessFactorSourcesInput.ToDerivePublicKey(
                    entityKind = EntityKind.PERSONA,
                    forNetworkId = getProfileUseCase().currentGateway.network.id,
                    factorSource = factorSource,
                    isBiometricsProvided = false
                )
            ).mapCatching { hdPublicKey ->
                createPersonaUseCase(
                    displayName = displayName,
                    personaData = personaData,
                    factorSourceId = factorSource.value.id.asGeneral(),
                    hdPublicKey = hdPublicKey.value
                )
            }.onSuccess {
                _state.update { state -> state.copy(shouldNavigateToCompletion = true) }
            }.onFailure { error ->
                when (error) {
                    is ProfileException.SecureStorageAccess -> {
                        appEventBus.sendEvent(AppEvent.SecureFolderWarning)
                    }

                    is ProfileException.NoMnemonic -> {
                        _state.update {
                            it.copy(
                                isNoMnemonicErrorVisible = true
                            )
                        }
                    }

                    else -> {
                        Timber.d(error)
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

    fun onNavigationEventHandled() {
        _state.update { it.copy(shouldNavigateToCompletion = false) }
    }

    fun dismissNoMnemonicError() {
        _state.update { it.copy(isNoMnemonicErrorVisible = false) }
    }

    data class CreatePersonaUiState(
        val currentFields: ImmutableList<PersonaFieldWrapper> = persistentListOf(),
        val fieldsToAdd: ImmutableList<PersonaFieldWrapper> = persistentListOf(),
        val personaDisplayName: PersonaDisplayNameFieldWrapper = PersonaDisplayNameFieldWrapper(),
        val continueButtonEnabled: Boolean = false,
        val anyFieldSelected: Boolean = false,
        val isAddFieldBottomSheetVisible: Boolean = false,
        val uiMessage: UiMessage? = null,
        val isNoMnemonicErrorVisible: Boolean = false,
        val shouldNavigateToCompletion: Boolean = false
    ) : UiState
}
