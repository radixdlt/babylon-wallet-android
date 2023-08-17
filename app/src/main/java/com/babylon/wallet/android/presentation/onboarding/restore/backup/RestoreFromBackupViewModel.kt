package com.babylon.wallet.android.presentation.onboarding.restore.backup

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.Header
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.factorsources.FactorSourceKind
import rdx.works.profile.data.repository.MnemonicRepository
import rdx.works.profile.data.repository.ProfileRepository
import rdx.works.profile.domain.backup.RestoreProfileFromBackupUseCase
import javax.inject.Inject

@HiltViewModel
class RestoreFromBackupViewModel @Inject constructor(
    profileRepository: ProfileRepository,
    private val restoreProfileFromBackupUseCase: RestoreProfileFromBackupUseCase,
    private val mnemonicRepository: MnemonicRepository
) : StateViewModel<RestoreFromBackupViewModel.State>(), OneOffEventHandler<RestoreFromBackupViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State = State()

    init {
        viewModelScope.launch {
            val profileToRestore = profileRepository.getRestoredProfileFromBackup()
            _state.update { it.copy(restoringProfileHeader = profileToRestore?.header) }
        }
    }

    fun toggleRestoringProfileCheck(isChecked: Boolean) {
        if (state.value.restoringProfileHeader?.isCompatible == true) {
            _state.update { it.copy(isRestoringProfileChecked = isChecked) }
        }
    }

    fun onBackClick() = viewModelScope.launch {
        sendEvent(Event.OnDismiss)
    }

    fun onSubmit() = viewModelScope.launch {
        if (state.value.isRestoringProfileChecked) {
            val restoredProfile = restoreProfileFromBackupUseCase()

            val factorSourcesNeedingRecovery = restoredProfile?.factorSources?.mapNotNull {
                val factorSourceHash = it.id as? FactorSource.FactorSourceID.FromHash ?: return@mapNotNull null
                if (factorSourceHash.kind == FactorSourceKind.DEVICE) {
                    factorSourceHash
                } else {
                    null
                }
            }?.filterNot { mnemonicRepository.readMnemonic(it) == null }.orEmpty()

            sendEvent(Event.OnRestored(needsMnemonicRecovery = factorSourcesNeedingRecovery.isNotEmpty()))
        }
    }

    data class State(
        val restoringProfileHeader: Header? = null,
        val isRestoringProfileChecked: Boolean = false
    ) : UiState {

        val isContinueEnabled: Boolean
            get() = isRestoringProfileChecked

    }

    sealed interface Event : OneOffEvent {
        object OnDismiss : Event
        data class OnRestored(val needsMnemonicRecovery: Boolean) : Event
    }
}
